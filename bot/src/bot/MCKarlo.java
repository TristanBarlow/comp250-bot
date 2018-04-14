/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.core.AI;
import ai.RandomBiasedAI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;
import ai.portfolio.*;
import ai.minimax.*;
import ai.abstraction.*;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.RandomAI;
import java.util.Map;


/**
 *
 * @author santi
 */
public class MCKarlo extends AIWithComputationBudget implements InterruptibleAI 
{
	EvaluationFunction EvaluationMethod;
	AI BaseAI;

	List<PlayerAction> GoodActions;
	Map<Float, GameState> Plays = new HashMap<Float, GameState>();
	
	int Depth = 10;
	int Breadth = 10;
	int MinPlayer = 1;
	int MaxPlayer =0;
	int RunsThisMove = 0;
	int TimeBudget = 0;
	int Playouts = 0;
	int TotalPlayouts = 0;
	
	MCNode root = null;
	GameState StartGameState;

	boolean ComputationComplete = false;
	
	PlayerAction FinalAction;
	ArrayList<GameState> States;
	
    public MCKarlo(UnitTypeTable utt) 
    {
        this(100,-1, 10,10, new RandomBiasedAI(utt), new SimpleSqrtEvaluationFunction3());
    }

    public MCKarlo(int available_time, int MaxPlayouts, int breadth, int depth, AI AIPolicy, EvaluationFunction a_ef) 
    {
        super(available_time, MaxPlayouts);
        TimeBudget = available_time;
        Playouts = MaxPlayouts;
        Depth = depth;
        Breadth =breadth;
        BaseAI = AIPolicy;
        EvaluationMethod = a_ef;
        States = new ArrayList<GameState>();
        GoodActions = new ArrayList<PlayerAction>();

    }
    
    public final PlayerAction getAction(int player, GameState gs) throws Exception
    {	
    	if(gs.canExecuteAnyAction(player))
    	{
    		startNewComputation(player, gs);
    		computeDuringOneGameFrame();
    		return getBestActionSoFar();
    	}
    	else return new PlayerAction();
    }

	@Override
	public void startNewComputation(int player, GameState gs) throws Exception
	{
		StartGameState = gs;
		root = new MCNode(player, gs.clone(), Depth);
		Playouts =0;
	}

	@Override
	public void computeDuringOneGameFrame() throws Exception
	{
		 	long start = System.currentTimeMillis();
	        int nPlayouts = 0;
	        int numberOfNodes = 0;
	        long cutOffTime = start + TimeBudget;
	        if (TimeBudget<=0) cutOffTime = 0;
	        while(nPlayouts < root.UntriedMoves.size() && nPlayouts < Breadth) 
	        {
	            if (cutOffTime >0 && System.currentTimeMillis() > cutOffTime) break;
    			MCNode node = root;
	            while(node.Depth< Depth)
	            {
	            	node = node.AddChild(Depth);
	            	numberOfNodes ++;
	            }
	            float Eval = 0;
	            while(node != null)
	            {
	            	GameState gs2 = node.GSCopy.clone();
	            	SimulateGame(gs2, gs2.getTime() + 100 );
	            	Eval  =+ EvaluationMethod.evaluate(MaxPlayer, MinPlayer, gs2);
	            	node.Update(Eval);
	            	node = node.ParentNode;
	            }
    			nPlayouts++;
    		}
	        System.out.println(numberOfNodes);
	         TotalPlayouts++;  
        }

	@Override
	public PlayerAction getBestActionSoFar() throws Exception
	{
        if (root.ChildNodes.size() < 1) 
        {
            return new PlayerAction();
        }
        else return root.GetBestMove();
	}

	@Override
	public void reset()
	{
        StartGameState= null;
        root = null;
        RunsThisMove = 0;
	}

	@Override
	public AI clone()
	{
		// TODO Auto-generated method stub
		return new MCKarlo(TimeBudget, Playouts, Breadth, Depth, BaseAI, EvaluationMethod);
	}
	
	@Override
	public List<ParameterSpecification> getParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public void SimulateGame(GameState gs, int time)throws Exception 
	{

        boolean gameover = false;

        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                gs.issue(BaseAI.getAction(0, gs));
                gs.issue(BaseAI.getAction(1, gs));
            }
        }while(!gameover && gs.getTime()<time);   
		
	}  
	public boolean SimulateMove(GameState Sgs, PlayerAction move)throws Exception
	{
		Sgs.issue(move);
		Sgs.issue(BaseAI.getAction(MinPlayer, Sgs));
		Sgs.cycle();
		if(Sgs.isComplete())return true;
		return false;
	}
}