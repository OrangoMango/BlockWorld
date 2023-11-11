package com.orangomango.blockworld.model;

import javafx.geometry.Point3D;

import java.util.*;
import java.io.*;

import com.orangomango.rendering3d.model.Mesh;
import static com.orangomango.blockworld.MainApplication.ENGINE;

public class ChunkManager{
	public static final double RENDER_DISTANCE = 6.5;

	private World world;
	private int chunks;

	public ChunkManager(World world, int chunks){
		this.world = world;
		this.chunks = chunks;
		loadPendingBlocks();
	}

	public void deleteSavedWorld(){
		File dir = new File(System.getProperty("user.home"), ".blockWorld/");
		if (dir.exists()){
			for (File file : dir.listFiles()){
				file.delete();
			}
		}
	}

	public void manage(Point3D chunkPos){
		// Get the chunks to unload
		List<Chunk> toUnload = new ArrayList<>();
		for (Chunk chunk : this.world.getChunks()){
			if ((new Point3D(chunk.getX(), chunk.getY(), chunk.getZ())).distance(chunkPos) > RENDER_DISTANCE){
				toUnload.add(chunk);
			}
		}

		// Unload the chunks
		for (Chunk chunk : toUnload){
			unloadChunk(chunk);
		}

		// Get the chunks to load
		int chunkX = (int)chunkPos.getX();
		int chunkY = (int)chunkPos.getY();
		int chunkZ = (int)chunkPos.getZ();
		List<Point3D> toLoad = new ArrayList<>();
		for (int i = -this.chunks/2; i < -this.chunks/2+this.chunks; i++){
			for (int j = -this.chunks/2; j < -this.chunks/2+this.chunks; j++){
				for (int k = -1; k < 3; k++){ // y-chunks
					if (chunkX+i < 0 || chunkY+k < 0 || chunkZ+j < 0) continue;
					if (this.world.getChunkAt(chunkX+i, chunkY+k, chunkZ+j) == null){
						if ((new Point3D(chunkX, chunkY, chunkZ)).distance(new Point3D(chunkX+i, chunkY+k, chunkZ+j)) <= RENDER_DISTANCE){
							toLoad.add(new Point3D(chunkX+i, chunkY+k, chunkZ+j));
						}
					}
				}
			}
		}

		// Load the chunks
		for (Point3D point : toLoad){
			loadChunk((int)point.getX(), (int)point.getY(), (int)point.getZ());
		}

		if (toUnload.size() > 0 || toLoad.size() > 0){
			for (Chunk chunk : this.world.getChunks()){
				chunk.setupFaces();
			}
		}
	}

	private void unloadChunk(Chunk chunk){
		saveChunkToFile(chunk);
		this.world.removeChunk(chunk.getX(), chunk.getY(), chunk.getZ());
		for (Mesh mesh : chunk.getMesh()){
			ENGINE.removeObject(mesh);
		}
	}

	private void loadChunk(int x, int y, int z){
		boolean loaded = loadChunkFromFile(x, y, z);
		if (!loaded){
			Chunk chunk = this.world.addChunk(x, y, z);
			for (Mesh mesh : chunk.getMesh()){
				ENGINE.addObject(mesh);
			}
		}
	}

	public void saveWorld(){
		for (Chunk chunk : this.world.getChunks()){
			saveChunkToFile(chunk);
		}
		savePendingBlocks();
		System.out.println("World saved");
	}

	private void saveChunkToFile(Chunk chunk){
		try {
			File dir = new File(System.getProperty("user.home"), ".blockWorld/");
			if (!dir.exists()) dir.mkdir();
			File chunkFile = new File(dir, chunk.toString()+".chunk");
			if (!chunkFile.exists()) chunkFile.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(chunkFile));
			for (int i = 0; i < Chunk.CHUNK_SIZE; i++){ // z
				for (int j = 0; j < Chunk.CHUNK_SIZE; j++){ // y
					for (int k = 0; k < Chunk.CHUNK_SIZE; k++){ // x
						Block block = chunk.getBlockAt(k, j, i);
						writer.write(block == null ? "0 " : block.getId()+" ");
					}
					writer.newLine();
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	private boolean loadChunkFromFile(int x, int y, int z){
		try {
			File dir = new File(System.getProperty("user.home"), ".blockWorld/");
			if (!dir.exists()) dir.mkdir();
			File chunkFile = new File(dir, String.format("%d_%d_%d", x, y, z)+".chunk");
			if (!chunkFile.exists()) return false;
			int[][][] chunkData = new int[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];
			BufferedReader reader = new BufferedReader(new FileReader(chunkFile));
			for (int i = 0; i < Chunk.CHUNK_SIZE; i++){ // z
				for (int j = 0; j < Chunk.CHUNK_SIZE; j++){ // y
					String line = reader.readLine();
					int k = 0;
					for (String piece : line.split(" ")){
						int id = Integer.parseInt(piece);
						chunkData[k++][j][i] = id;
					}
				}
				reader.readLine();
			}
			reader.close();
			Chunk.ChunkPosition chunkPos = new Chunk.ChunkPosition(x, y, z);
			Chunk chunk = new Chunk(this.world, chunkPos, chunkData);
			this.world.addChunk(chunk, chunkPos);
			for (Mesh mesh : chunk.getMesh()){
				ENGINE.addObject(mesh);
			}
			return true;
		} catch (IOException ex){
			ex.printStackTrace();
			return false;
		}
	}

	private void savePendingBlocks(){
		try {
			File dir = new File(System.getProperty("user.home"), ".blockWorld/");
			if (!dir.exists()) dir.mkdir();
			File file = new File(dir, "pendingBlocks.data");
			if (!file.exists()) file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for (Block block : Chunk.pendingBlocks){
				writer.write(String.format("%d %d %d %d\n", block.getX(), block.getY(), block.getZ(), block.getId()));
			}
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	private void loadPendingBlocks(){
		try {
			File dir = new File(System.getProperty("user.home"), ".blockWorld/");
			if (!dir.exists()) dir.mkdir();
			File file = new File(dir, "pendingBlocks.data");
			if (!file.exists()) file.createNewFile();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Chunk.pendingBlocks.clear();
			reader.lines().forEach(line -> {
				String[] parts = line.split(" ");
				int xp = Integer.parseInt(parts[0]);
				int yp = Integer.parseInt(parts[1]);
				int zp = Integer.parseInt(parts[2]);
				int id = Integer.parseInt(parts[3]);
				Block block = new Block(this.world, xp, yp, zp, Atlas.MAIN_ATLAS.getBlockType(id));
				Chunk.pendingBlocks.add(block);
			});
			reader.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
}
