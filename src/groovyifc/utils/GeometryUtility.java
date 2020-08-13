package groovyifc.utils;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.*;
import java.util.stream.Collectors;

public class GeometryUtility {

    public static double[] calculateExtrusionOffsetXyz(List<Double> offset, double depth) {
        double ratio = depth / Math.sqrt(Math.pow(offset.get(0), 2) + Math.pow(offset.get(1), 2) + Math.pow(offset.get(2), 2));
        double[] offsetXyz = new double[3];
        for (int xyz = 0; xyz < 3; xyz++) {
            offsetXyz[xyz] = ratio * offset.get(xyz);
        }
        return offsetXyz;
    }

    public static List<Double> createPointList(PlacementResolver.Transformation productTransformation, PlacementResolver.Transformation shapeTransformation, List<Point3d> points) {
        List<Double> ptList = new ArrayList<>(12);
        for(Point3d pt: points){
            Point3d absPt = new Point3d(pt);
            PlacementResolver.getAbsolutePoint(shapeTransformation, absPt);
            PlacementResolver.getAbsolutePoint(productTransformation, absPt);
            ptList.addAll(Arrays.asList(absPt.getX(),absPt.getY(),absPt.getZ()));
        }
        return ptList;
    }

    public static boolean isCounterClockwise(List<Point3d> pline){
        assert pline.get(0).equals(pline.get(pline.size()-1));
        assert pline.stream().allMatch( p -> p.z == 0);
        double area = 0;
        for(int i=0; i<=pline.size()-2; i++){
            area += (pline.get(i+1).x -pline.get(i).x)*(pline.get(i+1).y+pline.get(i).y);
        }
        return area < 0;
    }

    public static Set<List<Point3d>> extrude(List<Point3d> basePoints, List<Point3d> transformed) {
        Set<List<Point3d>> faces = new HashSet<>();
        if(basePoints.size()>1) {
            int lastIndex = basePoints.size() - 1;
            for (int i = 0; i < lastIndex; i++) {
                faces.add(Arrays.asList(basePoints.get(i + 1), basePoints.get(i), transformed.get(i), transformed.get(i + 1), basePoints.get(i+1)));
            }
        }
        return faces;
    }

    public static List<Point3d> translate(List<Point3d> basePoints, Vector3d extrusionOffsetXyz) {
        return basePoints.stream().map(pt -> {
            Point3d offset = new Point3d(pt);
            offset.add(extrusionOffsetXyz);
            return offset;
        }).collect(Collectors.toList());
    }

}
