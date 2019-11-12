package com.pactera.turf.geojson;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Polygon;

import junit.framework.TestCase;

public class GeoJsonTest extends TestCase {

	public GeoJsonTest(String testName) {
		super(testName);
	}

	public void polygonFromJson() {
		String polygonJson = "{\"coordinates\":[[[0,-7],[5,-7],[5,-3],[0,-3],[0,-7]]]}";
		Polygon polygon = Polygon.fromJson(polygonJson);
		System.out.println(polygon);
	}

	public void featureFromJson() {
		String featureJson = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,-7],[5,-7],[5,-3],[0,-3],[0,-7]]]}}";
		Feature feature = Feature.fromJson(featureJson);
		System.out.println(feature);
	}
}
