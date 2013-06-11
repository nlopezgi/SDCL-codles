package sdcl.ics.uci.edu.lda.util;

import java.util.ArrayList;
import java.util.List;

public class SelectedTopic {

	private int topicNumber;
	private double filePercentage;
	private List<String> files = new ArrayList<String>();
	private double[] probabilities;

	public SelectedTopic() {
	
	}
	
	public SelectedTopic(int topicNumber, double filePercentage) {
		this.topicNumber = topicNumber;
		this.filePercentage = filePercentage;
	}

	public int getTopicNumber() {
		return topicNumber;
	}

	public void setTopicNumber(int topicNumber) {
		this.topicNumber = topicNumber;
	}

	public double getFilePercentage() {
		return filePercentage;
	}

	public void setfilePercentage(double filePercentage) {
		this.filePercentage = filePercentage;
	}

	public void addFile(String fileName) {		
		files.add(fileName);
	}

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}

	public double[] getProbabilities() {
		return probabilities;
	}

	public void setProbabilities(double[] probabilities) {
		this.probabilities = probabilities;
	}

	public ProfitPair calculateWeightedProfit() {
		ProfitPair ret = new ProfitPair();
		ret.averageProfit = 0;
		ret.numClasses = 0;
		for (double oneProb : probabilities) {
			//if (oneProb > TopicResolver.PROFIT_THRESHOLD) {
				ret.averageProfit += oneProb;
				ret.numClasses++;
			//}
		}
		if (ret.numClasses > 0) {
			ret.averageProfit = ret.averageProfit / ret.numClasses;
		}
		return ret;
	}

	public class ProfitPair {
		public ProfitPair(){
			
		}
		public double averageProfit;
		public int numClasses;
	}
}
