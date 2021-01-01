/* Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.neamar.kiss;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

/**
 * Represents the player's progress in the game. The player's progress is how many stars
 * they got on each level.
 *
 */
public class ZenLayout implements Serializable {

    private static final String TAG = "SaveGame";

    // serialization format version
    private static final long serialVersionUID = 1L;
    private static final String SERIAL_VERSION = "1.1";
    private final String json;
    private final String jsonWidgets;
    private byte[] data = null;
    private byte[] database = null;


    public byte[] getData(){
        return data;
    }

    public ZenLayout(String serializedSettings, String serializedWidgetSettings, ByteArrayOutputStream screenShotWallPaper, ByteArrayOutputStream database) {
        this.json = serializedSettings;
        this.jsonWidgets = serializedWidgetSettings;
        this.data = screenShotWallPaper.toByteArray();
        this.database = database.toByteArray();
    }

    public String getSavedSettings(){
        return this.json;
    }

    public String getSavedWidgets(){
        return this.jsonWidgets;
    }

    public byte[] getDataBase() {
        return database;
    }
}
