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
package io.bioimage.modelrunner.engine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import io.bioimage.modelrunner.bioimageio.description.weights.WeightFormat;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.versionmanagement.AvailableEngines;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;
import io.bioimage.modelrunner.versionmanagement.InstalledEngines;
import io.bioimage.modelrunner.versionmanagement.SupportedVersions;

/**
 * Class to create an object that contains all the information about a Deep
 * Learning framework (engine) that is needed to launch the engine in an
 * independent ClassLoader
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class EngineInfo
{
	/**
	 * Deep Learning framework (engine)., for example Pytorch or Tensorflow
	 */
	private String engine;

	/**
	 * Version of the Deep Learning framework (engine) used to train the model.
	 * This version usually corresponds to the Python API.
	 */
	private String version;

	/**
	 * Version of the Deep Learning framework (engine) of the Java API, which is
	 * going to be used to load the model.
	 */
	private String versionJava;

	/**
	 * True if the engine supports gpu or false otherwise. False by default.
	 */
	private boolean gpu = false;

	/**
	 * True if the engine supports cpu or false otherwise. True by default
	 * because at the moment of development all engines support cpu.
	 */
	private boolean cpu = true;

	/**
	 * Operating system of the machine where the plugin is running
	 */
	private String os;

	/**
	 * Tags to open the model, only needed for Tensorflow
	 */
	private String tfTag;

	private String tfSigDef;

	/**
	 * Directory where the all the jars needed to load a version are stored. It
	 * should be organized in the following way:
	 * <pre>
	 * - jarsDirectory
	 * 	- engineName1_engineJavaVersion1_engineOs1_engineMachine1
	 * 		- engine1Jar1.jar
	 * 		- engine1Jar2.jar
	 * 		- ...
	 * 		- engine1JarN.jar
	 * 	- engineName2_engineJavaVersion2_engineOs2_engineMachine2
	 * 		- engine2Jar1.jar
	 * 		- engine2Jar2.jar
	 * 		- ...
	 * 		- engine2JarN.jar
	 * </pre>
	 */
	private String jarsDirectory;

	/**
	 * If the JARs directory is not going to change during the execution of the
	 * program
	 */
	private static String STATIC_JARS_DIRECTORY;

	/**
	 * Object containing all the supported versions for the selected Deep
	 * Learning framework (engine)
	 */
	private SupportedVersions supportedVersions;

	/**
	 * TODO change by official bioimageio tags? Variable containing the name
	 * used to refer to Tensorflow in the program
	 */
	private static final String TENSORFLOW_ENGINE_NAME = "tensorflow";

	/**
	 * Variable containing the name used to refer to Pytorch in the program
	 */
	private static final String PYTORCH_ENGINE_NAME = "pytorch";

	/**
	 * Variable containing the name used to refer to Onnx in the program
	 */
	private static final String ONNX_ENGINE_NAME = "onnx";

	/**
	 * Variable containing the name used to refer to Keras in the program
	 */
	private static final String KERAS_ENGINE_NAME = "keras";

	/**
	 * Variable containing the name used to refer to Tensorflow in the program
	 */
	private static final String TENSORFLOW_JAVA_BIOIMAGEIO_TAG = "tensorflow_saved_model_bundle";

	/**
	 * Variable containing the name used to refer to Pytorch in the program
	 */
	private static final String PYTORCH_JAVA_BIOIMAGEIO_TAG = "torchscript";

	/**
	 * Variable containing the name used to refer to Pytorch in the program
	 */
	private static final String ONNX_JAVA_BIOIMAGEIO_TAG = "onnx";

	/**
	 * Variable containing the name used to refer to Keras in the program
	 */
	private static final String KERAS_JAVA_BIOIMAGEIO_TAG = "keras_hdf5";

	/**
	 * Variable that stores which version of Tensorflow 1 has been already
	 * loaded to avoid errors for loading two different native libraries in the
	 * same namespace
	 */
	private static String loadedTf1Version;

	/**
	 * Variable that stores which version of Tensorflow 2 has been already
	 * loaded to avoid errors for loading two different native libraries in the
	 * same namespace
	 */
	private static String loadedTf2Version;

	/**
	 * Variable that stores which version of Pytorch has been already loaded to
	 * avoid errors for loading two different native libraries in the same
	 * namespace
	 */
	private static String loadedPytorchVersion;

	/**
	 * Variable that stores which version of Onnx has been already loaded to
	 * avoid errors for loading two different native libraries in the same
	 * namespace
	 */
	private static String loadedOnnxVersion;

	/**
	 * Information needed to know how to launch the corresponding Deep Learning
	 * framework
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param jarsDirectory
	 *            directory where the folder containing the JARs needed to
	 *            launch the corresponding engine are located
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework
	 */
	private EngineInfo( String engine, String version, String jarsDirectory )
	{
		Objects.requireNonNull( engine, "The Deep Learning engine should not be null." );
		Objects.requireNonNull( version, "The Deep Learning engine version should not be null." );
		Objects.requireNonNull( jarsDirectory, "The Jars directory should not be null." );
		setEngine( engine );
		this.version = version;
		checkEngineAreadyLoaded();
		this.jarsDirectory = jarsDirectory;
		this.os = new PlatformDetection().toString();
		setSupportedVersions();
		this.versionJava = findCorrespondingJavaVersion();

	}
	
	/**
	 * Check if the engine has already been loaded or not.
	 * If it is not possible to load the wanted version because another has already been
	 * loaded, an exception is thrown
	 * @throws IllegalArgumentException if an incompatible engine has already been loaded
	 */
	private void checkEngineAreadyLoaded() throws IllegalArgumentException {
		String versionedEngine = this.engine + this.getMajorVersion();
		if (!engine.equals(TENSORFLOW_ENGINE_NAME)  
				&& EngineLoader.getLoadedVersions().get(versionedEngine) != null
				&& !EngineLoader.getLoadedVersions().get(versionedEngine).equals(version))
			throw new IllegalArgumentException("The program will not be able to load "
					+ "'" + engine + " " + version + "' because another version (" 
					+ EngineLoader.getLoadedVersions().get(versionedEngine).equals(version) + ") "
					+ "of the same framework has already been loaded." + System.lineSeparator()
					+ "If loading the wanted version (" + version + ") is strictly necessary "
					+ "please restart the JVM, however, if the previously loaded version "
					+ "(" + EngineLoader.getLoadedVersions().get(versionedEngine).equals(version)
					+ ") can be used please call EngineInfo.defineCompatibleDLEngine(...) "
					+ "to avoid restarting.");
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program
	 * 
	 * If the engine specified is not installed, the method will return null.
	 * The engine of interest needs to be installed first.
	 * A good way to check whether the engine of interest exists or not
	 * is: {@link InstalledEngines#checkEngineWithArgsInstalled(String, String, Boolean, Boolean, Boolean, String)}
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param jarsDirectory
	 *            directory where the folder containing the JARs needed to
	 *            launch the corresponding engine are located
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the wanted version engine is not installed
	 */
	public static EngineInfo defineDLEngine( String engine, String version, String jarsDirectory )
	{	
		if (AvailableEngines.modelRunnerToBioimageioKeysMap().keySet().contains(engine))
			engine = AvailableEngines.modelRunnerToBioimageioKeysMap().get(engine);
		boolean cpu = true;
		boolean gpu = false;
		if ( engine.equals( PYTORCH_JAVA_BIOIMAGEIO_TAG ) )
		{
			cpu = true;
			gpu = true;
		}
		else
		{
			throw new IllegalArgumentException( "Please specify whether the engine can be CPU or not "
					+ "and whether it can use GPU or not. Default values only exist for " + PYTORCH_JAVA_BIOIMAGEIO_TAG
					+ " engines." );
		}
		try {
			return defineDLEngine( engine, version, jarsDirectory, cpu, gpu );
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program
	 * 
	 * If the engine specified is not installed, the method will return null.
	 * The engine of interest needs to be installed first.
	 * A good way to check whether the engine of interest exists or not
	 * is: {@link InstalledEngines#checkEngineWithArgsInstalled(String, String, Boolean, Boolean, Boolean, String)}
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param jarsDirectory
	 *            directory where the folder containing the JARs needed to
	 *            launch the corresponding engine are located
	 * @param gpu
	 *            whether the engine can use GPU or not
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the wanted version engine is not installed
	 */
	public static EngineInfo defineDLEngine( String engine, String version, String jarsDirectory, boolean gpu )
	{
		if (AvailableEngines.modelRunnerToBioimageioKeysMap().keySet().contains(engine))
			engine = AvailableEngines.modelRunnerToBioimageioKeysMap().get(engine);
		boolean cpu = true;
		if ( engine.equals( TENSORFLOW_JAVA_BIOIMAGEIO_TAG )
				|| engine.equals( ONNX_JAVA_BIOIMAGEIO_TAG ))
		{
			cpu = true;
		}
		else if ( engine.equals( PYTORCH_JAVA_BIOIMAGEIO_TAG ) )
		{
			cpu = true;
		}
		else
		{
			throw new IllegalArgumentException( "Please spedicify whether the engine can CPU or not "
					+ "and whether it can use GPU or not. Default values only exist for " + TENSORFLOW_JAVA_BIOIMAGEIO_TAG
					+ " and " + PYTORCH_JAVA_BIOIMAGEIO_TAG + " engines." );
		}
		try {
			return defineDLEngine( engine, version, jarsDirectory, cpu, gpu );
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program.
	 * 
	 * If the engine specified is not installed, the method will return null.
	 * The engine of interest needs to be installed first.
	 * A good way to check whether the engine of interest exists or not
	 * is: {@link InstalledEngines#checkEngineWithArgsInstalled(String, String, Boolean, Boolean, Boolean, String)}
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param jarsDirectory
	 *            directory where the folder containing the JARs needed to
	 *            launch the corresponding engine are located
	 * @param cpu
	 *            whether the engine can use CPU or not
	 * @param gpu
	 *            whether the engine can use GPU or not
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the wanted version engine is not installed
	 */
	public static EngineInfo defineDLEngine( String engine, String version, String jarsDirectory, boolean cpu,
			boolean gpu )
	{
		if (AvailableEngines.modelRunnerToBioimageioKeysMap().keySet().contains(engine))
			engine = AvailableEngines.modelRunnerToBioimageioKeysMap().get(engine);
		boolean rosetta = new PlatformDetection().isUsingRosseta();
		List<DeepLearningVersion> vvs =
				InstalledEngines.checkEngineWithArgsInstalled(engine, version, cpu, gpu, rosetta, jarsDirectory);
		if (vvs.size() == 0)
			return null;
		try {
			EngineInfo engineInfo = new EngineInfo(engine, version, jarsDirectory);
			engineInfo.cpu = cpu;
			engineInfo.gpu = gpu;
			return engineInfo;
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program
	 * 
	 * If the engine specified is not installed, the method will return null.
	 * The engine of interest needs to be installed first.
	 * A good way to check whether the engine of interest exists or not
	 * is: {@link InstalledEngines#checkEngineWithArgsInstalled(String, String, Boolean, Boolean, Boolean, String)}
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the wanted version engine is not installed
	 */
	public static EngineInfo defineDLEngine( String engine, String version )
	{
		if (AvailableEngines.modelRunnerToBioimageioKeysMap().keySet().contains(engine))
			engine = AvailableEngines.modelRunnerToBioimageioKeysMap().get(engine);
		Objects.requireNonNull( STATIC_JARS_DIRECTORY, "The Jars directory should not be null." );
		try {
			return defineDLEngine( engine, version, STATIC_JARS_DIRECTORY );
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program
	 * 
	 * If the engine specified is not installed, the method will return null.
	 * The engine of interest needs to be installed first.
	 * A good way to check whether the engine of interest exists or not
	 * is: {@link InstalledEngines#checkEngineWithArgsInstalled(String, String, Boolean, Boolean, Boolean, String)}
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param gpu
	 *            whether the engine can use GPU or not
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the wanted version engine is not installed
	 */
	public static EngineInfo defineDLEngine( String engine, String version, boolean gpu )
	{
		if (AvailableEngines.modelRunnerToBioimageioKeysMap().keySet().contains(engine))
			engine = AvailableEngines.modelRunnerToBioimageioKeysMap().get(engine);
		Objects.requireNonNull( STATIC_JARS_DIRECTORY, "The Jars directory should not be null." );
		boolean cpu = true;
		if ( engine.equals( TENSORFLOW_JAVA_BIOIMAGEIO_TAG )
				|| engine.equals( ONNX_JAVA_BIOIMAGEIO_TAG ))
		{
			cpu = true;
		}
		else if ( engine.equals( PYTORCH_JAVA_BIOIMAGEIO_TAG ) )
		{
			cpu = true;
		}
		else
		{
			throw new IllegalArgumentException( "Please spedicify whether the engine can CPU or not "
					+ "and whether it can use GPU or not. Default values only exist for " + TENSORFLOW_JAVA_BIOIMAGEIO_TAG
					+ " and " + PYTORCH_JAVA_BIOIMAGEIO_TAG + " engines." );
		}
		try {
			return defineDLEngine( engine, version, STATIC_JARS_DIRECTORY, cpu, gpu );
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program
	 * 
	 * If the engine specified is not installed, the method will return null.
	 * The engine of interest needs to be installed first.
	 * A good way to check whether the engine of interest exists or not
	 * is: {@link InstalledEngines#checkEngineWithArgsInstalled(String, String, Boolean, Boolean, Boolean, String)}
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param cpu
	 *            whether the engine can use CPU or not
	 * @param gpu
	 *            whether the engine can use GPU or not
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the wanted version engine is not installed
	 */
	public static EngineInfo defineDLEngine( String engine, String version, boolean cpu, boolean gpu )
	{
		if (AvailableEngines.modelRunnerToBioimageioKeysMap().keySet().contains(engine))
			engine = AvailableEngines.modelRunnerToBioimageioKeysMap().get(engine);
		Objects.requireNonNull( STATIC_JARS_DIRECTORY, "The Jars directory should not be null." );
		try {
			return defineDLEngine( engine, version, STATIC_JARS_DIRECTORY, cpu, gpu );
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the parameters to launch the wanted Deep Learning framework (engine)
	 * in the program.
	 * In this method, the version defined is orientative to some extent. 
	 * If the version provided as
	 * the argument is not installed, and there is another installed version of the
	 * same framework which has the same major version (for example pytorch 1.13 and pytorch 1.9),
	 * the version installed will be loaded directly instead of requiring the installation
	 * of the original version.
	 * Also, for Pytorch if there is already another engine of the same framework, 
	 * same major version (same as before) but different overall version, 
	 * the previously loaded version will be used. This is because loading different versions
	 * of the Pytorch native libraries produce conflicts.
	 * 
	 *  Regard, that the arguments of this method do not specify GPU or not.
	 *  It will always try first to load an engine version with GPU, but if it is not
	 *  available for the most compatible engine version it will simply use the 
	 *  one with CPU only.
	 *  To know if the EngineInfo object has been created for GPU, call {@link #isGPU()}
	 *  and if it returns false, install the engine for GPU if available.
	 * 
	 * @param engine
	 *            name of the Deep Learning framework (engine). For example:
	 *            Pytorch, Tensorflow....
	 * @param version
	 *            version of the training Deep Learning framework (engine)
	 * @param jarsDirectory
	 *            directory where the folder containing the JARs needed to
	 *            launch the corresponding engine are located
	 * @return an object containing all the information needed to launch a Deep
	 *         learning framework or null if the engine of interest is not installed
	 * @throws IOException if the engines directory does not exist
	 */
	public static EngineInfo defineCompatibleDLEngine( String engine, String version, 
			String jarsDirectory ) throws IOException 
	{
		InstalledEngines manager = InstalledEngines.buildEnginesFinder(jarsDirectory);
		String compatibleVersion = manager.getMostCompatibleVersionForEngine(engine, version);
		if (compatibleVersion == null)
			return null;
		List<DeepLearningVersion> vv = manager.getDownloadedForVersionedEngine(engine, compatibleVersion);
		boolean gpu = vv.stream().filter(v -> v.getGPU()).findFirst().orElse(null) != null;
		return EngineInfo.defineDLEngine(engine, compatibleVersion, true, gpu);
	}
	
	/**
	 * Create an {@link EngineInfo} object from an specific weigth definition of the rdf.yaml file
	 * This method assumes that the directory where the engine folders are downloaded to is 
	 * a directory called "engines" inside the application folder of the main program.
	 * 
	 * The version of the weights does not need to match exactly the version of the
	 * engine installed to enable loading Pytorch 1.11.0 models with Pytorch 1.13.1
	 * 
	 * @param weight
	 * 	the weights of a model for a specific single engine (DL framework)
	 * @return the {@link EngineInfo} object if there are compatible installed engines or null
	 * 	if they do not exist
	 * @throws IOException if the engines directory does not exist
	 */
	public static EngineInfo defineCompatibleDLEngineWithRdfYamlWeights(WeightFormat weight) throws IOException {
		return defineCompatibleDLEngineWithRdfYamlWeights(weight, InstalledEngines.getEnginesDir());
	}
	
	/**
	 * Create an {@link EngineInfo} object from an specific weigth definition of the rdf.yaml file
	 * 
	 * The version of the weights does not need to match exactly the version of the
	 * engine installed to enable loading Pytorch 1.11.0 models with Pytorch 1.13.1
	 * 
	 * @param weight
	 * 	the weights of a model for a specific single engine (DL framework)
	 * @param enginesDir
	 * 	directory where all the engine folders are downloaded
	 * @return the {@link EngineInfo} object if there are compatible installed engines or null
	 * 	if they do not exist
	 * @throws IOException if the engines directory does not exist
	 */
	public static EngineInfo defineCompatibleDLEngineWithRdfYamlWeights(WeightFormat weight, String enginesDir) throws IOException {
		String compatibleVersion = null;
		String engine = weight.getWeightsFormat();
		String version = weight.getTrainingVersion();
		InstalledEngines manager = InstalledEngines.buildEnginesFinder(enginesDir);
		compatibleVersion = manager.getMostCompatibleVersionForEngine(engine, version);
		if (compatibleVersion == null)
			return null;
		List<DeepLearningVersion> vv = manager.getDownloadedForVersionedEngine(engine, compatibleVersion);
		boolean gpu = vv.stream().filter(v -> v.getGPU()).findFirst().orElse(null) != null;
		return EngineInfo.defineDLEngine(engine, compatibleVersion, true, gpu);
	}
	
	/**
	 * Create an {@link EngineInfo} object from an specific weigth definition of the rdf.yaml file
	 * This method assumes that the directory where the engine folders are downloaded to is 
	 * a directory called "engines" inside the application folder of the main program.
	 * 
	 * The version of the weights needs to match exactly the version of the
	 * engine installed. The major and minor versions need to match.
	 * Only Pytorch 1.11 can be used to load Pytorch 1.11
	 * 
	 * @param weight
	 * 	the weights of a model for a specific single engine (DL framework)
	 * @return the {@link EngineInfo} object if there are compatible installed engines or null
	 * 	if they do not exist
	 * @throws IOException if the engines directory does not exist
	 */
	public static EngineInfo defineExactDLEngineWithRdfYamlWeights(WeightFormat weight) throws IOException {
		return defineExactDLEngineWithRdfYamlWeights(weight, InstalledEngines.getEnginesDir());
	}
	
	/**
	 * Create an {@link EngineInfo} object from an specific weigth definition of the rdf.yaml file
	 * 
	 * The version of the weights needs to match exactly the version of the
	 * engine installed. The major and minor versions need to match.
	 * Only Pytorch 1.11 can be used to load Pytorch 1.11
	 * 
	 * @param weight
	 * 	the weights of a model for a specific single engine (DL framework)
	 * @param enginesDir
	 * 	directory where all the engine folders are downloaded
	 * @return the {@link EngineInfo} object if there are compatible installed engines or null
	 * 	if they do not exist
	 * @throws IOException if the engines directory does not exist
	 */
	public static EngineInfo defineExactDLEngineWithRdfYamlWeights(WeightFormat weight, String enginesDir) throws IOException {
		String engine = weight.getWeightsFormat();
		String version = weight.getTrainingVersion();
		InstalledEngines manager = InstalledEngines.buildEnginesFinder(enginesDir);
		if (version == null)
			return null;
		List<DeepLearningVersion> vv = manager.getDownloadedForVersionedEngine(engine, version);
		if (vv.size() == 0)
			return null;
		boolean gpu = vv.stream().filter(v -> v.getGPU()).findFirst().orElse(null) != null;
		try {
			return EngineInfo.defineDLEngine(engine, version, true, gpu);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Retrieve the complete name of the Deep Learning framework (engine)
	 * version. It includes the engine, the Java version, the os and the the
	 * machine. It should be the name of the directory where the needed JARs are
	 * stored.
	 * 
	 * @return a String with all the characteristics of the Deep Learning engine
	 */
	public String getDeepLearningVersionJarsDirectory()
	{
		final String vv = this.engine + "-" + this.version + "-" + this.versionJava + "-" + this.os
					+ ( this.cpu ? "-cpu" : "" ) + ( this.gpu ? "-gpu" : "" );
		return this.jarsDirectory + File.separator + vv;
	}

	/**
	 * Set whether the engine supports GPU or not. Does not support GPU by
	 * default.
	 * 
	 * @param support
	 *            true if it supports GPU and false otherwise
	 */
	public void supportGPU( boolean support )
	{
		gpu = support;
	}

	/**
	 * Set whether the engine supports CPU or not. By default supports CPU
	 * 
	 * @param support
	 *            true if it supports CPU and false otherwise
	 */
	public void supportCPU( boolean support )
	{
		cpu = support;
	}

	/**
	 * Finds the version of Deep Learning framework (engine) equivalent or
	 * compatible with the one used to train the model. This is done because
	 * sometimes APIs for different languages are named differently
	 * 
	 * @return corresponding compatible version of the DL framework Java version
	 */
	public String findCorrespondingJavaVersion()
	{
		try
		{
			return this.supportedVersions.getCorrespondingJavaVersion( this.version );
		}
		catch ( Exception e )
		{
			// TODO Refine exception
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Create the object that contains all the supported versions for the Deep
	 * Learning framework (engine) selected
	 */
	private void setSupportedVersions()
	{
		this.supportedVersions = new SupportedVersions( this.engine );
	}

	/**
	 * REturn the name of the engine (Deep Learning framework)
	 * 
	 * @return the name of the Deep Learning framework
	 */
	public String getEngine()
	{
		return engine;
	}

	/**
	 * Set the Deep Learning framework (engine) of the model
	 * 
	 * @param engine
	 *            Deep Learning framework used for the model
	 */
	public void setEngine( String engine )
	{
		if ( engine.contentEquals( TENSORFLOW_JAVA_BIOIMAGEIO_TAG ) )
			this.engine = TENSORFLOW_ENGINE_NAME;
		else if ( engine.contentEquals( PYTORCH_JAVA_BIOIMAGEIO_TAG ) )
			this.engine = PYTORCH_ENGINE_NAME;
		else if ( engine.contentEquals( ONNX_JAVA_BIOIMAGEIO_TAG ) )
			this.engine = ONNX_ENGINE_NAME;
	}

	/**
	 * Set the directory where the program will look for the Deep Learning
	 * framework jars See {@link #jarsDirectory} for more explanation
	 * 
	 * @param jarsDirectory
	 *            directory where all the folders containing the JARs are stored
	 */
	public void setJarsDirectory( String jarsDirectory )
	{
		this.jarsDirectory = jarsDirectory;
	}

	/**
	 * Return the String path to the directory where all the jars to load a Deep
	 * Learning framework (engine) are stored. See {@link #jarsDirectory} for
	 * more explanation
	 * 
	 * @return String path to the directory where all the jars are stored
	 */
	public String getJarsDirectory()
	{
		return this.jarsDirectory;
	}

	/**
	 * Set the tags needed to load a Tensorflow model. These fields are useless
	 * for other models
	 * 
	 * @param tag
	 *            tad used to open a Tf model
	 * @param sigDef
	 *            signature definition used to open a tf model
	 */
	public void setTags( String tag, String sigDef )
	{
		if ( this.engine.contentEquals( TENSORFLOW_ENGINE_NAME ) )
		{
			this.tfTag = tag;
			this.tfSigDef = sigDef;
		}
	}

	/**
	 * Get Tensorflow Signature Definition to open model
	 * 
	 * @return Tensorflow Signature Definition to open model
	 */
	public String getTfSigDef()
	{
		return this.tfSigDef;
	}

	/**
	 * Get Tensorflow tag to open model
	 * 
	 * @return Tensorflow tag to open model
	 */
	public String getTfTag()
	{
		return this.tfTag;
	}

	/**
	 * Return version of the Deep Learning framework (engine). The version
	 * corresponds to the one used to train the network.
	 * 
	 * @return version of the engine where the model was trained
	 */
	public String getVersion()
	{
		return this.version;
	}

	/**
	 * Return version of the Deep Learning framework (engine). The version
	 * corresponds to the one used to run the model in Java.
	 * 
	 * @return version of the engine where the model was trained
	 */
	public String getJavaVersion()
	{
		return this.versionJava;
	}

	/**
	 * True if the engine allows running on GPU or false otherwise. By default
	 * false
	 * 
	 * @return True if the engine allows running on GPU or false otherwise
	 */
	public boolean isGPU()
	{
		return this.gpu;
	}

	/**
	 * True if the engine allows running on CPU or false otherwise. True by
	 * default
	 * 
	 * @return True if the engine allows running on CPU or false otherwise
	 */
	public boolean isCPU()
	{
		return this.cpu;
	}

	/**
	 * Get the operating system of the machine
	 * 
	 * @return the operation system. If can be either: "windows", "linux",
	 *         "solaris" or "mac"
	 */
	public String getOS()
	{
		return this.os;
	}

	/**
	 * Sets which versions have already been loaed to avoid errors trying to
	 * load another version from the same engine, which always crashes the
	 * application
	 */
	public void setLoadedVersion()
	{
		if ( this.engine.equals( TENSORFLOW_ENGINE_NAME ) && this.version.startsWith( "1" ) )
		{
			loadedTf1Version = this.version;
		}
		else if ( this.engine.equals( TENSORFLOW_ENGINE_NAME ) && this.version.startsWith( "2" ) )
		{
			loadedTf2Version = this.version;
		}
		else if ( this.engine.equals( PYTORCH_ENGINE_NAME ) )
		{
			loadedPytorchVersion = this.version;
		}
		else if ( this.engine.equals( ONNX_ENGINE_NAME ) )
		{
			loadedOnnxVersion = this.version;
		}
	}

	/**
	 * REturns which versions have been already been loaded to avoid errors of
	 * overlapping versions
	 * 
	 * @param engine
	 *            the Deep Learning framework of interest
	 * @param version
	 *            the Deep LEarning version of interest
	 * @return the loaded version of the selected engine or null if no version
	 *         has been loaded
	 * @throws IllegalArgumentException
	 *             if the engine is not supported yet
	 */
	public static String getLoadedVersions( String engine, String version ) throws IllegalArgumentException
	{
		if ( engine.equals( TENSORFLOW_JAVA_BIOIMAGEIO_TAG ) && version.startsWith( "1" ) )
		{
			return loadedTf1Version;
		}
		else if ( engine.equals( TENSORFLOW_JAVA_BIOIMAGEIO_TAG ) && version.startsWith( "2" ) )
		{
			return loadedTf2Version;
		}
		else if ( engine.equals( PYTORCH_JAVA_BIOIMAGEIO_TAG ) )
		{
			return loadedPytorchVersion;
		}
		else if ( engine.equals( ONNX_JAVA_BIOIMAGEIO_TAG ) )
		{
			return loadedOnnxVersion;
		}
		else
		{
			throw new IllegalArgumentException( "The selected engine '" + engine + "' is not supported yet." );
		}
	}

	/**
	 * Get the major version of the Deep Learning framework. This is the first
	 * number of the version until the first dot.
	 * 
	 * @return the major version of the engine
	 */
	public String getMajorVersion()
	{
		int ind = version.indexOf( "." );
		String majorVersion = "" + version;
		if ( ind != -1 )
			majorVersion = version.substring( 0, ind );
		return majorVersion;
	}
	
	/**
	 * Method that checks if the Deep Learning engine specified by the {@link EngineInfo}
	 * object is 
	 * @return true if the engine is installed and false otherwise
	 */
	public boolean isEngineInstalled() {
		File file = new File(this.getDeepLearningVersionJarsDirectory());
		try {
			boolean missingJars = (DeepLearningVersion.fromFile(file).checkMissingJars().size() == 0);
			if (!missingJars)
				return false;
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Find the installed engine that has a version closes to the one defined for this instance.
	 * If this instance has version 1.4 and the only installed engine for the same DL framework
	 * is 1.8, the result will be an engine info object with the same characteristics and different
	 * engine version.
	 * If the original engine was cpu gpu and there are cpu installed but not gpu, a cpu will be returned.
	 * 
	 * If there is an installed engine for the original EngineInfo instance, the same EngineInfo will
	 * be returned
	 * 
	 * @return the most compatible engine with the one defined, if it exists
	 * @throws IOException if no engine of the same DL framework is found
	 */
	public EngineInfo getEngineInfoOfTheClosestInstalledEngineVersion() throws IOException {
		String newV = InstalledEngines.getMostCompatibleVersionForEngine(jarsDirectory, engine, version);
		String msg = "There are no installed engines of the DL framework: "  + engine + version.split("\\.")[0];
		if (newV == null)
			throw new IOException(msg);
		EngineInfo newInfo = EngineInfo.defineDLEngine(engine, newV, jarsDirectory, this.isCPU(), this.isGPU());
		if (!newInfo.isEngineInstalled())
			newInfo.gpu = !this.gpu;
		if (!newInfo.isEngineInstalled())
			throw new IOException(msg);
		return newInfo;
	}

	/**
	 * Set in a static manner the {@link #STATIC_JARS_DIRECTORY} if it is not
	 * going to change during the execution of the program
	 * 
	 * @param jarsDirectory
	 *            the permanent jars directory
	 */
	public static void setStaticJarsDirectory( String jarsDirectory )
	{
		STATIC_JARS_DIRECTORY = jarsDirectory;
	}

	/**
	 * Method that returns the name with which Tensorflow is defined needed at
	 * some points to differentiate between tf1 and tf2
	 * 
	 * @return the String used for tensorflow
	 */
	public static String getTensorflowKey()
	{
		return TENSORFLOW_ENGINE_NAME;
	}

	/**
	 * Method that returns the name with which Pytorch is defined
	 * 
	 * @return the String used for Pytorch
	 */
	public static String getPytorchKey()
	{
		return PYTORCH_ENGINE_NAME;
	}

	/**
	 * Method that returns the name with which Onnx is defined
	 * 
	 * @return the String used for Onnx
	 */
	public static String getOnnxKey()
	{
		return ONNX_ENGINE_NAME;
	}

	/**
	 * Method that returns the name with which Keras is defined 
	 * 
	 * @return the String used for Keras
	 */
	public static String getKerasKey()
	{
		return KERAS_ENGINE_NAME;
	}

	/**
	 * Method that returns the name with which Tensorflow is defined in the
	 * Bioimage.io.
	 * 
	 * @return the String used for tensorflow in the Bioimage.io
	 */
	public static String getBioimageioTfKey()
	{
		return TENSORFLOW_JAVA_BIOIMAGEIO_TAG;
	}

	/**
	 * Method that returns the name with which Pytorch is defined in the
	 * Bioimage.io.
	 * 
	 * @return the String used for Pytorch in the Bioimage.io
	 */
	public static String getBioimageioPytorchKey()
	{
		return PYTORCH_JAVA_BIOIMAGEIO_TAG;
	}

	/**
	 * Method that returns the name with which Onnx is defined in the
	 * Bioimage.io.
	 * 
	 * @return the String used for Onnx in the Bioimage.io
	 */
	public static String getBioimageioOnnxKey()
	{
		return ONNX_JAVA_BIOIMAGEIO_TAG;
	}

	/**
	 * Method that returns the name with which Keras is defined in the
	 * Bioimage.io.
	 * 
	 * @return the String used for Onnx in the Bioimage.io
	 */
	public static String getBioimageioKerasKey()
	{
		return KERAS_JAVA_BIOIMAGEIO_TAG;
	}
}
