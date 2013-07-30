package sdcl.ics.uci.edu.taskToTopics.gitDownloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;

/**
 * Explores a git repo and makes copies of the project at differnt points in
 * time.
 * 
 * @author nlopezgi
 * 
 */
public class GitMultiVersionExporer {

	/**
	 * The location of a directory that contains multiple git projects (each in a
	 * sub directory)
	 */

	public static final String TEST_REMOTE_REPO = "git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.git";
	

	public static Date CHECKOUT_DATE = new Date(112, 9, 01, 11, 59, 00);

	private static final String[] repos = {
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.builds.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.commons.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.context.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.context.mft.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.docs.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.incubator.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.reviews.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.tasks.git",
			"git://git.eclipse.org/gitroot/mylyn/org.eclipse.mylyn.versions.git", };

	private static final String REPO_PREFIX = "mylyn/";
	private static final String REV_FILE = "rev.sav";

	/**
	 * Periodicity in days to get a different version of the repo (30 means get a
	 * different copy of the repo for every 30 days)
	 */
	public static final int DAY_PERIODICITY = 30;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		GitMultiVersionExporer gmve = new GitMultiVersionExporer();
		gmve.run();
	}

	public void run() throws Exception {
		cloneAllRemoteRepos();
		List<String> repos = getSubdirectories();
		for (String oneRepo : repos) {
			try {
				System.out.println("");
				System.out.println("****************************");
				System.out.println("Starting to copy one repo:" + oneRepo);
				Repository repo = getRepo(ExperimentDataUtil.GIT_REPOS_TEMP
						+ ExperimentDataUtil.SEPARATOR + oneRepo);

				makePeriodicCopiesOfRepo(repo, oneRepo);
				System.out.println("DONE WITH REPO:" + oneRepo);
			} catch (Exception e) {
				System.err.println("Error processing repo:" + oneRepo);
				e.printStackTrace();
			}
		}
	}

	public void cloneAllRemoteRepos() throws Exception {
		for (String oneRepo : repos) {

			String repoDir = ExperimentDataUtil.GIT_REPOS_TEMP
					+ ExperimentDataUtil.SEPARATOR
					+ oneRepo.substring(oneRepo.indexOf(REPO_PREFIX) + 6);
			System.out.println("cloning repo @" + oneRepo + " to:" + repoDir);
			File repoDirFile = new File(repoDir);
			if (!repoDirFile.exists()) {
				repoDirFile.mkdir();
				Git.cloneRepository().setURI(oneRepo).setDirectory(repoDirFile).call();
				System.out.println("done cloning " + oneRepo);
			} else {
				System.out
						.println("repo"
								+ oneRepo
								+ " already exists (or dir was present, so delete if force clone desired)");
			}
		}
		System.out.println("----------------------");
		System.out.println("Done cloning all repos");
		System.out.println("----------------------");
	}

	public void makePeriodicCopiesOfRepo(Repository repository, String repoName)
			throws Exception {
		List<CommitData> revCommitMap = new ArrayList<CommitData>();
		System.out.println("making perioidic copies for repo " + repoName);
		File repoOut = new File(ExperimentDataUtil.GIT_REPOS_OUT
				+ ExperimentDataUtil.SEPARATOR + repoName);

		// Create the directory
		if (!repoOut.exists()) {
			boolean success = repoOut.mkdir();
			if (!success) {
				throw new Exception("Could not create folder:"
						+ ExperimentDataUtil.GIT_REPOS_OUT + ExperimentDataUtil.SEPARATOR
						+ repoName);
			}
		}
		Git git = new Git(repository);

		Iterator<RevCommit> walkIterator = git.log().all().call().iterator();
		Date now = CHECKOUT_DATE;
		long periodicity = (long) DAY_PERIODICITY * 1000 * 24 * 60 * 60;
		Date nextSnapshot = new Date(now.getTime() - periodicity);
		while (walkIterator.hasNext()) {
			RevCommit oneCommit = walkIterator.next();

			int commitTime = oneCommit.getCommitTime();
			long dateLong = ((long) commitTime) * 1000;

			Date commitDate = new Date(dateLong);
			if (commitDate.before(nextSnapshot)) {
				// System.out.println("starting to copy repo for commit: "
				// + oneCommit.getName() + ". Date long:" + commitDate.getTime()
				// + ". real date:" + commitDate);

				// copyRepoAtCommit(repository, git, oneCommit, commitDate, repoName);
				if (checkDirExists(repoName, oneCommit)) {
					revCommitMap.add(new CommitData(oneCommit));
					// System.out.println("found dir" + oneCommit.getCommitTime());
				} else {
					System.err.println("Could not find dir for rev:"
							+ oneCommit.getName() + ". Date long:" + commitDate.getTime()
							+ ". real date:" + commitDate);
				}
				nextSnapshot = new Date(nextSnapshot.getTime() - periodicity);
			}
		}
		saveRevCommitMap(revCommitMap, repoName);
	}

	private boolean checkDirExists(String repoName, RevCommit oneCommit) {
		File oneRepoVersionDir = new File(ExperimentDataUtil.GIT_REPOS_OUT
				+ ExperimentDataUtil.SEPARATOR + repoName
				+ ExperimentDataUtil.SEPARATOR + oneCommit.getCommitTime());
		return oneRepoVersionDir.exists();
	}

	private void saveRevCommitMap(List<CommitData> revCommitList, String repoName)
			throws IOException {
		System.out.println("Saving rev map to " + ExperimentDataUtil.GIT_REPOS_OUT
				+ ExperimentDataUtil.SEPARATOR + repoName
				+ ExperimentDataUtil.SEPARATOR + REV_FILE);
		File revFile = new File(ExperimentDataUtil.GIT_REPOS_OUT
				+ ExperimentDataUtil.SEPARATOR + repoName
				+ ExperimentDataUtil.SEPARATOR + REV_FILE);
		if (!revFile.exists()) {
			revFile.createNewFile();
		} else {
			revFile.delete();
			revFile.createNewFile();
		}

		FileWriter writer = new FileWriter(revFile);
		BufferedWriter out = new BufferedWriter(writer);

		for (CommitData entry : revCommitList) {
			out.write(entry.toString());
			out.write("\n");
		}
		out.close();
		System.out.println("done saving rev file");
	}

	private void copyRepoAtCommit(Repository repository, Git git,
			RevCommit oneCommit, Date date, String repoName) throws Exception {

		File oneRepoVersionDir = new File(ExperimentDataUtil.GIT_REPOS_OUT
				+ ExperimentDataUtil.SEPARATOR + repoName
				+ ExperimentDataUtil.SEPARATOR + oneCommit.getCommitTime());
		if (!oneRepoVersionDir.exists()) {
			boolean success = oneRepoVersionDir.mkdirs();
			if (!success) {
				throw new Exception("Could not create folder:"
						+ ExperimentDataUtil.GIT_REPOS_OUT + ExperimentDataUtil.SEPARATOR
						+ repoName + ExperimentDataUtil.SEPARATOR
						+ oneCommit.getCommitTime());
			}
			// make copy of repository
			CloneCommand command = Git.cloneRepository();
			command.setDirectory(oneRepoVersionDir);
			command.setURI("file://"
					+ git.getRepository().getWorkTree().getAbsolutePath());
			Git git2 = null;
			try {
				git2 = command.call();
				// reset it to the desired state
				git2.reset().setRef(oneCommit.getName()).setMode(ResetType.HARD).call();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("AN ERROR OCCURED COPYING OR RESETTING REPO: "
						+ ExperimentDataUtil.GIT_REPOS_OUT + ExperimentDataUtil.SEPARATOR
						+ repoName + ExperimentDataUtil.SEPARATOR
						+ oneCommit.getCommitTime());
				System.err.println("commit that failed:" + oneCommit.getName());
				e.printStackTrace();
			}
		} else {
			System.out
					.println("directory exists, skipping: "
							+ ExperimentDataUtil.GIT_REPOS_OUT + ExperimentDataUtil.SEPARATOR
							+ repoName + ExperimentDataUtil.SEPARATOR
							+ oneCommit.getCommitTime());
		}
	}

	/**
	 * Gets all subdirectories (each assumed to be a different git repo)
	 * 
	 * @return
	 */
	public List<String> getSubdirectories() {
		List<String> ret = new ArrayList<>();
		File parentDir = new File(ExperimentDataUtil.GIT_REPOS_TEMP);
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

	public List<String> getSubdirectories(String dir) {
		List<String> ret = new ArrayList<>();
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

	public Repository getRepo(String location) throws IOException {
		return new FileRepository(location + "/.git");
	}
}
