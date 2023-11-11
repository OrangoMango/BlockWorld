package com.orangomango.blockworld.model;

import javafx.scene.image.Image;
import javafx.geometry.Point3D;

import java.io.*;
import java.util.*;
import org.json.JSONObject;

public class Atlas{
	private JSONObject json;
	private Map<Integer, String> blockIds = new HashMap<>();
	private Map<String, BlockMesh> blocks = new HashMap<>();

	public static Atlas MAIN_ATLAS;

	static {
		MAIN_ATLAS = new Atlas("/atlas.json");
	}

	public Atlas(String name){
		try {
			File file = new File(Atlas.class.getResource(name).toURI());
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuilder builder = new StringBuilder();
			reader.lines().forEach(line -> builder.append(line).append("\n"));
			reader.close();
			this.json = new JSONObject(builder.toString());
		} catch (Exception ex){
			ex.printStackTrace();
		}

		for (String blockType : this.json.getJSONObject("blocks").keySet()){
			this.blockIds.put(getBlockId(blockType), blockType);
			BlockMesh block = new BlockMesh(this.json.getJSONObject("blocks").getJSONObject(blockType).getString("mesh"), this.json.getJSONObject("blocks").getJSONObject(blockType).getJSONObject("textures"));
			this.blocks.put(blockType, block);
		}

		/*BlockMesh blockMesh = getBlockMesh("debug");
		System.out.println("Vertices: "+Arrays.toString(blockMesh.getVertices()));
		System.out.println("Faces points:");
		Arrays.asList(blockMesh.getFacesPoints()).stream().forEach(l -> System.out.println(Arrays.toString(l)));
		System.out.println("Images: "+Arrays.toString(blockMesh.getImages()));
		System.out.println("Image idx: "+Arrays.toString(blockMesh.getImageIndices()));
		System.out.println("Texture v: "+Arrays.toString(blockMesh.getTex()));
		System.out.println("Faces tex:");
		Arrays.asList(blockMesh.getFacesTex()).stream().forEach(l -> System.out.println(Arrays.toString(l)));
		System.exit(0);*/
	}

	public int getMaxId(){
		return this.blockIds.keySet().stream().mapToInt(i -> i.intValue()).max().getAsInt();
	}
	
	public int getBlockId(String blockType){
		return this.json.getJSONObject("blocks").getJSONObject(blockType).getInt("id");
	}

	public String getBlockType(int id){
		return this.blockIds.get(id);
	}
	
	public boolean isTransparent(String blockType){
		return this.json.getJSONObject("blocks").getJSONObject(blockType).getBoolean("transparent");
	}
	
	public boolean isSprite(String blockType){
		return false; //this.json.getJSONObject("blocks").getJSONObject(blockType).optBoolean("sprite"); // TODO
	}
	
	public boolean isLiquid(String blockType){
		return this.json.getJSONObject("blocks").getJSONObject(blockType).getBoolean("liquid");
	}

	public int getHidePattern(String blockType){
		return this.json.getJSONObject("blocks").getJSONObject(blockType).getInt("hidePattern");
	}

	public BlockMesh getBlockMesh(String blockType){
		return this.blocks.get(blockType);
	}
}