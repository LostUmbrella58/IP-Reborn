package dev.efnilite.ip.reward;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RewardsTest {

    private static final List<String> MODES = List.of("default", "speed", "team_survival", "duels");

    @Test
    void defaultModeKeepsLegacyKey() {
        assertEquals("10", Rewards.oneTimeKey("default", "default", 10));
    }

    @Test
    void otherModesHaveTheirOwnKey() {
        assertEquals("team_survival:1", Rewards.oneTimeKey("team_survival", "default", 1));
        assertNotEquals(Rewards.oneTimeKey("default", "default", 1), Rewards.oneTimeKey("duels", "default", 1));
    }

    @Test
    void rewardsWithoutPrefixTargetDefaultMode() {
        assertEquals("default", Reward.targetMode("send:You made your first jump!", MODES, "default"));
        assertEquals("default", Reward.targetMode("give %player% diamond", MODES, "default"));
    }

    @Test
    void prefixedRewardsTargetTheirMode() {
        assertEquals("team_survival", Reward.targetMode("team_survival:send:<gray>Be sure to invite your friends!", MODES, "default"));
        assertEquals("speed", Reward.targetMode("speed:leave:send:bye", MODES, "default"));
    }
}
