package com.hackmobile.fridge;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Item {

	String name;
	int[][] histogram;
	String imageFileName;
	Time dateOfCreation;

	public Item(String name, int[][] histogram, String imageFileName, Time dateOfCreation) {
		this.name = name;
		this.histogram = histogram;
		this.imageFileName = imageFileName;
		this.dateOfCreation = dateOfCreation;
	}
	
	public String matchItem(String json) { // attempt to find match under _threshhold_ from known items
		JsonParser jsonParser = new JsonParser();
		JsonArray items = null;
		try {
			items = (JsonArray) jsonParser.parse(json);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String name = "";
		int smallestDiff = Integer.MAX_VALUE;
		for (JsonElement item : items) {
			JsonArray hist = item.getAsJsonObject().getAsJsonArray("histogram");
			int[] rgbDiffs = new int[3];
			for (int i = 0; i < 3; i++) {
				int stockSum = 0;
				int currentSum = 0;
				JsonArray rgbArray = hist.get(i).getAsJsonArray();
				for (int j = 0; j < 6; j++) {
					stockSum += rgbArray.get(j).getAsInt();
					currentSum += this.histogram[i][j];
				}
				rgbDiffs[i] = Math.abs(stockSum - currentSum);
			}
			int diffSum = 0;
			for (int i = 0; i < 3; i++) {
				diffSum += rgbDiffs[i];
			}
			if (diffSum < smallestDiff) {
				name = item.getAsJsonObject().get("name").getAsString();
				smallestDiff = diffSum;
			}
		}
		this.name = name;
		Log.d("Fridge", "Assigned the new image a name of " + name);
		return name;
	}

}
