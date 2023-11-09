package santa.simulator;

import santa.simulator.compartments.CompartmentEpoch;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import santa.simulator.compartments.Compartment;
import santa.simulator.compartments.Compartments;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: Simulation.java,v 1.11 2006/07/19 12:53:05 kdforc0 Exp $
 */
public class Simulation {

    private List<SimulationEpoch> epochs;
    private Compartments compartments;
    private static Logger memlogger = Simulator.memlogger;
    
    public Simulation(Compartments compartments) {
        this.compartments = compartments;
        
        // split CompartmentEpochs so that they have the same time slices
        this.epochs = new ArrayList<>();
        int compartmentEpochIndex = 0;
        boolean cont = true;
        
         while (cont) {
            int minGenerations = Integer.MAX_VALUE;
            ArrayList<CompartmentEpoch> compartmentEpochs = new ArrayList();
            
            for (Compartment compartment: compartments) {
                CompartmentEpoch currentCompartmentEpoch = compartment.getEpochs().get(compartmentEpochIndex);
                compartmentEpochs.add(currentCompartmentEpoch);
                
                if (minGenerations > currentCompartmentEpoch.getGenerationCount()) {
                    minGenerations = currentCompartmentEpoch.getGenerationCount();
                }
            }
            
            for (Compartment compartment: compartments) {
                List<CompartmentEpoch> compartmentsEpochs = compartment.getEpochs();
                CompartmentEpoch currentCompartmentEpoch = compartmentsEpochs.get(compartmentEpochIndex);
                
                if (minGenerations < currentCompartmentEpoch.getGenerationCount()) {
                    compartmentsEpochs.add(compartmentEpochIndex + 1, new CompartmentEpoch(
                        currentCompartmentEpoch.getName(),
                        currentCompartmentEpoch.getGenerationCount() - minGenerations,
                        currentCompartmentEpoch.getFitnessFunction(),
                        currentCompartmentEpoch.getMutator(),
                        currentCompartmentEpoch.getReplicator()));
                }
                
                currentCompartmentEpoch.setGenerationCount(minGenerations);
                
                if (compartmentsEpochs.size() <= compartmentEpochIndex + 1)
                    cont = false;
            }
            
            this.epochs.add(new SimulationEpoch(compartmentEpochs, minGenerations));
            
            /* 
             * TODO:
             * Add check for duplicate epoch names
             * Add check / deal with different overall epoch length
            */
            
            compartmentEpochIndex++;
        }
    }
    
    public void run(int replicate, Logger logger) {
        for (Compartment compartment: compartments) {
            compartment.initalize(replicate, logger);
        }
        
        int generation = 1;

        int epochCount = 0;

        for (SimulationEpoch epoch:epochs) {
            EventLogger.setEpoch(epochCount);

            generation = epoch.run(this, logger, generation);
            
            boolean allDead = true;
            
            for (Compartment compartment: compartments) {
                if (!compartment.getPopulation().getCurrentGeneration().isEmpty()) {
                    allDead = false;
                    break;
                }
            }
        
            if (allDead) {
            	System.err.println("Population crashed after "+generation+" generations.");
            	return;
            }
            epochCount++;
        }
        
        for (Compartment compartment: compartments) {
            compartment.cleanup(replicate, logger);
        }
    }
   
    public Compartments getCompartments() {
        return compartments;
    }
}
