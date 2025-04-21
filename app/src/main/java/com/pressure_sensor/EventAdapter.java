package com.pressure_sensor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends ArrayAdapter<Event> {
    public EventAdapter(Context context, List<Event> events) {
        super(context, 0, events);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_event, parent, false);
        }
        Event event = getItem(position);
        TextView eventTextView = convertView.findViewById(R.id.eventTextView);

        // Format or set your text
        eventTextView.setText(event.description + " - " + formatTime(event.timestamp));
        return convertView;
    }

    private String formatTime(long timestamp) {
        // month/day/2‑digit‑year 24h:minutes
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
