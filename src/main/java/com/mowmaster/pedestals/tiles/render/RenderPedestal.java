package com.mowmaster.pedestals.tiles.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mowmaster.pedestals.tiles.PedestalTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import javax.annotation.Nonnull;

import static net.minecraft.block.DirectionalBlock.FACING;

public class RenderPedestal extends TileEntityRenderer<PedestalTileEntity> {

    public RenderPedestal(TileEntityRendererDispatcher rendererDispatcher)
    {
        super(rendererDispatcher);
    }

    @Override
    public void render(@Nonnull PedestalTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {

        if (!tileEntityIn.isRemoved()) {
            Direction facing = tileEntityIn.getBlockState().get(FACING);
            ItemStack stack = tileEntityIn.getItemInPedestalOverride();
            ItemStack coin = tileEntityIn.getCoinOnPedestal();
            World world = tileEntityIn.getWorld();
            int renderAugmentType = tileEntityIn.getRenderAugmentType();

            if(renderAugmentType !=0)
            {
                if(facing== Direction.UP)//when placed on ground
                {
                    renderTile(world,matrixStackIn,bufferIn,coin,stack,combinedLightIn,combinedOverlayIn,renderAugmentType);
                }
                if(facing== Direction.DOWN) {
                    //matrixStackIn.rotate(new Quaternion(0, 0, 1,180));
                    matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(180));
                    matrixStackIn.translate(0, -1, 0);
                    matrixStackIn.translate(-1, 0, 0);
                    renderTile(world,matrixStackIn,bufferIn,coin,stack,combinedLightIn,combinedOverlayIn,renderAugmentType);            }
                if(facing== Direction.NORTH) {
                    //matrixStackIn.rotate(new Quaternion(1, 0, 0,270));
                    matrixStackIn.rotate(Vector3f.XP.rotationDegrees(270));
                    matrixStackIn.translate(0, -1, 0);
                    renderTile(world,matrixStackIn,bufferIn,coin,stack,combinedLightIn,combinedOverlayIn,renderAugmentType);            }
                if(facing== Direction.EAST) {
                    //matrixStackIn.rotate(270, 0, 0, 1);
                    matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(270));
                    matrixStackIn.translate(-1, 0, 0);
                    renderTile(world,matrixStackIn,bufferIn,coin,stack,combinedLightIn,combinedOverlayIn,renderAugmentType);            }
                if(facing== Direction.SOUTH) {
                    //matrixStackIn.rotate(90, 1, 0, 0);
                    matrixStackIn.rotate(Vector3f.XP.rotationDegrees(90));
                    matrixStackIn.translate(0, 0, -1);
                    renderTile(world,matrixStackIn,bufferIn,coin,stack,combinedLightIn,combinedOverlayIn,renderAugmentType);            }
                if(facing== Direction.WEST) {
                    //matrixStackIn.rotate(90, 0, 0, 1);
                    matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(90));
                    matrixStackIn.translate(0, -1, 0);
                    renderTile(world,matrixStackIn,bufferIn,coin,stack,combinedLightIn,combinedOverlayIn,renderAugmentType);            }
            }
        }
    }

    public static void  renderTile(World worldIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, ItemStack coin, ItemStack item, int combinedLightIn, int combinedOverlayIn, int renderAugmentType)
    {
        if(renderAugmentType !=2)renderItemRotating(worldIn,matrixStackIn,bufferIn,item,combinedLightIn,combinedOverlayIn);
        if(renderAugmentType !=1)
        {
            renderCoin(worldIn,coin,matrixStackIn,bufferIn,0.5f,0.475f,0.3125f,0,0,0,0,combinedLightIn,combinedOverlayIn);
            renderCoin(worldIn,coin,matrixStackIn,bufferIn,0.3125f,0.475f,0.5f,90,0,1f,0,combinedLightIn,combinedOverlayIn);
            renderCoin(worldIn,coin,matrixStackIn,bufferIn,0.5f,0.475f,0.6875f,180,0,1f,0,combinedLightIn,combinedOverlayIn);
            renderCoin(worldIn,coin,matrixStackIn,bufferIn,0.6875f,0.475f,0.5f,270,0,1f,0,combinedLightIn,combinedOverlayIn);
        }
    }

    public static void renderItemRotating(World worldIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, ItemStack itemStack, int combinedLightIn, int combinedOverlayIn)
    {
        if (!itemStack.isEmpty()) {
            matrixStackIn.push();
            matrixStackIn.translate(0.5, 1.0, 0.5);
            //matrixStackIn.translate(0, MathHelper.sin((worldIn.getGameTime()) / 10.0F) * 0.1 + 0.1, 0); BOBBING ITEM
            matrixStackIn.scale(0.75F, 0.75F, 0.75F);
            long time = System.currentTimeMillis();
            float angle = (time/25) % 360;
            //float angle = (worldIn.getGameTime()) / 20.0F * (180F / (float) Math.PI);
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(angle));
            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            IBakedModel baked = renderer.getItemModelWithOverrides(itemStack,worldIn,null);
            renderer.renderItem(itemStack,ItemCameraTransforms.TransformType.GROUND,true,matrixStackIn,bufferIn,combinedLightIn,combinedOverlayIn,baked);
            //Minecraft.getInstance().getItemRenderer().renderItem(itemStack, ItemCameraTransforms.TransformType.GROUND, combinedLightIn, combinedOverlayIn, matrixStackIn, bufferIn);
            matrixStackIn.pop();
        }
    }
    public static void renderCoin(World worldIn,ItemStack itemCoin, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, float x, float y, float z, float angle, float xr, float yr, float zr, int combinedLightIn, int combinedOverlayIn) {
        if (!itemCoin.isEmpty()) {
            matrixStackIn.push();
            matrixStackIn.translate(x, y, z);
            matrixStackIn.scale(0.1875f, 0.1875f, 0.1875f);
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(angle));
            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            IBakedModel baked = renderer.getItemModelWithOverrides(itemCoin,worldIn,null);
            renderer.renderItem(itemCoin,ItemCameraTransforms.TransformType.FIXED,true,matrixStackIn,bufferIn,combinedLightIn,combinedOverlayIn,baked);
            //Minecraft.getInstance().getItemRenderer().renderItem(itemCoin, ItemCameraTransforms.TransformType.FIXED, combinedLightIn, combinedOverlayIn, matrixStackIn, bufferIn);
            matrixStackIn.pop();
        }
    }

    //Old event was FMLClientSetupEvent
    public static void init(final ModelRegistryEvent event)
    {
        ClientRegistry.bindTileEntityRenderer(PedestalTileEntity.PEDESTALTYPE, RenderPedestal::new);
    }
}