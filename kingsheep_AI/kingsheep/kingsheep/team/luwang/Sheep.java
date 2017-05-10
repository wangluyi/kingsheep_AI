package kingsheep.team.luwang;

import kingsheep.*;
import java.util.Queue;


public class Sheep extends UzhShortNameCreature {

    protected Move previousMove;
    protected int countWithinThreat = 0;

    public Sheep(Type type, Simulator parent, int playerID, int x, int y) {
        super(type, parent, playerID, x, y);
    }


    protected void think(Type map[][]) {
		/*
		TODO
		YOUR SHEEP CODE HERE
		BASE YOUR LOGIC ON THE INFORMATION FROM THE ARGUMENT map[][]
		
		YOUR CODE NEED TO BE DETERMINISTIC. 
		THAT MEANS, GIVEN A DETERMINISTIC OPPONENT AND MAP THE ACTIONS OF YOUR SHEEP HAVE TO BE REPRODUCIBLE
		
		SET THE MOVE VARIABLE TO ONE TOF THE 5 VALUES
        move = Move.UP;
        move = Move.DOWN;
        move = Move.LEFT;
        move = Move.RIGHT;
        move = Move.WAIT;
		*/
        char[] objectives = new char[2];
        objectives[0] = 'r';
        objectives[1] = 'g';

        this.map = map;
        this.objective = new Type[objectives.length];
        for (int i = 0; i< objectives.length; ++i) {
            this.objective[i] = Type.getType(objectives[i]);
        }
        Square root = new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x]));
        Square wolf = find(Type.WOLF2);

        int distanceFromWolf = getDistance(root, wolf);
        if (distanceFromWolf < minThreatDistance) {
            countWithinThreat += 1;
            planNextMoveAway(root, wolf);
            if (squareQueue.size()!=0) {
                move = squareQueue.poll().howToGetHere;  // poll() returns the first element and remove it
                previousMove = move;
                squareQueue.clear();
            }
            else {
                move = Move.WAIT;
            }
        }
        else if (countWithinThreat > 0) {
            // get away from wolf and do not always head to nearest food
            countWithinThreat = 0;
            planNextMoveAway(root, wolf);
            if (squareQueue.size()!=0) {
                move = squareQueue.poll().howToGetHere;
                squareQueue.clear();
            }
            else {
                move = Move.WAIT;
            }
        }
        else {
            move = getAction(map, objectives);
        }

        Queue<Square> foodSquareQueue = findFood(new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x])));
        if (foodSquareQueue.size() == 0) {
            move = fleeOrTrap(map, Type.WOLF2);
        }
    }
}