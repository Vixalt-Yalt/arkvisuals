package dev.arkvisuals.gui;

import dev.arkvisuals.ArkVisualsClient;
import dev.arkvisuals.config.VisualFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public final class VisualsScreen extends Screen {
    private static final VisualFeature.Group[] PAGES = VisualFeature.Group.values();
    private static final String[] PAGE_NAMES = {"Частицы", "HUD", "Мир и природа"};
    private static final int MAX_PANEL_HEIGHT = 360;
    private static final int MAX_VISIBLE_FEATURES = 6;
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

    private int panelWidth() { return Math.min(520, this.width - 24); }
    private int panelHeight() { return Math.min(MAX_PANEL_HEIGHT, Math.max(210, this.height - 12)); }
    private int panelTop() { return Math.max(6, (this.height - panelHeight()) / 2); }
    private int visibleFeatureCount() {
        return Math.max(1, Math.min(MAX_VISIBLE_FEATURES, (panelHeight() - 178) / 23));
    }

    @Override
    protected void init() {
        clearChildren();
        int left = (this.width - panelWidth()) / 2;
        int top = panelTop();
        int visibleLimit = visibleFeatureCount();
        List<VisualFeature> entries = pageFeatures();
        int maxOffset = Math.max(0, entries.size() - visibleLimit);
        featureOffset = Math.max(0, Math.min(featureOffset, maxOffset));
        int visible = Math.min(visibleLimit, entries.size() - featureOffset);
        int buttonWidth = panelWidth() - 48;
        for (int i = 0; i < visible; i++) {
            VisualFeature feature = entries.get(featureOffset + i);
            String state = ArkVisualsClient.CONFIG.enabled(feature) ? "●  ВКЛ" : "○  ВЫКЛ";
            addArkButton(left + 12, top + 64 + i * 23, buttonWidth, 21,
                    Text.literal(feature.title() + "     " + state), button -> {
                        ArkVisualsClient.CONFIG.toggle(feature);
                        init();
                    });
        }

        int controlY = top + 64 + visible * 23 + 10;
        int controlW = (panelWidth() - 38) / 2;
        addArkButton(left + 12, controlY, controlW, 22,
                Text.literal("✦  Цвет: " + ArkVisualsClient.CONFIG.colorName()), button -> {
                    ArkVisualsClient.CONFIG.nextColor(); init();
                });
        addArkButton(left + 20 + controlW, controlY, controlW, 22,
                Text.literal("✧  Плотность: " + densityName()), button -> {
                    ArkVisualsClient.CONFIG.cycleDensity(); init();
                });
        addArkButton(left + 12, controlY + 25, controlW, 22,
                Text.literal("◈  Масштаб HUD: " + scaleName()), button -> {
                    ArkVisualsClient.CONFIG.cycleScale(); init();
                });
        addArkButton(left + 20 + controlW, controlY + 25, controlW, 22,
                Text.literal("▤  Угол: " + ArkVisualsClient.CONFIG.cornerName()), button -> {
                    ArkVisualsClient.CONFIG.cycleHudCorner(); init();
                });
        int navigationY = controlY + 53;
        addArkButton(left + 12, navigationY, controlW, 22, Text.literal("‹  Предыдущая"), button -> changePage(-1));
        addArkButton(left + 20 + controlW, navigationY, controlW, 22, Text.literal("Следующая  ›"), button -> changePage(1));
        addArkButton(this.width / 2 - 62, navigationY + 27, 124, 22,
                Text.literal("Готово  ·  Esc"), button -> close());
    }

    private void addArkButton(int x, int y, int width, int height, Text text, net.minecraft.client.gui.widget.ButtonWidget.PressAction action) {
        addDrawableChild(new ArkButtonWidget(x, y, width, height, text, action));
    }

    private void changePage(int direction) {
        page = Math.floorMod(page + direction, PAGES.length);
        featureOffset = 0;
        init();
    }

    private void scrollFeatures(int direction) {
        int maxOffset = Math.max(0, pageFeatures().size() - visibleFeatureCount());
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
        int top = panelTop() + 60;
        int bottom = top + visibleFeatureCount() * 23 + 6;
        if (verticalAmount != 0 && mouseY >= top && mouseY <= bottom) {
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

    private int scrollbarTrackHeight() { return Math.max(10, visibleFeatureCount() * 23 - 2); }
    private int scrollbarThumbHeight() {
        List<VisualFeature> entries = pageFeatures();
        int visible = visibleFeatureCount();
        return Math.min(scrollbarTrackHeight(), Math.max(14, scrollbarTrackHeight() * visible / entries.size()));
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int left = (this.width - panelWidth()) / 2;
        int x = left + panelWidth() - 22;
        int y = panelTop() + 64;
        return mouseX >= x && mouseX <= x + 15 && mouseY >= y && mouseY <= y + scrollbarTrackHeight();
    }

    private void setOffsetFromMouse(double mouseY) {
        int maxOffset = Math.max(0, pageFeatures().size() - visibleFeatureCount());
        if (maxOffset == 0) return;
        double trackY = panelTop() + 64;
        int travel = scrollbarTrackHeight() - scrollbarThumbHeight();
        double ratio = travel <= 0 ? 0 : (mouseY - trackY - scrollbarThumbHeight() / 2.0) / travel;
        int nextOffset = (int) Math.round(Math.max(0, Math.min(1, ratio)) * maxOffset);
        if (nextOffset != featureOffset) {
            featureOffset = nextOffset;
            init();
            draggingScrollbar = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // A single, intentional overlay and panel layer: no vanilla blur or opaque gray button plates.
        context.fill(0, 0, width, height, 0xC9080C18);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = panelTop();
        int accent = ArkVisualsClient.CONFIG.color() | 0xFF000000;
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xF20D1224);
        context.fill(left, top, left + panelWidth, top + 1, 0xFF343B58);
        context.fill(left, top, left + 2, top + panelHeight, 0xFF343B58);
        context.fill(left, top, left + panelWidth, top + 3, accent);
        context.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, 0xFF343B58);
        context.drawCenteredTextWithShadow(textRenderer, "ARK VISUALS", width / 2, top + 12, 0xFFF8F7FF);
        context.drawCenteredTextWithShadow(textRenderer, "Студия визуальных эффектов  ·  N — настройки", width / 2, top + 29, 0xFFB9C7E8);

        List<VisualFeature> entries = pageFeatures();
        int visible = visibleFeatureCount();
        int end = Math.min(entries.size(), featureOffset + visible);
        context.drawCenteredTextWithShadow(textRenderer,
                PAGE_NAMES[page] + "  ·  " + (page + 1) + "/" + PAGES.length + "  ·  " + (featureOffset + 1) + "–" + end + "/" + entries.size(),
                width / 2, top + 47, accent);

        int trackX = left + panelWidth - 19;
        int trackY = top + 64;
        int trackHeight = scrollbarTrackHeight();
        int thumbHeight = scrollbarThumbHeight();
        int maxOffset = Math.max(0, entries.size() - visible);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + (maxOffset == 0 ? 0 : thumbTravel * featureOffset / maxOffset);
        context.fill(trackX + 5, trackY, trackX + 8, trackY + trackHeight, 0xFF30364C);
        context.fill(trackX + 2, thumbY, trackX + 11, thumbY + thumbHeight, accent);

        if (panelHeight >= 340) {
            context.drawCenteredTextWithShadow(textRenderer, "Колесо мыши или ↑↓ — прокрутка списка", width / 2,
                    top + panelHeight - 17, 0xFF8991B2);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
