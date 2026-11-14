package dev.efnilite.ipp.util;

import dev.efnilite.ip.leaderboard.Score;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlusLogicTest {

    @Test
    void lowestFreeSlotStartsAtZero() {
        assertEquals(0, PlusLogic.lowestFreeSlot(Set.of()));
        assertEquals(3, PlusLogic.lowestFreeSlot(Set.of(0, 1, 2)));
    }

    @Test
    void lowestFreeSlotReusesGapsInsteadOfColliding() {
        // A=0, B=1, C=2; B leaves -> next joiner gets 1, not C's slot 2
        assertEquals(1, PlusLogic.lowestFreeSlot(List.of(0, 2)));
        assertEquals(0, PlusLogic.lowestFreeSlot(List.of(1, 2)));
    }

    @Test
    void barIntervalNeverZero() {
        assertEquals(1, PlusLogic.barInterval(0, 50));
        assertEquals(1, PlusLogic.barInterval(-5, 50));
        assertEquals(1, PlusLogic.barInterval(30, 50));
        assertEquals(1, PlusLogic.barInterval(50, 50));
        assertEquals(2, PlusLogic.barInterval(100, 50));
        assertEquals(2, PlusLogic.barInterval(75, 50));
        assertEquals(3, PlusLogic.barInterval(120, 50));
        assertEquals(1, PlusLogic.barInterval(10, 20));
        assertEquals(25, PlusLogic.barInterval(500, 20));
    }

    @Test
    void gradientIndexAlwaysInBoundsForAnyGoal() {
        int length = 50;
        for (int goal = 0; goal <= 1000; goal++) {
            int interval = PlusLogic.barInterval(goal, length);
            for (int i = 0; i < goal; i++) {
                int idx = PlusLogic.gradientIndex(i, interval, length);
                assertTrue(idx >= 0 && idx < length, "goal=%d i=%d idx=%d".formatted(goal, i, idx));
            }
        }
    }

    @Test
    void gradientIndexMatchesOldBehaviourForDefaultGoal() {
        int interval = PlusLogic.barInterval(100, 50);
        for (int i = 0; i < 100; i++) {
            assertEquals(i / 2, PlusLogic.gradientIndex(i, interval, 50));
        }
    }

    @Test
    void betterTimeOnlyWhenFaster() {
        Score previous = new Score("p", "01:00:000", "0.5", 100);

        assertTrue(PlusLogic.isBetterTime("00:59:999", previous));
        assertFalse(PlusLogic.isBetterTime("01:00:000", previous));
        assertFalse(PlusLogic.isBetterTime("01:30:000", previous));
    }

    @Test
    void betterTimeWhenNoOrMalformedPrevious() {
        assertTrue(PlusLogic.isBetterTime("05:00:000", null));
        assertTrue(PlusLogic.isBetterTime("05:00:000", new Score("?", "?", "?", 0)));
        assertFalse(PlusLogic.isBetterTime("garbage", new Score("p", "01:00:000", "0.5", 100)));
    }

    @Test
    void rewritesOnlyExactParkourSubcommands() {
        assertEquals("/ipp create", PlusLogic.rewriteParkourCommand("/parkour create"));
        assertEquals("/ipp lobbies", PlusLogic.rewriteParkourCommand("/PARKOUR Lobbies"));
        assertEquals("/ipp invite Steve", PlusLogic.rewriteParkourCommand("/parkour invite Steve"));
    }

    @Test
    void doesNotRewriteOtherCommands() {
        assertNull(PlusLogic.rewriteParkourCommand("/parkour join"));
        assertNull(PlusLogic.rewriteParkourCommand("/parkour"));
        assertNull(PlusLogic.rewriteParkourCommand("/msg Bob parkour create"));
        assertNull(PlusLogic.rewriteParkourCommand("/parkourx create"));
        assertNull(PlusLogic.rewriteParkourCommand("parkour create"));
        assertNull(PlusLogic.rewriteParkourCommand("/parkour createx"));
    }
}
