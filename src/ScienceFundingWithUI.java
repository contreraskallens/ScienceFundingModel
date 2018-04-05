import sim.display.*;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.grid.ValueGridPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.*;
import sim.display.ChartUtilities;
import javax.swing.*;
import java.awt.*;

public class ScienceFundingWithUI extends GUIState {

    // set up the display of the landscape and moving labs //
    public Display2D display;
    public JFrame displayFrame;
    final ValueGridPortrayal2D landscapePortrayal = new ValueGridPortrayal2D();
    final SparseGridPortrayal2D labsPortrayal = new SparseGridPortrayal2D();

    // set up the default to be displayed charts //

    // time series chart that includes False Discovery Rate, Gini Index of Funds, Gini Index of Number of Postdocs and Gini Index of the Duration of Postdocs (cycles in which lab will have at least 1 postdoc).

    public TimeSeriesChartGenerator timeSeriesChart;
    public TimeSeriesAttributes falseDiscoveryRateAttribs;
    public TimeSeriesAttributes fundsGiniAttribs;
    public TimeSeriesAttributes postdocNumberGiniAttribs;
    public TimeSeriesAttributes postdocDurationGiniAttribs;

    // histogram chart with the distribution of total funds.

    public HistogramGenerator fundsHistogram;
    public HistogramSeriesAttributes fundsDistributionAttribs;

    // histogram chart with the distribution of total number of postdocs.

    public HistogramGenerator postdocNumberHistogram;
    public HistogramSeriesAttributes postdocNumberDistributionAttribs;

    // time series chart tracking the mean and standard deviation of the base rates of cells of the landscape.

    public TimeSeriesChartGenerator landscapeChart;
    public TimeSeriesAttributes discoveredMeanAttribs;
    public TimeSeriesAttributes discoveredStandardDevAttribs;

    // time series chart tracking the mean and standard deviation of the number of publications of each cell in the landscape.

    public TimeSeriesChartGenerator publicationChart;
    public TimeSeriesAttributes publicationMeanAttribs;
    public TimeSeriesAttributes publicationStandardDevAttribs;

    // main and constructors //

    public static void main(String[] args) {
        ScienceFundingWithUI vid = new ScienceFundingWithUI();
        Console c = new Console(vid);
        c.setVisible(true);
    }

    public ScienceFundingWithUI() {
        super(new ScienceFunding(System.currentTimeMillis()));
    }

    public ScienceFundingWithUI(SimState state){
        super(state);
    }

    public Object getSimulationInspectedObject() {
        return state;
    }

    // set name to display at the UI console

    public static String getName(){
        return "Science Funding";
    }

    // set up an inspector that tracks the models globals for monitoring in the UI console

    public Inspector getInspector(){ // inspector of model globals
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
    }


    // start method that includes all of the custom default charts

    public void start(){
        super.start();

        // clear all time series charts
        timeSeriesChart.clearAllSeries();
        landscapeChart.clearAllSeries();
        publicationChart.clearAllSeries();

        // allocates the data for the charts in ChartUtilities.Attributes objects for each data source being tracked.
        ChartUtilities.scheduleSeries(this, falseDiscoveryRateAttribs, () -> ((ScienceFunding) state).getFalseDiscoveryRate());
        ChartUtilities.scheduleSeries(this, fundsGiniAttribs, () -> ((ScienceFunding) state).getFundsGini());
        ChartUtilities.scheduleSeries(this, postdocNumberGiniAttribs, () -> ((ScienceFunding) state).getPostdocNumberGini());
        ChartUtilities.scheduleSeries(this, postdocDurationGiniAttribs, () -> ((ScienceFunding) state).getPostdocDurationGini());
        ChartUtilities.scheduleSeries(this, fundsDistributionAttribs, () -> ((ScienceFunding) state).getFundsDistribution());
        ChartUtilities.scheduleSeries(this, postdocNumberDistributionAttribs, () -> ((ScienceFunding) state).getPostdocNumberDistribution());
        ChartUtilities.scheduleSeries(this, discoveredMeanAttribs, () -> ((ScienceFunding) state).getDiscoveredMean());
        ChartUtilities.scheduleSeries(this, discoveredStandardDevAttribs, () -> ((ScienceFunding) state).getDiscoveredStandardDev());
        ChartUtilities.scheduleSeries(this, publicationMeanAttribs, () -> ((ScienceFunding) state).getPublicationMean());
        ChartUtilities.scheduleSeries(this, publicationStandardDevAttribs, () -> ((ScienceFunding) state).getPublicationStandardDev());

        setupPortrayals((ScienceFunding) getSimulationInspectedObject());
    }

    public void load(SimState state){
        super.load(state);

        // allocates the data for the charts in ChartUtilities.Attributes objects for each data source being tracked. uses the getters in ScienceFunding.
        ChartUtilities.scheduleSeries(this, falseDiscoveryRateAttribs, ((ScienceFunding) state)::getDiscoveredMean);
        ChartUtilities.scheduleSeries(this, fundsGiniAttribs, ((ScienceFunding) state)::getFundsGini);
        ChartUtilities.scheduleSeries(this, postdocNumberGiniAttribs, ((ScienceFunding) state)::getPostdocNumberGini);
        ChartUtilities.scheduleSeries(this, postdocDurationGiniAttribs, ((ScienceFunding) state)::getPostdocDurationGini);
        ChartUtilities.scheduleSeries(this, fundsDistributionAttribs, ((ScienceFunding) state)::getFundsDistribution);
        ChartUtilities.scheduleSeries(this, postdocNumberDistributionAttribs, ((ScienceFunding) state)::getPostdocNumberDistribution);
        ChartUtilities.scheduleSeries(this, discoveredMeanAttribs, ((ScienceFunding) state)::getDiscoveredMean);
        ChartUtilities.scheduleSeries(this, discoveredStandardDevAttribs, ((ScienceFunding) state)::getDiscoveredStandardDev);
        ChartUtilities.scheduleSeries(this, publicationMeanAttribs, ((ScienceFunding) state)::getPublicationMean);
        ChartUtilities.scheduleSeries(this, publicationStandardDevAttribs, ((ScienceFunding) state)::getPublicationStandardDev);

        setupPortrayals((ScienceFunding) state);
    }

    public void setupPortrayals(ScienceFunding state){

        // sets up the portrayal of the labs in the landscape
        ScienceFunding scienceFunding = (ScienceFunding) state;

        landscapePortrayal.setField(state.getLandscape());
        labsPortrayal.setField(state.getLabs());

        // portrays labs as ovals with colors according the funding they have
        labsPortrayal.setPortrayalForAll(new OvalPortrayal2D(){
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
                Lab lab = (Lab) object;

                int fundingShade = lab.getNumberOfPostdocs() * 10 + 50;
                if (fundingShade > 255) {
                    fundingShade = 255;
                }
                if(fundingShade == 0){
                    fundingShade = 1;
                }
                paint = new Color(fundingShade, 0, 255 - fundingShade);
                super.draw(object, graphics, info);
            }
        });

        // allocate colors for ovals
        SimpleColorMap cm = new SimpleColorMap();
        cm.setLevels(0.001, 0.5, new Color(0,0,0,0), new Color(255,0,0,150));

        //allocates and paints the portrayal of the landscape.
        landscapePortrayal.setMap(cm);
        display.reset();
        display.setBackdrop(Color.white);
        display.repaint();
    }

    public void init(Controller c) {
        super.init(c);
        int sizeOfLandscape = ((ScienceFunding) state).getSizeOfLandscape();
        // initializes the landscape determined by parameters
        display = new Display2D(sizeOfLandscape, sizeOfLandscape, this);
        display.setClipping(false);

        // display the console with name Science Funding and attach both landscape and lab portrayals.
        displayFrame = display.createFrame();
        displayFrame.setTitle("Science Funding");
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);
        display.attach(landscapePortrayal, "Landscape");
        display.attach(labsPortrayal, "labs");

        // initialize the custom charts and the properties they track.

        // time series with Gini Indices and False Discovery Rate
        timeSeriesChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Time series", "Steps");
        fundsGiniAttribs = ChartUtilities.addSeries(timeSeriesChart, "Funds Gini");
        falseDiscoveryRateAttribs = ChartUtilities.addSeries(timeSeriesChart, "False Discovery Rate");
        postdocNumberGiniAttribs = ChartUtilities.addSeries(timeSeriesChart, "Number of Postdocs Gini");
        postdocDurationGiniAttribs = ChartUtilities.addSeries(timeSeriesChart, "Duration of Postdocs Gini");

        // histogram of funds
        fundsHistogram = ChartUtilities.buildHistogramGenerator(this, "Distribution of Funds", "Number of Labs");
        fundsDistributionAttribs = ChartUtilities.addSeries(fundsHistogram, "Total funds (sum)", 5);

        // histogram of total number of postdocs
        postdocNumberHistogram = ChartUtilities.buildHistogramGenerator(this, "Distribution of Number of Postdocs", "number of labs");
        postdocNumberDistributionAttribs = ChartUtilities.addSeries(postdocNumberHistogram, "Total number of postdocs", 5);

        //time series for landscape discovery
        landscapeChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Landscape state", "Steps");
        discoveredMeanAttribs = ChartUtilities.addSeries(landscapeChart, "Mean Base Rate of Landscape");
        discoveredStandardDevAttribs = ChartUtilities.addSeries(landscapeChart, "Standard Dev of Base Rates");

        // time series for publications
        publicationChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Publication record", "Steps");
        publicationMeanAttribs = ChartUtilities.addSeries(publicationChart, "Mean number of publications per topic");
        publicationStandardDevAttribs = ChartUtilities.addSeries(publicationChart, "Standard Dev of Publications per Topic");
    }

    // quit UI.

    public void quit() {
        super.quit();
        if (displayFrame != null) {
            displayFrame.dispose();
        }
        displayFrame = null;
        display = null;
    }
}