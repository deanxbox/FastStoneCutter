package net.deanxbox.faststonecutter;

import java.util.List;

public final class StonecutterAutomation {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    public static final int DEFAULT_MAX_ACTIONS = 256;

    private StonecutterAutomation() {
    }

    public static Action nextAction(
            int selectedRecipeIndex,
            boolean inputHasItem,
            boolean resultHasItem,
            List<SlotSnapshot> playerSlots,
            int playerSlotStartInclusive,
            int playerSlotEndExclusive
    ) {
        if (selectedRecipeIndex < 0) {
            return Action.none();
        }

        if (inputHasItem && resultHasItem) {
            return Action.takeResult();
        }

        if (inputHasItem) {
            return Action.selectRecipe();
        }

        for (SlotSnapshot slot : playerSlots) {
            if (slot.index() >= playerSlotStartInclusive
                    && slot.index() < playerSlotEndExclusive
                    && slot.matchesSource()) {
                return new Action(ActionType.LOAD_SOURCE_SLOT, slot.index());
            }
        }

        return Action.none();
    }

    public static Session start(int selectedRecipeIndex) {
        return start(selectedRecipeIndex, DEFAULT_MAX_ACTIONS);
    }

    public static Session start(int selectedRecipeIndex, int maxActions) {
        return new Session(selectedRecipeIndex, Math.max(0, maxActions), selectedRecipeIndex >= 0 && maxActions > 0);
    }

    public enum ActionType {
        NONE,
        TAKE_RESULT,
        SELECT_RECIPE,
        LOAD_SOURCE_SLOT
    }

    public record Action(ActionType type, int slotIndex) {
        public static Action none() {
            return new Action(ActionType.NONE, -1);
        }

        public static Action takeResult() {
            return new Action(ActionType.TAKE_RESULT, RESULT_SLOT);
        }

        public static Action selectRecipe() {
            return new Action(ActionType.SELECT_RECIPE, -1);
        }
    }

    public record SlotSnapshot(int index, boolean matchesSource) {
    }

    public record Session(int selectedRecipeIndex, int remainingActions, boolean active) {
        public TickResult next(
                boolean inputHasItem,
                boolean resultHasItem,
                List<SlotSnapshot> playerSlots,
                int playerSlotStartInclusive,
                int playerSlotEndExclusive
        ) {
            if (!active || remainingActions <= 0) {
                return new TickResult(Action.none(), new Session(selectedRecipeIndex, 0, false));
            }

            Action action = StonecutterAutomation.nextAction(
                    selectedRecipeIndex,
                    inputHasItem,
                    resultHasItem,
                    playerSlots,
                    playerSlotStartInclusive,
                    playerSlotEndExclusive
            );

            if (action.type() == ActionType.NONE) {
                return new TickResult(action, new Session(selectedRecipeIndex, remainingActions, false));
            }

            return new TickResult(action, new Session(selectedRecipeIndex, remainingActions - 1, true));
        }
    }

    public record TickResult(Action action, Session session) {
    }
}
