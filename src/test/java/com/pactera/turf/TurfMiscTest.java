package com.pactera.turf;

import java.util.Calendar;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import junit.framework.TestCase;

public class TurfMiscTest extends TestCase {

	public void shortestPath() {
		System.out.println(Calendar.getInstance().getTimeInMillis());
		String featureJson1 = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,-5],[5,-5],[5,-3],[0,-3],[0,-5]]]}}";
		String featureJson2 = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,-7],[5,-7],[5,-5],[0,-5],[0,-7]]]}}";
		String featureJson3 = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,-7],[5,-7],[5,-3],[0,-3],[0,-7]]]}}";
		Feature feature1 = Feature.fromJson(featureJson1);
		Feature feature2 = Feature.fromJson(featureJson2);
		Feature feature3 = Feature.fromJson(featureJson3);
		FeatureCollection coll = FeatureCollection.fromFeatures(new Feature[] { feature1, feature2 });
		TurfMisc.shortestPath(Point.fromJson("{\"coordinates\":[-5,-6]}"), Point.fromJson("{\"coordinates\":[9,-6]}"),
				coll);
		TurfMisc.shortestPath(Point.fromJson("{\"coordinates\":[-5,-6]}"), Point.fromJson("{\"coordinates\":[9,-6]}"),
				feature3);
		System.out.println(Calendar.getInstance().getTimeInMillis());
	}

	public void bbox() {
		String featureJson3 = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[117228079, 31750482],[117228079, 31750497],[5,-3],[0,-3],[0,-7]]]}}";
		Feature feature3 = Feature.fromJson(featureJson3);
		double[] d = TurfMeasurement.bbox(feature3);
		for (double e : d) {
			System.out.print(e + ",");
		}
	}
}
