package com.simran.powermanagement;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {

    private List<Event> eventList;


    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.eventName.setText(event.getEventName());
        holder.eventTime.setText(event.getEventTime());
        holder.eventId.setText(event.getEventId());

        if (event.getEventName().equals("Turn On")) {
            holder.eventImg.setImageResource(R.drawable.power_on);
        }else{
            holder.eventImg.setImageResource(R.drawable.power_off);
        }


    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView eventName, eventTime, eventId;
        public ImageView eventImg;

        public MyViewHolder(View view) {
            super(view);
            eventImg = view.findViewById(R.id.stateIcon);
            eventName = view.findViewById(R.id.eventName);
            eventTime = view.findViewById(R.id.eventTime);
            eventId = view.findViewById(R.id.eventId);
        }
    }
}