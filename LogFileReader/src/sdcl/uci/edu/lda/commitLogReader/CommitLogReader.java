package sdcl.uci.edu.lda.commitLogReader;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommitLogReader {

	public static final String COMMIT_START_TOKEN = "commit";
	public static final String DATE_TOKEN = "Date:";
	public static final String CHANGED_TOKEN = "changed";
	public static final String INSERTED_TOKEN = "insertion";
	public static final String DELETED_TOKEN = "deletion";
	public static final String AUTHOR_TOKEN = "Author:";
	public static final String FILE_TOKEN = "...";
	public static final String JAVA_FILE_TOKEN = ".java";
	public static final String SVN_FILE_IGNORE = ".java.svn-base";
	public static final String SRC_TRIM_TOKEN = "src.";
	public static final String COMMIT_LOG_DIR = "commitLogs";
	public static final String SEPARATOR = "/";
	public static final int MIN_COMMIT_SIZE = 2;
	public static final String FILE_CHANGE_DATA_TOKEN = "|";
	public static final char ADD = '+';
	public static final char DELETE = '-';

	// /**
	// * For testing purposes
	// */
	// public static void main(String[] args) throws Exception {
	// CommitLogReader clr = new CommitLogReader();
	// // clr.readCommitLogFile(COMMIT_LOG_DIR + SEPARATOR
	// // + "org.eclipse.mylyn.tasks-log.log");
	// List<CommitLogData> allLogs = clr.getAllCommitLogs();
	// System.out.println("done");
	// }

	// /**
	// * Recursively reads files inside a folder and adds their names to the list
	// */
	// private void readFiles(File folder, List<String> files) {
	// if (folder.isDirectory()) {
	// File[] listOfFiles = folder.listFiles();
	// for (int i = 0; i < listOfFiles.length; i++) {
	// if (listOfFiles[i].isFile()) {
	// String fileName = listOfFiles[i].getAbsolutePath();
	// files.add(fileName);
	// }
	// if (listOfFiles[i].isDirectory()) {
	// readFiles(listOfFiles[i], files);
	//
	// }
	// }
	// }
	// }

	// public List<CommitLogData> getAllCommitLogs() throws Exception {
	// List<String> commitLogFiles = new ArrayList<String>();
	// File commitLogDir = new File(COMMIT_LOG_DIR);
	// readFiles(commitLogDir, commitLogFiles);
	// List<List<CommitLogData>> allCommitLogs = new
	// ArrayList<List<CommitLogData>>();
	// for (String oneFile : commitLogFiles) {
	// allCommitLogs.add(readCommitLogFile(oneFile));
	// }
	// List<CommitLogData> ret = new ArrayList<CommitLogData>();
	// // order the commit logs sequentially
	// boolean done = false;
	// List<List<CommitLogData>> toRemove;
	// while (!done) {
	// toRemove = null;
	// Date oldest = null;
	// CommitLogData oldestCommitLog = null;
	// List<CommitLogData> selectedList = null;
	// // pick the oldest date from all the last elements of each list
	// for (List<CommitLogData> oneList : allCommitLogs) {
	// if ((oneList.size() > 0)
	// && (oldest == null || oldest.after(oneList.get(oneList.size() - 1)
	// .getDate()))) {
	// oldest = oneList.get(oneList.size() - 1).getDate();
	// selectedList = oneList;
	// oldestCommitLog = oneList.get(oneList.size() - 1);
	// }
	// if (oneList.size() == 0) {
	// if (toRemove == null) {
	// toRemove = new ArrayList<List<CommitLogData>>();
	// }
	// toRemove.add(oneList);
	//
	// }
	// }
	// // remove one from the selected list and add it to the ret list
	// if (oldest != null) {
	// ret.add(oldestCommitLog);
	// selectedList.remove(selectedList.size() - 1);
	// }
	// if (allCommitLogs.size() == 0) {
	// done = true;
	// }
	// if (toRemove != null) {
	// for (List<CommitLogData> oneList : toRemove) {
	// allCommitLogs.remove(oneList);
	// }
	// toRemove = null;
	// }
	// }
	// return ret;
	// }

	/**
	 * Returns the list of names of java files that are part of each commit (from
	 * the file). The date of the issue is left in issueDate
	 */
	public static List<CommitLogData> readCommitLogFile(String fileName)
			throws IOException {
		List<CommitLogData> ret = new ArrayList<CommitLogData>();
		FileInputStream fstream = new FileInputStream(fileName);

		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		CommitLogData oneCommit = null;
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith(COMMIT_START_TOKEN)) {
				if (oneCommit != null && oneCommit.getFiles().size() >= 2) {
					ret.add(oneCommit);
				}
				oneCommit = new CommitLogData();
				oneCommit.setId(strLine.substring(COMMIT_START_TOKEN.length() + 1));
			}
			if (strLine.contains(DATE_TOKEN)) {
				String dateStr = strLine.substring(strLine.indexOf(DATE_TOKEN)
						+ DATE_TOKEN.length() + 1);
				dateStr = dateStr.trim();
				if (dateStr.contains("-0")) {
					dateStr = dateStr.substring(0, dateStr.lastIndexOf("-0"));
				} else if (dateStr.contains("+0")) {
					dateStr = dateStr.substring(0, dateStr.lastIndexOf("+0"));
				}
				SimpleDateFormat format = new SimpleDateFormat(
						"EEE MMM dd HH:mm:ss yyyy");
				try {
					Date parsed = format.parse(dateStr);
					oneCommit.setDate(parsed);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (strLine.contains(AUTHOR_TOKEN)) {
				String authStr = strLine.substring(strLine.indexOf(AUTHOR_TOKEN)
						+ AUTHOR_TOKEN.length() + 1);
				oneCommit.setAuthor(authStr);
			}
			if (strLine.contains(CHANGED_TOKEN)
					&& (strLine.contains(INSERTED_TOKEN) || strLine
							.contains(DELETED_TOKEN))) {
				// ITs a change detail line like :
				// "7 files changed, 81 insertions(+), 4 deletions(-)"
				String[] changeLineTokens = strLine.split(" ");
				for (int i = 0; i < changeLineTokens.length; i++) {
					if (changeLineTokens[i].contains(INSERTED_TOKEN)) {
						int insertions = Integer.parseInt(changeLineTokens[i - 1]);
						oneCommit.setAdded(insertions);
					}
					if (changeLineTokens[i].contains(DELETED_TOKEN)) {
						int deletions = Integer.parseInt(changeLineTokens[i - 1]);
						oneCommit.setRemoved(deletions);
					}
				}
			}
			if (strLine.contains(FILE_TOKEN)
					&& (strLine.contains(JAVA_FILE_TOKEN) && !strLine
							.contains(SVN_FILE_IGNORE))) {
				String oneFile = strLine.substring(strLine.indexOf(FILE_TOKEN)
						+ FILE_TOKEN.length(), strLine.indexOf(JAVA_FILE_TOKEN));
				oneFile = oneFile.replace("/", ".");
				if (oneFile.startsWith(".")) {
					oneFile = oneFile.substring(1);

				}
				if (oneFile.contains(SRC_TRIM_TOKEN)) {
					oneFile = oneFile.substring(oneFile.indexOf(SRC_TRIM_TOKEN) + 4);

				}
				oneCommit.addFile(oneFile);
				readFileChangeData(strLine, oneCommit, oneFile);
			}
		}

		fstream.close();
		return ret;
	}

	private static void readFileChangeData(String fileLine, CommitLogData data,
			String className) {
		String changeData = fileLine.substring(fileLine
				.indexOf(FILE_CHANGE_DATA_TOKEN) + 1);
		String[] changeDataTokens = changeData.trim().split(" ");
		int changed = 0;
		int propAdded = 0;
		int propDeleted = 0;
		for (int i = 0; i < changeDataTokens.length; i++) {
			if (i == 0) {
				changed = Integer.parseInt(changeDataTokens[i]);
			}
			if (i == 1) {
				for (int j = 0; j < changeDataTokens[i].length(); j++) {
					char oneChar = changeDataTokens[i].charAt(j);
					if (oneChar == ADD) {
						propAdded++;
					} else if (oneChar == DELETE) {
						propDeleted++;
					}
				}
			}
		}
		data.addClassChange(className, changed, propAdded, propDeleted);
	}
}
