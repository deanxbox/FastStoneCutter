package net.deanxbox.faststonecutter;

import net.deanxbox.faststonecutter.StonecutterAutomation.Action;
import net.deanxbox.faststonecutter.StonecutterAutomation.ActionType;
import net.deanxbox.faststonecutter.StonecutterAutomation.Session;
import net.deanxbox.faststonecutter.StonecutterAutomation.SlotSnapshot;
import net.deanxbox.faststonecutter.StonecutterAutomation.TickResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StonecutterAutomationTest {
    @Test
    void returnsNoneWhenRecipeIsNotSelected() {
        Action action = StonecutterAutomation.nextAction(-1, true, true, List.of(), 2, 38);

        assertEquals(Action.none(), action);
    }

    @Test
    void takesResultBeforeLoadingMoreMaterial() {
        Action action = StonecutterAutomation.nextAction(0, true, true, List.of(slot(10, true)), 2, 38);

        assertEquals(Action.takeResult(), action);
    }

    @Test
    void selectsRememberedRecipeWhenInputHasNoResultYet() {
        Action action = StonecutterAutomation.nextAction(0, true, false, List.of(slot(10, true)), 2, 38);

        assertEquals(Action.selectRecipe(), action);
    }

    @Test
    void loadsFirstMatchingSourceStackWhenInputIsEmpty() {
        Action action = StonecutterAutomation.nextAction(0, false, false, List.of(
                slot(4, false),
                slot(9, true),
                slot(12, true)
        ), 2, 38);

        assertEquals(new Action(ActionType.LOAD_SOURCE_SLOT, 9), action);
    }

    @Test
    void ignoresSlotsOutsidePlayerInventoryRange() {
        Action action = StonecutterAutomation.nextAction(0, false, false, List.of(
                slot(1, true),
                slot(38, true),
                slot(39, true)
        ), 2, 38);

        assertEquals(Action.none(), action);
    }

    @Test
    void stopsWhenNoMatchingSourceStackRemains() {
        Action action = StonecutterAutomation.nextAction(0, false, false, List.of(
                slot(2, false),
                slot(3, false)
        ), 2, 38);

        assertEquals(Action.none(), action);
    }

    @Test
    void sessionRunsOneActionPerTickAndKeepsSelectedRecipe() {
        Session session = StonecutterAutomation.start(3, 10);

        TickResult firstTick = session.next(true, true, List.of(slot(9, true)), 2, 38);
        TickResult secondTick = firstTick.session().next(false, false, List.of(slot(9, true)), 2, 38);

        assertEquals(Action.takeResult(), firstTick.action());
        assertEquals(new Action(ActionType.LOAD_SOURCE_SLOT, 9), secondTick.action());
        assertEquals(3, secondTick.session().selectedRecipeIndex());
        assertEquals(8, secondTick.session().remainingActions());

        TickResult thirdTick = secondTick.session().next(true, false, List.of(slot(9, false)), 2, 38);
        assertEquals(Action.selectRecipe(), thirdTick.action());
    }

    @Test
    void sessionStopsAfterNoActionIsAvailable() {
        Session session = StonecutterAutomation.start(0, 10);

        TickResult tick = session.next(false, false, List.of(slot(2, false)), 2, 38);

        assertEquals(Action.none(), tick.action());
        assertEquals(false, tick.session().active());
    }

    private static SlotSnapshot slot(int index, boolean matchingSource) {
        return new SlotSnapshot(index, matchingSource);
    }
}
