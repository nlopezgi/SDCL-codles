package sdcl.ics.uci.edu.taskToTopics.lda.mstem;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class MStemStemmer extends Pipe {

	private static final long serialVersionUID = 2473902728130245487L;

	private static final String MSTEM_FILE = ExperimentDataUtil.MODEL_MSTEMDATA_DIR
			+ ExperimentDataUtil.SEPARATOR + "mstem.txt";
	private Map<String, String> tokenToRootMap;

	@Override
	public Instance pipe(Instance carrier) {
		if (tokenToRootMap == null) {
			readMStemFile();
		}
		TokenSequence in = (TokenSequence) carrier.getData();
		for (Token token : in) {
			if (tokenToRootMap.containsKey(in)) {
				token.setText(tokenToRootMap.get(in));
			}
		}
		return carrier;
	}

	private void readMStemFile() {
		tokenToRootMap = new HashMap<String, String>();
		try {
			FileInputStream fstream = new FileInputStream(MSTEM_FILE);

			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				String[] data = strLine.split(" ");
				tokenToRootMap.put(data[0], data[1]);
			}
			fstream.close();
		} catch (Exception e) {
			System.err.println("ERROR LOADING MSTEM DATA");
			e.printStackTrace();
		}
	}
}
