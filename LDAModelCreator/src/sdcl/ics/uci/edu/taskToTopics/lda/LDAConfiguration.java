package sdcl.ics.uci.edu.taskToTopics.lda;


public class LDAConfiguration {
	private static final int THREADS = 20;
	private static final int ITERATIONS = 1000;

	public double alpha;
	public double beta;
	public int threads;
	public int numTopics;
	public int iterations;

	public LDAConfiguration(double alpha, double beta, int threads,
			int numTopics, int iterations) {
		this.alpha = alpha;
		this.beta = beta;
		this.threads = threads;
		this.numTopics = numTopics;
		this.iterations = iterations;
	}

	public LDAConfiguration(double alpha, double beta, int numTopics) {
		this.alpha = alpha;
		this.beta = beta;
		this.threads = THREADS;
		this.iterations = ITERATIONS;
		this.numTopics = numTopics;

	}

}
