package sdcl.ics.uci.edu.taskToTopics.lda;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sdcl.ics.uci.edu.lda.moduleSplitter.ModuleSplitter;
import sdcl.ics.uci.edu.lda.moduleSplitter.ModuleSplitter.Project;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModelFileUtil;
import sdcl.ics.uci.edu.lda.util.ModuleData;
import sdcl.ics.uci.edu.lda.util.Term;
import sdcl.ics.uci.edu.lda.util.TopModuleData;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class CalicoLda {

	public static final CalicoConfiguration DEFAULT_CONFIGURATION = CalicoConfiguration
			.getSampleConfiguration(1.0, 0.1, 50, 5, 2000);

	public static final CalicoConfiguration MYLYN_CONFIGURATION = CalicoConfiguration
			.getSampleConfiguration(0.025, 1.5, 50, 5, 2000);

	private static String CALICO_MODEL_FILE = "calico.model";

	private static int MIN_SUB_TOPICS = 3;

	public static int NUM_TOPICS = 60;
	private static int ITERATIONS = 500;
	private static int THREADS = 5;
	private static final boolean REMOVE_NON_DOMAIN_KEYWORDS = true;
	private static final boolean USE_DEFAULT_AB = false;

	private int calicoSize = 1;

	private ModuleData calicoFull;
	private List<ModuleData> modules;

	public String getCalicoModelFilePath() {
		return ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR
				+ ExperimentDataUtil.SEPARATOR + CALICO_MODEL_FILE;
	}

	public CalicoLda() throws Exception {
		File file = new File(ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR);
		if (file.isDirectory()) {
			if (file.listFiles().length == 0) {
				calicoFull = createCalicoFullModel(
						ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR, NUM_TOPICS,
						ITERATIONS, THREADS);
				modules = createCalicoModels(
						ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR, NUM_TOPICS,
						ITERATIONS, THREADS, calicoSize);
			} else {
				calicoFull = readCalicoFullModel(ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR);
				modules = readCalicoModels(ExperimentDataUtil.CALICO_LDA_MODELS_BY_MODULES_DIR);
			}
		}
	}

	public ModuleData readCalicoFullModel(String modelsRootDir) throws Exception {
		File file = new File(modelsRootDir, CALICO_MODEL_FILE);
		ParallelTopicModel ret = ParallelTopicModel.read(file);
		TopModuleData moduleData = new TopModuleData(
				ModuleSplitter.CALICO_CLIENT_SRC);
		moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_CLIENT_SRC);
		moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_SERVER_SRC);
		moduleData.setModel(ret);
		return (ModuleData) moduleData;
	}

	private ModuleData createCalicoFullModel(String modelsRootDir, int numTopics,
			int iterations, int threads) throws Exception {
		ModuleSplitter moduleSplitter = new ModuleSplitter();
		ModuleData client = moduleSplitter.getCalicoClient();
		ModuleData server = moduleSplitter.getCalicoServer();
		List<String> fileNames = new ArrayList<String>();
		List<String> texts = client.getTexts(fileNames);
		texts.addAll(server.getTexts(fileNames));
		calicoSize = fileNames.size();
		ParallelTopicModel ret = createModelFile(modelsRootDir, CALICO_MODEL_FILE,
				texts, numTopics, iterations, threads);
		TopModuleData moduleData = new TopModuleData(
				ModuleSplitter.CALICO_CLIENT_SRC);
		moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_CLIENT_SRC);
		moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_SERVER_SRC);
		moduleData.setModel(ret);
		return (ModuleData) moduleData;
	}

	private ModuleData createCalicoFullModelWithLDAParameters(
			String modelsRootDir, int numTopics, int iterations, int threads,
			double alpha, double beta, String modelFile) throws Exception {
		ModuleSplitter moduleSplitter = new ModuleSplitter();
		ModuleData client = moduleSplitter.getCalicoClient();
		ModuleData server = moduleSplitter.getCalicoServer();
		List<String> fileNames = new ArrayList<String>();
		List<String> texts = client.getTexts(fileNames);
		texts.addAll(server.getTexts(fileNames));
		calicoSize = fileNames.size();
		ParallelTopicModel ret = createModelFileWithLDAParameters(modelsRootDir,
				modelFile, texts, numTopics, iterations, threads, alpha, beta);
		TopModuleData moduleData = new TopModuleData(
				ModuleSplitter.CALICO_CLIENT_SRC);
		moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_CLIENT_SRC);
		moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_SERVER_SRC);
		moduleData.setModel(ret);
		return (ModuleData) moduleData;
	}

	public List<String> getCalicoFullFileNames() {
		ModuleSplitter moduleSplitter = new ModuleSplitter();
		ModuleData client = moduleSplitter.getCalicoClient();
		ModuleData server = moduleSplitter.getCalicoServer();
		List<String> fileNames = new ArrayList<String>();
		List<String> texts = client.getTexts(fileNames);
		texts.addAll(server.getTexts(fileNames));
		return fileNames;
	}

	private List<ModuleData> createCalicoModels(String modelsRootDir,
			int numTotalTopics, int iterations, int threads, int calicoSize)
			throws Exception {
		List<ModuleData> ret = new ArrayList<ModuleData>();
		ModuleSplitter moduleSplitter = new ModuleSplitter();
		List<ModuleData> modules = moduleSplitter.getProjectModules(Project.CALICO);
		List<String> fileNames = null;
		List<String> texts;
		for (ModuleData oneModule : modules) {
			fileNames = new ArrayList<String>();
			texts = oneModule.getTexts(fileNames);
			int subModelSize = MIN_SUB_TOPICS + numTotalTopics
					* oneModule.getNumFiles() / calicoSize;
			oneModule.setModel(createModelFile(modelsRootDir,
					oneModule.getModuleName() + ".model", texts, subModelSize,
					iterations, threads));
			oneModule.setModelSize(subModelSize);
			ret.add(oneModule);
		}
		return ret;
	}

	public List<ModuleData> readCalicoModels(String modelsRootDir)
			throws Exception {
		List<ModuleData> ret = new ArrayList<ModuleData>();
		ModuleSplitter moduleSplitter = new ModuleSplitter();
		List<ModuleData> modules = moduleSplitter.getProjectModules(Project.CALICO);
		for (ModuleData oneModule : modules) {
			File file = new File(modelsRootDir, oneModule.getModuleName() + ".model");
			ParallelTopicModel model = ParallelTopicModel.read(file);
			oneModule.setModel(model);
			oneModule.setModelSize(model.getNumTopics());
			ret.add(oneModule);
		}
		return ret;
	}

	private ParallelTopicModel createModelFile(String directoryPath,
			String modelFile, List<String> texts, int numTopics, int iterations,
			int threads) throws Exception {
		File directory = new File(directoryPath);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		if (file.exists()) {
			file.delete();
		}
		if (!file.exists()) {
			model = createNewModel(texts, numTopics, iterations, threads);
			model.write(file);
		}
		return model;
	}

	private ParallelTopicModel createModelFileWithLDAParameters(
			String directoryPath, String modelFile, List<String> texts,
			int numTopics, int iterations, int threads, double alpha, double beta)
			throws Exception {
		File directory = new File(directoryPath);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File file = new File(directory, modelFile);
		ParallelTopicModel model = null;
		if (file.exists()) {
			file.delete();
		}
		if (!file.exists()) {
			model = createNewModelWithLDAParameters(texts, numTopics, iterations,
					threads, alpha, beta);
			model.write(file);
		}
		return model;
	}

	private ParallelTopicModel createNewModel(List<String> texts, int numTopics,
			int iterations, int threads) throws IOException {

		System.out.println("creating model with " + texts.size() + " texts");
		InstanceList instanceList = createInstanceList(texts);
		ParallelTopicModel model;
		if (USE_DEFAULT_AB) {
			model = new ParallelTopicModel(numTopics);
		} else {
			model = new ParallelTopicModel(numTopics,
					ModelFileUtil.MYLYN_DEFINED_ALPHA, ModelFileUtil.MYLYN_DEFINED_BETA);
		}

		model.addInstances(instanceList);
		model.setNumThreads(threads);
		model.setNumIterations(iterations);
		model.estimate();
		return model;
	}

	private ParallelTopicModel createNewModelWithLDAParameters(
			List<String> texts, int numTopics, int iterations, int threads,
			double alpha, double beta) throws IOException {

		System.out.println("creating model with " + texts.size() + " texts");
		InstanceList instanceList = createInstanceList(texts);
		ParallelTopicModel model;

		model = new ParallelTopicModel(numTopics, alpha, beta);

		model.addInstances(instanceList);
		model.setNumThreads(threads);
		model.setNumIterations(iterations);
		model.estimate();
		return model;
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
		if (REMOVE_NON_DOMAIN_KEYWORDS) {
			removeStopwords.addStopWords(new File(
					ExperimentDataUtil.MODEL_STOPLISTS_DIR + ExperimentDataUtil.SEPARATOR
							+ "calicoNDK.txt"));
		}

		pipes.add(removeStopwords);

		SynonymHandler synonymPipe = new SynonymHandler(Project.CALICO);
		pipes.add(synonymPipe);

		// Rather than storing tokens as strings, convert
		// them to integers by looking them up in an alphabet.
		pipes.add(new TokenSequence2FeatureSequence());

		InstanceList instanceList = new InstanceList(new SerialPipes(pipes));
		instanceList.addThruPipe(new ArrayIterator(texts));
		return instanceList;
	}

	public ModuleData getCalicoFull() {
		return calicoFull;
	}

	public List<ModuleData> getModules() {
		return modules;
	}

	public List<ModuleData> getMultipleCalicoModels(
			CalicoConfiguration configuration) throws Exception {
		boolean modelsExist = false;
		// create the configurations for the models
		List<ModuleData> ret = new ArrayList<ModuleData>();

		List<CalicoConfiguration> calicoConfs = getMultipleIdenticalCalicoConfigurations(configuration);
		// List<CalicoConfiguration> calicoConfs = getCalicoConfigurations();

		// check if the models are in the dir.
		// simple solution: If there are enough files in the dir then they are there
		// (will fail later if they are not there)
		String multiModelRootDir = ExperimentDataUtil.CALICO_LDA_MULTI_MODELS_DIR;
		File multiModelRootDirFile = new File(multiModelRootDir);

		if (multiModelRootDirFile.listFiles().length == calicoConfs.size()) {
			modelsExist = true;
		}
		// if not create them
		if (!modelsExist) {
			for (CalicoConfiguration conf : calicoConfs) {
				createCalicoFullModelWithLDAParameters(multiModelRootDir,
						conf.numTopics, conf.iterations, conf.threads, conf.alpha,
						conf.beta, conf.fileName);
			}
		}

		// Read them
		for (CalicoConfiguration conf : calicoConfs) {
			File file = new File(multiModelRootDir, conf.fileName);
			ParallelTopicModel onePTM = ParallelTopicModel.read(file);
			TopModuleData moduleData = new TopModuleData(
					ModuleSplitter.CALICO_CLIENT_SRC);
			moduleData.setModuleName(conf.confName);
			moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_CLIENT_SRC);
			moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_SERVER_SRC);
			moduleData.setModel(onePTM);
			moduleData
					.setNonDefaultFilePath(ExperimentDataUtil.CALICO_LDA_MULTI_MODELS_DIR
							+ ExperimentDataUtil.SEPARATOR + conf.fileName);
			ret.add(moduleData);
		}
		// return them
		return ret;
	}

	public List<ModuleData> getMultipleCalicoModels(
			CalicoConfiguration configuration, String configurationDir)
			throws Exception {
		boolean modelsExist = false;
		// create the configurations for the models
		List<ModuleData> ret = new ArrayList<ModuleData>();

		List<CalicoConfiguration> calicoConfs = getMultipleIdenticalCalicoConfigurations(configuration);
		// List<CalicoConfiguration> calicoConfs = getCalicoConfigurations();

		// check if the models are in the dir.
		// simple solution: If there are enough files in the dir then they are there
		// (will fail later if they are not there)
		String multiModelRootDir = configurationDir;
		File multiModelRootDirFile = new File(multiModelRootDir);

		if (multiModelRootDirFile.listFiles().length == calicoConfs.size()) {
			modelsExist = true;
		}
		// if not create them
		if (!modelsExist) {
			for (CalicoConfiguration conf : calicoConfs) {
				createCalicoFullModelWithLDAParameters(multiModelRootDir,
						conf.numTopics, conf.iterations, conf.threads, conf.alpha,
						conf.beta, conf.fileName);
			}
		}

		// Read them
		for (CalicoConfiguration conf : calicoConfs) {
			File file = new File(multiModelRootDir, conf.fileName);
			ParallelTopicModel onePTM = ParallelTopicModel.read(file);
			TopModuleData moduleData = new TopModuleData(
					ModuleSplitter.CALICO_CLIENT_SRC);
			moduleData.setModuleName(conf.confName);
			moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_CLIENT_SRC);
			moduleData.addTopModuleSourceFolder(ModuleSplitter.CALICO_SERVER_SRC);
			moduleData.setModel(onePTM);
			moduleData.setNonDefaultFilePath(configurationDir
					+ ExperimentDataUtil.SEPARATOR + conf.fileName);
			ret.add(moduleData);
		}
		// return them
		return ret;
	}

	private List<CalicoConfiguration> getCalicoConfigurations() {
		List<CalicoConfiguration> calicoConfs = new ArrayList<CalicoConfiguration>();
		String fileNamePrefix = "calico-";
		String fileNameSuffix = ".model";
		double[] alphas = { 0.1, 0.25, 0.5, 0.75, 1.0 };
		double[] betas = { 0.1, 0.25, 0.5, 0.75, 1.0 };
		int[] numTopics = { 50, 75, 100 };
		int[] iterations = { 500, 1000, 2000 };
		int[] threads = { 5 };
		for (double alpha : alphas) {
			for (double beta : betas) {
				for (int oneNumTopics : numTopics) {
					for (int oneIterations : iterations) {
						for (int oneThreads : threads) {
							String modelName = generateModelName(alpha, beta, oneThreads,
									oneNumTopics, oneIterations);
							calicoConfs.add(new CalicoConfiguration(fileNamePrefix
									+ modelName + fileNameSuffix, modelName, alpha, beta,
									oneThreads, oneNumTopics, oneIterations));
						}
					}
				}
			}
		}
		return calicoConfs;
	}

	private List<CalicoConfiguration> getMultipleIdenticalCalicoConfigurations(
			CalicoConfiguration sampleConfiguration) {
		List<CalicoConfiguration> calicoConfs = new ArrayList<CalicoConfiguration>();
		String fileNamePrefix = "calico-";
		String fileNameSuffix = ".model";

		for (int i = 0; i < 10; i++) {
			String modelName = "-model" + i + "-";
			calicoConfs.add(new CalicoConfiguration(fileNamePrefix + modelName
					+ fileNameSuffix, modelName, sampleConfiguration.alpha,
					sampleConfiguration.beta, sampleConfiguration.threads,
					sampleConfiguration.numTopics, sampleConfiguration.iterations));
		}
		return calicoConfs;
	}

	private String generateModelName(double alpha, double beta, int threads,
			int numTopics, int iterations) {
		return "A-" + alpha + "-B-" + beta + "-N-" + numTopics + "-I-" + iterations
				+ "-T-" + threads;
	}

	public static class CalicoConfiguration {
		protected CalicoConfiguration(String fileName, String confName,
				double alpha, double beta, int threads, int numTopics, int iterations) {
			this.fileName = fileName;
			this.confName = confName;
			this.alpha = alpha;
			this.beta = beta;
			this.threads = threads;
			this.numTopics = numTopics;
			this.iterations = iterations;
		}

		public static CalicoConfiguration getSampleConfiguration(double alpha,
				double beta, int threads, int numTopics, int iterations) {
			return new CalicoConfiguration(alpha, beta, threads, numTopics,
					iterations);
		}

		private CalicoConfiguration(double alpha, double beta, int threads,
				int numTopics, int iterations) {
			this.alpha = alpha;
			this.beta = beta;
			this.threads = threads;
			this.numTopics = numTopics;
			this.iterations = iterations;
		}

		public String fileName;
		public String confName;
		public double alpha;
		public double beta;
		public int threads;
		public int numTopics;
		public int iterations;

		public String createConfigDirNameFromSample() {
			return new String("a-" + alpha + "-b-" + beta + "-n-" + numTopics + "-i-"
					+ iterations);
		}
	}

	public static void main(String[] args) throws Exception {
		CalicoLda lda = new CalicoLda();
		lda.printTopicWordWeights();
	}

	public void printTopicWordWeights() throws Exception {
		ModuleData fullCalico = getCalicoFull();
		ParallelTopicModel model = fullCalico.getLDAModel();

		File testOut = new File(ExperimentDataUtil.CALICO_LDA_MODELS_BASE_DIR
				+ ExperimentDataUtil.SEPARATOR + "terms.txt");
		if (testOut.exists()) {
			testOut.delete();
		}
		testOut.createNewFile();
		System.out.println("PRINTING TOPIC WORD WEIGHTS");
		model.printTopicWordWeights(testOut);
		System.out.println("PRINTING TOP WORDS");
		// model.printTopWords(testOut, 20, true);
		System.out.println(model.displayTopWords(20, true));
		System.out.println("*");
		System.out.println("*");
		System.out.println("*");
		System.out.println("*");
		System.out.println("*");
		System.out.println("*");
		System.out.println("*");
		Map<Integer, List<Term>> terms = ModelFileUtil.getModelTerms(model, 20);
		for (Integer topic : terms.keySet()) {
			System.out.println("TOPIC " + topic);
			for (Term term : terms.get(topic)) {
				System.out.println(term.getTerm() + "\t" + term.getWeight());
			}
		}
	}

}
