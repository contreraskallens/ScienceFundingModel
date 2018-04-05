import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Double2D;
import sim.util.IntBag;
import sim.engine.*;


class Lab implements Steppable {

    // labs do research and apply to funding. they have a topic they are currently researching, which is a location in the
    // landscape, their own lefvel of effort to put on their research, a number of postdocs, each with their own duration,
    // and a utility function calculated using parameters ScienceFunding.weightOfInnovation and ScienceFunding.weightOfRecord.
    // in each cycle, a lab will attempt to apply for funding (affecting the PI's chances to research) and do and publish research.

    // objects and fields //

    private int age; // how many cycles has this lab been alive for?
    private final int labId; // tracks the unique identity of a specific lab

    double effort; // 1 to 100. initialized at 75 at the beginning of simulation, but can be mutated at creation after first allocation. determines detection of false positives.
    double clout; // rewards from publishing. 1 from publishing novel results, 0.5 from publishing replications.
    private double utility; // the score with which labs apply for funding. calculated using ScienceFunding.weightOfInnovation and ScienceFunding.weightOfRecord.

    int topicX; // location on the landscape.
    int topicY;

    int numberOfPostdocs; // total number of postdocs that the lab has.
    IntBag grants; // bag that stores the grants that the lab has won. each grant is stored as an int that represents the duration of the grant. the lab has 1 postdoc for each grant.

    Stoppable stoppable; // stoppable to kill the lab

    // parameters //

    private double probOfMoving = 0.5; // the probability of changing location to a nearby topic each given cycle.
    private double innovativenessTopic = 0.1; // the probability that, in the case of changing location, it will be to a random one.
    private double innovativeness;
    private double relativeRecord;

    // debugging //

    private boolean original;



    // constructor //

    Lab(int labId, int topicX, int topicY, boolean originality) { // create lab at designated location, with specified ID.
        this.labId = labId;
        this.topicX = topicX;
        this.topicY = topicY;
        this.age = 0;
        this.numberOfPostdocs = 0; // labs are initialized without postdocs.
        this.clout = 0; // the publication record is initialized as blank. publications don't carry over from parent lab.
        this.grants = new IntBag(); // grant bag is initialized empty.
        this.original = originality;
    }

    public void step(SimState state){
        // each step, a lab grows a cycle older, has a probability of changing topics determined by parameters, does research and publishes its results,
        // and updates its funding. when a lab updates its funding, it subtracts a cycle of each of its remaining grants.

        ScienceFunding simulation = (ScienceFunding) state;
        age++;
        if (simulation.schedule.getSteps() != 0) { // don't do this the first turn.
            clearFunding(); // at the beginning of the cycle, delete from your bag of grants every grant with 0 years old. that way, postdocs with 0 years left can participate in that year's funding and lab creation process.
            checkFunding(); // after deleting grants with 0 years left, update the total number of postdocs you have.
        }
        updateTopic(simulation, simulation.getLabs()); // with a parametrized probability, change your topic.
        doResearch(simulation, simulation.getPublications(), simulation.getLandscape());
        updateFunding(); // update your funding after a year of research, subtracting 1 from each grant.
    }

    private void checkFunding(){
        this.numberOfPostdocs = grants.size(); // for each active grant, add a postdoc for this year. this function is called after grants with 0 years remaining were cleaned.
    }

    private void updateTopic(ScienceFunding state, SparseGrid2D landscape){
        // this function changes the lab's topic with some probability. there's a probability of moving determined by parameter probOfMoving.
        // if the lab moves, by default, it performs a random walk horizontally and vertically for a distance of 1.
        // the parameter innovativenessTopic determines the probability that, instead of a random walk around its current topic,
        // the lab will move to a random location of the landscape.

        if (state.random.nextDouble() < probOfMoving) { // parametrized probability of moving.
            if (state.random.nextDouble() < this.innovativenessTopic) { // with a chance determined by innovativenessTopic, move to random location.
                topicX = state.random.nextInt(state.getSizeOfLandscape());
                topicY = state.random.nextInt(state.getSizeOfLandscape());
            } else { // if this is not an "innovative" randomly move around your current patch.
                if (state.random.nextBoolean()) { // true: moves right 1. false: moves left 1.
                    topicX++;
                } else {
                    topicX--;
                }
                if (state.random.nextBoolean()) { // true: moves up 1. false: moves down 1.
                    topicY++;
                } else {
                    topicY--;
                }
            }
            if (topicX >= state.getSizeOfLandscape()) { // after movement, cap location at 0 - 199.
                topicX = 199;
            }
            if (topicX < 0) {
                topicX = 0;
            }
            if (topicY >= state.getSizeOfLandscape()) {
                topicY = 199;
            }
            if (topicY < 0) {
                topicY = 0;
            }
            landscape.setObjectLocation(this, this.topicX, this.topicY); // change the lab's location in the landscape representation.
        }
    }

    private void doResearch(ScienceFunding state, IntGrid2D publications, DoubleGrid2D landscape) {
        // each turn, a lab does research after trying to apply to a grant. the first step calls the functions applyToGrant,
        // which, with a probability of ScienceFunding.probabilityOfApplying, puts the lab on the list of labs applying for funding this turn.
        // the probability of doing research is further reduced for the PI (that is, the first member of the lab to go through the loop) if they applied for research this turn. how much it reduces it is determined by ScienceFunding.costOfapplication.

        boolean appliedToGrant = applyToGrant(state, landscape); // did you apply to a grant this turn?

        int numberOfResearchers = 1 + this.numberOfPostdocs; // how many will attempt to do research this turn? PI (1) + every postdoc

        for(int i = 0; i < numberOfResearchers; i++) { // loop through members of the lab

            // each member of the lab (that is, the PI + every postdoc) attempts to do research each turn ("numberOfResearchers").
            // for each number of researcher, there's a probability of X that the member of the lab will do research. that probability is 1 -
            // the effort of the lab (log10) multiplied by ScienceFunding.effortConstant, which represents how much extra effort hinders productivity.

            double  probabilityOfResearch = 1 - (state.getEffortConstant() * Math.log10(this.effort)); // calculate probability of doing research this turn
            if(appliedToGrant && i == 0) { // if you're the PI and you applied for a grant, get a reduced probability of doing research.
                probabilityOfResearch = probabilityOfResearch * state.getCostOfApplication(); // multiplied by constant.
            }

            if (state.random.nextDouble() < probabilityOfResearch) { // if it does research after all.

                // in case the member of the lab does research, it is decided if the research is going to be novel or a replication.
                // the probability of each research attempt to be a replication is determined by ScienceFunding.probabilityOfReplication. Moreover, it is required that there is at least 1 publication in the location of the landscape that the lab is currently in.

                boolean replication = false; // store if it's replication.
                if (state.random.nextDouble() < state.getProbabilityOfReplication() && publications.get(this.topicX, this.topicY) > 0) { // check if replication (left) and if there's anything to replicate (right)
                    replication = true;
                }

                // after deciding the novelty of the study, the truth of the hypothesis being tested is decided. this is decided
                // with the base rate of the patch of the landscape that the lab is currently in. the value (base rate) is the probability that the hypothesis is true.

                boolean hypothesisTruth; // is hypothesis true?
                hypothesisTruth = state.random.nextDouble() < landscape.get(this.topicX, this.topicY); // value on landscape is the probability that it's true

                // the capability of the lab to detect false positives is determined by the default statistical power of the field (ScienceFunding.powerLevel)
                // and the lab's effort level.

                double labFalsePositive = state.getPowerLevel() / (1 + (1 - state.getPowerLevel()) * this.effort); // calculate false positive rate.

                // labs can claim that the hypothesis is true or false (publishingEffect), and thus can be right, if they correctly detect the truth, or wrong, if they don't.

                boolean labIsRight; // does the lab correctly detect truth of hypothesis?
                boolean publishingEffect; // positive = true; negative = false;

                // if the hypothesis is true, a lab has a probability of ScienceFunding.powerLevel of detecting that positive effect.
                // in case the researcher correctly detects the positive effect, the lab is right (labIsRight = true), and it will publish a positive effect (publishingEffect = true).
                // if it doesn't detect the positive effect, the lab is wrong (labIsRight = false) and it will publish a negative effect (publishingEffect = false).

                if (hypothesisTruth) { // if the hypothesis is true
                    if (state.random.nextDouble() < state.getPowerLevel()) { // with probability powerLevel, you detect positive effects
                        labIsRight = true; // is correct in thinking that it's positive
                        publishingEffect = true; // will publish positive
                    } else { // else, you think it's negative when it's positive.
                        labIsRight = false; // lab is wrong in thinking that it's negative
                        publishingEffect = false; // will publish negative
                    }

                    // if the hypothesis is false, the probability of the lab being right depends on its falsePositiveRate, calculated previously.
                    // if it falsely detects a positive effect, the lab is wrong (labIsRight = false) and it publishes a positive effect (publishingEffect = true).
                    // if it correctly identifies the negative effect, the lab is right (labIsRight = true) and it publishes a negative effect (publishingEffect = false).

                } else { // if the hypothesis is false
                    if (state.random.nextDouble() < labFalsePositive) { // with probability false positive rate, you detect a positive effect
                        labIsRight = false; // you think it's positive when it's negative
                        publishingEffect = true; // you publish positive
                    } else {
                        labIsRight = true; // you think it's negative when it's negative
                        publishingEffect = false; // you publish negative
                    }
                }

                Globals theGlobals = state.getTheGlobals(); // point to globals to increase number of false discoveries and number of publications for this turn

                // if a lab is publishing a wrong result (labIsRight = false), with a probability of ScienceFunding.effectivenessOfPeers, the process is stopped and the publication is rejected (nothing happens).

                if (!labIsRight && (state.random.nextDouble() < state.getEffectivenessOfPeers())) { // you don't publish if you're wrong AND the peers detect you.
                    // nothing happens
                }

                // if the researcher is right, or if they're wrong but not detected, the normal publishing effect ensues.
                else {
                    if (publishingEffect) {

                        // in the case of a publication of positive results, the publication is immediately accepted. the global counter of publications is increased by one,
                        // and the publications for the location the lab is currently in is also increased by one. moreover, the base rate of that location is increased by ScienceFunding.discoveryPower.
                        // if the publication is of a false detection (labIsRight = false), the global counter of false discoveries is also increased.
                        // finally, the cumulative reward for publication of the lab (clout) is calculated. if the study was a replication, the lab obtains 0.5 of clout. else, it receives 1.0.

                        theGlobals.addPublications(); // add one to publication counter
                        int currentPublicationsTopic = publications.get(this.topicX, this.topicY); // how many publications are there in this topic?
                        publications.set(this.topicX, this.topicY, currentPublicationsTopic + 1); // add a publication to this topic
                        LandscapeUtils.increaseAndDisperse(landscape, this.topicX, this.topicY, state.getDiscoveryPower()); // increase by ScienceFunding.discoveryPower every time you publish
                        if (!labIsRight) {
                            theGlobals.addFalseDiscoveries();
                        } // if you're publishing a false discovery, add 1 to globals tracker

                        if (replication) { // if it's a replication'
                            this.clout += 0.5; // add 0.5 to your publication payoff
                        } else {
                            this.clout += 1; // it it's a novel result, add 1 to your publication payoff
                        }

                    }

                    // if the effect being published is negative, the probability of its publication is determined by the parameter ScienceFunding.probabilityOfPublishingNegative.
                    // after that, the process is identical to that of the positive effects.

                    if (!publishingEffect && (state.random.nextDouble() < state.getProbabilityOfPublishingNegative())) { // if you're publishing negative, roll for negative publication. if you get it, publish.
                        theGlobals.addPublications();
                        if (!labIsRight) {
                            theGlobals.addFalseDiscoveries();
                        } // if you're publishing a false discovery, add 1 to globals.
                        int currentPublicationsTopic = publications.get(this.topicX, this.topicY); // how many publications are there in this topic?
                        publications.set(this.topicX, this.topicY, currentPublicationsTopic + 1); // add a publication to this topic
                        LandscapeUtils.increaseAndDisperse(landscape, this.topicX, this.topicY, state.getDiscoveryPower()); // increase by 0.001 every time you publish
                        if (replication) { // if it's a replication, add 0.5 to your payoffs.
                            this.clout += 0.5;
                        } else { // if it's not a replication, add 1
                            this.clout += 1;
                        }
                    }
                }
            }
        }
    }

    private boolean applyToGrant(ScienceFunding state, DoubleGrid2D landscape) {

        // when applying to funding, a lab calculates its application score (utility) based on the parameters provided,
        // ScienceFunding.weightOfInnovation and ScienceFunding.weightOfRecord. innovation determines how much a lab is rewarded
        // for publishing in less explored topics (locations with relatively low base rate). record determines how much a lab
        // is rewarded for their clout in relationship with the best record of the previous cycle.

        Agency theAgency = state.getAgency(); // get the instance of the agency scheduled in the simulation using the getter.
        ScienceMaster theScienceMaster = state.getScienceMaster();
        // calculate utility //

        double thisRate = landscape.get(this.topicX, this.topicY); // base rate of the topic the lab is currently researching.
        this.innovativeness = 1 - ((Math.log10(thisRate / state.getInitialBaseRate())) / (Math.log10(0.5 / state.getInitialBaseRate()))); // innovativeness of the lab's current location as compared to the established topics (base rate = 0.5).
        relativeRecord = this.clout / theScienceMaster.getHighestPublication(); // the lab's clout in relationship with the previous cycle's highest clout.
        this.utility = state.getWeightOfInnovation() * innovativeness + state.getWeightOfRecord() * relativeRecord; // final utility is the sum of weighted innovativeness and relative record.
        // after calculating its utility and saving it into its utility field, a lab adds itself to the agency's list of applicants
        // with a probability of ScienceFunding.probabilityOfApplying. if the lab applies, the function returns true; else, it returns false.

        if (state.random.nextDouble() < state.getProbabilityOfApplying()) { // roll for application.
            theAgency.addToApplicants(this); // add myself to the bag of applicants for money
            return true; //
        } else {
            return false;
        }
        //
    }

    private void updateFunding() {

        // reduce the grants in the lab's bag of grants by 1 cycle left. the grants with 0 cycles left will be cleaned THE NEXT TURN.
        // this occurs at the end of the cycle.

        if (this.grants.size() > 0) {  // if you don't have any grants, don't do anything.
            for (int i = 0; i < this.grants.size(); i++) { // loop through grants
                int myGrant = this.grants.get(i) - 1; // diminish by one cycle for the cycle just past.
                this.grants.set(i, myGrant); // replace previous value in the bag for new one.
            }
        }
    }

    private void clearFunding() {
        // clear grants that have 0 cycles left. this occurs at the beginning of the cycle.

        if (this.grants.size() > 0) {
            for (int i = 0; i < this.grants.size(); i++) { // loop through grants, if it's 0 delete it.
                int myGrant = this.grants.get(i);
                if (myGrant <= 0) {
                    this.grants.removeNondestructively(i); // remove yourself if you ran out.
                    i--; // check bag again as now it's in a different order.
                }
            }
        }
    }

    // getters and setters for agency and object inspector in UI console //

    public double getUtility() {
        return utility;
    }

    public void setUtility(double newUtility){
        this.utility = newUtility;
    }

    public double getClout(){
        return clout;
    }

    public int getNumberOfPostdocs(){
        return numberOfPostdocs;
    }

    public int getAge(){
        return age;
    }

    public int getLabId(){
        return labId;
    }

    public Double2D getLocation(){
        return new Double2D(topicX, topicY);
    }

    public int[] getGrants(){
        return grants.toArray();
    }

    public double getInnovativeness(){
        return innovativeness;
    }

    public double getRelativeRecord() {
        return relativeRecord;
    }

    public boolean isOriginal() {
        return original;
    }
}
