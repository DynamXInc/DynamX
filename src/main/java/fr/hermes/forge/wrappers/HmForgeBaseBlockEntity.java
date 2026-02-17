package fr.hermes.forge.wrappers;

import fr.hermes.api.mc.blocks.HmBlockEntity;
import fr.hermes.api.mc.blocks.HmBlockEntityFactory;
import fr.hermes.api.mc.blocks.HmBlockEntityLogic;
import fr.hermes.api.mc.blocks.HmModBlockEntity;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

// TODO REGISTER, HOW TO HANDLE RENDERING, ETC
public class HmForgeBaseBlockEntity extends TileEntity implements HmModBlockEntity, ITickable {
    @Getter
    private HmBlockEntityLogic logic;

    public HmForgeBaseBlockEntity() {
        super();
    }

    public HmForgeBaseBlockEntity(HmBlockEntityFactory blockEntityFactory) {
        super();
        logic = blockEntityFactory.createEntityLogic((HmBlockEntity) this);
    }

    @Override
    public void update() {
        logic.update();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        // TODO logic instantiation
        logic.readFromNbt(compound);
        super.readFromNBT(compound);
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        // TODO logic save
        logic.writeToNbt(compound);
        return super.writeToNBT(compound);
    }

    // TODO Aym: are you sure of this?
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        float distance = logic.getMaxRenderDistanceSquared();
        return distance >= 0 ? distance : super.getMaxRenderDistanceSquared();
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new SPacketUpdateTileEntity(pos, 0, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        this.readFromNBT(pkt.getNbtCompound());
        this.world.markBlockRangeForRenderUpdate(pos, pos);
    }

    @Override
    public void onLoad() {
        logic.onLoad();
    }
}
