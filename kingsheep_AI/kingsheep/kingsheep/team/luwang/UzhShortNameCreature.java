package kingsheep.team.luwang;

import kingsheep.Creature;
import kingsheep.Simulator;
import kingsheep.Type;
import java.util.*;
// import java.util.concurrent.SynchronousQueue;

public abstract class UzhShortNameCreature extends Creature {

    protected HashMap<String, Square> visitedSquares;
    private ArrayList<Square> squareQueueToAdd;
    protected Type map[][];
    protected Type objective[];
    static int mapSizeX = 19;
    static int mapSizeY = 15;
    Map<Type, Integer> scoreMap;
    int minThreatDistance = 4;
    protected final Map<MapPoint, Square> nodes = new HashMap<MapPoint, Square>();
    protected Queue<Square> squareQueue;

    public UzhShortNameCreature(Type type, Simulator parent, int playerID, int x, int y) {
        super(type, parent, playerID, x, y);
        visitedSquares = new HashMap<>();
        squareQueue = new PriorityQueue<Square>(new scoreComparator());
        squareQueueToAdd = new ArrayList<Square>();
        scoreMap = new HashMap<>();
        scoreMap.put(Type.RHUBARB, 5);
        scoreMap.put(Type.GRASS, 1);
        scoreMap.put(Type.WOLF2, -10);
        scoreMap.put(Type.EMPTY, 0);
        scoreMap.put(Type.SHEEP1, 0);
        scoreMap.put(Type.SHEEP2, 0);
        scoreMap.put(Type.FENCE, 0);
        scoreMap.put(Type.WOLF1, 0);
    }

    private class scoreComparator implements Comparator<Square> {
        @Override
        public int compare(Square s1, Square s2) {
            return Integer.compare(s2.score, s1.score); // reverse the order in priority queue
        }
    }

    private final Comparator fValueComparator = new Comparator<Square>() {
        public int compare(Square a, Square b) {
            return Integer.compare(a.getFValue(), b.getFValue()); //ascending to get the lowest
        }
    };

    public String getNickname(){
        // change this to any nickname you like. This should not be your luwang. That way you can stay anonymous on the ranking list.
        return "wangluyi";
    }

    protected void setupAstar() {
        for (int x = 0; x < mapSizeX; x++) {
            for (int y = 0; y < mapSizeY; y++) {
                MapPoint point = new MapPoint(x, y);
                Square tempSquare = new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x]));
                if (    map[y][x]==Type.FENCE
                        || map[y][x]==Type.WOLF1
//                      || map[y][x]==Type.SHEEP1
                        || map[y][x]==Type.SHEEP2
                        || map[y][x]==Type.WOLF2  ) {
                    tempSquare.isNotAccessible = true;
                }
                this.nodes.put(point, tempSquare);
            }
        }
    }


    public ArrayList<MapPoint> calculateAStarNoTerrain(MapPoint x_y1, MapPoint x_y2) {
        List<Square> openList = new ArrayList<Square>();
        List<Square> closedList = new ArrayList<Square>();
        Square destNode = null;
        Square currentNode = null;
        for (MapPoint eachPoint: this.nodes.keySet()) {
            if (eachPoint.x == x_y2.x && eachPoint.y == x_y2.y) {
                destNode = this.nodes.get(eachPoint);
                break;
            }
        }
        for (MapPoint eachPoint: this.nodes.keySet()) {
            if (eachPoint.x==x_y1.x && eachPoint.y==x_y1.y) {
                currentNode = this.nodes.get(eachPoint);
                break;
            }
        }

        currentNode.gotHereFrom = null;
        currentNode.setGValue(0);
        openList.add(currentNode);

        while(!openList.isEmpty()) {
            Collections.sort(openList, this.fValueComparator);
            currentNode = openList.get(0); // index of the element to return

            openList.remove(currentNode);
            closedList.add(currentNode);

            if (currentNode.getStringCoordinate().equals(destNode.getStringCoordinate())) {
                return this.calculatePath(destNode);
            }

            int[][] mapDirection = new int[][] {
                    {1,0},{-1,0},{0,1},{0,-1}
            };

            for (int index=0;index<mapDirection.length;index++) {
                MapPoint adjPoint = new MapPoint
                        (currentNode.getXCoordinate() + mapDirection[index][0],
                        currentNode.getYCoordinate() + mapDirection[index][1]);
                if (!this.isInsideBounds(adjPoint)) {
                    continue;
                }
                Square adjNode = null;
                for (MapPoint p: this.nodes.keySet()) {
                    if (adjPoint.x==p.x && adjPoint.y==p.y) {
                        adjNode = this.nodes.get(p);
                        break;
                    }
                }
                if (adjNode.isNotAccessible) {
                    continue;
                }
                if (!closedList.contains(adjNode)) {
                    if (!openList.contains(adjNode)) {
                        adjNode.gotHereFrom = currentNode;
                        adjNode.calculateGValue(currentNode);
                        adjNode.calculateHValue(destNode);
                        openList.add(adjNode);
                    } else { // ajdNode is in openList
                        if (adjNode.gValue > currentNode.gValue) {
                            adjNode.gotHereFrom = currentNode;
                            adjNode.calculateGValue(currentNode);
                        }
                    }
                }
            }
        }
        return null;
    }

    private ArrayList<MapPoint> calculatePath(Square destinationNode) { // path from D to Origin, include the destination, not the origin
        ArrayList<MapPoint> path = new ArrayList<MapPoint>();
        Square node = destinationNode;
        while (node.gotHereFrom != null) {
            path.add(new MapPoint(node.getXCoordinate(), node.getYCoordinate()));
            node = node.gotHereFrom; // track back to the origin square
        }
        return path;
    }

    private boolean isInsideBounds(MapPoint point) {
        return point.x >= 0 &&
                point.x < mapSizeX &&
                point.y >= 0 &&
                point.y < mapSizeY;
    }


    protected Move getAction(Type map[][], char[] objective) {
        this.map = map;
        this.objective = new Type[objective.length];
        for (int i = 0; i< objective.length; ++i) {
            this.objective[i] = Type.getType(objective[i]);
        }
        Square root = new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x]));
        // use A star search to find food for sheep
        // Queue.element() returns the first element and do not delete it from queue
        Queue<Square> foodSquareQueue = findFood(root);
        if (!foodSquareQueue.isEmpty()) {
            setupAstar();
            Square nearestFood = null;
            int distanceToFood;
            int minDistanceToFood = 99999;
            ArrayList<MapPoint> nearestPathFromFood = null;
            for(Square foodSquare: foodSquareQueue) {
                // need to bypass the fence if blocked by it between the food
                ArrayList<MapPoint> pathFromFood = calculateAStarNoTerrain(new MapPoint(this.x,this.y), new MapPoint(foodSquare.getXCoordinate(), foodSquare.getYCoordinate()));
                if (pathFromFood==null)
                    continue;
                distanceToFood = pathFromFood.size();
                if (distanceToFood < minDistanceToFood) {
                    minDistanceToFood = distanceToFood;
                    nearestFood = foodSquare;
                    nearestPathFromFood = new ArrayList<>(pathFromFood);
                } // eat the nearest food, no matter score 1 or 5 points, need improve
            }
            if (nearestFood != null) {
                if (nearestPathFromFood.size()!=0) {
                    MapPoint nextMapPoint = nearestPathFromFood.get(nearestPathFromFood.size() - 1);
                    return getMoveFromOD(new MapPoint(root.getXCoordinate(),root.getYCoordinate()), nextMapPoint);
                }
            }
        }

        return Move.WAIT;
    }

    protected Move getMoveFromOD(MapPoint origin, MapPoint desti) {
        int dX = desti.x - origin.x;
        int dY = desti.y - origin.y;
        if (dX==1 && dY==0) {
            return Move.RIGHT;
        }
        if (dX==-1 && dY==0) {
            return Move.LEFT;
        }
        if (dX==0 && dY==1) {
            return Move.DOWN;
        }
        if (dX==0 && dY==-1) {
            return Move.UP;
        }
        return null;
    }

    protected Square find(Type type) {
        for(int y=0;y < map.length; y++) {
            for(int x=0; x < map[y].length; x++) {
                if(map[y][x] == type) {
                    return new Square(type, x, y, this.objective, null, null, scoreMap.get(type));
                }
            }
        }
        return null;
    }

    protected Queue<Square> findFood(Square root) {
        Queue<Square> foodSquare = new LinkedList<Square>();
        //search to the top
        for(int i=root.y; i >= 0; --i) {
            for(int j=root.x; j >=0; --j) { // to the left
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS) {
                    if (j!=root.x) {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.LEFT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.UP,null, scoreMap.get(map[i][j])));
                    }
                }
            }
            for(int j=root.x; j < map[i].length; j++) { // to the right
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS) {
                    if (j!=root.x) {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.RIGHT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.UP,null, scoreMap.get(map[i][j])));
                    }
                }
            }
        }

        for(int i=root.y; i < map.length; ++i) { // search to the bottom
            for(int j=root.x; j >=0; --j) {
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS) {
                    if (j!=root.x) {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.LEFT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.DOWN,null, scoreMap.get(map[i][j])));
                    }
                }
            }
            for(int j=root.x; j < map[i].length; j++) {
                if(map[i][j] == Type.RHUBARB || map[i][j] == Type.GRASS) {
                    if (j!=root.x) {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.RIGHT,null, scoreMap.get(map[i][j])));
                    } else {
                        foodSquare.add(new Square(map[i][j], j, i, this.objective, Move.DOWN,null, scoreMap.get(map[i][j])));
                    }
                }
            }
        }
        return foodSquare;
    }

    protected Square findOtherSheep(Square root, Type targetType) {
        for(int i=0;i < map.length; i++) {
            for(int j=0; j < map[i].length; j++) {
                if(map[i][j] == targetType) {
                    return new Square(targetType, j, i, root.objective, null, null, scoreMap.get(map[i][j]));
                }
            }
        }
        return null;
    }

    Move fleeOrTrap(Type map[][], Type threatType) { // need improve for trapping the other sheep
        this.map = map;
        Square root = new Square(map[y][x], x, y, this.objective, null, null, scoreMap.get(map[y][x]));
        Square threat  = find(threatType);
        int distance = getDistance(root, threat);
        if(distance < minThreatDistance) {
            planNextMoveAway(root, threat);
        }
        if(!squareQueue.isEmpty()) {
            Move move = squareQueue.poll().howToGetHere;
            squareQueue.clear();
            return move;
        }
        return Move.WAIT;
    }

    protected void planNextMove(Square root, Square s) { // heuristic search for routine
        if(s != null) {
            if(s.y == y && s.x > x) {
                tryHeuristicMoves(root, s, Move.RIGHT, Move.UP, Move.DOWN, Move.LEFT);
            }else if(s.y == y && s.x < x) {
                tryHeuristicMoves(root, s, Move.LEFT, Move.UP, Move.DOWN, Move.RIGHT);
            }else if(s.y < y && s.x == x){
                tryHeuristicMoves(root, s, Move.UP, Move.LEFT, Move.RIGHT, Move.DOWN);
            }else if(s.y > y && s.x == x) {
                tryHeuristicMoves(root, s, Move.DOWN, Move.LEFT, Move.RIGHT, Move.UP);
            }else if(s.y > y && s.x > x) {
                tryHeuristicMoves(root, s, Move.DOWN, Move.RIGHT, Move.UP, Move.LEFT);
            }else if(s.y > y && s.x < x) {
                tryHeuristicMoves(root, s, Move.DOWN, Move.LEFT, Move.UP, Move.RIGHT);
            }else if(s.y < y && s.x < x) {
                tryHeuristicMoves(root, s, Move.UP, Move.LEFT, Move.DOWN, Move.RIGHT);
            }else if(s.y < y && s.x > x) {
                tryHeuristicMoves(root, s, Move.UP, Move.RIGHT, Move.DOWN, Move.LEFT);
            }
        }
    }

    protected void planNextMoveAway(Square root, Square destination) { // try to move away from destination (threat)
        if(destination != null) {
            if(destination.y == y && destination.x > x) {
                tryHeuristicMovesAway(root, destination, Move.LEFT, Move.UP, Move.DOWN, Move.RIGHT);
            }else if(destination.y == y && destination.x < x) {
                tryHeuristicMovesAway(root, destination, Move.RIGHT, Move.UP, Move.DOWN, Move.LEFT);
            }else if(destination.y < y && destination.x == x){
                tryHeuristicMovesAway(root, destination, Move.DOWN, Move.LEFT, Move.RIGHT, Move.UP);
            }else if(destination.y > y && destination.x == x) {
                tryHeuristicMovesAway(root, destination, Move.UP, Move.LEFT, Move.RIGHT, Move.DOWN);
            }

            else if(destination.y > y && destination.x > x) {
                tryHeuristicMovesAway(root, destination, Move.UP, Move.LEFT, Move.DOWN, Move.RIGHT);
            }else if(destination.y > y && destination.x < x) {
                tryHeuristicMovesAway(root, destination, Move.UP, Move.RIGHT, Move.DOWN, Move.LEFT);
            }else if(destination.y < y && destination.x < x) {
                tryHeuristicMovesAway(root, destination, Move.DOWN, Move.RIGHT, Move.UP, Move.LEFT);
            }else if(destination.y < y && destination.x > x) {
                tryHeuristicMovesAway(root, destination, Move.DOWN, Move.LEFT, Move.UP, Move.RIGHT);
            }
        }
    }

    protected int deltaX(Move m) {
        if (m == Move.UP) {
            return 0;
        }
        else if (m == Move.DOWN) {
            return 0;
        }
        else if (m == Move.LEFT) {
            return -1;
        }
        else if (m == Move.RIGHT) {
            return 1;
        }
        return 0;
    }

    protected int deltaY(Move m) {
        if (m == Move.UP) {
            return -1;
        }
        else if (m == Move.DOWN) {
            return 1;
        }
        else if (m == Move.LEFT) {
            return 0;
        }
        else if (m == Move.RIGHT) {
            return 0;
        }
        return 0;
    }

    private void tryHeuristicMoves(Square root, Square destination, Move m1, Move m2, Move m3, Move m4) {
        int newX = x + deltaX(m1);
        int newY = y + deltaY(m1);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m1, root, destination.score));
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
        newX = x + deltaX(m2);
        newY = y + deltaY(m2);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m2, root, destination.score));
        } catch (ArrayIndexOutOfBoundsException e) {
        // continue
        }
        newX = x + deltaX(m3);
        newY = y + deltaY(m3);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m3, root, destination.score));
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
        newX = x + deltaX(m4);
        newY = y + deltaY(m4);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m4, root, destination.score));
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
    }

    private void tryHeuristicMovesAway(Square root, Square destination, Move m1, Move m2, Move m3, Move m4) {
        int newX = x + deltaX(m1);
        int newY = y + deltaY(m1);
        int score = -1;
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m1, root, score));
            squareQueueToAdd.add(new Square(map[newY][newX], newX, newY, objective, m1, root, score));
            score -= 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
        newX = x + deltaX(m2);
        newY = y + deltaY(m2);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m2, root, score));
            squareQueueToAdd.add(new Square(map[newY][newX], newX, newY, objective, m2, root, score));
            score -= 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
        newX = x + deltaX(m3);
        newY = y + deltaY(m3);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m3, root, score));
            squareQueueToAdd.add(new Square(map[newY][newX], newX, newY, objective, m3, root, score));
            score -= 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
        newX = x + deltaX(m4);
        newY = y + deltaY(m4);
        try {
            addToSquareQueueIfFree(new Square(map[newY][newX], newX, newY, objective, m4, root, score));
            squareQueueToAdd.add(new Square(map[newY][newX], newX, newY, objective, m4, root, score));
            score -= 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            // continue
        }
    }

    protected int getDistance(Square s1, Square s2) {
        return Math.abs(s1.x - s2.x) + Math.abs(s1.y - s2.y);
    }

    private void addToSquareQueueIfFree(Square s) {
        if (map[y][x] == Type.SHEEP1) {
            if (s.type != Type.FENCE && s.type != Type.WOLF2 &&
                    s.type != Type.SHEEP2 && s.type != Type.WOLF1) {
                try {
                    squareQueue.add(s);
                } catch (Exception e) {
                    // continue
                }
            }
        } else { //in wolf case
            if (s.type != Type.FENCE &&
                    s.type != Type.WOLF2 &&
                    s.type != Type.SHEEP1) {
                try {
                    squareQueue.add(s);
                } catch (Exception e) {
                    // continue
                }
            }
        }
    }

    protected class MapPoint {
        int x,y;

        protected MapPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private String getStringCoordinate() {
            return Integer.toString(x) + "_" + Integer.toString(y);
        }
    }

    protected class Square {
        int x, y;
        protected Type type;
        boolean visited;
        private Type objective[];
        protected Move howToGetHere;
        protected Square gotHereFrom;
        int score;
        public int gValue; //distance cost from start
        public int hValue; //distance from target
        public int fValue;
        public boolean isNotAccessible = false;
        private final int MOVEMENT_COST = 1;

        protected Square(Type type, int x, int y, Type objective[], Move howToGetHere, Square gotHereFrom, int score) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.visited = false;
            this.objective = objective;
            this.howToGetHere = howToGetHere;
            this.gotHereFrom = gotHereFrom;
            this.score = score;
        }

        public void setGValue(int amount) {
            this.gValue = amount;
        }

        public void calculateHValue(Square destPoint) {
            this.hValue = (Math.abs(this.x - destPoint.x) + Math.abs(this.y - destPoint.y));
        }

        public void calculateGValue(Square point) {
            this.gValue = point.gValue + this.MOVEMENT_COST;
        }

        public int getFValue() {
            return this.gValue + this.hValue;
        }

        private void setScore(int myVal) {
            this.score = myVal;
        }


        private boolean isSquareVisitable() {
            if (type == Type.FENCE){
                return false;
            }
            if (visitedSquares.get(getStringCoordinate()) != null){
                return false;
            }
            return true;
        }

        private String getStringCoordinate() {
            return Integer.toString(x) + "_" + Integer.toString(y);
        }

        protected int getXCoordinate() {
            return x;
        }
        protected int getYCoordinate() {
            return y;
        }

    }
}
