/*
 * EmpiricalFitnessFunction.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package santa.simulator.fitness;

import santa.simulator.genomes.*;
import santa.simulator.Population;

import java.util.*;

/**
 * This is an implementation of FitnessFunction which encapsulates empirical estimates of the
 * fitness effects of different states.
 */
public abstract class AbstractSiteFitnessFactor extends AbstractFitnessFactor {

	public AbstractSiteFitnessFactor(Feature feature, Set<Integer> sites) {
		super(feature, sites);
	}

	/**
	 * Set the fitnesses for every state at every site included.
	 */
	protected void initialize(double[][] logFitness) {
		this.logFitness = logFitness;
	}

	public boolean updateGeneration(int generation, Population population) {
		return false;
	}

	public double computeLogFitness(byte[] sequence) {

		double logFitness = 0.0;

		Iterator<Integer> it = getSites().iterator();

		while (it.hasNext()) {
			int site = it.next();

			if (sequence[site - 1] >= getAlphabet().getStateCount())
				logFitness += Double.NEGATIVE_INFINITY;
			else
				logFitness += this.logFitness[site - 1][sequence[site - 1]];
		}

		return logFitness;
	}

	public double updateLogFitness(Genome genome, double logFitness, Mutation m) {
		int mutationSite = m.position / getAlphabet().getTokenSize();

		if (getSites().contains(mutationSite + 1)) {
			switch (getAlphabet()) {
				case NUCLEOTIDES:
					return logFitness + updateNucleotideMutation(genome, m);
				case AMINO_ACIDS:
					return logFitness + updateAminoAcidMutation(genome, m, mutationSite);
			}

			assert(false);
			return 0;
		} else
			return logFitness;
	}

	private double updateAminoAcidMutation(Genome genome, Mutation m, int mutationSite) {
		codon.setNucleotide(0, genome.getNucleotide(mutationSite * 3));
		codon.setNucleotide(1, genome.getNucleotide(mutationSite * 3 + 1));
		codon.setNucleotide(2, genome.getNucleotide(mutationSite * 3 + 2));

		byte oldState = codon.getAminoAcid(0);

		if (oldState == AminoAcid.STP) {
			/*
						 * It could be that we already had a stop codon, but only in case of
						 * multiple mutations at the same amino acid site, and the first single
						 * mutation resulted in a stop codon...
						 *
						 * To handle that cleanly, we would need to group mutations by amino acid site
						 * before updating the fitness but that is simply too cumbersome.
						 * The alternative option is to recompute the whole thing for this rare event,
						 * which we pursue here.
						 */
			return computeLogFitness(genome);
		} else {
			double result = -logFitness[mutationSite][codon.getAminoAcid(0)];

			codon.setNucleotide(m.position % 3, m.state);

			byte newState = codon.getAminoAcid(0);

			if (newState == AminoAcid.STP)
				result += Double.NEGATIVE_INFINITY;
			else
				result += logFitness[mutationSite][newState];

			return result;
		}
	}

	private double updateNucleotideMutation(Genome genome, Mutation m) {
		return -logFitness[m.position][genome.getNucleotide(m.position)]
				+logFitness[m.position][m.state];
	}

	public double getLogFitness(int i, byte state) {
		return logFitness[i][state];
	}

	protected void setLogFitness(int i, byte state, double f) {
		logFitness[i][state] = f;
	}

	private SimpleSequence codon = new SimpleSequence(3);
	private double[][] logFitness;
}