package dev.arkvisuals;

import dev.arkvisuals.config.VisualConfig;
import dev.arkvisuals.config.VisualFeature;
import dev.arkvisuals.gui.VisualsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ArkVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "arkvisuals";
    public static VisualConfig CONFIG;
    private static KeyBinding openSettings;
    private static float previousHealth = -1;
    private static boolean wasGrounded = true;
    private static int ticks;
    private static int damageTicks;

    @Override
    public void onInitializeClient() {
        CONFIG = VisualConfig.load();
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arkvisuals.open_settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.arkvisuals"));
        ClientTickEvents.END_CLIENT_TICK.register(ArkVisualsClient::tick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderHud(drawContext));
    }

    private static void tick(MinecraftClient client) {
        while (openSettings.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new VisualsScreen(null));
        }
        if (client.player == null || client.world == null) {
            previousHealth = -1;
            return;
        }
        var player = client.player;
        var world = client.world;
        Random random = world.random;
        ticks++;
        boolean grounded = player.isOnGround();
        boolean moving = player.getVelocity().horizontalLengthSquared() > 0.006;
        float health = player.getHealth();
        if (previousHealth >= 0 && health > previousHealth + 0.01f && CONFIG.enabled(VisualFeature.HEALING_GLOW)) {
            burst(world, player.getX(), player.getY() + 0.8, player.getZ(), ParticleTypes.HEART, 8 + CONFIG.density() * 3);
        }
        if (!wasGrounded && grounded && CONFIG.enabled(VisualFeature.LANDING_BURST)) {
            burst(world, player.getX(), player.getY() + 0.08, player.getZ(), ParticleTypes.CLOUD, 7 + CONFIG.density() * 3);
            burst(world, player.getX(), player.getY() + 0.15, player.getZ(), ParticleTypes.END_ROD, CONFIG.density() + 1);
        }
        if (wasGrounded && !grounded && CONFIG.enabled(VisualFeature.JUMP_RIBBON)) {
            burst(world, player.getX(), player.getY() + 0.12, player.getZ(), ParticleTypes.ENCHANT, 4 + CONFIG.density() * 2);
        }
        if (moving && grounded && ticks % Math.max(2, 7 - CONFIG.density()) == 0) {
            if (CONFIG.enabled(VisualFeature.MAGIC_TRAIL)) spawn(world, player.getX(), player.getY() + 0.16, player.getZ(), sparkle(), random);
            if (CONFIG.enabled(VisualFeature.FOOTSTEP_SPARKS)) {
                double side = random.nextBoolean() ? 0.19 : -0.19;
                spawn(world, player.getX() + side * Math.cos(Math.toRadians(player.getYaw())), player.getY() + 0.05,
                        player.getZ() + side * Math.sin(Math.toRadians(player.getYaw())), ParticleTypes.END_ROD, random);
            }
        }
        if (CONFIG.enabled(VisualFeature.AURA_MOTES) && ticks % Math.max(3, 9 - CONFIG.density() * 2) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.38 + random.nextDouble() * 0.42;
            spawn(world, player.getX() + Math.cos(angle) * radius, player.getY() + random.nextDouble() * 1.6,
                    player.getZ() + Math.sin(angle) * radius, sparkle(), random);
        }
        if (player.isSubmergedInWater() && CONFIG.enabled(VisualFeature.WATER_BUBBLES) && ticks % 3 == 0)
            spawn(world, player.getX() + random.nextDouble() - 0.5, player.getY() + random.nextDouble() * 1.5,
                    player.getZ() + random.nextDouble() - 0.5, ParticleTypes.BUBBLE, random);
        if (CONFIG.enabled(VisualFeature.BIOME_MOTES) && player.getWorld().getLightLevel(player.getBlockPos()) < 10
                && ticks % Math.max(4, 12 - CONFIG.density() * 2) == 0) {
            spawn(world, player.getX() + random.nextDouble() * 8 - 4, player.getY() + random.nextDouble() * 3,
                    player.getZ() + random.nextDouble() * 8 - 4, ParticleTypes.HAPPY_VILLAGER, random);
        }
        // Sample loaded nearby foliage rather than attaching every ambient effect to the player.
        if (CONFIG.enabled(VisualFeature.FOLIAGE_WHISPERS) && ticks % Math.max(4, 11 - CONFIG.density() * 2) == 0) {
            double x = player.getX() + random.nextDouble() * 18 - 9;
            double y = player.getY() + random.nextDouble() * 8 - 3;
            double z = player.getZ() + random.nextDouble() * 18 - 9;
            BlockPos sample = BlockPos.ofFloored(x, y, z);
            if (world.getBlockState(sample).isIn(BlockTags.LEAVES)) {
                world.addParticle(ParticleTypes.CHERRY_LEAVES, sample.getX() + .5, sample.getY() + .3, sample.getZ() + .5,
                        (random.nextDouble() - .5) * .02, -.015, (random.nextDouble() - .5) * .02);
            }
        }
        if (CONFIG.enabled(VisualFeature.WATER_SHIMMER) && ticks % 5 == 0) {
            double x = player.getX() + random.nextDouble() * 16 - 8;
            double y = player.getY() + random.nextDouble() * 5 - 2;
            double z = player.getZ() + random.nextDouble() * 16 - 8;
            BlockPos sample = BlockPos.ofFloored(x, y, z);
            if (world.getBlockState(sample).isOf(Blocks.WATER)) {
                spawn(world, sample.getX() + random.nextDouble(), sample.getY() + .7,
                        sample.getZ() + random.nextDouble(), ParticleTypes.END_ROD, random);
            }
        }
        if (CONFIG.enabled(VisualFeature.FAUNA_AURA) && ticks % Math.max(8, 20 - CONFIG.density() * 3) == 0) {
            var nearbyAnimals = world.getEntitiesByClass(AnimalEntity.class, player.getBoundingBox().expand(12, 5, 12),
                    animal -> animal.isAlive());
            if (!nearbyAnimals.isEmpty()) {
                AnimalEntity animal = nearbyAnimals.get(random.nextInt(nearbyAnimals.size()));
                spawn(world, animal.getX() + (random.nextDouble() - .5) * animal.getWidth(),
                        animal.getY() + animal.getHeight() * (.4 + random.nextDouble() * .5),
                        animal.getZ() + (random.nextDouble() - .5) * animal.getWidth(),
                        ParticleTypes.HAPPY_VILLAGER, random);
            }
        }
        if (CONFIG.enabled(VisualFeature.RAIN_SPARKS) && world.isRaining() && ticks % 5 == 0)
            spawn(world, player.getX() + random.nextDouble() * 10 - 5, player.getY() + 2 + random.nextDouble() * 2,
                    player.getZ() + random.nextDouble() * 10 - 5, ParticleTypes.END_ROD, random);
        if (CONFIG.enabled(VisualFeature.NIGHT_STARS) && world.getTimeOfDay() % 24000 >= 13000
                && world.getTimeOfDay() % 24000 < 23000 && ticks % 12 == 0)
            spawn(world, player.getX() + random.nextDouble() * 12 - 6, player.getY() + 3 + random.nextDouble() * 3,
                    player.getZ() + random.nextDouble() * 12 - 6, ParticleTypes.END_ROD, random);
        if (CONFIG.enabled(VisualFeature.PORTAL_AURA) && player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NAUSEA)
                && ticks % 4 == 0)
            spawn(world, player.getX() + random.nextDouble() - .5, player.getY() + random.nextDouble() * 1.7,
                    player.getZ() + random.nextDouble() - .5, ParticleTypes.PORTAL, random);
        if (CONFIG.enabled(VisualFeature.SOUL_FLAME) && player.getHealth() <= player.getMaxHealth() * .3f && ticks % 7 == 0)
            spawn(world, player.getX() + random.nextDouble() - .5, player.getY() + .1,
                    player.getZ() + random.nextDouble() - .5, ParticleTypes.SOUL, random);
        if (CONFIG.enabled(VisualFeature.STATUS_SWIRL) && !player.getStatusEffects().isEmpty() && ticks % 8 == 0)
            spawn(world, player.getX() + random.nextDouble() - .5, player.getY() + .6 + random.nextDouble(),
                    player.getZ() + random.nextDouble() - .5, ParticleTypes.ENCHANT, random);
        if (CONFIG.enabled(VisualFeature.CRITICAL_SPARKS) && player.fallDistance > 0.2f
                && player.getVelocity().y < -0.06 && ticks % 2 == 0)
            burst(world, player.getX(), player.getY() + 0.25, player.getZ(), ParticleTypes.CRIT, CONFIG.density() + 2);
        wasGrounded = grounded;
        previousHealth = health;
        if (player.hurtTime > 0) damageTicks = 9;
        else if (damageTicks > 0) damageTicks--;
    }

    private static ParticleEffect sparkle() {
        return switch (CONFIG.paletteIndexForParticle()) {
            case 1, 5 -> ParticleTypes.SOUL;
            case 2, 7 -> ParticleTypes.WITCH;
            case 3, 4 -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static void spawn(net.minecraft.client.world.ClientWorld world, double x, double y, double z,
                              ParticleEffect particle, Random random) {
        world.addParticle(particle, x, y, z, (random.nextDouble() - .5) * .012, 0.015 + random.nextDouble() * .02,
                (random.nextDouble() - .5) * .012);
    }

    private static void burst(net.minecraft.client.world.ClientWorld world, double x, double y, double z,
                              ParticleEffect particle, int amount) {
        for (int i = 0; i < amount; i++) world.addParticle(particle,
                x + (world.random.nextDouble() - .5) * .85, y + world.random.nextDouble() * .5,
                z + (world.random.nextDouble() - .5) * .85, (world.random.nextDouble() - .5) * .07,
                world.random.nextDouble() * .055, (world.random.nextDouble() - .5) * .07);
    }

    private static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        var player = client.player;
        int color = CONFIG.color() | 0xFF000000;
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        if (CONFIG.enabled(VisualFeature.HOTBAR_ACCENT)) {
            int cx = width / 2;
            context.fill(cx - 92, height - 24, cx + 92, height - 23, (90 << 24) | CONFIG.color());
        }
        if (CONFIG.enabled(VisualFeature.LOW_HEALTH_PULSE) && player.getHealth() <= player.getMaxHealth() * .3f) {
            int alpha = (int) (28 + (Math.sin(client.world.getTime() * .35) + 1) * 22);
            context.fill(0, 0, width, height, (alpha << 24) | 0xFF3355);
        }
        if (CONFIG.enabled(VisualFeature.HEARTBEAT) && player.getHealth() < player.getMaxHealth() && player.getHealth() > player.getMaxHealth() * .3f) {
            int alpha = (int) (10 + (Math.sin(client.world.getTime() * .22) + 1) * 8);
            context.fill(0, 0, width, height, (alpha << 24) | CONFIG.color());
        }
        if (CONFIG.enabled(VisualFeature.DAMAGE_FLASH) && damageTicks > 0) {
            int alpha = damageTicks * 4;
            context.fill(0, 0, width, height, (alpha << 24) | 0xFF526B);
        }
        List<Text> lines = new ArrayList<>();
        if (CONFIG.enabled(VisualFeature.COORDINATES)) {
            var pos = player.getBlockPos();
            lines.add(Text.literal("XYZ  " + pos.getX() + "  " + pos.getY() + "  " + pos.getZ()));
        }
        if (CONFIG.enabled(VisualFeature.CLOCK)) {
            long dayTime = Math.floorMod(client.world.getTimeOfDay() + 6000, 24000);
            lines.add(Text.literal(String.format("☾  %02d:%02d", dayTime / 1000, (dayTime % 1000) * 60 / 1000)));
        }
        if (CONFIG.enabled(VisualFeature.DIRECTION)) lines.add(Text.literal("↗  " + player.getHorizontalFacing().getName().toUpperCase()));
        if (CONFIG.enabled(VisualFeature.ARMOR_STATUS)) lines.add(Text.literal("◆  Броня  " + player.getArmor() + "/20"));
        if (CONFIG.enabled(VisualFeature.XP_STATUS)) lines.add(Text.literal("✦  Ур. " + player.experienceLevel + "  " + Math.round(player.experienceProgress * 100) + "%"));
        if (!lines.isEmpty()) {
            int margin = 8;
            int lineHeight = 8 + CONFIG.scale() * 2;
            boolean right = CONFIG.hudCorner() == 1 || CONFIG.hudCorner() == 3;
            boolean bottom = CONFIG.hudCorner() >= 2;
            int startY = bottom ? height - margin - lines.size() * lineHeight : margin;
            for (int i = 0; i < lines.size(); i++) {
                Text line = lines.get(i);
                int x = right ? width - margin - client.textRenderer.getWidth(line) : margin;
                int y = startY + i * lineHeight;
                context.fill(x - 3, y - 2, x + client.textRenderer.getWidth(line) + 3, y + lineHeight - 1,
                        0x8A0A0C20);
                context.drawTextWithShadow(client.textRenderer, line, x, y, color);
            }
        }
    }
}
