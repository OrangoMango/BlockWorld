package com.orangomango.blockworld.model;

import javafx.geometry.Point3D;
import javafx.geometry.Point2D;

import java.util.List;

import com.orangomango.rendering3d.model.Mesh;

public class Block{
	public static final double LIQUID_OFFSET = 0.2;
	public static final int MAX_LIGHT_INTENSITY = 15;

	private int x, y, z;
	private World world;
	private Mesh mesh;
	private String type;
	private int light = MAX_LIGHT_INTENSITY;
	private BlockMesh blockMesh;
	private double yOffset;

	public Block(Chunk chunk, int x, int y, int z, String type){
		this.world = chunk.getWorld();
		this.x = x+chunk.getX()*Chunk.CHUNK_SIZE;
		this.y = y+chunk.getY()*Chunk.CHUNK_SIZE;
		this.z = z+chunk.getZ()*Chunk.CHUNK_SIZE;
		this.type = type;
		this.blockMesh = Atlas.MAIN_ATLAS.getBlockMesh(this.type);
	}

	public Block(World world, int gx, int gy, int gz, String type){
		this.world = world;
		this.x = gx;
		this.y = gy;
		this.z = gz;
		this.type = type;
		this.blockMesh = Atlas.MAIN_ATLAS.getBlockMesh(this.type);
	}

	public void setLight(int intensity){
		this.light = intensity;
	}

	private void hideFace(int name){
		List<Integer> idx = this.blockMesh.getCullingIdx().getOrDefault(name, null);
		if (idx != null){
			for (int n : idx){
				this.mesh.addHiddenFace(n);
			}
		}
	}

	private static int evalHidePattern(String type, int faceName){
		return Atlas.MAIN_ATLAS.getHidePattern(type) & faceName;
	}

	public void setupFaces(){
		if (this.mesh == null) return;
		this.mesh.clearHiddenFaces();
		Block block = this.world.getBlockAt(this.x+1, this.y, this.z);
		if (block != null && (evalHidePattern(block.getType(), BlockMesh.FACE_LEFT) == 0 || (evalHidePattern(this.getType(), BlockMesh.FACE_RIGHT) != 0 && evalHidePattern(block.getType(), BlockMesh.FACE_LEFT) != 0))){
			hideFace(BlockMesh.FACE_RIGHT);
		}
		block = this.world.getBlockAt(this.x, this.y+1, this.z);
		if (block != null && (evalHidePattern(block.getType(), BlockMesh.FACE_TOP) == 0 || (evalHidePattern(this.getType(), BlockMesh.FACE_DOWN) != 0 && evalHidePattern(block.getType(), BlockMesh.FACE_TOP) != 0))){
			hideFace(BlockMesh.FACE_DOWN);
		}
		block = this.world.getBlockAt(this.x, this.y, this.z+1);
		if (block != null && (evalHidePattern(block.getType(), BlockMesh.FACE_FRONT) == 0 || (evalHidePattern(this.getType(), BlockMesh.FACE_BACK) != 0 && evalHidePattern(block.getType(), BlockMesh.FACE_FRONT) != 0))){
			hideFace(BlockMesh.FACE_BACK);
		}
		block = this.world.getBlockAt(this.x-1, this.y, this.z);
		if (block != null && (evalHidePattern(block.getType(), BlockMesh.FACE_RIGHT) == 0 || (evalHidePattern(this.getType(), BlockMesh.FACE_LEFT) != 0 && evalHidePattern(block.getType(), BlockMesh.FACE_RIGHT) != 0))){
			hideFace(BlockMesh.FACE_LEFT);
		}
		block = this.world.getBlockAt(this.x, this.y-1, this.z);
		if (block != null && (evalHidePattern(block.getType(), BlockMesh.FACE_DOWN) == 0 || (evalHidePattern(this.getType(), BlockMesh.FACE_TOP) != 0 && evalHidePattern(block.getType(), BlockMesh.FACE_DOWN) != 0))){
			hideFace(BlockMesh.FACE_TOP);
		}
		block = this.world.getBlockAt(this.x, this.y, this.z-1);
		if (block != null && (evalHidePattern(block.getType(), BlockMesh.FACE_BACK) == 0 || (evalHidePattern(this.getType(), BlockMesh.FACE_FRONT) != 0 && evalHidePattern(block.getType(), BlockMesh.FACE_BACK) != 0))){
			hideFace(BlockMesh.FACE_FRONT);
		}
	}

	public Mesh getMesh(){
		if (this.mesh != null) return this.mesh;

		Block top = this.world.getBlockAt(this.x, this.y-1, this.z);
		if ((top == null || !top.isLiquid()) && isLiquid()){
			this.yOffset = LIQUID_OFFSET;
		}

		Point3D[] vertices = this.blockMesh.getVertices();
		int[][] faces = this.blockMesh.getFacesPoints();
		Point3D[] blockVertices = new Point3D[vertices.length];
		int[][] blockFaces = new int[faces.length][3];

		System.arraycopy(vertices, 0, blockVertices, 0, vertices.length);
		System.arraycopy(faces, 0, blockFaces, 0, faces.length);

		if (this.yOffset != 0){
			for (int i = 0; i < blockVertices.length; i++){
				Point3D v = blockVertices[i];
				if (v.getY() == 0){
					blockVertices[i] = new Point3D(v.getX(), this.yOffset, v.getZ());
				}
			}
		}

		this.mesh = new Mesh(blockVertices, blockFaces, null, this.blockMesh.getImages(), this.blockMesh.getImageIndices(), this.blockMesh.getTex(), this.blockMesh.getFacesTex());
		this.mesh.setSkipCondition(cam -> {
			Point3D pos = new Point3D(this.x, this.y, this.z);
			return pos.distance(cam.getPosition()) > ChunkManager.RENDER_DISTANCE*Chunk.CHUNK_SIZE;
		});

		this.mesh.translate(this.x, this.y, this.z);
		this.mesh.build();
		this.mesh.setTransparentProcessing(isTransparent());
		this.mesh.setShowAllFaces(isTransparent());
		return this.mesh;
	}

	// Called once every frame
	public void update(){
		// Set light
		if (this.mesh != null){
			for (int i = 0; i < this.mesh.getTriangles().length; i++){
				this.mesh.getTriangles()[i].setLight((double)this.light/MAX_LIGHT_INTENSITY);
			}
		}
	}

	public void removeMesh(){
		this.mesh = null;
	}

	public boolean isTransparent(){
		return Atlas.MAIN_ATLAS.isTransparent(this.type);
	}

	public boolean isLiquid(){
		return Atlas.MAIN_ATLAS.isLiquid(this.type);
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

	public String getType(){
		return this.type;
	}

	public int getId(){
		return Atlas.MAIN_ATLAS.getBlockId(this.type);
	}
}