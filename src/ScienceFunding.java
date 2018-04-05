import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;

public class ScienceFunding extends SimState {

    // objects and fields //

    private int runNumber = 0; // tracks run number for batches
    private Bag allLabs; // bag with all labs to use on functions that iterate on all labs.
    private int latestId; // track labs to assign ids when ScienceMaster creates new labs.
    private Agency theAgency; // store pointer to instance of agency so it can be accessed by labs through SimState.
    private ScienceMaster theScienceMaster; // store pointer to instance of ScienceMaster
    private Globals theGlobals; // store pointer to instance of Globals

    // model parameters //

    private final int sizeOfLandscape = 200; // width and height of square landscape.
    private final int numberOfLabs = 100; // number of concurrent labs
    private final double initialBaseRate = 0.1; // initial value for each cell in the landscape.
    private final int numberOfEstablishedTopics = 0; // number of topics that are initiated with maximum (0.5) base rate.
    private final int frequencyOfGlobals = 100; // controls the number of steps inbetween each updating of the model's statistics. higher number improve performance but make data coarser.

    // research and publication parameters //

    private final double initialEffort = 75; // effort that labs created at the beginning of the simulation will have.
    private final double powerLevel = 0.8; // The default statistical power for labs to detect false positives.
    private final double effortConstant = 0.2; // parameter that determines how much effort reduces research output.
    private final double probabilityOfReplication = 0.3; // parameter that determines how often a research attempt will be a replication.
    private final double probabilityOfPublishingNegative = 0.5; // probability of journals publishing negative results.
    private final double discoveryPower = 0; // increase in the base rate of the patch a lab is in when it publishes about it.
    private final double effectivenessOfPeers = 0.2; // probability that an incorrect result will be detected and thus not published.

    // funding parameters //

    private final double initialPostdoctProbability = 0.05; // probability of being assigned a postdoc at the beginning of the model.
    private final double costOfApplication = 0.2; // if PI applies for funding in a cycle, the probability of researching goes down by this much.
    private final double probabilityOfApplying = 1; // probability of applying for a grant each cycle
    private final double weightOfInnovation = 0; // how much innovation weighs in the allocation of fund. goes from -1 to 1
    private final double weightOfRecord = 1; // how much the record of a lab (clout) weighs in the allocation of funds. from 0 to 1. this plus weight of innovation must sum more than 0.
    private final boolean lottery = false; // if set to true, funds are distributed at random

    // mutation parameters //

    private final double probabilityOfMutationEffort = 0.1; // probability that the effort level of a lab mutates when reproducing itself.
    private final double standardDevOfMutation = 10; // standard deviation of the gaussian distribution that determines how much effort mutates.
    private final int topicMutation = 2; // maximum distance that a new lab will have from the topic inherited from the lab it came from at creation.

    // epistemic and publication landscape initialization //

    private DoubleGrid2D landscape = new DoubleGrid2D(sizeOfLandscape, sizeOfLandscape, initialBaseRate); // initialize underlying epistemic landscape
    private SparseGrid2D labs = new SparseGrid2D(sizeOfLandscape, sizeOfLandscape); // initialize plane of lab movement.
    private IntGrid2D publications = new IntGrid2D(sizeOfLandscape, sizeOfLandscape, 0); // initialize grid of publications.

    public ScienceFunding(long seed) {
        super(seed);
    }

    public void start(){
        super.start();

        // preparation //

        this.runNumber++; // at the start, increase the run number by 1.
        labs.clear(); // clear the location of all labs.
        Bag establishedTopics = new Bag(); // allocate a bag to store the established topics so they don't repeat by chance.
        allLabs = new Bag(); // initialize bag to store labs

        // allocation of established topics //
        if (numberOfEstablishedTopics > 0) {
            for (int i = 0; i < numberOfEstablishedTopics; i++) {
                // define and allocate established topics. generates random X and Y values, and use dispersal from landscapeUtils
                // to calculate how the increase to 0.5 from 0.001 affects the surrounding patches . loop repeats according to parameter "numberOfEstablishedTopics".

                Double2D establishedTopic; // topic is stored as a Double2D to take into DoubleGrid2D landscape.
                int xValue;
                int yValue;
                do {
                    xValue = random.nextInt(sizeOfLandscape);
                    yValue = random.nextInt(sizeOfLandscape);
                    establishedTopic = new Double2D(xValue, yValue);
                }
                while (establishedTopics.contains(establishedTopic)); // loop generates a random location. if the location is already in the bag of topics, repeat.
                establishedTopics.add(establishedTopic);

                LandscapeUtils.increaseAndDisperse(landscape, (int) establishedTopic.x, (int) establishedTopic.y, 0.499); // add 0.499 to the random patch that was picked. disperse to neighborhood.
            }
        }

        // allocation of labs near the established topics //

        for (int i = 0; i < numberOfLabs; i++) { // create labs in a loop controlled by the parameter numberOfLabs.

            Double2D myTopic;

            if (numberOfEstablishedTopics > 0) {
                myTopic = (Double2D) establishedTopics.get(random.nextInt(establishedTopics.size())); // gets the location of a random established topic.
            } else {
                myTopic = new Double2D(random.nextInt(sizeOfLandscape), random.nextInt(sizeOfLandscape));
            }


            int myXNearTopic = random.nextInt(3); // randomly generate your distance from the established topic.
            int myYNearTopic = random.nextInt(3);
            if (random.nextBoolean()) { // then, randomly decide the direction of your distance.
                myXNearTopic = -1 * myXNearTopic;
            }
            if (random.nextBoolean()) {
                myYNearTopic = -1 * myYNearTopic;
            }

            int myX = (int) myTopic.x + myXNearTopic;
            int myY = (int) myTopic.y + myYNearTopic;

            if (myX >= sizeOfLandscape) {
                myX = sizeOfLandscape - 1;
            } // if location exceeds the boundaries of landscape (0, 199) after movement, bound them to the limits of landscape.
            if (myX < 0) {
                myX = 0;
            }
            if (myY >= sizeOfLandscape) {
                myY = sizeOfLandscape - 1;
            }
            if (myY < 0) {
                myY = 0;
            }

            Lab thisLab = new Lab(i, myX, myY, true); // create lab using new location.

            latestId = i; // assign a labId according to place in initial for loop.

            if (random.nextDouble() < initialPostdoctProbability) {// chance of being assigned a postdoc at the beginning of the simulation is controlled by parameter initialPostdocProbability. this postdoc will last one year.
                thisLab.numberOfPostdocs += 1;
            }
            thisLab.effort = initialEffort; // initial effort of labs is controlled by parameter initialEffort.
            allLabs.add(thisLab); // adds lab to bag of all labs.
            labs.setObjectLocation(thisLab, myX, myY); // places lab on the landscape.
            thisLab.stoppable = schedule.scheduleRepeating(thisLab, 1, 1); // schedule the labs. it is scheduled after the simulation's instance of ScienceMaster. stoppable is stored to remove dying labs from the simulation.
        }

        // allocate and schedule other objects //

        this.theScienceMaster = new ScienceMaster(); // initialize and schedule ScienceMaster at the beginning of each cycle (priority 0)
        schedule.scheduleRepeating(this.theScienceMaster, 0, 1);

        this.theAgency = new Agency();  // initialize and schedule the funding agency, scheduled after the labs (priority 2). agency is also stored in a global pointer to be used by other objects via getter.
        schedule.scheduleRepeating(this.theAgency, 2, 1);

        this.theGlobals = new Globals(); // initialize and schedule the object that tracks the global properties of the simulation. scheduled after everything else (priority 3).
        schedule.scheduleOnce(this.theGlobals);
        schedule.scheduleRepeating(this.theGlobals, 3, frequencyOfGlobals); // interval is controlled by parameter frequencyOfGlobals.
    }

    // main method //

    public static void main(String[] args){
        {
            doLoop(ScienceFunding.class, args);
            System.exit(0);
        }
    }

    // getters //

    // returns the scheduled objects.
    public Agency getAgency() {
        return this.theAgency;
    }

    public DoubleGrid2D getLandscape(){
        return this.landscape;
    }

    public SparseGrid2D getLabs(){
        return this.labs;
    }

    public IntGrid2D getPublications(){
        return this.publications;
    }

    public Bag getAllLabs(){
        return this.allLabs;
    }

    public void increaseLatestId(){
        this.latestId++;
    }

    public int getLatestId(){
        return this.latestId;
    }

    public ScienceMaster getScienceMaster(){
        return this.theScienceMaster;
    }

    public Globals getTheGlobals(){
        return this.theGlobals;
    }

    // getters used for visualization in ScienceFundingWithUI. they return fields stored in the Globals object.

    public double getFalseDiscoveryRate(){
        return this.theGlobals.getFalseDiscoveryRate();
    }

    public double getRateOfDiscoveries(){
        return this.theGlobals.getRateOfDiscovery();
    }

    public double getDiscoveredMean() {
        return this.theGlobals.getDiscoveredMean();
    }

    public double[] getDiscoveredDistribution(){
        return this.theGlobals.getDiscoveredDistribution();
    }

    public double getDiscoveredStandardDev(){
        return this.theGlobals.getDiscoveredStandardDev();
    }

    public double getPublicationMean(){
        return this.theGlobals.getPublicationMean();
    }

    public int[] getPublicationDistribution(){
        return this.theGlobals.getPublicationDistribution();
    }

    public double getPublicationStandardDev(){
        return this.theGlobals.getPublicationStandardDev();
    }

    public double getFundsMean(){
        return this.theGlobals.getFundsMean();
    }

    public double getFundStandardDev(){
        return this.theGlobals.getFundsStandardDev();
    }

    public double[] getFundsDistribution(){
        return this.theGlobals.getFundsDistribution();
    }

    public double getFundsGini(){
        return this.theGlobals.getFundsGini();
    }

    public double getPostdocNumberMean(){
        return this.theGlobals.getPostdocNumberMean();
    }

    public double[] getPostdocNumberDistribution(){
        return this.theGlobals.getPostdocNumberDistribution();
    }

    public double getPostdocNumberGini(){
        return this.theGlobals.getPostdocNumberGini();
    }

    public double getPostdocNumberStandardDev(){
        return this.theGlobals.getPostdocNumberStandardDev();
    }

    public double getPostdocDurationMean(){
        return this.theGlobals.getPostdocDurationMean();
    }

    public double[] getPostdocDurationDistribution(){
        return this.theGlobals.getPostdocDurationDistribution();
    }

    public double getPostdocDurationStandardDev(){
        return this.theGlobals.getPostdocDurationStandardDev();
    }

    public double getPostdocDurationGini(){
        return this.theGlobals.getPostdocDurationGini();
    }

    // getters of parameters to avoid access bugs //

    public int getRunNumber() {
        return runNumber;
    }

    public int getSizeOfLandscape() {
        return sizeOfLandscape;
    }

    public double getPowerLevel() {
        return powerLevel;
    }

    public double getEffortConstant() {
        return effortConstant;
    }

    public double getProbabilityOfReplication() {
        return probabilityOfReplication;
    }

    public double getProbabilityOfPublishingNegative() {
        return probabilityOfPublishingNegative;
    }

    public double getDiscoveryPower() {
        return discoveryPower;
    }

    public double getEffectivenessOfPeers() {
        return effectivenessOfPeers;
    }

    public double getCostOfApplication() {
        return costOfApplication;
    }

    public double getProbabilityOfApplying() {
        return probabilityOfApplying;
    }

    public double getWeightOfInnovation() {
        return weightOfInnovation;
    }

    public double getWeightOfRecord() {
        return weightOfRecord;
    }

    public double getProbabilityOfMutationEffort() {
        return probabilityOfMutationEffort;
    }

    public int getTopicMutation() {
        return topicMutation;
    }

    public boolean getLottery() {
        return lottery;
    }

    public double getInitialBaseRate() {
        return initialBaseRate;
    }

    public double getStandardDevOfMutation() {
        return standardDevOfMutation;
    }

    // debugging //


    public double getAverageEffort() {
        return theGlobals.getAverageEffort();

    }
}
