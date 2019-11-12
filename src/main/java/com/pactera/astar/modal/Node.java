package com.pactera.astar.modal;

/**
 * ClassName: Node
 * 
 * @Description: 路径结点
 */
public class Node implements Comparable<Node> {

	/**
	 * 坐标
	 */
	private Coord coord;
	/**
	 * 父结点
	 */
	private Node parent;
	/**
	 * G：是个准确的值，是起点到当前结点的代价
	 */
	private double G;
	/**
	 * H：是个估值，当前结点到目的结点的估计代价
	 */
	private double H;

	public Node(int x, int y) {
		this.coord = new Coord(x, y);
	}

	public Node(Coord coord, Node parent, double g, double h) {
		this.coord = coord;
		this.parent = parent;
		G = g;
		H = h;
	}

	public int compareTo(Node o) {
		if (o == null)
			return -1;
		if (G + H > o.G + o.H)
			return 1;
		else if (G + H < o.G + o.H)
			return -1;
		return 0;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public double getG() {
		return G;
	}

	public void setG(double g) {
		G = g;
	}

	public double getH() {
		return H;
	}

	public void setH(double h) {
		H = h;
	}

}
