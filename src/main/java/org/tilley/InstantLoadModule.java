package org.tilley;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
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
import org.rusherhack.client.api.utils.objects.PlayerMessage;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.StringSetting;
import org.rusherhack.core.event.subscribe.Subscribe;

public class InstantLoadModule extends ToggleableModule {
    StringSetting username = new StringSetting("Username", mc.getUser().getName().toLowerCase());
    BooleanSetting onConnect = new BooleanSetting("OnConnect", true);
    BooleanSetting chatMsgs = new BooleanSetting("Chat Messages", false);
    StringSetting chatText = new StringSetting("text", "load");

    public InstantLoadModule() {
        super("InstantLoad", "Instantly close a trapdoor to load pearl stasis chamber and avoid danger", ModuleCategory.COMBAT);
        this.registerSettings(username, onConnect, chatMsgs);
        chatMsgs.addSubSettings(chatText);
        username.setDescription("Username of the player to check for");
        onConnect.setDescription("Load pearl when target player logs on to the server");
        chatMsgs.setDescription("Load pearl when target player sends specific chat messages");
        chatText.setDescription("Substring required in player's messages to load. If blank, all messages from target player will trigger.");
    }

    @Subscribe
    public void onPacketReceive(EventPacket.Receive event) {
        if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet && onConnect.getValue()) {
            if (packet.newEntries().stream().noneMatch(p -> p.profile() != null &&
                    p.profile().getName().equalsIgnoreCase(username.getValue()))) return;

            ChatUtils.print(username.getValue() + " has joined!");
            attemptToLoad();

        } else if (event.getPacket() instanceof ClientboundPlayerChatPacket packet && chatMsgs.getValue()) {
            String message = PlayerMessage.parse(packet.body().content(), true).getMessage();
            PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(packet.sender());
            if (info == null || info.getProfile().getName() == null) return;
            String sender = info.getProfile().getName();
            if (!sender.equalsIgnoreCase(username.getValue())) return;
            if (message.toLowerCase().contains(chatText.getValue().toLowerCase()) || chatText.getValue().isEmpty()) {
                ChatUtils.print(sender + " has chatted trigger message!");
                attemptToLoad();
            }

        } else if (event.getPacket() instanceof ClientboundSystemChatPacket packet && chatMsgs.getValue()) {
            PlayerMessage m = PlayerMessage.parse(packet.content().toString(), true);

            if (m.getSender() == null || !m.getSender().equalsIgnoreCase(username.getValue())) return;
            if (m.getMessage().toLowerCase().contains(chatText.getValue().toLowerCase()) || chatText.getValue().isEmpty()) {
                ChatUtils.print(m.getSender() + " has chatted trigger message!");
                attemptToLoad();
            }
        }
    }

    @Subscribe
    private void onRender3D(EventRender3D event) {
        if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.gameMode == null || mc.level == null ||
                !(mc.level.getBlockState(bhr.getBlockPos()).getBlock() instanceof TrapDoorBlock)) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        if (state.getBlock() instanceof TrapDoorBlock) {
            boolean hasPearl = !mc.level.getEntitiesOfClass(ThrownEnderpearl.class, new AABB(pos)).isEmpty();
            int color = !state.getValue(TrapDoorBlock.OPEN) ? 0xFF0000FF : hasPearl ? 0xFF00FF00 : 0xFFFF0000;

            IRenderer3D renderer = event.getRenderer();
            renderer.begin(event.getMatrixStack());
            renderer.setLineWidth(5f);
            renderer.drawBox(pos, false, true, color);
            renderer.end();
        }
    }

    private void attemptToLoad() {
        if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.gameMode == null || mc.level == null ||
                !(mc.level.getBlockState(bhr.getBlockPos()).getBlock() instanceof TrapDoorBlock)) {
            ChatUtils.print("Unable to load pearl! No trapdoor was targeted.");
            return;
        }

        mc.execute(() -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr));
        ChatUtils.print("Loaded pearl!");
    }

}
