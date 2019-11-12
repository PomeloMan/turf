package com.pactera.turf;

import java.util.List;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Point;

public class TurfUtils {

	private TurfUtils() {
		throw new AssertionError("No Instances.");
	}

	/**
	 * inBBox
	 *
	 * @private
	 * @param {Position} pt point [x,y]
	 * @param {BBox}     bbox BBox [west, south, east, north]
	 * @returns {boolean} true/false if point is inside BBox
	 */
	public static boolean inBBox(Point pt, BoundingBox bbox) {
		return bbox.west() <= pt.longitude() && bbox.south() <= pt.latitude() && bbox.east() >= pt.longitude()
				&& bbox.north() >= pt.latitude();
	}

	/**
	 * inRing
	 *
	 * @private
	 * @param {Array<number>}        pt [x,y]
	 * @param {Array<Array<number>>} ring [[x,y], [x,y],..]
	 * @param {boolean}              ignoreBoundary ignoreBoundary
	 * @returns {boolean} inRing
	 */
	public static boolean inRing(Point pt, List<Point> ring, boolean ignoreBoundary) {
		boolean isInside = false;
		if (ring.get(0).longitude() == ring.get(ring.size() - 1).longitude()
				&& ring.get(0).latitude() == ring.get(ring.size() - 1).latitude())
			ring.remove(ring.size() - 1);

		for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
			double xi = ring.get(i).longitude();
			double yi = ring.get(i).latitude();
			double xj = ring.get(j).longitude();
			double yj = ring.get(j).latitude();
			boolean onBoundary = (pt.latitude() * (xi - xj) + yi * (xj - pt.longitude())
					+ yj * (pt.longitude() - xi) == 0) && ((xi - pt.longitude()) * (xj - pt.longitude()) <= 0)
					&& ((yi - pt.latitude()) * (yj - pt.latitude()) <= 0);
			if (onBoundary)
				return !ignoreBoundary;
			boolean intersect = ((yi > pt.latitude()) != (yj > pt.latitude()))
					&& (pt.longitude() < (xj - xi) * (pt.latitude() - yi) / (yj - yi) + xi);
			if (intersect)
				isInside = !isInside;
		}
		return isInside;
	}
}
