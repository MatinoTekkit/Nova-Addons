package xyz.xenondevs.nova.addon.machines.util

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.type.CaveVinesPlant
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.below
import xyz.xenondevs.nova.util.item.soundGroup
import kotlin.random.Random

fun Material.isTillable(): Boolean {
    return this == Material.GRASS_BLOCK
        || this == Material.DIRT
        || this == Material.DIRT_PATH
}

fun Material.isLeaveLike(): Boolean {
    return Tag.LEAVES.isTagged(this) || Tag.WART_BLOCKS.isTagged(this)
}

fun Block.isFullyAged(): Boolean {
    val blockData = blockData
    return blockData is Ageable && blockData.age == blockData.maximumAge
}

fun Block.hasSameTypeBelow(): Boolean {
    val below = below
    return below.type == type
}

private fun Block.harvestSweetBerries(harvest: Boolean): List<ItemStack> {
    if (harvest) {
        val data = blockData as Ageable
        data.age = 1
        blockData = data
    }
    
    return listOf(ItemStack(Material.SWEET_BERRIES, Random.nextInt(2, 3)))
}

private fun Block.canHarvestCaveVines(): Boolean {
    val blockData = blockData
    return blockData is CaveVinesPlant && blockData.isBerries
}

private fun Block.harvestCaveVines(harvest: Boolean): List<ItemStack> {
    if (harvest) {
        val data = blockData as CaveVinesPlant
        data.isBerries = false
        blockData = data
    }
    
    return listOf(ItemStack(Material.GLOW_BERRIES))
}

private typealias HarvestableCheck = Block.() -> Boolean
private typealias HarvestFunction = Block.(Boolean) -> List<ItemStack>

object PlantUtils {
    
    private val SEED_SOIL_BLOCKS: Map<Material, Set<Material>>
    private val SEED_GROWTH_BLOCKS: Map<Material, Material>
    private val HARVESTABLE_PLANTS: Map<Material, Pair<HarvestableCheck?, HarvestFunction?>?>
    
    init {
        val farmland = hashSetOf(Material.FARMLAND)
        val defaultDirt = hashSetOf(Material.FARMLAND, Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.ROOTED_DIRT, Material.PODZOL, Material.MYCELIUM)
        
        SEED_SOIL_BLOCKS = enumMapOf(
            Material.WHEAT_SEEDS to farmland,
            Material.BEETROOT_SEEDS to farmland,
            Material.POTATO to farmland,
            Material.CARROT to farmland,
            Material.PUMPKIN_SEEDS to farmland,
            Material.MELON_SEEDS to farmland,
            Material.SWEET_BERRIES to defaultDirt,
            Material.OAK_SAPLING to defaultDirt,
            Material.SPRUCE_SAPLING to defaultDirt,
            Material.BIRCH_SAPLING to defaultDirt,
            Material.JUNGLE_SAPLING to defaultDirt,
            Material.ACACIA_SAPLING to defaultDirt,
            Material.DARK_OAK_SAPLING to defaultDirt,
            Material.CRIMSON_FUNGUS to hashSetOf(Material.CRIMSON_NYLIUM),
            Material.WARPED_FUNGUS to hashSetOf(Material.WARPED_NYLIUM),
            Material.NETHER_WART to hashSetOf(Material.SOUL_SAND)
        )
        
        SEED_GROWTH_BLOCKS = enumMapOf(
            Material.WHEAT_SEEDS to Material.WHEAT,
            Material.BEETROOT_SEEDS to Material.BEETROOTS,
            Material.POTATO to Material.POTATOES,
            Material.CARROT to Material.CARROTS,
            Material.SWEET_BERRIES to Material.SWEET_BERRY_BUSH,
            Material.PUMPKIN_SEEDS to Material.PUMPKIN_STEM,
            Material.MELON_SEEDS to Material.MELON_STEM
        )
        
        HARVESTABLE_PLANTS = enumMapOf(
            Material.SHORT_GRASS to null,
            Material.TALL_GRASS to null,
            Material.BEE_NEST to null,
            Material.PUMPKIN to null,
            Material.MELON to null,
            Material.SHROOMLIGHT to null,
            Material.WEEPING_VINES to null,
            Material.WEEPING_VINES_PLANT to null,
            Material.WHEAT to (Block::isFullyAged to null),
            Material.BEETROOTS to (Block::isFullyAged to null),
            Material.POTATOES to (Block::isFullyAged to null),
            Material.CARROTS to (Block::isFullyAged to null),
            Material.SWEET_BERRY_BUSH to (Block::isFullyAged to null),
            Material.CACTUS to (Block::hasSameTypeBelow to null),
            Material.SUGAR_CANE to (Block::hasSameTypeBelow to null),
            Material.SWEET_BERRY_BUSH to (Block::isFullyAged to Block::harvestSweetBerries),
            Material.CAVE_VINES to (Block::canHarvestCaveVines to Block::harvestCaveVines),
            Material.CAVE_VINES_PLANT to (Block::canHarvestCaveVines to Block::harvestCaveVines)
        ).also { map ->
            fun addTags(vararg tags: Tag<Material>) =
                tags.forEach { tag -> tag.values.forEach { material -> map[material] = null } }
            
            addTags(Tag.LEAVES, Tag.LOGS, Tag.FLOWERS, Tag.WART_BLOCKS)
        }
    }
    
    fun isSeed(item: ItemStack): Boolean =
        CustomItemServiceManager.getItemType(item) == CustomItemType.SEED
            || item.type in SEED_SOIL_BLOCKS
    
    fun canBePlaced(seed: ItemStack, block: Block): Boolean {
        val placeOn = block.below
        return (CustomItemServiceManager.getItemType(seed) == CustomItemType.SEED && placeOn.type == Material.FARMLAND)
            || SEED_SOIL_BLOCKS[seed.type]?.contains(placeOn.type) == true
    }
    
    fun requiresFarmland(seed: ItemStack): Boolean =
        CustomItemServiceManager.getItemType(seed) == CustomItemType.SEED
            || SEED_SOIL_BLOCKS[seed.type]?.contains(Material.FARMLAND) == true
    
    fun placeSeed(seed: ItemStack, block: Block, playEffects: Boolean) {
        if (CustomItemServiceManager.placeBlock(seed, block.location, playEffects)) return
        
        val newType = SEED_GROWTH_BLOCKS[seed.type] ?: seed.type
        block.type = newType
        
        if (playEffects) block.world.playSound(
            block.location,
            newType.soundGroup.placeSound,
            1f,
            Random.nextDouble(0.8, 0.95).toFloat()
        )
    }
    
    fun isHarvestable(block: Block): Boolean {
        val type = block.type
        return type in HARVESTABLE_PLANTS
            && HARVESTABLE_PLANTS[block.type]?.first?.invoke(block) ?: true
    }
    
    fun harvest(ctx: Context<BlockBreak>) {
        val block = ctx[DefaultContextParamTypes.BLOCK_POS]!!.block
        val type = block.type
        if (block.type !in HARVESTABLE_PLANTS) return
        
        val harvestFunction = HARVESTABLE_PLANTS[type]?.second
        if (harvestFunction != null) {
            harvestFunction(block, true)
        } else {
            BlockUtils.breakBlock(ctx)
        }
    }
    
    fun getHarvestDrops(ctx: Context<BlockBreak>): List<ItemStack>? {
        val block = ctx[DefaultContextParamTypes.BLOCK_POS]!!.block
        val type = block.type
        if (block.type !in HARVESTABLE_PLANTS)
            return null
        
        val customBlockType = CustomItemServiceManager.getBlockType(block)
        if (customBlockType == CustomBlockType.NORMAL)
            return null
        
        val drops: List<ItemStack>
        if (customBlockType != CustomBlockType.CROP) {
            val harvestFunction = HARVESTABLE_PLANTS[type]?.second
            if (harvestFunction != null) {
                drops = harvestFunction(block, false)
            } else {
                drops = BlockUtils.getDrops(ctx)
            }
        } else {
            drops = CustomItemServiceManager.getDrops(block, null)!!
        }
        
        return drops
    }
    
    fun isTreeAttachment(material: Material): Boolean {
        return material == Material.BEE_NEST
            || material == Material.SHROOMLIGHT
            || material == Material.WEEPING_VINES
            || material == Material.WEEPING_VINES_PLANT
    }
    
}
