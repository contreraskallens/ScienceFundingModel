import java.io.*;


public class Outputter {

    /**
     * Field to save the file name to be recorded. useful for batch runs.
     */
    private String fileName;

    /**
     *
     * @param state hey
     * @throws IOException hey
     */
    public Outputter(ScienceFunding state) throws IOException {

        // every time Outputter is constructed, it outputs the data contained in Globals to the file recorded in fileName.
        // this method avoids having to have Outputter as an additional agent in the schedule. Outputter is only constructed by Globals.
        // at time step 0, when the outputter is called once by the simulation, it writes down the columns by using prepareFile();
        // every time it's constructed after that, it writes all of the global statistics to the file in the order provided.
        // the output file is a .csv.

        fileName = "resources" + System.getProperty("file.separator") + "run" + state.job() + ".csv"; // file name is directory/resources/runX.csv. X is determined by ScienceFunding.runNumber.


        BufferedWriter pw = null; // allocate the file writer. open file in append mode.
        try {
            pw = new BufferedWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(state.schedule.getSteps() == 0){
            prepareFile();
        }
        writeGlobals(pw, state);
        closeFile(pw);

    }

    private void prepareFile() throws IOException {
        // at time step 0, when Outputter is first allocated, prepare file by writing the column names for subsequent import
         BufferedWriter pw = new BufferedWriter(new FileWriter(this.fileName));
         pw.write("stepNumber," + "falseDiscoveryRate," + "rateOfDiscovery," +
                "discoveredMean," + "discoveredStandardDev," + "publicationMean," + "publicationStandardDev," +
                "fundsMean," + "fundsStandardDev," + "fundsGini," + "postdocNumberMean," + "postdocNumberStandardDev," +
                "postdocNumberGini," + "postdocDurationMean," + "postdocDurationStandardDev," + "postdocDurationGini");
         pw.newLine();
         pw.flush();
    }

    private void writeGlobals(BufferedWriter pw, ScienceFunding state) throws IOException {
        // writes all of the data saved to Globals, and then writes a new line.
        Globals theGlobals = state.getGlobalsObject(); // point to globals to get the measures

        pw.write(state.schedule.getSteps() + "," + theGlobals.getFalseDiscoveryRate() + "," + theGlobals.getRateOfDiscovery() + "," +
                theGlobals.getDiscoveredMean() + "," + theGlobals.getDiscoveredStandardDev() +
                "," + theGlobals.getPublicationMean() + "," + theGlobals.getPublicationStandardDev() + "," + theGlobals.getFundsMean() + "," +
                theGlobals.getFundsStandardDev() + "," + theGlobals.getFundsGini() + "," + theGlobals.getPostdocNumberMean() +  "," +
                theGlobals.getPostdocNumberStandardDev() + "," + theGlobals.getPostdocNumberGini() +  "," + theGlobals.getPostdocDurationMean() + "," + theGlobals.getPostdocDurationStandardDev() +
                "," + theGlobals.getPostdocDurationGini());
        pw.newLine();
        pw.flush();
    }

    private void closeFile(BufferedWriter pw) throws IOException {
        // flushes the file writer, and then closes the file. this happens after writing the data to it.

        pw.flush();
        pw.close();
    }

}