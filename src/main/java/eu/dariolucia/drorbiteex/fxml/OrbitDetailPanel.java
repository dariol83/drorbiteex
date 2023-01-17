/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.orekit.bodies.GeodeticPoint;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class OrbitDetailPanel implements Initializable {
    public static final String SECOND_FORMAT = "%.2f s";
    public Label semiMajorAxisLabel;
    public Label eccentricityLabel;
    public Label inclinationLabel;
    public Label muLabel;
    public Label orbitTypeLabel;
    public Label refFrameLabel;
    public Label periodLabel;
    public Label positionLabel;

    private Orbit currentOrbit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //
    }

    public void update(Orbit o) {
        this.currentOrbit = o;
        SpacecraftPosition sp = o.getCurrentSpacecraftPosition();
        if(sp != null) {
            semiMajorAxisLabel.setText(String.format("%.3f km", sp.getSpacecraftState().getOrbit().getA() / 1000.0));
            eccentricityLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getE()));
            inclinationLabel.setText(String.format("%.4f°", Math.toDegrees(sp.getSpacecraftState().getOrbit().getI())));
            muLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getMu()));
            orbitTypeLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getType()));
            refFrameLabel.setText(String.valueOf(sp.getSpacecraftState().getOrbit().getFrame()));
            periodLabel.setText(toPeriodString(sp.getSpacecraftState().getOrbit().getKeplerianPeriod()));
            GeodeticPoint position = sp.getLatLonHeight();
            positionLabel.setText(String.format("%.4f,%.4f,%d", Math.toDegrees(position.getLatitude()), Math.toDegrees(position.getLongitude()), (int) position.getAltitude()));
        }
    }

    public void updatePosition(Orbit o, SpacecraftPosition sp) {
        if(Objects.equals(this.currentOrbit, o) && sp != null) {
            GeodeticPoint position = sp.getLatLonHeight();
            positionLabel.setText(String.format("%.4f,%.4f,%d", Math.toDegrees(position.getLatitude()), Math.toDegrees(position.getLongitude()), (int) position.getAltitude()));
        }
    }

    private String toPeriodString(double keplerianPeriod) {
        int hours = 0;
        int minutes = 0;
        // Compute hours
        if(keplerianPeriod > 3600) {
            // Count the hours
            hours = (int) Math.floor(keplerianPeriod / 3600.0);
            // Update keplerianPeriod
            keplerianPeriod -= hours * 3600.0;
        }
        // Compute minutes
        if(keplerianPeriod > 60) {
            // Count the minutes
            minutes = (int) Math.floor(keplerianPeriod / 60.0);
            // Update keplerianPeriod
            keplerianPeriod -= minutes * 60.0;
        }
        // The seconds are there
        if(hours > 0) {
            return hours + " h " + minutes + " m " + String.format(SECOND_FORMAT,keplerianPeriod);
        } else if(minutes > 0) {
            return minutes + " m " + String.format(SECOND_FORMAT,keplerianPeriod);
        } else {
            return String.format(SECOND_FORMAT,keplerianPeriod);
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
        positionLabel.setText("---");
    }
}
