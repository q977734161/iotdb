package org.apache.iotdb.db.query.eBUG;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tool {
    // 计算三角形面积
    public static double triArea(Point d0, Point d1, Point d2) {
        double dArea = ((d1.x - d0.x) * (d2.y - d0.y) - (d2.x - d0.x) * (d1.y - d0.y)) / 2.0;
        return (dArea > 0.0) ? dArea : -dArea; // abs
    }

    // 计算多边形面积（鞋带公式）
    public static double calculatePolygonArea(List<Point> points) {
        // points多边形顶点，按照逆时针或者顺时针的顺序枚举
        int n = points.size();
        double area = 0;
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            area += points.get(i).x * points.get(next).y - points.get(next).x * points.get(i).y;
        }
        return Math.abs(area) / 2.0;
    }

    // 计算两个向量的叉积
    public static double crossProduct(double x1, double y1, double x2, double y2) {
        //  >0: (x2,y2)在(x1,y1)的逆时针方向
        //  <0: (x2,y2)在(x1,y1)的顺时针方向
        //  =0: 平行或共线
        return x1 * y2 - y1 * x2;
    }

    // 判断两条线段是否相交并计算交点
    // L1包含一个线段的两个端点，L2包含另一个线段的两个端点
    public static Object[] lineIntersection(Point[] L1, Point[] L2) {
        double x1 = L1[0].x, y1 = L1[0].y, x2 = L1[1].x, y2 = L1[1].y;
        double x3 = L2[0].x, y3 = L2[0].y, x4 = L2[1].x, y4 = L2[1].y;

        // 判断是否相交（叉积）
        double d1 = crossProduct(x2 - x1, y2 - y1, x3 - x1, y3 - y1);
        double d2 = crossProduct(x2 - x1, y2 - y1, x4 - x1, y4 - y1);
        double d3 = crossProduct(x4 - x3, y4 - y3, x1 - x3, y1 - y3);
        double d4 = crossProduct(x4 - x3, y4 - y3, x2 - x3, y2 - y3);

        // 如果叉积条件满足，表示有交点
        // d1*d2<0意味着P3、P4分别在L12的两边
        // d3*d4<0意味着P1、P2分别在L34的两边
        // 两个同时满足说明有交点
        if (d1 * d2 < 0 && d3 * d4 < 0) {
            double denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1); // 不可能为0（平行或共线），因为已经判断相交了
            double t1 = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator;
            double x = x1 + t1 * (x2 - x1);
            double y = y1 + t1 * (y2 - y1);
            return new Object[]{true, new Point(x, y)};
        }

        // 检查是否起点或终点重合
        if ((x1 == x3 && y1 == y3) || (x1 == x4 && y1 == y4)) {
            return new Object[]{true, new Point(x1, y1)};
        }
        if ((x2 == x3 && y2 == y3) || (x2 == x4 && y2 == y4)) {
            return new Object[]{true, new Point(x2, y2)};
        }

        return new Object[]{false, null};
    }

    // 计算总的多边形面积（通过时间序列扫描交点）
    public static double total_areal_displacement(List<Point> points, List<Point> points2, boolean debug) {
        double totalArea = 0;
        int i = 0, j = 0; // i for points, j for points2
        Point prevIntersection = null;
        int prevI = -1, prevJ = -1;

//        List<double[]> intersectionCoords = new ArrayList<>();
        List<Double> areaList = new ArrayList<>();

        while (i < points.size() - 1 && j < points2.size() - 1) {
            if (debug) {
                System.out.println("--------- " + i + " " + j + " ------------");
            }

            // 当前线段
            Point[] L1 = {points.get(i), points.get(i + 1)};
            Point[] L2 = {points2.get(j), points2.get(j + 1)};

            // 判断是否有交点
            Object[] result = lineIntersection(L1, L2);
            boolean isIntersect = (boolean) result[0];
            Point intersection = (Point) result[1];

            if (isIntersect) {
//                intersectionCoords.add(intersection);

                if (prevIntersection != null) {
                    // 构造多边形点集
                    List<Point> polygon = new ArrayList<>(); // 按照顺时针/逆时针几何连接顺序枚举多边形的顶点
                    polygon.add(prevIntersection);
                    if (debug) {
                        System.out.println("- start intersection: " + prevIntersection);
                    }
                    polygon.addAll(points.subList(prevI, i + 1)); // 添加当前线段的点，左闭右开
//                    polygon.addAll(Arrays.asList(Arrays.copyOfRange(points, prevI, i + 1)));  // 添加当前线段的点，左闭右开
                    if (debug) {
                        System.out.println("- one side: " + points.subList(prevI, i + 1));
                    }
                    polygon.add(intersection);
                    if (debug) {
                        System.out.println("- end intersection: " + intersection);
                    }
                    List<Point> tempPoints2 = points2.subList(prevJ, j + 1);
                    Collections.reverse(tempPoints2);  // 添加另一序列的点
                    polygon.addAll(tempPoints2);
                    if (debug) {
                        System.out.println("- another side: " + tempPoints2);
                    }

//                    double[][] polygonArray = new double[polygon.size()][2];
//                    for (int k = 0; k < polygon.size(); k++) {
//                        polygonArray[k] = polygon.get(k);
//                    }

                    // 计算多边形面积
                    double area = calculatePolygonArea(polygon);
                    if (debug) {
                        System.out.println("Area = " + area);
                    }
                    totalArea += area;
                    areaList.add(area);
                }

                prevIntersection = intersection;
                prevI = i + 1;
                prevJ = j + 1;
                if (debug) {
                    System.out.println("This intersection = " + intersection
                            + ", next polygon: side1 = " + prevI + ", side2 = " + prevJ);
                }
            }


            int currentI = i; // 临时存储i
            int currentJ = j; // 临时存储j
            if (points.get(currentI + 1).x <= points2.get(currentJ + 1).x) {
                i += 1; // 基于时间戳严格递增的假设，Line不会回头或者垂直
            }
            if (points.get(currentI + 1).x >= points2.get(currentJ + 1).x) {
                j += 1; // 基于时间戳严格递增的假设，Line不会回头或者垂直
            }
        } // end of while

        if (debug) {
            System.out.println(areaList);
        }

        return totalArea;
    }

    // 测试方法
    public static void main(String[] args) {
        // 示例数据
        List<Point> points = new ArrayList<>();
        points.add(new Point(0, 0));
        points.add(new Point(1, 2));
        points.add(new Point(2, -20));
        points.add(new Point(3, 0));
        points.add(new Point(4, -1));
        points.add(new Point(5, -1.5));
        points.add(new Point(6, 0));

        List<Point> points2 = new ArrayList<>();
        points2.add(points.get(0));
        points2.add(points.get(3));
        points2.add(points.get(6));
//        points2.add(points.get(5));

        double area = total_areal_displacement(points, points2, true);
        System.out.println("Total area: " + area);

//        points = new ArrayList<>();
//        points.add(new Point(0, 0));
//        points.add(new Point(1, 2));
//        points.add(new Point(1.5, -10));
//        double area = calculatePolygonArea(points);
//        System.out.println(area);
//
//        Point[] L1 = new Point[2];
//        L1[0] = new Point(1, 2);
//        L1[1] = new Point(2, -2);
//        Point[] L2 = new Point[2];
//        L2[0] = new Point(0, 0);
//        L2[1] = new Point(3, 0);
//        Object[] res = lineIntersection(L1, L2);
//        System.out.println(res[1]);
    }
}
