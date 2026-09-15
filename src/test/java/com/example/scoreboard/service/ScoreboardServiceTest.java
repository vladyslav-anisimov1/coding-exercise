package com.example.scoreboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.scoreboard.domain.ErrorCode;
import com.example.scoreboard.domain.Match;
import com.example.scoreboard.domain.ScoreboardException;
import com.example.scoreboard.persistence.InMemoryMatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScoreboardServiceTest {
  private ScoreboardService scoreboard;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    scoreboard = new ScoreboardService(new InMemoryMatchRepository(), clock);
  }

  @Test
  void startsMatchWithNormalizedTeamsAndZeroScore() {
    Match match = scoreboard.startMatch(" spain ", "bRAZIL");

    assertEquals("Spain", match.getHomeTeam());
    assertEquals("BRAZIL", match.getAwayTeam());
    assertEquals(0, match.getTotalScore());
    assertFalse(match.isFinished());
  }

  @Test
  void rejectsSameTeamAndDuplicateActiveTeamCaseInsensitively() {
    assertCode(ErrorCode.SAME_TEAM, () -> scoreboard.startMatch("Spain", " spain "));
    scoreboard.startMatch("Spain", "Brazil");

    assertCode(ErrorCode.DUPLICATE_ACTIVE_TEAM, () -> scoreboard.startMatch("GERMANY", "spain"));
  }

  @Test
  void replacesScoreAndOrdersSummaryByTotalThenStartOrder() {
    Match first = scoreboard.startMatch("Mexico", "Canada");
    Match second = scoreboard.startMatch("Spain", "Brazil");
    Match third = scoreboard.startMatch("Germany", "France");
    scoreboard.updateScore(first.getMatchId(), 0, 5);
    scoreboard.updateScore(second.getMatchId(), 10, 2);
    scoreboard.updateScore(third.getMatchId(), 6, 6);

    List<Match> summary = scoreboard.getSummary();

    assertEquals(List.of(third.getMatchId(), second.getMatchId(), first.getMatchId()),
        summary.stream().map(Match::getMatchId).toList());
    Match reduced = scoreboard.updateScore(second.getMatchId(), 1, 0);
    assertEquals(1, reduced.getTotalScore());
  }

  @Test
  void finishesAndLooksUpMatchUntilHistoryEvictsIt() {
    Match match = scoreboard.startMatch("Spain", "Brazil");
    scoreboard.updateScore(match.getMatchId(), 2, 1);
    Match finished = scoreboard.finishMatch(match.getMatchId());

    assertTrue(finished.isFinished());
    assertTrue(scoreboard.getSummary().isEmpty());
    assertEquals(finished, scoreboard.getMatch(match.getMatchId()));
    assertCode(ErrorCode.MATCH_ALREADY_FINISHED, () -> scoreboard.updateScore(match.getMatchId(), 3, 2));
    assertCode(ErrorCode.MATCH_ALREADY_FINISHED, () -> scoreboard.finishMatch(match.getMatchId()));

    for (int i = 0; i < 20; i++) {
      Match other = scoreboard.startMatch("Home" + i, "Away" + i);
      scoreboard.finishMatch(other.getMatchId());
    }
    assertCode(ErrorCode.MATCH_NOT_FOUND, () -> scoreboard.getMatch(match.getMatchId()));
  }

  @Test
  void rejectsNegativeScoresAndUnknownMatches() {
    Match match = scoreboard.startMatch("Spain", "Brazil");

    assertCode(ErrorCode.INVALID_SCORE, () -> scoreboard.updateScore(match.getMatchId(), -1, 0));
    assertCode(ErrorCode.MATCH_NOT_FOUND, () -> scoreboard.getMatch(999));
  }

  private static void assertCode(ErrorCode expected, Runnable operation) {
    ScoreboardException exception = assertThrows(ScoreboardException.class, operation::run);
    assertEquals(expected, exception.getErrorCode());
  }
}
