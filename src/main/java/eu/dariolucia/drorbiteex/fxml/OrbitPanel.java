package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class OrbitPanel implements Initializable {
    public Label semiMajorAxisLabel;
    public Label eccentricityLabel;
    public Label inclinationLabel;
    public Label muLabel;
    public Label orbitTypeLabel;
    public Label refFrameLabel;
    public Label periodLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //
    }

    public void update(Orbit o) {
        SpacecraftPosition sp = o.getCurrentSpacecraftPosition();
        if(sp != null) {
            semiMajorAxisLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getA()));
            eccentricityLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getE()));
            inclinationLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getI()));
            muLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getMu()));
            orbitTypeLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getType()));
            refFrameLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getFrame()));
            periodLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getKeplerianPeriod()));
        }
    }

    public void clear() {
        semiMajorAxisLabel.setText("---");
        eccentricityLabel.setText("---");
        inclinationLabel.setText("---");
        muLabel.setText("---");
        orbitTypeLabel.setText("---");
        refFrameLabel.setText("---");
        periodLabel.setText("---");
    }
}
