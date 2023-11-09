package santa.simulator.compartments;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import santa.simulator.EventLogger;
import santa.simulator.Random;
import santa.simulator.genomes.GenePool;
import santa.simulator.genomes.GenomeDescription;
import santa.simulator.genomes.Sequence;
import santa.simulator.phylogeny.Phylogeny;
import santa.simulator.population.Population;
import santa.simulator.population.PopulationGrowth;
import santa.simulator.samplers.SamplingSchedule;
import santa.simulator.selectors.Selector;

/**
 *
 * @author Bradley R. Jones
 */
public class Compartment {
    private int populationSize;
    private String name;
    private Compartment.InoculumType inoculumType;
    private GenePool genePool;
    private List<CompartmentEpoch> epochs;
    private Selector selector;
    private SamplingSchedule samplingSchedule;
    private Population population;

    	public enum InoculumType {
		NONE,
		CONSENSUS,
		RANDOM,
		ALL
	};
	
	//Default constructor (dynamic)
    public Compartment (
            String name,
            int populationSize,
            Selector selector,
            PopulationGrowth growth,
            InoculumType inoculumType,
            GenePool genePool,
            List<CompartmentEpoch> epochs,
            SamplingSchedule samplingSchedule) {

        this.name = name;
        this.populationSize = populationSize;
        this.inoculumType = inoculumType;
        this.epochs = epochs;
        this.samplingSchedule = samplingSchedule;
        this.genePool = genePool;
        this.selector = selector;

        population = new Population(genePool, selector, growth, samplingSchedule.isSamplingTrees() ? new Phylogeny(populationSize) : null);
    }
    
    public void initalize(int replicate, Logger logger) {
        samplingSchedule.initialize(replicate);

        EventLogger.setReplicate(replicate);

        logger.finer("Initializing population: " + populationSize + " viruses.");

	    List<Sequence> inoculum = new ArrayList<>();
	    if (null == inoculumType) { // NONE
                // do nothing
            } else switch (inoculumType) {
            case CONSENSUS:
                inoculum.add(GenomeDescription.getConsensus());
                break;
            case ALL:
                inoculum.addAll(GenomeDescription.getSequences());
                break;
            case RANDOM:
                List<Sequence> sequences = GenomeDescription.getSequences();
                if (sequences.size() == 1) {
                    inoculum.add(sequences.get(0));
                } else {
                    inoculum.add(sequences.get(Random.nextInt(0, sequences.size() - 1)));
                }
                break;
            default:
                break;
        }
        population.initialize(inoculum, populationSize);
    }
    
    public void cleanup(int replicate, Logger logger) {
        samplingSchedule.cleanUp();
    }
    
    public GenePool getGenePool() {
        return genePool;
    }

    public Population getPopulation() {
        return population;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public SamplingSchedule getSamplingSchedule() {
        return samplingSchedule;
    }
    
    public String getName() {
        return name;
    }
    
    public List<CompartmentEpoch> getEpochs() {
        return epochs;
    }
}
