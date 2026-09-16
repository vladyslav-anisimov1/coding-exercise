package com.example.scoreboard.domain;

public final class ScoreboardException extends RuntimeException {
  private final ErrorCode errorCode;

  public ScoreboardException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
