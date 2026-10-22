package co.blumeplugins.deathsareitems.block;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DeathsAreItems.MOD_ID);
    public static final RegistryObject<Block> FRAGILE_SAND = BLOCKS.register("fragile_sand", () ->
            new co.blumeplugins.deathsareitems.block.custom.FragileSedimentBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.SAND)));
    public static final RegistryObject<Block> FRAGILE_GRAVEL = BLOCKS.register("fragile_gravel", () ->
            new co.blumeplugins.deathsareitems.block.custom.FragileSedimentBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GRAVEL)));

    public static final RegistryObject<Block> BEDROCK = BLOCKS.register("bedrock", co.blumeplugins.deathsareitems.block.custom.BedrockBedBlock::new);

    public static final RegistryObject<Block> CACTUS_SPIKE = BLOCKS.register("cactus_spike", () -> new Block(
            BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.CACTUS).noCollission().noLootTable()));

    // Example block registration. Use registerBlock so a BlockItem is created automatically.
    // public static final RegistryObject<Block> EXAMPLE_BLOCK = registerBlock("example_block",
    //         () -> new Block(BlockBehaviour.Properties.of().strength(2f)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
