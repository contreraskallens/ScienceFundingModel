import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Double2D;
import sim.util.IntBag;

/**
 * Labs are steppable that do move, do research, and apply for funding each turn.
 * They have parameters that can evolve when reproducing: effort.
 * Each time they publish, the accumulate prestige.
 * When applying for funding, labs calculate their score according to the parameters.
 * They have a location on the epistemic landscape.
 * They store grants at a bag that measures how many years each postdoc of the lab has left.
 * Stoppable stores the switch to kill them from the simulation when dying.
 */
class Lab implements Steppable {
    private final int labId;
    double effort;
    double prestige;
    int xLocationInLandscape;
    int yLocationInLandscape;
    IntBag grants;
    int numberOfPostdocs;
    Stoppable stoppable;
    private int age;
    private double scoreForApplying;
    private double probabilityOfMoving = 0.5; // the probability of changing location to a nearby topic each given cycle.
    private double probabilityOfRandomMove = 0.1; // the probability that, in the case of changing location, it will be to a random one.
    private double innovativenessOfTopic;
    private double relativePrestige;

    /**
     * Creates lab in a specified location of landscape and with a specified unique identifier.
     * Other attributes initialized as 0.
     *
     * @param labId                Unique identifier for the lab
     * @param xLocationInLandscape X dimension of the location of the lab in the epistemic landscape
     * @param yLocationInLandscape Y dimension of the location of the lab in the epistemic landscape.
     */
    Lab(int labId, int xLocationInLandscape, int yLocationInLandscape) {
        this.labId = labId;
        this.xLocationInLandscape = xLocationInLandscape;
        this.yLocationInLandscape = yLocationInLandscape;
        this.age = 0;
        this.numberOfPostdocs = 0;
        this.prestige = 0;
        this.grants = new IntBag();
    }

    /**
     * Each step, lab grows a cycle older, has a probability of changing topic determined by model parameters,
     * does research, applies for funding, and publishes results, then updates the funding.
     *
     * @param state The simulation state.
     */
    @Override
    public void step(SimState state) {
        ScienceFunding simulation = (ScienceFunding) state;
        age++;
        if (simulation.schedule.getSteps() != 0) {
            clearFunding();
            checkFunding();
        }
        updateTopic(simulation, simulation.getLocationOfLaboratories());
        doResearch(simulation, simulation.getPublicationRecordOfTopics(), simulation.getEpistemicLandscape());
        updateFunding();
    }

    /**
     * Removes from the grants those that have 0 years left.
     * Loops through bag of grants and each time it finds one <= 0,
     * removes it from the bag and restarts loop.
     */
    private void clearFunding() {
        if (this.grants.size() > 0) {
            for (int i = 0; i < this.grants.size(); i++) {
                int myGrant = this.grants.get(i);
                if (myGrant <= 0) {
                    this.grants.removeNondestructively(i);
                    i--;
                }
            }
        }
    }

    /**
     * For each active grant (> 0 years left), add a postdoc for this year.
     */
    private void checkFunding() {
        this.numberOfPostdocs = grants.size(); // for each active grant, add a postdoc for this year. this function is called after grants with 0 years remaining were cleaned.
    }

    /**
     * Changes the lab topic with some probability. At this point, this is a random walk.
     * There are two parameters that control this method: the probability of moving at all, probabilityOfMoving,
     * and the probability that instead of doing a random walk around the current location, lab will move to a random topic.
     *
     * @param state              The simulation state.
     * @param epistemicLandscape The epistemic landscape where labs are situated.
     */
    private void updateTopic(ScienceFunding state, SparseGrid2D epistemicLandscape) {

        if (state.random.nextDouble() < probabilityOfMoving) {
            if (state.random.nextDouble() < this.probabilityOfRandomMove) {
                xLocationInLandscape = state.random.nextInt(state.getSizeOfLandscape());
                yLocationInLandscape = state.random.nextInt(state.getSizeOfLandscape());
            } else {
                if (state.random.nextBoolean()) {
                    xLocationInLandscape++;
                } else {
                    xLocationInLandscape--;
                }
                if (state.random.nextBoolean()) {
                    yLocationInLandscape++;
                } else {
                    yLocationInLandscape--;
                }
            }
            if (xLocationInLandscape >= state.getSizeOfLandscape()) { // after movement, cap location at 0 - 199.
                xLocationInLandscape = 199;
            }
            if (xLocationInLandscape < 0) {
                xLocationInLandscape = 0;
            }
            if (yLocationInLandscape >= state.getSizeOfLandscape()) {
                yLocationInLandscape = 199;
            }
            if (yLocationInLandscape < 0) {
                yLocationInLandscape = 0;
            }
            epistemicLandscape.setObjectLocation(this, this.xLocationInLandscape, this.yLocationInLandscape);
        }
    }

    /**
     * First, labs apply for funding by calling function applyToGrant.
     * Each member of the lab (PI + postdocs) attempts to do research this turn with a probability.
     *
     * @param state              The simulation state
     * @param publicationSpace   The grid that stores the publications per topic
     * @param epistemicLandscape The epistemic landscape grid.
     */
    private void doResearch(ScienceFunding state, IntGrid2D publicationSpace, DoubleGrid2D epistemicLandscape) {

        boolean appliedToGrant = applyToGrant(state, epistemicLandscape);
        int numberOfResearchers = 1 + this.numberOfPostdocs;
        for (int i = 0; i < numberOfResearchers; i++) {

            /*
             The probability of doing research for each member of the lab is modified by its effort level.
             The amount that effort hinders productivity is specified in parameter ScienceFunding.costOfEffortConstant.
             If the lab applied for funding, the productivity of the PI (i = 0) is also affected by parameter ScienceFunding.costOfApplyingForFunding.
             */
            double probabilityOfResearch = 1 - (state.getCostOfEffortConstant() * Math.log10(effort));
            if (appliedToGrant && i == 0) {
                probabilityOfResearch = probabilityOfResearch * state.getCostOfApplyingForFunding();
            }
            if (state.random.nextDouble() < probabilityOfResearch) {

                /*
                In case the member of the lab does research, it is decided if the research is going to be novel or a replication.
                The probability of each research attempt to be a replication is determined by ScienceFunding.probabilityOfReplication
                This happens only if there is at least one publication in the topic that the lab is in right now.
                 */
                boolean researchIsReplication = false;
                if (state.random.nextDouble() < state.getProbabilityOfReplication() && publicationSpace.get(xLocationInLandscape, yLocationInLandscape) > 0) {
                    researchIsReplication = true;
                }

                /*
                 The truth of the hypothesis being tested is decided. this is decided with the base rate of the
                 topic that the lab is currently in.
                  */
                boolean hypothesisIsTrue;
                hypothesisIsTrue = state.random.nextDouble() < epistemicLandscape.get(xLocationInLandscape, yLocationInLandscape);

                /*
                 The capability of the lab to detect false positives is determined
                 both by the default statistical power of the field (ScienceFunding.powerLevel)
                  */
                double labFalsePositiveRate = state.getPowerLevel() / (1 + (1 - state.getPowerLevel()) * this.effort); // calculate false positive rate.

                /*
                 Labs can claim that the hypothesis is true or false (publishingPositiveEffect)
                 The truth of their claim is evaluated.
                  */
                boolean labIsRight;
                boolean publishingPositiveEffect;
                if (hypothesisIsTrue) {
                    if (state.random.nextDouble() < state.getPowerLevel()) {
                        labIsRight = true;
                        publishingPositiveEffect = true;
                    } else {
                        labIsRight = false;
                        publishingPositiveEffect = false;
                    }

                    /*
                     If the hypothesis is false, the probability of the lab being right depends on
                     its falsePositiveRate, calculated previously.
                      */
                } else {
                    if (state.random.nextDouble() < labFalsePositiveRate) {
                        labIsRight = false;
                        publishingPositiveEffect = true;
                    } else {
                        labIsRight = true;
                        publishingPositiveEffect = false;
                    }
                }

                /*
                Loads Globals to store the number of publications of this cycle
                 and the number of false discoveries published.
                 */
                Globals globalsObject = state.getGlobalsObject();

                /*
                 If a lab is publishing a wrong result, with a probability of ScienceFunding.effectivenessOfPeers,
                 the publication is rejected/
                  */
                if (!labIsRight && (state.random.nextDouble() < state.getEffectivenessOfPeerReviewers())) {
                } else {
                    if (publishingPositiveEffect) {

                        /*
                         In the case of a publication of positive results, the publication is immediately accepted.
                         Probability of publishing negative results are controlled by parameter
                         ScienceFunding.probabilityOfPublishingNegative.
                         The global measures are updated, and the base rate of the topic of the publication is increased.
                         In the study was a replication, the lab obtains 0.5 of prestige. else, it receives 1.0.
                          */
                        globalsObject.addPublications(); // add one to publication counter
                        int currentPublicationsTopic = publicationSpace.get(this.xLocationInLandscape, this.yLocationInLandscape);
                        publicationSpace.set(this.xLocationInLandscape, this.yLocationInLandscape, currentPublicationsTopic + 1);
                        LandscapeUtils.increaseAndDisperse(epistemicLandscape, this.xLocationInLandscape, this.yLocationInLandscape, state.getIncreaseInBaseRate());
                        if (!labIsRight) {
                            globalsObject.addFalseDiscoveries();
                        }
                        if (researchIsReplication) {
                            this.prestige += 0.5;
                        } else {
                            this.prestige += 1;
                        }

                    }
                    if (!publishingPositiveEffect && (state.random.nextDouble() < state.getProbabilityOfPublishingNegative())) {
                        globalsObject.addPublications();
                        if (!labIsRight) {
                            globalsObject.addFalseDiscoveries();
                        }
                        int currentPublicationsTopic = publicationSpace.get(this.xLocationInLandscape, this.yLocationInLandscape);
                        publicationSpace.set(this.xLocationInLandscape, this.yLocationInLandscape, currentPublicationsTopic + 1);
                        LandscapeUtils.increaseAndDisperse(epistemicLandscape, this.xLocationInLandscape, this.yLocationInLandscape, state.getIncreaseInBaseRate());
                        if (researchIsReplication) {
                            this.prestige += 0.5;
                        } else {
                            this.prestige += 1;
                        }
                    }
                }
            }
        }
    }

    /**
     * When applying to funding, lab calculates its score for the process based on the parameters set in ScienceFunding,
     * weightOfInnovationInFunding and weightOfPrestigeInFunding. See model description for the difference in both.
     * After calculating its score, the lab adds itself to the funding agency's list of applicant with a probability.
     *
     * @param state              The Simulation State casted as ScienceFunding
     * @param epistemicLandscape The epistemic landscape grid
     * @return A boolean value. True if the lab applied for funding, false if it didn't.
     */
    private boolean applyToGrant(ScienceFunding state, DoubleGrid2D epistemicLandscape) {
        Agency fundingAgency = state.getAgency();
        ScienceMaster scienceMaster = state.getScienceMaster();

        double baseRateOfTopic = epistemicLandscape.get(xLocationInLandscape, yLocationInLandscape);
        innovativenessOfTopic = 1 - ((Math.log10(baseRateOfTopic / state.getInitialBaseRate())) / (Math.log10(0.5 / state.getInitialBaseRate())));
        relativePrestige = prestige / scienceMaster.getHighestPublication();

        scoreForApplying = state.getWeightOfInnovationInFunding() * innovativenessOfTopic + state.getWeightOfPrestigeInFunding() * relativePrestige;

        if (state.random.nextDouble() < state.getProbabilityOfApplyingForFunding()) {
            fundingAgency.addToApplicants(this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reduce the grants in the lab's bag of grants by 1 cycle left.
     * The grants with 0 cycles left will be cleaned THE NEXT TURN.
     * If lab doesn't have grants, nothing happens.
     */
    private void updateFunding() {
        if (this.grants.size() > 0) {
            for (int i = 0; i < this.grants.size(); i++) {
                int myGrant = this.grants.get(i) - 1;
                this.grants.set(i, myGrant);
            }
        }
    }

    public double getScoreForApplying() {
        return scoreForApplying;
    }

    //region Getters

    /**
     * This setter is used when including error in the funding agency's ranking of scores.
     *
     * @param newScore The new value for this lab's score.
     */
    public void setScoreForApplying(double newScore) {
        scoreForApplying = newScore;
    }

    public double getPrestige() {
        return prestige;
    }

    public int getNumberOfPostdocs() {
        return numberOfPostdocs;
    }

    public int getAge() {
        return age;
    }

    public int getLabId() {
        return labId;
    }

    public Double2D getLocation() {
        return new Double2D(xLocationInLandscape, yLocationInLandscape);
    }

    public int[] getGrants() {
        return grants.toArray();
    }

    public double getInnovativenessOfTopic() {
        return innovativenessOfTopic;
    }

    public double getRelativePrestige() {
        return relativePrestige;
    }
    //endregion
}
