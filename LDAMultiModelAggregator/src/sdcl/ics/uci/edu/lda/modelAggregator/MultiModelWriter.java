package sdcl.ics.uci.edu.lda.modelAggregator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel.LightweightTopicModel;

/**
 * Writes a LighweightTopicModel object to a csv file. See @MultiModelReader in
 * project TopicModelComparer as both files must match
 * 
 * @author nlopezgi
 * 
 */
public class MultiModelWriter {

	public static void writeToFile(LightweightTopicModel model, File modelFile,
			boolean filterSmallClusters) throws Exception {

		if (!modelFile.exists()) {
			modelFile.createNewFile();
		} else {
			modelFile.delete();
			modelFile.createNewFile();
		}
		FileWriter writer = new FileWriter(modelFile);
		BufferedWriter modelOut = new BufferedWriter(writer);
		writeModel(model, modelOut, filterSmallClusters);
		modelOut.close();
	}

	private static void writeModel(LightweightTopicModel model,
			BufferedWriter modelOut, boolean filterSmallClusters) throws Exception {
		String terms = "";
		for (int i = 0; i < model.terms.length; i++) {
			terms = terms + model.terms[i];
			if (i < model.terms.length - 1) {
				terms = terms + ",";
			}
		}
		terms = terms + "\n";
		modelOut.write(terms);
		int numTopics = model.topicToTerm.length;
		String numTopicsStr = "";
		if (filterSmallClusters) {
			numTopicsStr = "" + model.getSelectedClusters().length + "\n";
		} else {
			numTopicsStr = "" + numTopics + "\n";
		}
		modelOut.write(numTopicsStr);

		String divergenceLine = "";
		for (int i = 0; i < model.aggregatedTopicDivergence.length; i++) {
			if ((filterSmallClusters && model.originClusterSize[i] > MultiModelAggregator.MIN_CLUSTER_SIZE)
					|| !filterSmallClusters) {
				divergenceLine = divergenceLine + model.aggregatedTopicDivergence[i];
				if (i < model.aggregatedTopicDivergence.length - 1) {
					divergenceLine = divergenceLine + ",";
				}
			}
		}
		divergenceLine = divergenceLine + "\n";
		modelOut.write(divergenceLine);

		String originClusterSizeLine = "";
		for (int i = 0; i < model.originClusterSize.length; i++) {
			if ((filterSmallClusters && model.originClusterSize[i] > MultiModelAggregator.MIN_CLUSTER_SIZE)
					|| !filterSmallClusters) {
				originClusterSizeLine = originClusterSizeLine
						+ model.originClusterSize[i];
				if (i < model.originClusterSize.length - 1) {
					originClusterSizeLine = originClusterSizeLine + ",";
				}
			}
		}
		originClusterSizeLine = originClusterSizeLine + "\n";
		modelOut.write(originClusterSizeLine);

		int numTerms = model.terms.length;
		for (int i = 0; i < numTopics; i++) {
			if ((filterSmallClusters && model.originClusterSize[i] > MultiModelAggregator.MIN_CLUSTER_SIZE)
					|| !filterSmallClusters) {
				String oneTopicLine = "";
				for (int j = 0; j < numTerms; j++) {

					oneTopicLine = oneTopicLine + model.topicToTerm[i][j];
					if (j < numTerms - 1) {
						oneTopicLine = oneTopicLine + ",";
					}

				}
				oneTopicLine = oneTopicLine + "\n";
				modelOut.write(oneTopicLine);
			}
		}
	}

	/**
	 * Writes the class probability matrix to a file with a similar structure as
	 * for the term file
	 * 
	 * @param model
	 * @param modelFile
	 * @throws Exception
	 */
	public static void writeDocumentMatrixToFile(LightweightTopicModel model,
			File modelFile) throws Exception {

		if (!modelFile.exists()) {
			modelFile.createNewFile();
		} else {
			modelFile.delete();
			modelFile.createNewFile();
		}
		FileWriter writer = new FileWriter(modelFile);
		BufferedWriter modelOut = new BufferedWriter(writer);
		writeDocumentMatrixModel(model, modelOut);
		modelOut.close();
	}

	private static void writeDocumentMatrixModel(LightweightTopicModel model,
			BufferedWriter modelOut) throws Exception {
		String classNames = "";
		for (int i = 0; i < model.classNames.length; i++) {
			classNames = classNames + model.classNames[i];
			if (i < model.classNames.length - 1) {
				classNames = classNames + ",";
			}
		}
		classNames = classNames + "\n";
		modelOut.write(classNames);
		int numTopics = model.topicToClasses.length;
		String numTopicsStr = "" + numTopics + "\n";
		modelOut.write(numTopicsStr);

		int numClasses = model.classNames.length;
		for (int i = 0; i < numTopics; i++) {
			String oneTopicLine = "";
			for (int j = 0; j < numClasses; j++) {
				oneTopicLine = oneTopicLine + model.topicToClasses[i][j];
				if (j < numClasses - 1) {
					oneTopicLine = oneTopicLine + ",";
				}
			}
			oneTopicLine = oneTopicLine + "\n";
			modelOut.write(oneTopicLine);
		}
	}
}
