package sdcl.ics.uci.edu.lda.modelAggregator.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sdcl.ics.uci.edu.lda.modelAggregator.ChangeDataAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.ChangeSetToTopicMapper;
import sdcl.ics.uci.edu.lda.modelAggregator.ClassSizesCalculator;
import sdcl.ics.uci.edu.lda.modelAggregator.DocumentMatrixAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelWriter;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.SerializableModelWriterReader;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.taskToTopics.lda.LDAConfiguration;
import sdcl.ics.uci.edu.taskToTopics.lda.LDAMultiModelGenerator;
import cc.mallet.topics.ParallelTopicModel;

/**
 * Test class for all the functionalities of the MultiModelAggregator
 * components. Please note that most paths for the execution are hardcoded here,
 * so change before running locally. This test runs as it is for the CALICO
 * project.
 * 
 * @author nlopezgi
 * 
 */
public class MultiModelAggregatorTest {

	public static String HOME = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/";

	/**
	 * Test path with source code for a single version of calico
	 */
	static String testPath = HOME
			+ "gitProjectRepos/calicoRepos/CalicoClient.git/1360199283";

	/**
	 * Path with a set of LDA models generated using the same code base (files
	 * produced will be named mallet-lda-XX.model)
	 */
	static String modelPath = HOME
			+ "LDAModels/topicsOverTimeModels/calico/testModels";

	/**
	 * Path with multiple folders, each corresponding to one version of the
	 * codebase, each with several LDA model files (files should be named
	 * mallet-lda-XX.model)(names of folders must match calicoTestDirectories
	 * numbers)F
	 */
	public static String modelBasePath = HOME
			+ "LDAModels/topicsOverTimeModels/calico/multipleTestModels/";

	/**
	 * Base path for all project source files. This folder should contain multiple
	 * folders, one for each version, each folder with the full project at a point
	 * in time. (and versions should correspond to the ones in modelBasePath)
	 * (names of folders must match calicoTestDirectories numbers)
	 */
	public static String srcBasePath = HOME
			+ "gitProjectRepos/calicoRepos/CalicoClient.git/";

	/**
	 * Base path for the calico log file. Used to map changes to
	 * lightweightTopicModel
	 */
	public static String calicoLogPath = HOME
			+ "gitProjectRepos/calicoRepos/logFiles/calicoGitLog.txt";

	/**
	 * Base path to store all resulting files (see MultiModelWriter for output
	 * format)
	 */
	static String aggregateModelOutPath = HOME
			+ "LDAModels/topicsOverTimeModels/aggregateModels/calico/testAggregateModels";

	static int numModels = 10;
	static int numTopics = 15;

	private static final String MODEL_FILE_PREFIX = "testAggregateModel";
	private static final String MODEL_FILE_TYPE = ".ser";// ".csv";
	private static final String DOC_MATRIX_SUFFIX = "-DocMatrix";

	/**
	 * Set of versions for Calico, each corresponds to several directories for
	 * source files and model files.
	 */
	public static long[] calicoTestDirectories = { 1360199283, 1360150771,
			1360144508, 1358571816, 1355821538, 1353488378, 1350519374, 1345875443,
			1345686647, 1342519450, 1340557741, 1337720513, 1335363373, 1331807281,
			1330107479, 1327522575, 1322511283, 1322106721 };

	/**
	 * Main test method that runs the complete functionality of the project
	 */
	public static void main(String[] args) throws Exception {
		// This bit is regarding association of changes in a git log file with the
		// models. If this is not required remove these two lines
		ChangeDataAggregator changeDataAggregator = new ChangeDataAggregator();
		changeDataAggregator.createChangeData(calicoTestDirectories, calicoLogPath);

		int calicoVersionNumber = 0;
		// For each one of the versions of calico
		for (int j = 0; j < calicoTestDirectories.length; j++) {
			calicoVersionNumber = (int) calicoTestDirectories[j];
			List<ParallelTopicModel> models = new ArrayList<ParallelTopicModel>();

			// Try to read model files
			List<String> modelsInPath = readModelFiles(modelBasePath
					+ calicoVersionNumber);
			if (modelsInPath.size() < numModels) {
				// If there are none, create the models (THIS CAN TAKE A COUPLE OF
				// MINUTES, or hours, but models should only need to be created once)

				LDAMultiModelGenerator mmg = new LDAMultiModelGenerator();
				LDAConfiguration conf = new LDAConfiguration(
						ModelFileUtil.MYLYN_DEFINED_ALPHA,
						ModelFileUtil.MYLYN_DEFINED_BETA, numTopics);
				// This create several models using the same configuration
				models = mmg.createMultipleModels(srcBasePath + calicoVersionNumber,
						numModels, conf);

				// and then saves them. If you want them to be recreated, delete them
				// manually
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
				// read the model files
				for (String modelFile : modelsInPath) {
					models.add(ModelFileUtil.getModel(
							modelBasePath + calicoVersionNumber, modelFile));
				}
			}

			// Replace with these lines to remove functionality to produce a document
			// matrix
			// MultiModelAggregator mma = new MultiModelAggregator();
			// LightweightTopicModel topicModel = mma.aggregateModels(models,
			// numTopics,
			// true, false);

			// This code calls the document agreggator directly. This
			// DocumentMatrixAggregator calls the MultiModelAggregator to aggregate
			// all the models created or read. The DocumentMatrixAggregator
			// additionally aggregates all data for the LDA doc matrices
			String srcPath = srcBasePath + calicoVersionNumber
					+ ExperimentDataUtil.SEPARATOR;
			DocumentMatrixAggregator dma = new DocumentMatrixAggregator();
			LightweightTopicModel topicModel = dma.aggregateModels(models, numTopics,
					srcPath);

			// This bit then attempts to associate changes stored in a log file with
			// the models. Before this is called the ChangeDataAggregator should have
			// already read all change data from the log file. REmove these lines if
			// not necesary
			changeDataAggregator.associateModelToChanges(topicModel,
					calicoTestDirectories[j]);

			// Loads the class sizes (LOCs) from the source paths
			ClassSizesCalculator.loadClassSizes(srcPath, topicModel);

			File testTopicModelFileOut = new File(aggregateModelOutPath,
					MODEL_FILE_PREFIX + j + MODEL_FILE_TYPE);

			// create the topic probability vectors for each change set
			ChangeSetToTopicMapper.associateChangeSetsToTopics(topicModel);

			// WRITE THE RESULT TO THE OUTPUT FILE. One file is created for each
			// LightweightTopicModel. This file is later read by the
			// TopicModelComparer project.

			// TODO: Still need to write the ChangeSet objects to the csv file
			String filename = aggregateModelOutPath + ExperimentDataUtil.SEPARATOR
					+ "SERIAL-" + MODEL_FILE_PREFIX + j + MODEL_FILE_TYPE;

			SerializableModelWriterReader.writeToFile(filename, topicModel);

			// Replace with this line to not produce a document matrix
			// MultiModelWriter.writeToFile(topicModel, testTopicModelFileOut, false);
			// MultiModelWriter.writeToFile(topicModel, testTopicModelFileOut, true);
			// File testTopicDocMatrixModelFileOut = new File(aggregateModelOutPath,
			// MODEL_FILE_PREFIX + DOC_MATRIX_SUFFIX + j + MODEL_FILE_TYPE);
			//
			// MultiModelWriter.writeDocumentMatrixToFile(topicModel,
			// testTopicDocMatrixModelFileOut);
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
