import sim.field.grid.DoubleGrid2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.DoubleBag;
import sim.util.IntBag;

import static java.lang.Math.pow;

/**
 * This class contains static methods to modify the epistemic landscape according to document describing model.
 */
class LandscapeUtils {

    /**
     * This method increases the base rate of a topic ("original topic") by a set amount.
     * After increasing this base rate, the change is dispersed to its neighbors and their
     * neighbors via recursive changeNeighbors().
     * Topics that have been changed already are added to a bag to avoid infinite recursion.
     *
     * @param epistemicLandscape The Double grid that contains the base rates of all topics in the landscape.
     * @param originalTopicX     The x dimension of the topic to be changed and whose change are to be dispersed.
     * @param originalTopicY     The y dimension of the topic to be changed and whose change are to be dispersed.
     * @param changeInBaseRate   The amount to be added to the topic in the center of the dispersal (the original topic).
     */
    static void increaseAndDisperse(DoubleGrid2D epistemicLandscape, int originalTopicX, int originalTopicY, double changeInBaseRate) {
        Double originalBaseRate = epistemicLandscape.get(originalTopicX, originalTopicY);
        Double2D originalTopic = new Double2D(originalTopicX, originalTopicY);
        epistemicLandscape.set(originalTopicX, originalTopicY, (originalBaseRate + changeInBaseRate));
        Bag topicsThatHaveBeenChanged = new Bag();
        topicsThatHaveBeenChanged.add(originalTopic);
        changeNeighbors(epistemicLandscape, originalTopic, originalTopic, changeInBaseRate, topicsThatHaveBeenChanged);
    }

    /**
     * Recursive function that loops through the neighbors of a topic and changes their base rate
     * based on their distance to the original topic being changed. The dispersed change in base rate is
     * calculated by getDispersedBaseRate. When the change in base rate for a topic's neighbors
     * is less than 0.00000001, the recursive process stops.
     *
     * @param epistemicLandscape       The epistemic landscape as a grid of doubles expressing the current base rates of all topics.
     * @param originalTopic            The topic that was originally being changed (the first one in which increaseAndDisperse was called.
     * @param thisTopic                The topic being changed by the current function (recursive).
     * @param baseRateChangeInOriginal The amount added to the original topic whose value with dispersal has to be added to the new one.
     * @param previouslyChangedTopics  The bag of the topics that have been changed already.
     */
    static void changeNeighbors(DoubleGrid2D epistemicLandscape, Double2D originalTopic, Double2D thisTopic, double baseRateChangeInOriginal, Bag previouslyChangedTopics) {
        IntBag neighborsX = new IntBag();
        IntBag neighborsY = new IntBag();
        DoubleBag neighborsBaseRate = new DoubleBag();
        epistemicLandscape.getMooreNeighbors((int) thisTopic.x, (int) thisTopic.y, 1, 0, false, neighborsBaseRate, neighborsX, neighborsY); // get the moore neighbors of the cell previously changed.

        for (int i = 0; i < neighborsX.size(); i++) {
            Double2D thisNeighbor = new Double2D(neighborsX.get(i), neighborsY.get(i));
            double thisNeighborBaseRate = epistemicLandscape.get((int) thisNeighbor.x, (int) thisNeighbor.y);
            if ((thisNeighbor.x == originalTopic.x) && (thisNeighbor.y == originalTopic.y)) {
                continue;
            }
            if (previouslyChangedTopics.contains(thisNeighbor)) {
                continue;
            }
            Double newBaseRate = getDispersedBaseRate(epistemicLandscape, originalTopic, thisNeighbor, baseRateChangeInOriginal);

            if (newBaseRate != thisNeighborBaseRate) {
                epistemicLandscape.set(neighborsX.get(i), neighborsY.get(i), newBaseRate);
                previouslyChangedTopics.add(thisNeighbor);
                changeNeighbors(epistemicLandscape, originalTopic, thisNeighbor, baseRateChangeInOriginal, previouslyChangedTopics);
            } else {
                epistemicLandscape.set(neighborsX.get(i), neighborsY.get(i), thisNeighborBaseRate);
            }
        }
    }

    /**
     * This method returns the new value of the topic being modified, dependent on eucledian distance between the
     * original topic and the topic whose base rate is being changed.
     *
     * @param epistemicLandscape       The grid of doubles with the base rates of every topic in the epistemic landscape.
     * @param originalTopic            The original topic in which increaseAndDisperse() was called.
     * @param thisTopic                The topic whose change in base rate is being calculated.
     * @param baseRateChangeInOriginal The amount that was added to the original topic in increaseAndDisperse()
     * @return Returns the new base rate of the topic being modified. If the change in base rate will be lower than
     * 0.00000001, the returned base rate will be the same as the value without modifications.
     */
    static double getDispersedBaseRate(DoubleGrid2D epistemicLandscape, Double2D originalTopic, Double2D thisTopic, double baseRateChangeInOriginal) {
        double oldBaseRate = epistemicLandscape.get((int) thisTopic.x, (int) thisTopic.y);
        double eucledianDistance = originalTopic.distance(thisTopic);
        double newBaseRate = pow(baseRateChangeInOriginal, eucledianDistance);
        if (newBaseRate >= 0.00000001) {
            newBaseRate += oldBaseRate;
        } else {
            newBaseRate = oldBaseRate;
        }
        if (newBaseRate >= 0.5) {
            return 0.5;
        } else {
            return newBaseRate;
        }
    }
}