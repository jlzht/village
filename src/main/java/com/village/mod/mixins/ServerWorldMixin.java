package com.village.mod.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.MinecraftServer;
import com.village.mod.accessor.SettlementManagerAccessor;
import java.util.concurrent.Executor;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.storage.LevelStorage;
import com.village.mod.world.SettlementManager;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.world.spawner.SpecialSpawner;
import net.minecraft.util.math.random.RandomSequencesState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements SettlementManagerAccessor {
    @Unique
    private long ticksUntilSettlementsUpdate = 100;
    @Unique private SettlementManager settlementManager;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(
        MinecraftServer server,
        Executor workerExecutor,
        LevelStorage.Session session,
        ServerWorldProperties properties,
        RegistryKey<World> worldKey,
        DimensionOptions dimensionOptions,
        WorldGenerationProgressListener worldGenerationProgressListener,
        boolean debugWorld,
        long seed,
        List<SpecialSpawner> spawners,
        boolean shouldTickTime,
        @Nullable RandomSequencesState randomSequencesState,
        CallbackInfo ci
    ) {
        settlementManager = ((ServerWorld)(Object)this).getPersistentStateManager().getOrCreate(SettlementManager.getPersistentStateType(((ServerWorld)(Object)this)), "settlements");
        SettlementManager.Companion.setInstance(settlementManager);
        // Using SettlementManagerAccessor casting pisses me of, So I made a singletron for global access, but with that for any ServerWorld instantiated,
        // the SettlementManager will point to it, so when I try to get a block state on overworld at settlement manager ticking. I instead access the nether or end.
        // I intend to fix this my making settlements possible on others dimensions, using RegistryKey I guess.
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickSettlements(CallbackInfo ci) {
        if (--this.ticksUntilSettlementsUpdate == 0L) {
            settlementManager.tick();
            ticksUntilSettlementsUpdate = 100;
        }
    }
    @Override
    public SettlementManager getSettlementManager() {
        return settlementManager;
    }

    @Override
    public void setSettlementsUpdateInterval(long ticks) {
        this.ticksUntilSettlementsUpdate = ticks;

    }
}
