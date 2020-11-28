package tracks.singlePlayer.advanced.myRHEA;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;

import java.util.ArrayList;
import java.util.HashMap;


public class MyHeuristic extends StateHeuristic {

    private static final double HUGE_NEGATIVE = -1000.0;
    private static final double HUGE_POSITIVE =  1000.0;
    private static int gameType;

    MyHeuristic(int gt){
        this.gameType = gt;
    }

    public double evaluateState(StateObservation stateObs){
        if (this.gameType == Agent.BM) return this.evalBM(stateObs);
        else if (this.gameType == Agent.JAWS) return this.evalJAWS(stateObs);
        else if (this.gameType == Agent.RF) return this.evalRF(stateObs);
        else if (this.gameType == Agent.SM) return this.evalSM(stateObs);

        //else Aliens
        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        double rawScore = stateObs.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        return rawScore;
    }


    /**
     * bomberman(9):  墙块itype=0, 可爆破墙块itype=26, 炸弹category=6, 4个发射方向的itype为22~25, 蝙蝠ityp=6, 蜘蛛itype=7;
     *                格子尺寸24, 总体尺寸(480, 216)
     */
    private double evalBM(StateObservation stateObs) {
//        System.out.println("Bomberman");
        double totScore = 0.0;

        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;
        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        double rawScore = stateObs.getGameScore();

        final double IN_DANGER = -10.0, MAX_DIST = 29.0, DANGER_DIST = 3.0, BOMBER_AREA = -12.0, CLOSE_TO_PORTAL = 5.0;
        final int BLOCK_SIZE = stateObs.getBlockSize();
        Vector2d avatarPos = stateObs.getAvatarPosition();

        Vector2d portalPos = this.getNearestPostionSafe(stateObs.getPortalsPositions(avatarPos), 0);
        double closeDestScore = 0.0;
        if (this.getManhattanDist(avatarPos, portalPos, BLOCK_SIZE) <= 2) {
            closeDestScore = CLOSE_TO_PORTAL;
        }

        Vector2d batPos = this.getNearestPostionSafe(stateObs.getNPCPositions(avatarPos), 0);
        Vector2d spiderPos = this.getNearestPostionSafe(stateObs.getNPCPositions(avatarPos), 1);

        double toKillScore = 0.0;
        if (batPos == null && spiderPos == null) {
            toKillScore = MAX_DIST;
        } else if (batPos == null && spiderPos != null) {
            double spiderDist = this.getManhattanDist(avatarPos, spiderPos, BLOCK_SIZE);
            if (spiderDist <= DANGER_DIST) toKillScore = IN_DANGER;
            else toKillScore = -spiderDist + MAX_DIST;
        } else if (batPos != null && spiderPos == null) {
            double batDist = this.getManhattanDist(avatarPos, batPos, BLOCK_SIZE);
            if (batDist <= DANGER_DIST) toKillScore = IN_DANGER;
            else toKillScore = -batDist + MAX_DIST;
        } else {
            double batDist = this.getManhattanDist(avatarPos, batPos, BLOCK_SIZE),
                   spiderDist = this.getManhattanDist(avatarPos, spiderPos, BLOCK_SIZE);
            if (batDist <= spiderDist)  toKillScore = -batDist + MAX_DIST;
            else toKillScore = -spiderDist + MAX_DIST;
        }


        double dangerAreaScore = 0.0;
        ArrayList<Observation>[] bomberPositions = stateObs.getMovablePositions(avatarPos);
        if (bomberPositions != null) {
            Vector2d bomberPos;
            for (ArrayList<Observation> alObses: bomberPositions) {
                if (alObses.size() > 0) {
                    bomberPos = alObses.get(0).position;
                    if (avatarPos.x == bomberPos.x || avatarPos.y == bomberPos.y) dangerAreaScore = BOMBER_AREA;
                    break;
                }
            }

        }

        totScore = rawScore + toKillScore + dangerAreaScore +closeDestScore;

        return  totScore;
    }

    /**
     *jaws(56): 鲨鱼itype=10 (npc), 普通鱼itype=11(movables), 巢穴itype=5; resources 钻石编号13, 达到20个可以击杀鲨鱼;
     *          格子尺寸38, 总体尺寸(798, 342)
     */
    private double evalJAWS(StateObservation stateObs) {
//        System.out.println("Jaws");
        double totScore = 0.0;

        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;
        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        double rawScore = stateObs.getGameScore();

        final double CLOSE_NEST = -10.0, CLOSE_FISH = -2.0, DANGER_DIST = 2.0, CLOSE_SHARK = -12.0, TO_KILL_SHARK = 15.0;
        final int BLOCK_SIZE = stateObs.getBlockSize();
        Vector2d avatarPos = stateObs.getAvatarPosition();

        double closeNestScore = 0.0;
        if (avatarPos.x == 0.0 || avatarPos.x == 798.0) closeNestScore = CLOSE_NEST;

        double closeFishScore = 0.0;
        Vector2d fishPos = this.getNearestPostionSafe(stateObs.getMovablePositions(avatarPos), 0);
        if (fishPos != null) {
            double fishDist = this.getManhattanDist(avatarPos, fishPos, BLOCK_SIZE);
            if (fishDist <= DANGER_DIST) closeFishScore = CLOSE_FISH;
        }

        double sharkScore = 0.0;
        Vector2d sharkPos = this.getNearestPostionSafe(stateObs.getNPCPositions(avatarPos), 0);
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources();
        if (sharkPos != null) {
            double sharkDist = this.getManhattanDist(avatarPos, sharkPos, BLOCK_SIZE);
            if (resources.size() > 0 && resources.get(13) >= 20) {
                if (sharkDist <= DANGER_DIST) {
                    sharkScore = TO_KILL_SHARK;
                }
            } else {
                if (sharkDist <= DANGER_DIST) {
                    sharkScore = CLOSE_SHARK;
                }
            }
        }

        totScore = rawScore + closeFishScore + sharkScore + closeNestScore;

        return  totScore;
    }

    /**
     * roadfighter(80): 无npc, 无portal; 绿车itype=14, 蓝车itype=13, 补给itype=15 (都是immovables, 补给是第三个数组列表); 最左路纵坐标24.0,
     *                最右路横坐标192; 格子尺寸24, 总体尺寸(240, 360); 一个补给10点hp
     */
    private double evalRF(StateObservation stateObs) {
//        System.out.println("Roadfighter");
        double totScore = 0.0;

        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        double rawScore = stateObs.getGameScore();

        final double TO_GET_SUPPLY = 10.0, IN_DANGER = -6.0;
        final int BLOCK_SIZE = stateObs.getBlockSize();
        Vector2d avatarPos = stateObs.getAvatarPosition();

        ArrayList<Observation>[] immovables = stateObs.getImmovablePositions(avatarPos);
        double collisionScore = 0.0;
        double blueDist = this.getEuclideanDistSafe(immovables, 0);
        Vector2d bluePos = this.getNearestPostionSafe(immovables, 0);
        double greenDist = this.getEuclideanDistSafe(immovables, 1);
        Vector2d greenPos = this.getNearestPostionSafe(immovables, 1);

        if (bluePos != null) {
            if (bluePos.x == avatarPos.x && bluePos.y < avatarPos.y &&  avatarPos.y - bluePos.y < 1.5 * BLOCK_SIZE)
                collisionScore += IN_DANGER;
        }

        if (greenPos != null) {
            if (greenPos.x == avatarPos.x && greenPos.y < avatarPos.y &&  avatarPos.y - greenPos.y < 1.5 * BLOCK_SIZE)
                collisionScore += IN_DANGER;
        }
        double supplyScore = 0.0;
        double supplyDist = this.getEuclideanDistSafe(immovables, 2);
        Vector2d supplyPos = this.getNearestPostionSafe(immovables, 2);
        if (supplyPos != null) {
            if (avatarPos.x == supplyPos.x &&
                ((blueDist >= 1.5 * BLOCK_SIZE && bluePos.y < supplyPos.y && supplyDist <= 1.2 * blueDist)
                    || (greenDist >= 1.5 * BLOCK_SIZE && greenPos.y < supplyPos.y && supplyDist <= 1.2 * greenDist))
                ) {
                supplyScore = TO_GET_SUPPLY;
            }
        }

        totScore = rawScore + supplyScore + collisionScore;
        return  totScore;
    }

    /**
     * superman(89): 墙块itype=0(immovable, 第1), 云朵itype=3(immovable, 第2), jail层 itype=4(immovable, 第3), jail itype=6(immovable, 第4),
     *               人质itype=16(movable, 第1), 下落的人质itype=18(movable, 第2), 子弹itype=19 (movable, 第3),
     *               杀手左边itype=12((npc, 第2)), 右边itype=11(npc, 第1), 老巢左itype=9(portal, 第2), 老巢右itype=8(portal, 第1);
     *               抓到的杀手, 编号13, 8个后就要放到监狱里面去;
     *               格子尺寸36, 总体尺寸(792, 504)
     */
    private double evalSM(StateObservation stateObs) {

//        System.out.println("Superman");
        double totScore = 0.0;

        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        double rawScore = stateObs.getGameScore();

        final double KILLER_DIST = 2.0, THREAT_DIST = 5.0, NEAR_KILLER = 5.0, NEAR_THREAT = 20.0,
                     JAIL_DIST = 6.0, NEAR_JAIL = 10.0, FALLING_DIST = 12.0;
        final int BLOCK_SIZE = stateObs.getBlockSize();
        Vector2d avatarPos = stateObs.getAvatarPosition();

        Vector2d fallingPos = this.getNearestPostionSafe(stateObs.getMovablePositions(avatarPos), 1);
        if (fallingPos != null && this.getManhattanDist(avatarPos, fallingPos, BLOCK_SIZE) <= FALLING_DIST) {
            return  HUGE_POSITIVE;
        }

        double closeKillerScore = 0.0;
        double threatKillerScore = 0.0;
        double putInJailScore = 0.0;

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);
        Observation threatKiller = null;
        double minY = HUGE_POSITIVE;
        for (ArrayList<Observation> obsesal: npcPositions) {
            for (Observation obs: obsesal) {
                if (obs.position.y < minY) {
                    threatKiller = obs;
                    minY = obs.position.y;
                } else if (obs.position.y == minY) {
                    if (obs.itype == 12) continue;
                }
            }
        }
        if (this.getManhattanDist(avatarPos, threatKiller.position, BLOCK_SIZE) < THREAT_DIST)
            threatKillerScore = NEAR_THREAT;

        HashMap<Integer, Integer> resources = stateObs.getAvatarResources();
        int killerCaughtNum = 0;
        if (resources.size() > 0) {
            killerCaughtNum = resources.get(13);
        }

        Vector2d rightKillerPos = this.getNearestPostionSafe(npcPositions, 0);
        Vector2d leftKillerPos = this.getNearestPostionSafe(npcPositions, 1);

        if (killerCaughtNum < 8) {
            if (rightKillerPos != null && this.getManhattanDist(avatarPos, rightKillerPos, BLOCK_SIZE) <= KILLER_DIST)
                closeKillerScore = NEAR_KILLER;
            if (leftKillerPos != null && this.getManhattanDist(avatarPos, leftKillerPos, BLOCK_SIZE) <= KILLER_DIST)
                closeKillerScore = NEAR_KILLER;
        } else {
            Vector2d jailPos = this.getNearestPostionSafe(stateObs.getImmovablePositions(avatarPos), 3);
            if (this.getManhattanDist(avatarPos, jailPos, BLOCK_SIZE) <= JAIL_DIST)
                putInJailScore = NEAR_JAIL;
        }

        totScore = rawScore + closeKillerScore + putInJailScore + threatKillerScore;

        return  totScore;
    }



    private double getManhattanDist(Vector2d p1, Vector2d p2, int blockSize) {
        if (p1 == null || p2 == null) return HUGE_POSITIVE;
        return (Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y)) / blockSize;
    }

    private double getEuclideanDistSafe(ArrayList<Observation>[] obsesAls, int alIdx) {
        if (obsesAls != null && obsesAls.length >= alIdx + 1) {
            if (obsesAls[alIdx].size() > 0)
                return Math.sqrt(obsesAls[alIdx].get(0).sqDist);
        }
        return -10.0;
    }

    private Vector2d getNearestPostionSafe (ArrayList<Observation>[] obsesAls, int alIdx) {
        if (obsesAls != null && obsesAls.length >= alIdx + 1) {
            if (obsesAls[alIdx].size() > 0)
                return obsesAls[alIdx].get(0).position;
        }
        return null;
    }
}
