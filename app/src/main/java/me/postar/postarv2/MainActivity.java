package me.postar.postarv2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout topBar_RelativeLayout;
    private ImageView add_ImageView;
    private TextView emptyMessage_TextView;
    private RecyclerView list_RecyclerView;
    private final ArrayList<PostParcel> parcels = new ArrayList<>();
    private SharedPreferences mPrefs;
    private ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topBar_RelativeLayout = (RelativeLayout) findViewById(R.id.main_relLay_topBar);
        add_ImageView = (ImageView) findViewById(R.id.main_iv_add);
        emptyMessage_TextView = (TextView) findViewById(R.id.main_tv_emptyMessage);
        list_RecyclerView = (RecyclerView) findViewById(R.id.main_rv_list);

        mPrefs = getPreferences(MODE_PRIVATE);

        if (mPrefs.getBoolean("firstStart", true))
        {
            SharedPreferences.Editor edit = mPrefs.edit();
            edit.putBoolean("firstStart", false);
            edit.apply();

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 30);

            Random rand = new Random();
            long randNumber = rand.nextInt(3600000);

            Intent alarmIntent = new Intent(MainActivity.this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, 0);

            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 86000000 + randNumber, pendingIntent);
        }

        add_ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View dialoglayout = inflater.inflate(R.layout.custom_dialog, null);

                final EditText name = (EditText)dialoglayout.findViewById(R.id.custom_dialog_et_name);
                final EditText parcelno = (EditText)dialoglayout.findViewById(R.id.custom_dialog_et_parcelNo);
                final CheckBox check = (CheckBox) dialoglayout.findViewById(R.id.custom_dialog_cb_alarm);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!name.getText().toString().isEmpty() && !parcelno.getText().toString().isEmpty())
                        {
                            PostParcel parcel = new PostParcel(name.getText().toString(), parcelno.getText().toString(), check.isChecked());

                            parcels.add(parcel);

                            Functions.storeParcels(parcels, MainActivity.this);
                            adapter.notifyDataSetChanged();
                            emptyMessage_TextView.setVisibility(View.GONE);

                            dialogInterface.dismiss();
                        }

                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.setView(dialoglayout);
                builder.show();
            }
        });

        list_RecyclerView.setLayoutManager(new LinearLayoutManager(this));


        adapter = new ListAdapter();
        list_RecyclerView.setAdapter(adapter);
    }

    private class ListAdapter extends RecyclerView.Adapter<ListRowHolder>
    {
        private View.OnClickListener myOnClickListener;
        private View.OnLongClickListener myOnLongClickListener;

        @Override
        public ListRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_parcel, parent, false);
            ListRowHolder mh = new ListRowHolder(v);
            myOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int itemPosition = list_RecyclerView.getChildAdapterPosition(v);
                    Intent i = new Intent(MainActivity.this, StatusActivity.class);
                    i.putExtra("parcel", parcels.get(itemPosition));
                    startActivity(i);
                }
            };
            myOnLongClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(getString(R.string.delete_confirmation_message));
                    builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int itemPosition = list_RecyclerView.getChildAdapterPosition(v);
                            parcels.remove(itemPosition);

                            Functions.storeParcels(parcels, MainActivity.this);
                            Functions.getParcels(parcels, MainActivity.this);

                            if (parcels.size() == 0) emptyMessage_TextView.setVisibility(View.VISIBLE);
                            else emptyMessage_TextView.setVisibility(View.GONE);

                            adapter.notifyDataSetChanged();
                            dialogInterface.dismiss();
                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.create().show();

                    return true;
                }
            };
            v.setOnClickListener(myOnClickListener);
            v.setOnLongClickListener(myOnLongClickListener);

            return mh;
        }

        @Override
        public void onBindViewHolder(ListRowHolder holder, int position) {
            holder.name_TextView.setText(parcels.get(position).getName());
            holder.parcelNo_TextView.setText(parcels.get(position).getParcelNo());
        }

        @Override
        public int getItemCount() {
            return parcels.size();
        }
    }

    private class ListRowHolder extends RecyclerView.ViewHolder
    {
        final TextView name_TextView;
        final TextView parcelNo_TextView;

        public ListRowHolder(View view)
        {
            super(view);

            this.name_TextView = (TextView)view.findViewById(R.id.row_tv_name);
            this.parcelNo_TextView = (TextView)view.findViewById(R.id.row_tv_parcelNo);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Functions.getParcels(parcels, MainActivity.this);
        if (parcels.size() == 0) emptyMessage_TextView.setVisibility(View.VISIBLE);
        else emptyMessage_TextView.setVisibility(View.GONE);

        adapter.notifyDataSetChanged();
    }
}
