package com.campbellyamane.podantic;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import java.util.ArrayList;


//custom autocompleteadapter to fill info from itunes search api
public class AutoCompleteAdapter extends ArrayAdapter implements Filterable {

    ArrayList<String> allPods;

    StringFilter filter;


    public AutoCompleteAdapter(Context context, int resource, ArrayList<String> pods) {
        super(context, resource, pods);

        allPods = pods;
    }

    public int getCount() {
        try{
            return allPods.size();
        }
        catch (NullPointerException e){
            return 0;
        }
    }

    public Object getItem(int position) {
        return allPods.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    private class StringFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults results = new FilterResults();

            results.values = allPods;
            results.count = allPods.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            allPods = (ArrayList<String>) results.values;
            notifyDataSetChanged();
        }
    }

    @Override
    public Filter getFilter()
    {
        return new StringFilter();
    }

    public void update(ArrayList<String> object) {
        allPods = object;
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        allPods.clear();
    }
}