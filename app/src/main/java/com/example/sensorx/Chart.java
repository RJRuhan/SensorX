package com.example.sensorx;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class Chart {

    private static LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    public static void addEntry(LineChart mChart, float xval, float yval) {
        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);
            data.addEntry(new Entry(xval, yval), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(4);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(xval);

        }
    }
    public static void enableTouch(LineChart mChart) {
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);
    }
    public static void init_chart(LineChart mChart, String desc) {
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText(desc);

        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(true);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend l = mChart.getLegend();

//         modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMaximum(5f);
        leftAxis.setAxisMinimum(-5f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);
    }

    public static void clearGraph(LineChart mChart) {
        LineData data = mChart.getData();

        mChart.fitScreen();
        if (data != null)
            data.clearValues();

        mChart.getXAxis().setValueFormatter(null);
        mChart.notifyDataSetChanged();
        mChart.clear();
        mChart.postInvalidate();
    }
}
