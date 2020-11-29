package tracks.singlePlayer.advanced.myRHEA;

import java.util.ArrayList;
import java.util.Random;

public class Individual implements Comparable{

    protected ArrayList<Integer> actions;
    private int nLegalActions; // number of legal actions
    protected double value;
    private Random gen;

    Individual(int L, int nLegalActions, Random gen) {
        this.actions = new ArrayList<>(L);
        for (int i = 0; i < L; i++) {
            this.actions.add(gen.nextInt(nLegalActions));
        }
        this.nLegalActions = nLegalActions;
        this.gen = gen;
    }


    /**
     * Returns new individual
     * @param MUT - number of genes to mutate
     * @return - new individual, mutated from this
     */
    public void mutate(int MUT) {
        Random mutProbGenerator = new Random();
        if (mutProbGenerator.nextFloat() < Agent.MUT_PROB) {
            // 以 Agent.mutProb 的概率发生突变
            int count = 0;
            if (this.nLegalActions > 1) { // make sure you can actually mutate
                while (count < MUT) {
                    int idx = this.gen.nextInt(this.actions.size()); // index of action to mutate, random mutation of one action

                    int newAction = this.gen.nextInt(this.nLegalActions); // find new action
                    this.actions.set(idx, newAction);
                    count++;
                }
            }
        }
    }

    public Individual copy() {
        Individual newInd = new Individual(this.actions.size(), this.nLegalActions, this.gen);
        newInd.value = this.value;
        for (int i = 0; i < this.actions.size(); i++) {
            newInd.actions.set(i, this.actions.get(i));
        }

        return newInd;
    }

    /**
     * Modifies individual
     * @param CROSSOVER_TYPE - type of crossover
     */
    public void crossover (Individual parent1, Individual parent2, int CROSSOVER_TYPE) {

        if (CROSSOVER_TYPE == Agent.POINT1_CROSS) {
            // 1-point
            int p = this.gen.nextInt(this.actions.size() - (this.gen.nextInt((int)Math.floor(this.actions.size() / 2)))) + 1;
            for ( int i = 0; i < actions.size(); i++) {
                if (i < p)
                    this.actions.set(i, parent1.actions.get(i));
                else
                    this.actions.set(i, parent2.actions.get(i));
            }

        } else if (CROSSOVER_TYPE == Agent.UNIFORM_CROSS) {
            // uniform
            for (int i = 0; i < this.actions.size(); i++) {
                if (this.gen.nextFloat() >= 0.5)
                    this.actions.set(i, parent1.actions.get(i));
                else
                    this.actions.set(i, parent2.actions.get(i));
            }
        } else if (CROSSOVER_TYPE == Agent.POINT2_CROSS) {
            int m1 = this.gen.nextInt((int)Math.floor(this.actions.size() / 3));
            int m2 = this.gen.nextInt((int)Math.ceil(this.actions.size() / 3)) + (int)Math.floor(this.actions.size() / 3);

            for (int i = 0; i < this.actions.size(); i++) {
                if (i <= m1) {
                    this.actions.set(i, parent1.actions.get(i));
                } else if (m1 < i && i < m2) {
                    this.actions.set(i, parent2.actions.get(i));
                } else if (i >= m2) {
                    this.actions.set(i, parent1.actions.get(i));
                }
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        Individual a = this;
        Individual b = (Individual)o;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Individual)) return false;

        Individual a = this;
        Individual b = (Individual)o;

        for (int i = 0; i < this.actions.size(); i++) {
            if (a.actions.get(i) != b.actions.get(i)) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("" + this.value + ": ");
        for (int action : this.actions) s.append(action).append(" ");
        return s.toString();
    }
}
