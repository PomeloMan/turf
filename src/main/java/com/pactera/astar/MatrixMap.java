package com.pactera.astar;

import java.util.Calendar;
import java.util.List;

import com.google.common.collect.Lists;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.pactera.astar.modal.Node;
import com.pactera.turf.TurfBooleans;

public class MatrixMap {

	/**
	 * 矩阵图
	 */
	private int[][] matrix;
	/**
	 * 颗粒度，单位：米
	 */
	private int unit = 1;
	/**
	 * 长
	 */
	private int width;
	/**
	 * 宽
	 */
	private int height;
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

	public MatrixMap(int width, int height) {
		this(width, height, 1);
	}

	public MatrixMap(int width, int height, int unit) {
		this(width, height, unit, null);
	}

	public MatrixMap(int width, int height, int unit, FeatureCollection obstacles) {
		this.width = width;
		this.height = height;
		this.unit = unit;
		if (width % this.unit != 0 || height % this.unit != 0) {
			throw new Error("当前颗粒度与长宽比无法切分矩阵图");
		}
		this.generateMatrix(obstacles);
	}

	/**
	 * 生成矩阵图
	 * 
	 * @param width     长
	 * @param height    宽
	 * @param obstacles 障碍物
	 */
	public int[][] generateMatrix(FeatureCollection obstacles) {
		double west = 0;
		double south = 0;
		double east = this.width;
		double north = this.height;

		int columns = (int) (east - west) / unit;
		int rows = (int) (north - south) / unit;

		int[][] matrix = new int[rows][columns];

		double currentY = north;
		int r = 0;
		while (currentY > south) {
			int[] matrixRow = new int[columns];
			double currentX = west;
			int c = 0;
			while (currentX < east) {
				Point pt = Point.fromLngLat(currentX, currentY);
				boolean isInsideObstacle = isInside(pt, obstacles);
				matrixRow[c] = isInsideObstacle ? 1 : 0;
				currentX += unit;
				c++;
			}
			matrix[r] = matrixRow;
			currentY -= unit;
			r++;
		}
		this.matrix = matrix;
		return matrix;
	}

	/**
	 * 计算2点间最短距离
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public double shortestPath(double[] start, double[] end) {
		if (this.matrix == null) {
			throw new Error("没有矩阵图");
		}
		this.start = new Node((int) start[0] / this.unit, (int) (matrix.length - start[1] / this.unit - 1));
		this.end = new Node((int) end[0] / this.unit, (int) (matrix.length - end[1] / this.unit - 1));
		AStar astar = new AStar();
		astar.start(this);
//		for (int i = 0; i < matrix.length; i++) {
//			for (int j = 0; j < matrix[i].length; j++) {
//				System.out.print(matrix[i][j] + " ");
//			}
//			System.out.println();
//		}
		return astar.getTotalCost();
	}

	/**
	 * Checks if Point is inside any of the Polygons
	 *
	 * @private
	 * @param {Feature<Point>}             pt to check
	 * @param {FeatureCollection<Polygon>} polygons features
	 * @returns {boolean} if inside or not
	 */
	private boolean isInside(Point pt, FeatureCollection polygons) {
		if (polygons != null) {
			for (int i = 0; i < polygons.features().size(); i++) {
				if (TurfBooleans.booleanPointInPolygon(pt, (Polygon) polygons.features().get(i).geometry())) {
					return true;
				}
			}
		}
		return false;
	}

	public static void main(String[] args) {
		System.out.println(Calendar.getInstance().getTimeInMillis());
		String featureJson1 = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[5,5],[15,5],[15,50],[5,50],[5,5]]]}}";
		String featureJson2 = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[20.5,5],[30,5],[30,50],[21,50],[20.5,5]]]}}";
		Feature feature1 = Feature.fromJson(featureJson1);
		Feature feature2 = Feature.fromJson(featureJson2);
		FeatureCollection coll = FeatureCollection.fromFeatures(new Feature[] { feature1, feature2 });
		MatrixMap map = new MatrixMap(96, 63, 1, null);
		map.generateMatrix(coll);
		System.out.println(Calendar.getInstance().getTimeInMillis());

		for (int i = 0; i < 5; i++) {
			System.out.println(Calendar.getInstance().getTimeInMillis() + "--start");
			System.out.println(map.shortestPath(new double[] { 5, 5 }, new double[] { 50 + i, 5 + i }));
			System.out.println(Calendar.getInstance().getTimeInMillis() + "--end");

			System.out.println("------------------------");
			System.out.println(map.getLength());
			System.out.println(map.getPaths().size());
			System.out.println("------------------------");
		}
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

	public int getUnit() {
		return unit;
	}

	public void setUnit(int unit) {
		this.unit = unit;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
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
