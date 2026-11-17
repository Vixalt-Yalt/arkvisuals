package dev.arkvisuals.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Flat, dark glass-style control matching the ArkVisuals panel. */
final class ArkButtonWidget extends ButtonWidget {
    ArkButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hover = isHovered();
        int background = !active ? 0xCC101522 : hover ? 0xFF242B43 : 0xE8151B2E;
        int edge = !active ? 0xFF34384A : hover ? 0xFF8C6DE8 : 0xFF343C5B;
        int foreground = active ? 0xFFF0F3FF : 0xFF858BA0;
        context.fill(getX(), getY(), getX() + width, getY() + height, background);
        context.fill(getX(), getY(), getX() + width, getY() + 1, edge);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, edge);
        context.fill(getX(), getY(), getX() + 2, getY() + height, hover ? 0xFF54DDF3 : 0xFF6F55C8);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, foreground);
    }
}
