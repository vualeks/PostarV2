package me.postar.postarv2;

import android.os.Parcel;
import android.os.Parcelable;

/**
 This file is part of PoštarV2.

 PoštarV2 is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 2 of the License, or
 (at your option) any later version.

 PoštarV2 is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with PoštarV2.  If not, see <http://www.gnu.org/licenses/>.
 **/
public class PostParcel implements Parcelable {
    private String name;
    private String parcelNo;
    private boolean isAlarmOn;

    public PostParcel(String name, String parcelNo, boolean isAlarmOn) {
        this.name = name;
        this.parcelNo = parcelNo;
        this.isAlarmOn = isAlarmOn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParcelNo() {
        return parcelNo;
    }

    public void setParcelNo(String parcelNo) {
        this.parcelNo = parcelNo;
    }

    public boolean isAlarmOn() {
        return isAlarmOn;
    }

    public void setAlarmOn(boolean alarmOn) {
        isAlarmOn = alarmOn;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.parcelNo);
        dest.writeByte(isAlarmOn ? (byte) 1 : (byte) 0);
    }

    PostParcel(Parcel in) {
        this.name = in.readString();
        this.parcelNo = in.readString();
        this.isAlarmOn = in.readByte() != 0;
    }

    public static final Creator<PostParcel> CREATOR = new Creator<PostParcel>() {
        public PostParcel createFromParcel(Parcel source) {
            return new PostParcel(source);
        }

        public PostParcel[] newArray(int size) {
            return new PostParcel[size];
        }
    };
}
