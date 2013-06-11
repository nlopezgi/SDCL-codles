package sdcl.ics.uci.edu.lda.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TopModuleData extends ModuleData {

	private List<String> sourceDirsAbsolutePath;

	public TopModuleData(String projectSrcRoot) {
		super(projectSrcRoot);
		sourceDirsAbsolutePath = new ArrayList<String>();
	}

	public void addTopModuleSourceFolder(String srcFolder) {
		sourceDirsAbsolutePath.add(srcFolder);
	}

	@Override
	public List<String> getFileNames() {
		if (fileNames == null) {
			List<String> files = new ArrayList<String>();
			for (String folderData : sourceDirsAbsolutePath) {
				files.addAll(getJavaFilesWithAbsolutePath(folderData, true));
			}
			fileNames = initializeFileNames(files);
		}
		return fileNames;
	}

	private List<String> getJavaFilesWithAbsolutePath(String dir, boolean recursive) {
		List<String> ret = new ArrayList<String>();

		File dirFile = new File(dir);
		readFiles(dirFile, ret, recursive);
		return ret;
	}

}
