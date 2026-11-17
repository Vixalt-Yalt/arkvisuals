package dev.arkvisuals.config;

public enum VisualFeature {
    MAGIC_TRAIL("Магический след", Group.PARTICLES),
    AURA_MOTES("Частицы ауры", Group.PARTICLES),
    FOOTSTEP_SPARKS("Искры шагов", Group.PARTICLES),
    LANDING_BURST("Всплеск при приземлении", Group.PARTICLES),
    JUMP_RIBBON("Искры прыжка", Group.PARTICLES),
    WATER_BUBBLES("Пузырьки в воде", Group.PARTICLES),
    CRITICAL_SPARKS("Искры критического удара", Group.PARTICLES),
    HEALING_GLOW("Свечение при лечении", Group.PARTICLES),
    LOW_HEALTH_PULSE("Пульс при низком здоровье", Group.HUD),
    COORDINATES("Координаты", Group.HUD),
    CLOCK("Игровое время", Group.HUD),
    DIRECTION("Компас направления", Group.HUD),
    ARMOR_STATUS("Состояние брони", Group.HUD),
    XP_STATUS("Уровень и опыт", Group.HUD),
    CROSSHAIR_GLOW("Подсветка прицела", Group.HUD),
    HOTBAR_ACCENT("Акцент панели быстрого доступа", Group.HUD),
    BIOME_MOTES("Лесные огоньки", Group.WORLD),
    RAIN_SPARKS("Искры дождя", Group.WORLD),
    NIGHT_STARS("Ночное звездное мерцание", Group.WORLD),
    PORTAL_AURA("Портальная аура", Group.WORLD),
    SOUL_FLAME("Душевное пламя", Group.WORLD),
    HEARTBEAT("Пульсация сердец", Group.WORLD),
    STATUS_SWIRL("Частицы эффектов", Group.WORLD),
    DAMAGE_FLASH("Мягкая вспышка урона", Group.WORLD);

    public enum Group { PARTICLES, HUD, WORLD }

    private final String title;
    private final Group group;

    VisualFeature(String title, Group group) {
        this.title = title;
        this.group = group;
    }

    public String title() { return title; }
    public Group group() { return group; }
}
