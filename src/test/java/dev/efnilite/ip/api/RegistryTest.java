package dev.efnilite.ip.api;

import dev.efnilite.ip.style.Style;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryTest {

    private static Style style(String name) {
        return new Style() {
            @Override
            public Material getNext() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    @Test
    void indexOfStyleFindsExistingName() {
        List<Style> styles = new ArrayList<>(List.of(style("red"), style("blue")));

        assertEquals(0, Registry.indexOfStyle(styles, "red"));
        assertEquals(1, Registry.indexOfStyle(styles, "blue"));
    }

    @Test
    void indexOfStyleReturnsMinusOneWhenMissing() {
        List<Style> styles = new ArrayList<>(List.of(style("red")));

        assertEquals(-1, Registry.indexOfStyle(styles, "green"));
        assertEquals(-1, Registry.indexOfStyle(new ArrayList<>(), "red"));
    }
}
