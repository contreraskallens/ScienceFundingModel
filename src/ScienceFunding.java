import sim.engine.SimState;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Double2D;

public class ScienceFunding extends SimState {

    private final int sizeOfLandscape = 200;
    private final int numberOfLabs = 100;
    private final double initialBaseRate = 0.1;
    private final int numberOfEstablishedTopics = 0;
    private final int frequencyOfGlobalsSchedule = 100;
    private final double initialEffort = 75;
    private final double powerLevel = 0.8;
    private final double costOfEffortConstant = 0.2;
    private final double probabilityOfReplication = 0.3;
    private final double probabilityOfPublishingNegative = 0.5;
    private final double increaseInBaseRate = 0;
    private final double effectivenessOfPeerReviewers = 0.2;
    private final double probabilityOfPostdocAtStart = 0.05;
    private final double costOfApplyingForFunding = 0.2;
    private final double probabilityOfApplyingForFunding = 1;
    private final double weightOfInnovationInFunding = 0;
    private final double weightOfPrestigeInFunding = 1;
    private final boolean lotteryOfFunding = false;
    private final double probabilityOfEffortMutation = 0.1;
    private final double standardDeviationOfEffortMutation = 10;
    private final int maximumTopicMutationDistance = 2;
    //region Parameters and Objects
    private Bag bagOfAllLabs;
    private int latestIdAssigned;
    private Agency agencyObject;
    private ScienceMaster scienceMasterObject;
    private Globals globalsObject;
    private DoubleGrid2D epistemicLandscape = new DoubleGrid2D(sizeOfLandscape, sizeOfLandscape, initialBaseRate);
    private SparseGrid2D locationOfLaboratories = new SparseGrid2D(sizeOfLandscape, sizeOfLandscape);
    private IntGrid2D publicationRecordOfTopics = new IntGrid2D(sizeOfLandscape, sizeOfLandscape, 0);
    //endregion

    /**
     * Construct a science funding object with a random seed
     *
     * @param seed random seed so that multiple runs can be the same
     */
    public ScienceFunding(long seed) {
        super(seed);
    }

    //region Methods

    /**
     * Main method loops the schedule class as per Mason manual.
     *
     * @param args various arguments to control execution flow, like -for N, -parallel P, -repeat R.
     *             Full list in Mason manual, pg. 91
     */
    public static void main(String[] args) {
        {
            doLoop(ScienceFunding.class, args);
            System.exit(0);
        }
    }

    /**
     * Start the simulation by clearing grids, allocating the established topics,
     * creating labs and assigning them to a topic, and scheduling the objects.
     */
    @Override
    public void start() {
        super.start();
        locationOfLaboratories.clear();
        bagOfAllLabs = new Bag();

        Bag allEstablishedTopics = new Bag();
        if (numberOfEstablishedTopics > 0) {
            for (int i = 0; i < numberOfEstablishedTopics; i++) {
                Double2D establishedTopic;
                int xDimensionOfTopic;
                int yDimensionOfTopic;
                do {
                    xDimensionOfTopic = random.nextInt(sizeOfLandscape);
                    yDimensionOfTopic = random.nextInt(sizeOfLandscape);
                    establishedTopic = new Double2D(xDimensionOfTopic, yDimensionOfTopic);
                }
                while (allEstablishedTopics.contains(establishedTopic));
                allEstablishedTopics.add(establishedTopic);
                LandscapeUtils.increaseAndDisperse(epistemicLandscape, (int) establishedTopic.x, (int) establishedTopic.y, 0.499);
            }
        }

        for (int i = 0; i < numberOfLabs; i++) {
            Double2D topicOfLab;
            int labTopicX;
            int labTopicY;
            if (numberOfEstablishedTopics > 0) {
                topicOfLab = (Double2D) allEstablishedTopics.get(random.nextInt(allEstablishedTopics.size()));
                int xMutationFromEstablished = random.nextInt(3);
                int yMutationFromEstablished = random.nextInt(3);
                if (random.nextBoolean()) { // then, randomly decide the direction of your distance.
                    xMutationFromEstablished = -1 * xMutationFromEstablished;
                }
                if (random.nextBoolean()) {
                    yMutationFromEstablished = -1 * yMutationFromEstablished;
                }
                labTopicX = (int) topicOfLab.x + xMutationFromEstablished;
                labTopicY = (int) topicOfLab.y + yMutationFromEstablished;
            } else {
                topicOfLab = new Double2D(random.nextInt(sizeOfLandscape), random.nextInt(sizeOfLandscape));
                labTopicX = (int) topicOfLab.x;
                labTopicY = (int) topicOfLab.y;
            }
            if (labTopicX >= sizeOfLandscape) {
                labTopicX = sizeOfLandscape - 1;
            }
            if (labTopicX < 0) {
                labTopicX = 0;
            }
            if (labTopicY >= sizeOfLandscape) {
                labTopicY = sizeOfLandscape - 1;
            }
            if (labTopicY < 0) {
                labTopicY = 0;
            }
            Lab schedulingLab = new Lab(i, labTopicX, labTopicY);
            latestIdAssigned = i;

            if (random.nextDouble() < probabilityOfPostdocAtStart) {
                schedulingLab.numberOfPostdocs += 1; // This postdoc will last only one turn.
            }

            schedulingLab.effort = initialEffort;
            bagOfAllLabs.add(schedulingLab);
            locationOfLaboratories.setObjectLocation(schedulingLab, labTopicX, labTopicY);
            schedulingLab.stoppable = schedule.scheduleRepeating(schedulingLab, 1, 1);
        }

        scienceMasterObject = new ScienceMaster();
        schedule.scheduleRepeating(this.scienceMasterObject, 0, 1);

        agencyObject = new Agency();
        schedule.scheduleRepeating(this.agencyObject, 2, 1);

        globalsObject = new Globals();
        schedule.scheduleOnce(this.globalsObject);
        schedule.scheduleRepeating(this.globalsObject, 3, frequencyOfGlobalsSchedule);
    }

    /**
     * Increase the id number for future lab creation.
     * This function is called by scienceMaster when creating a new lab.
     */
    public void increaseLatestId() {
        latestIdAssigned++;
    }
    //endregion

    //region Getters

    /**
     * The first set of getters are used by other objects to access parameters of the simulation and
     * and objects that have been scheduled.
     */
    public Agency getAgency() {
        return agencyObject;
    }

    public DoubleGrid2D getEpistemicLandscape() {
        return epistemicLandscape;
    }

    public SparseGrid2D getLocationOfLaboratories() {
        return locationOfLaboratories;
    }

    public IntGrid2D getPublicationRecordOfTopics() {
        return publicationRecordOfTopics;
    }

    public Bag getBagOfAllLabs() {
        return bagOfAllLabs;
    }

    public int getLatestIdAssigned() {
        return latestIdAssigned;
    }

    public ScienceMaster getScienceMaster() {
        return scienceMasterObject;
    }

    public Globals getGlobalsObject() {
        return globalsObject;
    }

    public int getSizeOfLandscape() {
        return sizeOfLandscape;
    }

    public double getPowerLevel() {
        return powerLevel;
    }

    public double getCostOfEffortConstant() {
        return costOfEffortConstant;
    }

    public double getProbabilityOfReplication() {
        return probabilityOfReplication;
    }

    public double getProbabilityOfPublishingNegative() {
        return probabilityOfPublishingNegative;
    }

    public double getIncreaseInBaseRate() {
        return increaseInBaseRate;
    }

    public double getEffectivenessOfPeerReviewers() {
        return effectivenessOfPeerReviewers;
    }

    public double getCostOfApplyingForFunding() {
        return costOfApplyingForFunding;
    }

    public double getProbabilityOfApplyingForFunding() {
        return probabilityOfApplyingForFunding;
    }

    public double getWeightOfInnovationInFunding() {
        return weightOfInnovationInFunding;
    }

    public double getWeightOfPrestigeInFunding() {
        return weightOfPrestigeInFunding;
    }

    public double getProbabilityOfEffortMutation() {
        return probabilityOfEffortMutation;
    }

    public int getMaximumTopicMutationDistance() {
        return maximumTopicMutationDistance;
    }

    public boolean getLotteryOfFunding() {
        return lotteryOfFunding;
    }

    public double getInitialBaseRate() {
        return initialBaseRate;
    }

    public double getStandardDeviationOfEffortMutation() {
        return standardDeviationOfEffortMutation;
    }

    /**
     * The remaining getters are used in visualization through ScienceFundingWithUI
     */

    public double getFalseDiscoveryRate() {
        return this.globalsObject.getFalseDiscoveryRateLastWindow();
    }

    public double getRateOfDiscoveries() {
        return this.globalsObject.getProportionOfTopicsExplored();
    }

    public double getDiscoveredMean() {
        return this.globalsObject.getMeanBaseRate();
    }

    public double[] getDiscoveredDistribution() {
        return this.globalsObject.getBaseRateDistribution();
    }

    public double getDiscoveredStandardDev() {
        return this.globalsObject.getBaseRateSDev();
    }

    public double getPublicationMean() {
        return this.globalsObject.getMeanPublicationsPerTopic();
    }

    public int[] getPublicationDistribution() {
        return this.globalsObject.getPublicationsPerTopicDistribution();
    }

    public double getPublicationStandardDev() {
        return this.globalsObject.getPublicationsPerTopicSDev();
    }

    public double getFundsMean() {
        return this.globalsObject.getMeanTotalFundsLastWindow();
    }

    public double getFundStandardDev() {
        return this.globalsObject.getTotalFundsSDev();
    }

    public double[] getFundsDistribution() {
        return this.globalsObject.getTotalFundsDistribution();
    }

    public double getFundsGini() {
        return this.globalsObject.getTotalFundsGiniLastWindow();
    }

    public double getPostdocNumberMean() {
        return this.globalsObject.getPostdocNumberMeanLastWindow();
    }

    public double[] getPostdocNumberDistribution() {
        return this.globalsObject.getPostdocNumberDistribution();
    }

    public double getPostdocNumberGini() {
        return this.globalsObject.getPostdocNumberGiniLastWindow();
    }

    public double getPostdocNumberStandardDev() {
        return this.globalsObject.getPostdocNumberSDev();
    }
}
