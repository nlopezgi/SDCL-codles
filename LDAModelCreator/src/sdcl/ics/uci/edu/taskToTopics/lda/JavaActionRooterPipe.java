package sdcl.ics.uci.edu.taskToTopics.lda;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class JavaActionRooterPipe extends Pipe {

	private static final long serialVersionUID = -2709920340685713529L;
	
	private static final String[] actionVerbs = new String[] { "get", "set",
			"add" };

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence in = (TokenSequence) carrier.getData();
		for (Token token : in) {

			token.setText(trimCommonJavaActionVerbs(token.getText()));
		}
		return carrier;
	}

	private String trimCommonJavaActionVerbs(String text) {
		for (int i = 0; i < actionVerbs.length; i++) {
			text = trimActionVerb(text, actionVerbs[i]);
		}
		return text;
	}

	public String trimActionVerb(String text, String actionVerb) {
		if (text.startsWith(actionVerb)) {
			text = text.replaceFirst(actionVerb, "");
		}
		return text;
	}
}
