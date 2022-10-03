package org.bioimageanalysis.icy.deeplearning.transformations;

import org.bioimageanalysis.icy.deeplearning.tensor.RaiArrayUtils;
import org.bioimageanalysis.icy.deeplearning.tensor.Tensor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class ScaleRangeTransformation extends AbstractTensorTransformation
{

	private static final String name = "scale_range";
	private double minPercentile = 0;
	private double maxPercentile = 100;
	private String axes;
	private Mode mode = Mode.PER_SAMPLE;
	private String tensorName;
	private float eps = 10^-6;
	
	public ScaleRangeTransformation()
	{
		super( name );
	}
	
	public void setMinPercentile(double minPercentile) {
		this.minPercentile = minPercentile;
	}
	
	public void setMaxPercentile(double maxPercentile) {
		this.maxPercentile = maxPercentile;
	}
	
	public void setAxes(String axes) {
		this.axes = axes;
	}
	
	public void setTensorName(String tensorName) {
		this.tensorName = tensorName;
	}
	
	public void setMode(String mode) {
		this.mode = Mode.valueOf(mode);
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	@Override
	public < R extends RealType< R > & NativeType< R > > Tensor< FloatType > apply( final Tensor< R > input )
	{
		final Tensor< FloatType > output = makeOutput( input );
		applyInPlace(output);
		return output;
	}

	@Override
	public void applyInPlace(Tensor<FloatType> input) {
		String selectedAxes = "";
		for (String ax : input.getAxesOrderString().split("")) {
			if (axes != null && !axes.toLowerCase().contains(ax.toLowerCase())
					&& !ax.toLowerCase().equals("b"))
				selectedAxes += ax;
		}
		if (axes == null || selectedAxes.equals("") 
				|| input.getAxesOrderString().replace("b", "").length() - selectedAxes.length() == 1) {
			globalScale(input);
		} else if (input.getAxesOrderString().replace("b", "").length() - selectedAxes.length() == 2) {
			axesScale(input, selectedAxes);
		} else {
			//TODO allow scaling of more complex structures
			throw new IllegalArgumentException("At the moment, only allowed scaling of planes.");
		}
		
	}
	
	private void globalScale( final Tensor< FloatType > output ) {
		float minPercentileVal = findPercentileValue(output.getData(), minPercentile);
		float maxPercentileVal = findPercentileValue(output.getData(), maxPercentile);
		LoopBuilder.setImages( output.getData() )
				.multiThreaded()
				.forEachPixel( i -> i.set( ( i.get() - minPercentileVal ) / ( maxPercentileVal - minPercentileVal + eps ) ) );
	}
	
	private float findPercentileValue(RandomAccessibleInterval<FloatType> rai, double percentile) {
		double percentileVal = Util.percentile(RaiArrayUtils.convertFloatArrIntoDoubleArr(RaiArrayUtils.floatArray(rai)), percentile);
		return (float) percentileVal;
	}
	
	private void axesScale( final Tensor< FloatType > output, String axesOfInterest) {
		long[] start = new long[output.getData().numDimensions()];
		long[] dims = output.getData().dimensionsAsLongArray();
		long[] indOfDims = new long[axesOfInterest.length()];
		long[] sizeOfDims = new long[axesOfInterest.length()];
		for (int i = 0; i < indOfDims.length; i ++) {
			indOfDims[i] = output.getAxesOrderString().indexOf(axesOfInterest.split("")[i]);
		}
		for (int i = 0; i < sizeOfDims.length; i ++) {
			sizeOfDims[i] = dims[(int) indOfDims[i]];
		}
		
		long[][] points = getAllCombinations(sizeOfDims);
		int c = 0;
		for (long[] pp : points) {
			for (int i = 0; i < pp.length; i ++) {
				start[(int) indOfDims[i]] = pp[i];
				dims[(int) indOfDims[i]] = pp[i] + 1;
			}
			IntervalView<FloatType> plane = Views.interval( output.getData(), start, dims );
			float minPercentileVal = findPercentileValue(plane, minPercentile);
			float maxPercentileVal = findPercentileValue(plane, maxPercentile);
			LoopBuilder.setImages( plane )
					.multiThreaded()
					.forEachPixel( i -> i.set( ( i.get() - minPercentileVal ) / ( maxPercentileVal - minPercentileVal  + eps) ) );
		}
	}
	
	private static long[][] getAllCombinations(long[] arr){
		long n = 1;
		for (long nn : arr) n *= nn;
		long[][] allPoints = new long[(int) n][arr.length];
		for (int i = 0; i < n; i ++) {
			for (int j = 0; j < arr.length; j ++) {
				int factor = 1;
				for (int k = 0; k < j; k ++) {
					factor *= arr[k];
				}
				int auxVal = i / factor;
				int val = auxVal % ((int) arr[j]);
				allPoints[i][j] = val;
			}
		}
		return allPoints;
	}
}
