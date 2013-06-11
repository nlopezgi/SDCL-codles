package sdcl.ics.uci.edu.lda.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.mallet.topics.ParallelTopicModel;

public class ModelFileUtil {
	public static final String MODEL_DIR = ExperimentDataUtil.MODEL_DIR;
	public static final String MODEL_FILE_PREFIX = "mallet-lda";
	public static final String MODEL_FILE_SUFFIX = ".model";

	// public static final double MYLYN_DEFINED_ALPHA = 0.025;
	public static final double MYLYN_DEFINED_ALPHA = 0.25;
	public static final double MYLYN_DEFINED_BETA = 1.5;
	public static final int DEFAULT_N = 100;

	public static String resolveModelFileName(Long snapshot, double alpha,
			double beta, int n) {
		return MODEL_FILE_PREFIX + "-" + snapshot + "-" + n + "-A-" + alpha + "-B-"
				+ beta + MODEL_FILE_SUFFIX;
	}

	public static String resolveModelFileName(Long snapshot) {
		return MODEL_FILE_PREFIX + "-" + snapshot + "-" + DEFAULT_N + "-A-"
				+ MYLYN_DEFINED_ALPHA + "-B-" + MYLYN_DEFINED_BETA + MODEL_FILE_SUFFIX;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static List<Long> readModelFiles() {
		List<Long> ret = new ArrayList<Long>();
		File modelDir = new File(MODEL_DIR);
		if (modelDir.isDirectory()) {
			File[] listOfFiles = modelDir.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].getName().startsWith(MODEL_FILE_PREFIX)) {
					String oneModelDate = listOfFiles[i].getName();
					oneModelDate = oneModelDate.substring(
							oneModelDate.indexOf(MODEL_FILE_PREFIX)
									+ MODEL_FILE_PREFIX.length() + 1, oneModelDate.indexOf('.'));
					oneModelDate = oneModelDate.substring(0, oneModelDate.indexOf('-'));
					try {
						Long dateLong = Long.parseLong(oneModelDate);
						if (!ret.contains(dateLong)) {
							ret.add(dateLong);
						}
					} catch (Exception e) {
						System.err.println("Could not parse date for model "
								+ listOfFiles[i].getAbsolutePath());
					}
				}
			}
		}
		return ret;
	}
	
	

	public static List<Long> readSampleModelFiles() {
		List<Long> ret = new ArrayList<Long>();
		File modelDir = new File(MODEL_DIR);
		if (modelDir.isDirectory()) {
			File[] listOfFiles = modelDir.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].getName().startsWith(MODEL_FILE_PREFIX)) {
					String oneModelDate = listOfFiles[i].getName();
					oneModelDate = oneModelDate.substring(
							oneModelDate.indexOf(MODEL_FILE_PREFIX)
									+ MODEL_FILE_PREFIX.length() + 1, oneModelDate.indexOf('.'));
					oneModelDate = oneModelDate.substring(0, oneModelDate.indexOf('-'));
					try {
						Long dateLong = Long.parseLong(oneModelDate);
						if (!ret.contains(dateLong)) {
							ret.add(dateLong);
						}
					} catch (Exception e) {
						System.err.println("Could not parse date for model "
								+ listOfFiles[i].getAbsolutePath());
					}
				}
			}
		}
		List<Long> prunedRet = new ArrayList<Long>();
		if (ret.size() >= 3) {
			prunedRet.add(ret.get(ret.size() - 3));
			prunedRet.add(ret.get(ret.size() - 2));
			prunedRet.add(ret.get(ret.size() - 1));
		}
		return prunedRet;
	}

	public static ParallelTopicModel getModel(String directoryPath,
			String modelFile) throws Exception {
		File directory = new File(directoryPath);
		if (!directory.exists()) {
			throw new Exception("Model dir:" + directoryPath + "not found");
		}
		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		if (!file.exists()) {
			throw new Exception("Model file:" + directoryPath + "/" + modelFile
					+ "not found");
		} else {
			model = ParallelTopicModel.read(file);
		}
		return model;
	}

	public static ParallelTopicModel getModelFromDefaultPath(long snapshot)
			throws Exception {
		return getModel(
				MODEL_DIR,
				resolveModelFileName(snapshot, MYLYN_DEFINED_ALPHA, MYLYN_DEFINED_BETA,
						DEFAULT_N));
	}

	public static Map<Integer, List<Term>> getModelTerms(
			ParallelTopicModel model, int numWords) {
		String allTopWords = model.displayTopWords(numWords, true);
		String[] tokenizedWords = allTopWords.split("\n");
		int currentTopic = 0;
		Map<Integer, List<Term>> ret = new HashMap<Integer, List<Term>>();
		List<Term> currentTopicTermList = new ArrayList<Term>();
		ret.put(currentTopic, currentTopicTermList);
		for (String oneLine : tokenizedWords) {
			int nextTopic = currentTopic + 1;
			if (oneLine.startsWith("" + currentTopic)) {

			} else if (oneLine.startsWith("" + nextTopic)) {
				currentTopic = nextTopic;
				currentTopicTermList = new ArrayList<Term>();
				ret.put(currentTopic, currentTopicTermList);
			} else {
				String[] tokenizeLine = oneLine.split("\t");
				String term = tokenizeLine[0];
				int weight = Integer.parseInt(tokenizeLine[1].replace(",", ""));
				Term oneTerm = new Term(term);
				oneTerm.setWeight(weight);
				currentTopicTermList.add(oneTerm);
			}
		}
		return ret;
	}

	/**
	 * Creates a map with the probabilities of each java file (keys in return map)
	 * being described by each topic (values in return map)
	 * 
	 * @param model
	 *          the model to get the probabilities from
	 * @param textNames
	 *          a list of classes (or documents) in the same order as those used
	 *          to train the model (i.e., corresponding to the models's instance
	 *          list by instanceId)
	 * @return A map from the class name to its probability vector (each item
	 *         represents the probability of the file being represented by each
	 *         topic)
	 */
	public static Map<String, double[]> createProbabilityMap(
			ParallelTopicModel model, List<String> textNames) {
		Map<String, double[]> ret = new HashMap<String, double[]>();

		for (int i = 0; i < textNames.size(); i++) {
			if (ret.containsKey(textNames.get(i))) {
				System.out.println("A class name is repeadted:" + textNames.get(i));
			} else {
				if (model.getTopicProbabilities(i) == null) {
					System.out
							.println("the list of classes does not match the given model ");
				}
				ret.put(textNames.get(i), model.getTopicProbabilities(i));
			}
		}
		return ret;
	}

}
