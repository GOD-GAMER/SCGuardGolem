package net.geforcemods.scguardgolem.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.geforcemods.scguardgolem.entity.GolemCameraEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * No-op renderer for GolemCameraEntity — the entity is invisible and should never render.
 */
public class GolemCameraRenderer extends EntityRenderer<GolemCameraEntity, EntityRenderState> {

    public GolemCameraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
