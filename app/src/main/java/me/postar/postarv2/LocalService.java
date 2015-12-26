package me.postar.postarv2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
public class LocalService extends Service {
    private final ArrayList<PostParcel> parcels = new ArrayList<>();
    private PowerManager.WakeLock wl;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Functions.getParcels(parcels, this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Postar");

        if (Functions.isConnectedToInternet(LocalService.this))
        {
            Ion.with(this)
                    .load("GET", "https://e-racuni.postacg.me/PracenjePosiljaka/")
                    .asString()
                    .withResponse()
                    .setCallback(new FutureCallback<Response<String>>() {
                        @Override
                        public void onCompleted(Exception e, Response<String> result) {
                            Document html = Jsoup.parse(result.getResult());
                            Element viewState = html.getElementById("__VIEWSTATE");
                            Element eventValidation = html.getElementById("__EVENTVALIDATION");
                            Element btnPronadji = html.getElementById("btnPronadji");

                            for (final PostParcel parcel : parcels) {
                                if (parcel.isAlarmOn()) {
                                    Ion.with(LocalService.this)
                                            .load("POST", "https://e-racuni.postacg.me/PracenjePosiljaka/")
                                            .setBodyParameter("__VIEWSTATE", viewState.val())
                                            .setBodyParameter("__EVENTVALIDATION", eventValidation.val())
                                            .setBodyParameter("btnPronadji", btnPronadji.val())
                                            .setBodyParameter("txtPrijemniBroj", parcel.getParcelNo())
                                            .asString()
                                            .withResponse()
                                            .setCallback(new FutureCallback<Response<String>>() {
                                                @Override
                                                public void onCompleted(Exception e, final Response<String> result) {
                                                    Document html = Jsoup.parse(result.getResult());
                                                    Element table = html.getElementById("dgInfo");

                                                    if (table != null) {
                                                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(LocalService.this)
                                                                .setSmallIcon(R.drawable.ic_mail_outline)
                                                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_mail_outline))
                                                                ;
                                                        mBuilder.setContentTitle(getString(R.string.message_title));
                                                        mBuilder.setContentText(getString(R.string.message_content));
                                                        Intent activityIntent = new Intent(LocalService.this, StatusActivity.class);
                                                        activityIntent.putExtra("parcel", parcel);
                                                        PendingIntent resultPendingIntent =
                                                                PendingIntent.getActivity(
                                                                        LocalService.this,
                                                                        0,
                                                                        activityIntent,
                                                                        PendingIntent.FLAG_UPDATE_CURRENT
                                                                );
                                                        mBuilder.setContentIntent(resultPendingIntent);
                                                        NotificationManager mNotificationManager =
                                                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                                        mNotificationManager.notify(12, mBuilder.build());

                                                        stopSelf();

                                                        wl.release();
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    });
        }

        return START_NOT_STICKY;
    }
}
