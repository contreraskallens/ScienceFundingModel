import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.DoubleBag;

import java.io.IOException;
import java.util.Arrays;

//TODO: globals working is wonky. Think better aggregation and all that. ScienceMaster seems to delete everything and aggregation feels pointless. This class smells weird.

class Globals implements Steppable {

    //region Fields
    /*
    Next parameters control aggregation frequency and the size of the window.
    The per-turn measures are updated every n turns, where n is frequencyOfGlobalsAggregation.
    In that turn, the x last turns are aggregated, where x is aggregationWindow.
     */
    private final int frequencyOfGlobalsAggregation = 10;
    private final double aggregationWindow = 10;

    private DoubleBag allFDRLastWindow;
    private DoubleBag allMeanTotalFundsLastWindow;
    private DoubleBag allTotalFundsGiniLastWindow;
    private DoubleBag allPostdocNumberMeanLastWindow;
    private DoubleBag allPostdocNumberGiniLastWindow;

    private double numberOfPublicationsThisTurn;
    private double falseDiscoveriesThisTurn;
    private double FDRThisTurn;

    private double[] baseRateDistribution;
    private int[] publicationsPerTopicDistribution;
    private double[] totalFundsDistribution;
    private double[] postdocNumberDistribution;

    private double meanBaseRate;
    private double baseRateSDev;
    private double proportionOfTopicsExplored;
    private double meanPublicationsPerTopic;
    private double publicationsPerTopicSDev;

    private double falseDiscoveryRateLastWindow;
    private double meanTotalFundsLastWindow;
    private double totalFundsGiniLastWindow;
    private double postdocNumberMeanLastWindow;
    private double postdocNumberGiniLastWindow;
    //endregion

    /**
     * Constructor sets all global fields to 0 to initiate data collection.
     */
    public Globals() {
        this.falseDiscoveriesThisTurn = 0;
        this.allFDRLastWindow = new DoubleBag();
        this.numberOfPublicationsThisTurn = 0;
        this.falseDiscoveryRateLastWindow = 0;
        this.proportionOfTopicsExplored = 0;
        this.meanBaseRate = 0;
        this.baseRateSDev = 0;
        this.baseRateDistribution = new double[0];
        this.meanPublicationsPerTopic = 0;
        this.publicationsPerTopicSDev = 0;
        this.publicationsPerTopicDistribution = new int[0];
        this.meanTotalFundsLastWindow = 0;
        this.allMeanTotalFundsLastWindow = new DoubleBag();
        this.totalFundsDistribution = new double[0];
        this.totalFundsGiniLastWindow = 0;
        this.allTotalFundsGiniLastWindow = new DoubleBag();
        this.postdocNumberMeanLastWindow = 0;
        this.allPostdocNumberMeanLastWindow = new DoubleBag();
        this.postdocNumberGiniLastWindow = 0;
        this.allPostdocNumberGiniLastWindow = new DoubleBag();
        this.postdocNumberDistribution = new double[0];
    }

    /**
     * Each turn, Globals step and measure the globals for this turn.
     * Every X turns, determined by ScienceFunding.frequencyOfGlobalsAggregation, the measures for each turn
     * are aggregated through averaging of the last Y turns, determined by aggregationWindow.
     * They are then written to a file by constructing an Outputter object.
     *
     * @param state The simulation state. Not necessary to cast as (ScienceFunding) here.
     */
    @Override
    public void step(SimState state) {
        ScienceFunding simulation = (ScienceFunding) state;
        if (state.schedule.getSteps() % frequencyOfGlobalsAggregation == 0) {
            updateGlobalsAggregationAndWrite((ScienceFunding) state);
        } else {
            updateGlobalsThisTurn(simulation);
        }
    }

    /**
     * Generates global measures for this turn.
     * FDR, mean and gini total funds, mean and gini postdoc number are then stored for aggregation later through
     * updateGLobalsAggregation.
     * After measuring these and adding them to the bags that hold all measures in the last X turns (determined by
     * aggregationWindow), the temporary measure fields for this turn's false discovery rate are reset.
     * Distributions and statistics in base rate and publications per topic are updated every turn, so they are not reset.
     *
     * @param state The simulation state cast as ScienceFunding.
     */
    private void updateGlobalsThisTurn(ScienceFunding state) {
        FDRThisTurn = falseDiscoveriesThisTurn / numberOfPublicationsThisTurn;
        if (numberOfPublicationsThisTurn == 0 || falseDiscoveriesThisTurn == 0) { // avoid dividing by 0
            falseDiscoveriesThisTurn = 0;
        }
        allFDRLastWindow.add(FDRThisTurn);

        baseRateDistribution = state.getEpistemicLandscape().toArray();
        meanBaseRate = calculateMean(baseRateDistribution);
        baseRateSDev = calculateStandardDev(baseRateDistribution, meanBaseRate);
        publicationsPerTopicDistribution = state.getPublicationRecordOfTopics().toArray();
        int numberOfExploredTopics = 0;
        for (int numberOfPubsThisTopic : publicationsPerTopicDistribution) {
            if (numberOfPubsThisTopic > 0) {
                numberOfExploredTopics++;
            }
        }
        proportionOfTopicsExplored = (double) numberOfExploredTopics / publicationsPerTopicDistribution.length;
        meanPublicationsPerTopic = calculateMean(publicationsPerTopicDistribution);
        publicationsPerTopicSDev = calculateStandardDev(publicationsPerTopicDistribution, meanPublicationsPerTopic);


        totalFundsDistribution = new double[state.getBagOfAllLabs().size()]; // Allocate arrays for total funds, total number of postdocs.
        postdocNumberDistribution = new double[state.getBagOfAllLabs().size()];
        /*
        Loop through labs and populate the arrays. Save the measures for this turn in the all...LastWindow array.
        After that, remove oldest measures until array has size specified by aggregationWindow using removeNonDestructively
        to preserve the order of the bag.
        Use method meanAndGini to obtain those measures.
         */
        for (int i = 0; i < state.getBagOfAllLabs().size(); i++) {
            Lab thisLab = (Lab) state.getBagOfAllLabs().get(i);
            double thisLabTotalFunds = 0;
            for (int n = 0; n < thisLab.grants.size(); n++) {
                thisLabTotalFunds += thisLab.grants.get(n);
            }
            totalFundsDistribution[i] = thisLabTotalFunds;
            postdocNumberDistribution[i] = thisLab.grants.size();
        }

        double[] totalFundsMeanAndGini = meanAndGini(totalFundsDistribution);
        double totalFundsMeanThisTurn = totalFundsMeanAndGini[0];
        double totalFundsGiniThisTurn = totalFundsMeanAndGini[1];
        allMeanTotalFundsLastWindow.add(totalFundsMeanThisTurn);
        allTotalFundsGiniLastWindow.add(totalFundsGiniThisTurn);

        double[] postdocNumberMeanAndGini = meanAndGini(postdocNumberDistribution);
        double postdocNumberMeanThisTurn = postdocNumberMeanAndGini[0];
        double postdocNumberGiniThisTurn = postdocNumberMeanAndGini[1];
        allPostdocNumberMeanLastWindow.add(postdocNumberMeanThisTurn);
        allPostdocNumberGiniLastWindow.add(postdocNumberGiniThisTurn);
        resetThisTurnGlobals();
    }

    /**
     * This function updates all of the global measures that aggregate over the determined window.
     * Size of window is determined by aggregationWindow.
     * The aggregation is performed through different bags associated with the measures.
     * After measures are collected and aggregated, write to file using Outputter.
     *
     * @param state The simulation state, casted as ScienceFunding.
     */
    private void updateGlobalsAggregationAndWrite(ScienceFunding state) {
        updateGlobalsThisTurn(state);
        falseDiscoveryRateLastWindow = aggregateGlobal(allFDRLastWindow, aggregationWindow);
        meanTotalFundsLastWindow = aggregateGlobal(allMeanTotalFundsLastWindow, aggregationWindow);
        totalFundsGiniLastWindow = aggregateGlobal(allTotalFundsGiniLastWindow, aggregationWindow);
        postdocNumberMeanLastWindow = aggregateGlobal(allPostdocNumberMeanLastWindow, aggregationWindow);
        postdocNumberGiniLastWindow = aggregateGlobal(allPostdocNumberGiniLastWindow, aggregationWindow);

        /*
        Construct an Outputter object. This writes globals to file through Outputter's construction method.
         */
        try {
            Outputter fileWriter = new Outputter(state);
            fileWriter = null;
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /**
     * Calculates the mean of the values in an array of doubles.
     *
     * @param array An array of doubles.
     * @return The mean of the values of the array as a double.
     */
    private double calculateMean(double[] array) {
        double mean = 0;
        for (double thisDouble : array) {
            mean += thisDouble;
        }
        return mean / array.length;
    }

    /**
     * Calculates the mean of the values in an array of integers. Difference with version for doubles
     * is that it casts them as values to avoid truncating all decimal places.
     *
     * @param array An array of integers.
     * @return The mean of the values of the array as a double.
     */
    private double calculateMean(int[] array) {
        double mean = 0;
        for (int thisInt : array) {
            mean += (double) thisInt;
        }
        return mean / array.length;
    }

    /**
     * Calculates the mean of the values in an array of doubles and the Gini Index of the distribution of those values.
     * For Gini Index, uses simplified expression found in https://en.wikipedia.org/wiki/Gini_coefficient#Alternate_expressions.
     * Uses Arrays.sort()
     *
     * @param array An array of doubles.
     * @return An array of doubles with length 2. [0] is the mean of the array, [1] is the Gini index.
     */
    private double[] meanAndGini(double[] array) {
        /*
        Gini calculation determines the parts of the equation separately. In a non-increasingly ordered array of values
        Yi, where i = 1 .... n:
        First, it calculates the numerator (2 * sum(i * Yi)). Then, calculates the denominator (n * sum(Yi)). After
        solving the division between the numerator and the denominator, it calculates the right hand of the equation:
        n + 1 / n.
        */
        Arrays.sort(array);
        double giniLeftHandNumerator = 0;
        double giniLeftHandDenominator = 0;
        double mean = calculateMean(array);
        for (int i = 0; i < array.length; i++) {
            giniLeftHandNumerator += array[i] * (i + 1); // Add to the numerator each value times its place in the sorted array + 1.
            giniLeftHandDenominator += array[i]; // Add the value by itself to the denominator.
        }
        giniLeftHandNumerator *= 2;
        giniLeftHandDenominator *= array.length;

        double giniLeftHand = giniLeftHandNumerator / giniLeftHandDenominator;
        Double giniIndex = giniLeftHand - ((1 + array.length) / array.length);
        if (giniIndex.isNaN()) {
            giniIndex = (double) 0;
        }
        return new double[]{mean, giniIndex};
    }

    /**
     * Calculates the standard deviation of the values in an array of doubles. Uses Math.sqrt.
     *
     * @param array An array of doubles.
     * @param mean  The mean of the array supplied as double.
     * @return The standard deviation of the array, as a double.
     */
    private double calculateStandardDev(double[] array, double mean) {
        double sumOfSquaredDeviations = 0;
        for (double thisDouble : array) {
            double deviation = thisDouble - mean;
            double squaredDeviation = deviation * deviation;
            sumOfSquaredDeviations += squaredDeviation;
        }
        sumOfSquaredDeviations /= array.length;
        return Math.sqrt(sumOfSquaredDeviations);
    }

    /**
     * Calculates the standard deviation of the values in an array of ints. Uses Math.sqrt.
     * The difference with method for doubles is that this version casts values as doubles.
     *
     * @param array An array of integers.
     * @param mean  The mean of the array as a double.
     * @return The standard deviation of the supplied array as a double.
     */
    private double calculateStandardDev(int[] array, double mean) {
        double sumSquaredDeviations = 0;
        for (int thisInt : array) {
            double deviation = ((double) thisInt) - mean;
            double squaredDeviation = deviation * deviation;
            sumSquaredDeviations += squaredDeviation;
        }
        sumSquaredDeviations /= array.length;
        return Math.sqrt(sumSquaredDeviations);
    }

    /**
     * Sets every measure that updates each turn to 0.
     */
    private void resetThisTurnGlobals() {
        this.falseDiscoveriesThisTurn = 0;
        this.numberOfPublicationsThisTurn = 0;
    }

    /**
     * Aggregates the values in a bag of doubles by averaging them.
     * Before aggregating, it prunes the bag non-destructively until the number of items in the bag
     * matches the number of items requested.
     *
     * @param bagOfMeasures     A bag of doubles with values to aggregate.
     * @param aggregationWindow The number of last steps that should be aggregated
     * @return The average of the values of the bag after pruning to desired length as a double.
     */
    private double aggregateGlobal(DoubleBag bagOfMeasures, double aggregationWindow) {
        while (bagOfMeasures.size() > aggregationWindow) {
            bagOfMeasures.removeNondestructively(0);
        }
        double aggregatedMeasure = 0;
        for (int i = 0; i < bagOfMeasures.size(); i++) {
            aggregatedMeasure += bagOfMeasures.get(i);
        }
        aggregatedMeasure /= aggregationWindow;
        if (aggregatedMeasure == Double.NaN) {
            return 0;
        } else {
            return aggregatedMeasure;
        }
    }

    /**
     * Adds 1 to the total number of publications for this turn.
     */
    public void addPublications() {
        this.numberOfPublicationsThisTurn++;
    }

    /**
     * Adds 1 to the total number of false discoveries of this turn.
     */
    public void addFalseDiscoveries() {
        this.falseDiscoveriesThisTurn++;
    }


    //region Getters

    public double getFalseDiscoveryRateLastWindow() {
        return falseDiscoveryRateLastWindow;
    }

    public double getProportionOfTopicsExplored() {
        return proportionOfTopicsExplored;
    }

    public double getMeanBaseRate() {
        return meanBaseRate;
    }

    public double getBaseRateSDev() {
        return baseRateSDev;
    }

    public double[] getBaseRateDistribution() {
        return baseRateDistribution;
    }

    public double getMeanPublicationsPerTopic() {
        return meanPublicationsPerTopic;
    }

    public double getPublicationsPerTopicSDev() {
        return publicationsPerTopicSDev;
    }

    public int[] getPublicationsPerTopicDistribution() {
        return publicationsPerTopicDistribution;
    }

    public double getMeanTotalFundsLastWindow() {
        return meanTotalFundsLastWindow;
    }

    public double[] getTotalFundsDistribution() {
        return totalFundsDistribution;
    }

    public double getTotalFundsGiniLastWindow() {
        return totalFundsGiniLastWindow;
    }

    public double getPostdocNumberMeanLastWindow() {
        return postdocNumberMeanLastWindow;
    }

    public double getPostdocNumberGiniLastWindow() {
        return postdocNumberGiniLastWindow;
    }

    public double[] getPostdocNumberDistribution() {
        return postdocNumberDistribution;
    }
    //endregion
}