import java.util.HashMap;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.problem.operator.Effect;
import fr.uga.pddl4j.util.BitVector;

public class SATransformer {

    private static SATransformer instance;
    private int nextActionId = 1; 
    private final Map<Action, Integer> actionIds = new HashMap<>();
    
    public static SATransformer getInstance() {
        if (instance == null) {
            instance = new SATransformer();
        }
        return instance;
    }

    // Encode the problem
    public ISolver encode(ISolver solver, Problem problem) throws ContradictionException {
        encodeInitialState(problem, solver);
        encodeGoalState(problem, solver);
        encodeActions(problem, solver);

        System.out.println(solver.toString());
        return solver;
    }

    private void encodeInitialState(Problem problem, ISolver solver) throws ContradictionException {
        BitVector initialState = problem.getInitialState().getPositiveFluents();
        for (int i = 0; i < initialState.size(); i++) {
            if (initialState.get(i)) {
                int var = fluentToVar(i, 0);
                if(var!=0)
                    solver.addClause(new VecInt(new int[] { var }));
            }
        }
    }
    private void encodeGoalState(Problem problem, ISolver solver) throws ContradictionException {
        BitVector goalState = problem.getGoal().getPositiveFluents();
        for (int i = 0; i < goalState.size(); i++) {
            if (goalState.get(i)) {
                int var = fluentToVar(i, Integer.MAX_VALUE);
                solver.addClause(new VecInt(new int[] { var }));
            }
        }
    }

    private void encodeActions(Problem problem, ISolver solver) throws ContradictionException {
        for (Action action : problem.getActions()) {
            int actionId = getActionId(action);

            if (action.getPrecondition() != null) {
                BitVector preconditions = action.getPrecondition().getPositiveFluents();
                for (int i = 0; i < preconditions.size(); i++) {
                    if (preconditions.get(i)) {
                        int var = fluentToVar(i, actionId);
                        solver.addClause(new VecInt(new int[] { -actionId, var })); 
                    }
                }
            }

            for (ConditionalEffect condEffect : action.getConditionalEffects()) {
                Effect effect = condEffect.getEffect();
                BitVector positiveEffects = effect.getPositiveFluents();
                for (int i = 0; i < positiveEffects.size(); i++) {
                    if (positiveEffects.get(i)) {
                        if (problem.getGoal().getPositiveFluents().get(i)) {
                            int var = fluentToVar(i, actionId + 1);
                            solver.addClause(new VecInt(new int[] { -actionId, var })); 
                        }
                    }
                }
            }
        }
    }

    // Decodes the solution from the SAT solver
    public Plan decodeSolution(int[] model, Problem problem) {
        Plan plan = new SequentialPlan();
        for (int var : model) {
            if (var > 0) { 
                Action action = getActionByVar(var);
                if (action != null) {
                    plan.add(0, action);
                }
            }
        }
        return plan;
    }

    private Action getActionByVar(int var) {
        for (Map.Entry<Action, Integer> entry : actionIds.entrySet()) {
            if (entry.getValue().equals(var)) {
                return entry.getKey();
            }
        }
        return null;
    }

     private int fluentToVar(int fluentId, int timeStep) {
        return (timeStep * 30000) + fluentId; 
    }

    private int getActionId(Action action) {
        return actionIds.computeIfAbsent(action, a -> nextActionId++);
    }

}

