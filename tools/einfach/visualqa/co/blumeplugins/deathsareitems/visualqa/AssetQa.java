package co.blumeplugins.deathsareitems.visualqa;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Development-only real GPU bake/GUI/foil/reload smoke test; no saves are loaded. */
@Mod.EventBusSubscriber(modid=DeathsAreItems.MOD_ID, value=Dist.CLIENT)
public final class AssetQa {
    private static boolean started;
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if (!started && event.phase==TickEvent.Phase.END && mc.screen!=null && mc.getOverlay()==null) {
            started=true;
            mc.setScreen(new Review(false));
        }
    }
    private static final class Review extends Screen {
        private final boolean reloaded;
        private int frames;
        Review(boolean reloaded) { super(Component.empty()); this.reloaded=reloaded; }
        @Override public void render(GuiGraphics g, int mx, int my, float dt) {
            g.fill(0,0,width,height,0xff262b33);
            g.drawString(font,"Minecraft 1.20.1 GPU model / GUI / foil / resource reload QA",12,12,0xffffff);
            g.drawString(font,reloaded ? "AFTER resource reload" : "BEFORE resource reload",12,28,0xc6e1ff);
            Item[] items={ModItems.BIRCH_SPLINTERS.get(),ModItems.VINYS_BUCKET.get(),ModItems.JUKYS_WHISTLE.get(),
                    ModItems.CHEESE_SLICE.get(),ModItems.HAMBURGER.get(),ModItems.GUSTAFS_NEW_BOOK.get(),ModItems.GUSTAFS_JUKEBOX.get()};
            String[] names={"splinters","Viny bucket","Juky whistle","cheese","hamburger","Gustaf book","jukebox"};
            for(int i=0;i<items.length;i++) {
                int x=(int)((i+0.5)*width/items.length);
                g.drawCenteredString(font,names[i],x,55,0xffffff);
                ItemStack stack=items[i].getDefaultInstance();
                stack.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
                var model=minecraft.getItemRenderer().getModel(stack,null,null,0);
                if(model==minecraft.getModelManager().getMissingModel() || model.getParticleIcon().contents().name().getPath().equals("missingno"))
                    throw new IllegalStateException("Missing model/texture: "+names[i]);
                draw(g,stack,x,105,false);
                draw(g,stack,x,205,true);
            }
            g.drawString(font,"Top: normal pass. Bottom: actual enchanted mod stacks.",12,260,0xffffff);
            g.drawString(font,"GUI only; player motion and trade/recipe screens are outside this smoke test.",12,278,0xabc2cf);
            g.flush();
            frames++;
            if(frames==60) {
                Path out=Path.of(System.getProperty("deathsareitems.visualqa.output"),reloaded?"minecraft-after-reload.png":"minecraft-before-reload.png");
                try(var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) { image.writeToFile(out); }
                catch(Exception e) { throw new RuntimeException(e); }
                DeathsAreItems.LOGGER.info("EINFACH_VISUAL_QA seven models rendered with/without foil; reloaded={}",reloaded);
            }
            if(frames==90) {
                if(reloaded) minecraft.stop();
                else minecraft.execute(() -> minecraft.reloadResourcePacks().thenRunAsync(() -> minecraft.setScreen(new Review(true)),minecraft));
            }
        }
        private void draw(GuiGraphics g,ItemStack stack,int x,int y,boolean foil) {
            var renderer=minecraft.getItemRenderer();
            var model=renderer.getModel(stack,null,null,0);
            var pose=g.pose();pose.pushPose();
            pose.translate(x,y,150);pose.scale(48,-48,48);
            if(!model.usesBlockLight()) Lighting.setupForFlatItems();
            renderer.render(foil?stack:new ItemStack(Items.STONE),ItemDisplayContext.GUI,false,
                    pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY,model);
            g.flush();Lighting.setupFor3DItems();pose.popPose();
        }
    }
}
