package sdcl.ics.uci.edu.lda.modelAggregator.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.taskToTopics.lda.LDAConfiguration;
import sdcl.ics.uci.edu.taskToTopics.lda.LDAMultiModelGenerator;
import cc.mallet.topics.ParallelTopicModel;

public class ClusteringStabilityTesterModelCreator {

	public static int[] calicoTestDirectories = { 1360199283 };
	public static String modelBasePath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/LDAModels/topicsOverTimeModels/calico/multipleTestModels/mutliModelClusteringTest/";
	public static String srcBasePath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/gitProjectRepos/calicoRepos/CalicoClient.git/";
	public static int NUM_TEST_RUNS = 20;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numTopics = 20;
		int numModels = 10;

		int calicoVersionNumber = 0;
		for (int j = 0; j < NUM_TEST_RUNS; j++) {
			System.out.println("creating models run #" + j);
			calicoVersionNumber = calicoTestDirectories[0];
			List<ParallelTopicModel> models = new ArrayList<ParallelTopicModel>();
			// File modelDir = new File(modelPath);
			List<String> modelsInPath = readModelFiles(modelBasePath + j);
			if (modelsInPath.size() < numModels) {
				// create the models
				LDAMultiModelGenerator mmg = new LDAMultiModelGenerator();
				LDAConfiguration conf = new LDAConfiguration(
						ModelFileUtil.MYLYN_DEFINED_ALPHA,
						ModelFileUtil.MYLYN_DEFINED_BETA, numTopics);
				models = mmg.createMultipleModels(srcBasePath + calicoVersionNumber,
						numModels, conf);

				// save them
				int i = 0;
				for (ParallelTopicModel model : models) {
					File baseDir = new File(modelBasePath + j);
					baseDir.mkdirs();
					File file = new File(
							modelBasePath + j + ExperimentDataUtil.SEPARATOR,
							ModelFileUtil.MODEL_FILE_PREFIX + "-" + i
									+ ModelFileUtil.MODEL_FILE_SUFFIX);
					model.write(file);
					i++;
				}
			}
		}

	}

	public static List<String> readModelFiles(String directoryPath) {
		List<String> ret = new ArrayList<String>();
		File modelDir = new File(directoryPath);
		if (modelDir.isDirectory()) {
			File[] listOfFiles = modelDir.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].getName()
						.startsWith(ModelFileUtil.MODEL_FILE_PREFIX)) {
					ret.add(listOfFiles[i].getName());
				}
			}
		}
		return ret;
	}

}
