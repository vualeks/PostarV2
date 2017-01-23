package me.postar.postarv2;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class StatusActivity extends AppCompatActivity {

    private PostParcel parcel;
    private final ArrayList<StatusMessage> messages = new ArrayList<>();

    private TextView name_TextView;
    private TextView parcelNo_TextView;
    private TextView noMessage_TextView;
    private RecyclerView status_RecyclerView;
    private ProgressBar loading_ProgressBar;
    private CheckBox alarm_CheckBox;
    private ListAdapter adapter;

    private static final String METHOD_NAME = "InformacijaOPosiljci";
    private static final String NAMESPACE = "http://TrackTrace.com";
    private static final String SOAP_ACTION = "http://TrackTrace.com/InformacijaOPosiljci";
    private static final String URL = "https://e-racuni.postacg.me/TTService/Service1.asmx?WSDL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        name_TextView = (TextView) findViewById(R.id.status_tv_name);
        loading_ProgressBar = (ProgressBar) findViewById(R.id.status_pb_loading);
        alarm_CheckBox = (CheckBox) findViewById(R.id.status_cb_alarm);
        parcelNo_TextView = (TextView) findViewById(R.id.status_tv_parcelNo);
        status_RecyclerView = (RecyclerView) findViewById(R.id.status_rv_status);
        noMessage_TextView = (TextView) findViewById(R.id.status_tv_noMessages);

        parcel = getIntent().getParcelableExtra("parcel");

        alarm_CheckBox.setChecked(parcel.isAlarmOn());
        name_TextView.setText(parcel.getName());
        parcelNo_TextView.setText(parcel.getParcelNo());

        adapter = new ListAdapter();
        status_RecyclerView.setLayoutManager(new LinearLayoutManager(this));
        status_RecyclerView.setAdapter(adapter);

        alarm_CheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ArrayList<PostParcel> parcels = new ArrayList<>();
                Functions.getParcels(parcels, StatusActivity.this);

                for (PostParcel tempParcel : parcels) {
                    if (parcel.getParcelNo().equalsIgnoreCase(tempParcel.getParcelNo())) {
                        tempParcel.setAlarmOn(isChecked);
                        break;
                    }
                }

                Functions.storeParcels(parcels, StatusActivity.this);
            }
        });
    }

    protected void onResume() {
        super.onResume();

        loadStaus();
    }

    private void loadStaus() {
        new CallWebService().execute(parcel.getParcelNo());
    }

    private class ListAdapter extends RecyclerView.Adapter<ListRowHolder> {

        @Override
        public ListRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_status_message, parent, false);

            return new ListRowHolder(v);
        }

        @Override
        public void onBindViewHolder(ListRowHolder holder, int position) {
            holder.desc_TextView.setText(messages.get(position).getDescription());
            holder.location_TextView.setText(messages.get(position).getPostNo() + " " + messages.get(position).getLocation());
            holder.date_TextView.setText(messages.get(position).getDate());
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

    private class ListRowHolder extends RecyclerView.ViewHolder {
        final TextView desc_TextView;
        final TextView location_TextView;
        final TextView date_TextView;

        public ListRowHolder(View view) {
            super(view);

            this.desc_TextView = (TextView) view.findViewById(R.id.row_status_message_tv_desc);
            this.location_TextView = (TextView) view.findViewById(R.id.row_status_message_tv_location);
            this.date_TextView = (TextView) view.findViewById(R.id.row_status_message_tv_date);
        }
    }

    private class CallWebService extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPostExecute(Void s) {
            if (messages.size() == 0) noMessage_TextView.setVisibility(View.VISIBLE);
            else adapter.notifyDataSetChanged();

            loading_ProgressBar.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(String... params) {

            SoapObject soapObject = new SoapObject(NAMESPACE, METHOD_NAME);

            PropertyInfo propertyInfo = new PropertyInfo();
            propertyInfo.setName("strPrijemniBroj");
            propertyInfo.setValue(params[0]);
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

                for (int i = 0; i < anyType2.getPropertyCount(); i++) {
                    PropertyInfo tableName = anyType2.getPropertyInfo(i);
                    SoapObject anyType3 = (SoapObject) tableName.getValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    SimpleDateFormat output = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
                    Date d = sdf.parse(anyType3.getPropertyInfo(5).getValue().toString());
                    String formattedTime = output.format(d);

                    messages.add(new StatusMessage(formattedTime,
                            anyType3.getPropertyInfo(1).getValue().toString(),
                            anyType3.getPropertyInfo(2).getValue().toString(),
                            anyType3.getPropertyInfo(7).getValue().toString(),
                            anyType3.getPropertyInfo(4).getValue().toString()
                            ));
                }

            } catch (Exception e) {

            }

            return null;
        }
    }
}
