package tracks.singlePlayer.advanced.myAdaptiveRHEA;

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
    protected static final double MUT_PROB = 0.6;
    private static final int POPULATION_SIZE = 10;
    private static final long BREAK_MS = 10;
    private static final int SIMULATION_DEPTH = 8;
    private static final int MUTATION_NUM = 1;
    private static final int ELITISM = 2;
    private static final int TOURNAMENT_SIZE = 3;
    private static final int BETTER_PARENT_RANK = 5; //前 BETTER_PARENT_RANK 个个体视为优秀父母个体
    private static final double BETTER_PARENT_PROB = 0.6; //在选择个体进入tournament时,
                                                        // 以 BETTER_PARENT_RATIO 概率选择前 BETTER_PARENT_RANK 范围内的个体
    private static final int MAX_ITER_DIFF = 5;

    // Parameters
    private int crossType;
    private int indNum;
    private int numActions;
    private boolean isFirstTime;
    private StateHeuristic heuristic;
    private Individual[] population, nextPop;
    private HashMap<Integer, Types.ACTIONS> actionMapping;
    private Random randomGenerator;
    private int lastIterNum;
    private boolean mutOnFirst;

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
        this.indNum = 0;
        this.isFirstTime = true;
        //this.heuristic = new MyHeuristic();
        this.heuristic = new WinScoreHeuristic(stateObs);
        this.randomGenerator = new Random();
        this.lastIterNum = 1;
        this.mutOnFirst = false;

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

        this.keepIterating = true;

//        System.out.println("====================== act () ======================");
        // INITIALISE POPULATION
//        init_pop(stateObs);
        if (this.isFirstTime) {
            this.init_pop(stateObs);
            this.isFirstTime = false;
//            System.out.println("Init populations");
        } else {
            for (int i = 0; i < this.indNum; i++) {
                this.population[i].actions.remove(0);
                this.population[i].actions.add(this.randomGenerator.nextInt(numActions));
                this.evaluate(this.population[i], this.heuristic, stateObs);
            }
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
//            System.out.println("Re-evaled populations");
        }
//        System.out.printf("Size of action mapping: %d. ", this.actionMapping.size());
//        for (int i = 0; i < 6; i++) {
//            System.out.print(this.actionMapping.get(i));
//            System.out.print(" ");
//        }
//        System.out.printf("\n");

//        System.out.printf("Individual num of population: %d\n", this.indNum);
        int iterCnt = 0;
        // RUN EVOLUTION
        this.globalRemaining = this.timer.remainingTimeMillis();
        while (this.globalRemaining > this.avgTimeTaken && this.globalRemaining > BREAK_MS && this.keepIterating) {
            iterCnt += 1;
            if (iterCnt - this.lastIterNum >= MAX_ITER_DIFF)  {
                this.mutOnFirst = true;
                System.out.println("here");
                for (int i = 0; i < ELITISM; i++) {
                    this.nextPop[i].mutate(0, true);
                }
                runIteration(stateObs);
                this.globalRemaining = this.timer.remainingTimeMillis();
                this.mutOnFirst = false;
//                break;
            }
            runIteration(stateObs);
            this.globalRemaining = this.timer.remainingTimeMillis();
        }

        System.out.printf("IterCnt: %d\n", iterCnt);
        this.lastIterNum = iterCnt;

        // RETURN ACTION
        Types.ACTIONS action = get_best_action(this.population);
//        for (int j = 0; j < this.indNum; j++) {
//            System.out.printf("Actions of population[%d]: ", j);
//            for (int i = 0; i < this.population[j].actions.size(); i++) {
//                System.out.print(this.actionMapping.get(this.population[j].actions.get(i)));
//                System.out.print(' ');
//            }
//            System.out.print('\n');
//        }
//        System.out.println(action);
//        System.out.println("======================++++++++======================");
        return action;
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(StateObservation stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
//        System.out.println("runIteration()");

        if (this.indNum > 1) {
            int curIndNum = ELITISM;
            for (int i = ELITISM; i < POPULATION_SIZE; i++) {
                if (this.globalRemaining > 2 * this.avgTimeTakenEval && this.globalRemaining > BREAK_MS) {
                    // if enough time to evaluate one more individual
                    Individual newInd = crossover();
                    //newInd = newInd.mutate(MUTATION_NUM);
                    newInd.mutate(MUTATION_NUM, this.mutOnFirst);

                    // evaluate new individual, insert into population
                    this.add_individual(newInd, this.nextPop, i, stateObs);
                    curIndNum += 1;

                    this.globalRemaining = this.timer.remainingTimeMillis();
                } else {
                    this.keepIterating = false;
                    break;
                }
            }
            if (curIndNum > this.indNum) this.indNum = curIndNum;

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
//            Individual newInd = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator).mutate(MUTATION_NUM);
            Individual newInd = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator);
            newInd.mutate(MUTATION_NUM, false);
            this.evaluate(newInd, this.heuristic, stateObs);
            if (newInd.value > this.population[0].value)
                this.nextPop[0] = newInd;
        }

        this.population = this.nextPop.clone();

        this.numIters++;
        this.acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        this.avgTimeTaken = this.acumTimeTaken / this.numIters;
//        System.out.println("End runIteration()");
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
//                stCopy.advance(this.actionMapping.get(individual.actions[i]));
                stCopy.advance(this.actionMapping.get(individual.actions.get(i)));

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
//        System.out.println("\tcrossover()");
        Individual newInd = null;
        if (this.indNum > 1) {
            newInd = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator);
            Individual[] tournament = null;

            if (this.indNum <= TOURNAMENT_SIZE) {
                tournament = new Individual[this.indNum];
                for (int i = 0; i < this.indNum; i++) {
                    tournament[i] = this.population[i];
                }
            } else {
                tournament = new Individual[TOURNAMENT_SIZE];
                ArrayList<Individual> popList = new ArrayList<>(Arrays.asList(this.population));

                //Select a number of random distinct individuals for tournament and sort them based on value
                Random ratioGenerator = new Random();
                int betterParentRank = this.indNum > BETTER_PARENT_RANK ? BETTER_PARENT_RANK : this.indNum / 2;
                int lastIndIdx = this.indNum - 1;
                for (int i = 0; i < TOURNAMENT_SIZE; i++) {
//                    System.out.printf("\t\tpopList size %d, bPR: %d, lastIdx: %d\n", popList.size(), betterParentRank, lastIndIdx);
                    if (ratioGenerator.nextInt(100) < BETTER_PARENT_PROB * 100) {
                        if (betterParentRank <= 0) {
                            i -= 1;
                            continue;
                        }
                        int idx = this.randomGenerator.nextInt(betterParentRank);
//                        System.out.printf("\t\tidx: %d", idx);
                        tournament[i] = popList.get(idx);
//                        System.out.print("\t\tBetter parents: ");
//                        System.out.println(popList.get(idx).getClass());
                        popList.remove(idx);
                        betterParentRank -= 1;
                        lastIndIdx -= 1;
                    } else {
                        if (lastIndIdx <= betterParentRank) {
                            i -= 1;
                            continue;
                        }
                        int idx = this.randomGenerator.nextInt(lastIndIdx - betterParentRank) + betterParentRank;
//                        System.out.printf("\t\tidx: %d", idx);
                        tournament[i] = popList.get(idx);
//                        System.out.print("\t\tWorse parents: ");
//                        System.out.println(popList.get(idx).getClass());
                        popList.remove(idx);
                        lastIndIdx -= 1;
                    }
                }
            }

            Arrays.sort(tournament);

//            System.out.print("\t\t");
//            for (int i = 0; i < tournament.length; i++) {
//                System.out.print("ha");
//                System.out.print(" ");
//            }
//            System.out.print("\n");

            //get best individuals in tournament as parents
            if (TOURNAMENT_SIZE >= 2) {
                Random crossGenerator = new Random();
                this.crossType = crossGenerator.nextInt(3);
//                System.out.println(newInd.actions.size());
                newInd.crossover(tournament[0], tournament[1], this.crossType);
            } else {
                System.out.println("WARNING: Number of parents must be LESS than tournament size.");
            }
        }
//        System.out.println("\tEnd crossover()");
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
        this.evaluate(newInd, this.heuristic, stateObs);
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
                this.evaluate(this.population[i], this.heuristic, stateObs);
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
//        int bestAction = pop[0].actions[0];
        int bestAction = pop[0].actions.get(0);
        return this.actionMapping.get(bestAction);
    }
    

}
