package ecgberht.BehaviourTrees.Build;

import ecgberht.GameState;
import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.type.Race;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.unit.Barracks;
import org.openbw.bwapi4j.unit.Building;
import org.openbw.bwapi4j.unit.Bunker;
import org.openbw.bwapi4j.unit.SupplyDepot;
import org.openbw.bwapi4j.util.Pair;

public class ChooseSupply extends Action {

    public ChooseSupply(String name, GameHandler gh) {
        super(name, gh);
    }

    @Override
    public State execute() {
        try {
            if (((GameState) this.handler).getPlayer().supplyTotal() >= 400) {
                return State.FAILURE;
            }
            //if(((GameState)this.handler).countUnit2(UnitType.Terran_Barracks) < 2 && ((GameState)this.handler).strat.name == "ProxyBBS") {
            if (((GameState) this.handler).strat.name == "ProxyBBS" && ((GameState) this.handler).countUnit(UnitType.Terran_Barracks) < 2) {
                return State.FAILURE;
            }
            if (((GameState) this.handler).EI.naughty && ((GameState) this.handler).MBs.isEmpty() && ((GameState) this.handler).enemyRace == Race.Zerg) {
                if (!((GameState) this.handler).SBs.isEmpty() && !((GameState) this.handler).DBs.isEmpty()) {
                    boolean found_bunker = false;
                    for (Pair<UnitType, TilePosition> w : ((GameState) this.handler).workerBuild.values()) {
                        if (w.first == UnitType.Terran_Bunker) {
                            found_bunker = true;
                        }
                    }
                    if (!found_bunker) {
                        for (Building w : ((GameState) this.handler).workerTask.values()) {
                            if (w instanceof Bunker) {
                                found_bunker = true;
                                break; // TODO test
                            }
                        }
                    }
                    if (!found_bunker) return State.FAILURE;
                }
                boolean found_rax = false;
                for (Pair<UnitType, TilePosition> w : ((GameState) this.handler).workerBuild.values()) {
                    if (w.first == UnitType.Terran_Barracks) {
                        found_rax = true;
                    }
                }
                if (!found_rax) {
                    for (Building w : ((GameState) this.handler).workerTask.values()) {
                        if (w instanceof Barracks) {
                            found_rax = true;
                            break; // TODO test
                        }
                    }
                }
                if (!found_rax) return State.FAILURE;
            }
            if (((GameState) this.handler).getSupply() <= 4 * ((GameState) this.handler).getCombatUnitsBuildings()) {
                for (Pair<UnitType, TilePosition> w : ((GameState) this.handler).workerBuild.values()) {
                    if (w.first == UnitType.Terran_Supply_Depot) {
                        return State.FAILURE;
                    }
                }
                for (Building w : ((GameState) this.handler).workerTask.values()) {
                    if (w instanceof SupplyDepot) {
                        return State.FAILURE;
                    }
                }
                ((GameState) this.handler).chosenToBuild = UnitType.Terran_Supply_Depot;
                return State.SUCCESS;
            }
            return State.FAILURE;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName());
            e.printStackTrace();
            return State.ERROR;
        }
    }
}
