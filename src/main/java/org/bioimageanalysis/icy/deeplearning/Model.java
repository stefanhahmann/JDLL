/**
 * 
 */
package org.bioimageanalysis.icy.deeplearning;

import java.util.List;

import org.bioimageanalysis.icy.deeplearning.exceptions.LoadModelException;
import org.bioimageanalysis.icy.deeplearning.exceptions.RunModelException;
import org.bioimageanalysis.icy.deeplearning.utils.DeepLearningInterface;
import org.bioimageanalysis.icy.deeplearning.utils.EngineLoader;
import org.bioimageanalysis.icy.deeplearning.utils.EngineInfo;

/**
 * Class that manages a Deep Learning model to load it and run it.
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class Model {
	/**
	 * ClassLoader containing all the classes needed to use the corresponding
	 * Deep Learning framework (engine).
	 */
	private EngineLoader engineClassLoader;
	/**
	 * All the information needed to load the engine corresponding to the model
	 * and the model itself.
	 */
	private EngineInfo engineInfo;
	/**
	 * Path to the folder containing the Bioimage.io model
	 */
	private String modelFolder;
	/**
	 * Source file of the Deep Learning model as defined in the 
	 * yaml file
	 */
	private String modelSource;
	
	/**
	 * Construct the object model with all the needed information to
	 * load a model and make inference
	 * @param modelInfo
	 * @throws Exception 
	 */
	private Model(EngineInfo engineInfo, String modelFolder, String modelSource) throws Exception
	{
		this.engineInfo = engineInfo;
		this.modelFolder = modelFolder;
		this.modelSource = modelSource;
		setEngineClassLoader();
	}
	
	/**
	 * Creates a DeepLearning model {@link Model} from the wanted Deep Learning
	 * framework (engine)
	 * @param modelFolder
	 * 	String path to the folder where all the components of the model are stored
	 * @param modelSource
	 * 	String path to the actual model file. In Pytorch is the path to a .pt file
	 * 	and for Tf it is the same as the modelFolder
	 * @param engineInfo
	 * 	all the information needed to load the classes of
	 *  a Deep Learning framework (engine)
	 * @return the Model that is going to be used to make inference
	 * @throws Exception
	 */
	public static Model createDeepLearningModel(String modelFolder, 
												String modelSource, EngineInfo engineInfo) 
														throws Exception
	{
		return new Model(engineInfo, modelFolder, modelSource);
	}
	
	public void setEngineClassLoader() throws Exception {
		this.engineClassLoader = EngineLoader.createEngine(engineInfo.getDeepLearningVersionJarsDirectory());
	}
	
	/**
	 * Load the model wanted to make inference into the particular ClassLoader 
	 * created to run a specific Deep Learning framework (engine) 
	 * @throws LoadModelException if the model was not loaded
	 */
	public void loadModel() throws LoadModelException
	{
		DeepLearningInterface engineInstance = getEngineClassLoader().getEngineInstance();
		engineInstance.loadModel(modelFolder, modelSource);
	}
	
	/**
	 * Close the Deep LEarning model in the ClassLoader where the Deep Learning
	 * framework has been called and instantiated
	 */
	public void closeModel()
	{
		DeepLearningInterface engineInstance = getEngineClassLoader().getEngineInstance();
		engineInstance.closeModel();
		getEngineClassLoader().close();
		engineInstance = null;
		this.engineClassLoader = null;
	}
	
	/**
	 * Method that calls the ClassLoader with the corresponding JARs of the
	 * Deep Learning framework (engine) loaded to run inference on the tensors.
	 * The method returns the corresponding output tensors
	 * 
	 * @param inTensors
	 * 	input tensors containing all the tensor data
	 * @param outTensors
	 * 	output tensors expected containing only the axes order and names. The
	 * 	data will be filled with the outputs of the models
	 * @return the output tensors produced by the model
	 * @throws RunModelException  if the model was not run
	 */
	public List<Tensor>  runModel(List<Tensor> inTensors, List<Tensor> outTensors) throws RunModelException
	{
		DeepLearningInterface engineInstance = getEngineClassLoader().getEngineInstance();
		return engineInstance.run(inTensors, outTensors);
	}
	
	/**
	 * Get the EngineClassLoader created by the DeepLearning Model {@link Model}.
	 * The EngineClassLoader loads the JAR files needed to use the corresponding 
	 * Deep Learning framework (engine)
	 * 
	 * @return the Model corresponding EngineClassLoader
	 */
	public EngineLoader getEngineClassLoader()
	{
		return this.engineClassLoader;
	}

}