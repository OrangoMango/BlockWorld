package com.orangomango.blockworld.model;

import javafx.scene.image.Image;
import javafx.geometry.Point3D;
import javafx.geometry.Point2D;

import java.util.*;
import java.io.*;

import org.json.JSONObject;

public class BlockMesh{
	private static class Vertex{
		private Point3D position;
		private Point2D tex;

		public Vertex(Point3D pos, Point2D tex){
			this.position = pos;
			this.tex = tex;
		}
	}

	private static class Face{
		private Point3D[] points;
		private Point2D startUv, endUv;
		private boolean culling;
		private int imageIndex;
		private int name;

		public Face(int name, Point3D[] v, Point2D sUv, Point2D eUv, boolean culling, int imageIndex){
			this.name = name;
			this.points = v;
			this.startUv = sUv;
			this.endUv = eUv;
			this.culling = culling;
			this.imageIndex = imageIndex;
		}

		public List<Vertex> getVertices(){
			Vertex v1 = new Vertex(points[0], new Point2D(startUv.getX(), startUv.getY()));
			Vertex v2 = new Vertex(points[1], new Point2D(startUv.getX(), endUv.getY()));
			Vertex v3 = new Vertex(points[2], new Point2D(endUv.getX(), endUv.getY()));
			Vertex v4 = new Vertex(points[3], new Point2D(endUv.getX(), startUv.getY()));
			List<Vertex> vertices = new ArrayList<>();
			vertices.add(v1);
			vertices.add(v2);
			vertices.add(v3);
			vertices.add(v4);
			return vertices;
		}
	}

	private static class Element{
		private List<Face> faces;
		private List<Point3D> vertices;
		private List<Point2D> tex;

		public Element(List<Face> faces){
			this.faces = faces;
			this.vertices = this.faces.stream().flatMap(f -> f.getVertices().stream()).map(v -> v.position).distinct().map(p -> p.multiply(1/16.0)).toList();
			this.tex = this.faces.stream().flatMap(f -> f.getVertices().stream()).map(v -> v.tex).distinct().map(p -> p.multiply(1/16.0)).toList();
		}

		public int[] buildImageIndices(){
			List<Integer> list = new ArrayList<>();
			for (Face face : this.faces){
				list.add(face.imageIndex); // 2 triangles
				list.add(face.imageIndex);
			}
			return list.stream().mapToInt(i -> i.intValue()).toArray();
		}

		public int[][] buildFacesPoints(){
			List<int[]> output = new ArrayList<>();
			for (Face face : this.faces){
				List<Vertex> v = face.getVertices();
				int v1 = this.vertices.indexOf(v.get(0).position.multiply(1/16.0));
				int v2 = this.vertices.indexOf(v.get(1).position.multiply(1/16.0));
				int v3 = this.vertices.indexOf(v.get(2).position.multiply(1/16.0));
				int v4 = this.vertices.indexOf(v.get(3).position.multiply(1/16.0));
				output.add(new int[]{v1, v2, v3});
				output.add(new int[]{v1, v3, v4});
			}
			return output.toArray(new int[output.size()][3]);
		}

		public int[][] buildFacesTextures(){
			List<int[]> output = new ArrayList<>();
			for (Face face : this.faces){
				List<Vertex> v = face.getVertices();
				int v1 = this.tex.indexOf(v.get(0).tex.multiply(1/16.0));
				int v2 = this.tex.indexOf(v.get(1).tex.multiply(1/16.0));
				int v3 = this.tex.indexOf(v.get(2).tex.multiply(1/16.0));
				int v4 = this.tex.indexOf(v.get(3).tex.multiply(1/16.0));
				output.add(new int[]{v1, v2, v3});
				output.add(new int[]{v1, v3, v4});
			}
			return output.toArray(new int[output.size()][3]);
		}

		public Map<Integer, List<Integer>> buildCullingIdx(){
			Map<Integer, List<Integer>> output = new HashMap<>();
			int count = 0;
			for (Face face : this.faces){
				if (face.culling){
					List<Integer> idx = output.getOrDefault(face.name, null);
					if (idx == null){
						idx = new ArrayList<Integer>();
					}
					idx.add(count);
					idx.add(count+1);
					output.put(face.name, idx);
				}
				count += 2;
			}
			return output;
		}
	}

	private JSONObject textures;
	private JSONObject meshJson;
	private Image[] images;
	private int[] imageIndices;
	private Point3D[] vertices;
	private Point2D[] tex;
	private int[][] facesPoints;
	private int[][] facesTex;
	private Map<Integer, List<Integer>> cullingIdx = new HashMap<>();

	// Hide pattern
	public static final int FACE_FRONT = 32;
	public static final int FACE_RIGHT = 16;
	public static final int FACE_BACK = 8;
	public static final int FACE_LEFT = 4;
	public static final int FACE_DOWN = 2;
	public static final int FACE_TOP = 1;

	public BlockMesh(String meshFile, JSONObject textures){
		this.textures = textures;

		List<Image> img = new ArrayList<>();
		for (Object o : this.textures.getJSONArray("images")){
			Image image = new Image(getClass().getResourceAsStream((String)o));
			img.add(image);
		}
		this.images = img.toArray(new Image[img.size()]);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(getClass().getResource(meshFile).toURI())));
			StringBuilder builder = new StringBuilder();
			reader.lines().forEach(builder::append);
			reader.close();
			this.meshJson = new JSONObject(builder.toString());
		} catch (Exception ex){
			ex.printStackTrace();
		}

		Element element = mergeElements(buildElements());
		this.vertices = element.vertices.toArray(new Point3D[element.vertices.size()]);
		this.tex = element.tex.toArray(new Point2D[element.tex.size()]);
		this.facesPoints = element.buildFacesPoints();
		this.facesTex = element.buildFacesTextures();
		this.imageIndices = element.buildImageIndices();
		this.cullingIdx = element.buildCullingIdx();
	}

	private Element mergeElements(List<Element> elements){
		List<Face> faces = new ArrayList<>(elements.stream().flatMap(e -> e.faces.stream()).toList());
		Element e = new Element(faces);
		return e;
	}

	private List<Element> buildElements(){
		List<Element> output = new ArrayList<>();
		for (Object e : this.meshJson.getJSONArray("elements")){
			JSONObject element = (JSONObject)e;
			output.add(makeElement(element));
		}

		return output;
	}

	private Element makeElement(JSONObject json){
		Point3D start = new Point3D(json.getJSONArray("from").getDouble(0), json.getJSONArray("from").getDouble(1), json.getJSONArray("from").getDouble(2));
		Point3D end = new Point3D(json.getJSONArray("to").getDouble(0), json.getJSONArray("to").getDouble(1), json.getJSONArray("to").getDouble(2));
		List<Face> faces = new ArrayList<>();

		for (String f : json.getJSONObject("faces").keySet()){
			JSONObject data = json.getJSONObject("faces").getJSONObject(f);
			Point2D sUv = new Point2D(data.getJSONArray("uv").getInt(0), data.getJSONArray("uv").getInt(1));
			Point2D eUv = new Point2D(data.getJSONArray("uv").getInt(2), data.getJSONArray("uv").getInt(3));
			boolean culling = data.getBoolean("culling");
			int imageIndex = this.textures.getJSONObject("config").optInt(f, -1);
			if (f.equals("front")){
				Point3D a = new Point3D(start.getX(), start.getY(), start.getZ()); // sx sy sz
				Point3D b = new Point3D(start.getX(), end.getY(), start.getZ()); // sx ey sz
				Point3D c = new Point3D(end.getX(), end.getY(), start.getZ()); // ex ey sz
				Point3D d = new Point3D(end.getX(), start.getY(), start.getZ()); // ex sy sz
				Face face = new Face(FACE_FRONT, new Point3D[]{a, b, c, d}, sUv, eUv, culling, imageIndex);
				faces.add(face);
			} else if (f.equals("right")){
				Point3D a = new Point3D(end.getX(), start.getY(), start.getZ()); // ex sy sz
				Point3D b = new Point3D(end.getX(), end.getY(), start.getZ()); // ex ey sz
				Point3D c = new Point3D(end.getX(), end.getY(), end.getZ()); // ex ey ez
				Point3D d = new Point3D(end.getX(), start.getY(), end.getZ()); // ex sy ez
				Face face = new Face(FACE_RIGHT, new Point3D[]{a, b, c, d}, sUv, eUv, culling, imageIndex);
				faces.add(face);
			} else if (f.equals("back")){
				Point3D a = new Point3D(end.getX(), start.getY(), end.getZ()); // ex sy ez
				Point3D b = new Point3D(end.getX(), end.getY(), end.getZ()); // ex ey ez
				Point3D c = new Point3D(start.getX(), end.getY(), end.getZ()); // sx ey ez
				Point3D d = new Point3D(start.getX(), start.getY(), end.getZ()); // sx sy ez
				Face face = new Face(FACE_BACK, new Point3D[]{a, b, c, d}, sUv, eUv, culling, imageIndex);
				faces.add(face);
			} else if (f.equals("left")){
				Point3D a = new Point3D(start.getX(), start.getY(), end.getZ()); // sx sy ez
				Point3D b = new Point3D(start.getX(), end.getY(), end.getZ()); // sx ey ez
				Point3D c = new Point3D(start.getX(), end.getY(), start.getZ()); // sx ey sz
				Point3D d = new Point3D(start.getX(), start.getY(), start.getZ()); // sx sy sz
				Face face = new Face(FACE_LEFT, new Point3D[]{a, b, c, d}, sUv, eUv, culling, imageIndex);
				faces.add(face);
			} else if (f.equals("down")){
				Point3D a = new Point3D(start.getX(), end.getY(), start.getZ()); // sx ey sz
				Point3D b = new Point3D(start.getX(), end.getY(), end.getZ()); // sx ey ez
				Point3D c = new Point3D(end.getX(), end.getY(), end.getZ()); // ex ey ez
				Point3D d = new Point3D(end.getX(), end.getY(), start.getZ()); // ex ey sz
				Face face = new Face(FACE_DOWN, new Point3D[]{a, b, c, d}, sUv, eUv, culling, imageIndex);
				faces.add(face);
			} else if (f.equals("top")){
				Point3D a = new Point3D(start.getX(), start.getY(), end.getZ()); // sx sy ez
				Point3D b = new Point3D(start.getX(), start.getY(), start.getZ()); // sx sy sz
				Point3D c = new Point3D(end.getX(), start.getY(), start.getZ()); // ex sy sz
				Point3D d = new Point3D(end.getX(), start.getY(), end.getZ()); // ex sy ez
				Face face = new Face(FACE_TOP, new Point3D[]{a, b, c, d}, sUv, eUv, culling, imageIndex);
				faces.add(face);
			}
		}

		Element element = new Element(faces);
		return element;
	}

	public Image[] getImages(){
		return this.images;
	}

	public int[] getImageIndices(){
		return this.imageIndices;
	}

	public Point3D[] getVertices(){
		return this.vertices;
	}

	public Point2D[] getTex(){
		return this.tex;
	}

	public int[][] getFacesPoints(){
		return this.facesPoints;
	}

	public int[][] getFacesTex(){
		return this.facesTex;
	}

	public Map<Integer, List<Integer>> getCullingIdx(){
		return this.cullingIdx;
	}
}