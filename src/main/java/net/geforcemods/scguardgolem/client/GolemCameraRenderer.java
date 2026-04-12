package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.entity.GolemCameraEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * No-op renderer for GolemCameraEntity — the entity is invisible and should never render.
 */
public class GolemCameraRenderer extends EntityRenderer<GolemCameraEntity> {

    public GolemCameraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(GolemCameraEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/unknown.png");
    }
}
