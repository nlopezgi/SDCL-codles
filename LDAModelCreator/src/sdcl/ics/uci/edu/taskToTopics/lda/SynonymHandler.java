package sdcl.ics.uci.edu.taskToTopics.lda;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import sdcl.ics.uci.edu.lda.moduleSplitter.ModuleSplitter.Project;
import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class SynonymHandler extends Pipe {

	Map<String, String> synonymMap = null;
	private final Project project;

	public SynonymHandler(Project project) {
		this.project = project;
	}

	@Override
	public Instance pipe(Instance carrier) {
		if (synonymMap == null) {
			readSynonymFile();
		}
		TokenSequence in = (TokenSequence) carrier.getData();
		for (Token token : in) {
			if (synonymMap.containsKey(token.getText())) {
				token.setText(synonymMap.get(token.getText()));
			}
		}
		return carrier;
	}

	private void readSynonymFile() {
		synonymMap = new HashMap<String, String>();
		switch (project) {
		case CALICO:
			try {

				FileInputStream fstream = new FileInputStream(
						ExperimentDataUtil.CALICO_SYNONYMS);

				// Get the object of DataInputStream
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					String[] data = strLine.trim().split(" ");
					synonymMap.put(data[0], data[1]);
				}
				fstream.close();
			} catch (Exception e) {
				System.err.println("ERROR LOADING CALICO SYNONYM DATA");
				e.printStackTrace();
			}
			break;

		default:
			break;
		}
	}
}
