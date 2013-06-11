package sdcl.ics.uci.edu.taskToTopics.lda;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class SnowballStemmerPipe extends Pipe {

	private static final long serialVersionUID = 6259600945332340947L;

	@Override
	public Instance pipe(Instance carrier) {
		SnowballStemmer stemmer = new englishStemmer();
		TokenSequence in = (TokenSequence) carrier.getData();

		for (Token token : in) {
			stemmer.setCurrent(token.getText());
			stemmer.stem();
			token.setText(stemmer.getCurrent());
		}

		return carrier;
	}

}
