package com.pactera.astar;

import java.util.Calendar;

public class Test {

	public static void main(String[] args) {
		System.out.println(Calendar.getInstance().getTimeInMillis());
		int[][] maps = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0 },
				{ 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0 }, { 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 } };
		MatrixMap info = new MatrixMap(maps[0].length, maps.length);
		info.setMatrix(maps);
		info.shortestPath(new double[] { 1, 5 }, new double[] { 10, 2 });
	}

	/**
	 * 打印地图
	 */
	public static void printMap(int[][] maps) {
		for (int i = 0; i < maps.length; i++) {
			for (int j = 0; j < maps[i].length; j++) {
				System.out.print(maps[i][j] + " ");
			}
			System.out.println();
		}
	}

}
