/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.drorbiteex.model.orbit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class CelestrakTleOrbitModel extends TleOrbitModel {

    private String celestrakName;

    private String group;

    public CelestrakTleOrbitModel() {
        //
    }

    public CelestrakTleOrbitModel(String group, String celestrakName, String tle) {
        super(tle);
        this.group = group;
        this.celestrakName = celestrakName;
    }

    @XmlAttribute
    public synchronized String getGroup() {
        return group;
    }

    private void setGroup(String group) {
        this.group = group;
    }

    @XmlAttribute
    public String getCelestrakName() {
        return celestrakName;
    }

    private void setCelestrakName(String celestrakName) {
        this.celestrakName = celestrakName;
    }

    @Override
    public boolean updateModel(IOrbitModel model) {
        if(model instanceof CelestrakTleOrbitModel) {
            CelestrakTleOrbitModel iModel = (CelestrakTleOrbitModel) model;
            this.group = iModel.getGroup();
            this.celestrakName = iModel.getCelestrakName();
            return super.updateModel(model);
        } else {
            throw new IllegalArgumentException("Not a class of type CelestrakTleOrbitModel");
        }
    }

    @Override
    public IOrbitModel copy() {
        return new CelestrakTleOrbitModel(getGroup(), getCelestrakName(), getTle());
    }
}
