package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * a simple cluster object pointing to topic references and to other collapsed
 * clusters (clusters that were collapsed by the HAC algorithm to create this
 * cluster)
 * 
 * @author nlopezgi
 * 
 */
public class Cluster implements Comparable<Cluster>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7730419533572624574L;
	
	public List<TopicRef> topics = new ArrayList<TopicRef>();
	public List<Cluster> collapsedClusters = new ArrayList<Cluster>();
	public double averageDivergenceFromCentroid;
	public double averageCoverage;
	public double averageWeightedCoverage;

	public void addTopic(TopicRef topicRef) {
		topics.add(topicRef);
	}

	public void addAllTopics(List<TopicRef> topics) {
		this.topics.addAll(topics);
	}

	public List<TopicRef> getTopics() {
		return topics;
	}

	public void addCollapsedCluster(Cluster cluster) {
		this.collapsedClusters.add(cluster);
	}

	public List<Cluster> getCollapsedClusters() {
		return collapsedClusters;
	}

	@Override
	public int compareTo(Cluster arg0) {
		if (this == arg0) {
			return 0;
		}
		// if (this.topics.size() <= TARGET_CLUSTER_SIZE
		// && (this.topics.size() != arg0.topics.size())) {
		// return arg0.topics.size() - this.topics.size();
		// } else {
		return (int) (this.averageDivergenceFromCentroid - arg0.averageDivergenceFromCentroid);
		// }
	}
}
