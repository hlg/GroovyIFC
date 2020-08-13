package groovyifc.utils;

import org.bimserver.models.ifc4.*;
import org.junit.Test;

import javax.vecmath.Point3d;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PlacementResolverTest {
    @Test
    public void getAbsolutePoint() {
        IfcAxis2Placement3D placement3D = createAxisPlacement(
                Arrays.asList(0., 0., 0.),
                Arrays.asList(0., 0., 1.),
                Arrays.asList(1., 0., 0.)
        );
        GeometryUtility.Transformation transformation = GeometryUtility.getTransform(placement3D);
        Point3d absolutePoint = GeometryUtility.getAbsolutePoint(transformation, 5, 0, 0);
        assertEquals(new Point3d(5,0,0), absolutePoint);
    }

    @Test
    public void getAbsolutePoint1() {
        IfcAxis2Placement3D placement3D = createAxisPlacement(
                Arrays.asList(0., 0., 0.),
                Arrays.asList(0., 0., 1.),
                Arrays.asList(4., 3., 0.)
        );
        GeometryUtility.Transformation transformation = GeometryUtility.getTransform(placement3D);
        Point3d absolutePoint = GeometryUtility.getAbsolutePoint(transformation, 5, 0, 0);
        assertEquals(new Point3d(4,3,0), absolutePoint);
    }

     @Test
    public void getAbsolutePoint2() {
        IfcAxis2Placement3D placement3D = createAxisPlacement(
                Arrays.asList(0., 0., 0.),
                Arrays.asList(-3., 0., 4.),
                Arrays.asList(1., 0., 0.)
        );
        GeometryUtility.Transformation transformation = GeometryUtility.getTransform(placement3D);
        Point3d absolutePoint = GeometryUtility.getAbsolutePoint(transformation, 5, 0, 0);
        assertEquals(new Point3d(4,0,3), absolutePoint);
    }

    @Test
    public void getAbsolutePoint2D() {
        IfcAxis2Placement2D placement2D = createAxisPlacement2D(Arrays.asList(300.,200.), Arrays.asList(-1.,0.));
        GeometryUtility.Transformation transformation= GeometryUtility.getTransform(placement2D);
        Point3d absolutePoint = GeometryUtility.getAbsolutePoint(transformation, 50,50,0);
        assertEquals(new Point3d(250.,150.,0.), absolutePoint);
    }

    @Test
    public void getAbsolutePointNestedPlacement(){
         IfcAxis2Placement3D placement3D = createAxisPlacement(
                Arrays.asList(200., 100., 0.),
                Arrays.asList(0., 0., 1.),
                Arrays.asList(400., 300., 0.)
        );
         GeometryUtility.Transformation transformation = GeometryUtility.getTransform(placement3D);
         Point3d absPt = GeometryUtility.getAbsolutePoint(transformation, 500,250,0);
         assertEqualsRounded(new Point3d(450.,600.,0.), absPt);
         IfcAxis2Placement3D placement3Dinner = createAxisPlacement(
                Arrays.asList(500., 250., 0.),
                Arrays.asList(0., 0., 1.),
                Arrays.asList(0., 1., 0.)
         );
        IfcLocalPlacement placement = Ifc4Factory.eINSTANCE.createIfcLocalPlacement();
        placement.setRelativePlacement(placement3D);
        IfcLocalPlacement innerPlacement = Ifc4Factory.eINSTANCE.createIfcLocalPlacement();
        innerPlacement.setRelativePlacement(placement3Dinner);
        innerPlacement.setPlacementRelTo(placement);
        GeometryUtility.Transformation resolved = GeometryUtility.resolvePlacement(innerPlacement);
        Point3d absPt2 = GeometryUtility.getAbsolutePoint(resolved, 250, 0,0);
        assertEqualsRounded(new Point3d(300,800,0), absPt2);
    }

    private void assertEqualsRounded(Point3d expectedRounded, Point3d actualToBeRounded){
        assertEquals(expectedRounded, new Point3d(Math.round(actualToBeRounded.x * 1000)/1000, Math.round(actualToBeRounded.y*1000)/1000, Math.round(actualToBeRounded.z*1000)/1000));
    }

    private IfcAxis2Placement3D createAxisPlacement(List<Double> locationCoords, List<Double> axisCoords, List<Double> refDirectionCoords) {
        IfcAxis2Placement3D placement3D = Ifc4Factory.eINSTANCE.createIfcAxis2Placement3D();
        IfcCartesianPoint location = Ifc4Factory.eINSTANCE.createIfcCartesianPoint();
        location.getCoordinates().addAll(locationCoords);
        IfcDirection axis = Ifc4Factory.eINSTANCE.createIfcDirection();
        axis.getDirectionRatios().addAll(axisCoords);
        IfcDirection refDir = Ifc4Factory.eINSTANCE.createIfcDirection();
        refDir.getDirectionRatios().addAll(refDirectionCoords);
        placement3D.setLocation(location);
        placement3D.setAxis(axis);
        placement3D.setRefDirection(refDir);
        return placement3D;
    }

    private IfcAxis2Placement2D createAxisPlacement2D(List<Double> locationCoords, List<Double> refDirectionCoords){
        IfcAxis2Placement2D placement2D = Ifc4Factory.eINSTANCE.createIfcAxis2Placement2D();
        IfcCartesianPoint location = Ifc4Factory.eINSTANCE.createIfcCartesianPoint();
        location.getCoordinates().addAll(locationCoords);
        IfcDirection refDir = Ifc4Factory.eINSTANCE.createIfcDirection();
        refDir.getDirectionRatios().addAll(refDirectionCoords);
        placement2D.setRefDirection(refDir);
        placement2D.setLocation(location);
        return placement2D;
    }

}