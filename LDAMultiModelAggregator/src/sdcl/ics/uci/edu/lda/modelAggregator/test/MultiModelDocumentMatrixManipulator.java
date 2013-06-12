package sdcl.ics.uci.edu.lda.modelAggregator.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.Cluster;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.TopicRef;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.lda.util.ModuleData;
import sdcl.ics.uci.edu.lda.util.TopModuleData;
import cc.mallet.topics.ParallelTopicModel;

/**
 * Class used to test the stability of the clustering algorithm. This class
 * dreates a report detailing the classes included in each cluster (tab
 * separated data to be read using excel). Not currently being maintained
 * 
 * @author nlopezgi
 * 
 */
public class MultiModelDocumentMatrixManipulator {

	static String modelBasePath = ClusteringStabilityTesterModelCreator.modelBasePath;
	static String srcBasePath = MultiModelAggregatorTest.srcBasePath;
	static String[] multiTopicTestsDirs = { "15TopicsTest", "20TopicsTest",
			"25TopicsTest", "30TopicsTest" };
	static int[] numTopics = { 15, 20, 25, 30 };
	public static long[] calicoTestDirectories = MultiModelAggregatorTest.calicoTestDirectories;
	public static final String TAB = "\t";
	public static final double PROB_THRESHOLD_FOR_RELATED_CLASSES = 0.01;

	private String prunedClassesFilePath = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData/LDAModels/topicsOverTimeModels/calico/multipleTestModels/pruned.txt";
	BufferedWriter prunedClassesOut;
	/**
	 * List of probability cubes for each LightweightTopicModel created by
	 * aggregating a set of LDA models
	 */
	List<double[][][]> allModelToTopicToClassProbability;
	List<double[]> allPrunedClassesProbabilities;
	List<int[]> allPrunedClassesCounts;
	/**
	 * List of arrays, each containing the classes for a LightweightTopicModel
	 * created by aggregating a set of LDA models
	 */
	List<String[]> allClassNamesArray;
	/**
	 * number of classes for each LightweightTopicModel created by aggregating a
	 * set of LDA models
	 */
	List<Integer> allNumClasses;
	/**
	 * List of identifiers for each LightweightTopicModel created by aggregating a
	 * set of LDA models
	 */
	List<String> indexIds;
	/**
	 * List of matrices, each containing the probabilities that a class is
	 * associated to a cluster for each LightweightTopicModel created by
	 * aggregating a set of LDA models
	 */
	List<ClusterClassMembership[][]> allClusterMemberships;
	/**
	 * List of LighweightTopicModels each created by aggregating a set of LDA
	 * models
	 */
	List<LightweightTopicModel> allTopicModels;

	public static void main(String[] args) throws Exception {

		MultiModelDocumentMatrixManipulator mmDmm = new MultiModelDocumentMatrixManipulator();
		int calicoVersionNumber = 0;

		mmDmm.allModelToTopicToClassProbability = new ArrayList<double[][][]>();
		mmDmm.allPrunedClassesProbabilities = new ArrayList<double[]>();
		mmDmm.allPrunedClassesCounts = new ArrayList<int[]>();
		mmDmm.allClassNamesArray = new ArrayList<String[]>();
		mmDmm.allNumClasses = new ArrayList<Integer>();
		mmDmm.indexIds = new ArrayList<String>();
		mmDmm.allClusterMemberships = new ArrayList<ClusterClassMembership[][]>();
		mmDmm.allTopicModels = new ArrayList<LightweightTopicModel>();
		int currentListIndex = 0;
		// for (int i = 0; i < multiTopicTestsDirs.length; i++) {
		// for (int j = 0; j < calicoTestDirectories.length; j++) {
		// for (int j = 0; j < ClusteringStabilityTesterModelCreator.NUM_TEST_RUNS;
		// j++) {
		// for (int j = 0; j < 9; j++) {
		for (int j = 0; j < 9; j++) {
			calicoVersionNumber = (int) calicoTestDirectories[0];
			// calicoVersionNumber = calicoTestDirectories[j];
			// String modelDir = modelBasePath + multiTopicTestsDirs[i]
			// + ExperimentDataUtil.SEPARATOR + calicoVersionNumber;
			String modelDir = modelBasePath + j;
			List<ParallelTopicModel> models = new ArrayList<ParallelTopicModel>();

			List<String> modelsInPath = mmDmm.readModelFiles(modelDir);

			for (String modelFile : modelsInPath) {
				models.add(ModelFileUtil.getModel(modelDir, modelFile));
			}
			MultiModelAggregator mma = new MultiModelAggregator();
			System.out.println("aggregating " + models.size() + " models with "
					+ numTopics[1] + " topics");

			String srcPath = srcBasePath + calicoVersionNumber
					+ ExperimentDataUtil.SEPARATOR;
			LightweightTopicModel topicModel = mma.aggregateModels(models,
					numTopics[1], false, true);
			mmDmm.allTopicModels.add(topicModel);
			double[][][] modelToTopicToClassProbability = mmDmm.loadClassVectors(
					models, srcPath, numTopics[1]);
			mmDmm.allModelToTopicToClassProbability
					.add(modelToTopicToClassProbability);
			// mmDmm.loadPrunedClassesProbabilities(modelToTopicToClassProbability,
			// topicModel, mmDmm.allNumClasses.get(currentListIndex));
			String[] classNames = mmDmm.allClassNamesArray.get(currentListIndex);
			mmDmm.allClusterMemberships.add(mmDmm.createClusterClassMembership(
					modelToTopicToClassProbability, classNames, classNames.length,
					topicModel, models.size(), numTopics[1]));

			mmDmm.indexIds.add("n:" + j);
			currentListIndex++;
		}
		// }
		mmDmm.startUpFile(mmDmm.prunedClassesFilePath);
		// mmDmm.createPrunedFilesReport();

		mmDmm.createClusterClassMembershipMatrixReport();

		mmDmm.closeReportFile(mmDmm.prunedClassesOut);
	}

	public void createPrunedFilesReport() throws Exception {
		int numAggregateModels = allModelToTopicToClassProbability.size();
		Map<String, List<String>> classToPruneData = new HashMap<String, List<String>>();
		Map<String, ClassPruneData> classToPruneDataMap = new HashMap<String, ClassPruneData>();
		for (int i = 0; i < numAggregateModels; i++) {
			String[] classNamesArray = allClassNamesArray.get(i);

			double[] prunedClassesProbabilities = allPrunedClassesProbabilities
					.get(i);
			int[] prunedClassesCount = allPrunedClassesCounts.get(i);
			for (int j = 0; j < classNamesArray.length; j++) {
				if (prunedClassesProbabilities[j] > 0) {
					String className = classNamesArray[j];
					ClassPruneData pruneData = null;
					if (!classToPruneData.containsKey(className)) {
						classToPruneData.put(className, new ArrayList<String>());
					}
					if (!classToPruneDataMap.containsKey(className)) {
						pruneData = new ClassPruneData(className);
						classToPruneDataMap.put(className, pruneData);
					} else {
						pruneData = classToPruneDataMap.get(className);
					}
					pruneData.runsIgnored++;
					pruneData.averageProbabilitiesForIgnoredRuns
							.add(prunedClassesProbabilities[j]);
					pruneData.numberOfTopicsForIgnoredRuns.add(prunedClassesCount[j]);
					String oneClassPrunedData = "";
					oneClassPrunedData += prunedClassesProbabilities[j] + TAB
							+ prunedClassesCount[j] + TAB;
					classToPruneData.get(className).add(oneClassPrunedData);
				}
			}
		}
		for (String oneClassName : classToPruneData.keySet()) {
			ClassPruneData pruneData = classToPruneDataMap.get(oneClassName);
			prunedClassesOut.write(oneClassName + TAB);
			prunedClassesOut.write(pruneData.runsIgnored + TAB);
			prunedClassesOut.write(pruneData.getAverageNumTopicsIgnore() + TAB);
			prunedClassesOut.write(pruneData.getAverageOfAveragProbs() + TAB);
			for (String oneStr : classToPruneData.get(oneClassName)) {
				prunedClassesOut.write(oneStr + TAB);
			}
			prunedClassesOut.write(ExperimentDataUtil.EOL);
		}
	}

	public void createClusterClassMembershipMatrixReport() throws Exception {

		Map<String, List<String>> stringsForEachClass = new HashMap<String, List<String>>();
		for (int i = 0; i < allTopicModels.size(); i++) {
			ClusterClassMembership[][] oneMatrix = allClusterMemberships.get(i);
			String[] classesForModel = allClassNamesArray.get(i);
			LightweightTopicModel topicModel = allTopicModels.get(i);
			prunedClassesOut.write(createClusterHeader(topicModel
					.getSelectedClusters()));
			for (int j = 0; j < classesForModel.length; j++) {
				String className = classesForModel[j];
				List<String> stringsForClass = null;
				if (!stringsForEachClass.containsKey(className)) {
					stringsForClass = new ArrayList<String>();
					stringsForEachClass.put(className, stringsForClass);
				} else {
					stringsForClass = stringsForEachClass.get(className);
				}
				ClusterClassMembership[] membershipForClass = oneMatrix[j];
				prunedClassesOut.write(className + TAB);
				String summary = "";
				String onlyProbs = "";
				for (int z = 0; z < topicModel.getSelectedClusters().length + 1; z++) {
					summary += membershipForClass[z] + TAB;
					onlyProbs += membershipForClass[z].averageValue + TAB;
				}
				prunedClassesOut.write(summary);
				prunedClassesOut.write(TAB + TAB + onlyProbs);
				stringsForClass.add(summary);
				prunedClassesOut.write(ExperimentDataUtil.EOL);
			}
		}
		prunedClassesOut.write(ExperimentDataUtil.EOL);
	}

	public String createClusterHeader(Cluster[] clusters) {
		String ret = "" + TAB;
		for (int i = 0; i < clusters.length; i++) {
			ret += "C" + i + "-" + TAB + TAB + TAB;
		}
		ret += "PRUNED topics cluster";
		ret += ExperimentDataUtil.EOL;
		return ret;
	}

	public List<String> readModelFiles(String directoryPath) {
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

	public double[][][] loadClassVectors(final List<ParallelTopicModel> models,
			String srcRootDir, int numTopics) throws Exception {

		// / START UP ALL OBJECTS FOR REPORT
		// Stores the info regarding source files
		ModuleData moduleData = createModuleData(srcRootDir);
		((TopModuleData) moduleData).addTopModuleSourceFolder(srcRootDir);
		List<String> classNamesList = moduleData.getFileNames();
		allNumClasses.add(classNamesList.size());
		String[] classNamesArray = new String[classNamesList.size()];
		int y = 0;
		for (String oneString : classNamesList) {
			classNamesArray[y] = oneString;
			y++;
		}
		allClassNamesArray.add(classNamesArray);
		double[][][] modelToTopicToClassProbability = new double[models.size()][numTopics][classNamesArray.length];
		for (int i = 0; i < models.size(); i++) {
			for (int z = 0; z < classNamesArray.length; z++) {
				double[] probs = models.get(i).getTopicProbabilities(z);
				for (int j = 0; j < numTopics; j++) {
					modelToTopicToClassProbability[i][j][z] = probs[j];
				}
			}
		}
		return modelToTopicToClassProbability;
	}

	public void loadPrunedClassesProbabilities(
			double[][][] modelToTopicToClassProbability, LightweightTopicModel model,
			int numClasses) {
		List<Cluster> singleTopicClusters = new ArrayList<Cluster>();
		Cluster[] clusters = model.getAllClusters();
		for (int i = 0; i < clusters.length; i++) {
			if (clusters[i].getTopics().size() == 1) {
				singleTopicClusters.add(clusters[i]);
			}
		}

		double[] ignoredProbabilities = new double[numClasses];
		int[] topicsThatIgnore = new int[numClasses];
		for (Cluster cluster : singleTopicClusters) {
			int modelNum = cluster.getTopics().get(0).model;
			int topicNum = cluster.getTopics().get(0).topic;
			for (int i = 0; i < numClasses; i++) {
				if (modelToTopicToClassProbability[modelNum][topicNum][i] > PROB_THRESHOLD_FOR_RELATED_CLASSES) {
					ignoredProbabilities[i] += modelToTopicToClassProbability[modelNum][topicNum][i];
					topicsThatIgnore[i]++;
				}
			}
		}
		this.allPrunedClassesCounts.add(topicsThatIgnore);
		this.allPrunedClassesProbabilities.add(ignoredProbabilities);
	}

	private static ModuleData createModuleData(String srcRootDir) {
		ModuleData ret = new TopModuleData(srcRootDir);

		return ret;
	}

	public void startUpFile(String path) throws Exception {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		FileWriter fwriter = new FileWriter(file);
		prunedClassesOut = new BufferedWriter(fwriter);

	}

	public void closeReportFile(BufferedWriter writer) throws Exception {
		writer.close();
	}

	public void addPrunedClassesToReport() {

	}

	/**
	 * Creates a matrix that aggregates the TopicToClass probabilities for all
	 * models aggregated in teh clusters contained in the lightweightTopicModel.
	 * Each row in the matrix corresponds to a class (in the same order as in the
	 * classNames array). Each column corresponds to the membership summary of the
	 * class to a given cluster (ordered in the same way as the cluster array
	 * inside the topicModel). The summary indicates the average membership
	 * (probability of belonging to the cluster, calculated as the average of
	 * probabilities for the class for all topics inside the cluster). THe last
	 * column of the matrix corresponds to the average membership info for a class
	 * corresponding to all other topics that are not part of any cluster.
	 * 
	 * @param modelToTopicToClassProbability
	 * @param classNames
	 * @param numClasses
	 * @param topicModel
	 * @param numModels
	 * @param numTopics
	 * @return
	 */
	public ClusterClassMembership[][] createClusterClassMembership(
			double[][][] modelToTopicToClassProbability, String[] classNames,
			int numClasses, LightweightTopicModel topicModel, int numModels,
			int numTopics) {
		// we will assume all pruned topics correspond to one unlabeled cluster
		// (thus, the +1)
		Cluster[] selectedClusters = topicModel.getSelectedClusters();
		int numClusters = selectedClusters.length + 1;

		ClusterClassMembership[][] membershipMatrix = new ClusterClassMembership[numClasses][numClusters];
		// i is our class name index
		for (int i = 0; i < numClasses; i++) {
			// j is our cluster number index
			for (int j = 0; j < numClusters; j++) {
				Cluster cluster = null;
				if (j < selectedClusters.length) {
					cluster = selectedClusters[j];
				} else {
					// its the last one, corresponds to pruned topics
					calculatePrunedTopicsCluster(topicModel, numModels, numTopics);
					cluster = topicModel.prunedTopicsCluster;

				}

				double totalProb = 0;
				int totalTopics = 0;
				for (TopicRef ref : cluster.getTopics()) {
					if (modelToTopicToClassProbability[ref.model][ref.topic][i] > PROB_THRESHOLD_FOR_RELATED_CLASSES) {
						totalProb += modelToTopicToClassProbability[ref.model][ref.topic][i];
						totalTopics++;
					}
				}
				if (totalTopics > 0) {
					double averageProb = totalProb / totalTopics;
					double sumOfSquDif = 0;
					for (TopicRef ref : cluster.getTopics()) {
						sumOfSquDif += Math
								.pow(
										(modelToTopicToClassProbability[ref.model][ref.topic][i] - averageProb),
										2);
					}
					double stDev = Math.sqrt(sumOfSquDif / totalTopics);
					ClusterClassMembership ccm = new ClusterClassMembership();
					ccm.averageValue = averageProb;
					ccm.standardDev = stDev;
					ccm.percentageOfTopics = (double) totalTopics
							/ (double) cluster.getTopics().size();
					membershipMatrix[i][j] = ccm;
				} else {
					ClusterClassMembership ccm = new ClusterClassMembership();
					ccm.averageValue = 0;
					ccm.standardDev = 0;
					ccm.percentageOfTopics = 0;
					membershipMatrix[i][j] = ccm;
				}
			}
		}
		return membershipMatrix;
	}

	public Cluster calculatePrunedTopicsCluster(LightweightTopicModel topicModel,
			int numModels, int numTopics) {
		if (topicModel.prunedTopicsCluster == null) {
			List<TopicRef> prunedTopics = new ArrayList<TopicRef>();
			for (int i = 0; i < numModels; i++) {
				for (int j = 0; j < numTopics; j++) {
					prunedTopics.add(new TopicRef(i, j, 0));
				}
			}
			Cluster[] selectedClusters = topicModel.getSelectedClusters();
			for (int i = 0; i < selectedClusters.length; i++) {
				prunedTopics.removeAll(selectedClusters[i].getTopics());
			}
			Cluster prunedTopicsCluster = new Cluster();
			prunedTopicsCluster.addAllTopics(prunedTopics);
			topicModel.prunedTopicsCluster = prunedTopicsCluster;
		}
		return topicModel.prunedTopicsCluster;
	}

	/**
	 * Simple class to store values representing the probability that a class is
	 * associated to a given cluster
	 * 
	 * @author nlopezgi
	 * 
	 */
	public static class ClusterClassMembership {

		/**
		 * Average value of document matrix probability for all topics included in
		 * this cluster regarding a specificclass
		 */
		double averageValue;
		/**
		 * Standard deviation for document matrix probs
		 */
		double standardDev;
		/**
		 * the percentage of topics inside this cluster that have a non-zero
		 * probability associated to this class
		 */
		double percentageOfTopics;

		public String toString() {
			return averageValue + TAB + standardDev + TAB + percentageOfTopics;
		}
	}

	private class ClassPruneData {
		final String className;
		int runsIgnored;
		List<Double> averageProbabilitiesForIgnoredRuns;
		List<Integer> numberOfTopicsForIgnoredRuns;

		public ClassPruneData(String className) {
			this.className = className;
			averageProbabilitiesForIgnoredRuns = new ArrayList<Double>();
			numberOfTopicsForIgnoredRuns = new ArrayList<Integer>();
		}

		public double getAverageNumTopicsIgnore() {
			int total = 0;
			for (int i = 0; i < runsIgnored; i++) {
				total += numberOfTopicsForIgnoredRuns.get(i);
			}
			return (double) ((double) total / (double) runsIgnored);
		}

		public double getAverageOfAveragProbs() {
			double total = 0;
			for (int i = 0; i < runsIgnored; i++) {
				total += averageProbabilitiesForIgnoredRuns.get(i)
						/ numberOfTopicsForIgnoredRuns.get(i);
			}
			return total / runsIgnored;
		}
	}

}
