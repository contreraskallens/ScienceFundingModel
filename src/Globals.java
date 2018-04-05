import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.DoubleBag;

import java.io.IOException;
import java.util.Arrays;

class Globals implements Steppable {

    // fields //

    private double falseDiscoveries = 0; // total number of false discoveries
    private double numberOfPublications = 0; // total number of publications
    private double falseDiscoveryRate; // the rate of publications that are false discoveries.
    private DoubleBag falseDiscoveryRates; // store false discovery rates to aggregate on globals.
    private double rateOfDiscovery; // the proportion of topics that have been discovered (number of publications > 1).
    private double discoveredMean; // the mean baserate of each topic in the landscape.
    private double discoveredStandardDev;  // the standard deviation of the rates of the landscape.
    private double[] discoveredDistribution; // all base rates of the landscape in array form.
    private double publicationMean; // the mean number of publications of each topic.
    private double publicationStandardDev; // the standard deviation of the publications of each topic.
    private int[] publicationDistribution; // all values of publications in each topic in array form.
    private double fundsMean; // the mean total amount of funds (years of funding) of the labs.
    private DoubleBag fundsMeans; // bag for aggregation of means
    private double fundsStandardDev; // the standard deviation of the total amnount of funds of the labs.
    private double[] fundsDistribution; // all values of total amount of funds in array form.
    private double fundsGini; // the gini index of the distribution of total amount of funds in the lab population.
    private DoubleBag fundsGiniIndices; // bag for aggregation
    private double postdocNumberMean; // mean number of postdocs of every lab (total amount of grants)
    private DoubleBag postdocNumberMeans; // bag for aggregation
    private double postdocNumberStandardDev; // standard deviation of the number of postdocs.
    private double postdocNumberGini; // gini index of the distribution of number of postdocs.
    private DoubleBag postdocNumberGinis; // bag for aggregation
    private double[] postdocNumberDistribution; // all values of number of postdocs, in array form.
    private double postdocDurationMean; // the mean amount of cycles that the lab will have at least one postdoc ( 0 to 5).
    private DoubleBag postdocDurationMeans; // bag for aggregation
    private double[] postdocDurationDistribution; // the distribution of amount of cycles that labs will have at least one postdoc.
    private double postdocDurationStandardDev; // the standard deviation of the amount of cycles that labs will have at least one postdoc.
    private double postdocDurationGini; // the gini index of the distribution of the amount of cycles that labs will have at least one postdoc.
    private DoubleBag postdocDurationGinis; // bag for aggregation


    // debugging //
    private double averageEffort;


    public Globals() {
        this.falseDiscoveries = 0;
        this.falseDiscoveryRates = new DoubleBag();
        this.numberOfPublications = 0;
        this.falseDiscoveryRate = 0;
        this.rateOfDiscovery = 0;
        this.discoveredMean = 0;
        this.discoveredStandardDev = 0;
        this.discoveredDistribution = new double[0];
        this.publicationMean = 0;
        this.publicationStandardDev = 0;
        this.publicationDistribution = new int[0];
        this.fundsMean = 0;
        this.fundsMeans = new DoubleBag();
        this.fundsStandardDev = 0;
        this.fundsDistribution = new double[0];
        this.fundsGini = 0;
        this.fundsGiniIndices = new DoubleBag();
        this.postdocNumberMean = 0;
        this.postdocNumberMeans = new DoubleBag();
        this.postdocNumberStandardDev = 0;
        this.postdocNumberGini = 0;
        this.postdocNumberGinis = new DoubleBag();
        this.postdocNumberDistribution = new double[0];
        this.postdocDurationMean = 0;
        this.postdocDurationMeans = new DoubleBag();
        this.postdocDurationDistribution = new double[0];
        this.postdocDurationStandardDev = 0;
        this.postdocDurationGini = 0;
        this.postdocDurationGinis = new DoubleBag();
    }

    public void step(SimState state) {
        // each turn that it is scheduled (interval controlled by ScienceFunding.frequencyOfGlobals), the globals agent updates a
        // series of measures of the state of the simulation. these are stored in the object's static fields, and accessed by the UI console
        // and the Outputter.

        updateGlobals((ScienceFunding) state);
    }

    private void updateGlobals(ScienceFunding state) {

        // false discovery rate aggregated over 100 turns (average) //

        double falseDiscoveryThisTurn = falseDiscoveries / numberOfPublications;
        if (numberOfPublications == 0 || falseDiscoveries == 0) {
            falseDiscoveryThisTurn = 0;
        }

        falseDiscoveryRates.add(falseDiscoveryThisTurn);

        falseDiscoveryRate = aggregateGlobal(falseDiscoveryRates, 100);

        // landscape discovery mean and standard deviation //

        double[] landscapeArray = state.getLandscape().toArray(); // all base rates from the landscape.
        discoveredDistribution = landscapeArray; // the distribution of each base rate in array form.
        discoveredMean = calculateMean(landscapeArray); // the mean base rate of the landscape.
        discoveredStandardDev = calculateStandardDev(landscapeArray, discoveredMean); // standard deviation of the base rates of the landscape.

        // publication metrics //

        // topic publication rate //

        int[] pubsArray = state.getPublications().toArray();
        publicationDistribution = pubsArray; // all publications of every topic, in array form.
        int exploredTopics = 0; // number of topics with more than 0 publications
        for (int aPubsArray1 : pubsArray) { // loop through all topics and add 1 to exploredTopics if it has at least 1 publication.
            if (aPubsArray1 > 0) {
                exploredTopics++;
            }
        }
        rateOfDiscovery = (double) exploredTopics / pubsArray.length; // rate of discovery: proportion of topics with more than publications.

        // mean and s //

        publicationMean = calculateMean(pubsArray); // mean amount of publications.
        publicationStandardDev = calculateStandardDev(pubsArray, publicationMean); // standard deviation of the amount of publications per topic.

        // funds and postdoc metrics //

        double[] fundsArray = new double[state.getAllLabs().size()]; // allocate arrays for total funds, total number of postdocs, and years that lab will have at least 1 postdoc.
        double[] postdocNumberArray = new double[state.getAllLabs().size()];
        double[] postdocDurationArray = new double[state.getAllLabs().size()];

        for (int i = 0; i < state.getAllLabs().size(); i++) { // populate the arrays by looping through the labs.
            Lab aLab = (Lab) state.getAllLabs().get(i);
            double labTotalFunds = 0;
            int maxGrantSoFar = 0; // biggest grant = years the lab will have at least 1 postdoc.
            for (int n = 0; n < aLab.grants.size(); n++) { // loop through the grants of the lab to populate the total funds and search for the biggest one.
                labTotalFunds += aLab.grants.get(n);
                if (aLab.grants.get(n) > maxGrantSoFar) {
                    maxGrantSoFar = aLab.grants.get(n);
                }
            }
            fundsArray[i] = labTotalFunds; // sum of every grant
            postdocNumberArray[i] = aLab.grants.size(); // number of grants
            postdocDurationArray[i] = maxGrantSoFar; // biggest grant

        }

        // mean, gini, standard deviation of funds (aggregated over 100 turns) //

        fundsDistribution = fundsArray; // all values in array form.
        double[] fundsResults = meanAndGini(fundsArray); // use custom function to return mean and gini index of distribution. [mean, gini]
        double fundsMeanThisTurn = fundsResults[0];
        double fundsGiniThisTurn = fundsResults[1];

        fundsMeans.add(fundsMeanThisTurn);

        fundsMean = aggregateGlobal(fundsMeans, 100);

        fundsGiniIndices.add(fundsGiniThisTurn);
        fundsGini = aggregateGlobal(fundsGiniIndices, 100);

        if (fundsGiniIndices.size() > 100) {
            fundsGiniIndices.removeNondestructively(0);
        }

        fundsGini = 0;
        for (int i = 0; i < fundsGiniIndices.size(); i++) {
            fundsGini += fundsGiniIndices.get(i);
        }
        fundsGini /= fundsGiniIndices.size();

        fundsStandardDev = calculateStandardDev(fundsArray, fundsMean); // use custom function to return standard deviation.

        // postdoc metrics //

        postdocNumberDistribution = postdocNumberArray; // all total number of postdocs in array form
        postdocDurationDistribution = postdocDurationArray; // all number of cycles that lab will have at least one postdoc in array form
        double[] postdocNumberResults = meanAndGini(postdocNumberArray); // use custom function to return mean and gini of number of postdocs. [mean, gini]
        double postdocNumberMeanThisTurn = postdocNumberResults[0];
        double postdocNumberGiniThisTurn = postdocNumberResults[1];
        postdocNumberMeans.add(postdocNumberMeanThisTurn);
        postdocNumberMean = aggregateGlobal(postdocNumberMeans, 100);
        postdocNumberGinis.add(postdocNumberGiniThisTurn);
        postdocNumberGini = aggregateGlobal(postdocNumberGinis, 100);
        postdocNumberStandardDev = calculateStandardDev(postdocNumberArray, postdocNumberMean); // standard deviation of number of postdocs.

        double[] postdocDurationResults = meanAndGini(postdocDurationArray); // use custom function to return mean and gini index of distribution of cycles that lab will have at least 1 postdoc. [mean, gini]
        double postdocDurationMeanThisTurn = postdocDurationResults[0];
        double postdocDurationGiniThisTurn = postdocDurationResults[1];
        postdocDurationMeans.add(postdocDurationMeanThisTurn);
        postdocDurationMean = aggregateGlobal(postdocDurationMeans, 100);
        postdocDurationGinis.add(postdocDurationGiniThisTurn);
        postdocDurationGini = aggregateGlobal(postdocDurationGinis, 100);

        postdocDurationStandardDev = calculateStandardDev(postdocDurationArray, postdocDurationMean); // standard deviation of cycles that lab will have at least 1 postdoc.


        // debugging //

        double effortCounter = 0;
        for (int i = 0; i < state.getAllLabs().size(); i++) {
            Lab aLab = (Lab) state.getAllLabs().get(i);
            effortCounter += aLab.effort;
        }
        this.averageEffort = effortCounter / state.getAllLabs().size();
        // write to file //


        try {
            Outputter io = new Outputter(state); // outputter writes to a file all globals as part of its construction. thus, it's assigned and then immediately cleaned from memory after it writes everything.
            io = null;
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private double calculateMean(double[] array) {
        // calculates the mean of an array of doubles

        double mean = 0;
        for (double aDouble : array) {
            mean += aDouble;
        }
        return mean / array.length;
    }

    private double calculateMean(int[] array) {
        // calculates the mean of an array of integers. difference with above is that it has to cast them as double before division.
        double mean = 0;
        for (int anInt : array) {
            mean += (double) anInt;
        }
        return mean / array.length;
    }

    private double[] meanAndGini(double[] array) {
        // calculates both the mean of an array and the gini coefficient of the distribution of its values.
        // [0] is mean, [1] is gini coefficient.

        double giniNumerator = 0; // the numerator of the calculation of gini coefficient.
        double giniDenominator = 0; // the denominator of the calculation of gini coefficient.
        double mean = 0;
        Arrays.sort(array); // sort the array for gini coefficient calculation.
        for (int i = 0; i < array.length; i++) {
            mean += array[i]; // sum each value to the mean calculator
            giniNumerator += array[i] * (i + 1); // add to the numerator each value times its place in the sorted array + 1.
            giniDenominator += array[i]; // add the value to the denominator.
        }
        mean /= array.length; // calculate mean
        giniNumerator *= 2; // multiply gini numerator by 2
        giniDenominator *= array.length; // multiply gini denominator by the number of values

        double giniLeftHand = giniNumerator / giniDenominator; // left hand of the final subtraction in gini index.
        Double giniIndex = giniLeftHand - ((1 + array.length) / array.length); // left hand minus right hand of the gini index calculation. cast as object to use isNaN.

        if(giniIndex.isNaN()){ // avoids spitting out a NaN at first time step
            giniIndex = (double) 0;
        }
        return new double[]{mean, giniIndex}; // return array with both mean and gini coef.
    }

    private double calculateStandardDev(double[] array, double mean) {
        // calculate standard deviation of an array of doubles. uses Math.sqrt().

        double sumOfSquaredDevs = 0;
        for (double aDouble : array) {
            double squaredDev = aDouble - mean;
            squaredDev *= squaredDev;
            sumOfSquaredDevs += squaredDev;
        }
        sumOfSquaredDevs /= array.length;
        return Math.sqrt(sumOfSquaredDevs);
    }

    private double calculateStandardDev(int[] array, double mean) {
        // calculate standard deviation of an array of ints. uses Math.sqrt() and casts ints as doubles before division.

        double sumOfSquaredDevs = 0;
        for (int anInt : array) {
            double squaredDev = ((double) anInt) - mean;
            squaredDev *= squaredDev;
            sumOfSquaredDevs += squaredDev;
        }
        sumOfSquaredDevs /= array.length;
        return Math.sqrt(sumOfSquaredDevs);
    }

    public void resetGlobals(){ // set everything to 0 to calculate only globals at step time.
        this.falseDiscoveries = 0; // total number of false discoveries
        this.numberOfPublications = 0; // total number of publications
        this.falseDiscoveryRate = 0; // the rate of publications that are false discoveries.
        this.rateOfDiscovery = 0; // the proportion of topics that have been discovered (number of publications > 1).
        this.discoveredMean = 0; // the mean baserate of each topic in the landscape.
        this.discoveredStandardDev = 0;  // the standard deviation of the rates of the landscape.
        this.discoveredDistribution = new double[0]; // all base rates of the landscape in array form.
        this.publicationMean = 0; // the mean number of publications of each topic.
        this.publicationStandardDev = 0; // the standard deviation of the publications of each topic.
        this.publicationDistribution = new int[0]; // all values of publications in each topic in array form.
        this.fundsMean = 0; // the mean total amount of funds (years of funding) of the labs.
        this.fundsStandardDev = 0; // the standard deviation of the total amnount of funds of the labs.
        this.fundsDistribution = new double[0]; // all values of total amount of funds in array form.
        this.fundsGini = 0; // the gini index of the distribution of total amount of funds in the lab population.
        this.postdocNumberMean = 0; // mean number of postdocs of every lab (total amount of grants)
        this.postdocNumberStandardDev = 0; // standard deviation of the number of postdocs.
        this.postdocNumberGini = 0; // gini index of the distribution of number of postdocs.
        this.postdocNumberDistribution = new double[0]; // all values of number of postdocs, in array form.
        this.postdocDurationMean = 0; // the mean amount of cycles that the lab will have at least one postdoc ( 0 to 5).
        this.postdocDurationDistribution = new double[0]; // the distribution of amount of cycles that labs will have at least one postdoc.
        this.postdocDurationStandardDev = 0; // the standard deviation of the amount of cycles that labs will have at least one postdoc.
        this.postdocDurationGini = 0; // the gini index of the distribution of the amount of cycles that labs will have at least one postdoc.
    }

    private double aggregateGlobal(DoubleBag bag, double aggregation){
        while(bag.size() > aggregation){ // prune it down to the max number requested
            bag.removeNondestructively(0);
        }
        double result = 0;
        for(int i = 0 ; i < bag.size(); i++){
            result += bag.get(i);
        }
        result /= aggregation;
        return result;
    }

    // getters and setters //


    public void addFalseDiscoveries() {
        this.falseDiscoveries++;
    }

    public void addPublications() {
        this.numberOfPublications++;
    }

    public double getFalseDiscoveryRate() {
        return falseDiscoveryRate;
    }

    public double getRateOfDiscovery() {
        return rateOfDiscovery;
    }

    public double getDiscoveredMean() {
        return discoveredMean;
    }

    public double getDiscoveredStandardDev() {
        return discoveredStandardDev;
    }

    public double[] getDiscoveredDistribution() {
        return discoveredDistribution;
    }

    public double getPublicationMean() {
        return publicationMean;
    }

    public double getPublicationStandardDev() {
        return publicationStandardDev;
    }

    public int[] getPublicationDistribution() {
        return publicationDistribution;
    }

    public double getFundsMean() {
        return fundsMean;
    }

    public double getFundsStandardDev() {
        return fundsStandardDev;
    }

    public double[] getFundsDistribution() {
        return fundsDistribution;
    }

    public double getFundsGini() {
        return fundsGini;
    }

    public double getPostdocNumberMean() {
        return postdocNumberMean;
    }

    public double getPostdocNumberStandardDev() {
        return postdocNumberStandardDev;
    }

    public double getPostdocNumberGini() {
        return postdocNumberGini;
    }

    public double[] getPostdocNumberDistribution() {
        return postdocNumberDistribution;
    }

    public double getPostdocDurationMean() {
        return postdocDurationMean;
    }

    public double[] getPostdocDurationDistribution() {
        return postdocDurationDistribution;
    }

    public double getPostdocDurationStandardDev() {
        return postdocDurationStandardDev;
    }

    public double getPostdocDurationGini() {
        return postdocDurationGini;
    }

    // debugging //


    public double getAverageEffort() {
        return averageEffort;
    }
}