package fr.dynamx.api.network.sync;

import net.minecraftforge.fml.relauncher.Side;

public abstract class SynchronizationRules
{
    public static final SynchronizationRules SERVER_TO_CLIENTS = new ServerToClients();
    public static final SynchronizationRules PHYSICS_TO_SPECTATORS = new PhysicsToSpectators();
    public static final SynchronizationRules CONTROLS_TO_SPECTATORS = new ControlsToSpectators();

    public abstract SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side);

    public abstract boolean listensSide(SimulationHolder simulationHolder, Side side);

    private static class ServerToClients extends SynchronizationRules
    {
        @Override
        public SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side) {
            return side.isServer() ? SyncTarget.ALL_CLIENTS : SyncTarget.NONE;
        }

        @Override
        public boolean listensSide(SimulationHolder simulationHolder, Side side) {
            return side == Side.SERVER;
        }

        @Override
        public String toString() {
            return "StC";
        }
    }

    private static class PhysicsToSpectators extends SynchronizationRules
    {
        @Override
        public SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side) {
            return simulationHolder.isPhysicsAuthority(side) ? side.isServer() ? SyncTarget.SPECTATORS : SyncTarget.SERVER : SyncTarget.NONE;
        }

        @Override
        public boolean listensSide(SimulationHolder simulationHolder, Side side) {
            return simulationHolder.isPhysicsAuthority(side);
        }

        @Override
        public String toString() {
            return "PtS";
        }
    }

    private static class ControlsToSpectators extends SynchronizationRules
    {
        @Override
        public SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side) {
            return simulationHolder.ownsControls(side) ? side.isServer() ? SyncTarget.SPECTATORS : SyncTarget.SERVER : SyncTarget.NONE;
        }

        @Override
        public boolean listensSide(SimulationHolder simulationHolder, Side side) {
            return simulationHolder.ownsControls(side);
        }

        @Override
        public String toString() {
            return "CtS";
        }
    }
}
