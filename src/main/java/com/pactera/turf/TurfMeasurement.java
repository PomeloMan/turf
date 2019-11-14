package com.pactera.turf;

import java.util.List;

import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConversion;

public class TurfMeasurement {

	// 地球半径
	public static final double earth_radius = 6371008.8;

	private TurfMeasurement() {

	}

	public static Point center(GeoJson geojson) {
		// Input validation
		if (geojson == null)
			throw new Error("geojson is required");

		double[] ext = com.mapbox.turf.TurfMeasurement.bbox(geojson);
		double x = (ext[0] + ext[2]) / 2;
		double y = (ext[1] + ext[3]) / 2;
		return Point.fromLngLat(x, y);
	}

	/**
	 * Calculates the distance along a rhumb line between two {@link Point|points}
	 * in degrees, radians, miles, or kilometers.
	 *
	 * @name rhumbDistance
	 * @param {Coord}  from origin point
	 * @param {Coord}  to destination point
	 * @param {Object} [options] Optional parameters
	 * @param {string} [options.units="kilometers"] can be degrees, radians, miles,
	 *                 or kilometers
	 * @returns {number} distance between the two points
	 */
	public static double rhumbDistance(Point from, Point to) {
		// validation
		if (from == null)
			throw new Error("from point is required");
		if (to == null)
			throw new Error("to point is required");

		List<Double> origin = from.coordinates();
		List<Double> destination = to.coordinates();
		if (destination.get(0) - origin.get(0) > 180) {
			destination.set(0, destination.get(0) + (-360));
		} else if (origin.get(0) - destination.get(0) > 180) {
			destination.set(0, destination.get(0) + (360));
		} else {
			// do nothing
		}
		double distanceInMeters = calculateRhumbDistance(origin, destination);
		double distance = TurfConversion.convertLength(distanceInMeters, "meters");

		return distance;
	}

	/**
	 * Returns the destination {@link Point} having travelled the given distance
	 * along a Rhumb line from the origin Point with the (varant) given bearing.
	 * 
	 * @reference https://en.wikipedia.org/wiki/Rhumb_line
	 * @name rhumbDestination
	 * @param {Coord}  origin starting point
	 * @param {number} distance distance from the starting point
	 * @param {number} bearing
	 * 
	 * @return {Feature<Point>} Destination point.
	 */
	public static Point rhumbDestination(Point origin, double distance, double bearing) {
		// validation
		if (origin == null)
			throw new Error("origin is required");
		if (!(distance >= 0))
			throw new Error("distance must be greater than 0");

		double distanceInMeters = TurfConversion.convertLength(distance, "kilometers", "meters");
		double[] coords = new double[] { origin.longitude(), origin.latitude() };
		double[] destination = calculateRhumbDestination(coords, distanceInMeters, bearing);

		// compensate the crossing of the 180th meridian
		// (https://macwright.org/2016/09/26/the-180th-meridian.html)
		// solution from
		// https://github.com/mapbox/mapbox-gl-js/issues/3250#issuecomment-294887678
		destination[0] += (destination[0] - coords[0] > 180) ? -360 : (coords[0] - destination[0] > 180) ? 360 : 0;
		return Point.fromLngLat(destination[0], destination[1]);
	}

	public static double rhumbBearing(Point start, Point end) {
		return rhumbBearing(start, end, false);
	}

	/**
	 * Takes two {@link Point|points} and finds the bearing angle between them along
	 * a Rhumb line i.e. the angle measured in degrees start the north line (0
	 * degrees)
	 *
	 * @name rhumbBearing
	 * @param {Coord}   start starting Point
	 * @param {Coord}   end ending Point
	 * @param {Object}  [options] Optional parameters
	 * @param {boolean} [options.final=false] calculates the final bearing if true
	 * @returns {number} bearing from north in decimal degrees, between -180 and 180
	 *          degrees (positive clockwise)
	 * @reference // https://en.wikipedia.org/wiki/Rhumb_line
	 */
	public static double rhumbBearing(Point start, Point end, boolean isFinal) {
		// validation
		if (start == null)
			throw new Error("start point is required");
		if (end == null)
			throw new Error("end point is required");

		double bear360 = 0;
		if (isFinal) {
			bear360 = calculateRhumbBearing(end, start);
		} else {
			bear360 = calculateRhumbBearing(start, end);
		}

		double bear180 = (bear360 > 180) ? -(360 - bear360) : bear360;

		return bear180;
	}

	/**
	 * Returns the distance travelling from ‘this’ point to destination point along
	 * a rhumb line. Adapted from Geodesy:
	 * https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
	 * 
	 * @param origin      point
	 * @param destination point
	 * @return Distance in km between this point and destination point (same units
	 *         as radius).
	 */
	private static double calculateRhumbDistance(List<Double> origin, List<Double> destination) {

		double phi1 = origin.get(1) * Math.PI / 180;
		double phi2 = destination.get(1) * Math.PI / 180;
		double DeltaPhi = phi2 - phi1;
		double DeltaLambda = Math.abs(destination.get(0) - origin.get(0)) * Math.PI / 180;
		// if dLon over 180° take shorter rhumb line across the anti-meridian:
		if (DeltaLambda > Math.PI)
			DeltaLambda -= 2 * Math.PI;

		// on Mercator projection, longitude distances shrink by latitude; q is the
		// 'stretch factor'
		// q becomes ill-conditioned along E-W line (0/0); use empirical tolerance to
		// avoid it
		double DeltaPsi = Math.log(Math.tan(phi2 / 2 + Math.PI / 4) / Math.tan(phi1 / 2 + Math.PI / 4));
		double q = Math.abs(DeltaPsi) > 10e-12 ? DeltaPhi / DeltaPsi : Math.cos(phi1);

		// distance is pythagoras on 'stretched' Mercator projection
		double delta = Math.sqrt(DeltaPhi * DeltaPhi + q * q * DeltaLambda * DeltaLambda); // angular distance in
																							// radians
		double dist = delta * earth_radius;

		return dist;
	}

	/**
	 * Returns the bearing from ‘this’ point to destination point along a rhumb
	 * line. Adapted from Geodesy:
	 * https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
	 * 
	 * @param {Array<number>} from - origin point.
	 * @param {Array<number>} to - destination point.
	 * @returns {number} Bearing in degrees from north.
	 */
	private static double calculateRhumbBearing(Point from, Point to) {
		// φ => phi
		// Δλ => deltaLambda
		// Δψ => deltaPsi
		// θ => theta
		double phi1 = TurfConversion.degreesToRadians(from.latitude());
		double phi2 = TurfConversion.degreesToRadians(to.latitude());
		double deltaLambda = TurfConversion.degreesToRadians((to.longitude() - from.longitude()));
		// if deltaLambdaon over 180° take shorter rhumb line across the anti-meridian:
		if (deltaLambda > Math.PI)
			deltaLambda -= 2 * Math.PI;
		if (deltaLambda < -Math.PI)
			deltaLambda += 2 * Math.PI;

		double deltaPsi = Math.log(Math.tan(phi2 / 2 + Math.PI / 4) / Math.tan(phi1 / 2 + Math.PI / 4));

		double theta = Math.atan2(deltaLambda, deltaPsi);

		return (TurfConversion.radiansToDegrees(theta) + 360) % 360;
	}

	/**
	 * Returns the destination point having travelled along a rhumb line from origin
	 * point the given distance on the given bearing. Adapted from Geodesy:
	 * http://www.movable-type.co.uk/scripts/latlong.html#rhumblines
	 * 
	 * @param {Array<number>} origin - point
	 * @param {number}        distance - Distance travelled, in same units as earth
	 *                        radius (default: metres).
	 * @param {number}        bearing - Bearing in degrees from north.
	 * @param {number}        [radius=6371e3] - (Mean) radius of earth (defaults to
	 *                        radius in metres).
	 * @return {Array<number>} Destination point.
	 */
	private static double[] calculateRhumbDestination(double[] origin, double distance, double bearing) {
		// φ => phi
		// λ => lambda
		// ψ => psi
		// Δ => Delta
		// δ => delta
		// θ => theta
		double delta = distance / earth_radius; // angular distance in radians
		double lambda1 = origin[0] * Math.PI / 180; // to radians, but without normalize to 𝜋
		double phi1 = TurfConversion.degreesToRadians(origin[1]);
		double theta = TurfConversion.degreesToRadians(bearing);

		double DeltaPhi = delta * Math.cos(theta);
		double phi2 = phi1 + DeltaPhi;

		// check for some daft bugger going past the pole, normalise latitude if so
		if (Math.abs(phi2) > Math.PI / 2)
			phi2 = phi2 > 0 ? Math.PI - phi2 : -Math.PI - phi2;

		double DeltaPsi = Math.log(Math.tan(phi2 / 2 + Math.PI / 4) / Math.tan(phi1 / 2 + Math.PI / 4));
		double q = Math.abs(DeltaPsi) > 10e-12 ? DeltaPhi / DeltaPsi : Math.cos(phi1); // E-W course becomes
																						// ill-conditioned with 0/0

		double DeltaLambda = delta * Math.sin(theta) / q;
		double lambda2 = lambda1 + DeltaLambda;

		return new double[] { ((lambda2 * 180 / Math.PI) + 540) % 360 - 180, phi2 * 180 / Math.PI }; // normalise to
																										// −180..+180°
	}

}
