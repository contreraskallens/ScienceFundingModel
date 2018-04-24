import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * It who determines the life and death of science labs.
 * This class creates new labs and kills the old labs.
 * It also stores the highest publication prestige achieved the last turn for labs
 * to calculate their relative prestige.
 */
class ScienceMaster implements Steppable {
    //region Fields
    private double highestPrestigeLastTurn;
    //endregion

    /**
     * Each step, the ScienceMaster updates highest prestige for last turn,
     * determines the lab that will die, and creates a new lab.
     *
     * @param state The simulation state.
     */
    @Override
    public void step(SimState state) {
        ScienceFunding simulation = (ScienceFunding) state;
        Globals theGlobals = simulation.getGlobalsObject(); // access Globals to reset numbers each turn

        theGlobals.resetGlobals();
        updateHighest(simulation);
        Lab dyingLab = chooseDyingLab(simulation);
        createALab(simulation, dyingLab);
    }

    /**
     * To kill a lab, the ScienceMaster chooses 10 random labs from all of them.
     * It then picks the lab with the highest age from those 10 labs.
     * The chosen lab is removed from the simulation state and from the landscape of labs.
     * It will be removed from the bag of all labs by createALab. This allows the dying lab to participate
     * in the drawing of who will get to reproduce. Only after this the lab is completely removed from the simulation.
     *
     * @param state The simulation state cast as ScienceFunding.
     * @return Returns the lab that was killed.
     */
    private Lab chooseDyingLab(ScienceFunding state) {
        Bag allLabs = state.getBagOfAllLabs();
        SparseGrid2D locationOfLabs = state.getLocationOfLaboratories();

        Bag candidatesForDying = new Bag();
        for (int i = 0; i < 10; i++) {
            Lab thisLab;
            do {
                thisLab = (Lab) allLabs.get(state.random.nextInt(allLabs.size()));
            } while (candidatesForDying.contains(thisLab));
            candidatesForDying.add(thisLab);
        }

        candidatesForDying.sort(Comparator.comparing(Lab::getAge));
        Lab dyingLab = (Lab) candidatesForDying.pop();
        dyingLab.stoppable.stop();
        locationOfLabs.remove(dyingLab);
        return dyingLab;
    }

    /**
     * To create a new lab, the ScienceMaster chooses a postdoc from a lab to become a lab by itself (PI).
     * This choice is weighted by the number of postdocs that the labs to be reproduced have.
     * The ScienceMaster draws a lab from the list of all labs. Each lab appears on the drawing n times, where n
     * is the number of postdocs they have. Obviously, labs with 0 postdocs will not be considered in the drawing.
     * After a postdoc is chosen, it's immediately replaced by a new postdoc in that same lab, with the remaining
     * years of funding that the old postdoc had.
     * The lab that the new postdoc founds has similar effort and location in the landscape than the lab from where
     * it came, with slight variations controlled by parameters probabilityOfEffortMutation, standardDeviationOfEffortMutation,
     * and maximumTopicMutationDistance, all controlled in ScienceFunding.
     * The dying lab is considered for the reproduction drawing.
     * After the drawing, the old lab is removed from the Bag of all labs, and the new lab is added to it, in addition to
     * the schedule and the landscape.
     *
     * @param state    The Simulation State, cast as ScienceFunding.
     * @param dyingLab The lab that is going to be killed, chosen by chooseDyingLab.
     */
    private void createALab(ScienceFunding state, Lab dyingLab) {
        Bag allLabs = state.getBagOfAllLabs();
        SparseGrid2D locationOfLabs = state.getLocationOfLaboratories();

        ArrayList<Lab> candidatesForReproduction = new ArrayList<>(); // create an empty arraylist for the drawing.
        for (int i = 0; i < allLabs.size(); i++) { // loop through all labs. if the lab doesn't have postdocs, ignore them. if they do, loop through them adding the lab to the ticket list once per postdoc.
            Lab thisLab = (Lab) allLabs.get(i);
            int thisLabsNumberOfPostdocs = thisLab.numberOfPostdocs;
            if (thisLabsNumberOfPostdocs == 0) {
                continue;
            }
            for (int n = 0; n < thisLabsNumberOfPostdocs; n++) {
                candidatesForReproduction.add(thisLab);
            }
        }

        if (candidatesForReproduction.size() > 0) { // Only reproduce labs if there is at least one postdoc in the simulation.
            Lab reproducedLab = candidatesForReproduction.get(state.random.nextInt(candidatesForReproduction.size()));

            /*
            The lab is created by using the Lab constructing method.
            The id of the new lab is drawn from the field in ScienceFunding where the latest Id assigned is stored.
             */
            state.increaseLatestId();
            Lab newLab = new Lab(state.getLatestIdAssigned(), reproducedLab.xLocationInLandscape, reproducedLab.yLocationInLandscape); // create a new lab with the same topic as the chosen lab, and a new ID.

            /*
            Mutate the topic of the parent lab by adding a random distance to move in a random direction.
            This movement is capped at 0 - the size of the landscape determined in ScienceFunding.
             */
            int xVariation = state.random.nextInt(state.getMaximumTopicMutationDistance() + 1);
            int yVariation = state.random.nextInt(state.getMaximumTopicMutationDistance() + 1);
            if (state.random.nextBoolean()) {
                xVariation *= -1;
            }
            if (state.random.nextBoolean()) {
                yVariation *= -1;
            }
            newLab.xLocationInLandscape += xVariation;
            newLab.yLocationInLandscape += yVariation;
            if (newLab.xLocationInLandscape >= state.getSizeOfLandscape()) {
                newLab.xLocationInLandscape = state.getSizeOfLandscape() - 1;
            }
            if (newLab.xLocationInLandscape < 0) {
                newLab.xLocationInLandscape = 0;
            }
            if (newLab.yLocationInLandscape >= state.getSizeOfLandscape()) {
                newLab.yLocationInLandscape = state.getSizeOfLandscape() - 1;
            }
            if (newLab.yLocationInLandscape < 0) {
                newLab.yLocationInLandscape = 0;
            }

            /*
            Mutate the effort of the old lab by drawing from a gaussian distribution with mean 0
            and standard deviation controlled by parameter in ScienceFunding. The amount is determined
            after it's determined if the effort will mutate at all.
             */
            newLab.effort = reproducedLab.effort;
            if (state.random.nextDouble() < state.getProbabilityOfEffortMutation()) {
                double effortMutation = state.random.nextGaussian();
                effortMutation *= state.getStandardDeviationOfEffortMutation();
                reproducedLab.effort += effortMutation;
                if (reproducedLab.effort > 100) {
                    reproducedLab.effort = 100;
                }
                if (reproducedLab.effort < 1) {
                    reproducedLab.effort = 1;
                }
            }

            allLabs.remove(dyingLab);
            allLabs.add(newLab);
            newLab.stoppable = state.schedule.scheduleRepeating(newLab, 1, 1);
            locationOfLabs.setObjectLocation(newLab, newLab.xLocationInLandscape, newLab.yLocationInLandscape); // add new lab to epistemic landscape
        }
    }

    /**
     * Method gets the highest prestige achieved by a lab on the previous time step.
     * This is used by the labs this time step to calculate their prestige relative to the record of the previous turn.
     * ScienceMaster only does this because it's the first one on the schedule.
     *
     * @param state The Simulation State cast as ScienceFunding.
     */
    private void updateHighest(ScienceFunding state) {
        double highestPrestigeYet = 0;
        for (int i = 0; i < state.getBagOfAllLabs().size(); i++) {
            Lab thisLab = (Lab) state.getBagOfAllLabs().get(i);
            double thisLabsPrestige = thisLab.prestige;
            if (thisLabsPrestige > highestPrestigeYet) {
                highestPrestigeYet = thisLabsPrestige;
            }
        }
        highestPrestigeLastTurn = highestPrestigeYet;
    }

    //region Getters
    public double getHighestPrestigeLastTurn() {
        return highestPrestigeLastTurn;
    }
    //endregion
}
