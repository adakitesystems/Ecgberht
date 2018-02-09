package ecgberht.Scouting;

import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Conditional;
import org.iaie.btree.util.GameHandler;

import bwta.BWTA;
import bwta.BaseLocation;
import ecgberht.GameState;

public class CheckScout extends Conditional {

	public CheckScout(String name, GameHandler gh) {
		super(name, gh);
	}

	@Override
	public State execute() {
		try {
			if(((GameState)this.handler).EI.defendHarass && BWTA.getStartLocations().size() == 2) {
				for(BaseLocation b : BWTA.getStartLocations()) {
					if(!((GameState)this.handler).getGame().isVisible(b.getTilePosition())) {
						((GameState)this.handler).enemyBase = b;
						((GameState)this.handler).scout = false;
						break;
					}
				}
				return State.FAILURE;
			}
			if(((GameState)this.handler).chosenScout == null && ((GameState)this.handler).getPlayer().supplyUsed() >= 12  && ((GameState)this.handler).enemyBase == null) {
				return State.SUCCESS;
			}
			return State.FAILURE;
		} catch(Exception e) {
			System.err.println(this.getClass().getSimpleName());
			System.err.println(e);
			return State.ERROR;
		}
	}
}
