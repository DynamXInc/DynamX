package fr.dynamx.common.core.mixin;

import fr.dynamx.common.DynamXContext;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Patches the NetHandlerPlayServer to disable console spam when players walk on moving vehicles
 */
@Mixin(value = NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {
    @Shadow
    private static boolean isMovePlayerPacketInvalid(CPacketPlayer packetIn) {
        throw new IllegalStateException("Mixin failed to shadow isMovePlayerPacketInvali()");
    }

    @Final
    @Shadow
    private static Logger LOGGER;

    @Final
    @Shadow
    private MinecraftServer server;
    @Shadow
    public EntityPlayerMP player;
    @Shadow
    private int networkTickCount;
    @Shadow
    private Vec3d targetPos;
    @Shadow
    private int lastPositionUpdate;

    @Shadow
    private double firstGoodX;
    @Shadow
    private double firstGoodY;
    @Shadow
    private double firstGoodZ;

    @Shadow
    private double lastGoodX;
    @Shadow
    private double lastGoodY;
    @Shadow
    private double lastGoodZ;

    @Shadow
    private int movePacketCounter;
    @Shadow
    private int lastMovePacketCounter;

    @Shadow
    private boolean floating;

    @Shadow
    public void disconnect(final ITextComponent textComponent) {
        throw new IllegalStateException("Mixin failed to shadow disconnect()");
    }

    @Shadow
    private void captureCurrentPosition() {
        throw new IllegalStateException("Mixin failed to shadow captureCurrentPosition()");
    }

    @Shadow
    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        throw new IllegalStateException("Mixin failed to shadow setPlayerLocation()");
    }

    /**
     * @author Aym'
     * @reason
     */
    @Overwrite
    public void processPlayer(CPacketPlayer packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (NetHandlerPlayServer) (Object) this, this.player.getServerWorld());

        if (isMovePlayerPacketInvalid(packetIn)) {
            this.disconnect(new TextComponentTranslation("multiplayer.disconnect.invalid_player_movement"));
        } else {
            WorldServer worldserver = this.server.getWorld(this.player.dimension);

            if (!this.player.queuedEndExit) {
                if (this.networkTickCount == 0) {
                    this.captureCurrentPosition();
                }

                if (this.targetPos != null) {
                    if (this.networkTickCount - this.lastPositionUpdate > 20) {
                        this.lastPositionUpdate = this.networkTickCount;
                        this.setPlayerLocation(this.targetPos.x, this.targetPos.y, this.targetPos.z, this.player.rotationYaw, this.player.rotationPitch);
                    }
                } else {
                    this.lastPositionUpdate = this.networkTickCount;

                    if (this.player.isRiding()) {
                        this.player.setPositionAndRotation(this.player.posX, this.player.posY, this.player.posZ, packetIn.getYaw(this.player.rotationYaw), packetIn.getPitch(this.player.rotationPitch));
                        this.server.getPlayerList().serverUpdateMovingPlayer(this.player);
                    } else {
                        double d0 = this.player.posX;
                        double d1 = this.player.posY;
                        double d2 = this.player.posZ;
                        double d3 = this.player.posY;
                        double d4 = packetIn.getX(this.player.posX);
                        double d5 = packetIn.getY(this.player.posY);
                        double d6 = packetIn.getZ(this.player.posZ);
                        float f = packetIn.getYaw(this.player.rotationYaw);
                        float f1 = packetIn.getPitch(this.player.rotationPitch);
                        double d7 = d4 - this.firstGoodX;
                        double d8 = d5 - this.firstGoodY;
                        double d9 = d6 - this.firstGoodZ;
                        double d10 = this.player.motionX * this.player.motionX + this.player.motionY * this.player.motionY + this.player.motionZ * this.player.motionZ;
                        double d11 = d7 * d7 + d8 * d8 + d9 * d9;

                        if (this.player.isPlayerSleeping()) {
                            if (d11 > 1.0D) {
                                this.setPlayerLocation(this.player.posX, this.player.posY, this.player.posZ, packetIn.getYaw(this.player.rotationYaw), packetIn.getPitch(this.player.rotationPitch));
                            }
                        } else {
                            ++this.movePacketCounter;
                            int i = this.movePacketCounter - this.lastMovePacketCounter;

                            if (i > 5) {
                                LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName(), Integer.valueOf(i));
                                i = 1;
                            }

                            if (!this.player.isInvulnerableDimensionChange() && (!this.player.getServerWorld().getGameRules().getBoolean("disableElytraMovementCheck") || !this.player.isElytraFlying())) {
                                float f2 = this.player.isElytraFlying() ? 300.0F : 100.0F;

                                if (d11 - d10 > (double) (f2 * (float) i) && (!this.server.isSinglePlayer() || !this.server.getServerOwner().equals(this.player.getName()))) {
                                    if (!DynamXContext.getWalkingPlayers().containsKey(player)) {
                                        LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName(), Double.valueOf(d7), Double.valueOf(d8), Double.valueOf(d9));
                                        this.setPlayerLocation(this.player.posX, this.player.posY, this.player.posZ, this.player.rotationYaw, this.player.rotationPitch);
                                        return;
                                    } else {
                                        //TODO IMPROVE SECURITY
                                        LOGGER.warn("Ignoring moving too quickly from " + player.getName() + " : walking on vehicle !");
                                    }
                                }
                            }

                            boolean flag2 = worldserver.getCollisionBoxes(this.player, this.player.getEntityBoundingBox().shrink(0.0625D)).isEmpty();
                            d7 = d4 - this.lastGoodX;
                            d8 = d5 - this.lastGoodY;
                            d9 = d6 - this.lastGoodZ;

                            if (this.player.onGround && !packetIn.isOnGround() && d8 > 0.0D) {
                                this.player.jump();
                            }

                            this.player.move(MoverType.PLAYER, d7, d8, d9);
                            this.player.onGround = packetIn.isOnGround();
                            double d12 = d8;
                            d7 = d4 - this.player.posX;
                            d8 = d5 - this.player.posY;

                            if (d8 > -0.5D || d8 < 0.5D) {
                                d8 = 0.0D;
                            }

                            d9 = d6 - this.player.posZ;
                            d11 = d7 * d7 + d8 * d8 + d9 * d9;
                            boolean flag = false;

                            if (!this.player.isInvulnerableDimensionChange() && d11 > 0.0625D && !this.player.isPlayerSleeping() && !this.player.interactionManager.isCreative() && this.player.interactionManager.getGameType() != GameType.SPECTATOR) {
                                if (!DynamXContext.getWalkingPlayers().containsKey(player)) {
                                    flag = true;
                                    LOGGER.warn("{} moved wrongly!", this.player.getName());
                                } else {
                                    //TODO IMPROVE
                                    LOGGER.warn("Ignoring moving wrongly from " + player.getName() + " : walking on vehicle !");
                                }
                            }

                            this.player.setPositionAndRotation(d4, d5, d6, f, f1);
                            this.player.addMovementStat(this.player.posX - d0, this.player.posY - d1, this.player.posZ - d2);

                            if (!this.player.noClip && !this.player.isPlayerSleeping()) {
                                boolean flag1 = worldserver.getCollisionBoxes(this.player, this.player.getEntityBoundingBox().shrink(0.0625D)).isEmpty();

                                if (flag2 && (flag || !flag1)) {
                                    this.setPlayerLocation(d0, d1, d2, f, f1);
                                    return;
                                }
                            }

                            this.floating = d12 >= -0.03125D;
                            this.floating &= !this.server.isFlightAllowed() && !this.player.capabilities.allowFlying;
                            this.floating &= !this.player.isPotionActive(MobEffects.LEVITATION) && !this.player.isElytraFlying() && !worldserver.checkBlockCollision(this.player.getEntityBoundingBox().grow(0.0625D).expand(0.0D, -0.55D, 0.0D));
                            this.player.onGround = packetIn.isOnGround();
                            this.server.getPlayerList().serverUpdateMovingPlayer(this.player);
                            this.player.handleFalling(this.player.posY - d3, packetIn.isOnGround());
                            this.lastGoodX = this.player.posX;
                            this.lastGoodY = this.player.posY;
                            this.lastGoodZ = this.player.posZ;
                        }
                    }
                }
            }
        }
    }
}
