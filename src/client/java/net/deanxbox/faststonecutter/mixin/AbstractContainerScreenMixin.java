package net.deanxbox.faststonecutter.mixin;

import net.deanxbox.faststonecutter.StonecutterAutomation;
import net.deanxbox.faststonecutter.StonecutterAutomation.Action;
import net.deanxbox.faststonecutter.StonecutterAutomation.ActionType;
import net.deanxbox.faststonecutter.StonecutterAutomation.Session;
import net.deanxbox.faststonecutter.StonecutterAutomation.SlotSnapshot;
import net.deanxbox.faststonecutter.StonecutterAutomation.TickResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends net.minecraft.client.gui.screens.Screen {
    @Unique
    private static final Component FASTSTONECUTTER_BUTTON_LABEL = Component.translatable("gui.faststonecutter.cut_all");

    @Unique
    private static final Component FASTSTONECUTTER_BUTTON_TOOLTIP = Component.translatable("gui.faststonecutter.cut_all.tooltip");

    @Unique
    private static final int FASTSTONECUTTER_BUTTON_WIDTH = 104;

    @Unique
    private static final int FASTSTONECUTTER_BUTTON_HEIGHT = 20;

    @Unique
    private static final int FASTSTONECUTTER_PLAYER_SLOT_START = 2;

    @Unique
    private static final int FASTSTONECUTTER_PLAYER_SLOT_END = 38;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected int imageHeight;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Unique
    private Button faststonecutter$cutAllButton;

    @Unique
    private Session faststonecutter$session;

    @Unique
    private ItemStack faststonecutter$sourceStack = ItemStack.EMPTY;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void faststonecutter$addCutAllButton(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!(screen.getMenu() instanceof StonecutterMenu)) {
            return;
        }

        int buttonX = this.leftPos + this.imageWidth + 6;
        int buttonY = this.topPos + 16;

        if (buttonX + FASTSTONECUTTER_BUTTON_WIDTH > this.width) {
            buttonX = this.leftPos + this.imageWidth - FASTSTONECUTTER_BUTTON_WIDTH - 6;
            buttonY = this.topPos + this.imageHeight - FASTSTONECUTTER_BUTTON_HEIGHT - 6;
        }

        this.faststonecutter$cutAllButton = Button.builder(
                        FASTSTONECUTTER_BUTTON_LABEL,
                        button -> this.faststonecutter$cutAllStone()
                )
                .bounds(buttonX, buttonY, FASTSTONECUTTER_BUTTON_WIDTH, FASTSTONECUTTER_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(FASTSTONECUTTER_BUTTON_TOOLTIP))
                .build();
        this.faststonecutter$cutAllButton.active = this.faststonecutter$canStartCutting();
        this.addRenderableWidget(this.faststonecutter$cutAllButton);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void faststonecutter$updateButtonState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.faststonecutter$cutAllButton != null) {
            this.faststonecutter$cutAllButton.active = !this.faststonecutter$isRunning() && this.faststonecutter$canStartCutting();
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void faststonecutter$runAutomationTick(CallbackInfo ci) {
        if (!this.faststonecutter$isRunning()) {
            return;
        }

        StonecutterMenu menu = this.faststonecutter$stonecutterMenu();
        if (menu == null || this.faststonecutter$sourceStack.isEmpty()) {
            this.faststonecutter$stopAutomation();
            return;
        }

        TickResult tick = this.faststonecutter$session.next(
                menu.getSlot(StonecutterAutomation.INPUT_SLOT).hasItem(),
                menu.getSlot(StonecutterAutomation.RESULT_SLOT).hasItem(),
                this.faststonecutter$snapshotPlayerSlots(menu, this.faststonecutter$sourceStack),
                FASTSTONECUTTER_PLAYER_SLOT_START,
                Math.min(FASTSTONECUTTER_PLAYER_SLOT_END, menu.slots.size())
        );
        this.faststonecutter$session = tick.session();

        Action action = tick.action();
        if (action.type() == ActionType.TAKE_RESULT) {
            this.faststonecutter$quickMoveSlot(menu, StonecutterAutomation.RESULT_SLOT);
            return;
        }

        if (action.type() == ActionType.SELECT_RECIPE) {
            this.faststonecutter$selectRecipe(menu, this.faststonecutter$session.selectedRecipeIndex());
            return;
        }

        if (action.type() == ActionType.LOAD_SOURCE_SLOT) {
            this.faststonecutter$quickMoveSlot(menu, action.slotIndex());
            return;
        }

        this.faststonecutter$stopAutomation();
    }

    @Unique
    private StonecutterMenu faststonecutter$stonecutterMenu() {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() instanceof StonecutterMenu stonecutterMenu) {
            return stonecutterMenu;
        }

        return null;
    }

    @Unique
    private boolean faststonecutter$canStartCutting() {
        StonecutterMenu menu = this.faststonecutter$stonecutterMenu();
        return menu != null
                && this.minecraft != null
                && this.minecraft.player != null
                && this.minecraft.gameMode != null
                && menu.getSelectedRecipeIndex() >= 0
                && menu.getSlot(StonecutterAutomation.INPUT_SLOT).hasItem();
    }

    @Unique
    private void faststonecutter$cutAllStone() {
        StonecutterMenu menu = this.faststonecutter$stonecutterMenu();
        if (menu == null || !this.faststonecutter$canStartCutting()) {
            return;
        }

        this.faststonecutter$sourceStack = menu.getSlot(StonecutterAutomation.INPUT_SLOT).getItem().copyWithCount(1);
        this.faststonecutter$session = StonecutterAutomation.start(menu.getSelectedRecipeIndex());
        this.faststonecutter$selectRecipe(menu, this.faststonecutter$session.selectedRecipeIndex());
    }

    @Unique
    private boolean faststonecutter$isRunning() {
        return this.faststonecutter$session != null && this.faststonecutter$session.active();
    }

    @Unique
    private void faststonecutter$stopAutomation() {
        this.faststonecutter$session = null;
        this.faststonecutter$sourceStack = ItemStack.EMPTY;
    }

    @Unique
    private List<SlotSnapshot> faststonecutter$snapshotPlayerSlots(StonecutterMenu menu, ItemStack sourceStack) {
        List<SlotSnapshot> snapshots = new ArrayList<>();
        int lastSlot = Math.min(FASTSTONECUTTER_PLAYER_SLOT_END, menu.slots.size());

        for (int slotIndex = FASTSTONECUTTER_PLAYER_SLOT_START; slotIndex < lastSlot; slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            snapshots.add(new SlotSnapshot(
                    slotIndex,
                    slot.hasItem() && ItemStack.isSameItemSameComponents(sourceStack, slot.getItem())
            ));
        }

        return snapshots;
    }

    @Unique
    private void faststonecutter$selectRecipe(StonecutterMenu menu, int selectedRecipeIndex) {
        if (this.minecraft != null
                && this.minecraft.player != null
                && this.minecraft.gameMode != null
                && selectedRecipeIndex >= 0
                && menu.clickMenuButton(this.minecraft.player, selectedRecipeIndex)) {
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selectedRecipeIndex);
        }
    }

    @Unique
    private void faststonecutter$quickMoveSlot(StonecutterMenu menu, int slotIndex) {
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 0, ClickType.QUICK_MOVE, this.minecraft.player);
        }
    }
}
