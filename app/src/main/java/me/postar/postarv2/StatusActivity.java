package me.postar.postarv2;

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

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

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

                for (PostParcel tempParcel : parcels)
                {
                    if (parcel.getParcelNo().equalsIgnoreCase(tempParcel.getParcelNo()))
                    {
                        tempParcel.setAlarmOn(isChecked);
                        break;
                    }
                }

                Functions.storeParcels(parcels, StatusActivity.this);
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        loadStaus();
    }

    private void loadStaus()
    {
        Ion.with(StatusActivity.this)
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

                        Ion.with(StatusActivity.this)
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

                                        if (table != null)
                                        {
                                            Elements rows = table.select("tr");

                                            for (int i = 1; i<rows.size(); i++)
                                            {
                                                messages.add(new StatusMessage(rows.get(i).select("td").get(1).text(),
                                                        rows.get(i).select("td").get(2).text(),
                                                        rows.get(i).select("td").get(3).text(),
                                                        rows.get(i).select("td").get(4).text(),
                                                        rows.get(i).select("td").get(5).text()
                                                ));
                                            }
                                            adapter.notifyDataSetChanged();
                                            loading_ProgressBar.setVisibility(View.GONE);
                                        }
                                        else
                                        {
                                            noMessage_TextView.setVisibility(View.VISIBLE);
                                            loading_ProgressBar.setVisibility(View.GONE);
                                        }


                                    }
                                });
                    }
                });
    }

    private class ListAdapter extends RecyclerView.Adapter<ListRowHolder>
    {

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

    private class ListRowHolder extends RecyclerView.ViewHolder
    {
        final TextView desc_TextView;
        final TextView location_TextView;
        final TextView date_TextView;

        public ListRowHolder(View view)
        {
            super(view);

            this.desc_TextView = (TextView)view.findViewById(R.id.row_status_message_tv_desc);
            this.location_TextView = (TextView)view.findViewById(R.id.row_status_message_tv_location);
            this.date_TextView = (TextView)view.findViewById(R.id.row_status_message_tv_date);
        }
    }
}
