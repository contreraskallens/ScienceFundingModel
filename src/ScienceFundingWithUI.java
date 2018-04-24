import sim.display.*;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.grid.ValueGridPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.HistogramGenerator;
import sim.util.media.chart.HistogramSeriesAttributes;
import sim.util.media.chart.TimeSeriesAttributes;
import sim.util.media.chart.TimeSeriesChartGenerator;

import javax.swing.*;
import java.awt.*;

public class ScienceFundingWithUI extends GUIState {
    //region Fields
    final ValueGridPortrayal2D landscapePortrayal = new ValueGridPortrayal2D();
    final SparseGridPortrayal2D labsPortrayal = new SparseGridPortrayal2D();
    public Display2D landscapeDisplay;
    public JFrame landscapeFrame;
    //endregion

    /**
     * The next lines control the default plots to be displayed when opening the visualization.
     */
    public TimeSeriesChartGenerator timeSeriesChart;
    public TimeSeriesAttributes falseDiscoveryRateAttributes;
    public TimeSeriesAttributes fundsGiniAttributes;
    public TimeSeriesAttributes postdocNumberGiniAttributes;

    public HistogramGenerator fundsHistogram;
    public HistogramSeriesAttributes fundsDistributionAttributes;
    public HistogramGenerator postdocNumberHistogram;
    public HistogramSeriesAttributes postdocNumberDistributionAttributes;

    public TimeSeriesChartGenerator landscapeChart;
    public TimeSeriesAttributes discoveredMeanAttributes;
    public TimeSeriesAttributes discoveredStandardDevAttributes;

    public TimeSeriesChartGenerator publicationChart;
    public TimeSeriesAttributes publicationMeanAttributes;
    public TimeSeriesAttributes publicationStandardDevAttributes;

    /**
     * Constructor that creates a new Simulation with current time as seed.
     */
    public ScienceFundingWithUI() {
        super(new ScienceFunding(System.currentTimeMillis()));
    }

    /**
     * Constructor that creates a visualization based on an existing simulation space.
     *
     * @param state An existing simulation space.
     */
    public ScienceFundingWithUI(SimState state) {
        super(state);
    }

    /**
     * Based on tutorial in manual.
     */
    public static void main(String[] args) {
        ScienceFundingWithUI visualization = new ScienceFundingWithUI();
        Console controlConsole = new Console(visualization);
        controlConsole.setVisible(true);
    }

    /**
     * Use this to change the name of the simulation in visualization
     *
     * @return The name of the visualization when displayed
     */
    public static String getName() {
        return "Science Funding";
    }

    /**
     * Gets the simulation space. Based on tutorial in manual.
     *
     * @return The simulation space.
     */
    public Object getSimulationInspectedObject() {
        return state;
    }

    /**
     * Set up an inspector that tracks the models globals for monitoring in the UI console
     *
     * @return The inspector object used in the visualization.
     */
    public Inspector getInspector() {
        Inspector inspector = super.getInspector();
        inspector.setVolatile(true);
        return inspector;
    }

    /**
     * Starts the visualization. This clears all previous chart and then allocates
     * the Attribs object that monitor the evolution of the measures. Finalizes by setting up the
     * portrayal of the current simulation state.
     */
    @Override
    public void start() {
        super.start();

        timeSeriesChart.clearAllSeries();
        landscapeChart.clearAllSeries();
        publicationChart.clearAllSeries();
        ChartUtilities.scheduleSeries(this, falseDiscoveryRateAttributes, () -> ((ScienceFunding) state).getFalseDiscoveryRate());
        ChartUtilities.scheduleSeries(this, fundsGiniAttributes, () -> ((ScienceFunding) state).getFundsGini());
        ChartUtilities.scheduleSeries(this, postdocNumberGiniAttributes, () -> ((ScienceFunding) state).getPostdocNumberGini());
        ChartUtilities.scheduleSeries(this, fundsDistributionAttributes, () -> ((ScienceFunding) state).getFundsDistribution());
        ChartUtilities.scheduleSeries(this, postdocNumberDistributionAttributes, () -> ((ScienceFunding) state).getPostdocNumberDistribution());
        ChartUtilities.scheduleSeries(this, discoveredMeanAttributes, () -> ((ScienceFunding) state).getDiscoveredMean());
        ChartUtilities.scheduleSeries(this, discoveredStandardDevAttributes, () -> ((ScienceFunding) state).getDiscoveredStandardDev());
        ChartUtilities.scheduleSeries(this, publicationMeanAttributes, () -> ((ScienceFunding) state).getPublicationMean());
        ChartUtilities.scheduleSeries(this, publicationStandardDevAttributes, () -> ((ScienceFunding) state).getPublicationStandardDev());
        setupPortrayals((ScienceFunding) getSimulationInspectedObject());
    }

    /**
     * I think this does the same thing that start(), but with a specific simulation state. Not sure. Saw it on tutorial.
     *
     * @param state The simulation state.
     */
    @Override
    public void load(SimState state) {
        super.load(state);
        ChartUtilities.scheduleSeries(this, falseDiscoveryRateAttributes, ((ScienceFunding) state)::getDiscoveredMean);
        ChartUtilities.scheduleSeries(this, fundsGiniAttributes, ((ScienceFunding) state)::getFundsGini);
        ChartUtilities.scheduleSeries(this, postdocNumberGiniAttributes, ((ScienceFunding) state)::getPostdocNumberGini);
        ChartUtilities.scheduleSeries(this, fundsDistributionAttributes, ((ScienceFunding) state)::getFundsDistribution);
        ChartUtilities.scheduleSeries(this, postdocNumberDistributionAttributes, ((ScienceFunding) state)::getPostdocNumberDistribution);
        ChartUtilities.scheduleSeries(this, discoveredMeanAttributes, ((ScienceFunding) state)::getDiscoveredMean);
        ChartUtilities.scheduleSeries(this, discoveredStandardDevAttributes, ((ScienceFunding) state)::getDiscoveredStandardDev);
        ChartUtilities.scheduleSeries(this, publicationMeanAttributes, ((ScienceFunding) state)::getPublicationMean);
        ChartUtilities.scheduleSeries(this, publicationStandardDevAttributes, ((ScienceFunding) state)::getPublicationStandardDev);
        setupPortrayals((ScienceFunding) state);
    }

    /**
     * Sets up both the charts and the visualization of the labs on the epistemic landscape.
     * This follows students tutorial available in Mason Manual, p. 33.
     * The last lines define the colors of the portrayal of the epistemic landscape.
     *
     * @param state Simulation state casted as ScienceFunding.
     */
    public void setupPortrayals(ScienceFunding state) {
        landscapePortrayal.setField(state.getEpistemicLandscape());
        labsPortrayal.setField(state.getLocationOfLaboratories());
        labsPortrayal.setPortrayalForAll(new OvalPortrayal2D() {
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
                Lab lab = (Lab) object;
                super.draw(object, graphics, info);
            }
        });

        SimpleColorMap colorsOfLandscape = new SimpleColorMap();
        colorsOfLandscape.setLevels(0.001, 0.5, new Color(0, 0, 0, 0), new Color(255, 0, 0, 150));
        landscapePortrayal.setMap(colorsOfLandscape);
        landscapeDisplay.reset();
        landscapeDisplay.setBackdrop(Color.white);
        landscapeDisplay.repaint();
    }

    /**
     * Initializes the UI portrayals. This includes setting up the plots and the 2D landscape.
     * It registers the 2D frames and attaches them to the console visualization, and then
     * attaches the various plots.
     *
     * @param console The controller console for the visualization.
     */
    @Override
    public void init(Controller console) {
        super.init(console);
        int sizeOfLandscape = ((ScienceFunding) state).getSizeOfLandscape();
        landscapeDisplay = new Display2D(sizeOfLandscape, sizeOfLandscape, this);
        landscapeDisplay.setClipping(false);
        landscapeFrame = landscapeDisplay.createFrame();
        landscapeFrame.setTitle("Science Funding");
        console.registerFrame(landscapeFrame);
        landscapeFrame.setVisible(true);
        landscapeDisplay.attach(landscapePortrayal, "Landscape");
        landscapeDisplay.attach(labsPortrayal, "labs");

        timeSeriesChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Time series", "Steps");
        fundsGiniAttributes = ChartUtilities.addSeries(timeSeriesChart, "Funds Gini");
        falseDiscoveryRateAttributes = ChartUtilities.addSeries(timeSeriesChart, "False Discovery Rate");
        postdocNumberGiniAttributes = ChartUtilities.addSeries(timeSeriesChart, "Number of Postdocs Gini");

        fundsHistogram = ChartUtilities.buildHistogramGenerator(this, "Distribution of Funds", "Number of Labs");
        fundsDistributionAttributes = ChartUtilities.addSeries(fundsHistogram, "Total funds (sum)", 5);

        postdocNumberHistogram = ChartUtilities.buildHistogramGenerator(this, "Distribution of Number of Postdocs", "number of labs");
        postdocNumberDistributionAttributes = ChartUtilities.addSeries(postdocNumberHistogram, "Total number of postdocs", 5);

        landscapeChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Landscape state", "Steps");
        discoveredMeanAttributes = ChartUtilities.addSeries(landscapeChart, "Mean Base Rate of Landscape");
        discoveredStandardDevAttributes = ChartUtilities.addSeries(landscapeChart, "Standard Dev of Base Rates");

        publicationChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Publication record", "Steps");
        publicationMeanAttributes = ChartUtilities.addSeries(publicationChart, "Mean number of publications per topic");
        publicationStandardDevAttributes = ChartUtilities.addSeries(publicationChart, "Standard Dev of Publications per Topic");
    }

    /**
     * Cleans the frames when quitting the UI visualization.
     */
    @Override
    public void quit() {
        super.quit();
        if (landscapeFrame != null) {
            landscapeFrame.dispose();
        }
        landscapeFrame = null;
        landscapeDisplay = null;
    }
}