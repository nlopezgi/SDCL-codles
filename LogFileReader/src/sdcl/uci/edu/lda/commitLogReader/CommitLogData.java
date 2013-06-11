package sdcl.uci.edu.lda.commitLogReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommitLogData {

	private List<String> files;
	private List<ClassChange> classChanges;

	private String id;
	private String author;
	private Date date;
	private int added;
	private int removed;

	public CommitLogData() {
		files = new ArrayList<String>();
		classChanges = new ArrayList<ClassChange>();
	}

	public List<String> getFiles() {
		return files;
	}

	public List<ClassChange> getClassChanges() {
		return classChanges;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void addFile(String file) {
		files.add(file);
	}

	public void addClassChange(String file, int numChanged, int proportionAdded,
			int proportionRemoved) {
		classChanges.add(new ClassChange(file, numChanged, proportionAdded,
				proportionRemoved));
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String toString() {
		return new String("Commit log from date:" + date + ". With " + files.size()
				+ " files.");
	}

	public int getAdded() {
		return added;
	}

	public void setAdded(int added) {
		this.added = added;
	}

	public int getRemoved() {
		return removed;
	}

	public void setRemoved(int removed) {
		this.removed = removed;
	}

	public static class ClassChange {
		public ClassChange(String className, int numChanged, int proportionalAdded,
				int proportionalRemoved) {
			super();
			this.className = className;
			this.numChanged = numChanged;
			this.proportionalAdded = proportionalAdded;
			this.proportionalRemoved = proportionalRemoved;
		}

		public String className;
		public int numChanged;
		/**
		 * These two ints correspond to the number of plusses and minuses in the
		 * commit log line, they may not add up to the total changed, in which case
		 * they represent a proportion
		 */
		public int proportionalAdded;
		public int proportionalRemoved;
	}
}
