package eu.dariolucia.drorbiteex.model.util;

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
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class EarthReferenceUtils {

    public static final int REAL_EARTH_RADIUS_METERS = 6371000;

    // Create GEOID
    private static final FactoryManagedFrame ITRF = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
    private static final ReferenceEllipsoid REFERENCE_ELLIPSOID = ReferenceEllipsoid.getWgs84(ITRF);
    private static final NormalizedSphericalHarmonicsProvider GRAVITY = GravityFieldFactory.getConstantNormalizedProvider(2,2);
    //The above parameters are (degree,order). Arbitrary for now.
    private static final Geoid GEOID = new Geoid(GRAVITY, REFERENCE_ELLIPSOID);
    private static final BodyShape EARTH_SHAPE = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            ITRF);

    public static BodyShape getEarthShape() {
        return EARTH_SHAPE;
    }

    public static FactoryManagedFrame getITRF() {
        return ITRF;
    }

    public static ReferenceEllipsoid getReferenceEllipsoid() {
        return REFERENCE_ELLIPSOID;
    }

    public static NormalizedSphericalHarmonicsProvider getGravity() {
        return GRAVITY;
    }

    public static Geoid getGeoid() {
        return GEOID;
    }

    public static GeodeticPoint cartesianToGeodetic(Vector3D cartesianPoint, AbsoluteDate date) {
        return getGeoid().transform(cartesianPoint, ITRF, date);
    }

    public static GeodeticPoint cartesianToGeodetic(Vector3D cartesianPoint) {
        return getGeoid().transform(cartesianPoint, ITRF, new AbsoluteDate());
    }

    public static Vector3D geodeticToCartesian(GeodeticPoint geodeticPoint) {
        return getGeoid().transform(geodeticPoint);
    }
}
