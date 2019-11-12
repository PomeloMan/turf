package com.pactera.astar;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import com.google.common.collect.Lists;
import com.pactera.astar.modal.Coord;
import com.pactera.astar.modal.Node;

/**
 * ClassName: AStar
 * 
 * @Description: A星算法
 */
public class AStar {

	/**
	 * 障碍值
	 */
	public final static int BAR = 1;
	/**
	 * 路径
	 */
	public final static int PATH = 2;
	/**
	 * 平移代价
	 */
	private double straightEdge = 1;
	/**
	 * 斜移代价
	 */
	private double bevelEdge = Math.sqrt(2);
	/**
	 * 临时矩阵图
	 */
	private int[][] matrix;

	public AStar() {
	}

	public AStar(double straightEdge) {
		this.straightEdge = straightEdge;
		this.bevelEdge = Math.sqrt(Math.pow(this.straightEdge, 2) + Math.pow(this.straightEdge, 2));
	}

	Queue<Node> openList = new PriorityQueue<Node>(); // 优先队列(升序)
	List<Node> closeList = new ArrayList<Node>();

	double totalCost = 0;
	List<int[]> pathList = new ArrayList<int[]>();

	/**
	 * 开始算法
	 */
	public void start(MatrixMap mapInfo) {
		if (mapInfo == null)
			return;

		openList.clear();
		closeList.clear();

		// 复制二维数组,画路径图时使用临时二维数组从而不污染原图数据
		int[][] matrix = mapInfo.getMatrix();
		this.matrix = new int[matrix.length][matrix[0].length];
		for (int i = 0; i < matrix.length; i++) {
			System.arraycopy(matrix[i], 0, this.matrix[i], 0, matrix[0].length);
		}

		openList.add(mapInfo.getStart());
		moveNodes(mapInfo);
	}

	/**
	 * 移动当前结点
	 */
	private void moveNodes(MatrixMap mapInfo) {
		while (!openList.isEmpty()) {
			if (isCoordInClose(mapInfo.getEnd().getCoord())) { // 到达终点
				mapInfo.setLength(mapInfo.getEnd().getG()); // 设置总长度(代价)
				mapInfo.setPaths(Lists.newArrayList()); // 清空路径集合
				drawPath(this.matrix, mapInfo.getEnd(), mapInfo.getPaths()); // 画路线图并保存路径坐标
				break;
			}
			Node current = openList.poll();
			closeList.add(current);
			addNeighborNodeInOpen(mapInfo, current);
		}
		AStar.printMap(this.matrix);
	}

	/**
	 * 在二维数组中绘制路径
	 */
	private void drawPath(int[][] maps, Node end, List<int[]> paths) {
		if (end == null || maps == null)
			return;
		while (end != null) {
			Coord c = end.getCoord();
			maps[c.getY()][c.getX()] = PATH;
			end = end.getParent();
			paths.add(new int[] { c.getX(), c.getY() });
		}
	}

	/**
	 * 添加所有邻结点到open表
	 */
	private void addNeighborNodeInOpen(MatrixMap mapInfo, Node current) {
		int x = current.getCoord().getX();
		int y = current.getCoord().getY();

		addNeighborNodeInOpen(mapInfo, current, x - 1, y, straightEdge); // 左
		addNeighborNodeInOpen(mapInfo, current, x, y - 1, straightEdge); // 上
		addNeighborNodeInOpen(mapInfo, current, x + 1, y, straightEdge); // 右
		addNeighborNodeInOpen(mapInfo, current, x, y + 1, straightEdge); // 下
		addNeighborNodeInOpen(mapInfo, current, x - 1, y - 1, bevelEdge); // 左上
		addNeighborNodeInOpen(mapInfo, current, x + 1, y - 1, bevelEdge); // 右上
		addNeighborNodeInOpen(mapInfo, current, x + 1, y + 1, bevelEdge); // 右下
		addNeighborNodeInOpen(mapInfo, current, x - 1, y + 1, bevelEdge); // 左下
	}

	/**
	 * 添加一个邻结点到open表
	 */
	private void addNeighborNodeInOpen(MatrixMap mapInfo, Node current, int x, int y, double value) {
		if (canAddNodeToOpen(mapInfo, x, y)) {
			Node end = mapInfo.getEnd();
			Coord coord = new Coord(x, y);
			double G = current.getG() + value;
			Node child = findNodeInOpen(coord);
			if (child == null) {
				int H = calcH(end.getCoord(), coord);
				if (isEndNode(end.getCoord(), coord)) {
					child = end;
					child.setParent(current);
					;
					child.setG(G);
					child.setH(H);
				} else {
					child = new Node(coord, current, G, H);
				}
				openList.add(child);
			} else if (child.getG() > G) {
				child.setG(G);
				child.setParent(current);
				openList.add(child);
			}
		}
	}

	/**
	 * 从Open列表中查找结点
	 */
	private Node findNodeInOpen(Coord coord) {
		if (coord == null || openList.isEmpty())
			return null;
		for (Node node : openList) {
			if (node.getCoord().equals(coord)) {
				return node;
			}
		}
		return null;
	}

	/**
	 * 计算H的估值：“曼哈顿”法，坐标分别取差值相加
	 */
	private int calcH(Coord end, Coord coord) {
		return Math.abs(end.getX() - coord.getX()) + Math.abs(end.getY() - coord.getY());
	}

	/**
	 * 判断结点是否是最终结点
	 */
	private boolean isEndNode(Coord end, Coord coord) {
		return coord != null && end.equals(coord);
	}

	/**
	 * 判断结点能否放入Open列表
	 */
	private boolean canAddNodeToOpen(MatrixMap mapInfo, int x, int y) {
		// 是否在地图中
		if (x < 0 || x >= mapInfo.getWidth() || y < 0 || y >= mapInfo.getHeight())
			return false;
		// 判断是否是不可通过的结点
		if (this.matrix[y][x] == BAR)
			return false;
		// 判断结点是否存在close表
		if (isCoordInClose(x, y))
			return false;

		return true;
	}

	/**
	 * 判断坐标是否在close表中
	 */
	private boolean isCoordInClose(Coord coord) {
		return coord != null && isCoordInClose(coord.getX(), coord.getY());
	}

	/**
	 * 判断坐标是否在close表中
	 */
	private boolean isCoordInClose(int x, int y) {
		if (closeList.isEmpty())
			return false;
		for (Node node : closeList) {
			if (node.getCoord().getX() == x && node.getCoord().getY() == y) {
				return true;
			}
		}
		return false;
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

	/**
	 * Getter & Setter
	 * 
	 * @return
	 */
	public List<int[]> getPathList() {
		return pathList;
	}

	public void setPathList(List<int[]> pathList) {
		this.pathList = pathList;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public double getStraightEdge() {
		return straightEdge;
	}

	public void setStraightEdge(double straightEdge) {
		this.straightEdge = straightEdge;
	}

	public double getBevelEdge() {
		return bevelEdge;
	}

	public void setBevelEdge(double bevelEdge) {
		this.bevelEdge = bevelEdge;
	}

	public Queue<Node> getOpenList() {
		return openList;
	}

	public void setOpenList(Queue<Node> openList) {
		this.openList = openList;
	}

	public List<Node> getCloseList() {
		return closeList;
	}

	public void setCloseList(List<Node> closeList) {
		this.closeList = closeList;
	}

}
