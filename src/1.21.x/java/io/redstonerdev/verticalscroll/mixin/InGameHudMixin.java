package io.redstonerdev.verticalscroll.mixin;

import io.redstonerdev.verticalscroll.VerticalScrollConfig;
import io.redstonerdev.verticalscroll.VerticalScrollMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin {

    // Each slot is 20x20, matching the hotbar slot size
    private static final int SLOT_SIZE = 20;
    private static final int GAP_OFF = 1;
    private static final int GAP_ON = 3;

    /**
     * Renders a vertical column of the 3 inventory rows above the hotbar whenever
     * the modifier key is held. The column is aligned with the currently selected
     * hotbar slot. Scroll indicators show which direction brings each row to hand:
     *
     *   ▲  [topRow]    ← scroll UP brings this to hotbar
     *      [midRow]
     *   ▼  [botRow]    ← scroll DOWN brings this to hotbar
     *      [hotbar]    (existing, not redrawn)
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void verticalscroll_renderColumnHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;
        if (!VerticalScrollMod.modifierKey.isPressed()) return;

        PlayerInventory inventory = client.player.getInventory();
        int hotbarIndex = ((PlayerInventoryAccessor) inventory).getSelectedSlot();

        int scaledWidth  = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();

        // X coordinate of the selected hotbar slot (left edge), matching Minecraft's hotbar layout
        int slotX = scaledWidth / 2 - 90 + hotbarIndex * SLOT_SIZE;

        // Inventory slot indices for this column (displayed top → bottom on screen)
        // topRow = top of main inventory (farthest from hotbar)
        // botRow = bottom of main inventory (just above hotbar)
        int[] inventorySlots = {
            hotbarIndex + 9,   // topRow  (i=0, top of column)
            hotbarIndex + 18,  // midRow  (i=1)
            hotbarIndex + 27,  // botRow  (i=2, bottom of column)
        };

        int columnHeight = inventorySlots.length * SLOT_SIZE;
        int columnTop = scaledHeight - 22 - (VerticalScrollConfig.get().gapEnabled ? GAP_ON : GAP_OFF) - columnHeight;

        // Outer background panel (slightly wider/taller for padding)
        context.fill(
            slotX - 1, columnTop - 1,
            slotX + SLOT_SIZE + 1, scaledHeight - 22 - (VerticalScrollConfig.get().gapEnabled ? GAP_ON : GAP_OFF),
            0xAA000000
        );

        for (int i = 0; i < inventorySlots.length; i++) {
            int slotY = columnTop + i * SLOT_SIZE;

            // Slot border: dark top/left edge (shadow), light bottom/right edge (highlight)
            context.fill(slotX,              slotY,              slotX + SLOT_SIZE,     slotY + 1,              0xFF373737); // top
            context.fill(slotX,              slotY,              slotX + 1,             slotY + SLOT_SIZE,      0xFF373737); // left
            context.fill(slotX,              slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE,      0xFF8B8B8B); // bottom
            context.fill(slotX + SLOT_SIZE - 1, slotY,          slotX + SLOT_SIZE,     slotY + SLOT_SIZE,      0xFF8B8B8B); // right

            // Slot interior
            context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0xFF555555);

            // Item stack (offset by 2 to stay within the 1px border, leaving a 1px inner gap)
            ItemStack stack = inventory.getStack(inventorySlots[i]);
            context.drawItem(stack, slotX + 2, slotY + 2);
            context.drawStackOverlay(client.textRenderer, stack, slotX + 2, slotY + 2);
        }

        // Scroll direction arrows, drawn to the left of the column
        int arrowX = slotX - 9;

        // ▲ beside topRow — scroll UP brings this item to the hotbar
        context.drawText(
            client.textRenderer,
            Text.literal("▲"),
            arrowX, columnTop + 6,
            0xFFFFFFFF, true
        );

        // ▼ beside botRow — scroll DOWN brings this item to the hotbar
        context.drawText(
            client.textRenderer,
            Text.literal("▼"),
            arrowX, columnTop + 2 * SLOT_SIZE + 6,
            0xFFFFFFFF, true
        );
    }
}
