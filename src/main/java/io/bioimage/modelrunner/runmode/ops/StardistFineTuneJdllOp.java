/*-
 * #%L
 * Use deep learning frameworks from Java in an agnostic and isolated way.
 * %%
 * Copyright (C) 2022 - 2023 Institut Pasteur and BioImage.IO developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.bioimage.modelrunner.runmode.ops;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.Yaml;

import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.tensor.Tensor;
import io.bioimage.modelrunner.utils.Constants;
import io.bioimage.modelrunner.utils.YAMLUtils;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * TODO
 * TODO
 * TODO
 * TODO
 * TODO add support for stardist 3D
 * 
 * 
 * 
 * Class that defines the methods needed to fine tune a StarDist pre-trained model
 * using JDLL and Python with Appose.
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class StardistFineTuneJdllOp implements OpInterface {
	
	private String model;
	
	private String nModelPath;
	
	private int nChannelsModel;
	
	private float lr = (float) 1e-5;
	
	private int batchSize = 16;
	
	private int epochs = 1;
	
	private boolean downloadStardistPretrained = false;
	
	private Tensor<FloatType> trainingSamples;
	
	private Tensor<UnsignedShortType> groundTruth;
	
	private String opFilePath;
	
	private String envPath;
	
	private LinkedHashMap<String, Object> inputsMap;
	
	private static final List<String> PRETRAINED_3C_STARDIST_MODELS;
	static {
		PRETRAINED_3C_STARDIST_MODELS = new ArrayList<String>();
		PRETRAINED_3C_STARDIST_MODELS.add("2D_versatile_fluo");
		PRETRAINED_3C_STARDIST_MODELS.add("2D_paper_dsb2018");
	}
	
	private static final List<String> PRETRAINED_1C_STARDIST_MODELS;
	static {
		PRETRAINED_1C_STARDIST_MODELS = new ArrayList<String>();
		PRETRAINED_1C_STARDIST_MODELS.add("2D_versatile_he");
	}
	
	private final static String STARDIST_CONFIG_KEY = "config";
	
	private final static String CONFIG_JSON = "config.json";
	
	private final static String STARDIST_THRES_KEY = "thresholds";
	
	private final static String THRES_JSON = "thresholds.json";
	
	private final static String MODEL_KEY = "model";
	
	private final static String NEW_MODEL_DIR_KEY = "n_model_dir";
	
	private final static String TRAIN_SAMPLES_KEY = "train_samples";
	
	private final static String GROUND_TRUTH_KEY = "ground_truth";
	
	private final static String PATCH_SIZE_KEY = "train_patch_size";
	
	private final static String BATCH_SIZE_KEY = "train_batch_size";
	
	private final static String LR_KEY = "train_learning_rate";
	
	private final static String EPOCHS_KEY = "train_epochs";
	
	private static final String STARDIST_WEIGHTS_FILE = "stardist_weights.h5";
	
	private final static String DOWNLOAD_STARDIST_KEY = "download_pretrained_stardist";
	
	private static final String OP_METHOD_NAME = "stardist_prediction_2d_mine";
	
	private static final int N_STARDIST_OUTPUTS = 1;
	
	private static final String STARDIST_OP_FNAME = "stardist_inference.py";
	
	private static final String STARDIST_2D_AXES = "bxyc";
	
	private static final String STARDIST_3D_AXES = "bxyzc";
	
	private static final String GROUNDTRUTH_AXES = "bxy";
	
	/**
	 * Create a JDLL OP to fine tune a stardist model with the wanted data.
	 * In order to set the data we want to fine tune the model on, use {@link #setFineTuningData(List, List)}
	 * or {@link #setFineTuningData(Tensor, Tensor)}. The batch size and learning rates
	 * can also be modified by with {@link #setBatchSize(int)} and {@link #setLearingRate(float)}.
	 * By default the batch size is 16 and the learning rate 1e-5.
	 * To set the number of epochs: {@link #setEpochs(int)}, default is 1.
	 * @param modelToFineTune
	 * 	Pre-trained model that is going to be fine tuned on the user's data, it
	 *  can be either a model existing in the users machine or a model existing in the model
	 *  zoo. If it is a model existing in th emodel zoo, it will have to be downloaded first.
	 * @param newModelDir
	 * 	directory where the new model will be saved
	 * @return a JDLL OP that can be used together with {@link RunMode} to fine tune a StarDist
	 * 	model on the user's data
	 */
	public StardistFineTuneJdllOp create(String modelToFineTune, String newModelDir) {
		StardistFineTuneJdllOp op = new StardistFineTuneJdllOp();
		op.nModelPath = newModelDir;
		op.setModel(modelToFineTune);
		try {
			op.findNChannels();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to correctly read the rdf.yaml file "
					+ "of Bioimage.io StarDist model at :" + this.model, e);
		}
		return op;
	}
	
	public < T extends RealType< T > & NativeType< T > > 
		void setFineTuningData(List<Tensor<T>> trainingSamples, List<Tensor<T>> groundTruth) {
		
	}
	
	public < T extends RealType< T > & NativeType< T > > 
		void setFineTuningData(Tensor<T> trainingSamples, Tensor<T> groundTruth) {
		checkTrainAndGroundTruthDimensions(trainingSamples, groundTruth);
		setTrainingSamples(trainingSamples);
		setGroundTruth(groundTruth);
		setUpConfigs();
	}
	
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
	
	public void setLearingRate(float learningRate) {
		this.lr = learningRate;
	}
	
	public void setEpochs(int epochs) {
		this.epochs = epochs;
	}

	@Override
	public String getOpPythonFilename() {
		return STARDIST_OP_FNAME;
	}

	@Override
	public int getNumberOfOutputs() {
		return N_STARDIST_OUTPUTS;
	}

	@Override
	public boolean isOpInstalled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void installOp() {
		// TODO this method checks if the OP file is at its correponding folder.
		// TODO if not unpack the python file and located (where??)
		opFilePath = "C:\\Users\\angel\\OneDrive\\Documentos\\pasteur\\git\\model-runner-java\\python\\ops\\stardist_inference";
		// TODO check if the env has also been created
		// TODO if not create it (where??)
		envPath  = "C:\\Users\\angel\\git\\jep\\miniconda\\envs\\stardist";
	}

	@Override
	public LinkedHashMap<String, Object> getOpInputs() {
		inputsMap = new LinkedHashMap<String, Object>();
		inputsMap.put(MODEL_KEY, this.model);
		inputsMap.put(NEW_MODEL_DIR_KEY, this.nModelPath);
		inputsMap.put(TRAIN_SAMPLES_KEY, this.trainingSamples);
		inputsMap.put(GROUND_TRUTH_KEY, this.groundTruth);
		inputsMap.put(BATCH_SIZE_KEY, this.batchSize);
		inputsMap.put(LR_KEY, this.lr);
		inputsMap.put(EPOCHS_KEY, this.epochs);
		inputsMap.put(DOWNLOAD_STARDIST_KEY, this.downloadStardistPretrained);
		return inputsMap;
	}

	@Override
	public String getCondaEnv() {
		return envPath;
	}

	@Override
	public String getMethodName() {
		return OP_METHOD_NAME;
	}

	@Override
	public String getOpDir() {
		return opFilePath;
	}
	
	public void setModel(String modelName) throws IllegalArgumentException {
		Objects.requireNonNull(modelName, "The modelName input argument cannot be null.");
		if (PRETRAINED_1C_STARDIST_MODELS.contains(modelName) || PRETRAINED_3C_STARDIST_MODELS.contains(modelName)) {
			this.model = modelName;
			this.downloadStardistPretrained = true;
			return;
		}
		if (new File(modelName).isFile() && !StardistInferJdllOp.isModelFileStardist(modelName))
			throw new IllegalArgumentException("The file selected does not correspond to "
					+ "the rdf.yaml file of a Bioiamge.io Stardist model.");
		else if (!(new File(modelName).isFile()) && !StardistInferJdllOp.isModelNameStardist(modelName))
			throw new IllegalArgumentException("The model name provided does not correspond to a valid"
					+ " Stardist model present in the Bioimage.io online reposritory.");
		this.model = modelName;
	}
	
	private void setUpStardistModelFromBioimageio() throws IOException, InterruptedException {
		BioimageioRepo br = BioimageioRepo.connect();
		if (br.selectByName(model) != null) {
			model = br.downloadByName(model, nModelPath);
		} else if (br.selectByID(model) != null) {
			model = br.downloadModelByID(model, nModelPath);
		} else if (br.selectByNickname(model) != null) {
			model = br.downloadByNickame(model, nModelPath);
		}
		File folder = new File(model);
		String fineTuned = folder.getParent() + File.separator + "finetuned_" + folder.getName();
        File renamedFolder = new File(fineTuned);
        if (folder.renameTo(renamedFolder))
        	model = fineTuned;
        downloadBioimageioStardistWeights();
	}
	
	private void downloadBioimageioStardistWeights() throws IllegalArgumentException,
															IOException, Exception {
		File stardistSubfolder = new File(this.model, StardistInferJdllOp.STARDIST_FIELD_KEY);
        if (!stardistSubfolder.exists()) {
            if (!stardistSubfolder.mkdirs()) {
            	throw new IOException("Unable to create folder named 'stardist' at: " + this.model);
            }
        }
		setUpKerasWeights();
	}
	
	private void setUpConfigs() throws IOException, Exception {
		if (new File(model + File.separator + Constants.RDF_FNAME).exists()) {
			setUpConfigsBioimageio();
		} else if (!(new File(model + File.separator + CONFIG_JSON).exists())) {
			throw new IOException("Missing necessary file for StarDist: " + CONFIG_JSON);
		} else if (!(new File(model + File.separator + THRES_JSON).exists())) {
			throw new IOException("Missing necessary file for StarDist: " + THRES_JSON);
		} else {
			Map<String, Object> config = YAMLUtils.load(model + File.separator + CONFIG_JSON);
			Map<String, Object> thres = YAMLUtils.load(model + File.separator + THRES_JSON);
			int w = trainingSamples.getShape()[trainingSamples.getAxesOrderString().indexOf("x")];
			int h = trainingSamples.getShape()[trainingSamples.getAxesOrderString().indexOf("y")];
			config.put(PATCH_SIZE_KEY, new int[] {w, h});
			config.put(BATCH_SIZE_KEY, this.batchSize);
			config.put(LR_KEY, this.lr);
			config.put(EPOCHS_KEY, this.epochs);
			YAMLUtils.writeYamlFile(model + File.separator + CONFIG_JSON, (Map<String, Object>) config);
		}
	}
	
	private void setUpConfigsBioimageio() throws IOException, Exception {
		ModelDescriptor descriptor = ModelDescriptor.readFromLocalFile(this.model + File.separator + Constants.RDF_FNAME);
		Object stardistInfo = descriptor.getConfig().getSpecMap().get(StardistInferJdllOp.STARDIST_FIELD_KEY);
		
		if (stardistInfo == null || !(stardistInfo instanceof Map)) {
			throw new IllegalArgumentException("The rdf.yaml file of the Bioimage.io StarDist "
					+ "model at: " + this.model + " is invalid. The field config>stardist is missing."
					+ " Look for StarDist models in the Bioimage.io repo to see how the rdf.yaml should look like.");
		}
		Object config = ((Map<String, Object>) stardistInfo).get(STARDIST_CONFIG_KEY);
		if (config == null || !(config instanceof Map)) {
			throw new IllegalArgumentException("The rdf.yaml file of the Bioimage.io StarDist "
					+ "model at: " + this.model + " is invalid. The field config>stardist>" + STARDIST_CONFIG_KEY + " is missing."
					+ " Look for StarDist models in the Bioimage.io repo to see how the rdf.yaml should look like.");
		}
		Object thres = ((Map<String, Object>) stardistInfo).get(STARDIST_THRES_KEY);
		if (thres == null || !(thres instanceof Map)) {
			throw new IllegalArgumentException("The rdf.yaml file of the Bioimage.io StarDist "
					+ "model at: " + this.model + " is invalid. The field config>stardist>" + STARDIST_THRES_KEY + " is missing."
					+ " Look for StarDist models in the Bioimage.io repo to see how the rdf.yaml should look like.");
		}
		String subfolder = this.model + File.separator + StardistInferJdllOp.STARDIST_FIELD_KEY;
		int w = trainingSamples.getShape()[trainingSamples.getAxesOrderString().indexOf("x")];
		int h = trainingSamples.getShape()[trainingSamples.getAxesOrderString().indexOf("y")];
		((Map<String, Object>) config).put(PATCH_SIZE_KEY, new int[] {w, h});
		((Map<String, Object>) config).put(BATCH_SIZE_KEY, this.batchSize);
		((Map<String, Object>) config).put(LR_KEY, this.lr);
		((Map<String, Object>) config).put(EPOCHS_KEY, this.epochs);
		YAMLUtils.writeYamlFile(subfolder + File.separator + CONFIG_JSON, (Map<String, Object>) config);
		YAMLUtils.writeYamlFile(opFilePath + File.separator + THRES_JSON, (Map<String, Object>) thres);
	}
	
	private void setUpKerasWeights() throws IOException, Exception {
		String rdfYamlFN = this.model + File.separator + Constants.RDF_FNAME;
		ModelDescriptor descriptor = ModelDescriptor.readFromLocalFile(rdfYamlFN);
		String stardistWeights = this.model + File.separator +  StardistInferJdllOp.STARDIST_FIELD_KEY;
		stardistWeights += File.separator + STARDIST_WEIGHTS_FILE;
		if (new File(stardistWeights).exists())
			return;
		String stardistWeightsParent = this.model + File.separator + STARDIST_WEIGHTS_FILE;
		if (new File(stardistWeights).exists()) {
			try {
	            Files.copy(Paths.get(stardistWeightsParent), Paths.get(stardistWeights), StandardCopyOption.REPLACE_EXISTING);
				return;
	        } catch (IOException e) {
	        }
		}
		downloadFileFromInternet(getKerasWeigthsLink(descriptor), new File(stardistWeights));
	}
	
	private static String getKerasWeigthsLink(ModelDescriptor descriptor) throws IOException {
		Object yamlFiles = descriptor.getAttachments().get("files");
		if (yamlFiles == null || !(yamlFiles instanceof List))
			throw new IllegalArgumentException("");
		for (String url : (List<String>) yamlFiles) {
			try {
				if (DownloadModel.getFileNameFromURLString(url).equals(STARDIST_WEIGHTS_FILE))
					return url;
			} catch (MalformedURLException e) {
			}
		}
		throw new IOException("Stardist rdf.yaml file at : " + descriptor.getModelPath()
				+ " is invalid, as it does not contain the URL to StarDist Keras weights in "
				+ "the attachements field. Look for a StarDist model on the Bioimage.io "
				+ "repository for an example of a correct version.");
	}
	
	private void setUpStardistModelFromLocal() {
		
	}
	
	private < T extends RealType< T > & NativeType< T > > 
	 void checkTrainAndGroundTruthDimensions(Tensor<T> trainingSamples, Tensor<T> groundTruth) {
		String axes = trainingSamples.getAxesOrderString();
		if (axes.length() != STARDIST_2D_AXES.length())
			throw new IllegalArgumentException("Training sample tensors should have for dimensions ("
					+ STARDIST_2D_AXES + "), but it has " + axes.length() + " (" + axes + ").");
		for (int c = 0; c < STARDIST_2D_AXES.length(); c ++) {
			int trueInd = STARDIST_2D_AXES.indexOf(STARDIST_2D_AXES.split("")[c]);
			if (trueInd == -1)
				throw new IllegalArgumentException("The training samples provided should have dimension '"
						+ STARDIST_2D_AXES.split("")[c] + "' in the axes order, but it does not (" + axes + ").");
			else if (trueInd == c) {
				c ++;
				continue;
			}
			IntervalView<T> wrapImg = Views.permute(trainingSamples.getData(), trueInd, c);
			trainingSamples = Tensor.build(trainingSamples.getName(), axes, wrapImg);
			c = 0;
		}
		
		// TODO check that train and ground truth have the same
		if 
		
	}
	
	private static < T extends RealType< T > & NativeType< T > > 
	 void checkTrainingSamplesTensorDimsForStardist(Tensor<T> trainingSamples) {
		String axes = trainingSamples.getAxesOrderString();
		String stardistAxes = STARDIST_2D_AXES;
		if (axes.length() == 5)
			stardistAxes = STARDIST_3D_AXES;
		else if (axes.length() < 5)
			throw new IllegalArgumentException("Training input tensors should have 4 dimensions ("
					+ STARDIST_2D_AXES + ") or 5 (" + STARDIST_3D_AXES + "), but it has " + axes.length() + " (" + axes + ").");
		
		checkDimOrderAndTranspose(trainingSamples, stardistAxes, "training input");
	}
	
	private static < T extends RealType< T > & NativeType< T > > 
	 void checkGroundTruthTensorDimsForStardist(Tensor<T> gt) {
		String axes = gt.getAxesOrderString();
		String stardistAxes = GROUNDTRUTH_AXES;
		if (axes.length() != GROUNDTRUTH_AXES.length())
			throw new IllegalArgumentException("Ground truth tensors should have 3 dimensions ("
					+ GROUNDTRUTH_AXES + "), but it has " + axes.length() + " (" + axes + ").");
		
		checkDimOrderAndTranspose(gt, stardistAxes, "ground truth");
	}
	
	private static < T extends RealType< T > & NativeType< T > > 
	 void checkDimOrderAndTranspose(Tensor<T> tensor, String stardistAxes, String errMsgObject) {
		for (int c = 0; c < stardistAxes.length(); c ++) {
			String axes = tensor.getAxesOrderString();
			int trueInd = axes.indexOf(stardistAxes.split("")[c]);
			if (trueInd == -1)
				throw new IllegalArgumentException("The " + errMsgObject + " tensors provided should have dimension '"
						+ stardistAxes.split("")[c] + "' in the axes order, but it does not (" + axes + ").");
			else if (trueInd == c) {
				c ++;
				continue;
			}
			IntervalView<T> wrapImg = Views.permute(tensor.getData(), trueInd, c);
			StringBuilder nAxes = new StringBuilder(axes);
			nAxes.setCharAt(c, stardistAxes.charAt(c));
			nAxes.setCharAt(trueInd, axes.charAt(c));
			tensor = Tensor.build(tensor.getName(), nAxes.toString(), wrapImg);
			c = 0;
		}
	}
	
	private < T extends RealType< T > & NativeType< T > > 
	 void setTrainingSamples(Tensor<T> trainingSamples) {
    	if (!(Util.getTypeFromInterval(trainingSamples.getData()) instanceof FloatType)) {
    		this.trainingSamples = Tensor.createCopyOfTensorInWantedDataType(trainingSamples, new FloatType());
    	} else {
    		this.trainingSamples = (Tensor<FloatType>) trainingSamples;
    	}
	}
	
	private < T extends RealType< T > & NativeType< T > > 
	 void setGroundTruth(Tensor<T> groundTruth) {
    	if (!(Util.getTypeFromInterval(groundTruth.getData()) instanceof UnsignedShortType)) {
    		this.groundTruth = Tensor.createCopyOfTensorInWantedDataType(groundTruth, new UnsignedShortType());
    	} else {
    		this.groundTruth = (Tensor<UnsignedShortType>) groundTruth;
    	}
	}
	
	private void findNChannels() throws Exception {
		if (this.downloadStardistPretrained && PRETRAINED_1C_STARDIST_MODELS.contains(this.model)) {
			this.nChannelsModel = 1;
		} else if (this.downloadStardistPretrained && PRETRAINED_3C_STARDIST_MODELS.contains(this.model)) {
			this.nChannelsModel = 3;
		}
		ModelDescriptor descriptor = ModelDescriptor.readFromLocalFile(model, false);
		int cInd = descriptor.getInputTensors().get(0).getAxesOrder().indexOf("c");
		nChannelsModel = descriptor.getInputTensors().get(0).getShape().getPatchMinimumSize()[cInd];
	}
	
	/**
	 * Method that downloads the model selected from the internet,
	 * copies it and unzips it into the models folder
	 * @param downloadURL
	 * 	url of the file to be downloaded
	 * @param targetFile
	 * 	file where the file from the url will be downloaded too
	 */
	public static void downloadFileFromInternet(String downloadURL, File targetFile) {
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			URL website = new URL(downloadURL);
			rbc = Channels.newChannel(website.openStream());
			// Create the new model file as a zip
			fos = new FileOutputStream(targetFile);
			// Send the correct parameters to the progress screen
			FileDownloader downloader = new FileDownloader(rbc, fos);
			downloader.call();
		} catch (IOException e) {
			String msg = "The link for the file: " + targetFile.getName() + " is broken.";
			new IOException(msg, e).printStackTrace();
		} finally {
			try {
				if (fos != null)
						fos.close();
				if (rbc != null)
					rbc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
