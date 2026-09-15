package com.example.scoreboard.persistence;

import com.example.scoreboard.domain.Match;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MatchRepository {
  Match create(
      long matchId, String homeTeam, String awayTeam, Instant startedAt, long startOrder);

  Match updateScore(long matchId, int homeScore, int awayScore);

  Match finish(long matchId);

  Optional<Match> findActive(long matchId);

  Optional<Match> findAny(long matchId);

  List<Match> findActiveMatches();
}
