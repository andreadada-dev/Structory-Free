package me.mrbast.structory.structure.validator;

import me.mrbast.structory.config.MainConfig;
import me.mrbast.structory.config.SingleStructureInstanceConfig;
import me.mrbast.structory.event.AltarCreateEvent;
import me.mrbast.structory.manager.ListenerManager;
import me.mrbast.structory.manager.StructureInstanceManager;
import me.mrbast.structory.structure.Structure;
import me.mrbast.structory.structure.StructureInstance;
import me.mrbast.structory.structure.layout.checker.CheckResult;
import me.mrbast.structory.structure.layout.StructureLayout;
import me.mrbast.structory.util.MaterialUtil;
import me.mrbast.structory.util.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

public class StructureValidator {

    private final Structure structure;
    private Location center;
    private CheckResult checkResult;
    private Valid valid;

    public enum Valid{
        VALID, NOT_VALID, STRUCTURE_NEARBY, ERROR;
    }

    public StructureValidator(Structure structure, Location location) {
        this.center = location;
        this.structure = structure;
    }

    public static Valid checkNearby(Location center){
        final double distance = MainConfig.getInstance().distance;

        Collection<StructureInstance> filtered = StructureInstanceManager.getInstance().getFiltered(instance -> {
            if (!Objects.equals(instance.getData().getCenter().getWorld(), center.getWorld())) return false;
            return (instance.getData().getCenter().distance(center) < distance)
                    && MaterialUtil.areSame(instance.getData().getStructure().getData().getCheckBlock(), center.getBlock().getType());
        });

        if(!filtered.isEmpty()) return Valid.STRUCTURE_NEARBY;
        return Valid.VALID;
    }

    public StructureValidator check(){
        this.checkResult = structure.getLayout().check(center, structure.getData().hasOrientation());
        this.valid = checkResult.isValid() ? Valid.VALID : Valid.NOT_VALID;
        return this;
    }

    public boolean isValid(){
        return this.valid == Valid.VALID;
    }

    public void execute(Consumer<StructureInstance> action){
        if(isValid()) action.accept(structure.createNewStructureInstance().prepare(center, checkResult.getOrientation()));
    }

    public void executeDefault(PlayerInteractEvent event) {
        if(!isValid()) return;

        StructureInstance inst = structure.createNewStructureInstance().prepare(center, checkResult.getOrientation());
        ListenerManager.getInstance().call(new AltarCreateEvent(event, inst));
        StructureInstanceManager.getInstance().register(inst);
        inst.init();

        // YAML serialization/disk I/O must never occupy a Folia region thread.
        SchedulerUtil.async(() -> new SingleStructureInstanceConfig(inst).save());
    }

    public void ifValidOrElse(Consumer<StructureLayout> action, Consumer<Valid> orElse){
        check();
        if(isValid()) {
            action.accept(structure.getLayout());
            return;
        }
        orElse.accept(this.valid);
    }
}
