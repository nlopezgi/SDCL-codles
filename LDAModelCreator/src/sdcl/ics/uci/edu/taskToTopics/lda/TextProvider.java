package sdcl.ics.uci.edu.taskToTopics.lda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModuleData;

/**
 * Finds java files in the data dir and gets their content.
 * 
 * @author nlopezgi
 */
public class TextProvider {

	List<String> files = new ArrayList<String>();
	List<String> fileNames = new ArrayList<String>();
	List<String> texts;

	private static final String JAVA_FILE_TYPE = ExperimentDataUtil.JAVA_FILE_SUFFIX;
	private static final String UNKNOWN = "unknown";
	private static final String SRC_TOKEN = "src\\";


	public TextProvider() {
	}

	public List<String> getTexts() {
		if (texts == null) {
			texts = initializeTexts();
			System.out.println("# of texts:" + texts.size());
		}
		return texts;
	}

	public List<String> getTexts(List<File> repoDirs, List<String> fileNamesRet) {
		List<String> allFileNames = new ArrayList<String>();
		for (File oneDir : repoDirs) {
			allFileNames.addAll(getJavaFiles(oneDir.getAbsolutePath()));
		}
		return TextProvider.initializeTexts(allFileNames, fileNamesRet);
	}

	/**
	 * Looks for all java files inside the data dir and returns a list with their
	 * contents (one string per file)
	 */
	public List<String> initializeTexts() {
		List<String> ret = new ArrayList<String>();
		BufferedReader reader = null;
		// Get all the .java files inside the data dir
		files = getJavaFiles();
		try {
			// Read tje cpmtemts pf eacj fo;e
			for (String oneFile : files) {
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
				// Store the file name (trimming everything before the data dir)
				// fileNames.add(oneFile.substring(oneFile.indexOf(Lda.DATA_DIR)
				// + Lda.DATA_DIR.length() + 1));
				String name = oneFile.substring(oneFile.indexOf(SRC_TOKEN)
						+ SRC_TOKEN.length());
				name = name.substring(0, name.indexOf(JAVA_FILE_TYPE));
				name = name.replace("\\", ".");
				fileNames.add(name);
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

	public static List<String> initializeTexts(List<String> allFiles,
			List<String> fileNamesRet) {
		List<String> ret = new ArrayList<String>();
		BufferedReader reader = null;
		// Get all the .java files inside the data dir
		try {
			// Read tje cpmtemts pf eacj fo;e
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
				name = name.substring(0, name.indexOf(JAVA_FILE_TYPE));
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

	/**
	 * Gets the file name for a given text (the id corresponds to its position in
	 * the texts list)
	 */
	public String getNameForTextId(int id) {
		if (id >= 0 && id < fileNames.size()) {
			return fileNames.get(id);
		}
		return UNKNOWN;
	}

	public int getNumTexts() {
		return fileNames.size();
	}

	/**
	 * Gets all names of java files inside the data dir
	 * 
	 * @return
	 */
	private List<String> getJavaFiles() {
		List<String> ret = new ArrayList<String>();
		File dirFile = new File(ExperimentDataUtil.MYLYN_SAMPLE_SRC_DIR);
		readFiles(dirFile, ret);
		return ret;
	}

	private List<String> getJavaFiles(String dir) {
		List<String> ret = new ArrayList<String>();
		File dirFile = new File(dir);
		readFiles(dirFile, ret);
		return ret;
	}

	/**
	 * Recursively reads files inside a folder and adds their names to the list
	 */
	public static void readFiles(File folder, List<String> files) {
//		if (folder.isDirectory()) {
//			File[] listOfFiles = folder.listFiles();
//			for (int i = 0; i < listOfFiles.length; i++) {
//				if (listOfFiles[i].isFile()
//						&& listOfFiles[i].getAbsolutePath().endsWith(JAVA_FILE_TYPE)) {
//					// TODO: THIS IS A CALICO HACK: THERE ARE SOME PACKAGES WITH A DEFAULT
//					// EMPTY CLASS, MUST NOT ADD THIS CLASS TO RET
//					if (!listOfFiles[i].getAbsolutePath().contains(PACKAGE_INFO_CLASS)) {
//						String fileName = listOfFiles[i].getAbsolutePath();
//						files.add(fileName);
//					}
//				}
//				if (listOfFiles[i].isDirectory()) {
//					readFiles(listOfFiles[i], files);
//				}
//			}
//		}
		ModuleData.readFiles(folder, files,true);
	}
}
