package tracks.singlePlayer.tools.human;

import core.game.Game;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.Direction;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by diego on 06/02/14.
 */
public class Agent extends AbstractPlayer
{

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
    }

    private void printInfo (StateObservation stateObs) {
        Vector2d avatarPosition = stateObs.getAvatarPosition();
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPosition);
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPosition);
        ArrayList<Observation>[] immovables = stateObs.getImmovablePositions(avatarPosition);
        ArrayList<Observation>[] movables = stateObs.getMovablePositions(avatarPosition);
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources();

        System.out.println("====================== Info () ======================");
//        System.out.print("World dimension: ");
//        System.out.println(stateObs.getWorldDimension());
//        System.out.print("Block size: ");
//        System.out.println(stateObs.getBlockSize());
        System.out.print("Resources: ");
        System.out.println(resources);
//        System.out.print("Avatar hp: ");
//        System.out.println(stateObs.getAvatarHealthPoints());


//        ArrayList<Observation>[] alObses = portalPositions;
//        if (alObses != null) {
//            System.out.print("alObeses len: ");
//            System.out.println(alObses.length);
//            for (ArrayList<Observation> al: alObses) {
//                System.out.println(al);
//            }
//        }
        System.out.println("======================++++++++======================");
    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        Direction move = Utils.processMovementActionKeys(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX);
        boolean useOn = Utils.processUseKey(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX);

        //In the keycontroller, move has preference.
        Types.ACTIONS action = Types.ACTIONS.fromVector(move);

        //if(action == Types.ACTIONS.ACTION_NIL && useOn)
        if(useOn) //This allows switching to Use when moving.
            action = Types.ACTIONS.ACTION_USE;

//        this.printInfo(stateObs);
        return action;
    }

    public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer)
    {
        //System.out.println("Thanks for playing! " + stateObservation.isAvatarAlive());
    }
}
