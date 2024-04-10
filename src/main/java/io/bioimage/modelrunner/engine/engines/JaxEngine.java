package io.bioimage.modelrunner.engine.engines;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveException;

import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.engine.AbstractEngine;
import io.bioimage.modelrunner.model.Model;

public class JaxEngine extends AbstractEngine {
	
	private Mamba mamba;
	
	private String version;
	
	private boolean gpu;
	
	private boolean isPython;
	
	private Boolean installed;
	
	public static final String NAME = "jax";

	private static final List<String> SUPPORTED_JAX_GPU_VERSIONS = Arrays.stream(new String[] {}).collect(Collectors.toList());
	private static final List<String> SUPPORTED_JAX_VERSION_NUMBERS = Arrays.stream(new String[] {}).collect(Collectors.toList());
	
	private JaxEngine(String version, boolean gpu, boolean isPython) {
		if (!isPython) 
			throw new IllegalArgumentException("JDLL only has support for JAX through a Python engine.");
		if (!SUPPORTED_JAX_VERSION_NUMBERS.contains(version))
			throw new IllegalArgumentException("The provided JAX version is not supported by JDLL: " + version
					+ ". The supported versions are: " + SUPPORTED_JAX_VERSION_NUMBERS);
		if (gpu && !SUPPORTED_JAX_GPU_VERSIONS.contains(version))
			throw new IllegalArgumentException("The provided JAX version has no GPU support in JDLL: " + version
					+ ". GPU supported versions are: " + SUPPORTED_JAX_GPU_VERSIONS);
		mamba = new Mamba();
		this.isPython = isPython;
		this.version = version;
	}

	
	public static JaxEngine initialize(String version, boolean gpu, boolean isPython) {
		return new JaxEngine(version, gpu, isPython);
	}
	
	public static List<JaxEngine> getInstalledVersions() {
		return null;
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDir() {
		return mamba.getEnvsDir() + File.separator + toString();
	}


	@Override
	public boolean isPython() {
		return isPython;
	}


	@Override
	public String getVersion() {
		return version;
	}


	@Override
	public boolean supportsGPU() {
		return gpu;
	}


	@Override
	public boolean isInstalled() {
		if (installed != null)
			return installed;
		if (!(new File(getDir()).exists()))
			return false;
		installed = getInstalledVersions().stream()
				.filter(vv -> vv.gpu == gpu && vv.version.equals(version)).findFirst().orElse(null) != null;
		return installed;
	}


	@Override
	public void install() throws IOException, InterruptedException, MambaInstallException, ArchiveException, URISyntaxException {
		if (!mamba.checkMambaInstalled()) mamba.installMicromamba();
		
		mamba.create(getDir(), getSupportedEngineKeys());
		installed = true;
	}


	@Override
	public Model load(String modelFolder, String modelSource) {
		if (!this.isInstalled())
			throw new IllegalArgumentException("Current engine '" + this.toString() 
												+ "' is not installed. Please install it first.");
		return null;
	}
	
	@Override
	public String toString() {
		return NAME + "_" + version + (gpu ? "_gpu" : "");
	}

}
