package sdcl.ics.uci.edu.taskToTopics.lda;

import java.util.ArrayList;
import java.util.Collection;

import cc.mallet.extract.StringSpan;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class CamelCaseSplitterPipe extends Pipe {

	private static final long serialVersionUID = 5940953843002423238L;

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence in = (TokenSequence) carrier.getData();
		TokenSequence out = new TokenSequence();
		for (Token token : in) {
			out.addAll(getCamelCaseSplitTokens(token));
		}
		carrier.setData(out);
		return carrier;
	}

	public Collection<Token> getCamelCaseSplitTokens(Token token) {
		Collection<Token> ret = new ArrayList<Token>();
		String text = token.getText();
		String[] splitText = text.split("(?=\\p{Upper})");
		if (splitText.length == 1) {
			ret.add(token);
			return ret;
		}
		int start;
		StringSpan span = (StringSpan) token;
		start = span.getStartIdx();
		CharSequence doc = (CharSequence) span.getDocument();
		for (String oneText : splitText) {
			if (oneText.length() > 0) {
				StringSpan oneSpan = new StringSpan(doc, start, start
						+ oneText.length());
				start = start + oneText.length();
				ret.add(oneSpan);
			}
		}
		return ret;
	}
}
