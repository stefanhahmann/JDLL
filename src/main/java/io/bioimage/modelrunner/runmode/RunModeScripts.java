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
package io.bioimage.modelrunner.runmode;

import io.bioimage.modelrunner.bioimageio.download.DownloadModel;

public class RunModeScripts {
	
	protected static final String AXES_KEY = "axes";
	
	protected static final String SHAPE_KEY = "shape";
	
	protected static final String DATA_KEY = "data";
	
	protected static final String NAME_KEY = "name";
	
	protected static final String DTYPE_KEY = "dtype";
	
	protected static final String NP_METHOD = "convertNpIntoDic";
	
	protected static final String XR_METHOD = "convertXrIntoDic";
	
	protected static final String LIST_METHOD = "convertListIntoSupportedList";
	
	protected static final String DICT_METHOD = "convertDicIntoDic";
	
	protected static final String APPOSE_DT_KEY = DownloadModel.addTimeStampToFileName("appose_data_type_", true);
	
	protected static final String TENSOR_KEY = "tensor";
	
	protected static final String NP_ARR_KEY = "np_arr";
	
	/**
	 * Script that contains all the methods neeed to convert python types 
	 * into Appose supported types (primitive types and dics and lists of them)
	 */
	protected static final String TYPE_CONVERSION_METHODS_SCRIPT = ""
			+ "def " + NP_METHOD + "(np_arr):" + System.lineSeparator()
			+ "  return {\"" + DATA_KEY + "\": np_arr.flatten().tolist(), \"" + SHAPE_KEY 
							+ "\": np_arr.shape, \"" + APPOSE_DT_KEY + "\": \"" 
							+ NP_ARR_KEY + "\", \"" + DTYPE_KEY + "\": str(np_arr.dtype)}" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def " + XR_METHOD + "(xr_arr):" + System.lineSeparator()
			+ "  return {\"" + DATA_KEY + "\": xr_arr.values.flatten().tolist(), \"" + SHAPE_KEY 
							+ "\": xr_arr.shape, \"" + AXES_KEY + "\": \"\".join(xr_arr.dims),\"" + NAME_KEY 
							+ "\": xr_arr.name, \"" + APPOSE_DT_KEY + "\": \"" + TENSOR_KEY + "\", "
							+ "\"" + DTYPE_KEY + "\": str(xr_arr.values.dtype)}" 
							+ System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def " + LIST_METHOD + "(list_ob):" + System.lineSeparator()
			+ "  n_list = []" + System.lineSeparator()
			+ "  for value in list_ob:" + System.lineSeparator()
			+ "    if str(type(value)) == \"<class 'xarray.core.dataarray.DataArray'>\":" + System.lineSeparator()
			+ "      n_list.append(" + XR_METHOD + "(value))" + System.lineSeparator()
			+ "    elif str(type(value)) == \"<class 'numpy.ndarray'>\":" + System.lineSeparator()
			+ "      n_list.append(" + NP_METHOD + "(value))" + System.lineSeparator()
			+ "    elif isinstance(value, dict):" + System.lineSeparator()
			+ "      n_list.append(" + DICT_METHOD + "(value))" + System.lineSeparator()
			+ "    elif isinstance(value, list):" + System.lineSeparator()
			+ "      n_list.append(" + LIST_METHOD + "(value))" + System.lineSeparator()
			+ "    else:" + System.lineSeparator()
			+ "      n_list.append(value)" + System.lineSeparator()
			+ "  return n_list" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def " + DICT_METHOD + "(dic):" + System.lineSeparator()
			+ "  n_dic = {}" + System.lineSeparator()
			+ "  for key, value in dic.items():" + System.lineSeparator()
			+ "    if str(type(value)) == \"<class 'xarray.core.dataarray.DataArray'>\":" + System.lineSeparator()
			+ "      n_dic[key] = " + XR_METHOD + "(value)" + System.lineSeparator()
			+ "    elif str(type(value)) == \"<class 'numpy.ndarray'>\":" + System.lineSeparator()
			+ "      n_dic[key] = " + NP_METHOD + "(value)" + System.lineSeparator()
			+ "    elif isinstance(value, dict):" + System.lineSeparator()
			+ "      n_dic[key] = " + DICT_METHOD + "(value)" + System.lineSeparator()
			+ "    elif isinstance(value, list):" + System.lineSeparator()
			+ "      n_dic[key] = " + LIST_METHOD + "(value)" + System.lineSeparator()
			+ "    else:" + System.lineSeparator()
			+ "      n_dic[key] = value" + System.lineSeparator()
			+ "  return n_dic" + System.lineSeparator()
			+ "globals()['" + XR_METHOD + "'] = " + XR_METHOD +  System.lineSeparator()
			+ "globals()['" + NP_METHOD + "'] = " + NP_METHOD +  System.lineSeparator()
			+ "globals()['" + DICT_METHOD + "'] = " + DICT_METHOD +  System.lineSeparator()
			+ "globals()['" + LIST_METHOD + "'] = " + LIST_METHOD +  System.lineSeparator();
}
