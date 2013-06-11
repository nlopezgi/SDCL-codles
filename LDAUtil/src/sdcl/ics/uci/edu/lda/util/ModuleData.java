package sdcl.ics.uci.edu.lda.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.mallet.topics.ParallelTopicModel;

public class ModuleData {

	private static final String SRC_TOKEN = "src\\";
	public String moduleName;
	private String projectSrcRoot;
	private List<ModuleFolderData> moduleSrcDirs;
	private ParallelTopicModel model;
	private int numFiles = 0;
	private int modelSize = 0;
	protected List<String> fileNames = null;
	private String nonDefaultFilePath;
	public static final String PACKAGE_INFO_CLASS = "package-info";

	protected Map<String, String> classToPathMap = new HashMap<String, String>();

	public ModuleData(String projectSrcRoot) {
		this.projectSrcRoot = projectSrcRoot;
		moduleSrcDirs = new ArrayList<ModuleFolderData>();
	}

	public String getDefaultFilePath() {
		return ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR
				+ ExperimentDataUtil.SEPARATOR + moduleName + ".model";
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public void addFolder(String folder, boolean recursive) {
		ModuleFolderData oneFolder = new ModuleFolderData();
		oneFolder.folder = folder;
		oneFolder.recursive = recursive;
		moduleSrcDirs.add(oneFolder);
	}

	public List<String> getTexts(List<String> fileNamesRet) {
		List<String> texts = new ArrayList<String>();
		List<String> files = new ArrayList<String>();
		for (ModuleFolderData folderData : moduleSrcDirs) {
			files.addAll(getJavaFiles(folderData.folder, folderData.recursive));
		}
		texts = initializeTexts(files, fileNamesRet);
		numFiles = fileNamesRet.size();
		fileNames = new ArrayList<String>();
		fileNames.addAll(fileNamesRet);
		return texts;
	}

	public List<String> getFileNames() {
		if (fileNames == null) {
			List<String> files = new ArrayList<String>();
			for (ModuleFolderData folderData : moduleSrcDirs) {
				files.addAll(getJavaFiles(folderData.folder, folderData.recursive));
			}
			fileNames = initializeFileNames(files);

		}
		return fileNames;
	}

	private class ModuleFolderData {
		String folder;
		boolean recursive;

	}

	/**
	 * Reads files inside a folder and adds their names to the list
	 */
	public static void readFiles(File folder, List<String> files,
			boolean recursive) {
		if (folder.isDirectory()) {
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()
						&& listOfFiles[i].getAbsolutePath().endsWith(
								ExperimentDataUtil.JAVA_FILE_SUFFIX)) {
					if (!listOfFiles[i].getAbsolutePath().contains(PACKAGE_INFO_CLASS)) {
						String fileName = listOfFiles[i].getAbsolutePath();
						files.add(fileName);
					}
				}
				if (listOfFiles[i].isDirectory() && recursive) {
					readFiles(listOfFiles[i], files, recursive);
				}
			}
		}
	}

	private List<String> getJavaFiles(String dir, boolean recursive) {
		List<String> ret = new ArrayList<String>();

		String dirPath = projectSrcRoot + ExperimentDataUtil.SEPARATOR
				+ dir.replace(".", ExperimentDataUtil.SEPARATOR);
		File dirFile = new File(dirPath);
		readFiles(dirFile, ret, recursive);
		return ret;
	}

	private static List<String> initializeTexts(List<String> allFiles,
			List<String> fileNamesRet) {
		List<String> ret = new ArrayList<String>();
		BufferedReader reader = null;

		try {
			// Read the contents of each file
			for (String oneFile : allFiles) {
				File file = new File(oneFile);

				reader = new BufferedReader(new FileReader(file));

				String line = null;
				StringBuilder stringBuilder = new StringBuilder();
				String ls = ExperimentDataUtil.EOL;

				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append(ls);
				}
				reader.close();
				String data = stringBuilder.toString();

				String name = oneFile.substring(oneFile.indexOf(SRC_TOKEN)
						+ SRC_TOKEN.length());
				name = name.substring(0,
						name.indexOf(ExperimentDataUtil.JAVA_FILE_SUFFIX));
				name = name.replace("\\", ".");
				fileNamesRet.add(name);
				ret.add(data);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	protected List<String> initializeFileNames(List<String> allFiles) {
		List<String> ret = new ArrayList<String>();

		// Read the contents of each file
		for (String oneFile : allFiles) {

			String name = oneFile.substring(oneFile.indexOf(SRC_TOKEN)
					+ SRC_TOKEN.length());
			name = name.substring(0,
					name.indexOf(ExperimentDataUtil.JAVA_FILE_SUFFIX));
			name = name.replace("\\", ".");
			ret.add(name);
			if (!classToPathMap.containsKey(name)) {
				classToPathMap.put(name, oneFile);
			}
		}

		return ret;
	}

	public ParallelTopicModel getLDAModel() {
		return model;
	}

	public void setModel(ParallelTopicModel model) {
		this.model = model;
	}

	public int getNumFiles() {
		return numFiles;
	}

	public int getModelSize() {
		return modelSize;
	}

	public void setModelSize(int modelSize) {
		this.modelSize = modelSize;
	}

	public String getClassText(String className) throws Exception {
		if (classToPathMap.containsKey(className)) {
			BufferedReader reader = null;
			File file = new File(classToPathMap.get(className));

			reader = new BufferedReader(new FileReader(file));

			String line = null;
			StringBuilder stringBuilder = new StringBuilder();
			String ls = ExperimentDataUtil.EOL;

			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			reader.close();
			String data = stringBuilder.toString();
			return data;
		} else {
			return "";
		}
	}

	public int getClassNumLOC(String className) throws Exception {
		if (classToPathMap.containsKey(className)) {
			int numLines = 0;
			BufferedReader reader = null;
			File file = new File(classToPathMap.get(className));

			reader = new BufferedReader(new FileReader(file));
			while ((reader.readLine()) != null) {
				numLines++;
			}
			reader.close();

			return numLines;
		} else {
			return -1;
		}
	}

	public String getNonDefaultFilePath() {
		return nonDefaultFilePath;
	}

	public void setNonDefaultFilePath(String nonDefaultFilePath) {
		this.nonDefaultFilePath = nonDefaultFilePath;
	}
}
