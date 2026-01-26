package worldgeneratorextension.quasistructure.populator;

import cn.nukkit.Server;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.biome.EnumBiome;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.populator.type.Populator;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import worldgeneratorextension.global.task.BlockActorSpawnTask;

public class PopulatorBeeNest extends Populator {

    private static final int[] BEE_HABITAT_BIOMES = {
            EnumBiome.PLAINS.id,
            EnumBiome.SUNFLOWER_PLAINS.id,
            EnumBiome.FLOWER_FOREST.id,
            EnumBiome.FOREST.id,
            EnumBiome.BIRCH_FOREST.id,
            EnumBiome.BIRCH_FOREST_HILLS.id,
            EnumBiome.BIRCH_FOREST_M.id,
            EnumBiome.BIRCH_FOREST_HILLS_M.id
    };

    private static final int OAK_LOG = BlockID.LOG;
    private static final int BIRCH_LOG = BlockID.LOG;
    private static final int OAK_LEAVES = BlockID.LEAVES;
    private static final int BIRCH_LEAVES = BlockID.LEAVES;
    private static final int BEE_NEST = BlockID.BEE_NEST;

    @Override
    public void populate(ChunkManager level, int chunkX, int chunkZ, NukkitRandom random, FullChunk chunk) {
        int biome = chunk.getBiomeId(7, 7);
        if (!isBeeHabitatBiome(biome)) {
            return;
        }

        int spawnChance = getSpawnChance(biome);
        if (random.nextBoundedInt(100) >= spawnChance) {
            return;
        }

        for (int attempt = 0; attempt < 16; attempt++) {
            int x = (chunkX << 4) + random.nextBoundedInt(16);
            int z = (chunkZ << 4) + random.nextBoundedInt(16);

            for (int y = 96; y > 62; y--) {
                int blockId = level.getBlockIdAt(x, y, z);
                int blockMeta = level.getBlockDataAt(x, y, z);

                if (isValidTreeLog(blockId, blockMeta)) {
                    if (hasLeavesNearby(level, x, y, z) && isGroundNearby(level, x, y, z)) {
                        BlockFace nestFace = findValidNestFace(level, x, y, z, random);
                        if (nestFace != null) {
                            int nestX = x + nestFace.getXOffset();
                            int nestZ = z + nestFace.getZOffset();

                            int facingMeta = getFacingMeta(nestFace);

                            level.setBlockAt(nestX, y, nestZ, BEE_NEST, facingMeta);

                            createBeehiveBlockEntity(chunk, nestX, y, nestZ, random);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isBeeHabitatBiome(int biome) {
        for (int habitatBiome : BEE_HABITAT_BIOMES) {
            if (biome == habitatBiome) {
                return true;
            }
        }
        return false;
    }

    private int getSpawnChance(int biome) {
        if (biome == EnumBiome.FLOWER_FOREST.id) {
            return 100;
        }
        if (biome == EnumBiome.PLAINS.id || biome == EnumBiome.SUNFLOWER_PLAINS.id) {
            return 5;
        }
        return 2;
    }

    private boolean isValidTreeLog(int blockId, int blockMeta) {
        if (blockId == OAK_LOG) {
            int woodType = blockMeta & 0x3;
            return woodType == 0 || woodType == 2;
        }
        return false;
    }

    private boolean hasLeavesNearby(ChunkManager level, int x, int y, int z) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int blockId = level.getBlockIdAt(x + dx, y + dy, z + dz);
                    if (blockId == OAK_LEAVES || blockId == BlockID.LEAVES2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isGroundNearby(ChunkManager level, int x, int y, int z) {
        for (int checkY = y - 1; checkY >= y - 6; checkY--) {
            int blockId = level.getBlockIdAt(x, checkY, z);
            if (blockId == OAK_LOG || blockId == BlockID.LOG2) {
                continue;
            }
            if (blockId == BlockID.GRASS || blockId == BlockID.DIRT || blockId == BlockID.PODZOL) {
                return true;
            }
            break;
        }
        return false;
    }

    private BlockFace findValidNestFace(ChunkManager level, int x, int y, int z, NukkitRandom random) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        int startIndex = random.nextBoundedInt(4);

        for (int i = 0; i < 4; i++) {
            BlockFace face = faces[(startIndex + i) % 4];
            int checkX = x + face.getXOffset();
            int checkZ = z + face.getZOffset();

            if (level.getBlockIdAt(checkX, y, checkZ) == AIR &&
                    level.getBlockIdAt(checkX, y - 1, checkZ) == AIR) {
                return face;
            }
        }
        return null;
    }

    private int getFacingMeta(BlockFace face) {
        switch (face) {
            case SOUTH:
                return 0;
            case WEST:
                return 1;
            case NORTH:
                return 2;
            case EAST:
                return 3;
            default:
                return 0;
        }
    }

    private void createBeehiveBlockEntity(FullChunk chunk, int x, int y, int z, NukkitRandom random) {
        CompoundTag nbt = new CompoundTag()
                .putString("id", BlockEntity.BEEHIVE)
                .putInt("x", x)
                .putInt("y", y)
                .putInt("z", z)
                .putByte("ShouldSpawnBees", 1);

        ListTag<CompoundTag> occupants = new ListTag<>("Occupants");
        int beeCount = random.nextBoundedInt(3) + 1;
        for (int i = 0; i < beeCount; i++) {
            CompoundTag beeData = new CompoundTag()
                    .putInt("TicksLeftToStay", 600 + random.nextBoundedInt(1200))
                    .putCompound("SaveData", createBeeNBT(x, y, z))
                    .putBoolean("HasNectar", false);
            occupants.add(beeData);
        }
        nbt.putList(occupants);

        Server.getInstance().getScheduler().scheduleDelayedTask(
                new BlockActorSpawnTask(chunk.getProvider().getLevel(), nbt), 2);
    }

    private CompoundTag createBeeNBT(int x, int y, int z) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("identifier", "minecraft:bee");
        nbt.putList(new ListTag<>("Pos"));
        nbt.putList(new ListTag<>("Motion"));
        nbt.putList(new ListTag<>("Rotation"));
        nbt.putBoolean("HasNectar", false);
        nbt.putBoolean("HasStung", false);
        nbt.putInt("AngerTime", 0);
        nbt.putDouble("hiveX", x);
        nbt.putDouble("hiveY", y);
        nbt.putDouble("hiveZ", z);
        return nbt;
    }
}
