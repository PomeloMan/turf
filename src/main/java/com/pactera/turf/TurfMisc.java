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
import com.pactera.astar.AStar;
import com.pactera.astar.MatrixMap;

public class TurfMisc {

	private TurfMisc() {
		throw new AssertionError("No Instances.");
	}

	public static Feature shortestPath(Point startPt, Point endPt, GeoJson geojson) {
		// Normalize Inputs
		Feature startFt = Feature.fromGeometry(startPt);
		Feature endFt = Feature.fromGeometry(endPt);

		// Handle obstacles
		FeatureCollection obstacles = null;
		FeatureCollection collection = null;
		System.out.println(geojson.type());
		if ("FeatureCollection".equals(geojson.type())) {
			FeatureCollection featureCollection = (FeatureCollection) geojson;
			if (featureCollection.features().size() == 0) {
				return Feature.fromGeometry(LineString.fromLngLats(Lists.newArrayList(startPt, endPt)));
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
//		double[] box = { -6.052378646445732, -7.300000000000001, 10.052378646445732, -2.7000000000000015 };

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
				matrixRow[c] = isInsideObstacle ? 0 : 1;
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
		MatrixMap info = new MatrixMap(matrix[0].length, matrix.length, 1, (FeatureCollection) obstacles);
		info.shortestPath(new double[] { closestToStart[0], closestToStart[1] },
				new double[] { closestToEnd[0], closestToEnd[1] });
		AStar astar = new AStar();
		astar.start(info);
		List<int[]> results = astar.getPathList();
		Collections.reverse(results);

		List<Point> path = Lists.newArrayList();
		path.add(startPt);
		for (int[] coord : results) {
			String[] coords = pointMatrix[coord[1]][coord[0]].split("\\|");
			path.add(Point.fromLngLat(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
		}
		path.add(endPt);

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println();
		}

		return Feature.fromGeometry(LineString.fromLngLats(path));
	}

	/**
	 * Checks if Point is inside any of the Polygons
	 *
	 * @private
	 * @param {Feature<Point>}             pt to check
	 * @param {FeatureCollection<Polygon>} polygons features
	 * @returns {boolean} if inside or not
	 */
	private static boolean isInside(Point pt, FeatureCollection polygons) {
		for (int i = 0; i < polygons.features().size(); i++) {
			if (TurfBooleans.booleanPointInPolygon(pt, (Polygon) polygons.features().get(i).geometry())) {
				return true;
			}
		}
		return false;
	}
}
