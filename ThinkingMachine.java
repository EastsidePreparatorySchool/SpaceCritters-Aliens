/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stockaliens;

import alieninterfaces.*;

/**
 *
 * @author mkoch
 */
public class ThinkingMachine implements Alien {

    //Current performance is running ~30k aliens by turn 1000, improved from ~20k with a random move function.
    //BTW, from my observation, it is insane how fast these things will breed to fill up a star. I will need to create a migration function.
    Context ctx;
    int state;    //I actually implemented this the patch right before the alien interface added a getState variable :P. beginGame = 0. getSpawn = 1. getTech = 2. beginLife = 3. Planned: goToStar = 4. makeFlower = 5.
    int flavorOffspringEnergy = 15;    //How many energy it will give to its children.
    int flavorSpawning = 5;    //How much energy it will save for itself before spawning.
    int flavorTech = 2;    //How much energy it will save before teching.
    int security;    //How safe and secure it feels about its surroundings. Not yet implemented. When it is, this will go down if there are Daleks nearby. The lower the security, the more energy it will save for itself.
    boolean pacifist = false;    //A relic from when viewing cost energy.
    double energyLastTile = 0;    //Also outdated from when getPresentEnergy didn't work.
    double energyThisTile = 0;
    double lastTurnEnergy = 0;
    double spawnTechConst;    //My function to calculate tile energy compares energy this turn with energy last turn. Teching and spawning wreak havok with this, so I have added a spawnTechConst.
    Direction lastMove;
    static boolean makeFlower = false;
    static Position flowerCenter;    //Might make this a position later.
    static int flowerSpotsFilled = 0;
    int[] flowerSpotOffsets = {};    //I considered making this a function of sine. But that would take a lot of memory. Will need to ask GMein which is more egregious.

    @Override
    public void init(Context cntxt, int i, int i1, String string) {
        ctx = cntxt;
        ctx.debugOut("Initialized at "
                + ctx.getPosition().toString()
                + " E: " + Double.toString(ctx.getEnergy())
                + " T: " + Double.toString(ctx.getTech()));
        state = 0;
        if (ctx.getGameTurn() == 1) {
            state = 3;
        }
    }

    public void checkReturns() {    //Whenever a Thinking Machine techs or spawns, it checks to see what it should do next.
        double energyReturnFromSpawning = 1.0 * ctx.getTech() / ctx.getSpawningCost();
        double energyReturnFromTech = 1.0 / ctx.getTech();    //This doesn't account for the fact that tech will give you returns sooner :/. This may be an existential problem for an alien that relies on breeding quickly.
        if (energyReturnFromSpawning > energyReturnFromTech && ctx.getTech() <= 30) {
            state = 1;
        } else {
            state = 2;
        }
//        if (ctx.getGameTurn() > 500){
//            makeFlower = true;
//            flowerCenter = ctx.getPosition();
//        }
    }
    public void updateEnergy() {    //Probably obsolete.
        energyLastTile = energyThisTile;
        energyThisTile = 30 * (ctx.getEnergy() - lastTurnEnergy + spawnTechConst);    //30 is the current dividing constant. Also, spawning and teching mess this up a bit, so I added a spawnTechConst.
        lastTurnEnergy = ctx.getEnergy();
        spawnTechConst = 0;
    }

    @Override
    public Direction getMove() {
        //   ctx.debugOut("Move requested,"
        //           + " E:" + Double.toString(ctx.getEnergy())
        //           + " T:" + Double.toString(ctx.getTech()));
        int move_energy;
        if (state == 3) {
            if (ctx.getPosition().y > 165) {    //Getting down to Sirius is advantageous for the first alien.
                state = 0;
            } else {
                return new Direction(0, 1);
            }
        }
        if (state == 0) {
            state = 2;
            return moveRandomDirection();
        } else {
            return basicEnergyFind();
        }
    }

    public Direction moveRandomDirection() {
        int viewEnergy = (int) Math.min(ctx.getTech(), ctx.getEnergy());    //I don't know if this is necessary, how much views cost and all. Will figure it out.
        Direction dir = new Direction(0, 0);    //I was doing something different, but it works better with the methods if the ThinkingMachine copy-pastes from the Dalek a lot.
        int i = 0;
        do {
            dir.x = ctx.getRandomInt(3) - 1;
            dir.y = ctx.getRandomInt(3) - 1;
            i++;
        } while ((lastMove.x * -1 == dir.x && lastMove.y * -1 == dir.y) || (dir.x == 0 && dir.y == 0) || i > 10);    //If thou hast moved from a poorer location into a richer one, thou shalt not move back. Thou shalt not be still, for then shalt thou be struck by a planet and crushed.
        //TBH, it would make more sense for an alien to die if it moved head-on into a planet.
        lastMove = dir;
//        ctx.debugOut("Moving (" + Integer.toString(x) + "," + Integer.toString(y) + ")");
//        checkForStupidity(viewEnergy, dir);    //Will need to ask GMein what happens if you move out of the game map. AlienContainer line 198 suggests that it just doesn't move.
        try {
            if (ctx.getView(viewEnergy).getSpaceObjectAtPos(ctx.getPosition().add(dir)) != null) {    //I implemented an isDumbMove function earlier. That one had all of the star locations pre-programmed in. So this might cost more energy, but it is also more legit.
                dir.x = 0;
                dir.y = 0;
            }
        } catch (Exception e) {
            // do something here to deal with errors
            ctx.debugOut("Ayudame!" + e.toString());
        }
        ctx.debugOut(Integer.toString(dir.x) + "X" + Integer.toString(dir.y) + "Y");
        return dir;
    }

    public Direction basicEnergyFind() {
        updateEnergy();
        int viewEnergy = (int) Math.min(ctx.getTech(), ctx.getEnergy());
        Direction dir = new Direction(0, 0);
        if (energyLastTile > energyThisTile) {    //If you moved from a high-energy spot to a low-energy spot, move back
            lastMove.x = lastMove.x * -1;
            lastMove.y = lastMove.y * -1;
            dir = lastMove;
        } else {    //If your current spot is better than your last spot, move in a random direction that is not your last spot.
            return moveRandomDirection();
        }
        // ctx.debugOut("Moving (" + Integer.toString(x) + "," + Integer.toString(y) + ")");
        try {
            if (ctx.getView(viewEnergy).getSpaceObjectAtPos(ctx.getPosition().add(dir)) != null) {    //I implemented an isDumbMove function earlier. That one had all of the star locations pre-programmed in. So this might cost more energy, but it is also more legit.
                dir.x = 0;
                dir.y = 0;
            }
        } catch (Exception e) {
            // do something here to deal with errors
            ctx.debugOut("Ayudame!" + e.toString());
        }
        ctx.debugOut("Moving to " + ctx.getPosition().add(dir).toString() + ctx.getStateString());
        return dir;
    }

    @Override
    public Action getAction() {
        // ctx.debugOut("Action requested,"
        //         + " E:" + Double.toString(ctx.getEnergy())
        //         + " T:" + Double.toString(ctx.getTech()));
        try {
            View view = ctx.getView((int) ctx.getTech());    //May change this later

            if (!pacifist) {

                if (view.getAliensAtPos(ctx.getPosition()).size() > 1 //Using the same fighting code as the Dalek.
                        && ctx.getEnergy() > ctx.getFightingCost() + 2) {
                    ctx.debugOut("Thinking Machine fighting."
                            + ctx.getStateString());
                    return new Action(Action.ActionCode.Fight, (int) ctx.getEnergy() - 2 - ctx.getFightingCost());
                }
            }
            if (state == 1) {    //If the machine wants to spawn
                if (ctx.getEnergy() > ctx.getSpawningCost() + flavorOffspringEnergy + flavorSpawning) {
                    //      ctx.debugOut("Flower Alien spawning."
                    //              + " E:" + Double.toString(ctx.getEnergy())
                    //              + " T:" + Double.toString(ctx.getTech()));
                    spawnTechConst += ctx.getSpawningCost() + flavorOffspringEnergy;
                    return new Action(Action.ActionCode.Spawn, flavorOffspringEnergy);
                } else {
                    //     ctx.debugOut("Choosing to gain energy,"
                    //            + " E:" + Double.toString(ctx.getEnergy())
                    //            + " T:" + Double.toString(ctx.getTech()));
                    return new Action(Action.ActionCode.Gain);
                }
            }
            if (state == 2) {    //If the machine wants to tech
                if (ctx.getEnergy() > ctx.getTech() + flavorTech && ctx.getTech() < 30) {    //30 is the limit defined in Constants. No way to access it directly.
                    //        ctx.debugOut("Choosing to research"
                    //                + " E:" + Double.toString(ctx.getEnergy())
                    //               + " T:" + Double.toString(ctx.getTech()));
                    checkReturns();
                    spawnTechConst += ctx.getTech();
                    return new Action(Action.ActionCode.Research);
                } else {
                    //       ctx.debugOut("Choosing to gain energy,"
                    //               + " E:" + Double.toString(ctx.getEnergy())
                    //               + " T:" + Double.toString(ctx.getTech()));
                    return new Action(Action.ActionCode.Gain);
                }
            }
            if (state == 3) {
                return new Action(Action.ActionCode.Gain);
            }
        } catch (Exception e) {
            ctx.debugOut("Mistakes were made!" + e.toString());    //Error handling

        }
        // ctx.debugOut("Gaining energy"
        //         + " E:" + Double.toString(ctx.getEnergy())
        //         + " T:" + Double.toString(ctx.getTech()));
        return new Action(Action.ActionCode.Gain);
    }
/*
    public void checkForStupidity(int viewEnergy, Direction dir) {    //Obsolete function.
        try {
            if (ctx.getView(viewEnergy).getSpaceObjectAtPos(ctx.getPosition().add(dir)) != null) {    //I implemented an isDumbMove function earlier. That one had all of the star locations pre-programmed in. So this might cost more energy, but it is also more legit.
                dir.x = 0;
                dir.y = 0;
            } else if (ctx.getPosition().add(dir).x < ctx.getMinPosition().x //Wrong
                    || ctx.getPosition().add(new Direction(dir.x * -1, 0)).x > ctx.getMaxPosition().x //Really, if there is an "add" function, there should be a "subtract" function. Eh.
                    || ctx.getPosition().add(dir).y < ctx.getMinPosition().y
                    || ctx.getPosition().add(new Direction(0, dir.y * -1)).y > ctx.getMaxPosition().y) {
                dir.x = 0;
                dir.y = 0;
            }
        } catch (Exception e) {
            // do something here to deal with errors
            ctx.debugOut("Ayudame!" + e.toString());
        }
    }
*/
    @Override
    public void communicate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void receive(String[] strings) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void processResults() {
    }

}
