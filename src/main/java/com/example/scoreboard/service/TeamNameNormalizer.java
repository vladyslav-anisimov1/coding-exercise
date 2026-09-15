package com.example.scoreboard.service;

import com.example.scoreboard.domain.ErrorCode;
import com.example.scoreboard.domain.ScoreboardException;
import java.util.Locale;

final class TeamNameNormalizer {
  private TeamNameNormalizer() {}

  static String normalize(String teamName) {
    if (teamName == null) {
      throw new ScoreboardException(ErrorCode.INVALID_TEAM_NAME, "Team name must not be null");
    }
    String trimmed = teamName.trim();
    if (trimmed.isEmpty()) {
      throw new ScoreboardException(ErrorCode.INVALID_TEAM_NAME, "Team name must not be blank");
    }
    int firstCodePoint = trimmed.codePointAt(0);
    int capitalizedCodePoint = Character.toUpperCase(firstCodePoint);
    return new StringBuilder()
        .appendCodePoint(capitalizedCodePoint)
        .append(trimmed.substring(Character.charCount(firstCodePoint)))
        .toString();
  }

  static boolean equalIgnoringCase(String first, String second) {
    return first.toLowerCase(Locale.ROOT).equals(second.toLowerCase(Locale.ROOT));
  }
}
