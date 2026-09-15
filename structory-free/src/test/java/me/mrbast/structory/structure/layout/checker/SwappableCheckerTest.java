package me.mrbast.structory.structure.layout.checker;

import me.mrbast.structory.structure.orientation.LevelOrientation;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SwappableCheckerTest {

    @Test
    void orientedCheckAcceptsLegacySwappedOffset() {
        BlockChecker acceptsSwappedPosition = new BlockChecker() {
            @Override
            public boolean isValid(Location location) {
                return location.getBlockX() == -1 && location.getBlockZ() == -2;
            }
        };

        SwappableChecker checker = new SwappableChecker(2, 1, acceptsSwappedPosition);

        assertTrue(checker.check(new Location(null, 0, 64, 0), LevelOrientation.EAST));
    }
}
