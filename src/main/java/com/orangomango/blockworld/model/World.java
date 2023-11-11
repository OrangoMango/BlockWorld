package com.orangomango.blockworld.model;

import java.util.*;

import static com.orangomango.blockworld.MainApplication.ENGINE;

public class World{
	private int seed;
	private boolean superFlat;
	private Random random;
	private HashMap<Chunk.ChunkPosition, Chunk> chunks = new HashMap<>();

	public World(int seed, boolean superFlat){
		this.seed = seed;
		this.superFlat = superFlat;
		this.random = new Random(seed);
	}

	public void addChunk(Chunk chunk, Chunk.ChunkPosition pos){
		this.chunks.put(pos, chunk);
	}

	public Chunk addChunk(int x, int y, int z){
		Chunk.ChunkPosition pos = new Chunk.ChunkPosition(x, y, z);
		Chunk chunk = new Chunk(this, pos);
		addChunk(chunk, pos);
		return chunk;
	}

	public void removeChunk(int x, int y, int z){
		Chunk.ChunkPosition pos = new Chunk.ChunkPosition(x, y, z);
		this.chunks.remove(pos);
	}

	public Block getBlockAt(int x, int y, int z){
		int chunkX = x / Chunk.CHUNK_SIZE;
		int chunkY = y / Chunk.CHUNK_SIZE;
		int chunkZ = z / Chunk.CHUNK_SIZE;
		Chunk chunk = getChunkAt(chunkX, chunkY, chunkZ);
		if (chunk != null){
			return chunk.getBlockAt(x % Chunk.CHUNK_SIZE, y % Chunk.CHUNK_SIZE, z % Chunk.CHUNK_SIZE);
		} else {
			return null;
		}
	}

	public void removeBlockAt(int x, int y, int z){
		int chunkX = x / Chunk.CHUNK_SIZE;
		int chunkY = y / Chunk.CHUNK_SIZE;
		int chunkZ = z / Chunk.CHUNK_SIZE;
		Chunk chunk = getChunkAt(chunkX, chunkY, chunkZ);
		if (chunk != null){
			chunk.setBlock(null, x % Chunk.CHUNK_SIZE, y % Chunk.CHUNK_SIZE, z % Chunk.CHUNK_SIZE);
		}
	}

	public void setBlockAt(int x, int y, int z, String type){
		int chunkX = x / Chunk.CHUNK_SIZE;
		int chunkY = y / Chunk.CHUNK_SIZE;
		int chunkZ = z / Chunk.CHUNK_SIZE;
		Chunk chunk = getChunkAt(chunkX, chunkY, chunkZ);
		if (chunk != null){
			int blockX = x % Chunk.CHUNK_SIZE;
			int blockY = y % Chunk.CHUNK_SIZE;
			int blockZ = z % Chunk.CHUNK_SIZE;
			chunk.setBlock(new Block(chunk, blockX, blockY, blockZ, type), blockX, blockY, blockZ);
		}
	}

	public Collection<Chunk> getChunks(){
		return this.chunks.values();
	}

	public void setupFaces(){
		for (Chunk chunk : getChunks()){
			chunk.setupFaces();
		}
	}

	public Chunk getChunkAt(int x, int y, int z){
		Chunk.ChunkPosition pos = new Chunk.ChunkPosition(x, y, z);
		return this.chunks.getOrDefault(pos, null);
	}

	public int getSeed(){
		return this.seed;
	}

	public void setSeed(int seed){
		this.seed = seed;
		this.chunks.clear();
		ENGINE.clearObjects();
	}

	public Random getRandom(){
		return this.random;
	}

	public boolean isSuperFlat(){
		return this.superFlat;
	}
}
