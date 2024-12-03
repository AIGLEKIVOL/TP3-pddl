import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.RequireKey;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.*;
import fr.uga.pddl4j.planners.statespace.search.Node;
import fr.uga.pddl4j.problem.*;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Condition;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.util.*;

/**
 * The class is an example. It shows how to create a simple A* search planner able to
 * solve an ADL problem by choosing the heuristic to used and its weight.
 *
 * @author D. Pellier
 * @version 4.0 - 30.11.2021
 */
@CommandLine.Command(name = "SAT",
    version = "SAT 1.0",
    description = "Solves a specified planning problem using Monte-Carlo search strategy.",
    sortOptions = false,
    mixinStandardHelpOptions = true,
    headerHeading = "Usage:%n",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n")

public class SAT extends AbstractPlanner {


    /**
     * The HEURISTIC property used for planner configuration.
     */
    public static final String HEURISTIC_SETTING = "HEURISTIC";

    /**
     * The default value of the HEURISTIC property used for planner configuration.
     */
    public static final StateHeuristic.Name DEFAULT_HEURISTIC = StateHeuristic.Name.FAST_FORWARD;

    /**
     * The WEIGHT_HEURISTIC property used for planner configuration.
     */
    public static final String WEIGHT_HEURISTIC_SETTING = "WEIGHT_HEURISTIC";

    /**
     * The default value of the WEIGHT_HEURISTIC property used for planner configuration.
     */
    public static final double DEFAULT_WEIGHT_HEURISTIC = 1.0;
    private double heuristicWeight ;
    private StateHeuristic.Name heuristic;
    /**
     * The class logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(SAT.class.getName());

    /**
     * Instantiates the planning problem from a parsed problem.
     *
     * @param problem the problem to instantiate.
     * @return the instantiated planning problem or null if the problem cannot be instantiated.
     */
    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    /**
     * Search a solution plan to a specified domain and problem Monte Carlo.
     *
     * @param problem the problem to solve.
     * @return the plan found or null if no plan was found.
     */
    @Override
    public Plan solve(final Problem problem)  {
        // Creates the A* search strategy
        LOGGER.info("* Starting Monte-Carlo search \n");
        // Search a solution
        final long begin = System.currentTimeMillis();
        final Plan plan = this.MonteCarlo(problem);
        final long end = System.currentTimeMillis();
        // If a plan is found update the statistics of the planner and log search information
        if (plan != null) {
            LOGGER.info("* Monte Carlo search succeeded\n");
            this.getStatistics().setTimeToSearch(end - begin);
            long total = this.getStatistics().getTimeToParse() + this.getStatistics().getTimeToSearch() + this.getStatistics().getTimeToEncode();
            System.out.print("|" + total + "|");
            System.out.println(plan.actions().size() + "|");
        } else {
            LOGGER.info("* Monte Carlo search failed\n");
        }
        // Return the plan found or null if the search fails.
        return plan;
    }
    
    /**
     * Returns if a specified problem is supported by the planner. Just ADL problem can be solved by this planner.
     *
     * @param problem the problem to test.
     * @return <code>true</code> if the problem is supported <code>false</code> otherwise.
     */
    @Override
    public boolean isSupported(Problem problem) {
        return !problem.getRequirements().contains(RequireKey.ACTION_COSTS)
                && !problem.getRequirements().contains(RequireKey.CONSTRAINTS)
                && !problem.getRequirements().contains(RequireKey.CONTINOUS_EFFECTS)
                && !problem.getRequirements().contains(RequireKey.DERIVED_PREDICATES)
                && !problem.getRequirements().contains(RequireKey.DURATIVE_ACTIONS)
                && !problem.getRequirements().contains(RequireKey.DURATION_INEQUALITIES)
                && !problem.getRequirements().contains(RequireKey.FLUENTS)
                && !problem.getRequirements().contains(RequireKey.GOAL_UTILITIES)
                && !problem.getRequirements().contains(RequireKey.METHOD_CONSTRAINTS)
                && !problem.getRequirements().contains(RequireKey.NUMERIC_FLUENTS)
                && !problem.getRequirements().contains(RequireKey.OBJECT_FLUENTS)
                && !problem.getRequirements().contains(RequireKey.PREFERENCES)
                && !problem.getRequirements().contains(RequireKey.TIMED_INITIAL_LITERALS)
                && !problem.getRequirements().contains(RequireKey.HIERARCHY);
    }

    /**
     * The main method of the <code>ASP</code> planner.
     *
     * @param args the arguments of the command line.
     */
    public static void main(String[] args) {
        try {
            final SAT planner = new SAT();
            CommandLine cmd = new CommandLine(planner);
            cmd.execute(args);
        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        }
    }
    
    /**
     * Returns the name of the heuristic used by the planner to solve a planning problem.
     *
     * @return the name of the heuristic used by the planner to solve a planning problem.
     */
    public final StateHeuristic.Name getHeuristic() {
        return this.heuristic;
    }

    /**
     * Returns the weight of the heuristic.
     *
     * @return the weight of the heuristic.
     */
    public final double getHeuristicWeight() {
        return this.heuristicWeight;
    }

    /**
     * Sets the weight of the heuristic.
     *
     * @param weight the weight of the heuristic. The weight must be greater than 0.
     * @throws IllegalArgumentException if the weight is strictly less than 0.
     */
    @CommandLine.Option(names = {"-w", "--weight"}, defaultValue = "1.0",
            paramLabel = "<weight>", description = "Set the weight of the heuristic (preset 1.0).")
    public void setHeuristicWeight(final double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight <= 0");
        }
        this.heuristicWeight = weight;
    }

    /**
     * Set the name of heuristic used by the planner to the solve a planning problem.
     *
     * @param heuristic the name of the heuristic.
     */
    @CommandLine.Option(names = {"-e", "--heuristic"}, defaultValue = "FAST_FORWARD",
            description = "Set the heuristic : AJUSTED_SUM, AJUSTED_SUM2, AJUSTED_SUM2M, COMBO, "
                    + "MAX, FAST_FORWARD SET_LEVEL, SUM, SUM_MUTEX (preset: FAST_FORWARD)")
    public void setHeuristic(StateHeuristic.Name heuristic) {
        this.heuristic = heuristic;
    }



    /**
     * Returns the configuration of the planner.
     *
     * @return the configuration of the planner.
     */
    @Override
    public PlannerConfiguration getConfiguration() {
        final PlannerConfiguration config = super.getConfiguration();
        config.setProperty(SAT.HEURISTIC_SETTING, this.getHeuristic().toString());
        config.setProperty(SAT.WEIGHT_HEURISTIC_SETTING, Double.toString(this.getHeuristicWeight()));
        return config;
    }

    /**
     * Sets the configuration of the planner. If a planner setting is not defined in
     * the specified configuration, the setting is initialized with its default value.
     *
     * @param configuration the configuration to set.
     */
    @Override
    public void setConfiguration(final PlannerConfiguration configuration) {
        super.setConfiguration(configuration);
        if (configuration.getProperty(SAT.WEIGHT_HEURISTIC_SETTING) == null) {
            this.setHeuristicWeight(SAT.DEFAULT_WEIGHT_HEURISTIC);
        } else {
            this.setHeuristicWeight(Double.parseDouble(configuration.getProperty(
                    SAT.WEIGHT_HEURISTIC_SETTING)));
        }
        if (configuration.getProperty(SAT.HEURISTIC_SETTING) == null) {
            this.setHeuristic(SAT.DEFAULT_HEURISTIC);
        } else {
            this.setHeuristic(StateHeuristic.Name.valueOf(configuration.getProperty(
                    SAT.HEURISTIC_SETTING)));
        }
    }

    /**
     * Checks the planner configuration and returns if the configuration is valid.
     * A configuration is valid if (1) the domain and the problem files exist and
     * can be read, (2) the timeout is greater than 0, (3) the weight of the
     * heuristic is greater than 0 and (4) the heuristic is a not null.
     *
     * @return <code>true</code> if the configuration is valid <code>false</code> otherwise.
     */
    public boolean hasValidConfiguration() {
        return super.hasValidConfiguration()
                && this.getHeuristicWeight() > 0.0
                && this.getHeuristic() != null;
    }

    /**
     * Creates a new A* search planner with the default configuration.
     */
    public SAT() {
        this(Planner.getDefaultConfiguration());
    }

    /**
     * Creates a new A* search planner with a specified configuration.
     *
     * @param configuration the configuration of the planner.
     */
    public SAT(final PlannerConfiguration configuration) {
        super();
        this.setConfiguration(configuration);
    }
    /**
     * Search a solution plan for a planning problem using an A* search strategy.
     *
     * @param problem the problem to solve.
     * @return a plan solution for the problem or null if there is no solution
     * @throws ProblemNotSupportedException if the problem to solve is not supported by the planner.
     */
    public Plan astar(Problem problem) throws ProblemNotSupportedException {
        // Check if the problem is supported by the planner
        if (!this.isSupported(problem)) {
            throw new ProblemNotSupportedException("Problem not supported");
        }

        // First we create an instance of the heuristic to use to guide the search
        final StateHeuristic heuristic = StateHeuristic.getInstance(this.getHeuristic(), problem);

        // We get the initial state from the planning problem
        final State init = new State(problem.getInitialState());

        // We initialize the closed list of nodes (store the nodes explored)
        final Set<Node> close = new HashSet<>();

        // We initialize the opened list to store the pending node according to function f
        final double weight = this.getHeuristicWeight();
        final PriorityQueue<Node> open = new PriorityQueue<>(100, new Comparator<Node>() {
            public int compare(Node n1, Node n2) {
                double f1 = weight * n1.getHeuristic() + n1.getCost();
                double f2 = weight * n2.getHeuristic() + n2.getCost();
                return Double.compare(f1, f2);
            }
        });

        // We create the root node of the tree search
        final Node root = new Node(init, null, -1, 0, heuristic.estimate(init, problem.getGoal()));

        // We add the root to the list of pending nodes
        open.add(root);
        Plan plan = null;

        // We set the timeout in ms allocated to the search
        final int timeout = this.getTimeout() * 1000;
        long time = System.currentTimeMillis();
        // We start the search
        while (!open.isEmpty() && plan == null && (time-System.currentTimeMillis()) < timeout) {

            // We pop the first node in the pending list open
            final Node current = open.poll();
            close.add(current);

            // If the goal is satisfied in the current node then extract the search and return it
            if (current.satisfy(problem.getGoal())) {
                return this.extractPlan(current, problem);
            } else { // Else we try to apply the actions of the problem to the current node
                for (int i = 0; i < problem.getActions().size(); i++) {
                    // We get the actions of the problem
                    Action a = problem.getActions().get(i);
                    // If the action is applicable in the current node
                    if (a.isApplicable(current)) {
                        Node next = new Node(current);
                        // We apply the effect of the action
                        final List<ConditionalEffect> effects = a.getConditionalEffects();
                        for (ConditionalEffect ce : effects) {
                            if (current.satisfy(ce.getCondition())) {
                                next.apply(ce.getEffect());
                            }
                        }
                        // We set the new child node information
                        final double g = current.getCost() + 1;
                        if (!close.contains(next)) {
                            next.setCost(g);
                            next.setParent(current);
                            next.setAction(i);
                            next.setHeuristic(heuristic.estimate(next, problem.getGoal()));
                            open.add(next);
                        }
                    }
                }
            }
        }
        // Finally, we return the search computed or null if no search was found
        return plan;
    }

    int NUM_WALK = 2000;
    int LENGTH_WALK = 10;
    int MAX_STEPS = 7;
    
    // Méthode principale de planification
    public Plan MonteCarlo(Problem problem) {
        StateHeuristic heuristic = StateHeuristic.getInstance(this.getHeuristic(), problem);
        Condition  goal = problem.getGoal();
        State s0 = new State(problem.getInitialState());
        Node s = new Node(s0, null, -1, 0, heuristic.estimate(s0, goal));
        double hmin = heuristic.estimate(s0, goal);
        int counter = 0;
        final int timeout = this.getTimeout() * 1000;
        long time = System.currentTimeMillis();

        while (!s.satisfy(goal) && (System.currentTimeMillis()-time< timeout)) {
            if (counter > MAX_STEPS || DeadEnd(s,problem)) {
                s = new Node(s0, null, -1, 0, heuristic.estimate(s0, goal));
                //System.out.println("s = " + s.getParent().getAction());
                counter = 0;
            }
            s = MonteCarloRandomWalks(problem,s, goal,heuristic);
            double hs = heuristic.estimate(s,goal);
            if (hs < hmin) {
                hmin = hs;
                counter = 0;
                //System.out.println("hmin = " + hmin);
            } else {
                counter++;
            }
        }
        return this.extractPlan(s, problem);
    }

    private boolean DeadEnd(Node sprime,Problem problem) {
        return getApplicableActions(sprime,problem).isEmpty();
    }

    // Monte Carlo 
    private Node MonteCarloRandomWalks(Problem problem,Node s, Condition goal, StateHeuristic heuristic) {
        double hmin = Double.POSITIVE_INFINITY;
        Node smin = null;
        //StringBuilder result = new StringBuilder();
        for (int i = 0; i < NUM_WALK; i++) {
            Node sPrime = new Node(s,s.getParent(),s.getAction(),s.getCost(),s.getHeuristic());
            for (int j = 0; j < LENGTH_WALK; j++) {
                List<Action> A = getApplicableActions(sPrime, problem);
                if(A.isEmpty()) break;
                Action a = getUniformelyRandomSelectFrom(A);
                int indexAction = problem.getActions().indexOf(a);
                Node newNode = appliquer(sPrime, a, problem ,indexAction,heuristic );
                //result.append(indexAction).append(":").append(a.getName()).append(" ");
                if (newNode.satisfy(goal)) {
                    //System.out.println("Action : "+result);
                    return newNode;
                }    
                sPrime = newNode;
            }
            //result = new StringBuilder();
            double hs = heuristic.estimate(sPrime,goal);
            if (hs < hmin) {
                smin = sPrime;
                hmin = hs;
                
            }
        }
        return (smin == null)? s:smin;
    }

    // Applique l'effet d'une action et met à jour l'état
    private Node appliquer(Node sPrime,  Action a, Problem problem, int indexAction, StateHeuristic heuristic) {
        // Application de l'effet de l'action
        Node next = new Node(sPrime);
        List<ConditionalEffect> effects = a.getConditionalEffects();
        for (ConditionalEffect ce : effects) {
            if (next.satisfy(ce.getCondition())) {
                next.apply(ce.getEffect());
            }
        }
        // Mise à jour des informations du nœud suivant
        double g = sPrime.getCost() + 1;
        next.setCost(g);
        next.setParent(sPrime);
        next.setAction(indexAction);
        next.setHeuristic(heuristic.estimate(next, problem.getGoal()));
        return next;
    }

    private Action getUniformelyRandomSelectFrom(List<Action> A) {
        return A.get((int) (Math.random() * A.size()));
    }

    // Sélection des actions applicables dans un état donné
    private List<Action> getApplicableActions(State s, Problem problem) {
        List<Action> actions = new ArrayList<>();
        for (int i = 0; i < problem.getActions().size(); i++) {
            Action a = problem.getActions().get(i);
            if (a.isApplicable(s)) {
                actions.add(a);
            }
        }
        return actions;
    }


    /**
     * Extracts a search from a specified node.
     *
     * @param node    the node.
     * @param problem the problem.
     * @return the search extracted from the specified node.
     */

    // Extraire le plan final
    private Plan extractPlan(final Node node, final Problem problem) {
        Node n = node;
        final Plan plan = new SequentialPlan();
        while (n.getAction() != -1) {
            final Action a = problem.getActions().get(n.getAction());
            plan.add(0, a);
            n = n.getParent();
        }
        return plan;
    }
    
}
