package eu.dariolucia.drorbiteex.model.tle;

import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleOrbitModel;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CelestrakTleRefresher {

    private final static Logger LOG = Logger.getLogger(CelestrakTleRefresher.class.getName());
    public boolean refresh(List<Orbit> toRefresh, ITaskProgressMonitor monitor) {
        if(monitor == null) {
            monitor = ITaskProgressMonitor.DUMMY_MONITOR;
        }
        int progress = 0;
        monitor.progress(progress, toRefresh.size(), "");

        for(Orbit orbit : toRefresh) {
            try {
                final CelestrakTleOrbitModel theOrbit = (CelestrakTleOrbitModel) orbit.getModel();
                String newTle = CelestrakTleData.retrieveUpdatedTle(theOrbit.getGroup(), theOrbit.getCelestrakName());
                if(monitor.isCancelled()) {
                    break;
                }
                if (newTle != null) {
                    CelestrakTleOrbitModel model = new CelestrakTleOrbitModel(theOrbit.getGroup(), theOrbit.getCelestrakName(), newTle);
                    orbit.update(new Orbit(orbit.getId(), orbit.getCode(), orbit.getName(), orbit.getColor(), orbit.isVisible(), model));
                    monitor.progress(++progress, toRefresh.size(), orbit.getName());
                    LOG.log(Level.INFO, "Orbit " + orbit.getName() + " TLE model updated from Celestrak");
                } else {
                    LOG.log(Level.WARNING, "Orbit " + orbit.getName() + " TLE model not updated: no TLE could be retrieved from Celestrak");
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Orbit " + orbit.getName() + " TLE model not updated: error during Celestrak update: " + e.getMessage(), e);
            }
        }
        return true;
    }
}
