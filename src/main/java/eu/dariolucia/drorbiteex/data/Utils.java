package eu.dariolucia.drorbiteex.data;

import eu.dariolucia.drorbiteex.fxml.Main;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Utils {

    public static final int EARTH_RADIUS = 200;
    public static final int REAL_EARTH_RADIUS_METERS = 6371000;
    public static final double EARTH_SCALE_FACTOR = (double) EARTH_RADIUS / (double) REAL_EARTH_RADIUS_METERS;

    public static AbsoluteDate toAbsoluteDate(Date time) {
        return new AbsoluteDate(time, TimeScalesFactory.getUTC());
    }

    public static Cylinder createConnection(Point3D origin, Point3D target, Color color) {
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D diff = target.subtract(origin);
        double height = diff.magnitude();

        Point3D mid = target.midpoint(origin);
        Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

        Point3D axisOfRotation = diff.crossProduct(yAxis);
        double angle = Math.acos(diff.normalize().dotProduct(yAxis));
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

        Cylinder line = new Cylinder(1, height, 8);
        line.setMaterial(new PhongMaterial(color));
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);

        return line;
    }

    public static Group createLine(List<Point3D> point3DList, Color color) {
        Group toReturn = new Group();
        for(int i = 0; i < point3DList.size() - 1; ++i) {
            toReturn.getChildren().add(createConnection(point3DList.get(i), point3DList.get(i + 1), color));
        }
        return toReturn;
    }

    public static Group createEarthSphere() {
        // Use triangular mesh
        int latLevels = 90;
        int lonLevels = 180;

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        double radius = EARTH_RADIUS;

        double latIncAngle = (Math.PI/latLevels);
        double lonIncAngle = (Math.PI * 2)/lonLevels;
        double textLatIncr = 1.0/latLevels;
        double textLonIncr = 1.0/lonLevels;

        int currentPointOffset = 0;
        int currentNormalOffset = 0;
        int currentTextOffset = 0;
        for(int i = 0; i < latLevels; ++i) {
            for(int j = 0; j < lonLevels; ++j) {
                // The point list is: top left - bottom left - bottom right - top right
                // The faces-normal points are: (0,0) (1,1) (2,2) (0,3) (2,4) (3,5)
                Point3D tp1 = new Point3D(0,radius * Math.cos(Math.PI - (i * latIncAngle)), radius * Math.sin(Math.PI - (i * latIncAngle)));
                Point3D tp2 = new Point3D(0,radius * Math.cos(Math.PI - (i * latIncAngle + latIncAngle)), radius * Math.sin(Math.PI - (i * latIncAngle + latIncAngle)));
                Point3D topLeft = new Rotate(Math.toDegrees(j * lonIncAngle), new Point3D(0, 1, 0)).transform(tp1);
                Point3D bottomLeft =  new Rotate(Math.toDegrees(j * lonIncAngle), new Point3D(0, 1, 0)).transform(tp2);
                Point3D bottomRight = new Rotate(Math.toDegrees(j * lonIncAngle + lonIncAngle), new Point3D(0, 1, 0)).transform(tp2);
                Point3D topRight = new Rotate(Math.toDegrees(j * lonIncAngle + lonIncAngle), new Point3D(0, 1, 0)).transform(tp1);

                // Compute normals
                Point3D topLeftNormal_1 = computeNormal(topLeft, bottomLeft, bottomRight); // 0
                Point3D bottomLeftNormal_1 = computeNormal(bottomLeft, bottomRight, topLeft); // 1
                Point3D bottomRightNormal_1 = computeNormal(bottomRight, topLeft, bottomLeft); // 2
                Point3D topLeftNormal_2 = computeNormal(topLeft, bottomRight, topRight); // 3
                Point3D bottomRightNormal_2 = computeNormal(bottomRight, topRight, topLeft); // 4
                Point3D topRightNormal_2 = computeNormal(topRight, topLeft, bottomRight); // 5

                // Add points
                mesh.getPoints().addAll((float) topLeft.getX(), (float) topLeft.getY(), (float) topLeft.getZ()); // 0
                mesh.getPoints().addAll((float) bottomLeft.getX(), (float) bottomLeft.getY(), (float) bottomLeft.getZ()); // 1
                mesh.getPoints().addAll((float) bottomRight.getX(), (float) bottomRight.getY(), (float) bottomRight.getZ()); // 2
                mesh.getPoints().addAll((float) topRight.getX(), (float) topRight.getY(), (float) topRight.getZ()); // 3

                // Add normals
                mesh.getNormals().addAll((float) topLeftNormal_1.getX(), (float) topLeftNormal_1.getY(), (float) topLeftNormal_1.getZ()); // 0
                mesh.getNormals().addAll((float) bottomLeftNormal_1.getX(), (float) bottomLeftNormal_1.getY(), (float) bottomLeftNormal_1.getZ()); // 1
                mesh.getNormals().addAll((float) bottomRightNormal_1.getX(), (float) bottomRightNormal_1.getY(), (float) bottomRightNormal_1.getZ()); // 2
                mesh.getNormals().addAll((float) topLeftNormal_2.getX(), (float) topLeftNormal_2.getY(), (float) topLeftNormal_2.getZ()); // 3
                mesh.getNormals().addAll((float) bottomRightNormal_2.getX(), (float) bottomRightNormal_2.getY(), (float) bottomRightNormal_2.getZ()); // 4
                mesh.getNormals().addAll((float) topRightNormal_2.getX(), (float) topRightNormal_2.getY(), (float) topRightNormal_2.getZ()); // 5

                // Add texture
                float[] p0t = { (float) (i * textLatIncr), 1.0f - (float) (j * textLonIncr) };
                float[] p1t = { (float) (i * textLatIncr + textLatIncr), 1.0f - (float) (j * textLonIncr) };
                float[] p2t = { (float) (i * textLatIncr + textLatIncr), 1.0f - (float) (j * textLonIncr + textLonIncr) };
                float[] p3t = { (float) (i * textLatIncr), 1.0f - (float) (j * textLonIncr + textLonIncr) };

                mesh.getTexCoords().addAll(
                        p0t[1], p0t[0],
                        p1t[1], p1t[0],
                        p2t[1], p2t[0],
                        p3t[1], p3t[0]
                );

                // Add faces
                mesh.getFaces().addAll(
                        currentPointOffset + 0, currentNormalOffset + 0, currentTextOffset + 0,
                        currentPointOffset + 2, currentNormalOffset + 2, currentTextOffset + 2,
                        currentPointOffset + 1, currentNormalOffset + 1, currentTextOffset + 1,
                        currentPointOffset + 0, currentNormalOffset + 3, currentTextOffset + 0,
                        currentPointOffset + 3, currentNormalOffset + 5, currentTextOffset + 3,
                        currentPointOffset + 2, currentNormalOffset + 4, currentTextOffset + 2

                );

                currentPointOffset += 4;
                currentNormalOffset += 6;
                currentTextOffset += 4;
            }
        }

        MeshView meshView = new MeshView(mesh);
        meshView.setCullFace(CullFace.BACK);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(new Image(Main.class.getResourceAsStream("/images/earth.jpg")));
        meshView.setMaterial(material);
        return new Group(meshView);
    }

    private static Point3D computeNormal(Point3D p1, Point3D p2, Point3D p3) {
        return (p3.subtract(p1).normalize()).crossProduct(p2.subtract(p1).normalize()).normalize();
    }

    // Create geoid
    public static FactoryManagedFrame ITRF = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
    private static ReferenceEllipsoid referenceEllipsoid = ReferenceEllipsoid.getWgs84(ITRF);
    private static NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getConstantNormalizedProvider(2,2);
    //The above parameters are (degree,order). Arbitrary for now.
    private static Geoid geoid = new Geoid(gravity, referenceEllipsoid);

    private static BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            ITRF);

    public static BodyShape getEarthShape() {
        return earthShape;
    }

    public static GeodeticPoint cartesianToGeodetic(Vector3D cartesianPoint, AbsoluteDate date) {
        return geoid.transform(cartesianPoint, ITRF, date);
    }
    public static GeodeticPoint cartesianToGeodetic(Vector3D cartesianPoint) {
        return geoid.transform(cartesianPoint, ITRF, new AbsoluteDate());
    }

    public static Vector3D geodeticToCartesian(GeodeticPoint geodeticPoint) {
        return geoid.transform(geodeticPoint);
    }

    public static Point3D latLonToScreenPoint(double lat, double lon, double radius) {
        double scaleRatio = radius / REAL_EARTH_RADIUS_METERS;
        Vector3D cartesian = geodeticToCartesian(new GeodeticPoint(Math.toRadians(lat), Math.toRadians(lon), 0));
        return new Point3D(cartesian.getY() * scaleRatio, - cartesian.getZ() * scaleRatio, - cartesian.getX() * scaleRatio);
    }

    public static double[] toXY(double latitude, double longitude, double width, double height) {
        // From LAT-LON to x,y:
        // x (lon): -180: 0 +180: w
        // y (lat): 90: 0 -90: h
        return new double[] { ((longitude + 180) / 360) * width,  ((90 - latitude) / 180) * height };
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public static String formatDate(Date d) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(d);
    }

    public static MeshView createVisibilityMesh(double latitude, double longitude, List<double[]> visibilityCircleSortedLatLon, double scHeight) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        float radius = Utils.EARTH_RADIUS + (float) (scHeight * EARTH_SCALE_FACTOR);
        // Point3D gsPoint = Utils.latLonToScreenPoint(latitude, longitude, radius);
        Point3D gsPoint = Utils.latLonToScreenPoint(latitude, longitude, 0);
        mesh.getPoints().addAll((float) gsPoint.getX(), (float) gsPoint.getY(), (float) gsPoint.getZ()); // Center (0)
        // The normal is the normalisation of the point
        Point3D gsNormal = gsPoint.normalize();
        mesh.getNormals().addAll((float) gsNormal.getX(), (float) gsNormal.getY(), (float) gsNormal.getZ()); // Center (0)
        mesh.getTexCoords().addAll(0,0);
        for(int i = 0; i < visibilityCircleSortedLatLon.size() - 1; ++i) {
            double[] latLon1 = visibilityCircleSortedLatLon.get(i);
            double[] latLon2 = visibilityCircleSortedLatLon.get(i + 1);
            // Make a triangle
            Point3D p1 = Utils.latLonToScreenPoint(latLon1[0], latLon1[1], radius);
            Point3D p2 = Utils.latLonToScreenPoint(latLon2[0], latLon2[1], radius);
            mesh.getPoints().addAll((float) p1.getX(), (float) p1.getY(), (float) p1.getZ()); // P1 (i * 0 + 1)
            mesh.getPoints().addAll((float) p2.getX(), (float) p2.getY(), (float) p2.getZ()); // P2 (i * 0 + 2)
            Point3D pn1 = computeNormal(p1, p2, gsPoint); // 1
            Point3D pn2 = computeNormal(p2, gsPoint, p1); // 2
            mesh.getNormals().addAll((float) pn1.getX(), (float) pn1.getY(), (float) pn1.getZ()); // P1
            mesh.getNormals().addAll((float) pn2.getX(), (float) pn2.getY(), (float) pn2.getZ()); // P2

            mesh.getFaces().addAll(0, 0, 0,
                    (i * 2 + 1), (i * 2 + 1), 0,
                    (i * 2 + 2), (i * 2 + 2), 0);
        }
        // Last face to close the base
        mesh.getFaces().addAll(0, 0, 0,
                mesh.getPoints().size()/3 - 1, mesh.getNormals().size()/3 - 1, 0,
                1, 1, 0);

        MeshView meshView = new MeshView(mesh);
        meshView.setCullFace(CullFace.NONE);
        return meshView;
    }

    public static Point2D adjustAzimuthElevationPoint(Point2D azElPoint) {

        return azElPoint;
    }
}
