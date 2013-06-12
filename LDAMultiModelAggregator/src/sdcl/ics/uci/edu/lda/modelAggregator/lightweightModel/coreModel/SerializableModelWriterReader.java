package sdcl.ics.uci.edu.lda.modelAggregator.lightweightModel.coreModel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SerializableModelWriterReader {

	public static void writeToFile(String filename, LightweightTopicModel model)
			throws Exception {
		try {
			// use buffering
//			File toCreate = new File(filename);
//			if(toCreate)
			OutputStream file = new FileOutputStream(filename);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(model);
			} finally {
				output.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			throw ex;
		}

	}

	public static LightweightTopicModel readFromFile(String filename)
			throws Exception {
		try {
			// use buffering
			InputStream file = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {

				LightweightTopicModel model = (LightweightTopicModel) input
						.readObject();
				return model;

			} finally {
				input.close();
			}
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			throw ex;
		} catch (IOException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
}
