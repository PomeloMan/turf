package com.pactera.turf;

import java.util.List;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class TurfBooleans {

	private TurfBooleans() {
		throw new AssertionError("No Instances.");
	}

	public static boolean booleanPointInPolygon(Point point, Polygon polygon) {
		// validation
		if (point == null)
			throw new Error("point is required");
		if (polygon == null)
			throw new Error("polygon is required");

		List<List<Point>> polys = polygon.coordinates();
		BoundingBox bbox = polygon.bbox();

		// Quick elimination if point is not inside bbox
		if (bbox != null && TurfUtils.inBBox(point, bbox) == false)
			return false;

		boolean insidePoly = false;
		// check if it is in the outer ring first
		if (TurfUtils.inRing(point, polys.get(0), false)) {
			boolean inHole = false;
			int k = 1;
			// check for the point in any of the holes
			while (k < polys.size() && !inHole) {
				if (TurfUtils.inRing(point, polys.get(k), true)) {
					inHole = true;
				}
				k++;
			}
			if (!inHole)
				insidePoly = true;
		}
		return insidePoly;
	}
}
