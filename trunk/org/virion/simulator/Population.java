/*
 * Population.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package org.virion.simulator;

import org.virion.simulator.mutators.Mutator;
import org.virion.simulator.genomes.*;
import org.virion.simulator.replicators.Replicator;
import org.virion.simulator.selectors.Selector;
import org.virion.simulator.fitness.FitnessFunction;
import org.virion.simulator.fitness.FitnessFunctionFactor;

import java.util.Arrays;
import java.util.List;

/**
 * @author rambaut
 *         Date: Apr 22, 2005
 *         Time: 9:12:27 AM
 */
public class Population {

    public Population(int populationSize, GenePool genePool, Selector selector) {

        this.populationSize = populationSize;
        this.genePool = genePool;
        this.selector = selector;

        lastGeneration = new Virus[populationSize];
        currentGeneration = new Virus[populationSize];
    }

    public void initialize(List<Sequence> inoculum, FitnessFunction fitnessFunction) {
        Genome[] ancestors;

        genePool.initialize();

        ancestors = new Genome[inoculum.size()];
        for (int i = 0; i < ancestors.length; i++) {
            Sequence sequence = inoculum.get(i);
            ancestors[i] = genePool.createGenome(sequence);
            ancestors[i].setLogFitness(fitnessFunction.computeLogFitness(ancestors[i]));
        }

        if (ancestors.length > 1) {
            for (int i = 0; i < populationSize; i++) {
                Genome ancestor = ancestors[Random.nextInt(0, ancestors.length - 1)];
                currentGeneration[i] = new Virus(ancestor, null);
                lastGeneration[i] = new Virus();
                ancestor.incrementFrequency();
            }
        } else {
            for (int i = 0; i < populationSize; i++) {
                currentGeneration[i] = new Virus(ancestors[0], null);
                lastGeneration[i] = new Virus();
                ancestors[0].incrementFrequency();
            }
        }
    }

    public void updateFitness(FitnessFunction fitnessFunction) {
        genePool.updateFitness(fitnessFunction);
        statisticsKnown = false;
    }

    public void selectNextGeneration(Replicator replicator, Mutator mutator, FitnessFunction fitnessFunction) {

        selector.initializeSelector(currentGeneration);

        // first swap the arrays around
        Virus[] tmp = currentGeneration;
        currentGeneration = lastGeneration;
        lastGeneration = tmp;

        // then select the currentGeneration based on the last.
        for (int i = 0; i < populationSize; i++) {

            // replicate the parents to create a new virus
            replicator.replicate(currentGeneration[i], selector, mutator, fitnessFunction, genePool);

        }

        // then kill off the genomes in the last population.
        for (int i = 0; i < populationSize; i++) {
            genePool.killGenome(lastGeneration[i].getGenome());
        }

        statisticsKnown = false;
    }

    protected Virus[] getSample(int sampleSize) {
        Virus[] viruses = getCurrentGeneration();
        Object[] tmp = Random.nextSample(Arrays.asList(viruses), sampleSize);
        Virus[] sample = new Virus[tmp.length];
        System.arraycopy(tmp, 0, sample, 0, tmp.length);
        return sample;
    }

    public void estimateDiversity(int sampleSize) {
        Virus[] sample = getSample(sampleSize);
        
        maxDiversity = 0;
        meanDiversity = 0;

        int count = 0;
        for (int i = 0; i < sample.length; ++i) {
            for (int j = i+1; j < sample.length; ++j) {
                double d = computeDistance(sample[i], sample[j]);
                
                if (d > maxDiversity)
                    maxDiversity = d;
                meanDiversity += d;
                ++count;
            }
        }
        
        meanDiversity /= (double)count;
    }
    
    private double computeDistance(Virus virus1, Virus virus2) {
        if (virus1.getGenome() == virus2.getGenome())
            return 0;
        
        Sequence seq1 = virus1.getGenome().getSequence();
        Sequence seq2 = virus2.getGenome().getSequence();

        int distance = 0;
        
        for (int i = 0; i < GenomeDescription.getGenomeLength(); ++i) {
            if (seq1.getNucleotide(i) != seq2.getNucleotide(i))
                ++distance;
        }

        return distance;
    }

    private void collectStatistics() {

        double d = 0;
        maxFrequency = 0;

        mostFrequentGenome = 0;
        sumFitness = 0.0;
        minFitness = Double.MAX_VALUE;
        maxFitness = 0.0;

        for (int i = 0; i < populationSize; i++) {
            Genome genome = currentGeneration[i].getGenome();

            d += genome.getTotalMutationCount();

            if (genome.getFrequency() > maxFrequency) {
                mostFrequentGenome = i;
                maxFrequency = genome.getFrequency();
            }

            sumFitness += genome.getFitness();
            if (genome.getFitness() > maxFitness) {
                maxFitness = genome.getFitness();
            }
            if (genome.getFitness() < minFitness) {
                minFitness = genome.getFitness();
            }
        }

        meanFitness = sumFitness / populationSize;
        meanDistance = d / populationSize;

        statisticsKnown = true;
    }

    public int getMaxFrequency() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return maxFrequency;
    }

    public double getMaxFitness() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return maxFitness;
    }

    public double getMinFitness() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return minFitness;
    }

    public double getSumFitness() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return sumFitness;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public double getMeanDistance() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return meanDistance;
    }

    public double getMeanFitness() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return meanFitness;
    }

    public int getMostFrequentGenome() {
        if (!statisticsKnown) {
            collectStatistics();
        }
        return mostFrequentGenome;
    }

    public double getMaxDiversity() {
        return maxDiversity;
    }

    public double getMeanDiversity() {
        return meanDiversity;
    }

    public double[] getAlleleFrequencies(int site) {
        int[] freqs = genePool.getStateFrequencies(site);
        double[] normalizedFreqs = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) {
            normalizedFreqs[i] = freqs[i] / (double)populationSize;
        }
        return normalizedFreqs;
    }

    public Virus[] getCurrentGeneration() {
        return currentGeneration;
    }

    private final int populationSize;

    private final GenePool genePool;

    private final Selector selector;

    private Virus[] lastGeneration;
    private Virus[] currentGeneration;
    private int[] pickedParents;

    private boolean statisticsKnown = false;

    private int mostFrequentGenome;
    private int maxFrequency;
    private double meanDistance;

    private double meanFitness;
    private double sumFitness;
    private double minFitness;
    private double maxFitness;
    private double maxDiversity;
    private double meanDiversity;
    public GenePool getGenePool() {
        return genePool;
    }
}
