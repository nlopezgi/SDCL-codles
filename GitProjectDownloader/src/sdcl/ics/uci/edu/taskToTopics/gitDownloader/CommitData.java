package sdcl.ics.uci.edu.taskToTopics.gitDownloader;

import java.io.Serializable;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

public class CommitData implements Serializable {

	private static final long serialVersionUID = -3872992030614831619L;

	ObjectId id;
	String name;
	int commitTime;

	public CommitData(RevCommit commit) {
		id = commit.getId();
		name = commit.getName();
		commitTime = commit.getCommitTime();
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(int commitTime) {
		this.commitTime = commitTime;
	}

	@Override
	public String toString() {
		//return new String(id + "," + name + "," + commitTime);
		return new String(commitTime + "," + name);
	}

}
