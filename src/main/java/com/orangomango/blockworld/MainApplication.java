package com.orangomango.blockworld;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.util.Pair;
import javafx.geometry.Point3D;

import com.orangomango.rendering3d.Engine3D;
import com.orangomango.rendering3d.model.Light;
import com.orangomango.rendering3d.model.MeshVertex;
import com.orangomango.blockworld.model.*;
import com.orangomango.blockworld.util.Util;
import com.orangomango.blockworld.entity.Player;
import com.orangomango.blockworld.console.Console;

/**
 * Minecraft-clone using a 3D engine made from scratch in Java/JavaFX
 * 
 * @author OrangoMango (https://orangomango.github.io)
 * @version 1.0
 */
public class MainApplication extends Application{
	private static final int WIDTH = 320;
	private static final int HEIGHT = 180;
	private static final int CHUNKS = 9;

	private static Image POINTER = new Image(MainApplication.class.getResourceAsStream("/images/pointer.png"));
	public static Engine3D ENGINE;

	private String currentBlock = "wood";
	private Color backgroundColor = Color.CYAN;
	private double time = 1;
	private boolean amTime = false;
	
	@Override
	public void start(Stage stage){
		stage.setTitle("BlockWorld");
		ENGINE = new Engine3D(stage, WIDTH, HEIGHT);

		Player player = new Player(0, -15, 0, WIDTH, HEIGHT);
		ENGINE.setCamera(player.getCamera());
		Light light = new Light(1);
		ENGINE.getLights().add(light);

		World world = new World((int)System.currentTimeMillis(), false);
		System.out.println("Seed: "+world.getSeed());
		ChunkManager manager = new ChunkManager(world, CHUNKS);
		manager.deleteSavedWorld();

		Thread dayNight = new Thread(() -> {
			int direction = -1;
			final double inc = 0.0008;
			while (true){
				try {
					this.time += inc*direction;
					if (this.time < 0 || this.time > 1){
						this.amTime = !this.amTime;
					}
					direction = this.amTime ? 1 : -1;
					this.time = Math.min(Math.max(0, this.time), 1);
					light.setFixedIntensity(this.time);
					this.backgroundColor = Color.hsb(this.backgroundColor.getHue(), this.backgroundColor.getSaturation(), this.time);
					ENGINE.setBackgroundColor(this.backgroundColor);
					Thread.sleep(250);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		dayNight.setDaemon(true);
		dayNight.start();

		// Console
		Console console = new Console(command -> {
			if (command.startsWith("/")){
				String[] args = command.split(" ");
				if (args[0].equals("/give")){
					try {
						int id = Integer.parseInt(args[1]);
						this.currentBlock = Atlas.MAIN_ATLAS.getBlockType(id);
						System.out.println(this.currentBlock);
					} catch (NumberFormatException ex){
						System.out.println("Error");
					}
				} else if (args[0].equals("/settime")){
					try {
						Pair<Double, Boolean> time = Util.parseTime(args[1]);
						this.time = time.getKey();
						this.amTime = time.getValue();
					} catch (NumberFormatException ex){
						System.out.println("Error");
					}
				} else if (args[0].equals("/quit") || args[0].equals("/exit")){
					System.exit(0);
				} else if (args[0].equals("/list")){
					int n = Atlas.MAIN_ATLAS.getMaxId();
					for (int i = 1; i <= n; i++){
						System.out.println(i+". "+Atlas.MAIN_ATLAS.getBlockType(i));
					}
				} else if (args[0].equals("/tp")){
					double x = Double.parseDouble(args[1]);
					double y = Double.parseDouble(args[2]);
					double z = Double.parseDouble(args[3]);
					player.getCamera().setPosition(new Point3D(x, y, z));
				} else if (args[0].equals("/save")){
					manager.saveWorld();
				} else if (args[0].equals("/delete")){
					world.setSeed((int)System.currentTimeMillis());
					System.out.println("Seed: "+world.getSeed());
					manager.deleteSavedWorld();
					manager.manage(Util.getChunkPos(player.getPosition()));
				}
			}
		});
		console.start();

		// Chunk managing
		player.setOnChunkPositionChanged(chunkPos -> {
			manager.manage(chunkPos);
		});

		// Ray-casting
		ENGINE.setOnMousePressed(e -> {
			Block block = null;

			double stepX = Math.cos(player.getRx())*Math.cos(player.getRy()+Math.PI/2);
			double stepY = -Math.sin(player.getRx());
			double stepZ = Math.cos(player.getRx())*Math.sin(player.getRy()+Math.PI/2);

			int lastX = 0;
			int lastY = 0;
			int lastZ = 0;

			for (double i = 0; i <= 10; i += 0.01){
				int lX = (int)(player.getX()+i*stepX);
				int lY = (int)(player.getY()+i*stepY);
				int lZ = (int)(player.getZ()+i*stepZ);
				block = world.getBlockAt(lX, lY, lZ);
				if (block == null || i == 0 || block.isLiquid()){
					lastX = lX;
					lastY = lY;
					lastZ = lZ;
				}
				if (block != null && !block.isLiquid()) break;
			}
			if (block != null){
				boolean chunkUpdate = false;
				Block down = null; // The underneath liquid block
				if (e.getButton() == MouseButton.PRIMARY){
					world.removeBlockAt(block.getX(), block.getY(), block.getZ());
					int chunkX = block.getX() / Chunk.CHUNK_SIZE;
					int chunkY = block.getY() / Chunk.CHUNK_SIZE;
					int chunkZ = block.getZ() / Chunk.CHUNK_SIZE;
					chunkUpdate = true;
					down = world.getBlockAt(block.getX(), block.getY()+1, block.getZ());
				} else if (e.getButton() == MouseButton.SECONDARY && lastX >= 0 && lastY >= 0 && lastZ >= 0){
					world.setBlockAt(lastX, lastY, lastZ, this.currentBlock);
					int chunkX = lastX / Chunk.CHUNK_SIZE;
					int chunkY = lastY / Chunk.CHUNK_SIZE;
					int chunkZ = lastZ / Chunk.CHUNK_SIZE;
					chunkUpdate = true;
					down = world.getBlockAt(lastX, lastY+1, lastZ);
				}
				if (chunkUpdate){
					if (down != null && down.isLiquid()){
						down.removeMesh(); // Update liquid blocks in case the user placed a block on top of them
					}

					// TODO (maybe don't update the entire chunk)
					for (int i = -1; i < 2; i++){
						for (int j = -1; j < 2; j++){
							for (int k = -1; k < 2; k++){
								Chunk chunk = world.getChunkAt(block.getX()/Chunk.CHUNK_SIZE+i, block.getY()/Chunk.CHUNK_SIZE+k, block.getZ()/Chunk.CHUNK_SIZE+j);
								if (chunk != null){
									chunk.updateMesh();
								}
							}
						}
					}
				}
			}
		});

		// Player movement
		final double speed = 0.4;
		ENGINE.setOnKey(KeyCode.W, () -> player.move(speed*Math.cos(player.getRy()+Math.PI/2), 0, speed*Math.sin(player.getRy()+Math.PI/2)), false);
		ENGINE.setOnKey(KeyCode.A, () -> player.move(-speed*Math.cos(player.getRy()), 0, -speed*Math.sin(player.getRy())), false);
		ENGINE.setOnKey(KeyCode.S, () -> player.move(-speed*Math.cos(player.getRy()+Math.PI/2), 0, -speed*Math.sin(player.getRy()+Math.PI/2)), false);
		ENGINE.setOnKey(KeyCode.D, () -> player.move(speed*Math.cos(player.getRy()), 0, speed*Math.sin(player.getRy())), false);
		ENGINE.setOnKey(KeyCode.SPACE, () -> player.move(0, -speed, 0), false);
		ENGINE.setOnKey(KeyCode.SHIFT, () -> player.move(0, speed, 0), false);

		// Settings
		ENGINE.setOnKey(KeyCode.O, ENGINE::toggleMouseMovement, true);
		ENGINE.setOnKey(KeyCode.R, player.getCamera()::reset, true);

		// Quick inventory
		ENGINE.setOnKey(KeyCode.DIGIT1, () -> this.currentBlock = "wood", true);
		ENGINE.setOnKey(KeyCode.DIGIT2, () -> this.currentBlock = "glass", true);
		ENGINE.setOnKey(KeyCode.DIGIT3, () -> this.currentBlock = "cobblestone", true);
		ENGINE.setOnKey(KeyCode.DIGIT4, () -> this.currentBlock = "wood_log", true);
		ENGINE.setOnKey(KeyCode.DIGIT5, () -> this.currentBlock = "stone", true);
		ENGINE.setOnKey(KeyCode.DIGIT6, () -> this.currentBlock = "dirt", true);
		ENGINE.setOnKey(KeyCode.DIGIT7, () -> this.currentBlock = "sand", true);
		ENGINE.setOnKey(KeyCode.DIGIT8, () -> this.currentBlock = "coal", true);
		ENGINE.setOnKey(KeyCode.DIGIT9, () -> this.currentBlock = "debug", true);
		ENGINE.setOnKey(KeyCode.DIGIT0, () -> this.currentBlock = "cactus", true);

		ENGINE.setOnPreUpdate(gc -> {
			console.runLastCommand();

			for (Chunk chunk : world.getChunks()){
				for (int i = 0; i < Chunk.CHUNK_SIZE; i++){
					for (int j = 0; j < Chunk.CHUNK_SIZE; j++){
						for (int k = 0; k < Chunk.CHUNK_SIZE; k++){
							Block block = chunk.getBlockAt(i, j, k);
							if (block != null){
								block.update();
							}
						}
					}
				}
			}
		});

		ENGINE.setOnUpdate(gc -> {
			gc.setFill(Color.BLACK);
			gc.setTextAlign(TextAlignment.RIGHT);
			gc.setFont(new Font("sans-serif", 11));
			String text = "Projected: "+MeshVertex.getProjectedVerticesCount();
			text += "\nView: "+MeshVertex.getViewVerticesCount();
			text += "\n"+Util.formatTime(this.time, this.amTime);
			gc.fillText(text, WIDTH*0.95, HEIGHT*0.1);

			// Show pointer
			double pointerSize = 26*player.getCamera().getAspectRatio();
			gc.drawImage(POINTER, WIDTH/2.0-pointerSize/2, HEIGHT/2.0-pointerSize/2, pointerSize, pointerSize);
		});
		
		stage.setResizable(false);
		stage.setScene(ENGINE.getScene());
		stage.show();
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
