package com.example.scoreboard.domain;

import java.time.Instant;
import java.util.Objects;

public final class Match {
  private final long matchId;
  private final String homeTeam;
  private final String awayTeam;
  private final int homeScore;
  private final int awayScore;
  private final Instant startedAt;
  private final long startOrder;
  private final boolean finished;

  public Match(
      long matchId,
      String homeTeam,
      String awayTeam,
      int homeScore,
      int awayScore,
      Instant startedAt,
      long startOrder,
      boolean finished) {
    if (matchId <= 0 || startOrder <= 0 || startedAt == null) {
      throw new IllegalArgumentException("Match identity and start time must be valid");
    }
    if (homeTeam == null || homeTeam.isBlank() || awayTeam == null || awayTeam.isBlank()) {
      throw new ScoreboardException(ErrorCode.INVALID_TEAM_NAME, "Team names must not be blank");
    }
    if (homeTeam.equalsIgnoreCase(awayTeam)) {
      throw new ScoreboardException(ErrorCode.SAME_TEAM, "A match requires two different teams");
    }
    if (homeScore < 0 || awayScore < 0) {
      throw new ScoreboardException(ErrorCode.INVALID_SCORE, "Scores must not be negative");
    }
    this.matchId = matchId;
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.homeScore = homeScore;
    this.awayScore = awayScore;
    this.startedAt = startedAt;
    this.startOrder = startOrder;
    this.finished = finished;
  }

  public long getMatchId() {
    return matchId;
  }

  public String getHomeTeam() {
    return homeTeam;
  }

  public String getAwayTeam() {
    return awayTeam;
  }

  public int getHomeScore() {
    return homeScore;
  }

  public int getAwayScore() {
    return awayScore;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public long getStartOrder() {
    return startOrder;
  }

  public boolean isFinished() {
    return finished;
  }

  public int getTotalScore() {
    return homeScore + awayScore;
  }

  public Match withScore(int newHomeScore, int newAwayScore) {
    return new Match(
        matchId,
        homeTeam,
        awayTeam,
        newHomeScore,
        newAwayScore,
        startedAt,
        startOrder,
        finished);
  }

  public Match finish() {
    return new Match(
        matchId, homeTeam, awayTeam, homeScore, awayScore, startedAt, startOrder, true);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Match match)) {
      return false;
    }
    return matchId == match.matchId
        && homeScore == match.homeScore
        && awayScore == match.awayScore
        && startOrder == match.startOrder
        && finished == match.finished
        && homeTeam.equals(match.homeTeam)
        && awayTeam.equals(match.awayTeam)
        && startedAt.equals(match.startedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        matchId, homeTeam, awayTeam, homeScore, awayScore, startedAt, startOrder, finished);
  }
}
