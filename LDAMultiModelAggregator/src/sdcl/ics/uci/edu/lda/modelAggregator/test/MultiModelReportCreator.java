package sdcl.ics.uci.edu.lda.modelAggregator.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelAggregator.TopicRef;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.Cluster;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.Topic;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.TopicClass;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.TopicModelForReport;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.lda.util.ModuleData;
import sdcl.ics.uci.edu.lda.util.TopModuleData;
import cc.mallet.topics.ParallelTopicModel;

public class MultiModelReportCreator {
	static String modelBasePath = MultiModelAggregatorTest.modelBasePath;
	static String srcBasePath = MultiModelAggregatorTest.srcBasePath;
	static String[] multiTopicTestsDirs = { "15TopicsTest", "20TopicsTest",
			"25TopicsTest", "30TopicsTest" };
	static int[] numTopics = { 15, 20, 25, 30 };
	public static long[] calicoTestDirectories = MultiModelAggregatorTest.calicoTestDirectories;
	public static final String TAB = "\t";
	public static final double PROB_THRESHOLD_FOR_RELATED_CLASSES = 0.01;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		MultiModelReportCreator mmrc = new MultiModelReportCreator();
		int calicoVersionNumber = 0;
		mmrc.startUpReportFile();
		for (int i = 0; i < multiTopicTestsDirs.length; i++) {
			for (int j = 0; j < calicoTestDirectories.length; j++) {
				calicoVersionNumber = (int) calicoTestDirectories[j];
				String modelDir = modelBasePath + multiTopicTestsDirs[i]
						+ ExperimentDataUtil.SEPARATOR + calicoVersionNumber;
				List<ParallelTopicModel> models = new ArrayList<ParallelTopicModel>();

				List<String> modelsInPath = readModelFiles(modelDir);

				for (String modelFile : modelsInPath) {
					models.add(ModelFileUtil.getModel(modelDir, modelFile));
				}
				MultiModelAggregator mma = new MultiModelAggregator();
				System.out.println("aggregating " + models.size() + " models with "
						+ numTopics[i] + " topics");
				String srcPath = srcBasePath + calicoVersionNumber
						+ ExperimentDataUtil.SEPARATOR;
				LightweightTopicModel topicModel = mma.aggregateModels(models,
						numTopics[i], false, true);
				mmrc.writeToReportFile(topicModel, numTopics[i], calicoVersionNumber,
						models.size(), models, srcPath);

			}
		}
		mmrc.closeReportFile();

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

	private String reportFilePath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/LDAModels/topicsOverTimeModels/calico/multipleTestModels/report.txt";
	private File reportFile;
	BufferedWriter reportOut;

	public void writeToReportFile(LightweightTopicModel model, int numTopics,
			int version, int numModels, List<ParallelTopicModel> models,
			String srcRootDir) throws Exception {

		// / START UP ALL OBJECTS FOR REPORT
		// Stores the info regarding source files
		ModuleData moduleData = createModuleData(srcRootDir);
		((TopModuleData) moduleData).addTopModuleSourceFolder(srcRootDir);

		List<Map<String, double[]>> modelProbabilities = new ArrayList<Map<String, double[]>>();
		for (ParallelTopicModel oneLDAModel : models) {
			modelProbabilities.add(ModelFileUtil.createProbabilityMap(oneLDAModel,
					moduleData.getFileNames()));
		}

		// create the topicModel objects
		List<TopicModelForReport> allTopicModelsForReport = new ArrayList<TopicModelForReport>();
		for (ParallelTopicModel topicModel : models) {
			TopicModelForReport topicModelForRep = new TopicModelForReport(topicModel);
			topicModelForRep.setModule(moduleData);
			allTopicModelsForReport.add(topicModelForRep);

		}
		// get all the text files for the classes
		associateModelWithClasses(moduleData, models, allTopicModelsForReport);

		reportOut.write("CLUSTERING RUN WITH " + numTopics
				+ " topics for version: " + version + ExperimentDataUtil.EOL);
		Map<String, Double> classWeights = calculateClassWeights(moduleData);

		List<Cluster> singleTopicClusters = new ArrayList<Cluster>();
		List<Cluster> smallClusters = new ArrayList<Cluster>();
		List<Cluster> acceptedClusters = new ArrayList<Cluster>();
		Cluster[] clusters = model.getAllClusters();
		for (int i = 0; i < clusters.length; i++) {
			if (clusters[i].getTopics().size() == 1) {
				singleTopicClusters.add(clusters[i]);
			} else if (clusters[i].getTopics().size() < MultiModelAggregator.MIN_CLUSTER_SIZE) {
				smallClusters.add(clusters[i]);
			} else {
				acceptedClusters.add(clusters[i]);
			}
		}

		reportOut
				.write("Single Topic Clusters:" + TAB + singleTopicClusters.size());

		// testClusterCoverageDataMethod(allTopicModelsForReport, moduleData,
		// numTopics);
		Map<String, Double> lowRepresentedClasses = new HashMap<String, Double>();
		Map<String, Integer> lowRepresentedClassHits = new HashMap<String, Integer>();

		List<TopicRef> looseTopics = new ArrayList<TopicRef>();
		for (Cluster cluster : singleTopicClusters) {
			int topicNum = cluster.getTopics().get(0).topic;
			int topicModel = cluster.getTopics().get(0).model;
			looseTopics.addAll(cluster.getTopics());
			List<Topic> topics = new ArrayList<Topic>();
			Topic looseTopic = allTopicModelsForReport.get(topicModel)
					.getTopicByNumber(topicNum);
			topics.add(allTopicModelsForReport.get(topicModel).getTopicByNumber(
					topicNum));
			// calculate the cluster coverage data for these clusters
			getClusterCovergeData(allTopicModelsForReport, cluster, moduleData);
			System.out.println("Coverage for loose topic cluster with topic:"
					+ topicNum + "from model:" + topicModel + ": "
					+ cluster.averageCoverage);
			for (String oneClass : looseTopic.getClassNameStrings()) {
				if (!lowRepresentedClasses.containsKey(oneClass)) {
					lowRepresentedClasses.put(oneClass, new Double(0));
					lowRepresentedClassHits.put(oneClass, new Integer(0));
				}
				lowRepresentedClasses.put(oneClass, lowRepresentedClasses.get(oneClass)
						+ looseTopic.getClassProbability(oneClass));
				lowRepresentedClassHits.put(oneClass,
						lowRepresentedClassHits.get(oneClass) + 1);
			}
		}
		for (String oneClass : lowRepresentedClasses.keySet()) {
			lowRepresentedClasses.put(oneClass, lowRepresentedClasses.get(oneClass)
					/ lowRepresentedClassHits.get(oneClass));
		}
		printMapDouble(lowRepresentedClasses, "LOW REPRESENTED CLASSES");
		printMapInteger(lowRepresentedClassHits, "LOW HITS");

		for (Cluster cluster : smallClusters) {
			List<Topic> topics = new ArrayList<Topic>();
			for (int j = 0; j < cluster.getTopics().size(); j++) {
				int topicNum = cluster.getTopics().get(0).topic;
				int topicModel = cluster.getTopics().get(0).model;
				topics.add(allTopicModelsForReport.get(topicModel).getTopicByNumber(
						topicNum));
			}

			getClusterCovergeData(allTopicModelsForReport, cluster, moduleData);
			System.out.println("Coverage for small topic cluster with"
					+ topics.size() + " topics:" + cluster.averageCoverage);
		}

		for (Cluster cluster : acceptedClusters) {
			List<Topic> topics = new ArrayList<Topic>();
			for (int j = 0; j < cluster.getTopics().size(); j++) {
				int topicNum = cluster.getTopics().get(0).topic;
				int topicModel = cluster.getTopics().get(0).model;
				topics.add(allTopicModelsForReport.get(topicModel).getTopicByNumber(
						topicNum));
			}

			getClusterCovergeData(allTopicModelsForReport, cluster, moduleData);
			System.out.println("Coverage for accepted topic cluster with"
					+ topics.size() + " topics:" + cluster.averageCoverage);
		}

		int[] modelsToLooseTopics = new int[numModels];
		for (TopicRef ref : looseTopics) {
			modelsToLooseTopics[ref.model]++;
		}
		for (int i = 0; i < modelsToLooseTopics.length; i++) {

		}

		// System.out.println("REPORT::: CLUSTERS WITH " + numTopics
		// + " topics for version" + version);
		String divergenceLine = "";
		for (int i = 0; i < model.aggregatedTopicDivergence.length; i++) {
			divergenceLine = divergenceLine + model.aggregatedTopicDivergence[i];
			if (i < model.aggregatedTopicDivergence.length - 1) {
				divergenceLine = divergenceLine + TAB;
			}
		}

		divergenceLine = divergenceLine + ExperimentDataUtil.EOL;
		// System.out.println("REPORT::: divergenceline" + divergenceLine);
		reportOut.write(divergenceLine);

		String originClusterSizeLine = "";
		for (int i = 0; i < model.originClusterSize.length; i++) {
			originClusterSizeLine = originClusterSizeLine
					+ model.originClusterSize[i];
			if (i < model.originClusterSize.length - 1) {
				originClusterSizeLine = originClusterSizeLine + TAB;
			}
		}
		originClusterSizeLine = originClusterSizeLine + ExperimentDataUtil.EOL;
		// System.out.println("REPORT::: clustersizeline" + originClusterSizeLine);
		reportOut.write(originClusterSizeLine);

	}

	public void startUpReportFile() throws Exception {
		reportFile = new File(reportFilePath);
		if (reportFile.exists()) {
			reportFile.delete();
		}
		FileWriter writer = new FileWriter(reportFile);
		reportOut = new BufferedWriter(writer);

	}

	public void closeReportFile() throws Exception {
		reportOut.close();
	}

	public TopicStats[] calculateTopicStats(ParallelTopicModel model) {
		return null;
	}

	public class TopicStats {
		double probabilityByTermWeight;
		double probabilityByClasses;

	}

	/**
	 * Calculates the relative weight of each class according to the number of
	 * LOCs in the class
	 * 
	 * @param moduleData
	 * @return
	 */
	public static Map<String, Double> calculateClassWeights(ModuleData moduleData) {

		List<String> fileNames = moduleData.getFileNames();
		Map<String, Double> ret = new HashMap<String, Double>();
		try {
			for (String oneClass : fileNames) {
				String text = moduleData.getClassText(oneClass);
				ret.put(oneClass, (double) getNumLOC(text));

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	private static int getNumLOC(String text) {
		return text.split(ExperimentDataUtil.EOL).length;
	}

	private ModuleData createModuleData(String srcRootDir) {
		ModuleData ret = new TopModuleData(srcRootDir);
		return ret;
	}

	private void associateModelWithClasses(ModuleData moduleData,
			final List<ParallelTopicModel> models,
			final List<TopicModelForReport> topicModels) {

		List<String> fileNames = moduleData.getFileNames();

		for (int i = 0; i < fileNames.size(); i++) {
			String oneClass = fileNames.get(i);

			String classText = "";
			try {
				classText = moduleData.getClassText(oneClass);
			} catch (Exception e) {

				e.printStackTrace();
			}
			for (int z = 0; z < models.size(); z++) {
				ParallelTopicModel model = models.get(z);
				TopicModelForReport topicModelForReport = topicModels.get(z);

				double[] probs = model.getTopicProbabilities(i);
				for (int j = 0; j < probs.length; j++) {
					// TODO: THIS IS PROBABLY NOT RIGHT, ONLY CERTAIN CLASSES ARE BEING
					// ADDED, BUT THE ONES LOST HAVE VERY LOW PROBS. DO NOT OPERATE OVER
					// CLASSES FOR A TOPIC
					if (probs[j] > PROB_THRESHOLD_FOR_RELATED_CLASSES) {
						TopicClass topicClass = new TopicClass(oneClass, probs[j]);
						topicClass.setClassText(classText);
						topicModelForReport.getTopicByNumber(j).addTopicClass(topicClass);
					}
				}
			}
		}
		for (int z = 0; z < models.size(); z++) {
			TopicModelForReport topicModelForReport = topicModels.get(z);
			for (Topic topic : topicModelForReport.getTopics()) {
				topic.orderTopicClasses();
			}
		}
	}

	/**
	 * Given a list of topics and an model, this method calculates for each class
	 * in the project what percentage is associated to all topics in the list.
	 * Intuitively one would expect a better set of topics to describe a larger
	 * part of the code.
	 * 
	 * @param model
	 *          a model that contains all the topics in the list (but probably
	 *          more than just the ones in the list)
	 * @param topics
	 */
	public static Map<String, Double> calculateModelCoverage(
			TopicModelForReport model, List<Topic> topics) {
		ModuleData moduleData = model.getModule();
		List<String> fileNames = moduleData.getFileNames();
		ParallelTopicModel LDAModel = model.getLDAModel();

		Map<String, double[]> modelProbabilities = ModelFileUtil
				.createProbabilityMap(LDAModel, fileNames);
		// printModelProbabilitiesForClasses(modelProbabilities);

		List<Integer> topicNumbers = new ArrayList<Integer>();
		for (Topic topic : topics) {
			topicNumbers.add(topic.getNumber());
		}
		Collections.sort(topicNumbers);
		Integer[] topicNumbersAr = new Integer[topics.size()];
		topicNumbersAr = topicNumbers.toArray(topicNumbersAr);
		Map<String, Double> ret = new HashMap<String, Double>();
		for (String oneClass : modelProbabilities.keySet()) {
			ret.put(
					oneClass,
					getProbabilitiesForTopics(modelProbabilities.get(oneClass),
							topicNumbersAr, oneClass));
		}
		return ret;
	}

	/**
	 * Receives an array of topic numbers and an array of probabilities (for a
	 * given class or document). This method sums the the probabilities for the
	 * given topics according to the class vector.
	 * 
	 * @param classVector
	 * @param topics
	 * @return
	 */
	private static double getProbabilitiesForTopics(double[] classVector,
			Integer[] topics, String className) {

		double ret = 0;
		for (int i = 0; i < topics.length; i++) {
			ret += classVector[topics[i]];
		}
		// System.out.println("getting probabilities for topics:" + topics
		// + " for class:" + className + ". Result:" + ret);
		return ret;
	}

	/**
	 * Calculates coverage data for clusters containing a single topic from model
	 * 0. Just for testing purposes
	 * 
	 * @param modelsForReport
	 * @param data
	 * @param numTopics
	 */
	private static void testClusterCoverageDataMethod(
			List<TopicModelForReport> modelsForReport, ModuleData data, int numTopics) {
		for (int i = 0; i < numTopics; i++) {
			Cluster cluster = new Cluster();
			cluster.addTopic(new TopicRef(0, i, 0));
			getClusterCovergeData(modelsForReport, cluster, data);
		}

	}

	/**
	 * For a given cluster, this stores the coverage information. For each model
	 * it calculates the relevance (in terms of probability of representing a
	 * certain file) including only the topics in the cluster (which correspond to
	 * a given model). It stores in the cluster two values, one corresponding to
	 * the relevance without considering the size of the file and one considering
	 * its relative size in LOC
	 * 
	 * @param clusters
	 */
	public static void getClusterCovergeData(
			List<TopicModelForReport> modelsForReport, Cluster cluster,
			ModuleData data) {
		System.out.println("CALCULATING COVERAGE FOR CLUSTER WITH "
				+ cluster.getTopics().size() + " topics");
		Map<TopicModelForReport, List<TopicRef>> modelToIncludedTopics = new HashMap<TopicModelForReport, List<TopicRef>>();

		for (TopicRef topic : cluster.getTopics()) {
			TopicModelForReport model = modelsForReport.get(topic.model);
			List<TopicRef> includedTopics;
			if (!modelToIncludedTopics.containsKey(model)) {
				includedTopics = new ArrayList<TopicRef>();
				modelToIncludedTopics.put(model, includedTopics);
			} else {
				includedTopics = modelToIncludedTopics.get(model);
			}
			includedTopics.add(topic);
		}

		double averagePercentage = 0;
		double averagePercentageWeighted = 0;
		for (TopicModelForReport oneModel : modelToIncludedTopics.keySet()) {
			List<Topic> topics = new ArrayList<Topic>();
			for (TopicRef ref : modelToIncludedTopics.get(oneModel)) {
				topics.add(oneModel.getTopicByNumber(ref.topic));
				// System.out.println("Topic has "
				// + oneModel.getTopicByNumber(ref.topic).getClassNameStrings().size()
				// + " classes");
				printList(oneModel.getTopicByNumber(ref.topic).getClassNameStrings());
			}
			// For each class gets the probability of representing it with these
			// topics
			Map<String, Double> modelCoverage = calculateModelCoverage(oneModel,
					topics);
			// gets the weight in terms of LOCs for each class
			Map<String, Double> classWeights = calculateClassWeights(data);

			double coverage = 0;
			double weightedCoverage = 0;
			double maxCoverage = 0;

			for (String oneClass : modelCoverage.keySet()) {

				double oneWeight = classWeights.get(oneClass);
				double oneCoverage = modelCoverage.get(oneClass);

				weightedCoverage += oneWeight * oneCoverage;
				coverage += oneCoverage;

				maxCoverage += oneWeight * 1;
				// System.out.println("coverage for class:"
				// + oneClass.substring(oneClass.lastIndexOf('.'))
				// + ". coverage with these topics " + oneCoverage);
			}
			double percentageCovered = coverage;
			double weightedPercentage = (weightedCoverage / maxCoverage) * 100;
			averagePercentage += percentageCovered;
			averagePercentageWeighted += weightedPercentage;
			// System.out.println("for model/module "
			// + oneModel.getModule().getModuleName()
			// + " the weighted (by LOC) coverage is:" + percentageCovered);
		}
		averagePercentage = averagePercentage / cluster.getTopics().size();

		cluster.averageCoverage = averagePercentage;
		cluster.averageWeightedCoverage = averagePercentageWeighted;
		// System.out.println("AVERAGE COVERAGE:" + averagePercentage);
		// System.out.println("AVERAGE COVERAGE, weighted:" +
		// averagePercentageWeighted);
	}

	private static void printMapDouble(Map<String, Double> map, String name) {
		String out = "";
		for (String key : map.keySet()) {
			out += key + ", " + map.get(key) + ExperimentDataUtil.EOL;
		}
		System.out.print(name + " MAP DATA:" + out);
	}

	private static void printMapInteger(Map<String, Integer> map, String name) {
		String out = "";
		for (String key : map.keySet()) {
			out += key + ", " + map.get(key) + ExperimentDataUtil.EOL;
		}
		System.out.print(name + " MAP DATA:" + out);
	}

	private static void printList(List<String> list) {
		String out = "";
		for (String key : list) {
			out += key + ",";
		}
		System.out.println("List:" + out);
	}

	private static void printModelProbabilitiesForClasses(
			Map<String, double[]> modelProbabilities) {
		String oneLine = "";
		for (String key : modelProbabilities.keySet()) {
			oneLine = key + ":,";
			double[] probs = modelProbabilities.get(key);
			for (int i = 0; i < probs.length; i++) {
				oneLine += probs[i] + ", ";
			}
			System.out.println(oneLine);
		}

	}
}
