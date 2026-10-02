package dev.efnilite.ip.foundation.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionTest {

    @ParameterizedTest
    @CsvSource({
            "1.16, V1_16",
            "1.20.4, V1_20",
            "1.20.5, V1_20_5",
            "1.21.11, V1_21",
            "26.1.2, V1_22",
            "26.2, V1_22",
            "26.3, V1_22",
            "26.3-rc-3, V1_22",
            "26.3.build.142-beta, V1_22",
            "1.15.2, V1_16",
            "unknown, V1_22"
    })
    void resolvesFeatureGatesAcrossVersionFormats(String minecraftVersion, Version expected) {
        assertEquals(expected, Version.resolve(minecraftVersion));
    }
}
