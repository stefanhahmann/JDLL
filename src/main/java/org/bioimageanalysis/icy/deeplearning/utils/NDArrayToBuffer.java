package org.bioimageanalysis.icy.deeplearning.utils;

import java.nio.Buffer;
import java.util.Arrays;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceCursor;

/**
 * Class that builds Buffers that can be retrieved by the Java consumer softwares
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class NDArrayToBuffer {

    /**
     * Utility class.
     */
    private NDArrayToBuffer()
    {
    }

    /**
     * Build an Icy sequence using the information from a tensor (NDArray) arranged with some
     * particular axes order specified with tensorDimOrder
     * @param tensor
     * 	NDArray containing the data to be transferred to the sequence
     * @param tensorDimOrder
     * 	order of the axes in the tensor
     * @return an Icy sequence with the information of the tensor
     * @throws IllegalArgumentException
     */
    public static Buffer build(NDArray tensor, int[] tensorDimOrder, int[] targetDimOrder) throws IllegalArgumentException
    {
		// TODO adapt to several batch sizes
		// Check if the axes order is valid
		checkTensorDimOrder(tensor, tensorDimOrder);
		// Add missing dimensions to the tensor axes order. The missing dimensions
		// are added at the end
		int[] completeDimOrder = completeImageDimensions(tensorDimOrder, targetDimOrder.length);
		// Get the order of the tensor with respect to the axes of anIcy sequence
        int[] seqDimOrder = getSequenceDimOrder(completeDimOrder);
        // GEt the size of the tensor for every dimension existing in an Icy sequence
        int[] seqSize = getSequenceSize(tensorDimOrder, tensor);
        // Create an Icy sequence of the same type of the tensor
        if (tensor.getDataType() == DataType.UINT8)
        {
            return buildFromTensorByte(icy.type.DataType.UBYTE, seqSize, seqDimOrder, tensor);
        }
        else if (tensor.getDataType() == DataType.INT32)
        {
            return buildFromTensorInt(icy.type.DataType.INT, seqSize, seqDimOrder, tensor);
        }
        else if (tensor.getDataType() == DataType.FLOAT32)
        {
            return buildFromTensorFloat(icy.type.DataType.FLOAT, seqSize, seqDimOrder, tensor);
        }
        else if (tensor.getDataType() == DataType.FLOAT64)
        {
            return buildFromTensorDouble(icy.type.DataType.DOUBLE, seqSize, seqDimOrder, tensor);
        }
        else if (tensor.getDataType() == DataType.BOOLEAN)
        {
            return buildFromTensorBoolean(icy.type.DataType.UBYTE, seqSize, seqDimOrder, tensor);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported tensor type: " + tensor.getDataType().name());
        }
    }

	/**
     * This method copies the information from a tensor (NDArray) to a float Icy Sequence. 
     * NOTE: For the moment only works for batch size = 1
	 * @param dtype
	 * 	data type of the icy image to be created
	 * @param seqSize
	 * 	size of the tensor for every dimension existing in
	 * a sequence
	 * @param seqDimOrder
	 * 	oder of the tensor dimensions (axes) with respect to the
	 * 	sequence order
	 * @param tensor
	 * 	tensor from where the info will be taken
	 * @return image (Icy sequence) with all the data
	 */
	private static Sequence buildFromTensorFloat(icy.type.DataType dtype, int[] seqSize, int[] seqDimOrder, NDArray tensor) {

        // Create result sequence
        Sequence sequence = createSequence(seqSize, dtype);// Fill the sequence with the NDArray data using cursors
		SequenceCursor cursor = new SequenceCursor(sequence);	
        // Create an array with the shape of the tensor for every dimension in Icy
        // REcall that Icy axes are organized as [xyztc] but in this plugin
        // to keep the convention with ImageJ and Fiji, we will always act as
        // they were [xyczt]. That is why in the following command, after
        // tensorSize[seqDimOrder[1]], it goes tensorSize[seqDimOrder[4]],
        // instead of tensorSize[seqDimOrder[2]], because seqSize uses 
        // Icy axes, but seqDimOrder refers to the tensor from ImageJ axes
        int[] tensorShape = {seqSize[seqDimOrder[0]], seqSize[seqDimOrder[1]], seqSize[seqDimOrder[4]], seqSize[seqDimOrder[2]], seqSize[seqDimOrder[3]]};
        float[] flatImageArray = tensor.toFloatArray();
		int pos = 0;
		int[] auxInd = {0, 0, 0, 0, 0};
		for (int i0 = 0; i0 < tensorShape[0]; i0 ++) {
			auxInd[0] = i0;
			for (int i1 = 0; i1 < tensorShape[1]; i1 ++) {
				auxInd[1] = i1;
				for (int i2 = 0; i2 < tensorShape[2]; i2 ++) {
					auxInd[2] = i2;
					for (int i3 = 0; i3 < tensorShape[3]; i3 ++) {
						auxInd[3] = i3;
						for (int i4 = 0; i4 < tensorShape[4]; i4 ++) {
							auxInd[4] = i4;
							cursor.set(auxInd[seqDimOrder[0]], auxInd[seqDimOrder[1]], auxInd[seqDimOrder[3]], auxInd[seqDimOrder[4]], auxInd[seqDimOrder[2]], (float) flatImageArray[pos ++]);
						}
					}
				}
			}
		}
		cursor.commitChanges();
		return sequence;
	}

	/**
     * This method copies the information from a tensor (NDArray) to a float Icy Sequence. 
     * NOTE: For the moment only works for batch size = 1
	 * @param dtype
	 * 	data type of the icy image to be created
	 * @param tensorSize
	 * 	size of the tensor for every dimension existing in
	 * a sequence
	 * @param seqDimOrder
	 * 	oder of the tensor dimensions (axes) with respect to the
	 * 	sequence order
	 * @param tensor
	 * 	tensor from where the info will be taken
	 * @return image (Icy sequence) with all the data
	 */
	private static Sequence buildFromTensorDouble(icy.type.DataType dtype, int[] tensorSize, int[] seqDimOrder, NDArray tensor) {
		// Get the sequence size using Icy's axes order
        int[] seqSize = getSequenceSize(seqDimOrder, tensor);
        // Create result sequence
        Sequence sequence = createSequence(seqSize, dtype);
        // Fill the sequence with the NDArray data using cursors
		SequenceCursor cursor = new SequenceCursor(sequence);
        // Create an array with the shape of the tensor for every dimension in Icy
        // REcall that Icy axes are organized as [xyztc] but in this plugin
        // to keep the convention with ImageJ and Fiji, we will always act as
        // they were [xyczt]. That is why in the following command, after
        // tensorSize[seqDimOrder[1]], it goes tensorSize[seqDimOrder[4]],
        // instead of tensorSize[seqDimOrder[2]], because seqSize uses 
        // Icy axes, but seqDimOrder refers to the tensor from ImageJ axes
        int[] tensorShape = {seqSize[seqDimOrder[0]], seqSize[seqDimOrder[1]], seqSize[seqDimOrder[4]], seqSize[seqDimOrder[2]], seqSize[seqDimOrder[3]]};
        double[] flatImageArray = tensor.toDoubleArray();
		int pos = 0;
		int[] auxInd = {0, 0, 0, 0, 0};
		for (int i0 = 0; i0 < tensorShape[0]; i0 ++) {
			auxInd[0] = i0;
			for (int i1 = 0; i1 < tensorShape[1]; i1 ++) {
				auxInd[1] = i1;
				for (int i2 = 0; i2 < tensorShape[2]; i2 ++) {
					auxInd[2] = i2;
					for (int i3 = 0; i3 < tensorShape[3]; i3 ++) {
						auxInd[3] = i3;
						for (int i4 = 0; i4 < tensorShape[4]; i4 ++) {
							auxInd[4] = i4;
							cursor.set(auxInd[seqDimOrder[0]], auxInd[seqDimOrder[1]], auxInd[seqDimOrder[3]], auxInd[seqDimOrder[4]], auxInd[seqDimOrder[2]], (double) flatImageArray[pos ++]);
						}
					}
				}
			}
		}
		cursor.commitChanges();
		return sequence;
	}

	/**
     * This method copies the information from a tensor (NDArray) to a float Icy Sequence. 
     * NOTE: For the moment only works for batch size = 1
	 * @param dtype
	 * 	data type of the icy image to be created
	 * @param tensorSize
	 * 	size of the tensor for every dimension existing in
	 * a sequence
	 * @param seqDimOrder
	 * 	oder of the tensor dimensions (axes) with respect to the
	 * 	sequence order
	 * @param tensor
	 * 	tensor from where the info will be taken
	 * @return image (Icy sequence) with all the data
	 */
	private static Sequence buildFromTensorInt(icy.type.DataType dtype, int[] tensorSize, int[] seqDimOrder, NDArray tensor) {
		// Get the sequence size using Icy's axes order
        int[] seqSize = getSequenceSize(seqDimOrder, tensor);
        // Create result sequence
        Sequence sequence = createSequence(seqSize, dtype);
        // Fill the sequence with the NDArray data using cursors
		SequenceCursor cursor = new SequenceCursor(sequence);
        // Create an array with the shape of the tensor for every dimension in Icy
        // REcall that Icy axes are organized as [xyztc] but in this plugin
        // to keep the convention with ImageJ and Fiji, we will always act as
        // they were [xyczt]. That is why in the following command, after
        // tensorSize[seqDimOrder[1]], it goes tensorSize[seqDimOrder[4]],
        // instead of tensorSize[seqDimOrder[2]], because seqSize uses 
        // Icy axes, but seqDimOrder refers to the tensor from ImageJ axes
        int[] tensorShape = {seqSize[seqDimOrder[0]], seqSize[seqDimOrder[1]], seqSize[seqDimOrder[4]], seqSize[seqDimOrder[2]], seqSize[seqDimOrder[3]]};
        int[] flatImageArray = tensor.toIntArray();
		int pos = 0;
		int[] auxInd = {0, 0, 0, 0, 0};
		for (int i0 = 0; i0 < tensorShape[0]; i0 ++) {
			auxInd[0] = i0;
			for (int i1 = 0; i1 < tensorShape[1]; i1 ++) {
				auxInd[1] = i1;
				for (int i2 = 0; i2 < tensorShape[2]; i2 ++) {
					auxInd[2] = i2;
					for (int i3 = 0; i3 < tensorShape[3]; i3 ++) {
						auxInd[3] = i3;
						for (int i4 = 0; i4 < tensorShape[4]; i4 ++) {
							auxInd[4] = i4;
							cursor.set(auxInd[seqDimOrder[0]], auxInd[seqDimOrder[1]], auxInd[seqDimOrder[3]], auxInd[seqDimOrder[4]], auxInd[seqDimOrder[2]], (int) flatImageArray[pos ++]);
						}
					}
				}
			}
		}
		cursor.commitChanges();
		return sequence;
	}

	/**
     * This method copies the information from a tensor (NDArray) to a float Icy Sequence. 
     * NOTE: For the moment only works for batch size = 1
	 * @param dtype
	 * 	data type of the icy image to be created
	 * @param tensorSize
	 * 	size of the tensor for every dimension existing in
	 * a sequence
	 * @param seqDimOrder
	 * 	oder of the tensor dimensions (axes) with respect to the
	 * 	sequence order
	 * @param tensor
	 * 	tensor from where the info will be taken
	 * @return image (Icy sequence) with all the data
	 */
	private static Sequence buildFromTensorByte(icy.type.DataType dtype, int[] tensorSize, int[] seqDimOrder, NDArray tensor) {
		// Get the sequence size using Icy's axes order
        int[] seqSize = getSequenceSize(seqDimOrder, tensor);
        // Create result sequence
        Sequence sequence = createSequence(seqSize, dtype);
        // Fill the sequence with the NDArray data using cursors
		SequenceCursor cursor = new SequenceCursor(sequence);
        // Create an array with the shape of the tensor for every dimension in Icy
        // REcall that Icy axes are organized as [xyztc] but in this plugin
        // to keep the convention with ImageJ and Fiji, we will always act as
        // they were [xyczt]. That is why in the following command, after
        // tensorSize[seqDimOrder[1]], it goes tensorSize[seqDimOrder[4]],
        // instead of tensorSize[seqDimOrder[2]], because seqSize uses 
        // Icy axes, but seqDimOrder refers to the tensor from ImageJ axes
        int[] tensorShape = {seqSize[seqDimOrder[0]], seqSize[seqDimOrder[1]], seqSize[seqDimOrder[4]], seqSize[seqDimOrder[2]], seqSize[seqDimOrder[3]]};
        byte[] flatImageArray = tensor.toByteArray();
		int pos = 0;
		int[] auxInd = {0, 0, 0, 0, 0};
		for (int i0 = 0; i0 < tensorShape[0]; i0 ++) {
			auxInd[0] = i0;
			for (int i1 = 0; i1 < tensorShape[1]; i1 ++) {
				auxInd[1] = i1;
				for (int i2 = 0; i2 < tensorShape[2]; i2 ++) {
					auxInd[2] = i2;
					for (int i3 = 0; i3 < tensorShape[3]; i3 ++) {
						auxInd[3] = i3;
						for (int i4 = 0; i4 < tensorShape[4]; i4 ++) {
							auxInd[4] = i4;
							cursor.set(auxInd[seqDimOrder[0]], auxInd[seqDimOrder[1]], auxInd[seqDimOrder[3]], auxInd[seqDimOrder[4]], auxInd[seqDimOrder[2]], (byte) flatImageArray[pos ++]);
						}
					}
				}
			}
		}
		cursor.commitChanges();
		return sequence;
	}

	/**
     * This method copies the information from a tensor (NDArray) to a float Icy Sequence. 
     * NOTE: For the moment only works for batch size = 1
	 * @param dtype
	 * 	data type of the icy image to be created
	 * @param tensorSize
	 * 	size of the tensor for every dimension existing in
	 * a sequence
	 * @param seqDimOrder
	 * 	oder of the tensor dimensions (axes) with respect to the
	 * 	sequence order
	 * @param tensor
	 * 	tensor from where the info will be taken
	 * @return image (Icy sequence) with all the data
	 */
	private static Sequence buildFromTensorBoolean(icy.type.DataType dtype, int[] tensorSize, int[] seqDimOrder, NDArray tensor) {
		// Get the sequence size using Icy's axes order
        int[] seqSize = getSequenceSize(seqDimOrder, tensor);
        // Create result sequence
        Sequence sequence = createSequence(seqSize, dtype);
        // Fill the sequence with the NDArray data using cursors
		SequenceCursor cursor = new SequenceCursor(sequence);
        // Create an array with the shape of the tensor for every dimension in Icy
        // REcall that Icy axes are organized as [xyztc] but in this plugin
        // to keep the convention with ImageJ and Fiji, we will always act as
        // they were [xyczt]. That is why in the following command, after
        // tensorSize[seqDimOrder[1]], it goes tensorSize[seqDimOrder[4]],
        // instead of tensorSize[seqDimOrder[2]], because seqSize uses 
        // Icy axes, but seqDimOrder refers to the tensor from ImageJ axes
        int[] tensorShape = {seqSize[seqDimOrder[0]], seqSize[seqDimOrder[1]], seqSize[seqDimOrder[4]], seqSize[seqDimOrder[2]], seqSize[seqDimOrder[3]]};
        byte[] flatImageArray = tensor.toByteArray();
		int pos = 0;
		int[] auxInd = {0, 0, 0, 0, 0};
		for (int i0 = 0; i0 < tensorShape[0]; i0 ++) {
			auxInd[0] = i0;
			for (int i1 = 0; i1 < tensorShape[1]; i1 ++) {
				auxInd[1] = i1;
				for (int i2 = 0; i2 < tensorShape[2]; i2 ++) {
					auxInd[2] = i2;
					for (int i3 = 0; i3 < tensorShape[3]; i3 ++) {
						auxInd[3] = i3;
						for (int i4 = 0; i4 < tensorShape[4]; i4 ++) {
							auxInd[4] = i4;
							double val = flatImageArray[pos ++] == 0 ? 0d : icy.type.DataType.UBYTE_MAX_VALUE;
							cursor.set(auxInd[seqDimOrder[0]], auxInd[seqDimOrder[1]], auxInd[seqDimOrder[3]], auxInd[seqDimOrder[4]], auxInd[seqDimOrder[2]], val);
						}
					}
				}
			}
		}
		cursor.commitChanges();
		return sequence;
	}
	
	/**
	 * Check that the dimensions order provided is compatible with
	 * the NDArray given. If it is not, the method throws an exception,
	 * if it is, nothing happens
	 * @param tensor
	 * 	NDArray given
	 * @param tensorDimOrder
	 * 	dimensions (axes) order given
	 * @throws IllegalArgumentException
	 */
    private static void checkTensorDimOrder(NDArray tensor, int[] tensorDimOrder)
            throws IllegalArgumentException
    {
        if (tensorDimOrder.length != tensor.getShape().dimension())
        {
            throw new IllegalArgumentException(
                    "Tensor dim order array length is different than number of dimensions in tensor ("
                            + tensorDimOrder.length + " != " + tensor.getShape().dimension() + ")");
        }
    }

    // TODO change ImageJ's axes order [xyczt] to Icy's axes order [xyztc]
    /**
     * Get the size of each of the dimensions expressed in an array that
     * follows the ImageJ axes order -> xyczt
     * @param seqDimOrder
     * 	order of the dimensions of the Icy sequence with respect to the tensor
     * @param tensor
     * 	tensor containing the data
     * @return array containing the size for each dimension
     */
    private static int[] getSequenceSize(int[] seqDimOrder, NDArray tensor)
    {
        Shape shape = tensor.getShape();
        int[] dims = new int[] {1, 1, 1, 1, 1};
        for (int i = 0; i < seqDimOrder.length; i ++) {
        	dims[seqDimOrder[i]] = (int) shape.size(i);
        }
        return dims;
    }

    /**
     * Create a sequence of the wanted size and data type
     * @param size
     * 	wanted size of the sequence in the order [xyczt]
     * @param type
     * 	type of the sequence
     * @return empty Icy sequence of the wanted type and dimensions
     */
    private static Sequence createSequence(int[] size, icy.type.DataType type)
    {
        Sequence seq = new Sequence();
        int t, z;
        for (t = 0; t < size[4]; t++)
        {
            for (z = 0; z < size[3]; z++)
            {
                seq.setImage(t, z, new IcyBufferedImage(size[0], size[1], size[2], type));
            }
        }
        return seq;
    }
    
    // TODO improve efficiency
    /**
     * Add to the tensor axes order array the dimensions missing,
     * the dimensions are always added at the end.
     * For example, for a tensor with axes [byxc], its tensorDimOrder
     * would be transformed from [4,1,0,2] to [4,1,0,2,3] ([byxcz])
     * @param tensorDimOrder; axes order of the tensor
     * @return new axes order with dimensions at the end
     */
    private static int[] completeImageDimensions(int[] tensorDimOrder, int numDimsTarget) {
    	int nTensorDims = tensorDimOrder.length;
    	int missingDims = numDimsTarget - nTensorDims;
    	int[] missingDimsArr = new int[missingDims];
    	int c = 0;
    	for (int ii : new int[] {0, 1, 2, 3, 4}) {
    		if (Arrays.stream(tensorDimOrder).noneMatch(i -> i == ii))
    			missingDimsArr[c ++] = ii;
    	}
    	int[] completeDims = new int[numDimsTarget];
        System.arraycopy(tensorDimOrder, 0, completeDims, 0, tensorDimOrder.length);
        System.arraycopy(missingDimsArr, 0, completeDims, tensorDimOrder.length, missingDimsArr.length);
    	return completeDims;
    }

    /**
     * Get the tensor dimensions order with respect to ImageJ's dimensions order ([xyczt]).
     * For example, for some tensor axes order is [bcyx], the tensorDimOrder will be
     * [4, 2, 1, 0], this method will map the dimensions to the axes order of ImageJ ([xyczt]).
     * Dimension X is on the fourth axis of the tensor, dimension Y on the third and so on, so
     * the result would be [3, 2, 1, -1, 0] (because axis Z is not presetn).
     * @param tensorDimOrder
     * 	axes order of a tensor
     * @param targetDimOrder
     * axes order of the target buffer
     * @return
     * 	axes order of a tensor with respect to ImageJ's axes order
     */
    private static int[] getSequenceDimOrder(int[] tensorDimOrder, int[] targetDimOrder)
    {
        int[] seqDimOrder = new int[targetDimOrder.length];
        for (int i = 0; i < tensorDimOrder.length; i++)
        {
        	tensorDimOrder[targetDimOrder[i]] = i;
        }
        return seqDimOrder;
    }
}
