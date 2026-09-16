package me.mrbast.structory.async;

import me.mrbast.platform.scheduler.PlatformTask;
import me.mrbast.structory.particle.AltarParticle;
import me.mrbast.structory.structure.Structure;
import me.mrbast.structory.util.SchedulerUtil;

public class StructureParticleScheduler {

    private static final StructureParticleScheduler instance = new StructureParticleScheduler();

    public static StructureParticleScheduler getInstance() {
        return instance;
    }

    private final StructureParticleRunnable runnable = new StructureParticleRunnable();
    private PlatformTask task;

    private StructureParticleScheduler() {
    }

    public void add(Structure structure, AltarParticle altarParticle) {
        runnable.addObserver(structure, altarParticle);
    }

    public synchronized void start() {
        if (task != null && !task.isCancelled()) return;
        task = SchedulerUtil.globalRepeating(runnable, 20L, 20L);
    }

    public synchronized void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    public void clear() {
        runnable.clear();
    }

    public StructureParticleRunnable getRunnable() {
        return runnable;
    }
}
