package com.example.scoreboard.persistence;

import com.example.scoreboard.domain.ErrorCode;
import com.example.scoreboard.domain.Match;
import com.example.scoreboard.domain.ScoreboardException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryMatchRepository implements MatchRepository {
  private static final int HISTORY_LIMIT = 20;

  private final ConcurrentMap<Long, MatchState> activeMatches = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Long> activeTeamToMatchId = new ConcurrentHashMap<>();
  private final Map<Long, Match> finishedHistory = new LinkedHashMap<>();
  private final Object creationLock = new Object();
  private final Object historyLock = new Object();

  @Override
  public Match create(
      long matchId, String homeTeam, String awayTeam, Instant startedAt, long startOrder) {
    Match match = new Match(matchId, homeTeam, awayTeam, 0, 0, startedAt, startOrder, false);
    String homeKey = canonical(homeTeam);
    String awayKey = canonical(awayTeam);
    synchronized (creationLock) {
      if (activeTeamToMatchId.containsKey(homeKey) || activeTeamToMatchId.containsKey(awayKey)) {
        throw new ScoreboardException(
            ErrorCode.DUPLICATE_ACTIVE_TEAM, "A team is already participating in a match");
      }
      activeTeamToMatchId.put(homeKey, matchId);
      activeTeamToMatchId.put(awayKey, matchId);
      activeMatches.put(matchId, new MatchState(match));
    }
    return match;
  }

  @Override
  public Match updateScore(long matchId, int homeScore, int awayScore) {
    validateScores(homeScore, awayScore);
    MatchState state = activeMatches.get(matchId);
    if (state == null) {
      throw missingOrFinished(matchId);
    }
    synchronized (state) {
      Match current = state.snapshot;
      if (current.isFinished()) {
        throw new ScoreboardException(
            ErrorCode.MATCH_ALREADY_FINISHED, "The match has already finished");
      }
      state.snapshot = current.withScore(homeScore, awayScore);
      return state.snapshot;
    }
  }

  @Override
  public Match finish(long matchId) {
    MatchState state = activeMatches.get(matchId);
    if (state == null) {
      throw missingOrFinished(matchId);
    }
    synchronized (state) {
      Match finishedMatch = state.snapshot.finish();
      synchronized (creationLock) {
        if (!activeMatches.remove(matchId, state)) {
          throw missingOrFinished(matchId);
        }
        activeTeamToMatchId.remove(canonical(finishedMatch.getHomeTeam()), matchId);
        activeTeamToMatchId.remove(canonical(finishedMatch.getAwayTeam()), matchId);
        synchronized (historyLock) {
          finishedHistory.put(matchId, finishedMatch);
          evictOldestIfNecessary();
        }
      }
      state.snapshot = finishedMatch;
      return finishedMatch;
    }
  }

  @Override
  public Optional<Match> findActive(long matchId) {
    MatchState state = activeMatches.get(matchId);
    return state == null ? Optional.empty() : Optional.of(state.snapshot());
  }

  @Override
  public Optional<Match> findAny(long matchId) {
    Optional<Match> active = findActive(matchId);
    if (active.isPresent()) {
      return active;
    }
    synchronized (historyLock) {
      return Optional.ofNullable(finishedHistory.get(matchId));
    }
  }

  @Override
  public List<Match> findActiveMatches() {
    List<Match> matches = new ArrayList<>();
    for (MatchState state : activeMatches.values()) {
      matches.add(state.snapshot());
    }
    return matches;
  }

  private static String canonical(String teamName) {
    return teamName.toLowerCase(Locale.ROOT);
  }

  private static void validateScores(int homeScore, int awayScore) {
    if (homeScore < 0 || awayScore < 0) {
      throw new ScoreboardException(ErrorCode.INVALID_SCORE, "Scores must not be negative");
    }
  }

  private ScoreboardException missingOrFinished(long matchId) {
    synchronized (historyLock) {
      if (finishedHistory.containsKey(matchId)) {
        return new ScoreboardException(
            ErrorCode.MATCH_ALREADY_FINISHED, "The match has already finished");
      }
    }
    return new ScoreboardException(ErrorCode.MATCH_NOT_FOUND, "Match was not found: " + matchId);
  }

  private void evictOldestIfNecessary() {
    while (finishedHistory.size() > HISTORY_LIMIT) {
      Long oldestMatchId = finishedHistory.keySet().iterator().next();
      finishedHistory.remove(oldestMatchId);
    }
  }

  private static final class MatchState {
    private Match snapshot;

    private MatchState(Match snapshot) {
      this.snapshot = snapshot;
    }

    private synchronized Match snapshot() {
      return snapshot;
    }
  }
}
