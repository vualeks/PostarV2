package me.postar.postarv2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.ArrayList;

/**
 * This file is part of PoštarV2.
 * <p>
 * PoštarV2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * PoštarV2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with PoštarV2.  If not, see <http://www.gnu.org/licenses/>.
 **/
public class LocalService extends Service {
    private final ArrayList<PostParcel> parcels = new ArrayList<>();
    private PowerManager.WakeLock wl;
    private static final String METHOD_NAME = "InformacijaOPosiljci";
    private static final String NAMESPACE = "http://TrackTrace.com";
    private static final String SOAP_ACTION = "http://TrackTrace.com/InformacijaOPosiljci";
    private static final String URL = "https://e-racuni.postacg.me/TTService/Service1.asmx?WSDL";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Functions.getParcels(parcels, this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Postar");

        if (Functions.isConnectedToInternet(LocalService.this)) {
            for (final PostParcel parcel : parcels) {
                if (parcel.isAlarmOn()) {
                    new CallWebService().execute(parcel);
                }
            }
            stopSelf();
            try {
                wl.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }

    private class CallWebService extends AsyncTask<PostParcel, Void, Integer> {
        PostParcel parcel;

        @Override
        protected void onPostExecute(Integer count) {
            if (count != 0) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(LocalService.this)
                        .setSmallIcon(R.drawable.ic_mail_outline)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_mail_outline))
                        .setAutoCancel(true);
                mBuilder.setContentTitle(getString(R.string.message_title) + " " + parcel.getName());
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
            }
        }

        @Override
        protected Integer doInBackground(PostParcel... params) {

            parcel = params[0];

            SoapObject soapObject = new SoapObject(NAMESPACE, METHOD_NAME);

            PropertyInfo propertyInfo = new PropertyInfo();
            propertyInfo.setName("strPrijemniBroj");
            propertyInfo.setValue(params[0].getParcelNo());
            propertyInfo.setType(String.class);

            soapObject.addProperty(propertyInfo);

            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            envelope.setOutputSoapObject(soapObject);
            envelope.dotNet = true;

            HttpTransportSE httpTransportSE = new HttpTransportSE(URL);

            try {
                httpTransportSE.call(SOAP_ACTION, envelope);
                SoapObject res = (SoapObject) envelope.getResponse();

                PropertyInfo diffGram = res.getPropertyInfo(1);
                SoapObject anyType = (SoapObject) diffGram.getValue();
                PropertyInfo documentElement = anyType.getPropertyInfo(0);
                SoapObject anyType2 = (SoapObject) documentElement.getValue();

                return anyType2.getPropertyCount();

            } catch (Exception e) {

            }

            return 0;
        }
    }
}
