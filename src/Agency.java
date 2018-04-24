import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import java.util.Comparator;

/**
 * This class builds an agent that assigns funding to the scheduled labs each turn.
 * The parameters for the assignment of this funding can be found in ScienceFunding.
 * This class also has parameters:
 * Budget determines the number of grants to be distributed measured in UNITS OF BIG GRANTS. A big grant provides 5
 * years of funding, and a small grant provides 1.
 * ProportionOfBigGrants determine how many of the units of budget are going to be assigned as big grants, and
 * how many are going to be assigned as small grants (1 - proportionOfBigGrants).
 * IncludeNoise is a boolean parameter that determines if there will be noise in the evaluation of the labs.
 * EvaluationNoise determines how much noise will be added to the evaluation. The parameter controls the
 * standard deviation of the gaussian distribution from which the noise is drawn.
 */
class Agency implements Steppable {

    //region Parameters
    private final int budget = 10;
    private final double proportionOfBigGrants = 0.2;
    private final boolean includeNoise = false;
    private final double evaluationNoise = 0.001;
    //endregion

    /*
    The labs that apply for funding during their research phase add themselves to this bag.
     */
    private Bag applicantsForThisTurn;

    public Agency(){
        applicantsForThisTurn = new Bag();
    }

    /**
     * Each time cycle, the agency evaluates all of the labs that applied for funding based on their ScoreForApplying.
     * The agency sorts the applicants according to their score and starts choosing the top ones in the ranking.
     * Each time it considers an applicant, it determines if it will assign 1 big grant or 4 small grants.
     * If the result is 1 big grant, the topmost lab receives 5 years of funding for 1 postdoc.
     * If the result is 4 small grants, the 4 topmost labs receive 1 year of funding for 1 postdoc.
     * Whenever a lab receives funding, it is removed from the ranking.
     * If lottery is set to true on ScienceFunding, instead of sorting according to score, the applicants ar randomly sorted.
     * Budget doesn't go over to the next time cycle. If grants are left after all applicants receive funding, they are lost.
     *
     * @param state The simulation state.
     */
    @Override
    public void step(SimState state) {
        ScienceFunding simulationState = (ScienceFunding) state;
        if (includeNoise) {
            for (int i = 0; i < applicantsForThisTurn.size(); i++) {
                Lab thisLab = (Lab) applicantsForThisTurn.get(i);
                thisLab.setScoreForApplying(thisLab.getScoreForApplying() + (state.random.nextGaussian() * evaluationNoise));
            }
        }
        if (!simulationState.getLotteryOfFunding()) {
            applicantsForThisTurn.sort(Comparator.comparing(Lab::getScoreForApplying));
        } else {
            applicantsForThisTurn.shuffle(state.random);
        }

        for (int i = 0; i < this.budget; i++) {
            if (applicantsForThisTurn.size() == 0) { // Fail safe for if there are fewer applicants than there are grants
                break;
            }
            /*
            Loop through the applicants and roll for if it's 1 big grant or 4 small grants.
             */
            if (state.random.nextDouble() < this.proportionOfBigGrants) {
                Lab topLabInRanking = (Lab) applicantsForThisTurn.pop(); // Note that pop() removes and return top agent on Bag.
                topLabInRanking.grants.add(5);
            } else {
                /*
                If it's a small grant, loop again assigning 1 year of funding to the 4 top labs.
                 */
                for (int n = 0; n < 4; n++) {
                    if (applicantsForThisTurn.size() == 0) { // Fail safe for if there are fewer applicants than there are grants
                        break;
                    }
                    Lab bestLab = (Lab) applicantsForThisTurn.pop();
                    bestLab.grants.add(1);
                }
            }
        }
        applicantsForThisTurn = new Bag();
    }

    /**
     * AddToApplicants is used by labs when they want to apply for funding. This adds a lab (putatively, the lab
     * using the method) to the Bag that contains this turn's applicants for funding.
     * @param lab
     */
    public void addToApplicants(Lab lab) {
        applicantsForThisTurn.add(lab);
    }
}