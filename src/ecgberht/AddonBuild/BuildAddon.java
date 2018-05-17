package ecgberht.AddonBuild;

import ecgberht.GameState;
import org.iaie.btree.state.State;
import org.iaie.btree.task.leaf.Action;
import org.iaie.btree.util.GameHandler;

public class BuildAddon extends Action {

    public BuildAddon(String name, GameHandler gh) {
        super(name, gh);
    }

    @Override
    public State execute() {
        try {
            if (((GameState) this.handler).chosenBuildingAddon.canBuildAddon(((GameState) this.handler).chosenAddon)) {
                if (((GameState) this.handler).chosenBuildingAddon.buildAddon(((GameState) this.handler).chosenAddon)) {
                    ((GameState) this.handler).chosenBuildingAddon = null;
                    ((GameState) this.handler).chosenAddon = null;
                    return State.SUCCESS;
                }
            }
            ((GameState) this.handler).chosenBuildingAddon = null;
            ((GameState) this.handler).chosenAddon = null;
            return State.FAILURE;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName());
            System.err.println(e);
            return State.ERROR;
        }
    }
}
