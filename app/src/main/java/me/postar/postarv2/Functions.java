package me.postar.postarv2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;

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
class Functions
{
    public static void storeParcels(ArrayList<PostParcel> parcels, Context c)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("array_size", parcels.size());
        for(int i=0;i<parcels.size(); i++)
        {
            Gson gson = new Gson();
            String json = gson.toJson(parcels.get(i));
            edit.putString("array_" + i, json);
        }
        edit.commit();
    }

    public static void getParcels(ArrayList<PostParcel> parcels, Context c)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        parcels.clear();

        int size = prefs.getInt("array_size", 0);
        for(int i=0; i<size; i++)
        {
            Gson gson = new Gson();
            String json = prefs.getString("array_" + i, null);
            PostParcel parcel = gson.fromJson(json, PostParcel.class);
            parcels.add(parcel);
        }
    }

    public static boolean isConnectedToInternet(Context c) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
