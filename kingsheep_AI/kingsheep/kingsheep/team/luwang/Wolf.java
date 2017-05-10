package kingsheep.team.luwang;

import kingsheep.*;
import java.util.*;

public class Wolf extends UzhShortNameCreature {

    public Wolf(Type type, Simulator parent, int playerID, int x, int y) {
        super(type, parent, playerID, x, y);
    }

    protected void think(Type map[][]) {
		/*
		TODO

		BASE YOUR LOGIC ON THE INFORMATION FROM THE ARGUMENT map[][]
		
		YOUR CODE NEED TO BE DETERMINISTIC. 
		THAT MEANS, GIVEN A DETERMINISTIC OPPONENT AND MAP THE ACTIONS OF YOUR WOLF HAVE TO BE REPRODUCIBLE
		
		SET THE MOVE VARIABLE TO ONE TOF THE 5 VALUES
        move = Move.UP;
        move = Move.DOWN;
        move = Move.LEFT;
        move = Move.RIGHT;
        move = Move.WAIT;
		*/

        char[] objectives = new char[1];
        objectives[0] = '3';
        move = getWolfAction(map, objectives);

        if (move == null) {
            move = Move.WAIT;
        }
    }

    private Move getWolfAction(Type[][] map, char[] objectives) {
        this.map = map;
        Square root = new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x]));

        this.objective = new Type[objectives.length];
        for (int i = 0; i< objectives.length; ++i) {
            this.objective[i] = Type.getType(objectives[i]);
        }
        Square theOtherSheep = find(Type.SHEEP2);

        Queue<Square> foodAndSheepSquareQueue = findFoodAndSheepSquareQueue(root);
        if (!foodAndSheepSquareQueue.isEmpty()) {
            setupAstarForWolf();
            Square nearestGoal = null;
            int distanceToGoal;
            int minDistanceToGoal = 99999;
            ArrayList<MapPoint> nearestPathFromGoal = null;
            for(Square goalSquare: foodAndSheepSquareQueue) {
                ArrayList<MapPoint> pathFromGoal = calculateAStarNoTerrain(new MapPoint(this.x,this.y), new MapPoint(goalSquare.getXCoordinate(), goalSquare.getYCoordinate()));
                if (pathFromGoal == null)
                    continue;
                distanceToGoal = pathFromGoal.size();
                if (distanceToGoal < minDistanceToGoal) {
                    minDistanceToGoal = distanceToGoal;
                    nearestGoal = goalSquare;
                    nearestPathFromGoal = new ArrayList<>(pathFromGoal);
                } // get to the nearest goal, no matter score 1 or 5
            }
            if (nearestGoal != null) {
                if (nearestPathFromGoal.size() != 0) {
                    MapPoint nextMapPoint = nearestPathFromGoal.get(nearestPathFromGoal.size() - 1);
                    return getMoveFromOD(new MapPoint(root.getXCoordinate(),root.getYCoordinate()), nextMapPoint);
                }
            }
            else { // wolf1 is blocked by fence or sheep1, so follow sheep1. sheep1 and wolf1 are family
                Square sheep1 = findSheep1(root, Type.SHEEP1);
                ArrayList<MapPoint> pathFromSheep1 = calculateAStarNoTerrain(new MapPoint(this.x,this.y), new MapPoint(sheep1.getXCoordinate(), sheep1.getYCoordinate()));
                if (pathFromSheep1 != null && pathFromSheep1.size() != 0) {
                    MapPoint nextMapPoint = pathFromSheep1.get(pathFromSheep1.size() - 1);
                    return getMoveFromOD(new MapPoint(root.getXCoordinate(),root.getYCoordinate()), nextMapPoint);
                }
            }
        }
        return Move.WAIT;
    }

    protected Queue<Square> findFoodAndSheepSquareQueue(Square root) {

        Queue<Square> foodAndSheepSquareQueue = new LinkedList<Square>();
        for(int i=root.y; i >= 0; --i) { //search to the top
            for(int j=root.x; j >=0; --j) { // to the left
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS || map[i][j] == Type.SHEEP2) {
                    if (j!=root.x) {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.LEFT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.UP,null, scoreMap.get(map[i][j])));
                    }
                }
            }
            for(int j=root.x; j < map[i].length; j++) { // to the right
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS || map[i][j] == Type.SHEEP2) {
                    if (j!=root.x) {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.RIGHT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.UP,null, scoreMap.get(map[i][j])));
                    }
                }
            }
        }

        for(int i=root.y; i < map.length; ++i) { // search to the bottom
            for(int j=root.x; j >=0; --j) {
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS || map[i][j] == Type.SHEEP2) {
                    if (j!=root.x) {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.LEFT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.DOWN,null, scoreMap.get(map[i][j])));
                    }
                }
            }
            for(int j=root.x; j < map[i].length; j++) {
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS || map[i][j] == Type.SHEEP2) {
                    if (j!=root.x) {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.RIGHT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodAndSheepSquareQueue.add(new Square(map[i][j], j, i, this.objective, Move.DOWN,null, scoreMap.get(map[i][j])));
                    }
                }
            }
        }
        return foodAndSheepSquareQueue;
    }

    protected void setupAstarForWolf() { // sheep2 is accessible !!!
        for (int x = 0; x < mapSizeX; x++) {
            for (int y = 0; y < mapSizeY; y++) {
                MapPoint point = new MapPoint(x, y);
                Square tempSquare = new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x]));
                if (    map[y][x]==Type.FENCE
                        || map[y][x]==Type.WOLF2 ) {
                    tempSquare.isNotAccessible = true;
                }
                this.nodes.put(point, tempSquare);
            }
        }
    }

    protected Square findSheep1(Square root, Type targetType) {
        for(int i=0;i < map.length; i++) {
            for(int j=0; j < map[i].length; j++) {
                if(map[i][j] == targetType) {
                    return new Square(targetType, j, i, null, null, null, scoreMap.get(map[i][j]));
                }
            }
        }
        return null;
    }
}
