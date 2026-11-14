package dev.efnilite.ip.leaderboard;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    private static Leaderboard leaderboard() {
        return new Leaderboard("Default", Leaderboard.Sort.SCORE, false);
    }

    private static Score score(String name, int score) {
        return new Score(name, "00:10:000", "0.5", score);
    }

    @Test
    void putKeepsScoresSorted() {
        Leaderboard leaderboard = leaderboard();

        leaderboard.put(A, score("a", 5));
        leaderboard.put(B, score("b", 10));
        leaderboard.put(C, score("c", 1));

        assertEquals(List.of(B, A, C), new ArrayList<>(leaderboard.getScores().keySet()));
        assertEquals(1, leaderboard.getRank(B));
        assertEquals("c", Objects.requireNonNull(leaderboard.getScoreAtRank(3)).name());
        assertNull(leaderboard.getScoreAtRank(0));
        assertNull(leaderboard.getScoreAtRank(4));
        assertEquals("default", leaderboard.mode);
    }

    @Test
    void snapshotsAreNotAffectedByLaterChanges() {
        Leaderboard leaderboard = leaderboard();
        leaderboard.put(A, score("a", 5));

        Map<UUID, Score> snapshot = leaderboard.getScores();
        leaderboard.put(B, score("b", 10));
        leaderboard.remove(A);

        assertEquals(Set.of(A), snapshot.keySet());
        assertEquals(Set.of(B), leaderboard.getScores().keySet());
        assertThrows(UnsupportedOperationException.class, () -> leaderboard.getScores().clear());
    }

    @Test
    void readKeepsUnsavedLocalChanges() {
        Leaderboard leaderboard = leaderboard();

        // a score set while the (async) startup read is still running
        leaderboard.put(A, score("a", 50));
        // a reset that has not been written yet
        leaderboard.remove(B);

        Map<UUID, Score> loaded = new HashMap<>();
        loaded.put(A, score("a", 3));
        loaded.put(B, score("b", 7));
        loaded.put(C, score("c", 9));
        leaderboard.applyRead(loaded);

        assertEquals(50, leaderboard.get(A).score());
        assertFalse(leaderboard.getScores().containsKey(B));
        assertEquals(List.of(A, C), new ArrayList<>(leaderboard.getScores().keySet()));
    }

    @Test
    void readReplacesWrittenData() {
        Leaderboard leaderboard = leaderboard();
        leaderboard.put(A, score("a", 50));
        leaderboard.markWritten(leaderboard.snapshot());

        leaderboard.applyRead(Map.of(B, score("b", 1)));

        assertEquals(Set.of(B), leaderboard.getScores().keySet());
    }

    @Test
    void writeSnapshotContainsRemovals() {
        Leaderboard leaderboard = leaderboard();
        leaderboard.put(A, score("a", 5));
        leaderboard.put(B, score("b", 6));
        leaderboard.resetAll();

        Leaderboard.WriteSnapshot snapshot = leaderboard.snapshot();

        assertTrue(snapshot.scores().isEmpty());
        assertEquals(Set.of(A, B), snapshot.removed());

        // a change after the snapshot was taken must survive markWritten
        leaderboard.put(A, score("a", 8));
        leaderboard.markWritten(snapshot);

        Leaderboard.WriteSnapshot next = leaderboard.snapshot();
        assertTrue(next.removed().isEmpty());
        assertEquals(Set.of(A), next.unsaved().keySet());
    }

    @Test
    void failedWriteKeepsPendingRemovals() {
        Leaderboard leaderboard = leaderboard();
        leaderboard.remove(A);

        leaderboard.snapshot(); // write failed, markWritten not called

        assertEquals(Set.of(A), leaderboard.snapshot().removed());
    }

    @Test
    void sortsByTimeAndDifficulty() {
        Leaderboard leaderboard = new Leaderboard("time_trial", Leaderboard.Sort.TIME, false);
        leaderboard.put(A, new Score("a", "61:00:000", "0.2", 1));
        leaderboard.put(B, new Score("b", "00:30:000", "1.0", 1));
        leaderboard.put(C, new Score("c", "?", "?", 1));

        assertEquals(List.of(B, A, C), new ArrayList<>(leaderboard.getScores().keySet()));
        assertEquals(List.of(B, A, C), new ArrayList<>(leaderboard.sort(Leaderboard.Sort.DIFFICULTY).keySet()));
    }
}
