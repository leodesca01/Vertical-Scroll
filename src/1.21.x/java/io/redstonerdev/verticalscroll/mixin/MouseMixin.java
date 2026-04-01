package io.redstonerdev.verticalscroll.mixin;

import io.redstonerdev.verticalscroll.VerticalScrollMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public class MouseMixin {

    /**
     * Intercepts mouse scroll events. When the modifier key (default: Left Alt) is held
     * and no screen is open, instead of scrolling the hotbar, we rotate items through
     * the vertical column of the currently selected hotbar slot.
     *
     * Column layout (PlayerScreenHandler screen slots):
     *   Top row    (farthest from hotbar): slot  9 + hotbarIndex
     *   Middle row:                        slot 18 + hotbarIndex
     *   Bottom row (closest to hotbar):    slot 27 + hotbarIndex
     *   Hotbar:                            slot 36 + hotbarIndex
     *
     * Scroll UP  → top-row item comes to hotbar (navigate up through the column).
     * Scroll DOWN → bottom-row item comes to hotbar (navigate down through the column).
     *
     * Rotation is achieved with 5 sequential left-click (PICKUP) actions using the cursor:
     *   1. Pick up slot[0]              (cursor ← A, slot[0] = empty)
     *   2. Click slot[1] → swap        (cursor ← B, slot[1] = A)
     *   3. Click slot[2] → swap        (cursor ← C, slot[2] = B)
     *   4. Click slot[3] → swap        (cursor ← D, slot[3] = C)
     *   5. Click slot[0] → place back  (cursor = empty, slot[0] = D)
     * Result: [slot0=D, slot1=A, slot2=B, slot3=C]
     */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void verticalscroll_onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical == 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Only activate when the player is playing (no open screen)
        if (client.currentScreen != null) return;

        // Check if our modifier key is currently held
        if (!VerticalScrollMod.modifierKey.isPressed()) return;

        // Consume the scroll event so the hotbar selection does not change
        ci.cancel();

        rotateColumn(client, vertical > 0);
    }

    /**
     * Rotates the items in the vertical column of the currently selected hotbar slot.
     *
     * @param client   the Minecraft client instance
     * @param scrollUp true if the mouse wheel was scrolled upward
     */
    private static void rotateColumn(MinecraftClient client, boolean scrollUp) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        PlayerInventory inventory = player.getInventory();
        int hotbarIndex = ((PlayerInventoryAccessor) inventory).getSelectedSlot(); // 0-8

        // Screen slot indices within PlayerScreenHandler (syncId = playerScreenHandler.syncId)
        int topRowSlot = 9  + hotbarIndex; // top row of main inventory
        int midRowSlot = 18 + hotbarIndex; // middle row
        int botRowSlot = 27 + hotbarIndex; // bottom row (just above hotbar)
        int hotbarSlot = 36 + hotbarIndex; // hotbar

        PlayerScreenHandler handler = player.playerScreenHandler;
        int syncId = handler.syncId;

        /*
         * Choose the rotation order.
         *
         * scrollUp=true  → top-row comes to hotbar (navigate up through column)
         *   Cursor rotation on [hotbar, bot, mid, top]:
         *   Result: hotbar=old-top, bot=old-hotbar, mid=old-bot, top=old-mid  ✓
         *
         * scrollUp=false → bottom-row comes to hotbar (navigate down through column)
         *   Cursor rotation on [top, mid, bot, hotbar]:
         *   Result: top=old-hotbar, mid=old-top, bot=old-mid, hotbar=old-bot  ✓
         */
        int[] slots = scrollUp
                ? new int[]{hotbarSlot, botRowSlot, midRowSlot, topRowSlot}
                : new int[]{topRowSlot, midRowSlot, botRowSlot, hotbarSlot};

        // 4 clicks to walk through all slots, then 1 final click on slots[0] to place
        for (int slot : slots) {
            client.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, player);
        }
        client.interactionManager.clickSlot(syncId, slots[0], 0, SlotActionType.PICKUP, player);
    }
}
