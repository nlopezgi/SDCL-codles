package sdcl.ics.uci.edu.lda.modelAggregator;

import java.util.ArrayList;
import java.util.List;

import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.Cluster;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.TopicRef;
import sdcl.ics.uci.edu.lda.util.ModuleData;
import sdcl.ics.uci.edu.lda.util.TopModuleData;
import cc.mallet.topics.ParallelTopicModel;

/**
 * Aggregates a set of document matrices, each from a different LDA model that
 * is clustered using the MultiModelAggregator. The result is stored in the
 * LightweightTopicModel objects
 * 
 * @author nlopezgi
 * 
 */
public class DocumentMatrixAggregator {

	public static final double PROB_THRESHOLD_FOR_RELATED_CLASSES = 0.01;

	int numClasses;
	String[] classNames;

	/**
	 * Main functionality for this class. Aggregates a set of models using the
	 * MultiModelAggregator (term similarity based HAC)
	 * 
	 * @param models
	 * @param numTopics
	 * @param srcRootDir
	 * @return
	 * @throws Exception
	 */
	public LightweightTopicModel aggregateModels(List<ParallelTopicModel> models,
			int numTopics, String srcRootDir) throws Exception {
		// Aggregate the models using the HAC clustering
		MultiModelAggregator mma = new MultiModelAggregator();
		LightweightTopicModel topicModel = mma.aggregateModels(models, numTopics,
				false, true);
		// Load all class-topic vectors into a single large cube
		double[][][] modelToTopicToClassProbability = loadClassVectors(models,
				srcRootDir, numTopics);
		// Create the membership matrix for each cluster found by the
		// MultiModelAggregator. First index is class name, second index is
		// membership to a given cluster
		ClusterClassMembership[][] membership = createClusterClassMembership(
				modelToTopicToClassProbability, classNames, classNames.length,
				topicModel, models.size(), numTopics);
		topicModel.classNames = classNames;

		// Store the info in the LighweightTopicModel
		createAggregateTopicToClassMatrix(topicModel, membership);
		return topicModel;
	}

	private void createAggregateTopicToClassMatrix(
			LightweightTopicModel topicModel, ClusterClassMembership[][] membership) {
		int numTopics = topicModel.getSelectedClusters().length;
		int numClasses = topicModel.classNames.length;
		double[][] topicToClass = new double[numTopics][numClasses];
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < numClasses; j++) {
				// BE careful, the membership matrix is organized by
				// [classes][clusters], the resulting matrix in the
				// LightweightTopicModel is organized by
				// [clusters][classes]
				topicToClass[i][j] = membership[j][i].averageValue;
			}
		}
		topicModel.topicToClasses = topicToClass;
	}

	/**
	 * Loads all document vectors for all the models into a cube (the return) the
	 * first index of the cube is the model number (a consecutive number) the
	 * second index corresponds to the topics and the third to classes. This
	 * method also loads the classNames vector in the process. The last index of
	 * the cube matches to classes stored in the className vector. Please note
	 * that the ParallelTopicModels MUST have been created using the EXACT SAME
	 * files as in the srcRootDir
	 * 
	 * @param models
	 *          set of models created using the same code files
	 * @param srcRootDir
	 *          the root directory for all these source files
	 * @param numTopics
	 * @return
	 * @throws Exception
	 */
	private double[][][] loadClassVectors(final List<ParallelTopicModel> models,
			String srcRootDir, int numTopics) throws Exception {

		// Stores the info regarding source files in a ModuleData object
		ModuleData moduleData = createModuleData(srcRootDir);
		((TopModuleData) moduleData).addTopModuleSourceFolder(srcRootDir);
		// get all the file names
		List<String> classNamesList = moduleData.getFileNames();
		numClasses = classNamesList.size();
		String[] classNamesArray = new String[classNamesList.size()];
		int y = 0;
		for (String oneString : classNamesList) {
			classNamesArray[y] = oneString;
			y++;
		}
		classNames = classNamesArray;
		// for each of the models, load its probability matrix into one row of the
		// cube
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
	private ClusterClassMembership[][] createClusterClassMembership(
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

	/**
	 * Creates a cluster that aggregates all topics that were not included in any
	 * other cluster. These are the 'trash' topics that were not consistently
	 * present in every run of LDA. We just create it for now to not lose any
	 * info, but I still do not know what is the purpose of storing this.
	 * 
	 * @param topicModel
	 * @param numModels
	 * @param numTopics
	 * @return
	 */
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

	private static ModuleData createModuleData(String srcRootDir) {
		ModuleData ret = new TopModuleData(srcRootDir);

		return ret;
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

	}

}
