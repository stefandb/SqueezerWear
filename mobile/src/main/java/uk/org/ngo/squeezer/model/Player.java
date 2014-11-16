/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.model;

import android.os.Parcel;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;


public class Player extends Item {

    private String mName;

    private final String mIp;

    private final String mModel;

    private final boolean mCanPowerOff;

    /** Is the player connected? */
    private boolean mConnected;

    public Player(Map<String, String> record) {
        setId(record.get("playerid"));
        mIp = record.get("ip");
        mName = record.get("name");
        mModel = record.get("model");
        mCanPowerOff = Util.parseDecimalIntOrZero(record.get("canpoweroff")) == 1;
        mConnected = Util.parseDecimalIntOrZero(record.get("connected")) == 1;
    }

    private Player(Parcel source) {
        setId(source.readString());
        mIp = source.readString();
        mName = source.readString();
        mModel = source.readString();
        mCanPowerOff = (source.readByte() == 1);
        mConnected = (source.readByte() == 1);
    }

    @Override
    public String getName() {
        return mName;
    }

    public Player setName(String name) {
        this.mName = name;
        return this;
    }

    public String getIp() {
        return mIp;
    }

    public String getModel() {
        return mModel;
    }

    public boolean isCanpoweroff() {
        return mCanPowerOff;
    }

    public void setConnected(boolean connected) {
        mConnected = connected;
    }

    public boolean getConnected() {
        return mConnected;
    }

    public static final Creator<Player> CREATOR = new Creator<Player>() {
        public Player[] newArray(int size) {
            return new Player[size];
        }

        public Player createFromParcel(Parcel source) {
            return new Player(source);
        }
    };

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mIp);
        dest.writeString(mName);
        dest.writeString(mModel);
        dest.writeByte(mCanPowerOff ? (byte) 1 : (byte) 0);
        dest.writeByte(mConnected ? (byte) 1 : (byte) 0);
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", name=" + mName + ", model=" + mModel + ", canpoweroff="
                + mCanPowerOff + ", ip=" + mIp + ", connected=" + mConnected;
    }
}
