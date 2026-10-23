package co.blumeplugins.deathsareitems.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Development-only vertex capture. Included only through export.init.gradle, never in the mod jar. */
@Mod.EventBusSubscriber(modid = "deathsareitems", value = Dist.CLIENT)
public final class BlockbenchSnapshotExport {
    private static boolean done;
    private static final String[] ITEMS = {"fall_damage_boots", "drowning_mask", "suffocation_boulder",
            "skeleton_bow", "mob_pickaxe", "hunger_staff", "swarm_of_anvils", "entity_crammer",
            "cactus_chestplate", "tnt_cannon", "fire_pants", "bedrock", "ender_pearl_shield", "soul_storm"};
    private static final ItemDisplayContext[] CONTEXTS = {ItemDisplayContext.GUI,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            ItemDisplayContext.GROUND, ItemDisplayContext.FIXED, ItemDisplayContext.HEAD};
    private static final Pattern TEXTURE = Pattern.compile("(?:minecraft|deathsareitems):textures/[a-zA-Z0-9_./-]+\\.png");
    private static final Gson JSON = new GsonBuilder().create();

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (done || event.phase != TickEvent.Phase.END || mc.screen == null || mc.getOverlay() != null) return;
        done = true;
        try { export(mc); } catch (Exception exception) { exception.printStackTrace(); }
        mc.stop();
    }

    private static void export(Minecraft mc) throws Exception {
        Path output = Path.of("snapshots");
        Files.createDirectories(output);
        Files.deleteIfExists(output.resolve("SUCCESS"));
        Map<String, String> textures = new LinkedHashMap<>();
        TextureAtlas atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        List<TextureAtlasSprite> sprites = atlas.getTextureLocations().stream().map(atlas::getSprite).toList();
        for (String name : ITEMS) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("deathsareitems", name)));
            var model = mc.getItemRenderer().getModel(stack, null, null, 0);
            Map<String, Object> contexts = new LinkedHashMap<>();
            Map<String, Object> display = new LinkedHashMap<>();
            for (ItemDisplayContext context : CONTEXTS) {
                boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                ItemTransform transform = model.getTransforms().getTransform(context);
                PoseStack transformPose = new PoseStack();
                transform.apply(left, transformPose);
                Matrix4f inverse = new Matrix4f(transformPose.last().pose()).invert();
                Capture capture = new Capture(inverse, sprites);
                mc.getItemRenderer().render(stack, context, left, new PoseStack(), capture,
                        15728880, OverlayTexture.NO_OVERLAY, model);
                capture.finish();
                contexts.put(context.getSerializedName(), capture.quads);
                display.put(context.getSerializedName(), Map.of("rotation", vec(transform.rotation, 1),
                        "translation", vec(transform.translation, 16), "scale", vec(transform.scale, 1)));
                if (transform.rightRotation.lengthSquared() != 0) throw new IllegalStateException("Unsupported post rotation: " + name);
                for (String texture : capture.usedTextures) {
                    if (!textures.containsKey(texture)) {
                        try (var stream = mc.getResourceManager().getResourceOrThrow(new ResourceLocation(texture)).open()) {
                            textures.put(texture, "data:image/png;base64," + Base64.getEncoder().encodeToString(stream.readAllBytes()));
                        }
                    }
                }
            }
            Files.writeString(output.resolve(name + ".json"), JSON.toJson(Map.of("contexts", contexts, "display", display)));
            System.out.println("BLOCKBENCH_EXPORTED " + name);
        }
        Files.writeString(output.resolve("textures.json"), JSON.toJson(textures));
        Files.writeString(output.resolve("SUCCESS"), "14 models, 8 display contexts each\n");
    }

    private static List<Float> vec(Vector3f v, float scale) { return List.of(v.x * scale, v.y * scale, v.z * scale); }

    private static final class Capture implements MultiBufferSource {
        final Matrix4f inverse;
        final List<TextureAtlasSprite> sprites;
        final List<Object> quads = new ArrayList<>();
        final Set<String> usedTextures = new LinkedHashSet<>();
        final Map<String, Sink> sinks = new LinkedHashMap<>();
        Capture(Matrix4f inverse, List<TextureAtlasSprite> sprites) { this.inverse = inverse; this.sprites = sprites; }
        @Override public VertexConsumer getBuffer(RenderType type) {
            // Glint is lighting, not model geometry.
            if (type.toString().contains("glint")) return new Sink(null);
            Matcher matcher = TEXTURE.matcher(type.toString());
            if (!matcher.find()) throw new IllegalStateException("Unknown render material: " + type);
            String texture = matcher.group();
            return sinks.computeIfAbsent(texture, Sink::new);
        }
        void finish() { for (Sink sink : sinks.values()) if (!sink.vertices.isEmpty()) throw new IllegalStateException("Incomplete quad"); }
        private final class Sink implements VertexConsumer {
            final String texture;
            final List<List<Double>> vertices = new ArrayList<>();
            double x, y, z; float u, v;
            Sink(String texture) { this.texture = texture; }
            @Override public VertexConsumer vertex(double x, double y, double z) { this.x=x; this.y=y; this.z=z; return this; }
            @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
            @Override public VertexConsumer uv(float u, float v) { this.u=u; this.v=v; return this; }
            @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
            @Override public VertexConsumer uv2(int u, int v) { return this; }
            @Override public VertexConsumer normal(float x, float y, float z) { return this; }
            @Override public void defaultColor(int r, int g, int b, int a) { }
            @Override public void unsetDefaultColor() { }
            @Override public void endVertex() {
                if (texture == null) return;
                Vector3f p = inverse.transformPosition(new Vector3f((float)x, (float)y, (float)z));
                vertices.add(List.of((double)p.x * 16 + 8, (double)p.y * 16 + 8, (double)p.z * 16 + 8, (double)u, (double)v));
                if (vertices.size() != 4) return;
                String resolved = texture;
                List<List<Double>> quad = new ArrayList<>(vertices);
                if (texture.equals(TextureAtlas.LOCATION_BLOCKS.toString())) {
                    double cu = vertices.stream().mapToDouble(a -> a.get(3)).average().orElseThrow();
                    double cv = vertices.stream().mapToDouble(a -> a.get(4)).average().orElseThrow();
                    TextureAtlasSprite sprite = sprites.stream().filter(s -> cu >= s.getU0() && cu <= s.getU1()
                            && cv >= s.getV0() && cv <= s.getV1()).findFirst().orElseThrow();
                    ResourceLocation id = sprite.contents().name();
                    resolved = id.getNamespace() + ":textures/" + id.getPath() + ".png";
                    quad = vertices.stream().map(a -> List.of(a.get(0), a.get(1), a.get(2),
                            (a.get(3) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
                            (a.get(4) - sprite.getV0()) / (sprite.getV1() - sprite.getV0()))).toList();
                }
                usedTextures.add(resolved);
                quads.add(Map.of("texture", resolved, "vertices", quad));
                vertices.clear();
            }
        }
    }
}
