package com.mygdx.game.android.ControlPanel;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.Button;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.mygdx.game.android.NeblinaClasses.Neblina;
import com.mygdx.game.android.R;

import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class RoundTripGraphs extends Activity {

    @InjectView(R.id.to_dynamic_button)
    Button toDynamicButton;

    private XYPlot plot;
    private XYPlot histogram;
    private int histogramSizeMax = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_round_trip_graphs);
        ButterKnife.inject(this);

        // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.round_trip_plot);
        histogram = (XYPlot) findViewById(R.id.round_trip_histogram);

        long max_value = 0;

        // create a couple arrays of y-values to plot:
        Number[] series1Numbers = new Number[Neblina.size_max];
        for(int i =0;i<Neblina.size_max;i++){
            series1Numbers[i] = Neblina.roundTripTimeArray[i];
            if (Neblina.roundTripTimeArray[i] > max_value) max_value = Neblina.roundTripTimeArray[i];
        }

        //initialize the histogramSeries
        Number[] histogramSeriesNumbers = new Number[histogramSizeMax];
        for(int i =0;i<histogramSizeMax;i++){
            histogramSeriesNumbers[i] = 0;
        }
        //Chop the series into 20 blocks based on the max value
        //Go through each element of the series
        //Increment one of the 20 blocks

        for(int i=0; i<Neblina.size_max;i++){
            double highValue = (double) max_value;
            double value = (double) Neblina.roundTripTimeArray[i];
            int bin = (int) Math.floor((value / highValue)*(histogramSizeMax-1));
            histogramSeriesNumbers[bin] = histogramSeriesNumbers[bin].intValue() +1;
        }


        // turn the above arrays into XYSeries':
        // (Y_VALS_ONLY means use the element index as the x value)
        XYSeries series1 = new SimpleXYSeries(Arrays.asList(series1Numbers),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series1");

        XYSeries histogramSeries = new SimpleXYSeries(Arrays.asList(histogramSeriesNumbers),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Histogram");

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        LineAndPointFormatter histogramSeriesFormat = new LineAndPointFormatter();

        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        histogramSeriesFormat.setPointLabelFormatter(new PointLabelFormatter());

        series1Format.configure(getApplicationContext(),
                R.layout.line_point_formatter_with_labels);
        histogramSeriesFormat.configure(getApplicationContext(),
                R.layout.line_point_formatter_with_labels);
        // just for fun, add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
//        series1Format.setInterpolationParams(
//                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));


        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);
        histogram.addSeries(histogramSeries,histogramSeriesFormat);

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);

        // rotate domain labels 45 degrees to make them more compact horizontally:
        plot.getGraphWidget().setDomainLabelOrientation(-45);
    }


    @OnClick(R.id.to_dynamic_button)void dynamicButtonPressed() {
        Log.w("DEBUG", "To Dynamic Data Button Pressed");
        Intent intent = new Intent(this, DynamicData.class);
        startActivity(intent);
    }
}
