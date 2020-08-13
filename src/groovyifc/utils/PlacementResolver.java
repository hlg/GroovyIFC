package groovyifc.utils;

import org.bimserver.models.ifc4.*;
import java.util.List;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class PlacementResolver {

    static Map<IfcObjectPlacement, Transformation> placementCache = new HashMap<IfcObjectPlacement, Transformation>();

    public  static Transformation resolvePlacement(IfcObjectPlacement placement)  {
        if(placement==null) return getIdentityTransform();
        if(placementCache.containsKey(placement)) return placementCache.get(placement);
        if(placement instanceof IfcLocalPlacement) { // TODO: other case IfcGridPlacement, use method overloading
            IfcLocalPlacement localPlacement = (IfcLocalPlacement) placement;
            if(localPlacement.getRelativePlacement() instanceof IfcAxis2Placement3D){ // TODO other case IfcAxisPlacement2D
                IfcAxis2Placement3D axis2placement3d = (IfcAxis2Placement3D) localPlacement.getRelativePlacement();
                Transformation placementTransformation = getTransform(axis2placement3d);
                if (localPlacement.getPlacementRelTo()!=null) {
                    Transformation relatedTransformation = resolvePlacement(localPlacement.getPlacementRelTo());
                    placementTransformation.rotation.mul(relatedTransformation.rotation);
                    placementTransformation.translation.set(getAbsolutePoint(relatedTransformation, new Point3d(placementTransformation.translation)));
                }
                placementCache.put(placement, placementTransformation);
                return placementTransformation;
            }
            throw new UnsupportedOperationException("found "+localPlacement.getRelativePlacement().getClass().getSimpleName()+" instead of IfcAxis2Plaement3D");
        }
        throw new UnsupportedOperationException("found "+placement.getClass().getSimpleName()+" instead of IfcLocalPlacement");
    }

    public static Transformation getTransform(IfcAxis2Placement2D placement){
        Vector3d location = vectorFor(placement.getLocation().getCoordinates());
        Vector3d zAxis = new Vector3d(0.0D, 0.0D, 1.0D);
        Vector3d xAxis = placement.getRefDirection()!=null ? vectorFor(placement.getRefDirection().getDirectionRatios()) : new Vector3d(1.0D, 0.0D, 0.0D);
        return getTransformation(location, xAxis,zAxis);
    }

    public static Transformation getTransform(IfcAxis2Placement3D placement) {
        if(placement==null) return getIdentityTransform();
        Vector3d location = placement.getLocation()!=null ? vectorFor(placement.getLocation().getCoordinates()) : new Vector3d(0.0D, 0.0D, 0.0D); // should never be null
        Vector3d refDirection = placement.getRefDirection()!=null ? vectorFor(placement.getRefDirection().getDirectionRatios()): new Vector3d(1.0D, 0.0D, 0.0D);
        Vector3d zAxis = placement.getAxis()!= null ? vectorFor(placement.getAxis().getDirectionRatios()) : new Vector3d(0.0D, 0.0D, 1.0D);
        Vector3d xAxis = retrieveXAxis(zAxis, refDirection);
        return getTransformation(location, xAxis, zAxis);
    }

    public static Transformation getIdentityTransform(){
        Vector3d vector3d = new Vector3d(0.,0.,0.);
        Matrix3d matrix3d = new Matrix3d();
        matrix3d.setIdentity();
        return new Transformation(vector3d, matrix3d);
    }
    private static Transformation getTransformation(Vector3d location, Vector3d xAxis, Vector3d zAxis) {
        Vector3d xNorm = new Vector3d();
        xNorm.normalize(xAxis);
        Vector3d yNorm = new Vector3d();
        yNorm.cross(zAxis, xAxis);
        yNorm.normalize();
        Vector3d zNorm = new Vector3d();
        zNorm.normalize(zAxis);
        return new Transformation(location, new Matrix3d(xNorm.getX(), xNorm.getY(), xNorm.getZ(), yNorm.getX(), yNorm.getY(), yNorm.getZ(), zNorm.getX(), zNorm.getY(), zNorm.getZ()));
    }

    public static Point3d getAbsolutePoint(PlacementResolver.Transformation placement, IfcCartesianPoint point) {
        // point.getDim() is a derived property, not working in BimServer
        return getAbsolutePoint(placement, point.getCoordinates().get(0), point.getCoordinates().get(1), point.getCoordinates().size() == 2 ? 0 : point.getCoordinates().get(2));
    }

    public static Point3d getAbsolutePoint(PlacementResolver.Transformation placement, double x, double y, double z) {
        Point3d absolutePt = new Point3d(x, y, z);
        getAbsolutePoint(placement, absolutePt);
        return absolutePt;
    }

    public static Point3d getAbsolutePoint(Transformation placement, Point3d absolutePt) {
        Matrix3d inverse = new Matrix3d(placement.rotation); // TODO: do this once for placement only?
        inverse.invert();
        inverse.transform(absolutePt); // changes absolutePt [sic]
        absolutePt.add(placement.translation); // changes absolutePt
        return absolutePt;
    }

    private static Vector3d retrieveXAxis(Vector3d zAxis, Vector3d refDirection) {
        double d = refDirection.dot(zAxis) / zAxis.lengthSquared();
        Vector3d xAxis = new Vector3d(refDirection);
        Vector3d refZ = new Vector3d(zAxis);
        refZ.scale(d);
        xAxis.sub(refZ);
        return xAxis;
    }

    private static Vector3d vectorFor(List<Double> coords) {
        return new Vector3d(coords.get(0), coords.get(1), coords.size()>2 ? coords.get(2) : 0);
    }

    public static class Transformation {
        public Vector3d translation;
        public Matrix3d rotation;

        public Transformation(Vector3d translation, Matrix3d rotation) {
            this.translation = translation;
            this.rotation = rotation;
        }
    }
}

