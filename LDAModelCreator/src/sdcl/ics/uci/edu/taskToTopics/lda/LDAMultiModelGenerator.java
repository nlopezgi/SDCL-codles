package sdcl.ics.uci.edu.taskToTopics.lda;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sdcl.ics.uci.edu.lda.moduleSplitter.ModuleSplitter.Project;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.taskToTopics.lda.mstem.MStemStemmer;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

/**
 * Given a project (a directory) this class project obtains all java files and
 * creates multiple topic models (X models) using random seeds.
 * 
 * @author nlopezgi
 * 
 */
public class LDAMultiModelGenerator {

	private TextProvider textProvider;
	private static final boolean REMOVE_NON_DOMAIN_KEYWORDS = true;

	public LDAMultiModelGenerator() {
		textProvider = new TextProvider();
	}

	public List<ParallelTopicModel> createMultipleModels(String dirPath,
			int numModels, LDAConfiguration conf) throws Exception {

		List<File> repoFolders = new ArrayList<File>();
		repoFolders.add(new File(dirPath));
		List<String> fileNamesRet = new ArrayList<String>();
		List<ParallelTopicModel> models = createNewModels(repoFolders,
				fileNamesRet, conf, numModels);
		return models;

	}

	private List<ParallelTopicModel> createNewModels(List<File> repoDirs,
			List<String> fileNamesRet, LDAConfiguration conf, int numModels)
			throws IOException {
		List<ParallelTopicModel> ret = new ArrayList<ParallelTopicModel>();
		List<String> texts = textProvider.getTexts(repoDirs, fileNamesRet);
		System.out.println("creating model with " + texts.size() + " texts");
		InstanceList instanceList = createInstanceList(texts);
		for (int i = 0; i < numModels; i++) {
			ParallelTopicModel model = new ParallelTopicModel(conf.numTopics,
					conf.alpha, conf.beta);

			model.addInstances(instanceList);
			model.setNumThreads(conf.threads);
			model.setNumIterations(conf.iterations);
			model.estimate();
			ret.add(model);
		}
		return ret;
	}

	private InstanceList createInstanceList(List<String> texts)
			throws IOException {
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
}
