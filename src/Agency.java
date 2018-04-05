import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import java.util.Comparator;

class Agency implements Steppable {

    // fields //

    private Bag thisTurnsApplicants; // renewable bag to store everyone applying for funds. labs insert themselves into the bag during their steps.

    // parameters //

    private final int budget = 10; // budget available measured in big grants. 1 big grant (5 cycles) = 4 small grants (1 cycle)
    private final double proportionOfBigGrants = 0.2; // proportion of total grants that will be awarded as big grands.
    private final boolean includeError = false; // is there error in the evaluation of the utility?
    private final double evaluationError = 0.001; // standard deviation of error in utility if utility evaluation includes error.

    // constructor //
    public Agency(){

        // initialize the bag of applicants along with the funding agency

        thisTurnsApplicants = new Bag();
    }

    public void step(SimState state){

        // each cycle, the agency evaluates each one of the labs that applied for funding based on their utility and awards them grants.
        // the number of grants depend on the parameter Budget. grants can be either big, and imply 5 years of funding for 1 lab,
        // or small, and imply 1 year of funding for 4 different labs. the parameter includeError determines if there is an added
        // error, drawn from a gaussian distribution with standard deviation evaluationError and mean of 0. if it includes error,
        // the agency loops through the applying labs adding the error to their utility field.

        if (includeError) {
            for (int i = 0; i < thisTurnsApplicants.size(); i++) { // sum of deviations from the mean
                Lab aLab = (Lab) thisTurnsApplicants.get(i);
                aLab.setUtility(aLab.getUtility() + (state.random.nextGaussian() * evaluationError)); // add a gaussian with a standard deviation of evaluationError.
            }
        }

        ScienceFunding simulation = (ScienceFunding) state;

        // to distribute the funds, the agency orders the bag of applicants based on their utility. if error is included,
        // this happens after adding it. the agency's budget is measured in big grants. thus, for each big grant that it can award,
        // the agency chooses the lab with the most utility, and awards it a big grant with a probability of proportionOfBigGrants.
        // else, the agency awards 4 small grants to 4 different labs. in that case, the agency chooses the 4 labs with the most utility and
        // awards them 1 year of funding. each time a lab gets awarded a grant, it's removed from the bag of applicants. if the bag
        // is empty before the budget runs out, the process stops. the budget doesn't go over to the next cycle.
        // if both weightOfInnovation + weightOfRecord are equal to 0, then the funding scheme is a lottery, randomizing the bag of
        // applicants and fetching the appropriate number.1

        //ScienceFunding simulation = (ScienceFunding) state; // cast state as ScienceFunding to acces parameters

        if(simulation.getLottery() == false) {
            thisTurnsApplicants.sort(Comparator.comparing(Lab::getUtility)); // order labs according to their utility
        } else {
            thisTurnsApplicants.shuffle(state.random);
        }

        for (int i = 0; i < this.budget; i++) {
            if (thisTurnsApplicants.size() == 0) { // fail safe for if there are fewer applicants than there are grants
                break;
            }
            if (state.random.nextDouble() < this.proportionOfBigGrants) { // win a big grant. Only one lab gets 5 years of funding.
                Lab bestLab = (Lab) thisTurnsApplicants.pop(); // get lab with highest utility
                bestLab.grants.add(5); // add a postdoc for 5 years
            } else { // 4 small grants are given. 4 labs get 1 year of funding.
                for (int n = 0; n < 4; n++) {
                    if (thisTurnsApplicants.size() == 0) { // fail safe for if there are fewer applicants than there are grants
                        break;
                    }
                    Lab bestLab = (Lab) thisTurnsApplicants.pop(); // get lab with highest utility
                    bestLab.grants.add(1); // add a postdoc for 1 year
                }
            }
        }
        // at the end of the process, the bag is cleared in preparation for the next cycle.

        thisTurnsApplicants = new Bag(); // clear the bag
    }

    // getters //

    public Bag getThisTurnsApplicants(){
        return thisTurnsApplicants;
    }

    public void addToApplicants(Lab lab){
        this.thisTurnsApplicants.add(lab);
    }
}