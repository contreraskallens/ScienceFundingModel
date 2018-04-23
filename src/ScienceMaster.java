import sim.engine.*;
import sim.field.grid.SparseGrid2D;
import sim.util.*;
import java.util.ArrayList;
import java.util.Comparator;

class ScienceMaster implements Steppable {
    // It who determines the life and death of science labs. This class creates new labs and kills the old labs.

    // fields and parameters //

    private double highestPublication; // the highest publication record measured for t-1. used to calculate the utility of labs, which in turn determines funding allocation.

    // methods //

    @Override
    public void step(SimState state){
        ScienceFunding simulation = (ScienceFunding) state; // cast state as ScienceFunding to access objects and parameters
        Globals theGlobals = simulation.getGlobalsObject(); // access Globals to reset numbers each turn

        // each step, the ScienceMaster updates the highest prestige for labs in the previous cycle, determines the lab that will die, and creates a new lab.

        theGlobals.resetGlobals(); // reset Globals each turn to catch measures only for this time step. ScienceMaster does it so it's the first thing that happens.
        updateHighest(simulation);
        Lab dyingLab = killALab(simulation);
        createALab(simulation, dyingLab);
    }

    private Lab killALab(ScienceFunding state){
        // to kill a lab, the ScienceMaster chooses 10 random labs from all labs. then, from that list of 10, it picks the oldest one.
        // if there's more than one oldest lab, it picks one at random from them. it determines the oldest lab by sorting the set of chosen labs by age.
        // the chosen lab is removed from the schedule and the landscapes.

        Bag allLabs = state.getBagOfAllLabs();
        SparseGrid2D spaceLabs = state.getLocationOfLaboratories();

        Bag chosenLabs = new Bag(); // bag to store the labs that are chosen to participate in the drawing
        for(int i = 0; i < 10; i++){// chose 10 random labs
            Lab aLab;
            do{
                aLab = (Lab) allLabs.get(state.random.nextInt(allLabs.size()));
            } while (chosenLabs.contains(aLab)); // do this until you have 10 different labs, with no repeats.
            chosenLabs.add(aLab);
        }

        chosenLabs.sort(Comparator.comparing(Lab::getAge)); // sort bag by age.
        Lab dyingLab = (Lab) chosenLabs.pop(); // get the oldest one.
        dyingLab.stoppable.stop(); // remove from schedule using the lab's stoppable.
        spaceLabs.remove(dyingLab); // remove it from epistemic landscape.
        return dyingLab; // return the lab that died to be stored for this turn. it will be removed from the list of all labs by next method, so that it is considered in the drawing for new lab creation.
    }

    private void createALab(ScienceFunding state, Lab dyingLab){

        // to create a new lab, the ScienceMaster chooses a lab from all available labs and creates a list of all labs to draw at random which lab will be reproduced.
        // only labs with postdocs are considered in the drawing. the lab who will die is considered in the drawing. each lab appears x times on the list, where x is their total number of postdocs.
        // the new lab is created with all of the same properties as the lab that was chosen to be reproduced.
        // there's a possibility (controlled by parameter ScienceFunding.probabilityOfMutationEffort) that the inherited effort will mutate an amount drawn from a gaussian with mean 0 and standard dev 1. effort is capped at 1 and 100.
        // the new lab inherits the reproduced lab topic with a slight variation, controlled by parameter ScienceFunding.topicMutation.

        Bag allLabs = state.getBagOfAllLabs();
        SparseGrid2D spaceLabs = state.getLocationOfLaboratories();

        // allocate the list of labs for the drawing.

        ArrayList<Lab> ticketList = new ArrayList<>(); // create an empty arraylist for the drawing.
        for (int i = 0; i < allLabs.size(); i++) { // loop through all labs. if the lab doesn't have postdocs, ignore them. if they do, loop through them adding the lab to the ticket list once per postdoc.
            Lab thisLab = (Lab) allLabs.get(i);
            int numberOfPostdocs = thisLab.numberOfPostdocs;
            if(numberOfPostdocs == 0){
                continue;
            }
            for (int n = 0; n < numberOfPostdocs; n++) {
                ticketList.add(thisLab);
            }
        }

        // choose the lab to be reproduced and create a new lab with its same properties.

        if (ticketList.size() > 0) { // only reproduce labs if there is at least one postdoc in the simulation. this could only happen if the initial probability of having a postdoc is too low.
            Lab reproducedLab = ticketList.get(state.random.nextInt(ticketList.size())); // grab a random lab from the ticket bag. more postdocs, more chances.

            // create the lab
            state.increaseLatestId(); // update the global id assignment, and get the id for the new lab.
            Lab newLab = new Lab(state.getLatestIdAssigned(), reproducedLab.xLocationInLandscape, reproducedLab.yLocationInLandscape); // create a new lab with the same topic as the chosen lab, and a new ID.

            // topic mutation //

            int xVariation = state.random.nextInt(state.getMaximumTopicMutationDistance() + 1); // generate a random number between 0 and parameter topicMutation to determine how far from the parent topic the new lab will be located.
            int yVariation = state.random.nextInt(state.getMaximumTopicMutationDistance() + 1);
            if (state.random.nextBoolean()) { // randomly choose the direction of the distance.
                xVariation *= -1;
            }
            if (state.random.nextBoolean()) {
                yVariation *= -1;
            }
            newLab.xLocationInLandscape += xVariation;
            newLab.yLocationInLandscape += yVariation;
            if (newLab.xLocationInLandscape >= state.getSizeOfLandscape()) {
                newLab.xLocationInLandscape = state.getSizeOfLandscape() - 1;
            } // cap the new lab's topic mutation between 0 and 199.
            if(newLab.xLocationInLandscape < 0){newLab.xLocationInLandscape = 0;}
            if (newLab.yLocationInLandscape >= state.getSizeOfLandscape()) {
                newLab.yLocationInLandscape = state.getSizeOfLandscape() - 1;
            }
            if(newLab.yLocationInLandscape <0){newLab.yLocationInLandscape = 0;}

            // effort mutation //

            newLab.effort = reproducedLab.effort; // copy parent lab's level of effort.

            if (state.random.nextDouble() < state.getProbabilityOfEffortMutation()) { // with probability controlled by parameter ScienceFunding.probabilityOfMutationEffort, determine if the inherited level of effort will change.
                double effortMutation = state.random.nextGaussian(); // determine how much it will change by drawing from a gaussian distribution with mean 0 and standard dev 1.
                effortMutation *= state.getStandardDeviationOfEffortMutation();
                reproducedLab.effort += effortMutation;
                if (reproducedLab.effort > 100) {
                    reproducedLab.effort = 100;
                } // cap effort at 1 - 100.
                if(reproducedLab.effort < 1){reproducedLab.effort = 1;}
            }

            // remove old lab and add new lab//

            allLabs.remove(dyingLab); // remove old lab from list of all labs
            allLabs.add(newLab); // add new lab to list of all labs
            newLab.stoppable = state.schedule.scheduleRepeating(newLab,0, 1); // add new lab to schedule and allocate stoppable to kill in the future
            spaceLabs.setObjectLocation(newLab, newLab.xLocationInLandscape, newLab.yLocationInLandscape); // add new lab to epistemic landscape
        }
    }

    private void updateHighest(ScienceFunding state){
        // method to get the highest prestige achieved by a lab on the previous cycle.
        // this is used for the labs this turn to calculate their prestige relative to the record of the previous turn.
        // the record is stored as a static field in ScienceMaster.

        double highestYet = 0;
        for(int i = 0; i < state.getBagOfAllLabs().size(); i++){
            Lab aLab = (Lab) state.getBagOfAllLabs().get(i);
            double record = aLab.prestige;
            if(record > highestYet){
                highestYet = record;
            }
        }
        highestPublication = highestYet;
    }

    public double getHighestPublication(){
        return highestPublication;
    }
}
