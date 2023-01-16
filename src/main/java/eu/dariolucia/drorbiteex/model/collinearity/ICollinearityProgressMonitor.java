package eu.dariolucia.drorbiteex.model.collinearity;

public interface ICollinearityProgressMonitor {

    void progress(long current, long total, String message);
}
