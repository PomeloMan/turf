package com.pactera.astar;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;
import com.pactera.astar.modal.Node;
import com.pactera.turf.TurfMisc;

public class MatrixMap {

	/**
	 * 矩阵图
	 */
	private int[][] matrix;
	/**
	 * 矩阵中每一格代表的坐标值 '|' 隔开; 例: x|y
	 */
	private String[][] pointMatrix;
	/**
	 * 起始节点
	 */
	private Node start;
	/**
	 * 最终节点
	 */
	private Node end;
	/**
	 * 起始节点到最终节点的长度(代价)
	 */
	private double length;
	/**
	 * 起始节点到最终节点的路径集合
	 */
	private List<int[]> paths = Lists.newArrayList();
	/**
	 * 出入口坐标对应的网格 例: ("x|y", [6,12])
	 */
	private Map<String, int[]> entMap = new HashMap<>();
	private Map<String, Double> distanceMap = new HashMap<>();

	public MatrixMap() {
	}

	public MatrixMap(GeoJson geojson) {
		this.generateMatrix(geojson);
	}

	/**
	 * 生成矩阵图
	 * 
	 * @reference com.pactera.turf.TurfMisc.shortestPath()
	 */
	public Map<String, Object> generateMatrix(GeoJson geojson) {
		// Handle obstacles
		FeatureCollection obstacles = null;
		FeatureCollection collection = null;
		if ("FeatureCollection".equals(geojson.type())) {
			FeatureCollection featureCollection = (FeatureCollection) geojson;
			if (featureCollection.features().size() == 0) {
				throw new Error("null obstacles");
			} else {
				obstacles = featureCollection;
				List<Feature> features = Lists.newArrayList(featureCollection.features());
				collection = FeatureCollection.fromFeatures(features);
			}
		} else if ("Feature".equals(geojson.type())) {
			Feature feature = (Feature) geojson;
			if ("Polygon".equals(feature.geometry().type())) {
				// define path grid area
				obstacles = FeatureCollection.fromFeatures(new Feature[] { Feature.fromGeometry(feature.geometry()) });
				collection = FeatureCollection.fromFeatures(new Feature[] { Feature.fromGeometry(feature.geometry()) });
			}
		} else {
			throw new Error("invalid obstacles");
		}

		this.initEntMap(obstacles);

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
		double currentY = north - deltaY;
		int r = 0;
		while (currentY >= south) {
			String[] pointMatrixRow = new String[(int) columns + 1];
			int[] matrixRow = new int[(int) columns + 1];
			double currentX = west + deltaX;
			int c = 0;
			while (currentX <= east) {
				Point pt = Point.fromLngLat(currentX, currentY);
				boolean isInsideObstacle = TurfMisc.isInside(pt, obstacles);
				matrixRow[c] = isInsideObstacle ? 1 : 0;
				// map point's coords
				pointMatrixRow[c] = currentX + "|" + currentY;

				// 计算当前点与出入口的最小距离并保存出入口对应的网格坐标
				this.saveEntMap(pt, new int[] { r, c });

				currentX += cellWidth;
				c++;
			}
			matrix[r] = matrixRow;
			pointMatrix[r] = pointMatrixRow;
			currentY -= cellHeight;
			r++;
		}

		this.matrix = matrix;
		this.pointMatrix = pointMatrix;

		Map<String, Object> matrixMap = Maps.newHashMap();
		matrixMap.put("matrix", matrix);
		matrixMap.put("pointMatrix", pointMatrix);
		return matrixMap;
	}

	/**
	 * 计算2点间最短距离
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public LineString shortestPath(double[] start, double[] end) {
		if (this.matrix == null) {
			throw new Error("没有矩阵图");
		}
		int[] startGrid = this.entMap.get(start[0] + "|" + start[1]);
		int[] endGrid = this.entMap.get(end[0] + "|" + end[1]);

		// 这里entMap取出来的数据直接是xy坐标格式,需要转换为二维数组格式(既x代表column,y代表row)
		if (startGrid != null && endGrid != null) {
			this.start = new Node(startGrid[1], startGrid[0]);
			this.end = new Node(endGrid[1], endGrid[0]);
		} else {
			this.start = new Node((int) start[0], (int) start[1]);
			this.end = new Node((int) end[0], (int) end[1]);
		}
		AStar astar = new AStar();
		astar.start(this);

		List<int[]> results = this.getPaths();
		Collections.reverse(results);
		List<Point> path = Lists.newArrayList();
		path.add(Point.fromLngLat(start[0], start[1]));
		for (int[] coord : results) {
			String[] coords = pointMatrix[coord[1]][coord[0]].split("\\|");
			path.add(Point.fromLngLat(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
		}
		path.add(Point.fromLngLat(end[0], end[1]));

		return LineString.fromLngLats(path);
	}

	/**
	 * 根据参数找出出入口对象信息
	 * 
	 * @param collection
	 */
	private void initEntMap(FeatureCollection collection) {
		collection.features().forEach(feature -> {
			if (feature.geometry().type().equals("Point")) {
				Point point = (Point) feature.geometry();
				this.entMap.put(point.longitude() + "|" + point.latitude(), null);
			}
		});
	}

	/**
	 * 跟新出入口对应网格
	 * 
	 * @param point
	 * @param position
	 */
	private void saveEntMap(Point current, int[] position) {
		this.entMap.keySet().forEach((key) -> {
			String[] xy = key.split("\\|");
			double newDistance = TurfMeasurement.distance(current,
					Point.fromLngLat(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
			Double distance = this.distanceMap.get(key);
			if (distance == null || newDistance < distance) {
				this.distanceMap.put(key, newDistance);
				this.entMap.put(key, position);
			}
		});

	}

	/**
	 * Getter & Setter
	 */
	public int[][] getMatrix() {
		return matrix;
	}

	public void setMatrix(int[][] matrix) {
		this.matrix = matrix;
	}

	public String[][] getPointMatrix() {
		return pointMatrix;
	}

	public void setPointMatrix(String[][] pointMatrix) {
		this.pointMatrix = pointMatrix;
	}

	public Node getStart() {
		return start;
	}

	public void setStart(Node start) {
		this.start = start;
	}

	public Node getEnd() {
		return end;
	}

	public void setEnd(Node end) {
		this.end = end;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public List<int[]> getPaths() {
		return paths;
	}

	public void setPaths(List<int[]> paths) {
		this.paths = paths;
	}

}
