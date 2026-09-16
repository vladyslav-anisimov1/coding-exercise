package com.example.scoreboard.service;

import com.example.scoreboard.domain.ErrorCode;
import com.example.scoreboard.domain.Match;
import com.example.scoreboard.domain.ScoreboardException;
import com.example.scoreboard.persistence.MatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class ScoreboardService {
  private static final Comparator<Match> SUMMARY_ORDER =
      Comparator.comparingInt(Match::getTotalScore)
          .reversed()
          .thenComparing(Comparator.comparingLong(Match::getStartOrder).reversed());

  private final MatchRepository matchRepository;
  private final Clock clock;
  private final AtomicLong sequence = new AtomicLong();

  public ScoreboardService(MatchRepository matchRepository) {
    this(matchRepository, Clock.systemUTC());
  }

  ScoreboardService(MatchRepository matchRepository, Clock clock) {
    this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Match startMatch(String homeTeam, String awayTeam) {
    String normalizedHomeTeam = TeamNameNormalizer.normalize(homeTeam);
    String normalizedAwayTeam = TeamNameNormalizer.normalize(awayTeam);
    if (TeamNameNormalizer.equalIgnoringCase(normalizedHomeTeam, normalizedAwayTeam)) {
      throw new ScoreboardException(
          ErrorCode.SAME_TEAM, "A match requires two different teams");
    }
    long matchId = sequence.incrementAndGet();
    Instant startedAt = clock.instant();
    return matchRepository.create(
        matchId, normalizedHomeTeam, normalizedAwayTeam, startedAt, matchId);
  }

  public Match updateScore(long matchId, int homeScore, int awayScore) {
    return matchRepository.updateScore(matchId, homeScore, awayScore);
  }

  public Match finishMatch(long matchId) {
    return matchRepository.finish(matchId);
  }

  public List<Match> getSummary() {
    return matchRepository.findActiveMatches().stream().sorted(SUMMARY_ORDER).toList();
  }

  public Match getMatch(long matchId) {
    return matchRepository
        .findAny(matchId)
        .orElseThrow(
            () ->
                new ScoreboardException(
                    ErrorCode.MATCH_NOT_FOUND, "Match was not found: " + matchId));
  }

}
