package sdcl.ics.uci.edu.taskToTopics.lda;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.SelectedModel;
import sdcl.ics.uci.edu.taskToTopics.lda.mstem.MStemStemmer;
import sdcl.ics.uci.edu.taskToTopics.logger.TaskToTopicLogger;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;

/**
 * Creates an LDA model for all java files inside the mylynSampleData folder.
 * 
 * @author nlopezgi
 */
public class Lda {

	private static final int CLASS_LEVEL_THREADS = 2;
	private static final int CLASS_LEVEL_ITERATIONS = 500;

	private static final int THREADS = 20;
	private static final int ITERATIONS = 2000;
	// private static final int ITERATIONS = 500;
	private static final int NUM_TOPICS = 50;
	// private static final int NUM_WORDS = 30;

	private static final String TOPIC_FILE = ExperimentDataUtil.MODEL_TOPICS_DIR
			+ ExperimentDataUtil.SEPARATOR + "topics.txt";
	private static final String MODEL_FILE = "mallet-lda";

	private static final String MODEL_FILE_SUFFIX = ".model";
	private static final String PROBABILITY_FILE = ExperimentDataUtil.MODEL_PROBABILITY_DIR
			+ ExperimentDataUtil.SEPARATOR + "probabilities.txt";

	private static final String TAB = "";

	/**
	 * These must match the GitMultiVersionExplorer values
	 */
	private static final int PERIODICITY = 30;
	public static Date CHECKOUT_DATE = new Date(112, 8, 01, 11, 59, 00);
	private static final String REV_FILE = "rev.sav";

	private String stemmerSuffix = "";

	/**
	 * These are used to resolve model dates to find the best model for a given
	 * task date
	 */
	private List<Long> modelDates = null;
	private Map<String, double[]> lastProbabilytMap = null;
	private SelectedModel lastSelectedModel = null;
	private Date nextModelDate;
	public Date currentModelDate;

	private TextProvider textProvider;
	private final ActiveStemmer activeStemmer;
	public final double selectedAlpha;
	public final double selectedBeta;

	public ActiveStemmer getActiveStemmer() {
		return activeStemmer;
	}

	public enum ActiveStemmer {
		NONE, SNOWBALL, MSTEM;
	}

	public Lda(ActiveStemmer activeStemmer, double alpha, double beta) {
		this.activeStemmer = activeStemmer;
		switch (activeStemmer) {
		case SNOWBALL:
			stemmerSuffix = "-SWB-";
			break;

		case MSTEM:
			stemmerSuffix = "-MST-";
			break;

		default:
			break;
		}
		textProvider = new TextProvider();
		selectedAlpha = alpha;
		selectedBeta = beta;
	}

	InstanceList createInstanceList(List<String> texts) throws IOException {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		// Tokenize raw strings
		pipes.add(new CharSequence2TokenSequence());

		// Split any camel case tokens
		pipes.add(new CamelCaseSplitterPipe());

		// Normalize all tokens to all lowercase
		pipes.add(new TokenSequenceLowercase());

		// Remove stopwords from a standard English stoplist.
		// options: [case sensitive] [mark deletions]
		TokenSequenceRemoveStopwords removeStopwords = new TokenSequenceRemoveStopwords(
				false, false);
		removeStopwords.addStopWords(new File(
				ExperimentDataUtil.MODEL_STOPLISTS_DIR + ExperimentDataUtil.SEPARATOR
						+ "java.txt"));
		removeStopwords.addStopWords(new File(
				ExperimentDataUtil.MODEL_STOPLISTS_DIR + ExperimentDataUtil.SEPARATOR
						+ "en.txt"));
		removeStopwords.addStopWords(new File(
				ExperimentDataUtil.MODEL_STOPLISTS_DIR + ExperimentDataUtil.SEPARATOR
						+ "otherNonDomainKeywords.txt"));
		pipes.add(removeStopwords);

		// Old pipe to remove get set, but with camel case splitter is no longer
		// necessary
		// pipes.add(new JavaActionRooterPipe());
		switch (activeStemmer) {
		case SNOWBALL:
			pipes.add(new SnowballStemmerPipe());
			break;

		case MSTEM:
			pipes.add(new MStemStemmer());
			break;

		default:
			break;
		}

		// Rather than storing tokens as strings, convert
		// them to integers by looking them up in an alphabet.
		pipes.add(new TokenSequence2FeatureSequence());

		InstanceList instanceList = new InstanceList(new SerialPipes(pipes));
		instanceList.addThruPipe(new ArrayIterator(texts));
		return instanceList;
	}

	private ParallelTopicModel createNewModel() throws IOException {
		List<String> texts = textProvider.getTexts();
		System.out.println("creating model with " + texts.size() + " texts");
		InstanceList instanceList = createInstanceList(texts);
		ParallelTopicModel model = new ParallelTopicModel(NUM_TOPICS);
		model.addInstances(instanceList);
		model.setNumThreads(THREADS);
		model.setNumIterations(ITERATIONS);
		model.estimate();
		return model;
	}

	private ParallelTopicModel createNewModel(List<File> repoDirs,
			List<String> fileNamesRet, int numTopics, double alpha, double beta)
			throws IOException {
		List<String> texts = textProvider.getTexts(repoDirs, fileNamesRet);
		System.out.println("creating model with " + texts.size() + " texts");
		InstanceList instanceList = createInstanceList(texts);
		ParallelTopicModel model = new ParallelTopicModel(numTopics, alpha, beta);

		model.addInstances(instanceList);
		model.setNumThreads(THREADS);
		model.setNumIterations(ITERATIONS);
		model.estimate();
		return model;
	}

	ParallelTopicModel getOrCreateDefaultModel() throws Exception {
		return getOrCreateModel(ExperimentDataUtil.MODEL_DIR,
				getModelFileName(NUM_TOPICS, ""));
	}

	private String getModelFileName(int numTopics, String snapshot) {
		return MODEL_FILE + "-" + snapshot + "-" + numTopics + stemmerSuffix
				+ "-A-" + selectedAlpha + "-B-" + selectedBeta + MODEL_FILE_SUFFIX;
	}

	private ParallelTopicModel getOrCreateModel(String directoryPath,
			String modelFile) throws Exception {
		File directory = new File(directoryPath);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		if (!file.exists()) {
			model = createNewModel();
			model.write(file);
		} else {
			model = ParallelTopicModel.read(file);
			textProvider.getTexts();
		}
		return model;
	}

	private ParallelTopicModel getOrCreateModel(String directoryPath,
			String modelFile, List<File> repoFolders, List<String> fileNamesRet,
			int numTopics) throws Exception {
		File directory = new File(directoryPath);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		if (!file.exists()) {
			model = createNewModel(repoFolders, fileNamesRet, numTopics,
					selectedAlpha, selectedBeta);
			model.write(file);
		} else {
			model = ParallelTopicModel.read(file);
			textProvider.getTexts(repoFolders, fileNamesRet);
		}
		return model;
	}

	/**
	 * Returns a map with file names and list of probabilities for a model closest
	 * and before the given date
	 */
	public Map<String, double[]> getProbabilitiesForModelBeforeDate(
			Date issueDate, int numTopics) throws Exception {
		if (lastProbabilytMap != null && nextModelDate != null
				&& issueDate.before(nextModelDate)) {
			// try to reuse it
			return lastProbabilytMap;
		}
		// System.out.println("could not reuse probability map, get a new one");
		File directory = new File(ExperimentDataUtil.MODEL_DIR);
		List<String> textNames = new ArrayList<String>();
		if (nextModelDate == null) {
			nextModelDate = ExperimentDataUtil.EXPERIMENT_START_DATE;
		}
		String modelFile = resolveModelFileAndTextNames(issueDate, textNames,
				nextModelDate, numTopics, true, new SelectedModel());
		System.out.println("SELECTED MODEL:" + modelFile);

		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		model = ParallelTopicModel.read(file);
		TaskToTopicLogger.logTopics(model, modelFile);
		Map<String, double[]> ret = createProbabilityMap(model, textNames);
		lastProbabilytMap = ret;

		return ret;
	}

	/**
	 * Returns a map with file names and list of probabilities for a model closest
	 * and before the given date
	 */
	public Map<String, double[]> getProbabilitiesForModelBeforeDate(
			Date issueDate, int numTopics, SelectedModel selectedModel)
			throws Exception {
		if (lastProbabilytMap != null && nextModelDate != null
				&& issueDate.before(nextModelDate)) {
			// try to reuse it
			selectedModel.setSelectedModel(lastSelectedModel.getSelectedModel());
			return lastProbabilytMap;
		}
		// System.out.println("could not reuse probability map, get a new one");
		File directory = new File(ExperimentDataUtil.MODEL_DIR);
		List<String> textNames = new ArrayList<String>();
		if (nextModelDate == null) {
			nextModelDate = new Date(112, 9, 10);
		}
		String modelFile = resolveModelFileAndTextNames(issueDate, textNames,
				nextModelDate, numTopics, true, selectedModel);

		System.out.println("SELECTED MODEL:" + modelFile);

		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		model = ParallelTopicModel.read(file);
		TaskToTopicLogger.logTopics(model, modelFile);
		Map<String, double[]> ret = createProbabilityMap(model, textNames);
		lastProbabilytMap = ret;

		return ret;
	}

	/**
	 * Returns a map with file names and list of probabilities for a model closest
	 * and after the given date
	 */
	public Map<String, double[]> getProbabilitiesForModelAfterDate(
			Date issueDate, int numTopics) throws Exception {
		if (lastProbabilytMap != null && currentModelDate != null
				&& issueDate.before(currentModelDate)) {
			// try to reuse it
			return lastProbabilytMap;
		}
		// System.out.println("could not reuse probability map, get a new one");
		File directory = new File(ExperimentDataUtil.MODEL_DIR);
		List<String> textNames = new ArrayList<String>();
		if (currentModelDate == null) {
			currentModelDate = new Date(112, 9, 10);
		}
		String modelFile = resolveModelFileAndTextNames(issueDate, textNames,
				currentModelDate, numTopics, false, new SelectedModel());
		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		model = ParallelTopicModel.read(file);
		Map<String, double[]> ret = createProbabilityMap(model, textNames);
		lastProbabilytMap = ret;
		return ret;
	}

	/**
	 * Finds the name of the appropriate model file. Reads the file containing all
	 * fileNames for this model and leaves the textNames in the list
	 */
	private String resolveModelFileAndTextNames(Date issueDate,
			List<String> textNames, Date nextModelDate, int numTopics,
			boolean before, SelectedModel selectedModel) throws Exception {
		if (modelDates == null) {
			modelDates = readModelFiles();
		}

		long chosenModelDate = 0;
		long issueDateLong = issueDate.getTime() / 1000;
		for (Long oneModelDate : modelDates) {
			if (before) {
				if (oneModelDate > chosenModelDate && oneModelDate < issueDateLong) {
					chosenModelDate = oneModelDate;
				}
				if (oneModelDate > issueDateLong) {
					nextModelDate.setTime(oneModelDate * 1000);
					break;
				}
			} else {
				if (oneModelDate > chosenModelDate && oneModelDate > issueDateLong) {
					chosenModelDate = oneModelDate;
					currentModelDate.setTime(oneModelDate * 1000);
					break;
				}
			}
		}
		if (chosenModelDate == 0) {
			throw new Exception("Could not find a model close to " + issueDate);
		}
		if (currentModelDate == null) {
			currentModelDate = new Date();
		}
		selectedModel.setSelectedModel(chosenModelDate);
		if (lastSelectedModel == null) {
			lastSelectedModel = new SelectedModel();
		}
		lastSelectedModel.setSelectedModel(chosenModelDate);
		currentModelDate.setTime(chosenModelDate * 1000);
		String chosenStr = Long.toString(chosenModelDate);
		String modelFile = getModelFileName(numTopics, chosenStr);

		// Now get the file names for this model
		File fileNames = new File(ExperimentDataUtil.MODEL_DIR + "/files/"
				+ chosenModelDate + ".files");
		try {
			// use buffering
			InputStream file = new FileInputStream(fileNames);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				// deserialize the List
				textNames.addAll((List<String>) input.readObject());
			} finally {
				input.close();
			}
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			throw new Exception("Error reading file" + fileNames.getAbsolutePath());
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new Exception("Error reading file" + fileNames.getAbsolutePath());
		}
		return modelFile;
	}

	public List<String> getModelTextNames(long modelDate) throws Exception {
		List<String> textNames = new ArrayList<String>();
		// Now get the file names for the model
		File fileNames = new File(ExperimentDataUtil.MODEL_DIR + "/files/"
				+ modelDate + ".files");
		try {
			// use buffering
			InputStream file = new FileInputStream(fileNames);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				// deserialize the List
				textNames.addAll((List<String>) input.readObject());
			} finally {
				input.close();
			}
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			throw new Exception("Error reading file" + fileNames.getAbsolutePath());
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new Exception("Error reading file" + fileNames.getAbsolutePath());
		}
		return textNames;
	}

	public void printTopics() throws Exception {
		ParallelTopicModel model = getOrCreateDefaultModel();
		Alphabet alphabet = model.getAlphabet();
		for (TreeSet<IDSorter> set : model.getSortedWords()) {
			System.out.print("TOPIC, ");
			for (IDSorter s : set) {
				System.out.print(alphabet.lookupObject(s.getID()) + ", ");
			}
			System.out.println();
		}
	}

	public void createTopicFile() throws Exception {
		ParallelTopicModel model = getOrCreateDefaultModel();
		Object[][] topics = model.getTopWords(20);

		File topicFile = new File(TOPIC_FILE);
		if (!topicFile.exists()) {
			topicFile.createNewFile();
		} else {
			topicFile.delete();
			topicFile.createNewFile();
		}
		FileWriter writer = new FileWriter(topicFile);
		BufferedWriter out = new BufferedWriter(writer);
		for (int i = 0; i < topics.length; i++) {
			out.write("TOPIC,");
			for (int j = 0; j < topics[i].length; j++) {
				out.write(topics[i][j] + "," + TAB + "");
			}
			out.write("\n");
		}
		out.close();
		System.out.println("done writing topic file");
	}

	public void createTopicFile(String directoryPath, String modelFile)
			throws Exception {
		ParallelTopicModel model = getOrCreateModel(directoryPath, modelFile);
		Object[][] topics = model.getTopWords(20);

		File topicFile = new File(TOPIC_FILE);
		if (!topicFile.exists()) {
			topicFile.createNewFile();
		} else {
			topicFile.delete();
			topicFile.createNewFile();
		}
		FileWriter writer = new FileWriter(topicFile);
		BufferedWriter out = new BufferedWriter(writer);
		for (int i = 0; i < topics.length; i++) {
			out.write("TOPIC,");
			for (int j = 0; j < topics[i].length; j++) {
				out.write(topics[i][j] + "," + TAB + "");
			}
			out.write("\n");
		}
		out.close();
		System.out.println("done writing topic file");
	}

	public void createProbabilityFile() throws Exception {
		ParallelTopicModel model = getOrCreateDefaultModel();
		File topicFile = new File(PROBABILITY_FILE);
		if (!topicFile.exists()) {
			topicFile.createNewFile();
		} else {
			topicFile.delete();
			topicFile.createNewFile();
		}
		FileWriter writer = new FileWriter(topicFile);
		BufferedWriter out = new BufferedWriter(writer);
		System.out.println("numTexts" + textProvider.getNumTexts());
		for (int i = 0; i < textProvider.getNumTexts(); i++) {
			out.write("Instance " + i + ",");
			out.write(textProvider.getNameForTextId(i));
			double[] probabilities = model.getTopicProbabilities(i);
			for (int j = 0; j < probabilities.length; j++) {
				out.write("," + TAB + "" + probabilities[j]);
			}
			out.write("\n");
		}
		out.close();
		System.out.println("done writing probabilities file");
	}

	/**
	 * Creates a map with the probabilities of each java file (keys in return map)
	 * being described by each topic (values in return map)
	 */
	public Map<String, double[]> createProbabilityMap() throws Exception {
		Map<String, double[]> ret = new HashMap<String, double[]>();
		ParallelTopicModel model = getOrCreateDefaultModel();

		for (int i = 0; i < textProvider.getNumTexts(); i++) {
			if (ret.containsKey(textProvider.getNameForTextId(i))) {

				// System.err.println("File '" + textProvider.getNameForTextId(i)
				// + "' has a duplicate name, ignoring");
			} else {
				if (model.getTopicProbabilities(i) == null) {
					System.out.println("very strange");
				}
				ret.put(textProvider.getNameForTextId(i),
						model.getTopicProbabilities(i));
			}
		}
		// System.out.println("done creating probabilities map");
		return ret;
	}

	/**
	 * Creates a map with the probabilities of each java file (keys in return map)
	 * being described by each topic (values in return map)
	 */
	public Map<String, double[]> createProbabilityMap(ParallelTopicModel model,
			List<String> textNames) {
		Map<String, double[]> ret = new HashMap<String, double[]>();

		for (int i = 0; i < textNames.size(); i++) {
			if (ret.containsKey(textNames.get(i))) {
				// System.err.println("File '" + textNames.get(i)
				// + "' has a duplicate name, ignoring");
			} else {
				if (model.getTopicProbabilities(i) == null) {
					System.out.println("very strange");
				}
				ret.put(textNames.get(i), model.getTopicProbabilities(i));
			}
		}
		// System.out.println("done creating probabilities map");
		return ret;
	}

	public static void main(String[] args) throws Exception {
		// Lda lda = new Lda(ActiveStemmer.NONE);
		// lda.printTopics();
		// lda.createTopicFile();
		// lda.createProbabilityFile();
		// lda.createProbabilityMap();
		// System.out.println("done creating probability map");

		// double[] alphaValues = { 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75,
		// 1.0, 1.25, 1.5 };
		// double[] alphaValues = { 0.25, 0.5, 0.75, 1.0, 1.25, 1.5 };
		// double[] betaValues = { 1.5, 2.0, 2.25, 2.5, 2.75, 3.0 };
		// double[] betaValues = { 0.1, 0.5, 0.75, 1.0, 1.25, 1.5 };
		// double[] betaValues = {1.75};
		// double[] alphaValues = {0.175, 0.375};
		// double[] betaValues = { 2.25, 2.5, 2.75, 3.0 };
		// int[] sizes = { 25, 50, 75, 100, 125, 150, 175, 200, 225, 250 };
		int[] sizes = { 100 };
		// int[] sizes = { 150 };
		// SELECTED OPTIMUM CONFIGURATION
		// double[] alphaValues = { 0.025 };
		// double[] alphaValues = { 0.375};
		// double[] betaValues = { 1.5 };
		// double[] betaValues = { 0.25 };

		// // int[] sizes = { 75, 100, 125, 150, 175, 200, 225, 250, 275, 300 };
		// for (int oneTopicModelSize : sizes) {
		// for (double alpha : alphaValues) {
		// for (double beta : betaValues) {
		// if (!(alpha == 0.25 && beta == 0.1)) {
		// Lda lda = new Lda(ActiveStemmer.NONE, alpha, beta);
		// lda.createModelsForHistory(oneTopicModelSize);
		//
		// // Lda lda = new Lda(ActiveStemmer.SNOWBALL, alpha, beta);
		// // lda.createModelsForHistory(oneTopicModelSize);
		// //
		// // lda = new Lda(ActiveStemmer.MSTEM, alpha, beta);
		// // lda.createModelsForHistory(oneTopicModelSize);
		// }
		// }
		// }
		// }

		// double[] alphaValues2 = { 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75,
		double[] alphaValues2 = { 0.75, 1.0, 1.25, 1.5 };
		double[] betaValues2 = { 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25,
				2.5, 2.75, 3.0 };
		for (int oneTopicModelSize : sizes) {
			for (double alpha : alphaValues2) {
				for (double beta : betaValues2) {
					if (alpha != 0.75 || (alpha == 0.75 && beta >= 0.75)) {
						System.out.println("STARTING RUN WITH ALPHA:" + alpha
								+ ". and BETA:" + beta);
						Lda lda = new Lda(ActiveStemmer.NONE, alpha, beta);
						lda.createModelsForHistory(oneTopicModelSize);
					}
				}
			}
		}

		// lda.createModelsForHistory(400);
		// lda.createTopicFile(ExperimentDataUtil.MODEL_DIR,
		// "mallet-lda-1343933940-100.model");

		// lda.readModelFiles();
		// lda = new Lda(ActiveStemmer.SNOWBALL, 1, 0.1);
		// lda.createModelsForHistory(100);
		//
		// lda = new Lda(ActiveStemmer.MSTEM, 1, 0.1);
		// lda.createModelsForHistory(100);
	}

	public void createModelsForHistory(int numtopics) throws Exception {
		List<String> repos = getSubdirectories(ExperimentDataUtil.GIT_REPOS_OUT);
		Map<String, List<Date>> repoToDatesMap = new HashMap<String, List<Date>>();
		for (String oneRepo : repos) {
			File revisionsFile = new File(ExperimentDataUtil.GIT_REPOS_OUT
					+ ExperimentDataUtil.SEPARATOR + oneRepo
					+ ExperimentDataUtil.SEPARATOR + REV_FILE);
			List<Date> revisions = new ArrayList<Date>();
			if (revisionsFile.exists()) {
				revisions = readRevisionsFile(ExperimentDataUtil.GIT_REPOS_OUT
						+ ExperimentDataUtil.SEPARATOR + oneRepo
						+ ExperimentDataUtil.SEPARATOR + REV_FILE);
				// System.out.println("found " + revisions.size() +
				// " revisions for repo "
				// + oneRepo);
				repoToDatesMap.put(oneRepo, revisions);
			} else {
				System.err.println("could not find revision file:"
						+ revisionsFile.getAbsolutePath());
			}
		}

		long periodicity = (long) PERIODICITY * 1000 * 24 * 60 * 60;
		Date nextSnapshot = new Date(CHECKOUT_DATE.getTime() - periodicity);
		// 104 is 2004, I have bugs starting after this time only
		while (nextSnapshot.getYear() >= 104) {
			// System.out.println("****************MODEL AT :" +
			// nextSnapshot.getTime()
			// + "---" + nextSnapshot + "****************");
			List<File> repoFolders = new ArrayList<File>();
			for (String oneRepo : repos) {
				File nextDir = getNextRevisionDir(repoToDatesMap.get(oneRepo),
						nextSnapshot,
						(ExperimentDataUtil.GIT_REPOS_OUT + ExperimentDataUtil.SEPARATOR
								+ oneRepo + ExperimentDataUtil.SEPARATOR));
				if (nextDir != null) {
					repoFolders.add(nextDir);
				}
			}
			createModelAtSnapshot(nextSnapshot, repoFolders, numtopics);
			nextSnapshot = new Date(nextSnapshot.getTime() - periodicity);
			if (repoFolders.isEmpty()) {
				break;
			}
		}
	}

	private void createModelAtSnapshot(Date snapshot, List<File> repoFolders,
			int numTopics) throws Exception {
		System.out.println("creating a new model for date: " + snapshot);
		System.out.println("model includes the following dirs: ");
		for (File oneFolder : repoFolders) {
			System.out.println(oneFolder.getAbsolutePath());
		}
		List<String> fileNamesRet = new ArrayList<String>();
		String snapshotStr = Long.toString((snapshot.getTime() / 1000));
		getOrCreateModel(ExperimentDataUtil.MODEL_DIR,
				getModelFileName(numTopics, snapshotStr), repoFolders, fileNamesRet,
				numTopics);
		// save the fileNamesRet object to recover it later on.
		saveFileNames(fileNamesRet, snapshot);
	}

	private void saveFileNames(List<String> fileNamesRet, Date date) {
		try {
			// use buffering
			OutputStream file = new FileOutputStream(ExperimentDataUtil.MODEL_DIR
					+ "/files/" + date.getTime() / 1000 + ".files");
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(fileNamesRet);
			} finally {
				output.close();
			}
		} catch (IOException ex) {
			System.err.println("COULD NOT SAVE FILE FOR MODEL " + date.getTime());
		}

	}

	private List<Date> readRevisionsFile(String fileName) throws IOException {
		List<Date> ret = new ArrayList<Date>();
		FileInputStream fstream = new FileInputStream(fileName);

		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			String[] data = strLine.split(",");
			Date date = new Date(Long.parseLong(data[0]) * 1000);
			ret.add(date);
		}

		fstream.close();
		return ret;
	}

	private File getNextRevisionDir(List<Date> revisions, Date nextSnapshotDate,
			String repoDir) {
		Date closestDate = null;
		for (Date oneRev : revisions) {
			if (closestDate == null || oneRev.after(closestDate)) {
				closestDate = oneRev;
			}
		}
		if (closestDate != null) {
			File ret = new File(repoDir + "/" + (closestDate.getTime() / 1000));
			if (ret.exists()) {
				if (closestDate.after(nextSnapshotDate)) {
					revisions.remove(closestDate);
				} // else {
				// System.out.println("DID NOT REMOVE DATE " + closestDate
				// + " for repo " + repoDir + " since it is before "
				// + nextSnapshotDate);
				// }
				return ret;
			}
		} // else {
		// System.out.println("could not find a revisions close to date"
		// + nextSnapshotDate + "for repo " + repoDir);
		// }
		return null;
	}

	public List<String> getSubdirectories(String dir) {
		List<String> ret = new ArrayList<String>();
		File parentDir = new File(dir);
		if (parentDir.isDirectory()) {
			File[] listOfFiles = parentDir.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isDirectory()) {
					ret.add(listOfFiles[i].getName());
				}
			}
		}
		return ret;
	}

	public List<Long> readModelFiles() {
		List<Long> ret = new ArrayList<Long>();
		File modelDir = new File(ExperimentDataUtil.MODEL_DIR);
		if (modelDir.isDirectory()) {
			File[] listOfFiles = modelDir.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].getName().startsWith(MODEL_FILE)) {
					String oneModelDate = listOfFiles[i].getName();
					oneModelDate = oneModelDate.substring(
							oneModelDate.indexOf(MODEL_FILE) + MODEL_FILE.length() + 1,
							oneModelDate.indexOf('.'));
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
		Date lastDate = new Date();
		lastDate.setTime(ret.get(ret.size() - 1) * 1000);
		System.out.println("last date found" + lastDate);
		return ret;
	}

}
