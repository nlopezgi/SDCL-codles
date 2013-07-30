package sdcl.ics.uci.edu.taskToTopics.taskExtractor;

public class AttachmentData {

	public String bugId;
	public String attachmentId;
	public String filename;
	
	public String getBugId() {
		return bugId;
	}
	public void setBugId(String bugId) {
		this.bugId = bugId;
	}
	public String getAttachmentId() {
		return attachmentId;
	}
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
