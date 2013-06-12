package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sdcl.ics.uci.edu.lda.modelAggregator.MultiModelAggregator;
import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.LightweightTopicModel;
import cc.mallet.topics.ParallelTopicModel;

/**
 * Builds a lightweight model with the given set of ParallelTopicModel. Used only by the MultiModelAggregator
 * 
 * @author nlopezgi
 * 
 */
public class Builder {

	/**
	 * Reads each model using the displayTopWords method and creates a
	 * MultiTopicModel
	 * 
	 * @param models
	 * @param numTopics
	 * @return
	 * @throws Exception
	 */
	public MultiTopicModel buildModel(List<ParallelTopicModel> models,
			int numTopics) throws Exception {
		MultiTopicModel ret = new MultiTopicModel();

		String[] terms;
		int[][][] modelToTopicToTerm;
		int numModels = models.size();
		int currentModel = 0;
		Map<String, TermToTopics> termToTopics = new HashMap<String, TermToTopics>();

		for (ParallelTopicModel model : models) {
			String allTopWords = model.displayTopWords(
					MultiModelAggregator.NUM_TERMS, true);
			String[] tokenizedWords = allTopWords.split("\n");
			int currentTopic = 0;

			for (String oneLine : tokenizedWords) {

				int nextTopic = currentTopic + 1;
				if (oneLine.startsWith("" + currentTopic)) {

				} else if (oneLine.startsWith("" + nextTopic)) {
					currentTopic = nextTopic;
				} else {
					String[] tokenizeLine = oneLine.split("\t");
					String term = tokenizeLine[0];
					int weight = Integer.parseInt(tokenizeLine[1].replace(",", ""));
					TermToTopics termToTopic = null;
					if (!termToTopics.containsKey(term)) {
						termToTopic = new TermToTopics(numModels, numTopics);
						termToTopic.term = term;
						termToTopics.put(term, termToTopic);
					} else {
						termToTopic = termToTopics.get(term);
					}
					// If this fails, the model file actually has more topics than you
					// expected, check its valid
					termToTopic.modelTopicWeights[currentModel][currentTopic] = weight;
				}
			}
			currentModel++;
		}
		int numTerms = termToTopics.size();
		terms = new String[numTerms];
		modelToTopicToTerm = new int[numModels][numTopics][numTerms];
		int termNum = 0;
		for (TermToTopics oneTermToTopic : termToTopics.values()) {
			terms[termNum] = oneTermToTopic.term;
			for (int modelNum = 0; modelNum < numModels; modelNum++) {
				for (int topicNum = 0; topicNum < numTopics; topicNum++) {
					modelToTopicToTerm[modelNum][topicNum][termNum] = oneTermToTopic.modelTopicWeights[modelNum][topicNum];
				}
			}
			termNum++;
		}
		ret.terms = terms;
		ret.modelToTopicToTerm = modelToTopicToTerm;

		return ret;
	}

	private class TermToTopics {
		String term;
		int[][] modelTopicWeights;

		protected TermToTopics(int numModels, int numTopics) {
			modelTopicWeights = new int[numModels][numTopics];
		}
	}

	public static void buildTopicToClassNameVector(LightweightTopicModel model,
			String[] classNames) {

	}
}
