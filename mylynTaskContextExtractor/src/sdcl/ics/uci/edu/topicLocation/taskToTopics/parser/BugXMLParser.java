package sdcl.ics.uci.edu.topicLocation.taskToTopics.parser;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sdcl.ics.uci.edu.taskToTopics.taskExtractor.AttachmentData;
import sdcl.ics.uci.edu.taskToTopics.taskExtractor.ContextFileDownloader;

public class BugXMLParser extends DefaultHandler {

	CharArrayWriter text = new CharArrayWriter();
	SAXParser parser = new SAXParser();

	String attachId;
	String bugId;
	String attachmentDesc;
	String fileName;
	ContextFileDownloader downloader;

	AttachmentData currentAttachment;

	boolean insideAttachment = false;
	boolean insideMylynAttachment = false;

	public void parse(InputStream is, ContextFileDownloader downloader)
			throws SAXException, IOException {
		this.downloader = downloader;
		parser.setContentHandler(this);
		parser.parse(new InputSource(is));
	}

	public void startDocument() throws SAXException {
	}

	public void endDocument() throws SAXException {
	}

	public void startElement(java.lang.String uri, java.lang.String localName,
			java.lang.String qName, Attributes attributes) throws SAXException {
		text.reset();
		if (qName.equals("bug")) {
			// System.out.println("starting bug");
			// System.out.println();
		}
		if (qName.equals("bug_id")) {
			bugId = getText();
			// System.out.println("starting bug_id:" + bugId);
			// System.out.println();
		}
		if (qName.equals("attachment")) {
			// System.out.println("starting attachment");
			// System.out.println();
			insideAttachment = true;

			currentAttachment = new AttachmentData();

		}
		if (qName.equals("attachid") && insideAttachment) {
			// System.out.println("starting attachId");
			// System.out.println();
		}
		if (qName.equals("desc") && insideAttachment) {
			// System.out.println("starting attachmentDesc");
			// System.out.println();
		}
	}

	public void endElement(java.lang.String uri, java.lang.String localName,
			java.lang.String qName) throws SAXException {

		if (qName.equals("bug_id")) {
			bugId = getText();

			// System.out.println("ending bug_id:" + bugId);

		}
		if (qName.equals("attachment")) {
			// System.out.println("ending attachment");
			insideAttachment = false;
		}
		if (qName.equals("attachid") && insideAttachment) {
			attachId = getText();
			currentAttachment.setAttachmentId(attachId);
			// System.out.println("ending attachid:" + attachId);
		}
		if (qName.equals("desc") && insideAttachment) {
			attachmentDesc = getText();
			// System.out.println("ending attachmentDesc:" + attachmentDesc);
			if (attachmentDesc.contains("/context/zip")) {
				insideMylynAttachment = true;
			}
		}
		if (qName.equals("filename") && insideMylynAttachment) {
			fileName = getText();
			currentAttachment.setBugId(bugId);
			currentAttachment.setFilename(fileName);
			// System.out.println("ending fileName:" + fileName);
			// downloader.addDownload(currentAttachment);
			downloader.downloadNow(currentAttachment);
			insideMylynAttachment = false;
			currentAttachment = null;
		}
	}

	public void characters(char[] ch, int start, int length) {
		text.write(ch, start, length);
	}

	public String getText() {
		return text.toString().trim();
	}

}
