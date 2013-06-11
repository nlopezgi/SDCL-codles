package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.lda.util.ModuleData;
import sdcl.ics.uci.edu.lda.util.Task;
import sdcl.ics.uci.edu.lda.util.Term;
import cc.mallet.topics.ParallelTopicModel;

public class TopicModelForReport {

	private ParallelTopicModel topicModel;
	private ModuleData module = null;

	// private final boolean isRootModel;
	private static int NUM_WORDS = 20;

	private Map<Integer, Topic> topics;

	public TopicModelForReport(ParallelTopicModel topicModel) {

		this.topicModel = topicModel;

		Map<Integer, List<Term>> termsMap = ModelFileUtil.getModelTerms(topicModel,
				NUM_WORDS);

		topics = new HashMap<Integer, Topic>();
		for (Integer topic : termsMap.keySet()) {
			topics.put(topic, new Topic(topic, termsMap.get(topic), this));
		}
	}

	public ParallelTopicModel getLDAModel() {
		return topicModel;
	}

	public void setTopicModel(ParallelTopicModel topicModel) {
		this.topicModel = topicModel;
	}

	public Collection<Topic> getTopics() {
		return topics.values();
	}

	public Topic getTopicByNumber(int number) {
		return topics.get(number);
	}

	public static int getNumWords() {
		return NUM_WORDS;
	}

	public void addTaskToTopic(int topicNumber, Task task) {
		topics.get(topicNumber).addTask(task);
	}

	public ModuleData getModule() {
		return module;
	}

	public void setModule(ModuleData module) {
		this.module = module;
	}

	// public List<TopicValuePair> getHighestRelatedTopics() {
	// if (highestTopics == null) {
	// highestTopics = new ArrayList<TopicValuePair>();
	// if (parentModelTopicProbabilities != null) {
	// double[] topicProbabilities = parentModelTopicProbabilities;
	// for (int i = 0; i < topicProbabilities.length; i++) {
	// double prob = topicProbabilities[i];
	// if (highestTopics.size() == 0) {
	// highestTopics.add(new TopicValuePair(i, prob));
	// }
	// boolean inserted = false;
	// for (int j = 0; (j < highestTopics.size() && !inserted); j++) {
	// TopicValuePair onePair = highestTopics.get(j);
	// if (onePair.value < prob) {
	// highestTopics.add(j, new TopicValuePair(i, prob));
	// inserted = true;
	// break;
	// }
	// }
	// if (!inserted) {
	// highestTopics.add(new TopicValuePair(i, prob));
	// }
	// while (highestTopics.size() > MAX_RELATED_TOPICS_TO_MODULE) {
	// highestTopics.remove(highestTopics.size() - 1);
	// }
	// }
	// }
	// }
	// return highestTopics;
	// }

	// public List<Term> getMostCommonTerms() {
	// if (mostCommonTerms == null) {
	// mostCommonTerms = new ArrayList<Term>();
	// Map<String, Term> termMap = new HashMap<String, Term>();
	// for (Topic topic : topics.values()) {
	// for (Term term : topic.getTerms()) {
	// if (!termMap.containsKey(term.getTerm())) {
	// termMap.put(term.getTerm(), new Term(term.getTerm()));
	// }
	// termMap.get(term.getTerm()).increaseFrecuency();
	// }
	// }
	// for (String termStr : termMap.keySet()) {
	// Term term = termMap.get(termStr);
	// boolean inserted = false;
	// for (int j = 0; (j < mostCommonTerms.size() && !inserted); j++) {
	// Term oneTerm = mostCommonTerms.get(j);
	// if (oneTerm.getFrequency() < term.getFrequency()) {
	// mostCommonTerms.add(j, term);
	// inserted = true;
	// break;
	// }
	// }
	// if (!inserted) {
	// mostCommonTerms.add(term);
	// }
	// while (mostCommonTerms.size() > MAX_COMMON_TERMS) {
	// mostCommonTerms.remove(mostCommonTerms.size() - 1);
	// }
	// }
	// }
	// return mostCommonTerms;
	// }
	//
	// public List<String> getAllOrderedTerms() {
	// if (orderedTerms == null) {
	//
	// orderedTerms = new ArrayList<String>();
	// Alphabet alphabet = getLDAModel().getAlphabet();
	//
	// Iterator alphabetIterator = alphabet.iterator();
	// while (alphabetIterator.hasNext()) {
	// String word = (String) alphabetIterator.next();
	// orderedTerms.add(word);
	// }
	// Collections.sort(orderedTerms, String.CASE_INSENSITIVE_ORDER);
	// }
	// return orderedTerms;
	// }

	public void removeTopic(Topic topic) {
		topics.remove(topic.getNumber());
	}
}
