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

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Parent;

public class CssHolder {

    private static final SimpleStringProperty CSS;

    public static final String DRORBITEEX_CSS_DEFAULT_THEME_KEY = "drorbiteex_css_default_theme";

    static {
        CSS = new SimpleStringProperty("/eu/dariolucia/drorbiteex/fxml/css/style.css");
        // Disable the styling if the property below is detected
        if(System.getProperty(DRORBITEEX_CSS_DEFAULT_THEME_KEY) != null) {
            CSS.set(null);
        }
    }

    private CssHolder() {
        throw new IllegalCallerException("Not to be called");
    }

    public static ReadOnlyStringProperty CSSProperty() {
        return CSS;
    }

    public static String getCSSProperty() {
        return CSS.get();
    }

    public static void setCSSProperty(String newCss) {
        CSS.set(newCss);
    }

    public static void applyTo(Parent n) {
        n.getStylesheets().removeIf(o -> o.contains("drorbiteex"));
        if(getCSSProperty() != null) {
            n.getStylesheets().add(getCSSProperty());
        }
    }
}
