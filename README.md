# ScienceFundingModel
Model of Science Funding Policies based on Natural Selection of Bad Science

Built using MASON library https://cs.gmu.edu/~eclab/projects/mason/. Originally built using IntelliJ IDE. For convenience of synching between working computers while coding it, .idea folder is included.

## Classes ##

The package has 8 classes: 
  Main class without UI is ScienceFunding.java. It contains most model parameters and the simulation state (SimState). It also initializes every object in the scheduler. This includes the different landscapes: the epistemic landscape proper (a 2D grid of doubles), the grid where the labs are positioned (a 2D sparse grid) and the number of publications for each location on the landscape (a 2D grid of integers). The function of the different parameters is detailed below. If user wants to include established topics in the simulation, they are initialized in this class too. 
	
Main class with UI is ScienceFundingWithUI.java. It calls the main method of ScienceFunding while initializing the visualization of the different landscapes and a series of default plotting of global measures of the model: a time series including different Gini Indices and the false discovery rate over time; a histogram of the distribution of funds among the different labs; a histogram of the total number of postdocs; a time series of the rate at which the landscape is being discovered; and a time series of the rate at which the landscape is being published about.
	
The main agent for the model is coded in the Lab.java class. Labs have different attributes, including a unique LabID identificator; its age; its methodology (effort); its historic payoffs for publication; the utility with which it applies for funding; its location on the landscape; and its number of grants and postdocs. During each step, labs define how many postdocs they will have this turn based on the number of grants left with more than 0 years of funding remaining. After that, they update their topic, moving according to the movement methods detailed in the updateTopic() method. (#NOTE: THIS IS STILL BEING WORKED ON#). After updating their topic, labs do research about their current location in the epistemic landscape; this process includes rolling for if they are going to apply for funding or not. When doing research, a series of rolls will determine if the hypothesis being considered is true or false, while different global parameters (discovery power) and agent-level parameters (effort) determine if the lab correctly detects the truth of the hypothesis. They then publish their results. Positive results are always published, while negative results are published according to a global parameter.
	
