package fr.dynamx.api.network.sync;

public abstract class SynchronizationRules
{
    public static final SynchronizationRules SERVER_TO_CLIENTS = new ServerToClients();
    public static final SynchronizationRules PHYSICS_TO_SPECTATORS = new PhysicsToSpectators();
    public static final SynchronizationRules CONTROLS_TO_SPECTATORS = new ControlsToSpectators();

    public abstract SyncTarget getSyncTarget(SimulationHolder simulationHolder, boolean isClientSide);

    public abstract boolean listensSide(SimulationHolder simulationHolder, boolean fromClientSide);

    private static class ServerToClients extends SynchronizationRules
    {
        @Override
        public SyncTarget getSyncTarget(SimulationHolder simulationHolder, boolean fromClientSide) {
            return fromClientSide ? SyncTarget.NONE : SyncTarget.ALL_CLIENTS;
        }

        @Override
        public boolean listensSide(SimulationHolder simulationHolder, boolean fromClientSide) {
            return !fromClientSide;
        }

        @Override
        public String toString() {
            return "StC";
        }
    }

    private static class PhysicsToSpectators extends SynchronizationRules
    {
        @Override
        public SyncTarget getSyncTarget(SimulationHolder simulationHolder, boolean fromClientSide) {
            return simulationHolder.isPhysicsAuthority(fromClientSide) ? !fromClientSide ? SyncTarget.SPECTATORS : SyncTarget.SERVER : SyncTarget.NONE;
        }

        @Override
        public boolean listensSide(SimulationHolder simulationHolder, boolean fromClientSide) {
            return simulationHolder.isPhysicsAuthority(fromClientSide);
        }

        @Override
        public String toString() {
            return "PtS";
        }
    }

    private static class ControlsToSpectators extends SynchronizationRules
    {
        @Override
        public SyncTarget getSyncTarget(SimulationHolder simulationHolder, boolean fromClientSide) {
            return simulationHolder.ownsControls(fromClientSide) ? !fromClientSide ? SyncTarget.SPECTATORS : SyncTarget.SERVER : SyncTarget.NONE;
        }

        @Override
        public boolean listensSide(SimulationHolder simulationHolder, boolean fromClientSide) {
            return simulationHolder.ownsControls(fromClientSide);
        }

        @Override
        public String toString() {
            return "CtS";
        }
    }
}
