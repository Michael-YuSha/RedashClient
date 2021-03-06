package com.bambina.dashboardViewer.widget;

import com.bambina.dashboardViewer.AxisHelper;
import com.bambina.dashboardViewer.ConvertDateFromString;
import com.bambina.dashboardViewer.model.Dashboard;

import com.bambina.dashboardViewer.activity.MainActivity;
import com.bambina.dashboardViewer.item.LineChartWidgetItem;
import com.bambina.dashboardViewer.model.Widget;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by hirono-mayuko on 2017/04/24.
 */

public class LineChartWidget extends Widget {
    public LineData mLineData;
    public Long maxTime = 0L;
    public Long minTime = 0L;
    public boolean isJsonException = false;
    public boolean isDateTime = true;
    private HashMap<String, String> mVisualData;
    private MainActivity mainActivity;
    private LineChartWidgetItem mItem;
    private HashMap<String, List<HashMap<String, Object>>> mData = new HashMap<>();
    private static final String X_MILLI_SEC = "xmsec";
    private static final String LABEL = "label";
    private static final String NORMALIZED_X = "normalizedX";
    private static final String Y = "y";

    public LineChartWidget(HashMap<String, String> visualData, MainActivity activity, LineChartWidgetItem item){
        mVisualData = visualData;
        mainActivity = activity;
        mItem = item;
        mainActivity.queryData(mVisualData.get(Dashboard.QUERY_ID), this);
    }

    public void setData(JSONArray dataArray){
        // Get axis information.
        String xAxis = mVisualData.get(Dashboard.X_AXIS);
        String yAxis = mVisualData.get(Dashboard.Y_AXIS);
        String isMultipleYAxis = mVisualData.get(Dashboard.IS_MULTIPLE_Y_AXIS);
        if(isMultipleYAxis.equals("true")){
            // Determine y axis from candidates.
            try {
                yAxis = AxisHelper.determineAxis(dataArray.getJSONObject(0), yAxis);
            } catch (JSONException e){
                e.printStackTrace();
                isJsonException = true;
            }
        }

        String series = mVisualData.get(Dashboard.SERIES);

        // TODO: In this function xAxisType is supposed to be "datetime".
        String xAxisType = mVisualData.get(Dashboard.X_AXIS_TYPE);
        if(xAxisType.equals("datetime")) {
            Locale l = Locale.getDefault();
            for (int i = 0; i < dataArray.length(); i++) {
                try {
                    JSONObject obj = dataArray.getJSONObject(i);
                    String x = obj.getString(xAxis);
                    String y = obj.getString(yAxis);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", l);
                    Long xMilliSec = ConvertDateFromString.parse(x, format).getTime();

                    // Create a HashMap of an entry.
                    HashMap<String, Object> entry = new HashMap<>(3);
                    entry.put(LABEL, x);
                    entry.put(X_MILLI_SEC, xMilliSec);
                    entry.put(Y, Float.parseFloat(y));

                    String groupName;
                    if (series.equals("")) {
                        groupName = Dashboard.SERIES;
                    } else {
                        groupName = obj.getString(series);
                    }

                    if (mData.containsKey(groupName)) {
                        mData.get(groupName).add(entry);
                    } else {
                        List<HashMap<String, Object>> list = new ArrayList<>();
                        list.add(entry);
                        mData.put(groupName, list);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    isJsonException = true;
                    break;
                }
            }

            // Sort entries and get maxTime and minTime.
            for (String key : mData.keySet()) {
                List<HashMap<String, Object>> entries = mData.get(key);
                Collections.sort(entries, new Comparator<HashMap<String, Object>>() {
                    @Override
                    public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
                        Long l1 = (Long) o1.get(X_MILLI_SEC);
                        Long l2 = (Long) o2.get(X_MILLI_SEC);
                        return l1.compareTo(l2);
                    }
                });
                Long firstTime = (Long) entries.get(0).get(X_MILLI_SEC);
                Long lastTime = (Long) entries.get(entries.size() - 1).get(X_MILLI_SEC);

                if (minTime == 0 || minTime > firstTime) {
                    minTime = firstTime;
                }

                if (maxTime == 0 || maxTime < lastTime) {
                    maxTime = lastTime;
                }
            }

            // Normalize x value(DateTime). Like (xmsec - min) / (max - min).
            for (String key : mData.keySet()) {
                List<HashMap<String, Object>> entries = mData.get(key);
                for (HashMap<String, Object> entry : entries) {
                    float normalizedX;
                    if(minTime.equals(maxTime)){
                        normalizedX = 0.5f;
                    } else {
                        Long xmsec = (Long) entry.get(X_MILLI_SEC);
                        normalizedX = (float) (xmsec - minTime) / (maxTime - minTime);
                    }
                    entry.put(NORMALIZED_X, normalizedX);
                }
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            int index = 0;
            for (String groupName : mData.keySet()) {
                List<HashMap<String, Object>> group = mData.get(groupName);
                ArrayList<Entry> list = new ArrayList<>();
                for (HashMap<String, Object> entry : group) {
                    list.add(new Entry((Float) entry.get(NORMALIZED_X), (Float) entry.get(Y)));
                }
                LineDataSet a = new LineDataSet(list, groupName);
                int[] color = mainActivity.getChartColor(index);
                a.setColors(color, mainActivity.getContext());
                a.setDrawCircles(false);
                dataSets.add(a);
                index++;
            }
            mLineData = new LineData(dataSets);
        } else {
            mLineData = new LineData();
            isDateTime = false;
        }

        mItem.notifyWidgetChanged();
    }

    public void callback(JSONArray dataArray){
        this.setData(dataArray);
    }

    public String getQueryName(){
        return mVisualData.get(Dashboard.QUERY_NAME);
    }
}
