package tracks.singlePlayer.advanced.myAdaptiveRHEA;

import java.util.Random;

public class Tuner {
    protected boolean doCrossover;
    private double doCrossProb;
    protected boolean doMutation;
    private double doMutProb;
    protected int crossoverType;
    private double remainCrossTypeProb;
    protected int mutationNum;
    private double remainMutNumProb;

    private Random gen;
    private int indLen;
    private boolean changeParams;

    public Tuner (int indLen) {
        this.gen = new Random();
        this.doCrossover = this.gen.nextBoolean();
        this.doCrossProb = 0.8; //this.gen.nextDouble();
        this.doMutation = this.gen.nextBoolean();
        this.doMutProb = 0.8; //this.gen.nextDouble();
        this.crossoverType = this.gen.nextInt(3);
        this.remainCrossTypeProb = 0.7; //this.gen.nextDouble();
        this.mutationNum = 1;
        this.remainMutNumProb = 0.7; //this.gen.nextDouble();

        this.indLen = indLen;
        this.changeParams = true;
    }

    public void assignParams() {
        if (this.changeParams) {
            if (this.gen.nextDouble() > this.doCrossProb) {
                this.doCrossover = false;
            } else if (this.gen.nextDouble() <= this.doCrossProb){
                this.doCrossover = true;
                if (this.gen.nextDouble() > this.remainCrossTypeProb) {
                    this.crossoverType = this.gen.nextInt(3);
                }
            }

            if (this.gen.nextDouble() > this.doMutProb) {
                this.doMutation = false;
            } else if (this.gen.nextDouble() <= this.doMutProb){
                this.doMutation = true;
                if (this.gen.nextDouble() > this.remainMutNumProb) {
                    this.mutationNum = this.gen.nextInt(this.indLen);
                }
            }
        }

    }

    public void updateProb(double fitnessDiff) {
        if (fitnessDiff > 0) {
            this.changeParams = false;
        } else if (fitnessDiff == 0) {
            this.changeParams = true;

        } else if (fitnessDiff < 0) {
            this.changeParams = true;
            this.doCrossProb = this.clip(this.doCrossProb + this.gen.nextDouble() / 10 * (2 * this.gen.nextInt(2) - 1));
            this.doMutProb = this.clip(this.doMutProb + this.gen.nextDouble() / 10 * (2 * this.gen.nextInt(2) - 1));
            this.remainCrossTypeProb = this.clip(this.remainCrossTypeProb + this.gen.nextDouble() / 10 * (2 * this.gen.nextInt(2) - 1));
            this.remainMutNumProb = this.clip(this.remainMutNumProb + this.gen.nextDouble() / 10 * (2 * this.gen.nextInt(2) - 1));
        }

    }

    private double clip(double prob) {
        if (prob < 0.3) {
            return  0.3;
        } else if (prob > 1.0) {
            return 0.99;
        }

        return prob;

    }




}
