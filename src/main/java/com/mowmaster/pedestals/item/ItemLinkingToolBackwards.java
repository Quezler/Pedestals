package com.mowmaster.pedestals.item;

import com.google.common.collect.Maps;
import com.mowmaster.pedestals.blocks.PedestalBlock;
import com.mowmaster.pedestals.tiles.PedestalTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mowmaster.pedestals.references.Reference.MODID;
import static net.minecraft.state.properties.BlockStateProperties.FACING;

public class ItemLinkingToolBackwards extends ItemLinkingTool {

    public static final BlockPos defaultPos = new BlockPos(0,-2000,0);
    public BlockPos storedPosition = defaultPos;
    public List<BlockPos> storedPositionList = new ArrayList<>();

    public ItemLinkingToolBackwards() {
        super();
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return new ItemStack(this.getItem());
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World worldIn = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getPos();

        //new TranslationTextComponent(getTranslationKey() + ".tool_speed", tilePedestal.getSpeed()).mergeStyle(TextFormatting.RED)
        TranslationTextComponent linksucess = new TranslationTextComponent(getTranslationKey() + ".tool_link_success");
        linksucess.mergeStyle(TextFormatting.WHITE);
        TranslationTextComponent linkunsuccess = new TranslationTextComponent(getTranslationKey() + ".tool_link_unsucess");
        linkunsuccess.mergeStyle(TextFormatting.WHITE);
        TranslationTextComponent linkremoved = new TranslationTextComponent(getTranslationKey() + ".tool_link_removed");
        linkremoved.mergeStyle(TextFormatting.WHITE);
        TranslationTextComponent linkitsself = new TranslationTextComponent(getTranslationKey() + ".tool_link_itsself");
        linkitsself.mergeStyle(TextFormatting.WHITE);
        TranslationTextComponent linknetwork = new TranslationTextComponent(getTranslationKey() + ".tool_link_network");
        linknetwork.mergeStyle(TextFormatting.WHITE);
        TranslationTextComponent linkdistance = new TranslationTextComponent(getTranslationKey() + ".tool_link_distance");
        linkdistance.mergeStyle(TextFormatting.WHITE);

        if(!worldIn.isRemote)
        {
            BlockState getBlockState = worldIn.getBlockState(pos);
            if(player.isCrouching())
            {
                if(getBlockState.getBlock() instanceof PedestalBlock)
                {
                    if(!player.getHeldItemMainhand().isEnchanted())
                    {
                        TileEntity tile = worldIn.getTileEntity(pos);
                        if(tile instanceof PedestalTileEntity)
                        {
                            PedestalTileEntity ped = ((PedestalTileEntity)tile);
                            this.storedPositionList = ped.getLocationList();
                        }
                        //Gets Pedestal Clicked on Pos
                        this.storedPosition = pos;
                        //Writes to NBT
                        writePosToNBT(player.getHeldItemMainhand());
                        writePosListToNBT(player.getHeldItemMainhand());
                        //Applies effect to wrench in hand
                        if(player.getHeldItemMainhand().getItem().equals(ItemLinkingToolBackwards.DEFAULT))
                        {
                            player.getHeldItemMainhand().addEnchantment(Enchantments.UNBREAKING,-1);
                        }
                        return ActionResultType.SUCCESS;
                    }
                    //If wrench has the compound stacks and has a position stored(is enchanted)
                    else if(player.getHeldItemMainhand().hasTag() && player.getHeldItemMainhand().isEnchanted())
                    {
                        BlockPos senderPos = getStoredPosition(player.getHeldItemMainhand());
                        //Checks if clicked blocks is a Pedestal
                        if(worldIn.getBlockState(pos).getBlock() instanceof PedestalBlock)
                        {
                            //Backwards LT finds the Sender Pedestal Based on the stored BlockPos
                            TileEntity tileSender = worldIn.getTileEntity(senderPos);
                            //TileEntity tileReceiver = worldIn.getTileEntity(pos);

                            if (tileSender instanceof PedestalTileEntity) {
                                PedestalTileEntity senderPedestal = (PedestalTileEntity) tileSender;

                                //checks if connecting pedestal is out of range of the senderPedestal
                                if(isPedestalInRange(senderPedestal,pos))
                                {
                                    //Checks if pedestals to be linked are on same networks or if one is neutral
                                    if(senderPedestal.canLinkToPedestalNetwork(pos))
                                    {
                                        //If stored location isnt the same as the connecting pedestal
                                        if(!senderPedestal.isSamePedestal(pos))
                                        {
                                            //Checks if the conenction hasnt been made once already yet
                                            if(!senderPedestal.isAlreadyLinked(pos))
                                            {
                                                //Checks if senderPedestal has locationSlots available
                                                ////System.out.println("Stored Locations: "+ tilePedestal.getNumberOfStoredLocations());
                                                if(senderPedestal.storeNewLocation(pos))
                                                {
                                                    //If slots are available then set wrench properties back to a default value
                                                    this.storedPosition = defaultPos;
                                                    this.storedPositionList = new ArrayList<>();
                                                    writePosToNBT(player.getHeldItemMainhand());
                                                    writePosListToNBT(player.getHeldItemMainhand());
                                                    worldIn.notifyBlockUpdate(pos,worldIn.getBlockState(pos),worldIn.getBlockState(pos),2);

                                                    if(player.getHeldItemMainhand().getItem().equals(ItemLinkingToolBackwards.DEFAULT))
                                                    {
                                                        if(player.getHeldItemMainhand().isEnchanted())
                                                        {
                                                            Map<Enchantment, Integer> enchantsNone = Maps.<Enchantment, Integer>newLinkedHashMap();
                                                            EnchantmentHelper.setEnchantments(enchantsNone,player.getHeldItemMainhand());
                                                        }
                                                    }
                                                    player.sendMessage(linksucess,Util.DUMMY_UUID);
                                                    return ActionResultType.SUCCESS;
                                                }
                                                else player.sendMessage(linkunsuccess,Util.DUMMY_UUID);
                                            }
                                            else
                                            {
                                                senderPedestal.removeLocation(pos);
                                                if(player.getHeldItemMainhand().getItem().equals(ItemLinkingToolBackwards.DEFAULT))
                                                {
                                                    if(player.getHeldItemMainhand().isEnchanted())
                                                    {
                                                        Map<Enchantment, Integer> enchantsNone = Maps.<Enchantment, Integer>newLinkedHashMap();
                                                        EnchantmentHelper.setEnchantments(enchantsNone,player.getHeldItemMainhand());
                                                    }
                                                }
                                                player.sendMessage(linkremoved,Util.DUMMY_UUID);
                                                return ActionResultType.SUCCESS;
                                            }
                                        }
                                        else player.sendMessage(linkitsself,Util.DUMMY_UUID);
                                    }
                                    else player.sendMessage(linknetwork,Util.DUMMY_UUID);
                                }
                                else player.sendMessage(linkdistance, Util.DUMMY_UUID);
                            }
                        }
                        return ActionResultType.FAIL;
                    }
                }
                else if(!(getBlockState.getBlock().equals(Blocks.AIR)))
                {
                    this.storedPosition = defaultPos;
                    this.storedPositionList = new ArrayList<>();
                    writePosToNBT(player.getHeldItemMainhand());
                    writePosListToNBT(player.getHeldItemMainhand());
                    worldIn.notifyBlockUpdate(pos,worldIn.getBlockState(pos),worldIn.getBlockState(pos),2);
                    if(player.getHeldItemMainhand().getItem() instanceof ItemLinkingTool)
                    {
                        if(player.getHeldItemMainhand().isEnchanted())
                        {
                            Map<Enchantment, Integer> enchantsNone = Maps.<Enchantment, Integer>newLinkedHashMap();
                            EnchantmentHelper.setEnchantments(enchantsNone,player.getHeldItemMainhand());
                            return ActionResultType.SUCCESS;
                        }
                    }
                }
                else
                {

                }

                return ActionResultType.FAIL;
            }
            else
            {
                if(worldIn.getBlockState(pos).getBlock() instanceof PedestalBlock) {
                    //Checks Tile at location to make sure its a TilePedestal
                    TileEntity tileEntity = worldIn.getTileEntity(pos);
                    if (tileEntity instanceof PedestalTileEntity) {
                        PedestalTileEntity tilePedestal = (PedestalTileEntity) tileEntity;

                        TranslationTextComponent rrobin = new TranslationTextComponent(getTranslationKey() + ".tool_rrobin");
                        TranslationTextComponent rrobint = new TranslationTextComponent(getTranslationKey() + ".tool_rrobin_true");
                        TranslationTextComponent rrobinf = new TranslationTextComponent(getTranslationKey() + ".tool_rrobin_false");
                        TranslationTextComponent TransferTypeText = (tilePedestal.hasRRobin())?(rrobint):(rrobinf);
                        rrobin.append(TransferTypeText);
                        rrobin.mergeStyle(TextFormatting.LIGHT_PURPLE);
                        player.sendMessage(rrobin,Util.DUMMY_UUID);

                        if(tilePedestal.hasTool())
                        {
                            TranslationTextComponent tool = new TranslationTextComponent(getTranslationKey() + ".tool_stored");
                            tool.append(tilePedestal.getToolOnPedestal().getDisplayName());
                            tool.mergeStyle(TextFormatting.WHITE);
                            player.sendMessage(tool,Util.DUMMY_UUID);
                        }

                        if(tilePedestal.getSpeed()>0)
                        {
                            TranslationTextComponent speed = new TranslationTextComponent(getTranslationKey() + ".tool_speed");
                            speed.appendString(""+tilePedestal.getSpeed()+"");
                            speed.mergeStyle(TextFormatting.RED);
                            player.sendMessage(speed,Util.DUMMY_UUID);
                        }

                        if(tilePedestal.getCapacity()>0)
                        {
                            TranslationTextComponent capacity = new TranslationTextComponent(getTranslationKey() + ".tool_capacity");
                            capacity.appendString(""+tilePedestal.getCapacity()+"");
                            capacity.mergeStyle(TextFormatting.BLUE);
                            player.sendMessage(capacity,Util.DUMMY_UUID);
                        }

                        if(tilePedestal.getCapacity()>0)
                        {
                            TranslationTextComponent range = new TranslationTextComponent(getTranslationKey() + ".tool_range");
                            range.appendString(""+tilePedestal.getRange()+"");
                            range.mergeStyle(TextFormatting.GREEN);
                            player.sendMessage(range,Util.DUMMY_UUID);
                        }


                        List<BlockPos> getLocations = tilePedestal.getLocationList();
                        if(getLocations.size()>0)
                        {
                            TranslationTextComponent links = new TranslationTextComponent(getTranslationKey() + ".tool_linked");
                            links.mergeStyle(TextFormatting.GOLD);
                            player.sendMessage(links,Util.DUMMY_UUID);

                            for(int i = 0; i < getLocations.size();i++)
                            {
                                TranslationTextComponent linked = new TranslationTextComponent("   " + getLocations.get(i).getX() + "");
                                TranslationTextComponent seperator = new TranslationTextComponent(getTranslationKey() + ".tool_seperator");
                                linked.appendString(seperator.getString());
                                linked.appendString("" + getLocations.get(i).getY() + "");
                                linked.appendString(seperator.getString());
                                linked.appendString("" + getLocations.get(i).getZ() + "");
                                linked.mergeStyle(TextFormatting.GRAY);
                                player.sendMessage(linked,Util.DUMMY_UUID);
                            }
                        }
                    }
                    return ActionResultType.SUCCESS;
                }
            }
        }

        return super.onItemUse(context);
    }

    //Thanks to TheBoo on the e6 Discord for this suggestion
    @Override
    public ActionResult<ItemStack> onItemRightClick(World p_77659_1_, PlayerEntity p_77659_2_, Hand p_77659_3_) {
        //Thankyou past self: https://github.com/Mowmaster/Ensorcelled/blob/main/src/main/java/com/mowmaster/ensorcelled/enchantments/handlers/HandlerAOEMiner.java#L53
        //RayTraceResult result = player.pick(player.getLookVec().length(),0,false); results in MISS type returns
        RayTraceResult result = p_77659_2_.pick(5,0,false);
        if(result != null)
        {
            //Assuming it it hits a block it wont work???
            if(result.getType() == RayTraceResult.Type.MISS)
            {
                if(p_77659_2_.isCrouching())
                {
                    ItemStack heldItem = p_77659_2_.getHeldItem(p_77659_3_);
                    if(heldItem.getItem().equals(ItemLinkingToolBackwards.DEFAULT) && !heldItem.isEnchanted())
                    {
                        p_77659_2_.setHeldItem(p_77659_3_,new ItemStack(ItemLinkingTool.DEFAULT));
                        TranslationTextComponent range = new TranslationTextComponent(MODID + ".tool_change");
                        range.mergeStyle(TextFormatting.GREEN);
                        p_77659_2_.sendStatusMessage(range,true);
                        return ActionResult.resultSuccess(p_77659_2_.getHeldItem(p_77659_3_));
                    }
                    return ActionResult.resultFail(p_77659_2_.getHeldItem(p_77659_3_));
                }
            }
        }

        return super.onItemRightClick(p_77659_1_, p_77659_2_, p_77659_3_);
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        //super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);

        if(entityIn instanceof PlayerEntity)
        {
            PlayerEntity player = ((PlayerEntity)entityIn);
            if(stack.isEnchanted() && isSelected)
            {
                if (stack.hasTag()) {
                    this.getPosFromNBT(stack);

                    List<BlockPos> storedRecievers = getStoredPositionList(stack);
                    int locationsNum = storedRecievers.size();

                    if(storedPosition!=defaultPos)
                    {
                        if(isSelected)
                        {
                            if(worldIn.isRemote)
                            {
                                ticker++;

                                for(int i=0;i<locationsNum;i++)
                                {
                                    float val = i*0.125f;
                                    if(storedPositionList.size()>i){spawnParticleAroundPedestalBase(worldIn,ticker,storedPositionList.get(i),val,val,val,1.0f);}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isPedestalInRange(PedestalTileEntity pedestalSender, BlockPos receiver)
    {
        int range = pedestalSender.getLinkingRange();
        int x = receiver.getX();
        int y = receiver.getY();
        int z = receiver.getZ();
        int x1 = pedestalSender.getPos().getX();
        int y1 = pedestalSender.getPos().getY();
        int z1 = pedestalSender.getPos().getZ();
        int xF = Math.abs(Math.subtractExact(x,x1));
        int yF = Math.abs(Math.subtractExact(y,y1));
        int zF = Math.abs(Math.subtractExact(z,z1));

        if(xF>range || yF>range || zF>range)
        {
            return false;
        }
        else return true;
    }

    @Override
    public BlockPos getStoredPosition(ItemStack getWrenchItem)
    {
        getPosFromNBT(getWrenchItem);
        return storedPosition;
    }

    @Override
    public List<BlockPos> getStoredPositionList(ItemStack getWrenchItem)
    {
        getPosListFromNBT(getWrenchItem);
        return storedPositionList;
    }

    @Override
    public void writePosToNBT(ItemStack stack)
    {
        CompoundNBT compound = new CompoundNBT();
        if(stack.hasTag())
        {
            compound = stack.getTag();
        }
        compound.putInt("stored_x",this.storedPosition.getX());
        compound.putInt("stored_y",this.storedPosition.getY());
        compound.putInt("stored_z",this.storedPosition.getZ());
        stack.setTag(compound);
    }

    @Override
    public void writePosListToNBT(ItemStack stack)
    {
        CompoundNBT compound = new CompoundNBT();
        if(stack.hasTag())
        {
            compound = stack.getTag();
        }
        List<Integer> xval = new ArrayList<Integer>();
        List<Integer> yval = new ArrayList<Integer>();
        List<Integer> zval = new ArrayList<Integer>();
        for(int i=0;i<storedPositionList.size();i++)
        {
            xval.add(i,storedPositionList.get(i).getX());
            yval.add(i,storedPositionList.get(i).getY());
            zval.add(i,storedPositionList.get(i).getZ());
        }
        compound.putIntArray("storedlist_x",xval);
        compound.putIntArray("storedlist_y",yval);
        compound.putIntArray("storedlist_z",zval);
        stack.setTag(compound);
    }

    @Override
    public void getPosFromNBT(ItemStack stack)
    {
        if(stack.hasTag())
        {
            CompoundNBT getCompound = stack.getTag();
            int x = getCompound.getInt("stored_x");
            int y = getCompound.getInt("stored_y");
            int z = getCompound.getInt("stored_z");
            this.storedPosition = new BlockPos(x,y,z);
        }
    }

    @Override
    public void getPosListFromNBT(ItemStack stack)
    {
        List<BlockPos> posStored = new ArrayList<>();
        if(stack.hasTag())
        {
            CompoundNBT getCompound = stack.getTag();
            int[] xval = getCompound.getIntArray("storedlist_x");
            int[] yval = getCompound.getIntArray("storedlist_y");
            int[] zval = getCompound.getIntArray("storedlist_z");

            for(int i = 0;i<xval.length;i++)
            {
                posStored.add(i,new BlockPos(xval[i],yval[i],zval[i]));
            }
            this.storedPositionList = posStored;
        }
    }

    @Override
    public void spawnParticleAroundPedestalBase(World world,int tick, BlockPos pos, float r, float g, float b, float alpha)
    {
        double dx = (double)pos.getX();
        double dy = (double)pos.getY();
        double dz = (double)pos.getZ();

        BlockState state = world.getBlockState(pos);
        Direction enumfacing = Direction.UP;
        if(state.getBlock() instanceof PedestalBlock)
        {
            enumfacing = state.get(FACING);
        }
        RedstoneParticleData parti = new RedstoneParticleData(r, g, b, alpha);
        switch (enumfacing)
        {
            case UP:
                if (tick%20 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.5D, dz+ 0.25D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.5D, dz+ 0.75D,0, 0, 0);
                if (tick%15 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.5D, dz+ 0.25D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.5D, dz+ 0.75D,0, 0, 0);
                return;
            case DOWN:
                if (tick%20 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.5D, dz+ 0.25D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.5D, dz+ 0.75D,0, 0, 0);
                if (tick%15 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.5D, dz+ 0.25D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.5D, dz+ 0.75D,0, 0, 0);
                return;
            case NORTH:
                if (tick%20 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.25D, dz+0.5D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.75D, dz+0.5D,0, 0, 0);
                if (tick%15 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.25D, dz+0.5D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.75D, dz+0.5D,0, 0, 0);
                return;
            case SOUTH:
                if (tick%20 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.25D, dz+0.5D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.75D, dz+0.5D,0, 0, 0);
                if (tick%15 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.25D, dz+0.5D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.75D, dz+0.5D,0, 0, 0);
                return;
            case EAST:
                if (tick%20 == 0) world.addParticle(parti, dx+0.5D, dy+ 0.25D, dz+0.25D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+0.5D, dy+ 0.25D, dz+0.75D,0, 0, 0);
                if (tick%15 == 0) world.addParticle(parti, dx+0.5D, dy+ 0.75D, dz+0.25D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+0.5D, dy+ 0.75D, dz+0.75D,0, 0, 0);
                return;
            case WEST:
                if (tick%20 == 0) world.addParticle(parti, dx+0.5D, dy+0.25D, dz+ 0.25D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+0.5D, dy+0.25D, dz+ 0.75D,0, 0, 0);
                if (tick%15 == 0) world.addParticle(parti, dx+0.5D, dy+0.75D, dz+ 0.25D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+0.5D, dy+0.75D, dz+ 0.75D,0, 0, 0);
                return;
            default:
                if (tick%30 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.5D, dz+ 0.25D,0, 0, 0);
                if (tick%35 == 0) world.addParticle(parti, dx+ 0.25D, dy+0.5D, dz+ 0.75D,0, 0, 0);
                if (tick%25 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.5D, dz+ 0.25D,0, 0, 0);
                if (tick%30 == 0) world.addParticle(parti, dx+ 0.75D, dy+0.5D, dz+ 0.75D,0, 0, 0);
                return;
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        //new TranslationTextComponent(getTranslationKey() + ".tool_speed", tilePedestal.getSpeed()).mergeStyle(TextFormatting.RED)
        TranslationTextComponent selected = new TranslationTextComponent(getTranslationKey() + ".tool_block_selected");
        TranslationTextComponent unselected = new TranslationTextComponent(getTranslationKey() + ".tool_block_unselected");
        TranslationTextComponent cordX = new TranslationTextComponent(getTranslationKey() + ".tool_X");
        TranslationTextComponent cordY = new TranslationTextComponent(getTranslationKey() + ".tool_Y");
        TranslationTextComponent cordZ = new TranslationTextComponent(getTranslationKey() + ".tool_Z");
        if(stack.getItem() instanceof ItemLinkingTool) {
            if (stack.hasTag()) {
                if (stack.isEnchanted()) {
                    selected.appendString(cordX.getString());
                    selected.appendString("" + this.getStoredPosition(stack).getX() + "");
                    selected.appendString(cordY.getString());
                    selected.appendString("" + this.getStoredPosition(stack).getY() + "");
                    selected.appendString(cordZ.getString());
                    selected.appendString("" + this.getStoredPosition(stack).getZ() + "");
                    tooltip.add(selected);
                } else tooltip.add(unselected);
            } else tooltip.add(unselected);
        }
    }

    public static final Item DEFAULT = new ItemLinkingToolBackwards().setRegistryName(new ResourceLocation(MODID, "linkingtoolbackwards"));

    @SubscribeEvent
    public static void onItemRegistryReady(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().register(DEFAULT);
    }




}
