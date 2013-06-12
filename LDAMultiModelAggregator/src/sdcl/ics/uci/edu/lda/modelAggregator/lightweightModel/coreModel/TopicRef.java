package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel;

import java.io.Serializable;

public class TopicRef implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -879415979562115962L;

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
