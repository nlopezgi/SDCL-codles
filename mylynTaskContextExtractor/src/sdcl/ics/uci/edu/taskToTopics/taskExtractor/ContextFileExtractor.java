package sdcl.ics.uci.edu.taskToTopics.taskExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.topicLocation.taskToTopics.parser.BugXMLParser;

public class ContextFileExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, SAXException {
		ContextFileDownloader downloader = new ContextFileDownloader();

		String bugsDir = ExperimentDataUtil.BUGZILLA_XMLS_DIR
				+ ExperimentDataUtil.SEPARATOR;
		File[] files = { new File(bugsDir + "bugs100k.xml"),
				new File(bugsDir + "bugs200k.xml"), new File(bugsDir + "bugs300k.xml") };
		for (File file : files) {
			InputStream bugInputStream = new FileInputStream(file);
			BugXMLParser xmlParser = new BugXMLParser();
			xmlParser.parse(bugInputStream, downloader);
			bugInputStream.close();
		}
		// downloader.listDownloads();
		// downloader.downloadAll();

	}

}
