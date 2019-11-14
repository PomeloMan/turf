package com.pactera.turf;

import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class TurfTransformation {

	private TurfTransformation() {
	}

	public static GeoJson transformScale(GeoJson geojson, double factor) {
		// Input validation
		if (geojson == null)
			throw new Error("geojson required");
		if (factor == 0)
			throw new Error("invalid factor");

		// Scale each Feature separately
	    if (geojson.type().equals("FeatureCollection")) {
	    	((FeatureCollection) geojson).features().stream().map((feature)->{
	    		return scale((Feature)feature, factor);	    		
	    	});
	        return geojson;
	    }
	    // Scale Feature/Geometry
	    return scale((Feature)geojson, factor);
	}

	/**
	 * Scale Feature/Geometry
	 *
	 * @private
	 * @param {Feature|Geometry} feature GeoJSON Feature/Geometry
	 * @param {number}           factor of scaling, positive or negative values
	 *                           greater than 0
	 * @param {string|Coord}     [origin="centroid"] Point from which the scaling
	 *                           will occur (string options:
	 *                           sw/se/nw/ne/center/centroid)
	 * @returns {Feature|Geometry} scaled GeoJSON Feature/Geometry
	 */
	private static Feature scale(Feature feature, double factor) {
		// Default params
		boolean isPoint = feature.type() == "Point";
		Point origin = defineOrigin(feature);

		// Shortcut no-scaling
		if (factor == 1 || isPoint)
			return feature;

		Geometry geo = feature.geometry();
		if (geo.type().equals("Polygon")) {
			List<List<Point>> coordinates = ((Polygon) geo).coordinates();
			coordinates.get(0).stream().forEach((point) -> {
				double originalDistance = TurfMeasurement.rhumbDistance(origin, point);
				double bearing = TurfMeasurement.rhumbBearing(origin, point);
				double newDistance = originalDistance * factor;
				Point newCoord = TurfMeasurement.rhumbDestination(origin, newDistance, bearing);
				point.coordinates().set(0, newCoord.longitude());
				point.coordinates().set(1, newCoord.latitude());
			});
		}

		return feature;
	}

	/**
	 * Define Origin
	 *
	 * @private
	 * @param {GeoJSON}      geojson GeoJSON
	 * @param {string|Coord} origin sw/se/nw/ne/center/centroid
	 * @returns {Feature<Point>} Point origin
	 */
	private static Point defineOrigin(GeoJson geojson) {
		return TurfMeasurement.center(geojson);
	}
}
