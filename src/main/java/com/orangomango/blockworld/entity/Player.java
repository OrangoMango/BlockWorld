package com.orangomango.blockworld.entity;

import javafx.geometry.Point3D;

import java.util.function.Consumer;

import static com.orangomango.blockworld.model.ChunkManager.RENDER_DISTANCE;
import static com.orangomango.blockworld.model.Chunk.CHUNK_SIZE;
import com.orangomango.blockworld.util.Util;
import com.orangomango.rendering3d.model.Camera;

public class Player{
	private Camera camera;
	private Point3D lastChunkPos;

	public Player(double x, double y, double z, int w, int h){
		this.camera = new Camera(new Point3D(x, y, z), w, h, Math.PI/2, RENDER_DISTANCE*CHUNK_SIZE*1.25, 0.1);
		this.lastChunkPos = Util.getChunkPos(getPosition());
	}

	public void setOnChunkPositionChanged(Consumer<Point3D> c){
		this.camera.setOnPositionChanged(pos -> {
			Point3D chunkPos = Util.getChunkPos(pos);
			if (chunkPos.equals(this.lastChunkPos)){
				return; // The player stayed in the same chunk
			}
			this.lastChunkPos = chunkPos;
			c.accept(chunkPos);
		});
	}

	// Changing this camera = changing the player
	public Camera getCamera(){
		return this.camera;
	}

	public Point3D getPosition(){
		return this.camera.getPosition();
	}

	public double getX(){
		return this.camera.getPosition().getX();
	}

	public double getY(){
		return this.camera.getPosition().getY();
	}

	public double getZ(){
		return this.camera.getPosition().getZ();
	}

	public double getRx(){
		return this.camera.getRx();
	}

	public double getRy(){
		return this.camera.getRy();
	}

	public void move(double x, double y, double z){
		this.camera.move(new Point3D(x, y, z));
	}
}