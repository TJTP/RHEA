package tracks.singlePlayer.advanced.myRHEA;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;
import tracks.singlePlayer.tools.Heuristics.WinScoreHeuristic;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Agent extends AbstractPlayer {

    // Constants
    protected static final int UNIFORM_CROSS = 0;
    protected static final int POINT1_CROSS = 1;
    protected static final int POINT2_CROSS = 2;
    protected static final double MUT_PROB = 0.96;
    private static final long BREAK_MS = 10;

    private static final int ELITISM = 2;
    private static final int TOURNAMENT_SIZE = 3;
    private static final int BETTER_PARENT_RANK = 5; //前 BETTER_PARENT_RANK 个个体视为优秀父母个体
    private static final double BETTER_PARENT_PROB = 0.7; //在选择个体进入tournament时,
                                                        // 以 BETTER_PARENT_RATIO 概率选择前 BETTER_PARENT_RANK 范围内的个体

    // 游戏编号
    protected static final int BM = 9;
    protected static final int JAWS = 56;
    protected static final int RF = 80;
    protected static final int SM = 89;

    // Parameters
    private static int POPULATION_SIZE;
    private static int SIMULATION_DEPTH;
    private static int MUTATION_NUM;
    private static int gameType;
    private int crossType;
    private int indNum;
    private int numActions;
    private boolean isFirstTime;
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
        this.gameType = this.recognizeGameType(stateObs);
        if (this.gameType == BM) {
            POPULATION_SIZE = 16;
            SIMULATION_DEPTH = 4;
            MUTATION_NUM = 2;
        } else if (this.gameType == JAWS) {
            POPULATION_SIZE = 16;
            SIMULATION_DEPTH = 4;
            MUTATION_NUM = 2;
        } else if (this.gameType == RF) {
            POPULATION_SIZE = 12;
            SIMULATION_DEPTH = 10;
            MUTATION_NUM = 1;
        } else if (this.gameType == SM) {
            POPULATION_SIZE = 15;
            SIMULATION_DEPTH = 15;
            MUTATION_NUM = 2;
        } else {
            POPULATION_SIZE = 16;
            SIMULATION_DEPTH = 4;
            MUTATION_NUM = 2;
        }

        this.crossType = UNIFORM_CROSS;
        this.indNum = 0;
        this.isFirstTime = true;
        this.heuristic = new MyHeuristic(this.gameType);
//        this.heuristic = new WinScoreHeuristic(stateObs);
        this.randomGenerator = new Random();

    }
    private void printInfo (StateObservation stateObs) {
        Vector2d avatarPosition = stateObs.getAvatarPosition();
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources();

        System.out.println("====================== Info () ======================");
        System.out.print("World dimension: ");
        System.out.println(stateObs.getWorldDimension());
        System.out.print("Block size: ");
        System.out.println(stateObs.getBlockSize());
        System.out.print("Resources: ");
        System.out.println(resources);
        System.out.print("Avatar hp: ");
        System.out.println(stateObs.getAvatarHealthPoints());

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPosition);
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPosition);
        ArrayList<Observation>[] immovables = stateObs.getImmovablePositions(avatarPosition);
        ArrayList<Observation>[] movables = stateObs.getMovablePositions(avatarPosition);
        ArrayList<Observation>[] alObses = npcPositions;
        if (alObses != null) {
            System.out.print("alObeses len: ");
            System.out.println(alObses.length);
            for (ArrayList<Observation> al: alObses) {
                System.out.println(al);
            }
        }
        System.out.println("======================++++++++======================");
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

//        this.printInfo(stateObs);

        // INITIALISE POPULATION
        if (this.isFirstTime) {
            this.init_pop(stateObs);
            this.isFirstTime = false;
        } else {
            for (int i = 0; i < this.indNum; i++) {
                this.population[i].actions.remove(0);
                this.population[i].actions.add(this.randomGenerator.nextInt(numActions));
                this.evaluate(this.population[i], stateObs);
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
        }

        // RUN EVOLUTION
        this.globalRemaining = this.timer.remainingTimeMillis();
        while (this.globalRemaining > this.avgTimeTaken && this.globalRemaining > BREAK_MS && this.keepIterating) {
            runIteration(stateObs);
            this.globalRemaining = this.timer.remainingTimeMillis();
        }


        // RETURN ACTION
        Types.ACTIONS action = get_best_action(this.population);
        return action;
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(StateObservation stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        if (this.indNum > 1) {
            int curIndNum = ELITISM;
            for (int i = ELITISM; i < POPULATION_SIZE; i++) {
                if (this.globalRemaining > 2 * this.avgTimeTakenEval && this.globalRemaining > BREAK_MS) {
                    // if enough time to evaluate one more individual
                    Individual newInd = crossover();
                    newInd.mutate(MUTATION_NUM);

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
            Individual newInd = new Individual(SIMULATION_DEPTH, this.numActions, this.randomGenerator);
            newInd.mutate(MUTATION_NUM);
            this.evaluate(newInd, stateObs);
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
     * @param state - current state, root of rollouts
     * @return - value of last state reached
     */
    private double evaluate(Individual individual, StateObservation state) {

        ElapsedCpuTimer elapsedTimerIterationEval = new ElapsedCpuTimer();

        StateObservation stCopy = state.copy();
        int i;
        double acum = 0, avg;
        for (i = 0; i < SIMULATION_DEPTH; i++) {
            if (!stCopy.isGameOver()) {
                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
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
                    if (ratioGenerator.nextInt(100) < BETTER_PARENT_PROB * 100) {
                        if (betterParentRank <= 0) {
                            i -= 1;
                            continue;
                        }
                        int idx = this.randomGenerator.nextInt(betterParentRank);
                        tournament[i] = popList.get(idx);
                        popList.remove(idx);
                        betterParentRank -= 1;
                        lastIndIdx -= 1;
                    } else {
                        if (lastIndIdx <= betterParentRank) {
                            i -= 1;
                            continue;
                        }
                        int idx = this.randomGenerator.nextInt(lastIndIdx - betterParentRank) + betterParentRank;
                        tournament[i] = popList.get(idx);
                        popList.remove(idx);
                        lastIndIdx -= 1;
                    }
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
        this.evaluate(newInd, stateObs);
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
                this.evaluate(this.population[i], stateObs);
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

    private int recognizeGameType(StateObservation stateObs) {
        int blockSize = stateObs.getBlockSize();
        Dimension wd = stateObs.getWorldDimension();
        if (blockSize== 24 && wd.width == 480 && wd.height == 216) return BM;
        else if (blockSize == 38 && wd.width == 798 && wd.height == 342) return  JAWS;
        else if (blockSize == 24 && wd.width == 240 && wd.height == 360) return  RF;
        else if (blockSize == 36 && wd.width == 792 && wd.height == 504) return SM;
        return -1;
    }
}
