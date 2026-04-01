package io.redstonerdev.verticalscroll.mixin;

import io.redstonerdev.verticalscroll.VerticalScrollMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class MouseMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void verticalscroll_onMouseScroll(long window, double xDelta, double yDelta, CallbackInfo ci) {
        if (yDelta == 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (minecraft.screen != null) return;
        if (!VerticalScrollMod.modifierKey.isDown()) return;

        ci.cancel();
        rotateColumn(minecraft, yDelta > 0);
    }

    private static void rotateColumn(Minecraft minecraft, boolean scrollUp) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) return;

        Inventory inventory = player.getInventory();
        int hotbarIndex = ((PlayerInventoryAccessor) inventory).getSelectedSlot();

        int topRowSlot = 9  + hotbarIndex;
        int midRowSlot = 18 + hotbarIndex;
        int botRowSlot = 27 + hotbarIndex;
        int hotbarSlot = 36 + hotbarIndex;

        InventoryMenu handler = player.inventoryMenu;
        int containerId = handler.containerId;

        int[] slots = scrollUp
                ? new int[]{hotbarSlot, botRowSlot, midRowSlot, topRowSlot}
                : new int[]{topRowSlot, midRowSlot, botRowSlot, hotbarSlot};

        for (int slot : slots) {
            minecraft.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, player);
        }
        minecraft.gameMode.handleContainerInput(containerId, slots[0], 0, ContainerInput.PICKUP, player);
    }
}
