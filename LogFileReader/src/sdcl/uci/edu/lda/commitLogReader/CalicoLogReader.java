package sdcl.uci.edu.lda.commitLogReader;

import java.util.List;

public class CalicoLogReader {
	public static final String CALICO_LOG_FILE = "calicoGitLog.txt";

	public static void main(String[] args) throws Exception {
		String path = CommitLogReader.COMMIT_LOG_DIR + CommitLogReader.SEPARATOR
				+ CALICO_LOG_FILE;
		List<CommitLogData> data = CommitLogReader.readCommitLogFile(path);
		
	}

}
