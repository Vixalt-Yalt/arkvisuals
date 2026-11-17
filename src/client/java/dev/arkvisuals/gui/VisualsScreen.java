package dev.arkvisuals.gui;

import dev.arkvisuals.ArkVisualsClient;
import dev.arkvisuals.config.VisualFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public final class VisualsScreen extends Screen {
    private static final VisualFeature.Group[] PAGES = VisualFeature.Group.values();
    private static final String[] PAGE_NAMES = {"Частицы", "HUD", "Мир и природа"};
    private static final int PANEL_HEIGHT = 360;
    private static final int VISIBLE_FEATURES = 6;
    private final Screen parent;
    private int page;
    private int featureOffset;
    private boolean draggingScrollbar;

    public VisualsScreen(Screen parent) {
        super(Text.literal("ArkVisuals — настройки визуалов"));
        this.parent = parent;
    }

    private List<VisualFeature> pageFeatures() {
        return Arrays.stream(VisualFeature.values()).filter(feature -> feature.group() == PAGES[page]).toList();
    }

    @Override
    protected void init() {
        clearChildren();
        int panelWidth = Math.min(520, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int panelTop = Math.max(6, (this.height - PANEL_HEIGHT) / 2);
        int maxOffset = Math.max(0, pageFeatures().size() - VISIBLE_FEATURES);
        featureOffset = Math.max(0, Math.min(featureOffset, maxOffset));
        int buttonWidth = panelWidth - 48;
        List<VisualFeature> entries = pageFeatures();
        int visible = Math.min(VISIBLE_FEATURES, entries.size() - featureOffset);
        for (int i = 0; i < visible; i++) {
            VisualFeature feature = entries.get(featureOffset + i);
            int y = panelTop + 64 + i * 23;
            String state = ArkVisualsClient.CONFIG.enabled(feature) ? "●  ВКЛ" : "○  ВЫКЛ";
            addDrawableChild(ButtonWidget.builder(Text.literal(feature.title() + "     " + state), button -> {
                ArkVisualsClient.CONFIG.toggle(feature);
                init();
            }).dimensions(left + 12, y, buttonWidth, 21).build());
        }

        int controlY = panelTop + 209;
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
        addDrawableChild(ButtonWidget.builder(Text.literal("‹  Предыдущая"), button -> changePage(-1))
                .dimensions(left + 12, panelTop + 266, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Следующая  ›"), button -> changePage(1))
                .dimensions(left + 20 + controlW, panelTop + 266, controlW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово  ·  Esc"), button -> close())
                .dimensions(this.width / 2 - 62, panelTop + 293, 124, 22).build());
    }

    private void changePage(int direction) {
        page = Math.floorMod(page + direction, PAGES.length);
        featureOffset = 0;
        init();
    }

    private void scrollFeatures(int direction) {
        int maxOffset = Math.max(0, pageFeatures().size() - VISIBLE_FEATURES);
        int nextOffset = Math.max(0, Math.min(maxOffset, featureOffset + direction));
        if (nextOffset != featureOffset) {
            featureOffset = nextOffset;
            init();
        }
    }

    private static String densityName() {
        return switch (ArkVisualsClient.CONFIG.density()) { case 0 -> "Минимальная"; case 1 -> "Низкая"; case 2 -> "Средняя"; default -> "Высокая"; };
    }
    private static String scaleName() {
        return switch (ArkVisualsClient.CONFIG.scale()) { case 0 -> "Компактный"; case 1 -> "Малый"; case 2 -> "Обычный"; default -> "Крупный"; };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelTop = Math.max(6, (this.height - PANEL_HEIGHT) / 2);
        if (verticalAmount != 0 && mouseY >= panelTop + 60 && mouseY <= panelTop + 204) {
            scrollFeatures(verticalAmount < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollFeatures(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollFeatures(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setOffsetFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbar && button == 0) {
            setOffsetFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int left = (this.width - Math.min(520, this.width - 24)) / 2;
        int panelWidth = Math.min(520, this.width - 24);
        int top = Math.max(6, (this.height - PANEL_HEIGHT) / 2) + 64;
        return mouseX >= left + panelWidth - 25 && mouseX <= left + panelWidth - 8
                && mouseY >= top && mouseY <= top + VISIBLE_FEATURES * 23 - 2;
    }

    private void setOffsetFromMouse(double mouseY) {
        int maxOffset = Math.max(0, pageFeatures().size() - VISIBLE_FEATURES);
        if (maxOffset == 0) return;
        double top = Math.max(6, (this.height - PANEL_HEIGHT) / 2) + 64;
        double trackHeight = VISIBLE_FEATURES * 23 - 2;
        double ratio = Math.max(0, Math.min(1, (mouseY - top) / trackHeight));
        int nextOffset = (int) Math.round(ratio * maxOffset);
        if (nextOffset != featureOffset) {
            featureOffset = nextOffset;
            init();
            draggingScrollbar = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(520, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int panelTop = Math.max(6, (this.height - PANEL_HEIGHT) / 2);
        context.fill(left, panelTop, left + panelWidth, panelTop + PANEL_HEIGHT, 0xE8101228);
        context.fill(left, panelTop, left + panelWidth, panelTop + 2, ArkVisualsClient.CONFIG.color() | 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer, "ARK VISUALS", width / 2, panelTop + 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Персональная студия эффектов  ·  N — открыть в игре", width / 2, panelTop + 29, 0xFFB9C7E8);
        List<VisualFeature> entries = pageFeatures();
        int end = Math.min(entries.size(), featureOffset + VISIBLE_FEATURES);
        context.drawCenteredTextWithShadow(textRenderer,
                PAGE_NAMES[page] + "   ·   " + (page + 1) + "/" + PAGES.length + "   ·   " + (featureOffset + 1) + "–" + end + "/" + entries.size(),
                width / 2, panelTop + 47, ArkVisualsClient.CONFIG.color() | 0xFF000000);

        int trackX = left + panelWidth - 19;
        int trackY = panelTop + 64;
        int trackHeight = VISIBLE_FEATURES * 23 - 2;
        int maxOffset = Math.max(0, entries.size() - VISIBLE_FEATURES);
        int thumbHeight = entries.size() <= VISIBLE_FEATURES ? trackHeight : Math.max(26, trackHeight * VISIBLE_FEATURES / entries.size());
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + (maxOffset == 0 ? 0 : thumbTravel * featureOffset / maxOffset);
        context.fill(trackX + 5, trackY, trackX + 9, trackY + trackHeight, 0x99515B7A);
        context.fill(trackX + 2, thumbY, trackX + 12, thumbY + thumbHeight, ArkVisualsClient.CONFIG.color() | 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer, "Колесо мыши или ↑↓ — прокрутка списка", width / 2,
                panelTop + 342, 0xFF8991B2);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
