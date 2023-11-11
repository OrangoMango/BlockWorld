package com.orangomango.blockworld.util;

import javafx.geometry.Point3D;
import javafx.util.Pair;

import com.orangomango.blockworld.model.Chunk;

public class Util{
	public static String formatTime(double value, boolean amTime){
		// 0 -> 00:00 PM
		// 1 -> 12:00 PM
		if (!amTime){
			value = 1-value;
		}
		int hours = (int)(value*12);
		int minutes = (int)((value*12-hours)*60);
		if (!amTime){
			hours += 12;
		}
		return String.format("%02d:%02d", hours, minutes);
	}

	public static Pair<Double, Boolean> parseTime(String text){
		String[] data = text.split(" ");
		int hours = Integer.parseInt(data[0].split(":")[0]);
		int minutes = Integer.parseInt(data[0].split(":")[1]);
		boolean amTime = hours < 12;
		double amount = ((hours%12)*60+minutes)/(12*60.0);
		if (!amTime){
			amount = 1-amount;
		}
		return new Pair<Double, Boolean>(amount, amTime);
	}

	public static Point3D getChunkPos(Point3D pos){
		int chunkX = (int)(pos.getX()/Chunk.CHUNK_SIZE);
		int chunkY = (int)(pos.getY()/Chunk.CHUNK_SIZE);
		int chunkZ = (int)(pos.getZ()/Chunk.CHUNK_SIZE);
		return new Point3D(chunkX, chunkY, chunkZ);
	}
}