import sim.field.grid.DoubleGrid2D;
import sim.util.*;
import static java.lang.Math.pow;

class LandscapeUtils {
    // implements static methods to manipulate the landscape //

    static void increaseAndDisperse(DoubleGrid2D landscape, int originalCellX, int originalCellY, double amount) {
        // includes dispersal. takes cell, adds amount, and then adds dispersing amount to neighboring cells proportional to their distance to the original cell.
        // the dispersion stops when the amount added to the cells is less than 0.00000001.
        // changed cells are add to a bag (previousChanges) so that they are not picked up as their neighbor's neighbors when the recursive dispersal happens.

        Double originalValue = landscape.get(originalCellX, originalCellY);
        Double2D originalCell = new Double2D(originalCellX, originalCellY);
        landscape.set(originalCellX, originalCellY, (originalValue + amount)); // add the amount specified in the call to the specified cell.
        Bag previousChanges = new Bag();
        previousChanges.add(originalCell);

        changeNeighbors(landscape, originalCell, originalCell, amount, previousChanges); // call the recursive function to disperse the original amount added to the original cell to all of its neighbors.
    }

    static void changeNeighbors(DoubleGrid2D landscape, Double2D originalCell, Double2D thisCell, double originalAmount, Bag previousChanges) {
        // recursive function. takes the landscape, the cell originally changed and the cell to change along with a Bag to avoid stack overflow and the original amount changed.
        // it loops through the neighbors of a cell and makes them change their value according to the distance to the original cell.
        // each of those neighbors calls the same function on their neighbors, until the value added to the cells is equal or less than 0.00000001 (min value).
        // after the function is called on a cell, it is added to the bag "previousChanges".
        // the function is only called on cells not previously changed. this avoids infinite recursion.

        IntBag neighborsX = new IntBag(); // allocate the bags needed by getMooreNeighbors().
        IntBag neighborsY = new IntBag();
        DoubleBag neighborsValues = new DoubleBag();
        landscape.getMooreNeighbors((int) thisCell.x, (int) thisCell.y, 1, 0, false, neighborsValues, neighborsX, neighborsY); // get the moore neighbors of the cell previously changed.

        for (int i = 0; i < neighborsX.size(); i++) { // loop through the neighbors of the original cell.
            Double2D thisNeighbor = new Double2D(neighborsX.get(i), neighborsY.get(i));
            double thisNeighborValue = landscape.get((int) thisNeighbor.x, (int) thisNeighbor.y); // allocate original value of the neighbor.
            if ((thisNeighbor.x == originalCell.x) && (thisNeighbor.y == originalCell.y)) { // if this neighbor is the original cell (the one in which increaseAndDisperse() was called), go to next iteration of the loop.
                continue;
            }
            if (previousChanges.contains(thisNeighbor)) { // if the cell was changed before, go to the next iteration of the loop.
                continue;
            }

            Double newValue = getValueWithDispersal(landscape, originalCell, thisNeighbor, originalAmount); // get the new value for the cell using custom function getValueWithDispersal().

            // if the change is below the precision level (0.00000001), this function returns the same value that was provided to it.

            if (newValue != thisNeighborValue) { // if the value returned is different than the value provided, change the value in the landscape and add the cell to the bag of previously changed cells.
                landscape.set(neighborsX.get(i), neighborsY.get(i), newValue);
                previousChanges.add(thisNeighbor);
                changeNeighbors(landscape, originalCell, thisNeighbor, originalAmount, previousChanges); // recursively call this same function on all of this cell's neighbors.
            } else { // if it didn't change because the amount added was below the precision level, the cell retains its value.
                landscape.set(neighborsX.get(i), neighborsY.get(i), thisNeighborValue);
            }
        }
    }

    static double getValueWithDispersal(DoubleGrid2D landscape, Double2D originalCell, Double2D thisCell, double originalAmount) {
        // returns amount to be added after dispersion dependent on euclidean distance.

        double oldValue = landscape.get((int) thisCell.x, (int) thisCell.y);
        double eucDistance = originalCell.distance(thisCell); // euclidean distance of this cell to the original cell (the one in which increaseAndDisperse() was called)
        double newValue = pow(originalAmount, eucDistance); // the amount added to the original cell to the power of the euclidean distance between this cell and the original cell.

        if (newValue >= 0.00000001) { // only add the amount to the value if the amount to be added is higher than 0.00000001.
            newValue += oldValue;
        } else { // if it's not, return the old value unchanged.
            newValue = oldValue;
        }
        if (newValue >= 0.5) { // if the value resulting from the addition is higher than the cap of 0.5, return 0.5. else, return the value.
            return 0.5;
        } else {
            return newValue;
        }
    }
}
