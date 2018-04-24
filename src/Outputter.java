import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class accesses the measures stored in the fields of the Globals agent and
 * writes them to a file in the order specified in the method. This happens everytime it is constructed.
 * NOT a steppable.
 */
public class Outputter {

    //region Fields
    private String fileName;
    //endregion

    /**
     * Every time Outputter is constructed, it outputs the data contained in Globals to the file recorded in fileName.
     * This way of saving them avoids having to have Outputter as an additional agent in the schedule.
     * Outputter is only constructed by Globals.
     * Before writing the globals to the file, the Outputter creates the files with the column headers to be written.
     * After this, it only writes values under these columns. Order of columns and of values being written have to match.
     * The file is stored in the project folder under /resources. It uses the job id of MASON to as an identifier.
     * Default filename is runID.csv (e.g. run04.csv).
     *
     * @param state The simulation state cast as ScienceFunding.
     * @throws IOException Exception needed by the package being used.
     */
    public Outputter(ScienceFunding state) throws IOException {
        fileName = "resources" + System.getProperty("file.separator") + "run" + state.job() + ".csv";
        BufferedWriter fileWriter = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(fileName, true));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (state.schedule.getSteps() == 0) {
            prepareFile();
        }
        writeGlobals(fileWriter, state);
        closeFile(fileWriter);
    }

    /**
     * Creates a file with filename stored in the field fileName writes column headers
     * specified below separated by commas.
     * This happens only when the time in simulation space is 0.
     * After writing the headers, writes a line break and flushes the buffer of the filewriter.
     *
     * @throws IOException Exception needed by the package being used.
     */
    private void prepareFile() throws IOException {
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(this.fileName));
        fileWriter.write("stepNumber," + "falseDiscoveryRate," + "rateOfDiscovery," +
                "discoveredMean," + "discoveredStandardDev," + "publicationMean," + "publicationStandardDev," +
                "fundsMean," + "fundsStandardDev," + "fundsGini," + "postdocNumberMean," + "postdocNumberStandardDev," +
                "postdocNumberGini");
        fileWriter.newLine();
        fileWriter.flush();
    }

    /**
     * Writes the global measures to a file created by prepareFile() with file name stored in field fileName.
     * The VALUES of the global measures are obtained from the Globals object scheduled in the simulation state.
     * The order of the values being written has to be matched to the order of the column headers in prepareFile().
     * Before the measures, the current time step is written.
     * After writing the measures in this order, separated by commas, it inserts a line break.
     *
     * @param fileWriter A bufferedWriter object.
     * @param state      The Simulation state, casted as ScienceFunding.
     * @throws IOException Exception needed by the package being used.
     */
    private void writeGlobals(BufferedWriter fileWriter, ScienceFunding state) throws IOException {
        Globals globalsObject = state.getGlobalsObject();

        fileWriter.write(state.schedule.getSteps() + "," + globalsObject.getFalseDiscoveryRateLastWindow() + "," + globalsObject.getProportionOfTopicsExplored() + "," +
                globalsObject.getMeanBaseRate() + "," + globalsObject.getBaseRateSDev() +
                "," + globalsObject.getMeanPublicationsPerTopic() + "," + globalsObject.getPublicationsPerTopicSDev() + "," + globalsObject.getMeanTotalFundsLastWindow() + "," +
                globalsObject.getTotalFundsSDev() + "," + globalsObject.getTotalFundsGiniLastWindow() + "," + globalsObject.getPostdocNumberMeanLastWindow() + "," +
                globalsObject.getPostdocNumberSDev() + "," + globalsObject.getPostdocNumberGiniLastWindow());
        fileWriter.newLine();
        fileWriter.flush();
    }

    /**
     * Flushes the file writer, and then closes the file.
     * This happens after writing the data to it.
     *
     * @param fileWriter A BufferedWriter to write the data to the file.
     * @throws IOException Exception needed by the package being used.
     */
    private void closeFile(BufferedWriter fileWriter) throws IOException {

        fileWriter.flush();
        fileWriter.close();
    }
}