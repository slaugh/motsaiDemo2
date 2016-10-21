package com.mygdx.game.android.ControlPanel;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.mygdx.game.android.NeblinaClasses.NebDeviceDetailFragment;
import com.mygdx.game.android.R;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class DynamicData extends Activity {

//    @InjectView(R.id.future_button)
//    Button futureButton;

    private static final int HISTORY_SIZE = 30;            // number of points to plot in history
    private SensorManager sensorMgr = null;
    private Sensor orSensor = null;

    private XYPlot aprLevelsPlot = null;
    private XYPlot aprHistoryPlot = null;

    private CheckBox hwAcceleratedCb;
    private CheckBox showFpsCb;
    private SimpleXYSeries aprLevelsSeries = null;
    private SimpleXYSeries azimuthHistorySeries = null;
//    private SimpleXYSeries pitchHistorySeries = null;
//    private SimpleXYSeries rollHistorySeries = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_data);
//        ButterKnife.inject(this);

//        NebDeviceDetailFragment.dynamicData = this;

        aprLevelsSeries = new SimpleXYSeries("APR Levels");
        aprLevelsSeries.useImplicitXVals();
        aprLevelsPlot.addSeries(aprLevelsSeries,
                new BarFormatter(Color.argb(100, 0, 200, 0), Color.rgb(0, 80, 0)));
        aprLevelsPlot.setDomainStepValue(3);
        aprLevelsPlot.setTicksPerRangeLabel(3);

        // per the android documentation, the minimum and maximum readings we can get from
        // any of the orientation sensors is -180 and 359 respectively so we will fix our plot's
        // boundaries to those values.  If we did not do this, the plot would auto-range which
        // can be visually confusing in the case of dynamic plots.
        aprLevelsPlot.setRangeBoundaries(-180, 359, BoundaryMode.FIXED);

        // use our custom domain value formatter:
        aprLevelsPlot.setDomainValueFormat(new APRIndexFormat());

        // update our domain and range axis labels:
        aprLevelsPlot.setDomainLabel("Axis");
        aprLevelsPlot.getDomainLabelWidget().pack();
        aprLevelsPlot.setRangeLabel("Angle (Degs)");
        aprLevelsPlot.getRangeLabelWidget().pack();
        aprLevelsPlot.setGridPadding(15, 0, 15, 0);

        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);

        azimuthHistorySeries = new SimpleXYSeries("Azimuth");
        azimuthHistorySeries.useImplicitXVals();
//        pitchHistorySeries = new SimpleXYSeries("Pitch");
//        pitchHistorySeries.useImplicitXVals();
//        rollHistorySeries = new SimpleXYSeries("Roll");
//        rollHistorySeries.useImplicitXVals();

        aprHistoryPlot.setRangeBoundaries(-180, 359, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainBoundaries(0, 30, BoundaryMode.FIXED);
        aprHistoryPlot.addSeries(azimuthHistorySeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), Color.BLACK, 0, null));//TODO: Fix Added random zeros???
//        aprHistoryPlot.addSeries(pitchHistorySeries, new LineAndPointFormatter(Color.rgb(100, 200, 100), Color.BLACK,0, null));
//        aprHistoryPlot.addSeries(rollHistorySeries, new LineAndPointFormatter(Color.rgb(200, 100, 100), Color.BLACK,0, null));
        aprHistoryPlot.setDomainStepValue(5);
        aprHistoryPlot.setTicksPerRangeLabel(3);
        aprHistoryPlot.setDomainLabel("Sample Index");
        aprHistoryPlot.getDomainLabelWidget().pack();
        aprHistoryPlot.setRangeLabel("Angle (Degs)");
        aprHistoryPlot.getRangeLabelWidget().pack();

        // setup checkboxes:
        hwAcceleratedCb = (CheckBox) findViewById(R.id.hwAccelerationCb);
        final PlotStatistics levelStats = new PlotStatistics(1000, false);
        final PlotStatistics histStats = new PlotStatistics(1000, false);

        aprLevelsPlot.addListener(levelStats);
        aprHistoryPlot.addListener(histStats);
        hwAcceleratedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    aprLevelsPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                } else {
                    aprLevelsPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }
        });
    }

        //TODO: Map this into a function that can be called in onCharacteristicChanged()
//        @Override
    public synchronized void onSensorChanged(float magnitude) {
        {
            // update instantaneous data:
            Number[] series1Numbers = {magnitude};
            aprLevelsSeries.setModel(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

            // get rid the oldest sample in history:
            if (azimuthHistorySeries.size() > HISTORY_SIZE) {
//                rollHistorySeries.removeFirst();
//                pitchHistorySeries.removeFirst();
                azimuthHistorySeries.removeFirst();
            }

            // add the latest history sample:
            azimuthHistorySeries.addLast(null, magnitude);
//            pitchHistorySeries.addLast(null, sensorEvent.values[1]);
//            rollHistorySeries.addLast(null, sensorEvent.values[2]);

            // redraw the Plots:
            aprLevelsPlot.redraw();
            aprHistoryPlot.redraw();
        }
    }

    /**
     * A simple formatter to convert bar indexes into sensor names.
     */
    private class APRIndexFormat extends Format {
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            Number num = (Number) obj;

            // using num.intValue() will floor the value, so we add 0.5 to round instead:
            int roundNum = (int) (num.floatValue() + 0.5f);
            switch (roundNum) {
                case 0:
                    toAppendTo.append("Azimuth");
                    break;
                case 1:
                    toAppendTo.append("Pitch");
                    break;
                case 2:
                    toAppendTo.append("Roll");
                    break;
                default:
                    toAppendTo.append("Unknown");
            }
            return toAppendTo;
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return null;  // We don't use this so just return null for now.
        }
    }


//    @OnClick(R.id.future_button)
//    void futureButton() {
//        Log.w("DEBUG", "Future Graph Button Bressed");
//
////        Intent intent = new Intent(this, RoundTripGraphs.class);
////        startActivity(intent);
//    }
}


