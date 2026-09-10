package net.deanxbox.faststonecutter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class FastStoneCutter implements ModInitializer {
    private static final int PLAYER_SLOT_START = 2;
    private static final int PLAYER_SLOT_END = 38;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(CutAllPayload.TYPE, CutAllPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CutAllPayload.TYPE, (payload, context) -> cutAll(context.player()));
    }

    private static void cutAll(ServerPlayer player) {
        if (player.isSpectator()
                || !(player.containerMenu instanceof StonecutterMenu menu)
                || !menu.stillValid(player)) {
            return;
        }

        int selectedRecipeIndex = menu.getSelectedRecipeIndex();
        if (selectedRecipeIndex < 0 || !menu.getSlot(StonecutterAutomation.INPUT_SLOT).hasItem()) {
            return;
        }

        ItemStack sourceStack = menu.getSlot(StonecutterAutomation.INPUT_SLOT).getItem().copyWithCount(1);
        StonecutterAutomation.Session session = StonecutterAutomation.start(selectedRecipeIndex);

        while (session.active()) {
            StonecutterAutomation.TickResult tick = session.next(
                    menu.getSlot(StonecutterAutomation.INPUT_SLOT).hasItem(),
                    menu.getSlot(StonecutterAutomation.RESULT_SLOT).hasItem(),
                    snapshotPlayerSlots(menu, sourceStack),
                    PLAYER_SLOT_START,
                    Math.min(PLAYER_SLOT_END, menu.slots.size())
            );
            session = tick.session();

            switch (tick.action().type()) {
                case TAKE_RESULT -> quickMoveSlot(menu, player, StonecutterAutomation.RESULT_SLOT);
                case SELECT_RECIPE -> {
                    if (menu.clickMenuButton(player, session.selectedRecipeIndex())) {
                        menu.broadcastChanges();
                    }
                }
                case LOAD_SOURCE_SLOT -> quickMoveSlot(menu, player, tick.action().slotIndex());
                case NONE -> {
                    return;
                }
            }
        }
    }

    private static List<StonecutterAutomation.SlotSnapshot> snapshotPlayerSlots(StonecutterMenu menu, ItemStack sourceStack) {
        List<StonecutterAutomation.SlotSnapshot> snapshots = new ArrayList<>();
        int lastSlot = Math.min(PLAYER_SLOT_END, menu.slots.size());

        for (int slotIndex = PLAYER_SLOT_START; slotIndex < lastSlot; slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            snapshots.add(new StonecutterAutomation.SlotSnapshot(
                    slotIndex,
                    slot.hasItem() && ItemStack.isSameItemSameComponents(sourceStack, slot.getItem())
            ));
        }

        return snapshots;
    }

    private static void quickMoveSlot(StonecutterMenu menu, ServerPlayer player, int slotIndex) {
        Slot slot = menu.getSlot(slotIndex);
        ItemStack moved = menu.quickMoveStack(player, slotIndex);

        while (!moved.isEmpty() && ItemStack.isSameItem(slot.getItem(), moved)) {
            moved = menu.quickMoveStack(player, slotIndex);
        }
    }

    public record CutAllPayload() implements CustomPacketPayload {
        public static final CutAllPayload INSTANCE = new CutAllPayload();
        public static final Type<CutAllPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("faststonecutter", "cut_all")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CutAllPayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
