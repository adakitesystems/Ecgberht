package ecgberht.Agents;

import ecgberht.EnemyBuilding;
import ecgberht.Simulation.SimInfo;
import org.openbw.bwapi4j.Position;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.unit.*;
import org.openbw.bwapi4j.util.Pair;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static ecgberht.Ecgberht.getGs;

public class WraithAgent extends Agent implements Comparable<Unit> {

    public Wraith unit;
    public String name = "Pepe";
    private Set<Unit> airAttackers = new TreeSet<>();

    public WraithAgent(Unit unit) {
        super();
        this.unit = (Wraith) unit;
        this.myUnit = unit;
    }

    public WraithAgent(Unit unit, String name) {
        super();
        this.unit = (Wraith) unit;
        this.name = name;
        this.myUnit = unit;
    }

    @Override
    public boolean runAgent() {
        try {
            if (!unit.exists()) return true;
            if (unit.getHitPoints() <= 15) {
                Position cc = getGs().MainCC.second.getPosition();
                if (cc != null) unit.move(cc);
                else unit.move(getGs().getPlayer().getStartLocation().toPosition());
                getGs().addToSquad(unit);
                return true;
            }
            actualFrame = getGs().frameCount;
            frameLastOrder = unit.getLastCommandFrame();
            closeEnemies.clear();
            closeWorkers.clear();
            airAttackers.clear();
            if (frameLastOrder == actualFrame) return false;
            Status old = status;
            getNewStatus();
            if (old == status && status != Status.COMBAT && status != Status.ATTACK) return false;
            if (status != Status.COMBAT) attackUnit = null;
            if (status == Status.ATTACK && unit.isIdle()) {
                Pair<Integer, Integer> pos = getGs().inMap.getPosition(unit.getTilePosition(), true);
                if (pos.first != -1 && pos.second != -1) {
                    Position newPos = new TilePosition(pos.second, pos.first).toPosition();
                    if (getGs().bw.getBWMap().isValidPosition(newPos)) {
                        unit.attack(newPos);
                        return false;
                    }
                }
            }
            switch (status) {
                case ATTACK:
                    attack();
                    break;
                case COMBAT:
                    combat();
                    break;
                case RETREAT:
                    retreat();
                    break;
                default:
                    break;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Exception WraithAgent");
            e.printStackTrace();
        }
        return false;
    }

    private void combat() {
        Unit toAttack = getUnitToAttack(unit, closeEnemies);
        if (toAttack != null) {
            if (attackUnit != null) {
                if (attackUnit.equals(toAttack)) return;
            }
            unit.attack(toAttack);
            attackUnit = toAttack;
        } else {
            if (!closeWorkers.isEmpty()) {
                toAttack = getUnitToAttack(unit, closeWorkers);
                if (toAttack != null) {
                    if (attackUnit != null) {
                        if (attackUnit.equals(toAttack)) {
                            return;
                        } else {
                            unit.attack(toAttack);
                            attackUnit = toAttack;
                            attackPos = null;
                        }
                    }
                }
            }
        }
    }

    private void getNewStatus() {
        Position myPos = unit.getPosition();
        if (getGs().enemyCombatUnitMemory.isEmpty()) {
            status = Status.ATTACK;
            return;
        }
        for (Unit u : getGs().enemyCombatUnitMemory) {
            if (u instanceof Worker && !((PlayerUnit) u).isAttacking()) closeWorkers.add(u);
            double dist = getGs().broodWarDistance(u.getPosition(), myPos);
            if (dist <= 700) closeEnemies.add(u);
            if (dist <= 700 && u instanceof AirAttacker) airAttackers.add(u);
        }
        for (EnemyBuilding u : getGs().enemyBuildingMemory.values()) {
            if (!getGs().getGame().getBWMap().isVisible(u.pos)) continue;
            double dist = getGs().broodWarDistance(u.pos.toPosition(), myPos);
            if (dist <= 700) closeEnemies.add(u.unit);
            if (dist <= 700 && (u.unit instanceof AirAttacker || u.type == UnitType.Terran_Bunker) && u.unit.isCompleted()) {
                airAttackers.add(u.unit);
            }
        }
        if (closeEnemies.isEmpty()) {
            status = Status.ATTACK;
            return;
        } else {
            if (!airAttackers.isEmpty() && getGs().sim.getSimulation(unit, SimInfo.SimType.AIR).lose) {
                status = Status.RETREAT;
                return;
            }
        }
    }

    private void attack() {
        Position newAttackPos = selectNewAttack();
        if (attackPos == null) {
            attackPos = newAttackPos;
            if (attackPos == null || !getGs().bw.getBWMap().isValidPosition(attackPos)) {
                attackUnit = null;
                attackPos = null;
                return;
            }
            if (getGs().bw.getBWMap().isValidPosition(attackPos)) {
                unit.attack(newAttackPos);
                attackUnit = null;
            }
            return;
        } else if (attackPos.equals(newAttackPos)) return;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this.unit) return true;
        if (!(o instanceof WraithAgent)) return false;
        WraithAgent wraith = (WraithAgent) o;
        return unit.equals(wraith.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit);
    }

    @Override
    public int compareTo(Unit v1) {
        return this.unit.getId() - v1.getId();
    }

}
