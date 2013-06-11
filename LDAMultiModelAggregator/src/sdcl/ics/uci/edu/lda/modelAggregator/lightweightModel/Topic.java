package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.Task;
import sdcl.ics.uci.edu.lda.util.Term;
import cc.mallet.examples.TopicModel;

public class Topic implements Comparable<Topic> {

	private static final SimilarityStrategy DEFAULT_STRATEGY = SimilarityStrategy.COSINE_DISTANCE;

	private static final int MAX_SIMILAR = 1;

	/**
	 * Constant for KL divergence algorithm. THe algorithm fails miserably
	 * whenever any probability in a vector =0. To compensate (and references
	 * online support this approach) we set all =0 probs to the
	 * MIN_PROBABILITY_VALUE. TO compensate for sum of probability>1 we also
	 * remove these marginal probabilities from any non zero probabiltiy in the
	 * vector (see KL divergence method for more info)
	 * 
	 */
	private static final double MIN_PROBABILITY_VALUE = 1.0e-250;

	private final int number;
	protected List<Term> terms;
	private List<Task> tasks;
	private final TopicModelForReport parent;
	private List<Topic> similarTopics;
	private Map<Topic, Double> similarTopicsToSimilarity;
	private static final String TERM_SPACER = "|";

	private List<Topic> incomingSimilar;

	private List<TopicClass> topicClasses;
	private double avgForTopicClasses = -1;

	private String label = null;

	private double weight = -1;
	private static SimilarityStrategy selectedStrategy = DEFAULT_STRATEGY;

	// private TopicCluster inCluster;
	/**
	 * A measure of how related this topic is with other similar topics
	 */
	private double quantifiedSimilarity = -1;

	protected static Topic getCentroidTopic(List<Term> terms,
			List<TopicClass> classes) {
		return new Topic(terms, classes);
	}

	private Topic(List<Term> terms, List<TopicClass> classes) {
		this.number = -1;
		this.parent = null;
		this.terms = terms;
		topicClasses = classes;
		similarTopics = new ArrayList<Topic>();
		similarTopicsToSimilarity = new HashMap<Topic, Double>();
		incomingSimilar = new ArrayList<Topic>();
	}

	public Topic(int number, List<Term> terms, TopicModelForReport parent) {
		this.number = number;
		this.parent = parent;
		this.terms = terms;
		tasks = new ArrayList<Task>();
		similarTopics = new ArrayList<Topic>();
		similarTopicsToSimilarity = new HashMap<Topic, Double>();
		incomingSimilar = new ArrayList<Topic>();
		topicClasses = new ArrayList<TopicClass>();
	}

	public void addTask(Task oneTask) {
		tasks.add(oneTask);
	}

	public void addTopicClass(TopicClass topicClass) {
		topicClasses.add(topicClass);
		if (avgForTopicClasses != -1) {
			avgForTopicClasses = -1;
		}
	}

	public void addSimilarTopic(Topic similarTopic) {
		similarTopics.add(similarTopic);
	}

	public void addSimilarTopicWithSimilarity(Topic similarTopic,
			Double similarity) {
		similarTopics.add(similarTopic);
		similarTopicsToSimilarity.put(similarTopic, similarity);
	}

	public void addIncomingSimilar(Topic similarTopic) {
		incomingSimilar.add(similarTopic);
	}

	public int getNumber() {
		return number;
	}

	public List<Term> getTerms() {
		return terms;
	}

	public List<String> getTermStrings() {
		List<String> termStrings = new ArrayList<String>();
		for (Term oneTerm : terms) {
			termStrings.add(oneTerm.getTerm());
		}
		return termStrings;
	}

	public List<String> getClassNameStrings() {
		List<String> classNameStrings = new ArrayList<String>();
		for (TopicClass oneClass : topicClasses) {
			classNameStrings.add(oneClass.getClassName());
		}
		return classNameStrings;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public TopicModelForReport getParentModel() {
		return parent;
	}

	public String getLabel() {
		if (terms.size() < 3) {
			return "" + number;
		}
		String label = "" + number + ":" + terms.get(0).getTerm() + TERM_SPACER
				+ terms.get(1).getTerm() + TERM_SPACER + terms.get(2).getTerm();
		return label;
	}

	public List<Topic> getSimilarTopics() {
		return similarTopics;
	}

	public List<Topic> getIncomingSimilarTopics() {
		return incomingSimilar;
	}

	public String toString() {
		// return getNumber() + "|" + getQuantifiedSimilarity() + "|" +
		// tasks.size();
		if (label == null) {
			createLabel();
		}
		return label;
	}

	public void createLabel() {

		label = new String(getNumber() + "");
		if (terms.size() == 0) {
			label = new String("|EMPTY");
		} else if (terms.size() < 20) {
			label = label + "|LOW";
		}
		if (incomingSimilar.size() > 0) {
			label = label + "|R:" + incomingSimilar.size();
		}
		if (similarTopics.size() > 0) {
			double weight = 0;
			for (Topic similar : similarTopics) {
				weight += Topic.calculateSimilarity(this, similar);
			}
			label = label + "|R:" + (int) (weight / 100);
		}
		if (topicClasses.size() > 0) {
			label = label + "|C:" + topicClasses.size();
			label = label + "@"
					+ (int) (getAverageProbabilityForTopicClasses() * 100) + "%";
		}

	}

	public void refreshQuantifiedSimilarity() {
		quantifiedSimilarity = -1;
		getQuantifiedSimilarity();
	}

	public double getQuantifiedSimilarity() {
		if (quantifiedSimilarity == -1) {
			quantifiedSimilarity = 0;
			for (Topic similar : similarTopics) {
				int sim = calculateSimilarity(this, similar);
				quantifiedSimilarity += sim;
			}
		}
		return quantifiedSimilarity;
	}

	/**
	 * Returns the average of the term and class similarity between the two topics
	 * (out a possible max 1000)
	 * 
	 * @param topic
	 * @param similarTopic
	 * @return
	 */
	public static int calculateSimilarity(Topic topic, Topic similarTopic) {
		if (topic == similarTopic) {
			return 1000;
		}
		switch (selectedStrategy) {
		case COSINE_DISTANCE:
			return (int) ((getVectorSpaceDistance(topic, similarTopic, false) + getVectorSpaceDistance(
					topic, similarTopic, true)) / 2);

		case KL_DIVERGENCE:
			return (int) getKLDivergenceVectorSpaceDistance(topic, similarTopic,
					false);

		default:
			return 0;
		}

	}

	public static int calculateTermOnlySimilarity(Topic topic, Topic similarTopic) {
		if (topic == similarTopic) {
			return 1000;
		}

		return (int) getVectorSpaceDistance(topic, similarTopic, false);

	}

	public static double calculateKLDivergence(Topic topic, Topic similarTopic) {
		if (topic == similarTopic) {
			return 0;
		}
		return getKLDivergenceVectorSpaceDistance(topic, similarTopic, false);
	}

	public enum SimilarityStrategy {
		COSINE_DISTANCE, KL_DIVERGENCE, MANHATTAN_DISTANCE;
	}

	public static Set<String> getMatchingClasses(Topic topic, Topic anotherTopic) {
		List<TopicClass> topicClasses = topic.getTopicClasses();
		List<TopicClass> anotherTopicClasses = anotherTopic.getTopicClasses();
		List<String> topClassStr = new ArrayList<String>();
		for (TopicClass oneClass : topicClasses) {
			topClassStr.add(oneClass.getClassName());
		}
		List<String> anotherTopClassStr = new ArrayList<String>();
		for (TopicClass oneClass : anotherTopicClasses) {
			anotherTopClassStr.add(oneClass.getClassName());
		}
		Set<String> intersect = new HashSet<String>(topClassStr);
		intersect.retainAll(anotherTopClassStr);
		return intersect;
	}

	public static Set<String> calculateMatches(Topic topic, Topic similarTopic) {
		List<String> oneTopicList = topic.getTermStrings();
		List<String> secondTopicList = similarTopic.getTermStrings();
		Set<String> intersect = new HashSet<String>(oneTopicList);
		intersect.retainAll(secondTopicList);
		return intersect;
	}

	public int getTermPosition(String searchTerm) {
		for (Term term : terms) {
			if (term.getTerm().equals(searchTerm)) {
				return terms.indexOf(term);
			}
		}
		return -1;
	}

	public static List<Topic> getHighestMatchingTopics(Topic oneTopic,
			Collection<Topic> compareTo) {
		Map<Double, Topic> distanceToTopicMap = new HashMap<Double, Topic>();
		List<Double> distances = new ArrayList<Double>();
		for (Topic compare : compareTo) {
			double distance = (getVectorSpaceDistance(oneTopic, compare, false) + getVectorSpaceDistance(
					oneTopic, compare, true) / 2);
			if (distance != 0) {
				distances.add(distance);
				distanceToTopicMap.put(distance, compare);
			}
		}
		Collections.sort(distances);
		Collections.reverse(distances);
		List<Topic> ret = new ArrayList<Topic>();
		for (int i = 0; i < distances.size() && i < MAX_SIMILAR; i++) {
			ret.add(distanceToTopicMap.get(distances.get(i)));
		}
		return ret;
	}

	private Term getTerm(String termString) {
		for (Term term : terms) {
			if (term.getTerm().equals(termString)) {
				return term;
			}
		}
		return null;
	}

	public double getTermWeight(String termString) {
		Term term = getTerm(termString);
		if (term != null) {
			return term.getWeight();
		}
		return 0;
	}

	public double getClassProbability(String className) {
		for (TopicClass topicClass : topicClasses) {
			if (topicClass.getClassName().equals(className)) {
				return topicClass.getProbability();
			}
		}
		return 0.0;
	}

	private static Collection<String> getWordVector(Topic oneTopic,
			Topic another, boolean useClasses) {
		// create a collection with all the words
		Collection<String> allWords = new ArrayList<String>();

		if (useClasses) {
			allWords.addAll(oneTopic.getClassNameStrings());
			allWords.addAll(another.getClassNameStrings());
		} else {
			// use terms
			allWords.addAll(oneTopic.getTermStrings());
			allWords.addAll(another.getTermStrings());
		}
		return allWords;
	}

	/**
	 * Calculates the vector space model distance between vectors representing the
	 * term distribution for the given topics USING COSINE DISTANCE
	 * 
	 * @param oneTopic
	 * @param another
	 * @return
	 */
	public static double getVectorSpaceDistance(Topic oneTopic, Topic another,
			boolean useClasses) {
		// create a collection with all the words
		Collection<String> allWords = getWordVector(oneTopic, another, useClasses);

		double sumOfSquareWeightsForOneTopic = 0;
		double sumOfSquareWeightsForAnotherTopic = 0;
		double sumOfWeightMult = 0;

		double oneTopicWordWeight = 0;
		double anotherTopicWordWeight = 0;

		// for each word in the all word collection
		for (String oneTerm : allWords) {

			oneTopicWordWeight = getTermWeightOrClassProbability(oneTopic, oneTerm,
					useClasses);
			anotherTopicWordWeight = getTermWeightOrClassProbability(another,
					oneTerm, useClasses);

			// increment the sums
			sumOfSquareWeightsForOneTopic += (oneTopicWordWeight * oneTopicWordWeight);
			sumOfSquareWeightsForAnotherTopic += (anotherTopicWordWeight * anotherTopicWordWeight);
			sumOfWeightMult += oneTopicWordWeight * anotherTopicWordWeight;
		}
		if (sumOfSquareWeightsForOneTopic == 0
				|| sumOfSquareWeightsForAnotherTopic == 0) {
			return 0;
		}
		// calculate the cosine distance
		return 1000 * (sumOfWeightMult / (Math.sqrt(sumOfSquareWeightsForOneTopic) * Math
				.sqrt(sumOfSquareWeightsForAnotherTopic)));

	}

	/**
	 * Calculates the vector space model distance between vectors representing the
	 * term distribution for the given topics USING KL divergence similarity
	 * 
	 * @param oneTopic
	 * @param another
	 * @return
	 */
	public static double getKLDivergenceVectorSpaceDistance(Topic oneTopic,
			Topic another, boolean useClasses) {
		double sum = 0.0;
		double sum2 = 0.0;
		// create a collection with all the words
		Collection<String> allWords = getWordVector(oneTopic, another, useClasses);
		// for each word in the all word collection
		double[] oneTopicVector = new double[allWords.size()];
		double[] anotherTopicVector = new double[allWords.size()];
		int discountFromOne = 0;
		int discountFromAnother = 0;
		int nonZeroFromOne = 0;
		int nonZeroFromAnother = 0;

		double totalWordWeightsForOne = 0.0;
		double totalWordWeightsForAnother = 0.0;
		int i = 0;
		for (String oneTerm : allWords) {
			oneTopicVector[i] = getTermWeightOrClassProbability(oneTopic, oneTerm,
					useClasses);
			if (oneTopicVector[i] == 0) {
				discountFromOne++;
			} else {
				nonZeroFromOne++;
				totalWordWeightsForOne += oneTopicVector[i];
			}
			anotherTopicVector[i] = getTermWeightOrClassProbability(another, oneTerm,
					useClasses);
			if (anotherTopicVector[i] == 0) {
				discountFromAnother++;
			} else {
				nonZeroFromAnother++;
				totalWordWeightsForAnother += anotherTopicVector[i];
			}

			i++;
		}
		// If we are using terms we need to convert the weights to probabilities for
		// this algorithm to work. Divide each non-zero weight by the
		// totalWordWeights
		if (!useClasses) {
			for (i = 0; i < oneTopicVector.length; i++) {
				if (oneTopicVector[i] != 0) {
					oneTopicVector[i] = oneTopicVector[i] / totalWordWeightsForOne;
				}
			}
			for (i = 0; i < anotherTopicVector.length; i++) {
				if (anotherTopicVector[i] != 0) {
					anotherTopicVector[i] = anotherTopicVector[i]
							/ totalWordWeightsForAnother;
				}
			}
		}

		double totalDiscountForOne = discountFromOne * MIN_PROBABILITY_VALUE;
		double totalDiscountForAnother = discountFromAnother
				* MIN_PROBABILITY_VALUE;
		double individualDiscountForNonZeroForOne = totalDiscountForOne = totalDiscountForOne
				/ nonZeroFromOne;
		double individualDiscountForNonZeroForAnother = totalDiscountForAnother = totalDiscountForAnother
				/ nonZeroFromAnother;
		for (i = 0; i < oneTopicVector.length; i++) {
			if (oneTopicVector[i] == 0) {
				oneTopicVector[i] = MIN_PROBABILITY_VALUE;
			} else {
				oneTopicVector[i] = oneTopicVector[i]
						- individualDiscountForNonZeroForOne;
			}
		}

		for (i = 0; i < anotherTopicVector.length; i++) {
			if (anotherTopicVector[i] == 0) {
				anotherTopicVector[i] = MIN_PROBABILITY_VALUE;
			} else {
				anotherTopicVector[i] = anotherTopicVector[i]
						- individualDiscountForNonZeroForAnother;
			}
		}

		for (i = 0; i < oneTopicVector.length; i++) {
			// double oneP = oneTopicVector[i];
			// double anotherP = anotherTopicVector[i];
			double log = Math.log(oneTopicVector[i] / anotherTopicVector[i])
					/ Math.log(2);
			double log2 = Math.log(anotherTopicVector[i] / oneTopicVector[i])
					/ Math.log(2);
			// sum += (oneTopicVector[i] - anotherTopicVector[i]) * log;
			sum += oneTopicVector[i] * log;
			sum2 += anotherTopicVector[i] * log2;
		}
		return (sum * 0.5) + (sum2 * 0.5);

	}

	public static double getTermWeightOrClassProbability(Topic topic,
			String termOrClass, boolean useClasses) {
		if (useClasses) {
			return topic.getClassProbability(termOrClass);
		} else {
			// use terms
			Term term = topic.getTerm(termOrClass);
			if (term != null) {
				return term.getWeight();
			} else {
				return 0;
			}
		}
	}

	public List<TopicClass> getTopicClasses() {
		return topicClasses;
	}

	private double getAverageProbabilityForTopicClasses() {
		if (avgForTopicClasses == -1) {
			avgForTopicClasses = 0;
			for (TopicClass topicClass : topicClasses) {
				avgForTopicClasses += topicClass.getProbability();
			}
			if (topicClasses.size() > 0) {
				avgForTopicClasses = avgForTopicClasses / topicClasses.size();
			}
		}
		return avgForTopicClasses;
	}

	public double quantifyWeight() {
		if (weight == -1) {
			if (similarTopics.size() > 0) {
				double outTopicSimilarity = 0;
				for (Topic similar : similarTopics) {
					int sim = calculateSimilarity(this, similar);
					outTopicSimilarity += sim;
				}
				weight = outTopicSimilarity;
			}
			if (incomingSimilar.size() > 0) {
				double inTopicSimilarity = 0;
				for (Topic similar : incomingSimilar) {
					int sim = calculateSimilarity(similar, this);
					inTopicSimilarity += sim;
				}
				weight = inTopicSimilarity;
			}

		}
		return weight;
	}

	@Override
	public int compareTo(Topic another) {

		Topic other = (Topic) another;
		return (int) (other.quantifyWeight() - this.quantifyWeight());
	}

	public void orderTopicClasses() {
		Collections.sort(topicClasses);
	}

	/**
	 * returns a list of classes with the number of times they are also referred
	 * to by a similar topic
	 */
	public List<TermOrClassFrequency> getClassFrequency() {
		List<TermOrClassFrequency> ret = new ArrayList<TermOrClassFrequency>();
		for (TopicClass topicClass : topicClasses) {
			TermOrClassFrequency oneClassFrequency = new TermOrClassFrequency(false,
					topicClass.getClassName());
			ret.add(oneClassFrequency);
			for (Topic similar : similarTopics) {
				double classProbability = similar.classProbability(topicClass
						.getClassName());
				if (classProbability > 0) {
					oneClassFrequency.frequency++;
					oneClassFrequency.increaseWeight(classProbability);
				}
			}
			for (Topic similar : incomingSimilar) {
				double classProbability = similar.classProbability(topicClass
						.getClassName());
				if (classProbability > 0) {
					oneClassFrequency.frequency++;
					oneClassFrequency.increaseWeight(classProbability);
				}
			}
		}
		return ret;
	}

	private double classProbability(String className) {
		for (TopicClass topicClass : topicClasses) {
			if (topicClass.getClassName().equals(className)) {
				return topicClass.getProbability();
			}
		}
		return 0;
	}

	private boolean refersToTerm(String inTerm) {
		for (Term term : terms) {
			if (term.getTerm().equals(inTerm)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns a list of terms with the number of times they are also referred to
	 * by a similar topic
	 */
	public List<TermOrClassFrequency> getTermFrequency() {
		List<TermOrClassFrequency> ret = new ArrayList<TermOrClassFrequency>();
		for (Term term : terms) {
			TermOrClassFrequency oneTermFrequency = new TermOrClassFrequency(true,
					term.getTerm());

			ret.add(oneTermFrequency);
			for (Topic similar : similarTopics) {
				if (similar.refersToTerm(term.getTerm())) {
					oneTermFrequency.frequency++;
				}
			}
			for (Topic similar : incomingSimilar) {
				if (similar.refersToTerm(term.getTerm())) {
					oneTermFrequency.frequency++;
				}
			}
		}
		return ret;
	}

	public static class TermOrClassFrequency implements
			Comparable<TermOrClassFrequency> {
		private final String token;
		public int frequency = 0;
		private double classOrTermWeight = 0;
		private final boolean term;

		public TermOrClassFrequency(boolean term, String token) {
			this.token = token;
			this.term = term;
		}

		public double getAverageWeight() {
			if (frequency == 0) {
				return 0;
			}
			return classOrTermWeight / frequency;
		}

		public double getWeight() {
			return classOrTermWeight;
		}

		public void increaseWeight(double prob) {
			classOrTermWeight += prob;
		}

		@Override
		public int compareTo(TermOrClassFrequency o) {
			// if (frequency != o.frequency) {
			// return frequency - o.frequency;
			// } else {
			// if (term) {
			// return (int) (o.getAverageWeight() * 1000 - getAverageWeight() * 1000);
			// }
			// return (int) (getAverageWeight() * 1000 - o.getAverageWeight() * 1000);
			// }
			return (int) (classOrTermWeight * 1000 - o.classOrTermWeight * 1000);
		}

		public String getToken() {
			return token;
		}

	}

	public void removeSimilarTopic(Topic similar) {
		similarTopics.remove(similar);
	}

	public static List<Topic> getHighestMatchingTopicsWithIgnoreList(
			Topic oneTopic, Collection<Topic> compareTo, List<Topic> ignoreTopicList) {
		Map<Double, Topic> distanceToTopicMap = new HashMap<Double, Topic>();
		List<Double> distances = new ArrayList<Double>();
		for (Topic compare : compareTo) {
			double distance = (getVectorSpaceDistance(oneTopic, compare, false) + getVectorSpaceDistance(
					oneTopic, compare, true) / 2);
			if (distance != 0) {
				distances.add(distance);
				distanceToTopicMap.put(distance, compare);
			}
		}
		Collections.sort(distances);
		Collections.reverse(distances);
		List<Topic> ret = new ArrayList<Topic>();
		for (int i = 0; i < distances.size() && ret.size() < MAX_SIMILAR; i++) {
			double distance = distances.get(i);
			Topic related = distanceToTopicMap.get(distances.get(i));
			if (!ignoreTopicList.contains(related)) {
				ret.add(distanceToTopicMap.get(distances.get(i)));
			}
		}
		return ret;
	}

	public void clearSimilarTopics() {
		similarTopics.clear();
	}

	/**
	 * Calculates the relevance of a topic in terms of the parts of the code
	 * characterized. For each class it multiplies the probability times the size
	 * of the class. Returns the sum of all values for all the classes included
	 * 
	 * @return
	 */
	public double getTopicRelevance() {
		double sum = 0;
		for (TopicClass oneTopicClass : topicClasses) {
			double LOCsinClass = oneTopicClass.getClassText().split(
					ExperimentDataUtil.EOL).length;
			sum += LOCsinClass * oneTopicClass.getProbability();
		}
		return sum;
	}

	public Map<Topic, Double> getSimilarTopicsToSimilarity() {
		return similarTopicsToSimilarity;
	}

	/**
	 * Get a measure of how important the topic is in terms of how many classes
	 * and the probability for each class.
	 * 
	 * @return
	 */
	public double getClassProbabilityWeight() {
		double weight = 0.0;
		for (TopicClass topicClass : topicClasses) {
			weight += topicClass.getProbability();
		}
		return weight;
	}

	public double getTermProbabilityWeight() {
		double weight = 0.0;
		for (Term term : terms) {
			weight += term.getWeight();
		}
		return weight;
	}
}
