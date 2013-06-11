package sdcl.ics.uci.edu.lda.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExperimentDataUtil {

	public static final String SEPARATOR = "/";
	private static String BASE_PATH = "D:/nlopezgi/devProjects/topicLocation/NewExperimentData";
	// private static String BASE_PATH =
	// "/home/nickl/nickl-data/topicLocationProject/experimentalData";

	public static final String BUGZILLA_XMLS_DIR = BASE_PATH + SEPARATOR
			+ "bugzillaXMLs";

	public static final String MYLYN_BUGS_ATTACHMENTS_BASE_URL = "https://bugs.eclipse.org/bugs/attachment.cgi?id=";

	public static final String TASK_CONTEXT_XML_DIR = BASE_PATH + SEPARATOR
			+ "bugTaskContextXMLs";

	public static final String TASK_CONTEXT_BASE_DIR = BASE_PATH + SEPARATOR
			+ "taskContextTXTfiles";

	public static final String TASK_CONTEXT_TXT_DIR = TASK_CONTEXT_BASE_DIR
			+ SEPARATOR + "fullTaskContext";

	public static final String TASK_CONTEXT_WRITES_ONLY_DIR = TASK_CONTEXT_BASE_DIR
			+ SEPARATOR + "writesOnlyTC";

	public static final String GIT_REPOS_BASE = BASE_PATH + SEPARATOR
			+ "gitProjectRepos";
	public static final String GIT_REPOS_OUT = GIT_REPOS_BASE + SEPARATOR
			+ "repos";
	
	public static final String GIT_CALICO_REPO_OUT = GIT_REPOS_BASE + SEPARATOR
			+ "calicoRepos";

	public static final String GIT_REPOS_TEMP = GIT_REPOS_BASE + SEPARATOR
			+ "tempRepoDir";

	public static final String MODEL_BASE_DIR = BASE_PATH + SEPARATOR
			+ "LDAModels";

	public static final String MODEL_STOPLISTS_DIR = MODEL_BASE_DIR + SEPARATOR
			+ "stoplists";

	public static final String MODEL_MSTEMDATA_DIR = MODEL_BASE_DIR + SEPARATOR
			+ "mstemData";

	public static final String MODEL_DIR = MODEL_BASE_DIR + SEPARATOR
	// + "newAlphaBetaCompare";
			+ "alphaCompare2012";

	public static final String MODEL_PROBABILITY_DIR = MODEL_BASE_DIR + SEPARATOR
			+ "probabilityFiles";

	public static final String MODEL_TOPICS_DIR = MODEL_BASE_DIR + SEPARATOR
			+ "topicFiles";

	public static final String MYLYN_SAMPLE_SRC_DIR = GIT_REPOS_BASE + SEPARATOR
			+ "mylynSampleData";

	public static final String JAVA_FILE_SUFFIX = ".java";

	public static final String TASK_TO_TOPICS_BASE_DIR = BASE_PATH + SEPARATOR
			+ "taskToTopics";
	public static final String TASK_TO_TOPICS_CONFIG = TASK_TO_TOPICS_BASE_DIR
			+ SEPARATOR + "config";
	public static final String TASK_TO_TOPICS_LOGGING = TASK_TO_TOPICS_BASE_DIR
			+ SEPARATOR + "logs";

	public static final Date EXPERIMENT_START_DATE = new Date(112, 9, 10);

	public static final Date EXPERIMENT_END_DATE = new Date(1343933941000L);

	public static final String SPLITTER_PROJECT_REPO = BASE_PATH + SEPARATOR
			+ "splitterProjects";

	public static final String CALICO_LDA_MODELS_BASE_DIR = MODEL_BASE_DIR
			+ SEPARATOR + "calico";

	public static final String CALICO_LDA_MODELS_BY_MODULES_DIR = CALICO_LDA_MODELS_BASE_DIR
			+ SEPARATOR + "modulesCompare";

	public static final String CALICO_LDA_MULTI_MODELS_DIR = CALICO_LDA_MODELS_BASE_DIR
			+ SEPARATOR + "calicoMultiModelCompare";
	
	public static final String CALICO_CONFIGURATION_COMPARE = CALICO_LDA_MODELS_BASE_DIR
			+ SEPARATOR + "calicoConfCompare";

	public static final String SYNONYMS_DIR = MODEL_BASE_DIR + SEPARATOR
			+ "synonymFiles";
	public static final String CALICO_SYNONYMS = SYNONYMS_DIR + SEPARATOR
			+ "calicoSynonyms.txt";

	public static final String TOPIC_CLUSTER_DATA_DIR = BASE_PATH + SEPARATOR
			+ "topicClusterData";

	public static final String EOL = System.getProperty("line.separator");

	public static void main(String[] args) throws Exception {
		readModelFileNames();
	}

	public static void readModelFileNames() throws Exception {
		List<Long> models = ModelFileUtil.readModelFiles();
		File fileNamesDir = new File(MODEL_DIR + SEPARATOR + "files" + SEPARATOR);
		List<String> textNames;
		for (long oneModel : models) {
			textNames = new ArrayList<String>();
			File fileNames = new File(ExperimentDataUtil.MODEL_DIR + "/files/"
					+ oneModel + ".files");

			// use buffering
			InputStream file = new FileInputStream(fileNames);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);

			// deserialize the List
			textNames.addAll((List<String>) input.readObject());

			input.close();
			System.out.println("ONE MODEL SIZE IN FILES:" + oneModel + ". Size:"
					+ textNames.size());

		}
	}

}
