package com.xziggsx.horseloyalty.riding.client.sense;

import com.xziggsx.horseloyalty.core.HorseTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The real {@link TerrainProbe}: reads the client's level. A column is read from the top down and
 * stops at the first solid block at or below the feet, so flat ground costs 4 block reads.
 */
public final class LevelTerrainProbe implements TerrainProbe {
	private final Level level;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

	public LevelTerrainProbe(Level level) {
		this.level = level;
	}

	@Override
	public Column probe(int x, int z, int feetY) {
		if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
			return Column.UNLOADED;
		}
		// The horse is about 1.6 blocks tall: a block at +1 is in its way, and one at +2 only
		// matters if it has to step up a block to get here.
		boolean headroom = solidShape(x, feetY + TOP, z) == null;
		if (solidShape(x, feetY + 1, z) != null) {
			return new Column(Terrain.WALL, 1);
		}
		for (int dy = 0; dy >= BOTTOM; dy--) {
			VoxelShape shape = solidShape(x, feetY + dy, z);
			if (shape == null) {
				continue;
			}
			if (dy == 0 && (!headroom || shape.max(Direction.Axis.Y) > 1)) {
				// A step it can't fit over, or a fence or wall it can't step onto.
				return new Column(Terrain.WALL, 0);
			}
			BlockState state = level.getBlockState(pos);
			if (isCover(state)) {
				// A carpet or thicker snow on a path still counts as the path below it.
				pos.move(0, -1, 0);
				state = level.getBlockState(pos);
			}
			return new Column(state.is(HorseTags.PATH_BLOCKS) ? Terrain.PATH : Terrain.GROUND, dy);
		}
		return Column.DROP;
	}

	/** The block's collision shape at this position, or null if the horse would pass through it. Leaves {@link #pos} there. */
	private VoxelShape solidShape(int x, int y, int z) {
		pos.set(x, y, z);
		VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
		return shape.isEmpty() ? null : shape;
	}

	/** Thin blocks lying on top of the ground. A single snow layer has no collision, so it's skipped anyway. */
	private static boolean isCover(BlockState state) {
		return state.is(Blocks.SNOW) || state.is(BlockTags.WOOL_CARPETS);
	}
}
