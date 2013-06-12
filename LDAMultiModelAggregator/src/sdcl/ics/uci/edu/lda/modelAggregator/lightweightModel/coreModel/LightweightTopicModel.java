package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that stores a minimal model resulting from aggregating several LDA
 * topic models. The contents of this class can be written to csv files using
 * the MultiModelWriter. To ensure proper reading of files written, please make
 * sure this class matches
 * sdcl.ics.uci.edu.lda.topicModelComparer.model.LightweightTopicModel
 * 
 * @author nlopezgi
 * 
 */
public class LightweightTopicModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5292548351281696706L;

	public static final int MIN_CLUSTER_SIZE = 4;

	public long snapshotTime;
	/**
	 * List of terms
	 */
	public String[] terms;
	/**
	 * The list of divergence for each cluster (the average divergence from each
	 * source topic to the centroid term vector)
	 */
	public double[] aggregatedTopicDivergence;
	/**
	 * The size of the cluster (in number of topics aggregated)
	 */
	public int[] originClusterSize;
	/**
	 * The topic to terms matrix [topicId][termId], for each topic (a cluster
	 * which is now interpreted as a topic) the terms and their weights (each
	 * vector ordered in the same way as the terms vector)
	 */
	public int[][] topicToTerm;
	/**
	 * List of cluster objects
	 */
	private Cluster[] clusters;

	/**
	 * list of classes
	 */
	public String[] classNames;
	/**
	 * The topic to classNames matrix [clusterID][classID], for each topic (a
	 * cluster which is now interpreted as a topic) the probability of being
	 * related to this topic (each vector ordered in the same way as the
	 * classNames vector)
	 */
	public double[][] topicToClasses;

	/**
	 * The set of changes assigned to this version
	 */
	public ChangeSet[] changeSets;

	/**
	 * The sizes of each class, orderd in the same way as the classNames array
	 */
	public int[] classSizes;

	/**
	 * Cluster indicating all topics not included in any other selected cluster
	 */
	public Cluster prunedTopicsCluster;

	/**
	 * Only the clusters that are selected (size is above
	 * MultiModelAggregator.MIN_CLUSTER_SIZE)
	 */
	private Cluster[] selectedClusters = null;

	public void setClusters(Cluster[] clusters) {
		this.clusters = clusters;
	}

	public Cluster[] getAllClusters() {
		return clusters;
	}

	public Cluster[] getSelectedClusters() {
		if (selectedClusters == null) {
			List<Cluster> selected = new ArrayList<Cluster>();
			for (int i = 0; i < clusters.length; i++) {
				if (clusters[i].getTopics().size() > MIN_CLUSTER_SIZE) {
					selected.add(clusters[i]);
				}
			}
			selectedClusters = new Cluster[selected.size()];
			selectedClusters = selected.toArray(selectedClusters);
		}
		return selectedClusters;
	}
}
