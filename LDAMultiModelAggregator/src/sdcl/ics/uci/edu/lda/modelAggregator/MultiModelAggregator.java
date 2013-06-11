package sdcl.ics.uci.edu.lda.modelAggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.Builder;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.Cluster;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.LightweightTopicModel;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.MultiTopicModel;
import cc.mallet.topics.ParallelTopicModel;

public class MultiModelAggregator {

	public static final int NUM_TERMS = 20;
	public static final int TARGET_CLUSTER_SIZE = 10;
	public static final int MIN_CLUSTER_SIZE = 4;
	private MultiTopicModel multiModel;

	/**
	 * Variable used for the HAC variation that blocks clusters that have over 10
	 * topics from being collapsed with other clusters
	 */
	public static final int MAX_CLUSTER_SIZE_TO_COLLAPSE = 10;

	/**
	 * Creates an aggregate model that clusters all topics in the given models
	 * using Hierarchical agglomerative clustering
	 * 
	 * @param models
	 * @param numTopics
	 * @param useMinClusterSize
	 * @return
	 * @throws Exception
	 */
	public LightweightTopicModel aggregateModels(List<ParallelTopicModel> models,
			int numTopics, boolean useMinClusterSize, boolean storeClusters)
			throws Exception {

		for (ParallelTopicModel model : models) {
			printModelDiagnostics(model, MultiModelAggregator.NUM_TERMS);
		}

		int numModels = models.size();
		// Flatten the term weight info to matrices of ints for ease of operation
		multiModel = createLightweightModels(models, numTopics);

		printModelTest(models.get(0), 20, 0, 0);

		int numTerms = multiModel.terms.length;
		double[][][] modelToTopicToTermProbabilities = convertTermWeightsToProbabilities(
				numModels, numTopics, numTerms, multiModel);
		// Create a big matrix with the distance between each pair of topics
		final double[][] topicToTopicDivergence = createTopicDivergenceMatrix(
				modelToTopicToTermProbabilities, numTopics, numModels, numTerms);

		// now that we have the model to topic to term probs (a cube with the data)
		// and a divergence matrix between these topics, we can start running the
		// clustering
		// algorithm.
		Cluster[] clustersFound = createClustersUsingHAC(topicToTopicDivergence,
				modelToTopicToTermProbabilities, multiModel, numModels, numTopics,
				numTerms);

		LightweightTopicModel aggregateModel = createAggregateModel(clustersFound,
				modelToTopicToTermProbabilities, useMinClusterSize);

		// for (int i = 0; i < aggregateModel.topicToTerm.length; i++) {
		// System.out.println("cluster " + i + " with "
		// + aggregateModel.originClusterSize[i]
		// + " size resulted in an aggregate topic with divergence "
		// + aggregateModel.aggregatedTopicDivergence[i]);
		// printTopicTerms(aggregateModel.topicToTerm[i], multiModel);
		// }
		if (storeClusters) {
			aggregateModel.setClusters(clustersFound);
		}
		return aggregateModel;
	}

	/**
	 * Creates an aggregate model with the info stored in the given clusters
	 * 
	 * @param clustersFound
	 * @param modelToTopicToTermProbabilities
	 * @param useMinClusterSize
	 *          if true then small clusters are removed
	 * @return
	 */
	private LightweightTopicModel createAggregateModel(Cluster[] clustersFound,
			double[][][] modelToTopicToTermProbabilities, boolean useMinClusterSize) {
		// int clustersInAggregateModel = 0;
		// Add to a list (to then sort) all the clusters that have more than
		// MIN_CLUSTER_SIZE topics if the useMinClusterSize var is true, otherwise
		// they all are included
		List<Cluster> clusters = new ArrayList<Cluster>();
		for (int i = 0; i < clustersFound.length; i++) {
			Cluster cluster = clustersFound[i];
			// ignore clusters with less than MIN_CLUSTER_SIZE topic
			if (!useMinClusterSize
					|| (useMinClusterSize && cluster.getTopics().size() > MIN_CLUSTER_SIZE)) {
				clusters.add(cluster);
				double averageDivergenceFromCentroid = calculateAverageDivergenceFromCentroid(
						cluster, multiModel, modelToTopicToTermProbabilities);
				cluster.averageDivergenceFromCentroid = averageDivergenceFromCentroid;
			}
		}

		// Sort the clusters by the size and by the average divergence
		Collections.sort(clusters);

		// create the aggregate lightweight model: a list of aggregate topics each
		// with its topic to term vector (centroid of all topics in a cluster).
		// additionally store the average divergence of topics from the cluster
		// centroid
		LightweightTopicModel aggregateModel = new LightweightTopicModel();
		aggregateModel.terms = multiModel.terms;
		aggregateModel.topicToTerm = new int[clusters.size()][];
		aggregateModel.aggregatedTopicDivergence = new double[clusters.size()];
		aggregateModel.originClusterSize = new int[clusters.size()];
		int indexInAggregateModel = 0;
		for (Cluster cluster : clusters) {
			// ignore clusters with just a single topic
			int[] topicToTermWeights = generateClusterCentroidWeightVector(cluster,
					multiModel);
			aggregateModel.topicToTerm[indexInAggregateModel] = topicToTermWeights;

			aggregateModel.aggregatedTopicDivergence[indexInAggregateModel] = cluster.averageDivergenceFromCentroid;

			aggregateModel.originClusterSize[indexInAggregateModel] = cluster
					.getTopics().size();
			indexInAggregateModel++;
		}

		return aggregateModel;
	}

	/**
	 * Creates a list of lightweight topic models using the given
	 * ParallelTopicModels. These lightweight models only contain a topic to word
	 * matrix for clustering purposes
	 * 
	 * @param models
	 * @return
	 */
	private MultiTopicModel createLightweightModels(
			List<ParallelTopicModel> models, int numTopics) throws Exception {
		Builder builder = new Builder();

		return builder.buildModel(models, numTopics);
	}

	private double[][][] convertTermWeightsToProbabilities(int numModels,
			int numTopics, int numTerms, MultiTopicModel multiTopicModel) {
		int[][][] modelToTopicToTerm = multiTopicModel.modelToTopicToTerm;
		double[][][] ret = new double[numModels][numTopics][numTerms];

		for (int model = 0; model < numModels; model++) {
			for (int topic = 0; topic < numTopics; topic++) {
				int totalWeight = 0;
				for (int term = 0; term < numTerms; term++) {
					if (modelToTopicToTerm[model][topic][term] != 0) {
						totalWeight += modelToTopicToTerm[model][topic][term];
					}
				}
				for (int term = 0; term < numTerms; term++) {
					if (modelToTopicToTerm[model][topic][term] != 0) {
						ret[model][topic][term] = ((double) modelToTopicToTerm[model][topic][term])
								/ (double) totalWeight;
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Creates a matrix of distances between the topics. Items in matrix are
	 * ordered according to model number and topic number (i.e. pos 0 is model0,
	 * topic0, etc.). That is, for (model 'x', topic 'y') use position
	 * (x*numTopics + y). The diagonal is filled with -1.
	 * 
	 * @return
	 */
	private double[][] createTopicDivergenceMatrix(
			double[][][] modelToTopicToProbability, int numTopics, int numModels,
			int numTerms) {
		double[][] topicToTopicDivergence = new double[numTopics * numModels][numTopics
				* numModels];

		for (int model = 0; model < numModels; model++) {
			for (int topic = 0; topic < numTopics; topic++) {

				for (int model2 = 0; model2 < numModels; model2++) {
					for (int topic2 = 0; topic2 < numTopics; topic2++) {
						double divergence = -1;
						if (!(model == model2 && topic == topic2)) {
							divergence = KLDivergenceCalculator
									.getKLDivergenceVectorSpaceDistance(
											modelToTopicToProbability[model][topic],
											modelToTopicToProbability[model2][topic2], numTerms);
						}
						topicToTopicDivergence[model * numTopics + topic][model2
								* numTopics + topic2] = divergence;
					}
				}
			}
		}
		return topicToTopicDivergence;
	}

	/**
	 * Test method, simply prints out to sysout the terms in a topic
	 * 
	 * @param model
	 * @param topic
	 */
	private static void printTopicTerms(int model, int topic,
			MultiTopicModel multiModel) {
		int[] termVector = multiModel.modelToTopicToTerm[model][topic];
		String terms = "model." + model + ".topic." + topic + ":";
		for (int i = 0; i < termVector.length; i++) {
			if (termVector[i] != 0) {
				terms = terms + multiModel.terms[i] + "|";
			}
		}
		System.out.println(terms);
	}

	/**
	 * Prints terms for a topic (given a vector of term weights and the multiModel
	 * with the alphabet of terms) SORTED by relevance descending
	 * 
	 * @param termVector
	 * @param multiModel
	 */
	private static void printTopicTerms(int[] termVector,
			MultiTopicModel multiModel) {
		Map<Integer, String> termToWeight = new HashMap<Integer, String>();

		String terms = ":";
		for (int i = 0; i < termVector.length; i++) {
			if (termVector[i] != 0) {
				int weight = termVector[i];
				while (termToWeight.containsKey(weight)) {
					weight++;
				}
				termToWeight.put(weight, multiModel.terms[i]);
			}
		}

		List<Integer> sortedTermWeights = new ArrayList<Integer>(
				termToWeight.keySet());
		Collections.sort(sortedTermWeights);
		Collections.reverse(sortedTermWeights);
		for (Integer oneWeigth : sortedTermWeights) {
			terms = terms + termToWeight.get(oneWeigth) + "|";
		}
		System.out.println(terms);
	}

	/**
	 * Creates clusters using the models stored in the cube using Hierarchical
	 * agglomerative clustering. On each iteration the algorithm finds the pair of
	 * closest topics and clusters them together. Topics are not clustered into
	 * clusters that already contain MAX_CLUSTER_SIZE_TO_COLLAPSE topics. Each
	 * time a topic is clustered its centroid is calculated and the divergence
	 * matrix (for the new cluster) is updated
	 * 
	 * @param topicToTopicDivergence
	 *          the divergence matrix between topics
	 * @param modelToTopicToTermProbabilities
	 *          the cube with models, topics and their terms
	 * @param multiModel
	 *          the original lightweight models
	 * @param numModels
	 *          the number of models
	 * @param numTopics
	 *          the number of topics per model
	 * @param numTerms
	 *          the number of terms in the cube
	 * @return a set of clusters grouping similar topics
	 */
	private Cluster[] createClustersUsingHAC(
			final double[][] topicToTopicDivergence,
			final double[][][] modelToTopicToTermProbabilities,
			final MultiTopicModel multiModel, int numModels, int numTopics,
			int numTerms) {

		// initial matrix size is [numModels * numTopics]x[numModels * numTopics]
		int initialMatrixSize = numModels * numTopics;

		// create the initial clusters with a single topic
		// create the cluster vector organized in the same order as the divergence
		// matrix
		// create the clusterCentroidTermProbabilityVectors objects with the initial
		// clusters
		Cluster[] clustersOrderedByDivergenceMatrixIndex = new Cluster[initialMatrixSize];
		double[][] clusterCentroidTermProbabilityVectors = new double[initialMatrixSize][];

		int model = 0;
		int topic = 0;
		for (int i = 0; i < initialMatrixSize; i++) {
			TopicRef topicRef = new TopicRef(model, topic, i);
			Cluster cluster = new Cluster();
			cluster.addTopic(topicRef);
			clustersOrderedByDivergenceMatrixIndex[i] = cluster;
			clusterCentroidTermProbabilityVectors[i] = generateClusterCentroidProbabilityVector(
					cluster, multiModel, modelToTopicToTermProbabilities);
			topic++;
			if (topic == numTopics) {
				model++;
				topic = 0;
			}
		}
		double[][] hacInitialDivergenceMatrix = topicToTopicDivergence;
		// run iterations of the HAC.
		// iterate again, only if the divergence between the collapsed clusters is
		// below a given threshold
		double divergenceForIteration = 0;
		double threshold = 300; // To be defined properly
		// Integer numClustersFound = 0;
		HACIterationObject hacIterationObject = new HACIterationObject();
		hacIterationObject.divergenceMatrix = hacInitialDivergenceMatrix;
		hacIterationObject.numClustersFound = initialMatrixSize;
		hacIterationObject.matrixSize = initialMatrixSize;
		while (divergenceForIteration < threshold) {
			divergenceForIteration = hierarchicalAgglomerativeClusteringIteration(
					numTerms, hacIterationObject, clustersOrderedByDivergenceMatrixIndex,
					clusterCentroidTermProbabilityVectors,
					modelToTopicToTermProbabilities, multiModel);
		}
		Cluster[] ret = new Cluster[hacIterationObject.numClustersFound];
		for (int i = 0; i < hacIterationObject.numClustersFound; i++) {
			ret[i] = clustersOrderedByDivergenceMatrixIndex[i];
		}
		return ret;

	}

	/**
	 * Runs one iteration of the HAC algorithm. The iteration finds the two
	 * closest clusters and collapses them into a single one. It then updates the
	 * divergenceMatrix, the clustersOrderedByDivergenceMatrixIndex and the
	 * clusterCentroidTermVectors (i.e. it removes all references to the collapsed
	 * clusters, and includes new references to the new cluster created by
	 * collapsing the two closest clusters)
	 * 
	 * @param matrixSize
	 *          . The size (its a square matrix) of the divergenceMatrix (i.e.,
	 *          the number of clusters before starting this iteration)
	 * @param numTerms
	 *          . The number of terms referenced in each probability vector
	 * @param divergenceMatrix
	 *          . 2x2 matrix. Each cell stores the value of the KL divergence
	 *          between two clusters. The indexes correspond to the ones stored in
	 *          the clustersOrderedByDivergenceMatrixIndex
	 * @param clustersOrderedByDivergenceMatrixIndex
	 *          . List of clusters referenced in by the divergenceMatrix
	 * @param clusterCentroidTermProbabilityVectors
	 *          . List of vectors, each corresponding to the term probability
	 *          vector for one cluster, the index of clusters corresponding to the
	 *          ones stored in the clustersOrderedByDivergenceMatrixIndex
	 * @param modelToTopicToTermProbabilities
	 *          the model to term probabilities (not modified in this method)
	 * @param multiModel
	 *          the multiTopicModel storing the model,topic x term weights
	 * @return the divergence of the two collapsed clusters
	 */
	private static double hierarchicalAgglomerativeClusteringIteration(
			final int numTerms, HACIterationObject hacIterationObject,
			Cluster[] clustersOrderedByDivergenceMatrixIndex,
			double[][] clusterCentroidTermProbabilityVectors,
			final double[][][] modelToTopicToTermProbabilities,
			final MultiTopicModel multiModel) {
		double[][] divergenceMatrix = hacIterationObject.divergenceMatrix;
		int matrixSize = hacIterationObject.matrixSize;
		int numClusters = hacIterationObject.numClustersFound;

		// ------------------
		// Find the pair of closest related clusters in the matrix and create a
		ReferencePair refPair = findSuitablePairToCollapse(matrixSize,
				divergenceMatrix, clustersOrderedByDivergenceMatrixIndex, numClusters);

		int closestIndexI = refPair.ref1;
		int closestIndexJ = refPair.ref2;
		double closest = refPair.divergence;

		// create a new cluster with the pair of existing clusters
		Cluster clusterOne = clustersOrderedByDivergenceMatrixIndex[closestIndexI];
		Cluster clusterTwo = clustersOrderedByDivergenceMatrixIndex[closestIndexJ];
		int toColapseClusterOneIndex = closestIndexI;
		int toColapseClusterTwoIndex = closestIndexJ;
		double divergenceBetweenCollapsedClusters = closest;

		Cluster newCluster = new Cluster();
		newCluster.addAllTopics(clusterOne.getTopics());
		newCluster.addAllTopics(clusterTwo.getTopics());
		newCluster.addCollapsedCluster(clusterOne);
		newCluster.addCollapsedCluster(clusterTwo);

		// --- test code
		// System.out.println("Collapsing two clusters into one with divergence:"
		// + divergenceBetweenCollapsedClusters);
		// System.out.println("cluster " + toColapseClusterOneIndex + ":");
		// for (TopicRef topRefP : clusterOne.getTopics()) {
		// printTopicTerms(topRefP.model, topRefP.topic, multiModel);
		// }
		// System.out.println("cluster " + toColapseClusterTwoIndex + ":");
		// for (TopicRef topRefP : clusterTwo.getTopics()) {
		// printTopicTerms(topRefP.model, topRefP.topic, multiModel);
		// }
		// /--- test code

		// ------------------
		// calculate the divergence of this new cluster with all other existing
		// clusters
		double[] newClusterCentroid = generateClusterCentroidProbabilityVector(
				newCluster, multiModel, modelToTopicToTermProbabilities);

		double[] newClusterToOtherClusterDivergences = new double[matrixSize - 2];
		int newClusterToOtherClusterDivergencesIndex = 0;
		for (int i = 0; i < numClusters; i++) {
			if (i != toColapseClusterOneIndex && i != toColapseClusterTwoIndex) {
				// calculate its divergence to the new cluster if its not one of the
				// collapsed ones
				double clusterCentroid[] = clusterCentroidTermProbabilityVectors[i];
				double divergence = KLDivergenceCalculator
						.getKLDivergenceVectorSpaceDistance(newClusterCentroid,
								clusterCentroid, numTerms);
				newClusterToOtherClusterDivergences[newClusterToOtherClusterDivergencesIndex] = divergence;
				newClusterToOtherClusterDivergencesIndex++;
			}
		}
		// the divergence to the new cluster is left in the
		// newClusterToOtherClusterDivergences with clusters in the same order as
		// they were in the original matrix (considering the two clusters that were
		// collapsed are not included in this vector)

		// ------------------
		// re-create the divergence matrix
		double[][] newDivergenceMatrix = new double[matrixSize - 1][matrixSize - 1];
		int newMatrixIndexI = 0;
		int newMatrixIndexJ = 0;
		// first move existing data for divergence between clusters that have not
		// changed
		for (int i = 0; i < matrixSize; i++) {
			// if this row is not referring to any of the two collapsed clusters
			if (i != toColapseClusterOneIndex && i != toColapseClusterTwoIndex) {
				newMatrixIndexJ = 0;
				for (int j = 0; j < matrixSize; j++) {
					// if this column is not referring to any of the two collapsed
					// clusters
					if (j != toColapseClusterOneIndex && j != toColapseClusterTwoIndex) {
						// then move the value to the new matrix
						// System.out.println("nm["+newMatrixIndexI+"]["+newMatrixIndexJ+"]=omm["+i+"]["+j+"]");
						newDivergenceMatrix[newMatrixIndexI][newMatrixIndexJ] = divergenceMatrix[i][j];
						newMatrixIndexJ++;
					}
				}
				newMatrixIndexI++;
			}
		}

		// now move the values of the divergence from the new cluster to all
		// existing clusters (newClusterToOtherClusterDivergences) to the new matrix
		// (newDivergenceMatrix)

		// First move values to the last column of the new matrix
		int j = matrixSize - 2; // newDivergenceMatrix is [matrixSize-1 x
														// matrixSize-1], the last column is -2
		for (int i = 0; i < matrixSize - 2; i++) {
			newDivergenceMatrix[i][j] = newClusterToOtherClusterDivergences[i];
		}

		// then move values to the last row of the new matrix
		int i = matrixSize - 2;
		for (j = 0; j < matrixSize - 2; j++) {
			newDivergenceMatrix[i][j] = newClusterToOtherClusterDivergences[j];
		}
		// now set -1 on the bottom right corner cell of the matrix
		newDivergenceMatrix[matrixSize - 2][matrixSize - 2] = -1;

		// ------------------
		// re-create the clustersOrederedByMatrixIndex vector
		// remove the two topics that were collapsed and add the new one at the end
		// (toColapseClusterOneIndex and toColapseClusterTwoIndex)
		int swapSpaces = 0;
		for (i = 0; i < matrixSize; i++) {
			if (i == toColapseClusterOneIndex || i == toColapseClusterTwoIndex) {
				// these have to be removed, just increase the swapSpaces var
				swapSpaces++;
			} else {
				// move the cluster to the left if swapSpaces>0
				if (swapSpaces > 0) {
					clustersOrderedByDivergenceMatrixIndex[i - swapSpaces] = clustersOrderedByDivergenceMatrixIndex[i];
				}
			}
		}
		clustersOrderedByDivergenceMatrixIndex[matrixSize - 2] = newCluster;

		// update the clusterCentroidTermVectors object, removing the ones for the
		// collapsed clusters and adding the ones for the new cluster

		swapSpaces = 0;
		for (i = 0; i < matrixSize; i++) {
			if (i == toColapseClusterOneIndex || i == toColapseClusterTwoIndex) {
				// these have to be removed, just increase the swapSpaces var
				swapSpaces++;
			} else {
				// move the cluster to the left if swapSpaces>0
				if (swapSpaces > 0) {
					clusterCentroidTermProbabilityVectors[i - swapSpaces] = clusterCentroidTermProbabilityVectors[i];
					// clustersOrderedByDivergenceMatrixIndex[i - swapSpaces] =
					// clustersOrderedByDivergenceMatrixIndex[i];
				}
			}
		}
		clusterCentroidTermProbabilityVectors[matrixSize - 2] = newClusterCentroid;

		// ------------
		// make sure all the new matrices and vectors are now referenced by the
		// objects passed as parameters to this method, so that the next iteration
		// can start smoothly right after

		hacIterationObject.divergenceMatrix = newDivergenceMatrix;
		hacIterationObject.numClustersFound = matrixSize - 1;
		hacIterationObject.matrixSize = matrixSize - 1;
		// System.out.println("Ending HAC iteration: clusters so far:"
		// + hacIterationObject.numClustersFound
		// + ". similarity for new cluster this iteration:"
		// + divergenceBetweenCollapsedClusters);
		// ------------------
		// return the divergence value between the two collapsed clusters
		return divergenceBetweenCollapsedClusters;
	}

	/**
	 * Creates objects that reference topics to their index in the big divergence
	 * matrix
	 * 
	 * @param numModels
	 * @param numTopics
	 * @return
	 */
	private TopicRef[][] createTopicReferenceObjects(int numModels, int numTopics) {
		TopicRef[][] topicReferenceObject = new TopicRef[numModels][numTopics];
		for (int model = 0; model < numModels; model++) {
			for (int topic = 0; topic < numTopics; topic++) {

				topicReferenceObject[model][topic] = new TopicRef(model, topic, model
						* numModels + topic);
			}
		}
		return topicReferenceObject;
	}

	/**
	 * Given a cluster that references several topics, and the topic to term
	 * probabilities matrix of vectors, this method creates a new probability
	 * vector representing the centroid of the cluster. The centroid is calculated
	 * using the term weight vectors from the original multiModel. The first step
	 * is to create a 'term weight centroid' vector by averaging out the weights
	 * of all non-zero terms from all the topics in the cluster. This weight
	 * vector is then converted to a probability vector.
	 * 
	 * @param cluster
	 * @param topicToTopicDivergence
	 * @return
	 */
	private static double[] generateClusterCentroidProbabilityVector(
			Cluster cluster, final MultiTopicModel multiModel,
			final double[][][] modelToTopicToTermProbabilities) {
		// If the cluster has a single topic, return its probability matrix as it is
		int clusterSize = cluster.getTopics().size();
		int numTerms = multiModel.terms.length;
		if (clusterSize == 1) {
			TopicRef ref = cluster.getTopics().get(0);
			return modelToTopicToTermProbabilities[ref.model][ref.topic];
		}

		double[] centroidTermWeights = new double[numTerms];
		// create the vector with term weights (integers), averaging out over the
		// number of topics in the cluster (the cluster size)
		boolean termInCluster = false;
		for (int curTerm = 0; curTerm < numTerms; curTerm++) {
			// for each topic
			for (TopicRef topicRef : cluster.getTopics()) {
				// if the term is present increase the weight in the vector (using the
				// weight from the topic for this term)
				if (multiModel.modelToTopicToTerm[topicRef.model][topicRef.topic][curTerm] != 0) {
					termInCluster = true;
					centroidTermWeights[curTerm] += multiModel.modelToTopicToTerm[topicRef.model][topicRef.topic][curTerm];
				}
			}
			// average out weights in vector over cluster size
			if (termInCluster) {
				centroidTermWeights[curTerm] = (double) (centroidTermWeights[curTerm] / clusterSize);
			}
			termInCluster = false;
		}

		double[] centroidTermProbabilities = new double[numTerms];
		// create the probability vector
		int totalWeight = 0;
		// calculate the total weight of all terms for the centroidTermWeights
		// vector
		for (int term = 0; term < numTerms; term++) {
			if (centroidTermWeights[term] != 0) {
				totalWeight += centroidTermWeights[term];
			}
		}
		// divide each weight by the total weights for all the vector
		for (int term = 0; term < numTerms; term++) {
			if (centroidTermWeights[term] != 0) {
				centroidTermProbabilities[term] = ((double) centroidTermWeights[term])
						/ (double) totalWeight;
			}
		}
		return centroidTermProbabilities;
	}

	/**
	 * Given a cluster that references several topics, and the topic to term
	 * probabilities matrix of vectors, this method creates a new probability
	 * vector representing the centroid of the cluster. The centroid is calculated
	 * using the term weight vectors from the original multiModel. The first step
	 * is to create a 'term weight centroid' vector by averaging out the weights
	 * of all non-zero terms from all the topics in the cluster.
	 * 
	 * @param cluster
	 * @param topicToTopicDivergence
	 * @return
	 */
	private static int[] generateClusterCentroidWeightVector(Cluster cluster,
			final MultiTopicModel multiModel) {
		// If the cluster has a single topic, return its probability matrix as it is
		int clusterSize = cluster.getTopics().size();
		int numTerms = multiModel.terms.length;
		if (clusterSize == 1) {
			TopicRef ref = cluster.getTopics().get(0);
			return multiModel.modelToTopicToTerm[ref.model][ref.topic];
		}

		int[] centroidTermWeights = new int[numTerms];
		// create the vector with term weights (integers), averaging out over the
		// number of topics in the cluster (the cluster size)
		boolean termInCluster = false;
		for (int curTerm = 0; curTerm < numTerms; curTerm++) {
			// for each topic
			for (TopicRef topicRef : cluster.getTopics()) {
				// if the term is present increase the weight in the vector (using the
				// weight from the topic for this term)
				if (multiModel.modelToTopicToTerm[topicRef.model][topicRef.topic][curTerm] != 0) {
					termInCluster = true;
					centroidTermWeights[curTerm] += multiModel.modelToTopicToTerm[topicRef.model][topicRef.topic][curTerm];
				}
			}
			// average out weights in vector over cluster size
			if (termInCluster) {
				centroidTermWeights[curTerm] = (int) (centroidTermWeights[curTerm] / clusterSize);
			}
			termInCluster = false;
		}

		return centroidTermWeights;
	}

	private static double calculateAverageDivergenceFromCentroid(Cluster cluster,
			final MultiTopicModel multiModel,
			final double[][][] modelToTopicToTermProbabilities) {
		int numTerms = multiModel.terms.length;
		int clusterSize = cluster.getTopics().size();
		if (clusterSize == 1) {
			return 0;
		}
		double[] centroid = generateClusterCentroidProbabilityVector(cluster,
				multiModel, modelToTopicToTermProbabilities);
		double sumOfDivergence = 0;
		for (TopicRef topicRef : cluster.getTopics()) {
			sumOfDivergence += KLDivergenceCalculator
					.getKLDivergenceVectorSpaceDistance(centroid,
							modelToTopicToTermProbabilities[topicRef.model][topicRef.topic],
							numTerms);
		}
		return sumOfDivergence / clusterSize;
	}

	private void printModelDiagnostics(ParallelTopicModel model, int numWords) {
		// TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model,
		// numWords);
		// System.out.print(diagnostics.toXML());

	}

	private void printModelTest(ParallelTopicModel model, int numWords,
			int topic, int modelNum) {
		// TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model,
		// 1);
		//
		// System.out.print(diagnostics.toXML());
		// printTopicTerms(modelNum, topic, multiModel);
	}

	/**
	 * Finds a suitable pair of clusters to collapse. This method extends the
	 * basic algorithm proposed by HAC. HAC clusters together too many topics,
	 * resulting in clusters that diverge too much. To compensate, this method
	 * changes the standard way in which the next pair of clusters to collapse is
	 * identified. Instead of just finding the two closest clusters, it finds two
	 * close clusters that are smaller than a given target cluster size. This
	 * target cluster size is a class level constant. (To test out, we will start
	 * by defining that clusters that have over 10 topics should not be collapsed)
	 * 
	 * @param matrixSize
	 * @param divergenceMatrix
	 * @param clusters
	 * @param numClusters
	 * @return
	 */
	private static ReferencePair findSuitablePairToCollapse(int matrixSize,
			double[][] divergenceMatrix, Cluster[] clusters, int numClusters) {
		// Find the pair of closest related clusters in the matrix and create a new
		// cluster
		double closest = 1000;
		int closestIndexI = -1;
		int closestIndexJ = -1;
		for (int i = 0; i < matrixSize; i++) {
			for (int j = 0; j < matrixSize; j++) {
				if (divergenceMatrix[i][j] >= 0 && divergenceMatrix[i][j] < closest) {
					// REMOVE THIS CONDITIONAL TO DEFAULT TO VANILLA HAC
					if (clusters[i].getTopics().size() < MAX_CLUSTER_SIZE_TO_COLLAPSE
							&& clusters[j].getTopics().size() < MAX_CLUSTER_SIZE_TO_COLLAPSE) {
						closest = divergenceMatrix[i][j];
						closestIndexI = i;
						closestIndexJ = j;
					}
				}
			}
		}
		ReferencePair refPair = new ReferencePair();
		refPair.ref1 = closestIndexI;
		refPair.ref2 = closestIndexJ;
		refPair.divergence = closest;
		return refPair;
	}

	/**
	 * ------------------HELPER CLASSES----------------------
	 */

	/**
	 * Reference from a pair of topics to their index in the big divergence matrix
	 * 
	 * @author nlopezgi
	 * 
	 */
	public static class TopicRef {
		public TopicRef(int model, int topic, int bigMatrixIndex) {
			super();
			this.model = model;
			this.topic = topic;
			this.bigMatrixIndex = bigMatrixIndex;
		}

		public final int model;
		public final int topic;
		public final int bigMatrixIndex;

		public boolean equals(Object other) {
			if (other instanceof TopicRef) {
				return model == ((TopicRef) other).model
						&& topic == ((TopicRef) other).topic;
			}
			return false;
		}
	}

	

	private static class HACIterationObject {
		public double[][] divergenceMatrix;
		public int numClustersFound;
		int matrixSize;
	}

	private static class ReferencePair {
		int ref1;
		int ref2;
		double divergence;
	}

}
