package sdcl.ics.uci.edu.taskToTopics.taskExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;

public class ContextFileDownloader {

	
	List<AttachmentData> downloads = new ArrayList<AttachmentData>();

	public void addDownload(AttachmentData attachment) {
		// downloads.add(attachment);
	}

	public void downloadNow(AttachmentData attachment) {
		AttachmentData data = attachment;
		downloadFile(data.getAttachmentId(), data.getBugId());
		unzipFile(data.getAttachmentId(), data.getBugId());
		deleteZipFile(data.getAttachmentId(), data.getBugId());
	}

	public void listDownloads() {
		for (AttachmentData s : downloads) {
			System.out.println(s.getAttachmentId());
		}
	}

	public void downloadTest() {
		if (!downloads.isEmpty()) {
			AttachmentData data = downloads.get(0);
			downloadFile(data.getAttachmentId(), data.getBugId());
			unzipFile(data.getAttachmentId(), data.getBugId());
			deleteZipFile(data.getAttachmentId(), data.getBugId());
		}
	}

	public void downloadAll() {
		for (AttachmentData data : downloads) {
			downloadFile(data.getAttachmentId(), data.getBugId());
			unzipFile(data.getAttachmentId(), data.getBugId());
			deleteZipFile(data.getAttachmentId(), data.getBugId());
		}
	}

	private void downloadFile(String attachmentId, String bugId) {
		URL website;
		FileOutputStream fos = null;
		try {
			website = new URL(ExperimentDataUtil.MYLYN_BUGS_ATTACHMENTS_BASE_URL
					+ attachmentId);

			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			File out = new File(ExperimentDataUtil.TASK_CONTEXT_XML_DIR
					+ ExperimentDataUtil.SEPARATOR + bugId + "_" + attachmentId + ".zip");

			if (!out.exists()) {
				out.createNewFile();
			}
			fos = new FileOutputStream(out);
			fos.getChannel().transferFrom(rbc, 0, 1 << 24);
			fos.flush();
			fos.close();

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void unzipFile(String attachmentId, String bugId) {
		ZipInputStream zinstream = null;
		try {
			String fName = ExperimentDataUtil.TASK_CONTEXT_XML_DIR
					+ ExperimentDataUtil.SEPARATOR + bugId + "_" + attachmentId + ".zip";
			zinstream = new ZipInputStream(new FileInputStream(fName));
			byte[] buf = new byte[1024];
			ZipEntry zentry = zinstream.getNextEntry();
			// System.out.println("Name of current Zip Entry : " + zentry + "\n");
			while (zentry != null) {
				String entryName = zentry.getName();
				entryName = entryName.replace("%3A%2F%2F", "-");
				entryName = entryName.replace("%2F", "-");
				entryName = bugId + "-" + attachmentId + "-" + entryName;

				System.out.println("Name of  Zip Entry : " + entryName);
				FileOutputStream outstream = new FileOutputStream(
						ExperimentDataUtil.TASK_CONTEXT_XML_DIR
								+ ExperimentDataUtil.SEPARATOR + entryName);
				int n;

				while ((n = zinstream.read(buf, 0, 1024)) > -1) {
					outstream.write(buf, 0, n);

				}
				// System.out.println("Successfully Extracted File Name : "
				// + entryName);
				outstream.close();

				zinstream.closeEntry();
				zentry = zinstream.getNextEntry();
			}
			zinstream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (zinstream != null) {
					zinstream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void deleteZipFile(String attachmentId, String bugId) {
		File out = new File(ExperimentDataUtil.TASK_CONTEXT_XML_DIR
				+ ExperimentDataUtil.SEPARATOR + bugId + "_" + attachmentId + ".zip");

		if (out.exists()) {
			out.delete();
		}
	}

}
