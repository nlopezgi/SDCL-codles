package sdcl.ics.uci.edu.lda.modelAggregator;

import java.util.List;

import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.util.ModuleData;
import sdcl.ics.uci.edu.lda.util.TopModuleData;

/**
 * Calculates the sizes of all classes (LOCs) and adds the information to the
 * lightweightTopicModel
 * 
 * @author nlopezgi
 * 
 */
public class ClassSizesCalculator {

	public static void loadClassSizes(String srcRootDir,
			LightweightTopicModel topicModel) throws Exception {
		// Stores the info regarding source files
		ModuleData moduleData = createModuleData(srcRootDir);
		((TopModuleData) moduleData).addTopModuleSourceFolder(srcRootDir);
		List<String> classNamesList = moduleData.getFileNames();
		
		topicModel.classSizes = new int[topicModel.classNames.length];
		for (int i = 0; i < topicModel.classNames.length; i++) {

			String oneClass = topicModel.classNames[i];
			int numLOC = moduleData.getClassNumLOC(oneClass);
			if (numLOC != -1) {
				topicModel.classSizes[i] = numLOC;
			} else {
				System.err.println("COULD NOT FIND NUM LOC FOR CLASS " + oneClass);
			}
		}

	}

	private static ModuleData createModuleData(String srcRootDir) {
		ModuleData ret = new TopModuleData(srcRootDir);

		return ret;
	}
}
