package com.example.scoreboard.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.scoreboard.domain.Match;
import com.example.scoreboard.service.ScoreboardService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InMemoryMatchRepositoryConcurrencyTest {
  @Test
  void serializesConcurrentUpdatesToTheSameMatch() throws Exception {
    ScoreboardService scoreboard = new ScoreboardService(new InMemoryMatchRepository());
    Match match = scoreboard.startMatch("Spain", "Brazil");
    int updates = 16;
    CountDownLatch ready = new CountDownLatch(updates);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(updates);
    List<java.util.concurrent.Future<Match>> results = new ArrayList<>();
    try {
      for (int i = 0; i < updates; i++) {
        int score = i;
        results.add(executor.submit(() -> {
          ready.countDown();
          start.await();
          return scoreboard.updateScore(match.getMatchId(), score, score + 1);
        }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      for (var result : results) {
        Match updated = result.get(5, TimeUnit.SECONDS);
        assertTrue(updated.getHomeScore() >= 0);
        assertTrue(updated.getAwayScore() >= 0);
      }
    } finally {
      executor.shutdownNow();
    }
    Match finalMatch = scoreboard.getSummary().stream()
        .filter(candidate -> candidate.getMatchId() == match.getMatchId())
        .findFirst()
        .orElseThrow();
    assertTrue(finalMatch.getHomeScore() >= 0);
    assertEquals(finalMatch.getHomeScore() + 1, finalMatch.getAwayScore());
  }

  @Test
  void allowsDifferentMatchesToBeUpdatedConcurrently() throws Exception {
    ScoreboardService scoreboard = new ScoreboardService(new InMemoryMatchRepository());
    Match first = scoreboard.startMatch("Spain", "Brazil");
    Match second = scoreboard.startMatch("Germany", "France");
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      var firstUpdate = executor.submit(() -> scoreboard.updateScore(first.getMatchId(), 3, 2));
      var secondUpdate = executor.submit(() -> scoreboard.updateScore(second.getMatchId(), 4, 1));
      assertEquals(5, firstUpdate.get(5, TimeUnit.SECONDS).getTotalScore());
      assertEquals(5, secondUpdate.get(5, TimeUnit.SECONDS).getTotalScore());
    } finally {
      executor.shutdownNow();
    }
  }
}
