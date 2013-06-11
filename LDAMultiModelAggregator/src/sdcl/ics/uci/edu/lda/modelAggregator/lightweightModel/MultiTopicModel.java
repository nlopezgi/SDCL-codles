package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel;

/**
 * Lightweight object to store a set of terms and a cube of models-topics-terms
 * values. Used only by the MultiModelAggregator
 * 
 * @author nlopezgi
 * 
 */
public class MultiTopicModel {

	public String[] terms;
	public int[][][] modelToTopicToTerm;

}
