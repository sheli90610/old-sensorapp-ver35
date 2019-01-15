/*
 * Copyright 2013-2017 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.youruserpools;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.simran.powermanagement.R;

public class UserAttributesAdapter extends BaseAdapter {
    private Context context;
    private int count;
    private static LayoutInflater layoutInflater;

    public UserAttributesAdapter(Context context) {
        this.context = context;

        count = AppHelper.getItemCount();

        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        if(convertView == null) {
            convertView = layoutInflater.inflate(R.layout.fields_generic, null);
            holder = new Holder();
            holder.label = convertView.findViewById(R.id.textViewUserDetailLabel);
            holder.data = convertView.findViewById(R.id.editTextUserDetailInput);
            holder.message = convertView.findViewById(R.id.textViewUserDetailMessage);

            convertView.setTag(holder);
        }
        else {
            holder = (Holder) convertView.getTag();
        }

        ItemToDisplay item = AppHelper.getItemForDisplay(position);
        holder.label.setText(item.getLabelText());
       // holder.label.setTextColor(item.getLabelColor());
        holder.label.setTextColor(Color.WHITE);
        holder.data.setHint(item.getLabelText());
        holder.data.setText(item.getDataText());
        holder.data.setTextColor(Color.WHITE);

        int resID = 0;
        if(item.getDataDrawable() != null) {
            if(item.getDataDrawable().equals("checked")) {
                resID = R.drawable.verified;
            }
            else if(item.getDataDrawable().equals("not_checked")) {
                resID = R.drawable.not_verified;
            }
        }
        holder.data.setCompoundDrawablesWithIntrinsicBounds(0, 0, resID, 0);
        holder.message.setText(item.getMessageText());
        holder.message.setTextColor(Color.WHITE);

        return convertView;
    }

    // Helper class to recycle View's
    static class Holder {
        TextView label;
        TextView data;
        TextView message;
    }
}
