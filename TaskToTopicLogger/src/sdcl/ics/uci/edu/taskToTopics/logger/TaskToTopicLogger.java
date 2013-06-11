package sdcl.ics.uci.edu.taskToTopics.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.internal.runners.statements.ExpectException;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;

import cc.mallet.topics.ParallelTopicModel;

public class TaskToTopicLogger {

	private static final String TAB = "\t";
	private static String TOPIC_FILE = ExperimentDataUtil.TASK_TO_TOPICS_LOGGING
			+ ExperimentDataUtil.SEPARATOR + "topics.txt";
	private static String PROBABILITY_FILE = ExperimentDataUtil.TASK_TO_TOPICS_LOGGING
			+ ExperimentDataUtil.SEPARATOR + "probabilities.txt";
	static BufferedWriter topicOut = null;
	static BufferedWriter probabilityMapsOut = null;
	static boolean logging = false;

	public static void startLogging() throws Exception {
		logging = true;
		startupProbabilityMapsOut();
		startupTopicOut();
	}

	public static void stopLogging() throws Exception {
		logging = false;
		closeProbabilityMapsOut();
		closeTopicOut();
	}

	public static void logProbabilityMap(
			Map<String, double[]> filesToProbabilities) {
		if (logging) {
			try {
				probabilityMapsOut.write("\n");
				for (Entry<String, double[]> oneEntry : filesToProbabilities.entrySet()) {
					probabilityMapsOut.write(oneEntry.getKey() + TAB);
					for (int i = 0; i < oneEntry.getValue().length; i++) {
						probabilityMapsOut.write(oneEntry.getValue()[i] + TAB);
					}
					probabilityMapsOut.write("\n");
				}
			} catch (Exception e) {
				System.err.println("ERROR IN LOGGING");
				e.printStackTrace();
			}
		}
	}

	private static void startupTopicOut() throws Exception {
		File topicFile = new File(TOPIC_FILE);
		if (!topicFile.exists()) {
			topicFile.createNewFile();
		} else {
			topicFile.delete();
			topicFile.createNewFile();
		}
		FileWriter writer = new FileWriter(topicFile);
		topicOut = new BufferedWriter(writer);
	}

	private static void closeTopicOut() throws Exception {
		topicOut.close();

	}

	private static void startupProbabilityMapsOut() throws Exception {
		File probabilitiesMapFile = new File(PROBABILITY_FILE);
		if (!probabilitiesMapFile.exists()) {
			probabilitiesMapFile.createNewFile();
		} else {
			probabilitiesMapFile.delete();
			probabilitiesMapFile.createNewFile();
		}
		FileWriter writer = new FileWriter(probabilitiesMapFile);
		probabilityMapsOut = new BufferedWriter(writer);
	}

	private static void closeProbabilityMapsOut() throws Exception {
		probabilityMapsOut.close();

	}

	public static void logTopics(ParallelTopicModel model, String modelName)
			throws Exception {
		if (logging) {
			Object[][] topics = model.getTopWords(20);
			topicOut.write("Model:" + modelName + "\n");
			for (int i = 0; i < topics.length; i++) {
				topicOut.write("TOPIC,");
				for (int j = 0; j < topics[i].length; j++) {
					topicOut.write(topics[i][j] + "," + TAB + "");
				}
				topicOut.write("\n");
			}
		}
	}
}
