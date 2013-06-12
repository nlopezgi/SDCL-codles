package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel;

import java.io.Serializable;

/**
 * A set of changes committed in a single operation. This object should be
 * associated with a given lightweightTopicModel
 * 
 * @author nlopezgi
 * 
 */
public class ChangeSet implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1165847666299161196L;

	public String id;
	public FileChange[] changes;
	/**
	 * Index of topics in optimal topic subset (small set of topics that best
	 * characterizes all files in this ChangeSet)
	 */
	public int[] topics;
	public double[] topicProbabilities;

	/**
	 * a file included in this change. The id should correspond to the index of
	 * this class in the LightweightTopicModel that contains this ChangeSet
	 * 
	 * @author nlopezgi
	 * 
	 */
	public static class FileChange implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3117446598186387759L;
		/**
		 * The id of the class (matches index of class in the LightweightTopicModel
		 * that contains this change
		 */
		public int id;
		public int detla;
		public int add;
		public int del;
	}
}
