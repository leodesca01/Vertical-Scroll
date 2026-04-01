package io.redstonerdev.verticalscroll;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class VerticalScrollMod implements ClientModInitializer {

    public static KeyMapping modifierKey;

    @Override
    public void onInitializeClient() {
        VerticalScrollConfig.load();

        modifierKey = new KeyMapping(
                "key.verticalscroll.modifier",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath("verticalscroll", "category"))
        );
    }
}
