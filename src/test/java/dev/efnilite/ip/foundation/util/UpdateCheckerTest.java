package dev.efnilite.ip.foundation.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateCheckerTest {

    @ParameterizedTest
    @CsvSource({
            // issue #4: the 6.0 release jar reported itself as 6.0.0-SNAPSHOT
            "6.0, 6.0.0-SNAPSHOT, false",
            "6.0.1, 6.0.1-26.3-SNAPSHOT, false",
            "6.0.1, 6.0.1, false",
            "6.0.1, 6.0, true",
            "6.1, 6.0.1-26.3-SNAPSHOT, true",
            "7.0, 6.9.9, true",
            "6.0.10, 6.0.9, true",
            "6.0, 6.0.1, false",
            "v6.1, 6.0.1, true",
            "Beta, 6.0.1, false",
            "6.1, dev, false"
    })
    void reportsOnlyStrictlyNewerReleases(String remote, String local, boolean expected) {
        assertEquals(expected, UpdateChecker.isNewer(remote, local));
    }
}
