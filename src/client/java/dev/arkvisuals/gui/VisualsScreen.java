package dev.arkvisuals.gui;

import dev.arkvisuals.ArkVisualsClient;
import dev.arkvisuals.config.VisualFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

public final class VisualsScreen extends Screen {
    private static final VisualFeature.Group[] PAGES = VisualFeature.Group.values();
    private static final String[] PAGE_NAMES = {"Частицы", "HUD", "Мир и эффекты"};
    private final Screen parent;
    private int page;

    public VisualsScreen(Screen parent) {
        super(Text.literal("ArkVisuals — настройки визуалов"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        int panelWidth = Math.min(520, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int panelTop = Math.max(8, (this.height - 310) / 2);
        int buttonWidth = (panelWidth - 38) / 2;
        List<VisualFeature> entries = Arrays.stream(VisualFeature.values()).filter(f -> f.group() == PAGES[page]).toList();
        for (int i = 0; i < entries.size(); i++) {
            VisualFeature feature = entries.get(i);
            int col = i % 2;
            int row = i / 2;
            int x = left + 12 + col * (buttonWidth + 8);
            int y = panelTop + 68 + row * 27;
            String state = ArkVisualsClient.CONFIG.enabled(feature) ? "●  ВКЛ" : "○  ВЫКЛ";
            addDrawableChild(ButtonWidget.builder(Text.literal(feature.title() + "   " + state), button -> {
                ArkVisualsClient.CONFIG.toggle(feature);
                init();
            }).dimensions(x, y, buttonWidth, 22).build());
        }

        int controlY = panelTop + 198;
        int controlW = (panelWidth - 38) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("✦  Цвет: " + ArkVisualsClient.CONFIG.colorName()), button -> {
            ArkVisualsClient.CONFIG.nextColor(); init();
        }).dimensions(left + 12, controlY, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("✧  Плотность: " + densityName()), button -> {
            ArkVisualsClient.CONFIG.cycleDensity(); init();
        }).dimensions(left + 20 + controlW, controlY, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("◈  Масштаб HUD: " + scaleName()), button -> {
            ArkVisualsClient.CONFIG.cycleScale(); init();
        }).dimensions(left + 12, controlY + 25, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▤  Угол: " + ArkVisualsClient.CONFIG.cornerName()), button -> {
            ArkVisualsClient.CONFIG.cycleHudCorner(); init();
        }).dimensions(left + 20 + controlW, controlY + 25, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("‹  Предыдущая"), button -> { page = Math.floorMod(page - 1, PAGES.length); init(); })
                .dimensions(left + 12, panelTop + 255, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Следующая  ›"), button -> { page = (page + 1) % PAGES.length; init(); })
                .dimensions(left + 20 + controlW, panelTop + 255, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово  ·  Esc"), button -> close())
                .dimensions(this.width / 2 - 62, panelTop + 281, 124, 22).build());
    }

    private static String densityName() {
        return switch (ArkVisualsClient.CONFIG.density()) { case 0 -> "Минимальная"; case 1 -> "Низкая"; case 2 -> "Средняя"; default -> "Высокая"; };
    }
    private static String scaleName() {
        return switch (ArkVisualsClient.CONFIG.scale()) { case 0 -> "Компактный"; case 1 -> "Малый"; case 2 -> "Обычный"; default -> "Крупный"; };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(520, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int panelTop = Math.max(8, (this.height - 310) / 2);
        context.fill(left, panelTop, left + panelWidth, panelTop + 310, 0xE8101228);
        context.fill(left, panelTop, left + panelWidth, panelTop + 2, ArkVisualsClient.CONFIG.color() | 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer, "ARK VISUALS", width / 2, panelTop + 14, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Персональная студия эффектов  ·  N — открыть в игре", width / 2, panelTop + 31, 0xFFB9C7E8);
        context.drawCenteredTextWithShadow(textRenderer, PAGE_NAMES[page] + "   ·   " + (page + 1) + "/" + PAGES.length,
                width / 2, panelTop + 49, ArkVisualsClient.CONFIG.color() | 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer, "Настройки сохраняются автоматически в config/arkvisuals.json", width / 2,
                panelTop + 294, 0xFF8991B2);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
