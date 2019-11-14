package com.pactera.turf;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfMeasurement;
import com.pactera.astar.MatrixMap;

public class TurfMisc {

	private TurfMisc() {
	}

	public static LineString shortestPath(Point startPt, Point endPt, GeoJson geojson) {
		// Normalize Inputs
		Feature startFt = Feature.fromGeometry(startPt);
		Feature endFt = Feature.fromGeometry(endPt);

		// Handle obstacles
		FeatureCollection obstacles = null;
		FeatureCollection collection = null;
		if ("FeatureCollection".equals(geojson.type())) {
			FeatureCollection featureCollection = (FeatureCollection) geojson;
			if (featureCollection.features().size() == 0) {
				return LineString.fromLngLats(Lists.newArrayList(startPt, endPt));
			} else {
				obstacles = featureCollection;
				List<Feature> features = Lists.newArrayList(featureCollection.features());
				features.add(startFt);
				features.add(endFt);
				collection = FeatureCollection.fromFeatures(features);
			}
		} else if ("Feature".equals(geojson.type())) {
			Feature feature = (Feature) geojson;
			if ("Polygon".equals(feature.geometry().type())) {
				// define path grid area
				obstacles = FeatureCollection.fromFeatures(new Feature[] { Feature.fromGeometry(feature.geometry()) });
				collection = FeatureCollection
						.fromFeatures(new Feature[] { Feature.fromGeometry(feature.geometry()), startFt, endFt });
			}
		} else {
			throw new Error("invalid obstacles");
		}

		double[] box = TurfMeasurement.bbox(com.pactera.turf.TurfTransformation
				.transformScale(TurfMeasurement.bboxPolygon(TurfMeasurement.bbox(collection)), 1.15));

		double width = TurfMeasurement.distance(Point.fromLngLat(box[0], box[1]), Point.fromLngLat(box[2], box[1]));
		double resolution = width / 100;

		double west = box[0];
		double south = box[1];
		double east = box[2];
		double north = box[3];

		double xFraction = resolution
				/ (TurfMeasurement.distance(Point.fromLngLat(west, south), Point.fromLngLat(east, south)));
		double cellWidth = xFraction * (east - west);
		double yFraction = resolution
				/ (TurfMeasurement.distance(Point.fromLngLat(west, south), Point.fromLngLat(west, north)));
		double cellHeight = yFraction * (north - south);

		double bboxHorizontalSide = (east - west);
		double bboxVerticalSide = (north - south);
		double columns = Math.floor(bboxHorizontalSide / cellWidth);
		double rows = Math.floor(bboxVerticalSide / cellHeight);
		// adjust origin of the grid
		double deltaX = (bboxHorizontalSide - columns * cellWidth) / 2;
		double deltaY = (bboxVerticalSide - rows * cellHeight) / 2;

		// loop through points only once to speed up process
		// define matrix grid for A-star algorithm
		String[][] pointMatrix = new String[(int) rows + 1][(int) columns + 1];
		int[][] matrix = new int[(int) rows + 1][(int) columns + 1];
		int[] closestToStart = new int[2];
		int[] closestToEnd = new int[2];
		double minDistStart = Double.MAX_VALUE;
		double minDistEnd = Double.MAX_VALUE;
		double currentY = north - deltaY;
		int r = 0;
		while (currentY >= south) {
			String[] pointMatrixRow = new String[(int) columns + 1];
			int[] matrixRow = new int[(int) columns + 1];
			double currentX = west + deltaX;
			int c = 0;
			while (currentX <= east) {
				Point pt = Point.fromLngLat(currentX, currentY);
				boolean isInsideObstacle = isInside(pt, obstacles);
				matrixRow[c] = isInsideObstacle ? 1 : 0;
				// map point's coords
				pointMatrixRow[c] = currentX + "|" + currentY;
				// set closest points
				double distStart = TurfMeasurement.distance(pt, startPt);
				if (!isInsideObstacle && distStart < minDistStart) {
					minDistStart = distStart;
					closestToStart[0] = c;
					closestToStart[1] = r;
				}
				double distEnd = TurfMeasurement.distance(pt, endPt);
				if (!isInsideObstacle && distEnd < minDistEnd) {
					minDistEnd = distEnd;
					closestToEnd[0] = c;
					closestToEnd[1] = r;
				}

				currentX += cellWidth;
				c++;
			}
			matrix[r] = matrixRow;
			pointMatrix[r] = pointMatrixRow;
			currentY -= cellHeight;
			r++;
		}

		// a-star algorithm
		MatrixMap info = new MatrixMap();
		info.setMatrix(matrix);
		info.shortestPath(new double[] { closestToStart[0], closestToStart[1] },
				new double[] { closestToEnd[0], closestToEnd[1] });
		List<int[]> results = info.getPaths();
		Collections.reverse(results);

		List<Point> path = Lists.newArrayList();
		path.add(startPt);
		for (int[] coord : results) {
			String[] coords = pointMatrix[coord[1]][coord[0]].split("\\|");
			path.add(Point.fromLngLat(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
		}
		path.add(endPt);

		return LineString.fromLngLats(path);
	}

	/**
	 * Checks if Point is inside any of the Polygons
	 *
	 * @private
	 * @param {Feature<Point>}             pt to check
	 * @param {FeatureCollection<Polygon>} polygons features
	 * @returns {boolean} if inside or not
	 */
	public static boolean isInside(Point pt, FeatureCollection collection) {
		for (int i = 0; i < collection.features().size(); i++) {
			Feature feature = collection.features().get(i);
			if (feature.geometry().type().equals("Polygon")) {
				if (TurfBooleans.booleanPointInPolygon(pt, (Polygon) feature.geometry())) {
					return true;
				}
			} else if (feature.geometry().type().equals("Point")) {
				if (pt.longitude() == ((Point) feature.geometry()).longitude()
						&& pt.latitude() == ((Point) feature.geometry()).latitude()) {
					return true;
				}
			} else if (feature.geometry().type().equals("LineString")) {

			}
		}
		return false;
	}
}
