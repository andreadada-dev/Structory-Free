package me.mrbast.structory.structure.layout.checker;

import me.mrbast.structory.structure.orientation.LevelOrientation;
import me.mrbast.structory.structure.orientation.Orientation;
import org.bukkit.Location;

public class SwappableChecker extends Checker {

    public SwappableChecker(int xOffset, int zOffset, BlockChecker check) {
        super(xOffset, zOffset, check);
    }

    @Override
    public boolean check(Location center) {
        return check(center, LevelOrientation.EAST);
    }

    @Override
    public boolean check(Location center, LevelOrientation levelOrientation) {
        Orientation primary = levelOrientation.from(new Orientation(xOffset, zOffset, LevelOrientation.EAST));
        if (check.isValid(center.clone().add(primary.getX().doubleValue(), 0, primary.getZ().doubleValue()))) {
            return true;
        }

        // Preserve the legacy swappable transform: (x, z) -> (-z, -x),
        // then rotate the transformed offset together with the whole structure.
        Orientation swapped = levelOrientation.from(new Orientation(-zOffset, -xOffset, LevelOrientation.EAST));
        return check.isValid(center.clone().add(swapped.getX().doubleValue(), 0, swapped.getZ().doubleValue()));
    }
}
