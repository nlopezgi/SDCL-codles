package sdcl.ics.uci.edu.lda.util;

import java.util.Date;
import java.util.List;

public class Task {

	private final String taskId;
	private final Date date;
	private final List<String> files;
	private final long modelSnapshot;
	private List<SelectedTopic> selectedTopics;

	private String fileNames = null;

	public Task(String taskId, List<String> files, Date date, long modelSnapshot) {
		this.taskId = taskId;
		this.files = files;
		this.date = date;
		this.modelSnapshot = modelSnapshot;
	}

	public List<SelectedTopic> getSelectedTopics() {
		return selectedTopics;
	}

	public void setSelectedTopics(List<SelectedTopic> selectedTopics) {
		this.selectedTopics = selectedTopics;
	}

	public String getTaskId() {
		return taskId;
	}

	public Date getDate() {
		return date;
	}

	public List<String> getFiles() {
		return files;
	}

	public long getModelSnapshot() {
		return modelSnapshot;
	}

	public String getFilesString() {
		if (fileNames == null) {
			fileNames = new String();
			for (String oneFile : files) {
				fileNames = fileNames.concat(oneFile + ", ");
			}
		}
		return fileNames;
	}

	public double getRelationshipWithTopic(int topicNumber) {
		for (SelectedTopic topic : selectedTopics) {
			if (topic.getTopicNumber() == topicNumber) {
				return topic.calculateWeightedProfit().averageProfit;
			}
		}
		return 0;
	}
}
