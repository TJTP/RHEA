package tracks.singlePlayer.advanced.myRHEA;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;
import tracks.singlePlayer.tools.Heuristics.WinScoreHeuristic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Agent extends AbstractPlayer {

    // Constants
    protected static final int UNIFORM_CROSS = 0;
    protected static final int POINT1_CROSS = 1;
    protected static final int POINT2_CROSS = 2;
    protected static final double MUT_PROB = 0.9;
    private static final int POPULATION_SIZE = 10;
    private static final long BREAK_MS = 10;
    private static final int SIMULATION_DEPTH = 10;
    private static final int MUTATION = 1;
    private static final int ELITISM = 2;
    private static final int TOURNAMENT_SIZE = 3;
    private static final int BETTER_PARENT_RANK = 5; //前 BETTER_PARENT_RANK 个个体视为优秀父母个体
    private static final double BETTER_PARENT_PROB = 0.8; //在选择个体进入tournament时,
                                                        // 以 BETTER_PARENT_RATIO 概率选择前 BETTER_PARENT_RANK 范围内的个体

    // Parameters
    private int crossType;
    private int indNum;
    private int numActions;
    private StateHeuristic heuristic;
    private Individual[] population, nextPop;
    private HashMap<Integer, Types.ACTIONS> actionMapping;
    private Random randomGenerator;

    // Budgets
    private ElapsedCpuTimer timer;
    private double acumTimeTakenEval, avgTimeTakenEval, avgTimeTaken, acumTimeTaken;
    private int numEvals, numIters;
    private boolean keepIterating;
    private long globalRemaining;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.crossType = UNIFORM_CROSS;
        //this.heuristic = new MyHeuristic();
        this.heuristic = new WinScoreHeuristic(stateObs);
        this.randomGenerator = new Random();

        this.timer = elapsedTimer;
        this.acumTimeTakenEval = 0;
        this.avgTimeTakenEval = 0;
        this.avgTimeTaken = 0;
        this.acumTimeTaken = 0;
        this.numEvals = 0;
        this.numIters = 0;
        this.keepIterating = true;

    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        this.timer = elapsedTimer;
        this.avgTimeTaken = 0;
        this.acumTimeTaken = 0;
        this.numEvals = 0;
        this.acumTimeTakenEval = 0;
        this.numIters = 0;
        this.globalRemaining = timer.remainingTimeMillis();
        this.indNum = 0;
        this.keepIterating = true;

        // INITIALISE POPULATION
        init_pop(stateObs);

        // RUN EVOLUTION
        this.globalRemaining = this.timer.remainingTimeMillis();
        while (this.globalRemaining > this.avgTimeTaken && this.globalRemaining > BREAK_MS && this.keepIterating) {
            runIteration(stateObs);
            this.globalRemaining = this.timer.remainingTimeMillis();
        }

        // RETURN ACTION
        return get_best_action(this.population);
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(StateObservation stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        if (this.indNum > 1) {
            for (int i = ELITISM; i < this.indNum; i++) {
                if (this.globalRemaining > 2 * this.avgTimeTakenEval && this.globalRemaining > BREAK_MS) {
                    // if enough time to evaluate one more individual
                    Individual newInd = crossover();
                    newInd = newInd.mutate(MUTATION);

                    // evaluate new individual, insert into population
                    add_individual(newInd, this.nextPop, i, stateObs);

                    this.globalRemaining = this.timer.remainingTimeMillis();
                } else {
                    this.keepIterating = false;
                    break;
                }
            }

            Arrays.sort(this.nextPop, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return o1.compareTo(o2);
            });

        } else if (this.indNum == 1){
            Individual newInd = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator).mutate(MUTATION);
            evaluate(newInd, this.heuristic, stateObs);
            if (newInd.value > this.population[0].value)
                this.nextPop[0] = newInd;
        }

        this.population = this.nextPop.clone();

        this.numIters++;
        this.acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        this.avgTimeTaken = this.acumTimeTaken / this.numIters;
    }

    /**
     * Evaluates an individual by rolling the current state with the actions in the individual
     * and returning the value of the resulting state; random action chosen for the opponent
     * @param individual - individual to be valued
     * @param heuristic - heuristic to be used for state evaluation
     * @param state - current state, root of rollouts
     * @return - value of last state reached
     */
    private double evaluate(Individual individual, StateHeuristic heuristic, StateObservation state) {

        ElapsedCpuTimer elapsedTimerIterationEval = new ElapsedCpuTimer();

        StateObservation stCopy = state.copy();
        int i;
        double acum = 0, avg;
        for (i = 0; i < SIMULATION_DEPTH; i++) {
            if (!stCopy.isGameOver()) {
                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
                stCopy.advance(this.actionMapping.get(individual.actions[i]));

                acum += elapsedTimerIteration.elapsedMillis();
                avg = acum / (i+1);
                this.globalRemaining = this.timer.remainingTimeMillis();
                if (this.globalRemaining < 2 * avg || this.globalRemaining < BREAK_MS) break;
            } else {
                break;
            }
        }

        individual.value = this.heuristic.evaluateState(stCopy);

        this.numEvals++;
        this.acumTimeTakenEval += (elapsedTimerIterationEval.elapsedMillis());
        this.avgTimeTakenEval = this.acumTimeTakenEval / this.numEvals;
        this.globalRemaining = this.timer.remainingTimeMillis();

        return individual.value;
    }

    /**
     * 在 parent selection 环节, 前 BETTER_PARENT_RANK 个个体更有可能被选为parents
     * 在 crossover 时, 随机从三种算子中随机选择一种
     * @return - the individual resulting from crossover applied to the specified population
     */
    private Individual crossover() {
        Individual newInd = null;
        if (this.indNum > 1) {
            newInd = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator);
            Individual[] tournament = new Individual[TOURNAMENT_SIZE];
            ArrayList<Individual> popList = new ArrayList<>(Arrays.asList(this.population));

            //Select a number of random distinct individuals for tournament and sort them based on value
            Random ratioGenerator = new Random();
            int betterParentRank = popList.size() > BETTER_PARENT_RANK ? BETTER_PARENT_RANK : popList.size() / 2;
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                if (ratioGenerator.nextInt(100) < BETTER_PARENT_PROB * 100) {
                    int idx = this.randomGenerator.nextInt(betterParentRank);
                    tournament[i] = popList.get(idx);
                    popList.remove(idx);
                } else {
                    int idx = this.randomGenerator.nextInt(popList.size() - betterParentRank) + betterParentRank;
                    tournament[i] = popList.get(idx);
                    popList.remove(idx);
                }
            }
            Arrays.sort(tournament);

            //get best individuals in tournament as parents
            if (TOURNAMENT_SIZE >= 2) {
                Random crossGenerator = new Random();
                this.crossType = crossGenerator.nextInt(3);
                newInd.crossover(tournament[0], tournament[1], this.crossType);
            } else {
                System.out.println("WARNING: Number of parents must be LESS than tournament size.");
            }
        }
        return newInd;
    }

    /**
     * Insert a new individual into the population at the specified position by replacing the old one.
     * @param newInd - individual to be inserted into population
     * @param pop - population
     * @param idx - position where individual should be inserted
     * @param stateObs - current game state
     */
    private void add_individual(Individual newInd, Individual[] pop, int idx, StateObservation stateObs) {
        evaluate(newInd, this.heuristic, stateObs);
        pop[idx] = newInd.copy();
    }

    /**
     * Initialize population
     * @param stateObs - current game state
     */
    private void init_pop(StateObservation stateObs) {

        double remaining = this.timer.remainingTimeMillis();

        this.numActions = stateObs.getAvailableActions().size() + 1;
        this.actionMapping = new HashMap<>();
        int k = 0;
        for (Types.ACTIONS action : stateObs.getAvailableActions()) {
            this.actionMapping.put(k, action);
            k++;
        }
        this.actionMapping.put(k, Types.ACTIONS.ACTION_NIL);

        this.population = new Individual[POPULATION_SIZE];
        this.nextPop = new Individual[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            if (i == 0 || remaining > this.avgTimeTakenEval && remaining > BREAK_MS) {
                population[i] = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator);
                evaluate(this.population[i], this.heuristic, stateObs);
                remaining = this.timer.remainingTimeMillis();
                this.indNum = i + 1;
            } else {break;}
        }

        if (this.indNum > 1)
            Arrays.sort(this.population, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return o1.compareTo(o2);
            });

        for (int i = 0; i < this.indNum; i++) {
            if (this.population[i] != null)
                this.nextPop[i] = this.population[i].copy();
        }

    }

    /**
     * @param pop - last population obtained after evolution
     * @return - first action of best individual in the population (found at index 0)
     */
    private Types.ACTIONS get_best_action(Individual[] pop) {
        int bestAction = pop[0].actions[0];
        return this.actionMapping.get(bestAction);
    }
}
