package sdcl.ics.uci.edu.lda.modelAggregator.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sdcl.ics.uci.edu.lda.modelAggregator.ChangeDataAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.ClassSizesCalculator;
import sdcl.ics.uci.edu.lda.modelAggregator.DocumentMatrixAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelWriter;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.taskToTopics.lda.LDAConfiguration;
import sdcl.ics.uci.edu.taskToTopics.lda.LDAMultiModelGenerator;
import cc.mallet.topics.ParallelTopicModel;

public class MultiModelAggregatorTest {

	static String testPath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/gitProjectRepos/calicoRepos/CalicoClient.git/1360199283";
	static String modelPath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/LDAModels/topicsOverTimeModels/calico/testModels";

	public static String modelBasePath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/LDAModels/topicsOverTimeModels/calico/multipleTestModels/";
	public static String srcBasePath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/gitProjectRepos/calicoRepos/CalicoClient.git/";
	public static String calicoLogPath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/gitProjectRepos/calicoRepos/logFiles/calicoGitLog.txt";

	static String aggregateModelOutPath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/LDAModels/topicsOverTimeModels/aggregateModels/calico/testAggregateModels";

	static int numModels = 10;
	static int numTopics = 15;

	private static final String MODEL_FILE_PREFIX = "testAggregateModel";
	private static final String MODEL_FILE_TYPE = ".csv";
	private static final String DOC_MATRIX_SUFFIX = "-DocMatrix";

	public static long[] calicoTestDirectories = { 1360199283, 1360150771,
			1360144508, 1358571816, 1355821538, 1353488378, 1350519374, 1345875443,
			1345686647, 1342519450, 1340557741, 1337720513, 1335363373, 1331807281,
			1330107479, 1327522575, 1322511283, 1322106721 };

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ChangeDataAggregator changeDataAggregator = new ChangeDataAggregator();
		changeDataAggregator.createChangeData(calicoTestDirectories, calicoLogPath);
		int calicoVersionNumber = 0;
		for (int j = 0; j < calicoTestDirectories.length; j++) {
			calicoVersionNumber = (int) calicoTestDirectories[j];
			List<ParallelTopicModel> models = new ArrayList<ParallelTopicModel>();
			// File modelDir = new File(modelPath);
			List<String> modelsInPath = readModelFiles(modelBasePath
					+ calicoVersionNumber);
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
					File baseDir = new File(modelBasePath + calicoVersionNumber);
					baseDir.mkdirs();
					File file = new File(modelBasePath + calicoVersionNumber,
							ModelFileUtil.MODEL_FILE_PREFIX + "-" + i
									+ ModelFileUtil.MODEL_FILE_SUFFIX);
					model.write(file);
					i++;
				}
			} else {
				// read them
				for (String modelFile : modelsInPath) {
					models.add(ModelFileUtil.getModel(
							modelBasePath + calicoVersionNumber, modelFile));
				}
			}
			// Replace with these lines to not produce a document matrix
			// MultiModelAggregator mma = new MultiModelAggregator();
			// LightweightTopicModel topicModel = mma.aggregateModels(models,
			// numTopics,
			// true, false);
			String srcPath = srcBasePath + calicoVersionNumber
					+ ExperimentDataUtil.SEPARATOR;
			DocumentMatrixAggregator dma = new DocumentMatrixAggregator();
			LightweightTopicModel topicModel = dma.aggregateModels(models, numTopics,
					srcPath);
			changeDataAggregator.associateModelToChanges(topicModel,
					calicoTestDirectories[j]);
			ClassSizesCalculator.loadClassSizes(srcPath, topicModel);

			File testTopicModelFileOut = new File(aggregateModelOutPath,
					MODEL_FILE_PREFIX + j + MODEL_FILE_TYPE);
			// Replace with this line to not produce a document matrix
			// MultiModelWriter.writeToFile(topicModel, testTopicModelFileOut, false);
			MultiModelWriter.writeToFile(topicModel, testTopicModelFileOut, true);
			File testTopicDocMatrixModelFileOut = new File(aggregateModelOutPath,
					MODEL_FILE_PREFIX + DOC_MATRIX_SUFFIX + j + MODEL_FILE_TYPE);

			MultiModelWriter.writeDocumentMatrixToFile(topicModel,
					testTopicDocMatrixModelFileOut);
		}
	}

	public static void readSingleTestModel() throws Exception {
		List<ParallelTopicModel> models = new ArrayList<ParallelTopicModel>();
		// File modelDir = new File(modelPath);
		List<String> modelsInPath = readModelFiles(modelPath);
		if (modelsInPath.size() < numModels) {
			// create the models
			LDAMultiModelGenerator mmg = new LDAMultiModelGenerator();
			LDAConfiguration conf = new LDAConfiguration(
					ModelFileUtil.MYLYN_DEFINED_ALPHA, ModelFileUtil.MYLYN_DEFINED_BETA,
					numTopics);
			models = mmg.createMultipleModels(testPath, numModels, conf);

			// save them
			int i = 0;
			for (ParallelTopicModel model : models) {
				File file = new File(modelPath, ModelFileUtil.MODEL_FILE_PREFIX + "-"
						+ i + ModelFileUtil.MODEL_FILE_SUFFIX);
				model.write(file);
				i++;
			}
		} else {
			// read them
			for (String modelFile : modelsInPath) {
				models.add(ModelFileUtil.getModel(modelPath, modelFile));
			}
		}
		MultiModelAggregator mma = new MultiModelAggregator();
		LightweightTopicModel topicModel = mma.aggregateModels(models, numTopics,
				true, false);
		File testTopicModelFileOut = new File(aggregateModelOutPath,
				"testAggregateModel.csv");
		MultiModelWriter.writeToFile(topicModel, testTopicModelFileOut, false);
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
