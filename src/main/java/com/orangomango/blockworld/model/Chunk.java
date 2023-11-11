package com.orangomango.blockworld.model;

import java.util.*;

import com.orangomango.rendering3d.model.Mesh;
import com.orangomango.blockworld.util.PerlinNoise;
import static com.orangomango.blockworld.MainApplication.ENGINE;

public class Chunk{
	public static final int CHUNK_SIZE = 4;
	private static final int HEIGHT_LIMIT = 2;
	private static final int WATER_HEIGHT = HEIGHT_LIMIT*CHUNK_SIZE+9;

	private World world;
	private ChunkPosition position;
	private Block[][][] blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
	private List<Mesh> meshgroup;

	public static List<Block> pendingBlocks = new ArrayList<>(); // pendingBlocks cannot contain blocks with special settings (like water's yOffset)

	public static class ChunkPosition{
		private int x, y, z;

		public ChunkPosition(int x, int y, int z){
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public int getX(){
			return this.x;
		}

		public int getY(){
			return this.y;
		}

		public int getZ(){
			return this.z;
		}

		@Override
		public int hashCode(){
			return Objects.hash(Integer.valueOf(this.x), Integer.valueOf(this.y), Integer.valueOf(this.z));
		}

		@Override
		public boolean equals(Object other){
			if (other instanceof ChunkPosition){
				ChunkPosition chunk = (ChunkPosition)other;
				return this.x == chunk.x && this.y == chunk.y && this.z == chunk.z;
			} else return false;
		}
	}

	public Chunk(World world, ChunkPosition position){
		this.world = world;
		this.position = position;

		PerlinNoise noise = new PerlinNoise(world.getSeed());
		Random random = world.getRandom();
		float frequency = 0.1575f;
		float biomeFreq = 0.05f;

		// World generation
		for (int i = 0; i < CHUNK_SIZE; i++){ // x
			for (int j = 0; j < CHUNK_SIZE; j++){ // y
				for (int k = 0; k < CHUNK_SIZE; k++){ // z
					if (getY() < HEIGHT_LIMIT){
						setBlock(null, i, j, k);
					} else {
						float n = (noise.noise((i+getX()*CHUNK_SIZE)*frequency, 0, (k+getZ()*CHUNK_SIZE)*frequency)+1)/2;
						float b = (noise.noise((i+getX()*CHUNK_SIZE)*biomeFreq, 0, (k+getZ()*CHUNK_SIZE)*biomeFreq)+1)/2;
						int h = Math.round(n*(16-1))+CHUNK_SIZE*HEIGHT_LIMIT; // air column
						if (world.isSuperFlat()) h = CHUNK_SIZE*HEIGHT_LIMIT+1;
						int pos = getY()*CHUNK_SIZE+j;
						if (pos >= h){
							String biome = b <= 0.4 || (pos > WATER_HEIGHT && random.nextInt(100) < 35) ? "sand" : (pos == h && pos <= WATER_HEIGHT ? "grass" : "dirt");
							setBlock(new Block(this, i, j, k, pos > h+3 ? "stone" : biome), i, j, k);
						}
					}
				}
			}
		}

		// Trees generation
		for (int i = 0; i < CHUNK_SIZE; i++){ // x
			for (int j = 0; j < CHUNK_SIZE; j++){ // z
				int h = 0;
				for (int k = 0; k < CHUNK_SIZE; k++){ // y
					if (this.blocks[i][k][j] == null) h++;
					else break;
				}
				if (h > 0 && h < CHUNK_SIZE && h-1+getY()*CHUNK_SIZE < WATER_HEIGHT){
					if (random.nextInt(1000) < 12){
						if (this.blocks[i][h][j].getType().equals("grass")){
							int treeHeight = 5;
							for (int k = 0; k < treeHeight; k++){
								setBlock(new Block(this, i, h-1-k, j, "wood_log"), i, h-1-k, j);
							}
							// 5x5
							for (int kx = i-2; kx < i+3; kx++){
								for (int ky = j-2; ky < j+3; ky++){
									if (kx == i && ky == j) continue;
									setBlock(new Block(this, kx, h-1-(treeHeight-2), ky, "leaves"), kx, h-1-(treeHeight-2), ky);
								}
							}
							// 3x3
							for (int kx = i-1; kx < i+2; kx++){
								for (int ky = j-1; ky < j+2; ky++){
									if (kx == i && ky == j) continue;
									setBlock(new Block(this, kx, h-1-(treeHeight-1), ky, "leaves"), kx, h-1-(treeHeight-1), ky);
								}
							}
							setBlock(new Block(this, i, h-1-treeHeight, j, "leaves"), i, h-1-treeHeight, j);
						} else if (this.blocks[i][h][j].getType().equals("sand")){
							int cactusHeight = 3;
							for (int k = 0; k < cactusHeight; k++){
								setBlock(new Block(this, i, h-1-k, j, "cactus"), i, h-1-k, j);
							}
						}
					} else if (random.nextInt(1000) < 16){
						String flowerType = "flower_"+(random.nextBoolean() ? "red" : "yellow");
						if (this.blocks[i][h][j].getType().equals("sand")){
							flowerType = "bush";
						}
						setBlock(new Block(this, i, h-1, j, flowerType), i, h-1, j);
					}
				}
			}
		}

		// Water generation
		for (int i = 0; i < CHUNK_SIZE; i++){ // x
			for (int j = 0; j < CHUNK_SIZE; j++){ // z
				for (int k = 0; k < CHUNK_SIZE; k++){ // y
					if (this.blocks[i][k][j] == null && k+getY()*CHUNK_SIZE >= WATER_HEIGHT){
						Block waterBlock = new Block(this, i, k, j, "water");
						setBlock(waterBlock, i, k, j);
					}
				}
			}
		}

		buildPendingBlocks();
	}

	public Chunk(World world, ChunkPosition position, int[][][] input){
		this.world = world;
		this.position = position;
		
		for (int i = 0; i < CHUNK_SIZE; i++){ // x
			for (int j = 0; j < CHUNK_SIZE; j++){ // y
				for (int k = 0; k < CHUNK_SIZE; k++){ // z
					int id = input[i][j][k];
					if (id == 0){
						setBlock(null, i, j, k);
					} else {
						setBlock(new Block(this, i, j, k, Atlas.MAIN_ATLAS.getBlockType(id)), i, j, k);
					}
				}
			}
		}
		
		buildPendingBlocks();
	}

	private void buildPendingBlocks(){
		// Build pending blocks generated from other chunks
		Iterator<Block> iterator = pendingBlocks.iterator();
		while (iterator.hasNext()){
			Block block = iterator.next();
			if (containsBlock(block.getX()-getX()*CHUNK_SIZE, block.getY()-getY()*CHUNK_SIZE, block.getZ()-getZ()*CHUNK_SIZE)){
				setBlock(block, block.getX()-getX()*CHUNK_SIZE, block.getY()-getY()*CHUNK_SIZE, block.getZ()-getZ()*CHUNK_SIZE);
				iterator.remove();
			}
		}
	}

	public List<Mesh> getMesh(){
		if (this.meshgroup != null) return this.meshgroup;

		List<Mesh> output = new ArrayList<>();
		for (int i = 0; i < CHUNK_SIZE; i++){
			for (int j = 0; j < CHUNK_SIZE; j++){
				for (int k = 0; k < CHUNK_SIZE; k++){
					if (this.blocks[i][j][k] != null){
						output.add(this.blocks[i][j][k].getMesh());
					}
				}
			}
		}
		this.meshgroup = output;
		return output;
	}

	public void updateMesh(){
		for (Mesh mesh : this.meshgroup){
			ENGINE.removeObject(mesh);
		}
		this.meshgroup = null;

		List<Mesh> newObjects = getMesh();
		for (Mesh mesh : newObjects){
			ENGINE.addObject(mesh);
		}

		setupFaces();
	}

	public void setupFaces(){
		for (int i = 0; i < CHUNK_SIZE; i++){ // x
			for (int j = 0; j < CHUNK_SIZE; j++){ // y
				for (int k = 0; k < CHUNK_SIZE; k++){ // z
					if (this.blocks[i][j][k] != null) this.blocks[i][j][k].setupFaces();
				}
			}
		}
	}

	public Block getBlockAt(int x, int y, int z){
		if (containsBlock(x, y, z)){
			return this.blocks[x][y][z];
		} else {
			return null;
		}
	}

	public void setBlock(Block block, int x, int y, int z){
		if (containsBlock(x, y, z)){
			this.blocks[x][y][z] = block;
		} else if (block != null){
			// Block will be stored
			Chunk chunk = this.world.getChunkAt(block.getX()/CHUNK_SIZE, block.getY()/CHUNK_SIZE, block.getZ()/CHUNK_SIZE);
			if (chunk == null){
				pendingBlocks.add(block);
			} else if (block.getX() >= 0 && block.getY() >= 0 && block.getZ() >= 0){
				chunk.setBlock(block, block.getX() % CHUNK_SIZE, block.getY() % CHUNK_SIZE, block.getZ() % CHUNK_SIZE);
				chunk.updateMesh();
			}
		}
	}

	private boolean containsBlock(int x, int y, int z){
		return x >= 0 && y >= 0 && z >= 0 && x < CHUNK_SIZE && y < CHUNK_SIZE && z < CHUNK_SIZE;
	}

	public World getWorld(){
		return this.world;
	}

	public int getX(){
		return this.position.getX();
	}

	public int getY(){
		return this.position.getY();
	}

	public int getZ(){
		return this.position.getZ();
	}

	@Override
	public String toString(){
		return String.format("%d_%d_%d", getX(), getY(), getZ());
	}
}
