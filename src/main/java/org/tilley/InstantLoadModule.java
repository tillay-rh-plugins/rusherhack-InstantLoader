package org.tilley;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.client.api.events.render.EventRender3D;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer3D;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.core.setting.StringSetting;
import org.rusherhack.core.event.subscribe.Subscribe;

public class InstantLoadModule extends ToggleableModule {
	StringSetting username = new StringSetting("Username", mc.getUser().getName().toLowerCase());

	public InstantLoadModule() {
		super("InstantLoad", "Load a trapdoor as soon as the target logs on", ModuleCategory.COMBAT);
		this.registerSettings(username);
	}

    @Subscribe
    public void onPacketReceive(EventPacket.Receive event) {
        if (!(event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet)) return;
        if (packet.newEntries().stream().noneMatch(p -> p.profile() != null &&
            p.profile().getName().equalsIgnoreCase(username.getValue()))) return;

        ChatUtils.print(username.getValue() + " has joined!");
        if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.gameMode == null || mc.level == null ||
            !(mc.level.getBlockState(bhr.getBlockPos()).getBlock() instanceof TrapDoorBlock)) {
            ChatUtils.print("Unable to load pearl! No trapdoor was targeted.");
            return;
        }

        mc.execute(() -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr));
        ChatUtils.print("Loaded pearl!");
    }

    @Subscribe
    private void onRender3D(EventRender3D event) {
        if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.gameMode == null || mc.level == null ||
            !(mc.level.getBlockState(bhr.getBlockPos()).getBlock() instanceof TrapDoorBlock)) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        if (state.getBlock() instanceof TrapDoorBlock) {
            BlockState closedState = state.setValue(TrapDoorBlock.OPEN, false);
            AABB trapdoorAABB = closedState.getCollisionShape(mc.level, pos).bounds().move(pos);
            boolean hasPearl = !mc.level.getEntitiesOfClass(ThrownEnderpearl.class, trapdoorAABB).isEmpty();
            int color = !state.getValue(TrapDoorBlock.OPEN) ? 0xFF0000FF : hasPearl ? 0xFF00FF00 : 0xFFFF0000;

            IRenderer3D renderer = event.getRenderer();
            renderer.begin(event.getMatrixStack());
            renderer.setLineWidth(5f);
            renderer.drawBox(pos, false, true, color);
            renderer.end();
        }
    }

}
