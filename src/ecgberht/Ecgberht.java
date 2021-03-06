package ecgberht;

import bwem.BWEM;
import bwem.Base;
import ecgberht.Agents.DropShipAgent;
import ecgberht.Agents.VesselAgent;
import ecgberht.Agents.VultureAgent;
import ecgberht.Agents.WraithAgent;
import ecgberht.BehaviourTrees.AddonBuild.*;
import ecgberht.BehaviourTrees.Attack.CheckArmy;
import ecgberht.BehaviourTrees.Attack.ChooseAttackPosition;
import ecgberht.BehaviourTrees.Build.*;
import ecgberht.BehaviourTrees.BuildingLot.CheckBuildingsLot;
import ecgberht.BehaviourTrees.BuildingLot.ChooseBlotWorker;
import ecgberht.BehaviourTrees.BuildingLot.ChooseBuildingLot;
import ecgberht.BehaviourTrees.BuildingLot.FinishBuilding;
import ecgberht.BehaviourTrees.Bunker.ChooseBunkerToLoad;
import ecgberht.BehaviourTrees.Bunker.ChooseMarineToEnter;
import ecgberht.BehaviourTrees.Bunker.EnterBunker;
import ecgberht.BehaviourTrees.Defense.CheckPerimeter;
import ecgberht.BehaviourTrees.Defense.ChooseDefensePosition;
import ecgberht.BehaviourTrees.Defense.SendDefenders;
import ecgberht.BehaviourTrees.Harass.*;
import ecgberht.BehaviourTrees.IslandExpansion.*;
import ecgberht.BehaviourTrees.Recollection.CollectGas;
import ecgberht.BehaviourTrees.Recollection.CollectMineral;
import ecgberht.BehaviourTrees.Recollection.FreeWorker;
import ecgberht.BehaviourTrees.Repair.CheckBuildingFlames;
import ecgberht.BehaviourTrees.Repair.ChooseRepairer;
import ecgberht.BehaviourTrees.Repair.Repair;
import ecgberht.BehaviourTrees.Scanner.CheckScan;
import ecgberht.BehaviourTrees.Scanner.Scan;
import ecgberht.BehaviourTrees.Scouting.*;
import ecgberht.BehaviourTrees.Training.*;
import ecgberht.BehaviourTrees.Upgrade.*;
import ecgberht.Strategies.BioBuild;
import org.iaie.btree.BehavioralTree;
import org.iaie.btree.task.composite.Selector;
import org.iaie.btree.task.composite.Sequence;
import org.iaie.btree.util.GameHandler;
import org.openbw.bwapi4j.*;
import org.openbw.bwapi4j.type.Race;
import org.openbw.bwapi4j.type.TechType;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.type.UpgradeType;
import org.openbw.bwapi4j.unit.*;
import org.openbw.bwapi4j.util.Pair;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

public class Ecgberht implements BWEventListener {

    private static BW bw;
    private static InteractionHandler ih;
    private static GameState gs;
    private static BehavioralTree addonBuildTree;
    private static BehavioralTree buildTree;
    private static BehavioralTree trainTree;
    private static BehavioralTree upgradeTree;
    private BehavioralTree attackTree;
    private BehavioralTree botherTree;
    private BehavioralTree buildingLotTree;
    private BehavioralTree bunkerTree;
    private BehavioralTree collectTree;
    private BehavioralTree defenseTree;
    private BehavioralTree repairTree;
    private BehavioralTree scannerTree;
    private BehavioralTree scoutingTree;
    private BehavioralTree islandTree;
    private boolean first = false;
    private Player self;
    private BWEM bwem = null;

    public static void main(String[] args) {
        new Ecgberht().run();
    }

    public static BW getGame() {
        return bw;
    }

    public static InteractionHandler getIH() {
        return ih;
    }

    public static GameState getGs() {
        return gs;
    }

    public static void transition() {
        initTrainTree();
        initBuildTree();
        initUpgradeTree();
        initAddonBuildTree();
    }

    private static void initTrainTree() {
        ChooseSituationalUnit cSU = new ChooseSituationalUnit("Choose situational unit", gs);
        ChooseNothingTrain cNT = new ChooseNothingTrain("Choose Nothing To Train", gs);
        ChooseSCV cSCV = new ChooseSCV("Choose SCV", gs);
        ChooseMarine cMar = new ChooseMarine("Choose Marine", gs);
        ChooseMedic cMed = new ChooseMedic("Choose Medic", gs);
        ChooseTank cTan = new ChooseTank("Choose Tank", gs);
        ChooseVulture cVul = new ChooseVulture("Choose vulture", gs);
        ChooseWraith cWra = new ChooseWraith("Choose Wraith", gs);
        CheckResourcesUnit cr = new CheckResourcesUnit("Check Cash", gs);
        TrainUnit tr = new TrainUnit("Train SCV", gs);
        Selector<GameHandler> chooseUnit = new Selector<>("Choose Recruit", cNT, cSU, cSCV);
        if (gs.strat.trainUnits.contains(UnitType.Terran_Siege_Tank_Tank_Mode)) chooseUnit.addChild(cTan);
        if (gs.strat.trainUnits.contains(UnitType.Terran_Vulture)) chooseUnit.addChild(cVul);
        if (gs.strat.trainUnits.contains(UnitType.Terran_Wraith)) chooseUnit.addChild(cWra);
        if (gs.strat.trainUnits.contains(UnitType.Terran_Medic)) chooseUnit.addChild(cMed);
        if (gs.strat.trainUnits.contains(UnitType.Terran_Marine)) chooseUnit.addChild(cMar);
        Sequence train = new Sequence("Train", chooseUnit, cr, tr);
        trainTree = new BehavioralTree("Training Tree");
        trainTree.addChild(train);
    }

    private static void initBuildTree() {
        Build b = new Build("Build", gs);
        ChooseSituationalBuilding cSB = new ChooseSituationalBuilding("Choose situational building", gs);
        ChooseNothingBuilding cNB = new ChooseNothingBuilding("Choose Nothing", gs);
        ChooseExpand cE = new ChooseExpand("Choose Expansion", gs);
        ChooseSupply cSup = new ChooseSupply("Choose Supply Depot", gs);
        ChooseBunker cBun = new ChooseBunker("Choose Bunker", gs);
        ChooseBarracks cBar = new ChooseBarracks("Choose Barracks", gs);
        ChooseFactory cFar = new ChooseFactory("Choose Factory", gs);
        ChoosePort cPor = new ChoosePort("Choose Star Port", gs);
        ChooseScience cSci = new ChooseScience("Choose Science Facility", gs);
        ChooseRefinery cRef = new ChooseRefinery("Choose Refinery", gs);
        ChooseBay cBay = new ChooseBay("Choose Bay", gs);
        ChooseTurret cTur = new ChooseTurret("Choose Turret", gs);
        ChooseAcademy cAca = new ChooseAcademy("Choose Academy", gs);
        ChooseArmory cArm = new ChooseArmory("Choose Armory", gs);
        CheckResourcesBuilding crb = new CheckResourcesBuilding("Check Cash", gs);
        ChoosePosition cp = new ChoosePosition("Choose Position", gs);
        ChooseWorker cw = new ChooseWorker("Choose Worker", gs);
        Move m = new Move("Move to chosen building position", gs);
        Selector<GameHandler> chooseBuildingBuild = new Selector<>("Choose Building to build", cNB, cSB, cE, cSup);
        if (gs.strat.bunker) chooseBuildingBuild.addChild(cBun);
        chooseBuildingBuild.addChild(cTur);
        chooseBuildingBuild.addChild(cRef);
        if (gs.strat.buildUnits.contains(UnitType.Terran_Academy)) chooseBuildingBuild.addChild(cAca);
        if (gs.strat.buildUnits.contains(UnitType.Terran_Engineering_Bay)) chooseBuildingBuild.addChild(cBay);
        if (gs.strat.buildUnits.contains(UnitType.Terran_Armory)) chooseBuildingBuild.addChild(cArm);
        if (gs.strat.buildUnits.contains(UnitType.Terran_Factory)) chooseBuildingBuild.addChild(cFar);
        if (gs.strat.buildUnits.contains(UnitType.Terran_Starport)) chooseBuildingBuild.addChild(cPor);
        if (gs.strat.buildUnits.contains(UnitType.Terran_Science_Facility)) chooseBuildingBuild.addChild(cSci);
        chooseBuildingBuild.addChild(cBar);
        Sequence buildMove = new Sequence("BuildMove", b, chooseBuildingBuild, cp, cw, crb, m);
        buildTree = new BehavioralTree("Building Tree");
        buildTree.addChild(buildMove);
    }

    private static void initUpgradeTree() {
        CheckResourcesUpgrade cRU = new CheckResourcesUpgrade("Check Resources Upgrade", gs);
        ChooseArmorMechUp cAMU = new ChooseArmorMechUp("Choose Armor mech upgrade", gs);
        ChooseWeaponMechUp cWMU = new ChooseWeaponMechUp("Choose weapon mech upgrade", gs);
        ChooseArmorInfUp cAIU = new ChooseArmorInfUp("Choose Armor inf upgrade", gs);
        ChooseWeaponInfUp cWIU = new ChooseWeaponInfUp("Choose Weapon inf upgrade", gs);
        ChooseMarineRange cMR = new ChooseMarineRange("Choose Marine Range upgrade", gs);
        ChooseStimUpgrade cSU = new ChooseStimUpgrade("Choose Stimpack upgrade", gs);
        ChooseSiegeMode cSM = new ChooseSiegeMode("Choose Siege Mode", gs);
        ResearchUpgrade rU = new ResearchUpgrade("Research Upgrade", gs);
        Selector<GameHandler> ChooseUP = new Selector<>("Choose Upgrade");
        if (gs.strat.upgradesToResearch.contains(UpgradeType.Terran_Infantry_Weapons)) ChooseUP.addChild(cWIU);
        if (gs.strat.upgradesToResearch.contains(UpgradeType.Terran_Infantry_Armor)) ChooseUP.addChild(cAIU);
        if (gs.strat.techToResearch.contains(TechType.Stim_Packs)) ChooseUP.addChild(cSU);
        if (gs.strat.upgradesToResearch.contains(UpgradeType.U_238_Shells)) ChooseUP.addChild(cMR);
        if (gs.strat.techToResearch.contains(TechType.Tank_Siege_Mode)) ChooseUP.addChild(cSM);
        if (gs.strat.upgradesToResearch.contains(UpgradeType.Terran_Vehicle_Weapons)) ChooseUP.addChild(cWMU);
        if (gs.strat.upgradesToResearch.contains(UpgradeType.Terran_Vehicle_Plating)) ChooseUP.addChild(cAMU);
        Sequence Upgrader = new Sequence("Upgrader", ChooseUP, cRU, rU);
        upgradeTree = new BehavioralTree("Technology");
        upgradeTree.addChild(Upgrader);
    }

    private static void initAddonBuildTree() {
        BuildAddon bA = new BuildAddon("Build Addon", gs);
        CheckResourcesAddon cRA = new CheckResourcesAddon("Check Resources Addon", gs);
        ChooseComsatStation cCS = new ChooseComsatStation("Choose Comsat Station", gs);
        ChooseMachineShop cMS = new ChooseMachineShop("Choose Machine Shop", gs);
        ChooseTower cT = new ChooseTower("Choose Control Tower", gs);
        Selector<GameHandler> ChooseAddon = new Selector<>("Choose Addon");
        if (gs.strat.buildAddons.contains(UnitType.Terran_Machine_Shop)) ChooseAddon.addChild(cMS);
        if (gs.strat.buildAddons.contains(UnitType.Terran_Comsat_Station)) ChooseAddon.addChild(cCS);
        if (gs.strat.buildAddons.contains(UnitType.Terran_Control_Tower)) ChooseAddon.addChild(cT);
        Sequence Addon = new Sequence("Addon", ChooseAddon, cRA, bA);
        addonBuildTree = new BehavioralTree("Addon Build Tree");
        addonBuildTree.addChild(Addon);
    }

    private void initIslandTree() {
        CheckIslands chI = new CheckIslands("Check islands", gs);
        CheckExpandingIsland cEI = new CheckExpandingIsland("Check Expanding To Island", gs);
        CheckDropped chD = new CheckDropped("Check Dropped", gs);
        CheckBlockingMinerals cBM = new CheckBlockingMinerals("Check Blocking minerals", gs);
        CheckResourcesIsland cRI = new CheckResourcesIsland("Check resources Island", gs);
        MoveIsland mI = new MoveIsland("Move Island", gs);
        ChooseDropShip cD = new ChooseDropShip("Choose DropShip", gs);
        ChooseIsland cI = new ChooseIsland("Choose Island", gs);
        ChooseWorkerDrop cWD = new ChooseWorkerDrop("Choose Worker Drop", gs);
        SendToDrop sD = new SendToDrop("Send To Drop", gs);
        Sequence chooseThings = new Sequence("Choose things", cD, cI, cWD, sD);
        Sequence expand = new Sequence("Island expand", cEI, chD, cBM, cRI, mI);
        Selector expanding = new Selector("Check if already expanding", expand, cEI, chooseThings);
        Sequence islandExpansion = new Sequence("island expansion", chI, expanding);
        islandTree = new BehavioralTree("islandTree");
        islandTree.addChild(islandExpansion);

    }

    private void run() {
        Ecgberht.bw = new BW(this);
        Ecgberht.bw.startGame();
    }

    @Override
    public void onStart() {
        try {
            ConfigManager.readConfig();
            if (!ConfigManager.getConfig().ecgConfig.debugConsole) {
                // Disables System.err and System.Out
                OutputStream output = null;
                try {
                    output = new FileOutputStream("NUL:");
                } catch (FileNotFoundException e) {
                }
                PrintStream nullOut = new PrintStream(output);
                System.setErr(nullOut);
                System.setOut(nullOut);
            }
            DataTraining.copyOnStart();
            self = bw.getInteractionHandler().self();
            ih = bw.getInteractionHandler();
            if (!ConfigManager.getConfig().ecgConfig.enableLatCom) ih.enableLatCom(false);
            else ih.enableLatCom(true);
            if (ConfigManager.getConfig().bwapiConfig.completeMapInformation) ih.enableCompleteMapInformation();
            if (ConfigManager.getConfig().bwapiConfig.frameSkip != 0)
                ih.setFrameSkip(ConfigManager.getConfig().bwapiConfig.frameSkip);
            if (ConfigManager.getConfig().bwapiConfig.localSpeed >= 0)
                ih.setLocalSpeed(ConfigManager.getConfig().bwapiConfig.localSpeed);
            if (ConfigManager.getConfig().bwapiConfig.userInput) ih.enableUserInput();
            bwem = new BWEM(bw);
            if (bw.getBWMap().mapHash().equals("69a3b6a5a3d4120e47408defd3ca44c954997948")) { // Hitchhiker
                ih.sendText("Hitchhiker :(");
            }
            bwem.initialize();
            bwem.getMap().assignStartingLocationsToSuitableBases();
            gs = new GameState(bw, bwem);
            gs.initEnemyRace();
            gs.readOpponentInfo();
            gs.alwaysPools();
            if (gs.enemyRace == Race.Zerg) {
                if (gs.EI.naughty) gs.playSound("rushed.mp3");
            }
            gs.strat = gs.initStrat();
            gs.initStartLocations();
            for (Base b : bwem.getMap().getBases()) {
                if (b.getArea().getAccessibleNeighbors().isEmpty()) {
                    gs.islandBases.add(b);
                } else gs.BLs.add(b);
            }
            gs.initBlockingMinerals();
            gs.initBaseLocations();
            gs.checkBasesWithBLockingMinerals();
            gs.initChokes();
            // Trees Initializations
            initCollectTree();
            initTrainTree();
            initBuildTree();
            initScoutingTree();
            initAttackTree();
            initDefenseTree();
            initUpgradeTree();
            initRepairTree();
            initAddonBuildTree();
            initBuildingLotTree();
            initBunkerTree();
            initScanTree();
            initHarassTree();
            //initIslandTree(); // TODO uncomment when BWAPI island bug is fixed
        } catch (Exception e) {
            System.err.println("onStart Exception");
            e.printStackTrace();
        }

    }

    private void initScoutingTree() {
        CheckScout cSc = new CheckScout("Check Scout", gs);
        ChooseScout chSc = new ChooseScout("Choose Scouter", gs);
        SendScout sSc = new SendScout("Send Scout", gs);
        CheckVisibleBase cVB = new CheckVisibleBase("Check visible Base", gs);
        CheckEnemyBaseVisible cEBV = new CheckEnemyBaseVisible("Check Enemy Base Visible", gs);
        Sequence scoutFalse = new Sequence("Scout ", cSc, chSc, sSc);
        Selector<GameHandler> EnemyFound = new Selector<>("Enemy found in base location", cEBV, sSc);
        Sequence scoutTrue = new Sequence("Scout True", cVB, EnemyFound);
        Selector<GameHandler> Scouting = new Selector<>("Select Scouting Plan", scoutFalse, scoutTrue);
        scoutingTree = new BehavioralTree("Movement Tree");
        scoutingTree.addChild(Scouting);
    }

    private void initAttackTree() {
        CheckArmy cA = new CheckArmy("Check Army", gs);
        ChooseAttackPosition cAP = new ChooseAttackPosition("Choose Attack Position", gs);
        Sequence Attack = new Sequence("Attack", cA, cAP);
        attackTree = new BehavioralTree("Attack Tree");
        attackTree.addChild(Attack);
    }

    private void initDefenseTree() {
        CheckPerimeter cP = new CheckPerimeter("Check Perimeter", gs);
        ChooseDefensePosition cDP = new ChooseDefensePosition("Choose Defence Position", gs);
        SendDefenders sD = new SendDefenders("Send Defenders", gs);
        Sequence Defense = new Sequence("Defence", cP, cDP, sD);
        defenseTree = new BehavioralTree("Defence Tree");
        defenseTree.addChild(Defense);
    }

    private void initRepairTree() {
        CheckBuildingFlames cBF = new CheckBuildingFlames("Check building in flames", gs);
        ChooseRepairer cR = new ChooseRepairer("Choose Repairer", gs);
        Repair R = new Repair("Repair Building", gs);
        Sequence Repair = new Sequence("Repair", cBF, cR, R);
        repairTree = new BehavioralTree("RepairTree");
        repairTree.addChild(Repair);
    }

    private void initBuildingLotTree() {
        CheckBuildingsLot chBL = new CheckBuildingsLot("Check Buildings Lot", gs);
        ChooseBlotWorker cBW = new ChooseBlotWorker("Choose Building Lot worker", gs);
        ChooseBuildingLot cBLot = new ChooseBuildingLot("Choose Building Lot building", gs);
        FinishBuilding fB = new FinishBuilding("Finish Building", gs);
        Sequence BLot = new Sequence("Building Lot", chBL, cBLot, cBW, fB);
        buildingLotTree = new BehavioralTree("Building Lot Tree");
        buildingLotTree.addChild(BLot);
    }

    private void initBunkerTree() {
        ChooseBunkerToLoad cBu = new ChooseBunkerToLoad("Choose Bunker to Load", gs);
        EnterBunker eB = new EnterBunker("Enter bunker", gs);
        ChooseMarineToEnter cMTE = new ChooseMarineToEnter("Choose Marine To Enter", gs);
        Sequence Bunker = new Sequence("Bunker", cBu, cMTE, eB);
        bunkerTree = new BehavioralTree("Bunker Tree");
        bunkerTree.addChild(Bunker);
    }

    private void initScanTree() {
        CheckScan cScan = new CheckScan("Check scan", gs);
        Scan s = new Scan("Scan", gs);
        Sequence Scanning = new Sequence("Scanning", cScan, s);
        scannerTree = new BehavioralTree("Scanner Tree");
        scannerTree.addChild(Scanning);
    }

    private void initHarassTree() {
        CheckHarasser cH = new CheckHarasser("Check Harasser", gs);
        CheckExplorer cE = new CheckExplorer("Check Explorer", gs);
        ChooseWorkerToHarass cWTH = new ChooseWorkerToHarass("Check Worker to Harass", gs);
        ChooseBuilderToHarass cWTB = new ChooseBuilderToHarass("Check Worker to Harass", gs);
        CheckHarasserAttacked cHA = new CheckHarasserAttacked("Check Harasser Attacked", gs);
        ChooseBuildingToHarass cBTH = new ChooseBuildingToHarass("Check Building to Harass", gs);
        Explore E = new Explore("Explore", gs);
        HarassWorker hW = new HarassWorker("Bother SCV", gs);
        Selector<GameHandler> bOw = new Selector<>("Choose Builder or Worker or Building", cWTH, cWTB, cBTH);
        Sequence harassAttack = new Sequence("Harass", cHA, bOw, hW);
        Sequence explorer = new Sequence("Explorer", cE, E);
        Selector<GameHandler> eOh = new Selector<>("Explorer or harasser", explorer, harassAttack);
        Sequence harass = new Sequence("Harass", cH, eOh);
        botherTree = new BehavioralTree("Harass Tree");
        botherTree.addChild(harass);
    }

    private void initCollectTree() {
        CollectGas cg = new CollectGas("Collect Gas", gs);
        CollectMineral cm = new CollectMineral("Collect Mineral", gs);
        FreeWorker fw = new FreeWorker("No Union", gs);
        Selector<GameHandler> collectResources = new Selector<>("Collect Melted Cash", cg, cm);
        Sequence collect = new Sequence("Collect", fw, collectResources);
        collectTree = new BehavioralTree("Recollection Tree");
        collectTree.addChild(collect);
    }

    @Override
    public void onFrame() {
        try {
            gs.frameCount = ih.getFrameCount();
            if (gs.frameCount == 1500) gs.sendCustomMessage();
            if (gs.frameCount == 2300) gs.sendRandomMessage();
            if (gs.frameCount == 1000 && bw.getBWMap().mapHash().equals("69a3b6a5a3d4120e47408defd3ca44c954997948")) {
                gs.getIH().sendText("RIP"); // Hitchhiker
                gs.getIH().leaveGame();
            }
            if (bw.getBWMap().mapHash().equals("6f5295624a7e3887470f3f2e14727b1411321a67") &&
                    gs.strat.name.equals("PlasmaWraithHell") && gs.frameCount == 24 * 700) {
                BioBuild b = new BioBuild();
                b.buildUnits.remove(UnitType.Terran_Bunker);
                gs.strat = b;
                gs.maxWraiths = 5;
                transition();
            }
            if (gs.frameCount % 3500 == 0) gs.resetInMap();
            if (bw.getBWMap().mapHash().equals("6f5295624a7e3887470f3f2e14727b1411321a67") &&
                    !gs.strat.name.equals("PlasmaWraithHell")) { // Plasma special eggs
                for (Unit u : bw.getAllUnits()) {
                    if (u.getInitialType() != UnitType.Zerg_Egg && u instanceof PlayerUnit && !Util.isEnemy(((PlayerUnit) u).getPlayer()))
                        continue;
                    if (!u.isVisible() && gs.enemyCombatUnitMemory.contains(u)) gs.enemyCombatUnitMemory.remove(u);
                    else if (u.getInitialType() == UnitType.Zerg_Egg &&
                            !Util.isEnemy(((PlayerUnit) u).getPlayer())) {
                        gs.enemyCombatUnitMemory.add(u);
                    }
                }
            }
            IntelligenceAgency.updateBullets();
            gs.fix();
            gs.updateEnemyBuildingsMemory();
            gs.inMapUnits = new InfluenceMap(bw, bw.getBWMap().mapHeight(), bw.getBWMap().mapWidth());
            IntelligenceAgency.onFrame();
            gs.sim.onFrameSim();
            gs.runAgents();
            buildingLotTree.run();
            repairTree.run();
            collectTree.run();
            upgradeTree.run();
            //islandTree.run(); // TODO uncomment when BWAPI island bug is fixed
            buildTree.run();
            addonBuildTree.run();
            trainTree.run();
            scoutingTree.run();
            botherTree.run();
            bunkerTree.run();
            scannerTree.run();
            if (gs.strat.name.equals("ProxyBBS")) gs.checkWorkerMilitia();
            defenseTree.run();
            attackTree.run();
            gs.updateSquadOrderAndMicro();
            gs.checkMainEnemyBase();
            gs.mergeSquads();
            if (gs.frameCount > 0 && gs.frameCount % 5 == 0) gs.mineralLocking();
            gs.debugScreen();
            gs.debugText();
        } catch (Exception e) {
            System.err.println("onFrame Exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnd(boolean arg0) {
        try {
            String name = ih.enemy().getName();
            if (bw.getBWMap().mapHash().equals("6f5295624a7e3887470f3f2e14727b1411321a67"))
                gs.strat.name = "PlasmaWraithHell";
            String oldStrat = IntelligenceAgency.getStartStrat();
            if (oldStrat != null && !oldStrat.equals(gs.strat.name)) gs.strat.name = oldStrat;
            gs.EI.updateStrategyOpponentHistory(gs.strat.name, gs.mapSize, arg0);
            if (arg0) {
                gs.EI.wins++;
                ih.sendText("gg wp " + name);
            } else {
                gs.EI.losses++;
                ih.sendText("gg wp! " + name + ", next game I will not lose!");
            }
            gs.writeOpponentInfo(name);
            DataTraining.writeTravelData();
        } catch (Exception e) {
            System.err.println("onEnd Exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onNukeDetect(Position arg0) {

    }

    @Override
    public void onPlayerLeft(Player arg0) {

    }

    @Override
    public void onReceiveText(Player arg0, String arg1) {

    }

    @Override
    public void onSaveGame(String arg0) {

    }

    @Override
    public void onSendText(String arg0) {

    }

    @Override
    public void onUnitCreate(Unit arg0) {
        try {
            if (arg0 == null) return;
            if (arg0 instanceof MineralPatch || arg0 instanceof VespeneGeyser || arg0 instanceof SpecialBuilding
                    || arg0 instanceof Critter)
                return;
            PlayerUnit pU = (PlayerUnit) arg0;
            UnitType type = Util.getType(pU);
            if (!type.isNeutral() && !type.isSpecialBuilding()) {
                if (arg0 instanceof Building) {
                    if (pU.getPlayer().getId() == self.getId()) {
                        if (!(arg0 instanceof CommandCenter)) {
                            gs.map.updateMap(arg0.getTilePosition(), type, false);
                            gs.testMap = gs.map.clone();
                        }
                        if (arg0 instanceof Addon) return;
                        if (arg0 instanceof CommandCenter && ih.getFrameCount() == 0) return;
                        SCV worker = (SCV) ((Building) arg0).getBuildUnit();
                        if (worker != null) {
                            if (gs.workerBuild.containsKey(worker)) {
                                DataTraining.TravelData t = DataTraining.travelData.get(worker);
                                if (t != null) {
                                    t.frames = gs.frameCount - t.frames;
                                    DataTraining.travelData.put(worker, t);
                                }
                                if (type.equals(gs.workerBuild.get(worker).first)) {
                                    gs.workerTask.put(worker, (Building) arg0);
                                    gs.deltaCash.first -= type.mineralPrice();
                                    gs.deltaCash.second -= type.gasPrice();
                                    gs.workerBuild.remove(worker);
                                }
                            }
                        }
                    }
                    gs.inMap.updateMap(arg0, false);
                } else if (pU.getPlayer().getId() == self.getId()) {
                    if (gs.ih.getFrameCount() > 0) gs.supplyMan.onCreate(arg0);
                    if (arg0 instanceof Vulture) gs.vulturesTrained++;
                }
            }
        } catch (Exception e) {
            System.err.println("onUnitCreate exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onUnitComplete(Unit arg0) {
        try {
            if (arg0 instanceof MineralPatch || arg0 instanceof VespeneGeyser || arg0 instanceof SpecialBuilding
                    || arg0 instanceof Critter) {
                return;
            }
            PlayerUnit pU = (PlayerUnit) arg0;
            UnitType type = Util.getType(pU);
            if (!type.isNeutral() && pU.getPlayer().getId() == self.getId()) {
                if (gs.ih.getFrameCount() > 0) gs.supplyMan.onComplete(arg0);
                if (type.isBuilding()) {
                    gs.builtBuildings++;
                    if (type.isRefinery()) {
                        for (Entry<VespeneGeyser, Boolean> r : gs.vespeneGeysers.entrySet()) {
                            if (r.getKey().getTilePosition().equals(arg0.getTilePosition())) {
                                gs.vespeneGeysers.put(r.getKey(), true);
                                break;
                            }
                        }
                        for (Entry<SCV, Building> u : gs.workerTask.entrySet()) {
                            if (u.getValue().equals(arg0)) {
                                gs.workerGas.put(u.getKey(), (GasMiningFacility) arg0);
                                gs.workerTask.remove(u.getKey());
                                break;
                            }
                        }
                        gs.refineriesAssigned.put((GasMiningFacility) arg0, 1);
                        gs.builtRefinery++;
                    } else {
                        if (type == UnitType.Terran_Command_Center) {
                            gs.CCs.put(Util.getClosestBaseLocation(bwem.getMap().getArea(arg0.getTilePosition()).getTop().toPosition()), (CommandCenter) arg0);
                            if (gs.strat.name.equals("BioMechGreedyFE") && gs.CCs.size() > 2) gs.strat.raxPerCC = 3;
                            else if (gs.strat.name.equals("BioMechGreedyFE") && gs.CCs.size() < 3)
                                gs.strat.raxPerCC = 2;
                            gs.addNewResources(arg0);
                            if (((CommandCenter) arg0).getAddon() != null && !gs.CSs.contains(((CommandCenter) arg0).getAddon())) {
                                gs.CSs.add((ComsatStation) ((CommandCenter) arg0).getAddon());
                            }
                            if (gs.frameCount == 0)
                                gs.MainCC = new Pair<>(Util.getClosestBaseLocation(arg0.getPosition()), arg0);
                            gs.builtCC++;
                        }
                        if (type == UnitType.Terran_Comsat_Station) gs.CSs.add((ComsatStation) arg0);
                        if (type == UnitType.Terran_Bunker) gs.DBs.put((Bunker) arg0, new TreeSet<>());
                        if (type == UnitType.Terran_Engineering_Bay || type == UnitType.Terran_Academy) {
                            gs.UBs.add((ResearchingFacility) arg0);
                        }
                        if (type == UnitType.Terran_Barracks) gs.MBs.add((Barracks) arg0);
                        if (type == UnitType.Terran_Factory) gs.Fs.add((Factory) arg0);
                        if (type == UnitType.Terran_Starport) gs.Ps.add((Starport) arg0);
                        if (type == UnitType.Terran_Science_Facility) gs.UBs.add((ResearchingFacility) arg0);
                        if (type == UnitType.Terran_Control_Tower) gs.UBs.add((ResearchingFacility) arg0);
                        if (type == UnitType.Terran_Armory) gs.UBs.add((ResearchingFacility) arg0);
                        if (type == UnitType.Terran_Supply_Depot) gs.SBs.add((SupplyDepot) arg0);
                        if (type == UnitType.Terran_Machine_Shop) gs.UBs.add((ResearchingFacility) arg0);
                        if (type == UnitType.Terran_Missile_Turret) gs.Ts.add((MissileTurret) arg0);
                        for (Entry<SCV, Building> u : gs.workerTask.entrySet()) {
                            if (u.getValue().equals(arg0)) {
                                gs.workerIdle.add(u.getKey());
                                gs.workerTask.remove(u.getKey());
                                break;
                            }
                        }
                    }
                } else {
                    if (type.isWorker()) {
                        gs.workerIdle.add((Worker) arg0);
                        gs.trainedWorkers++;
                    } else {
                        if (type == UnitType.Terran_Siege_Tank_Tank_Mode) {
                            if (!gs.TTMs.containsKey(arg0)) {
                                String name = gs.addToSquad(arg0);
                                gs.TTMs.put(arg0, name);
                                if (!gs.DBs.isEmpty()) {
                                    ((MobileUnit) arg0).attack(gs.DBs.keySet().iterator().next().getPosition());
                                } else if (gs.mainChoke != null) {
                                    ((MobileUnit) arg0).attack(gs.mainChoke.getCenter().toPosition());
                                } else {
                                    ((MobileUnit) arg0).attack(Util.getClosestChokepoint(self.getStartLocation().toPosition()).getCenter().toPosition());
                                }
                            } else {
                                Squad tankS = gs.squads.get(gs.TTMs.get(arg0));
                                Position beforeSiege = null;
                                if (tankS != null) beforeSiege = tankS.attack;
                                if (beforeSiege != null) ((MobileUnit) arg0).attack(beforeSiege);
                            }
                        } else if (type == UnitType.Terran_Vulture) {
                            gs.agents.put(arg0, new VultureAgent(arg0));

                        } else if (type == UnitType.Terran_Dropship) {
                            DropShipAgent d = new DropShipAgent(arg0);
                            /*d.setTarget(gs.enemyBase.getCenter());
                            Entry<Worker, MineralPatch> scv = gs.workerMining.entrySet().iterator().next();
                            gs.mineralsAssigned.put(scv.getValue(),  gs.mineralsAssigned.get(scv.getValue()) - 1);
                            gs.workerMining.remove(scv.getKey());
                            scv.getKey().stop(false);
                            d.setCargo(new TreeSet<>(Arrays.asList(scv.getKey())));*/
                            gs.agents.put(arg0, d);

                        } else if (type == UnitType.Terran_Science_Vessel) {
                            Squad s = gs.chooseVesselSquad(arg0.getPosition());
                            VesselAgent v = new VesselAgent(arg0, s);
                            gs.agents.put(arg0, v);
                            gs.squads.get(s.name).detector = v;
                        } else if (type == UnitType.Terran_Wraith) {
                            if (gs.strat.name.equals("PlasmaWraithHell")) {
                                gs.addToSquad(arg0);
                            } else {
                                String name = gs.pickShipName();
                                gs.agents.put(arg0, new WraithAgent(arg0, name));
                            }
                        } else if (type == UnitType.Terran_Marine || type == UnitType.Terran_Medic) {
                            gs.addToSquad(arg0);
                            if (!gs.strat.name.equals("ProxyBBS")) {
                                if (!gs.EI.naughty || gs.enemyRace != Race.Zerg) {
                                    if (!gs.DBs.isEmpty()) {
                                        ((MobileUnit) arg0).attack(gs.DBs.keySet().iterator().next().getPosition());
                                    } else if (gs.mainChoke != null) {
                                        ((MobileUnit) arg0).attack(gs.mainChoke.getCenter().toPosition());
                                    } else {
                                        ((MobileUnit) arg0).attack(Util.getClosestChokepoint(self.getStartLocation().toPosition()).getCenter().toPosition());
                                    }
                                }
                            } else {
                                if (new TilePosition(bw.getBWMap().mapWidth() / 2, bw.getBWMap().mapHeight() / 2).getDistance(gs.enemyBase.getLocation()) < arg0.getTilePosition().getDistance(gs.enemyBase.getLocation())) {
                                    ((MobileUnit) arg0).attack(new TilePosition(bw.getBWMap().mapWidth() / 2, bw.getBWMap().mapHeight() / 2).toPosition());
                                }
                            }
                        }
                        gs.trainedCombatUnits++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("onUnitComplete exception");
            e.printStackTrace();
        }

    }

    @Override
    public void onUnitDestroy(Unit arg0) {
        try {
            UnitType type;
            if (arg0 instanceof MineralPatch || arg0 instanceof VespeneGeyser || arg0 instanceof SpecialBuilding
                    || arg0 instanceof Critter) {
                type = arg0.getInitialType();
            } else type = Util.getType((PlayerUnit) arg0);
            if (type.isMineralField()) {
                if (gs.mineralsAssigned.containsKey(arg0)) {
                    gs.map.updateMap(arg0.getTilePosition(), type, true);
                    gs.testMap = gs.map.clone();
                    List<Unit> aux = new ArrayList<>();
                    for (Entry<Worker, MineralPatch> w : gs.workerMining.entrySet()) {
                        if (arg0.equals(w.getValue())) {
                            w.getKey().stop(false);
                            gs.workerIdle.add(w.getKey());
                            aux.add(w.getKey());
                        }
                    }
                    for (Unit u : aux) gs.workerMining.remove(u);
                    gs.mineralsAssigned.remove(arg0);
                }
            }
            if (!type.isBuilding() && !type.isRefinery() && type != UnitType.Resource_Vespene_Geyser && type != UnitType.Spell_Scanner_Sweep) {
                if (!first) {
                    gs.playSound("first.mp3");
                    first = true;
                }
            }
            if (!type.isNeutral() && (!type.isSpecialBuilding() || type.isRefinery())) {
                if (Util.isEnemy(((PlayerUnit) arg0).getPlayer())) {
                    IntelligenceAgency.onDestroy(arg0, type);
                    if (arg0.equals(gs.chosenUnitToHarass)) gs.chosenUnitToHarass = null;
                    if (type.isBuilding()) {
                        gs.inMap.updateMap(arg0, true);
                        gs.enemyBuildingMemory.remove(arg0);
                        gs.initAttackPosition = arg0.getTilePosition();
                        if (!type.isResourceDepot()) gs.map.updateMap(arg0.getTilePosition(), type, true);
                    } else gs.initDefensePosition = arg0.getTilePosition();
                } else if (((PlayerUnit) arg0).getPlayer().getId() == self.getId()) {
                    if (gs.ih.getFrameCount() > 0) gs.supplyMan.onDestroy(arg0);
                    if (type.isWorker()) {
                        if (gs.strat.name == "ProxyBBS") gs.removeFromSquad(arg0);
                        for (SCV r : gs.repairerTask.keySet()) {
                            if (r.equals(arg0)) {
                                gs.workerIdle.add((Worker) arg0);
                                gs.repairerTask.remove(r);
                                break;
                            }
                        }
                        if (gs.workerIdle.contains(arg0)) gs.workerIdle.remove(arg0);
                        if (arg0.equals(gs.chosenScout)) gs.chosenScout = null;
                        if (arg0.equals(gs.chosenHarasser)) {
                            gs.chosenHarasser = null;
                            gs.chosenUnitToHarass = null;
                        }
                        if (arg0.equals(gs.chosenWorker)) gs.chosenWorker = null;
                        if (arg0.equals(gs.chosenRepairer)) gs.chosenRepairer = null;
                        if (arg0.equals(gs.chosenBuilderBL)) {
                            gs.chosenBuilderBL = null;
                            gs.expanding = false;
                            gs.chosenBaseLocation = null;
                            gs.movingToExpand = false;
                            gs.deltaCash.first -= UnitType.Terran_Command_Center.mineralPrice();
                            gs.deltaCash.second -= UnitType.Terran_Command_Center.gasPrice();
                        }
                        for (Worker u : gs.workerDefenders.keySet()) {
                            if (arg0.equals(u)) {
                                gs.workerDefenders.remove(u);
                                break;
                            }
                        }
                        if (gs.workerMining.containsKey(arg0)) {
                            Unit mineral = gs.workerMining.get(arg0);
                            gs.workerMining.remove(arg0);
                            if (gs.mineralsAssigned.containsKey(mineral)) {
                                gs.mining--;
                                gs.mineralsAssigned.put((MineralPatch) mineral, gs.mineralsAssigned.get(mineral) - 1);
                            }
                        }
                        if (gs.workerGas.containsKey(arg0)) { // TODO fix when destroyed
                            GasMiningFacility aux = gs.workerGas.get(arg0);
                            Integer auxInt = gs.refineriesAssigned.get(aux);
                            gs.refineriesAssigned.put(aux, auxInt - 1);
                            gs.workerGas.remove(arg0);
                        }
                        if (gs.workerTask.containsKey(arg0)) {
                            gs.buildingLot.add(gs.workerTask.get(arg0));
                            gs.workerTask.remove(arg0);
                        }
                        if (gs.workerBuild.containsKey(arg0)) {
                            if (DataTraining.travelData.containsKey(arg0)) {
                                DataTraining.travelData.remove(arg0);
                            }
                            gs.deltaCash.first -= gs.workerBuild.get(arg0).first.mineralPrice();
                            gs.deltaCash.second -= gs.workerBuild.get(arg0).first.gasPrice();
                            gs.workerBuild.remove(arg0);
                        }
                    } else if (type.isBuilding()) {
                        gs.inMap.updateMap(arg0, true);
                        if (type != UnitType.Terran_Command_Center) {
                            gs.map.updateMap(arg0.getTilePosition(), type, true);
                        }
                        for (Entry<SCV, Building> r : gs.repairerTask.entrySet()) {
                            if (r.getValue().equals(arg0)) {
                                gs.workerIdle.add(r.getKey());
                                gs.repairerTask.remove(r.getKey());
                                break;
                            }
                        }
                        for (Entry<SCV, Building> w : gs.workerTask.entrySet()) {
                            if (w.getValue().equals(arg0)) {
                                gs.workerTask.remove(w.getKey());
                                gs.workerIdle.add(w.getKey());
                                break;
                            }
                        }
                        for (Unit w : gs.buildingLot) {
                            if (w.equals(arg0)) {
                                gs.buildingLot.remove(w);
                                break;
                            }
                        }
                        for (CommandCenter u : gs.CCs.values()) {
                            if (u.equals(arg0)) {
                                gs.removeResources(arg0);
                                if (u.getAddon() != null && gs.CSs.contains(u.getAddon())) {
                                    gs.CSs.remove(u.getAddon());
                                }
                                if (bwem.getMap().getArea(arg0.getTilePosition()).equals(gs.naturalRegion)) {
                                    gs.defendPosition = gs.mainChoke.getCenter().toPosition();
                                }
                                gs.CCs.remove(Util.getClosestBaseLocation(arg0.getPosition()));
                                if (arg0.equals(gs.MainCC)) {
                                    if (gs.CCs.size() > 0) {
                                        for (Unit c : gs.CCs.values()) {
                                            if (!c.equals(arg0)) {
                                                gs.MainCC = new Pair<>(Util.getClosestBaseLocation(u.getPosition()), u);
                                                break;
                                            }
                                        }
                                    } else {
                                        gs.MainCC = null;
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        if (gs.CSs.contains(arg0)) gs.CSs.remove(arg0);
                        if (gs.Fs.contains(arg0)) gs.Fs.remove(arg0);
                        if (gs.MBs.contains(arg0)) gs.MBs.remove(arg0);
                        if (arg0 instanceof ResearchingFacility) {
                            if (gs.UBs.contains(arg0)) gs.UBs.remove(arg0);
                        }
                        if (gs.SBs.contains(arg0)) gs.SBs.remove(arg0);
                        if (gs.Ts.contains(arg0)) gs.Ts.remove(arg0);
                        if (gs.Ps.contains(arg0)) gs.Ps.remove(arg0);
                        if (type == UnitType.Terran_Bunker) {
                            if (gs.DBs.containsKey(arg0)) {
                                for (Unit u : gs.DBs.get(arg0)) gs.addToSquad(u);
                                gs.DBs.remove(arg0);
                            }
                        }
                        if (type.isRefinery()) { // TODO test
                            if (gs.refineriesAssigned.containsKey(arg0)) {
                                List<Unit> aux = new ArrayList<>();
                                for (Entry<Worker, GasMiningFacility> w : gs.workerGas.entrySet()) {
                                    if (arg0.equals(w.getValue())) {
                                        gs.workerIdle.add(w.getKey());
                                        aux.add(w.getKey());
                                    }
                                }
                                for (Unit u : aux) gs.workerGas.remove(u);
                                gs.refineriesAssigned.remove(arg0);
                                for (VespeneGeyser g : gs.vespeneGeysers.keySet()) {
                                    if (g.getTilePosition().equals(arg0.getTilePosition())) {
                                        gs.vespeneGeysers.put(g, false);
                                    }
                                }
                            }
                        }
                        gs.testMap = gs.map.clone();
                    } else {
                        if (type == UnitType.Terran_Siege_Tank_Siege_Mode || type == UnitType.Terran_Siege_Tank_Tank_Mode) {
                            if (gs.TTMs.containsKey(arg0)) {
                                gs.TTMs.remove(arg0);
                                gs.removeFromSquad(arg0);
                            }
                        } else if (type == UnitType.Terran_Marine || type == UnitType.Terran_Medic) {
                            gs.removeFromSquad(arg0);
                        } else if (type == UnitType.Terran_Vulture) {
                            if (gs.agents.containsKey(arg0)) gs.agents.remove(arg0);
                            else gs.removeFromSquad(arg0);
                        } else if (type == UnitType.Terran_Dropship) {
                            if (gs.agents.containsKey(arg0)) gs.agents.remove(arg0);
                        } else if (type == UnitType.Terran_Science_Vessel) {
                            if (gs.agents.containsKey(arg0)) {
                                VesselAgent v = (VesselAgent) gs.agents.get(arg0);
                                if (gs.squads.containsKey(v.follow.name)) gs.squads.get(v.follow.name).detector = null;
                                gs.agents.remove(arg0);
                            }
                        } else if (type == UnitType.Terran_Wraith) {
                            if (gs.strat.name.equals("PlasmaWraithHell")) {
                                gs.removeFromSquad(arg0);
                            } else if (gs.agents.containsKey(arg0)) {
                                String wraith = ((WraithAgent) gs.agents.get(arg0)).name;
                                gs.shipNames.add(wraith);
                                gs.agents.remove(arg0);
                            } else gs.removeFromSquad(arg0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("OnUnitDestroy Exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onUnitMorph(Unit arg0) {
        try {
            UnitType type;
            if (arg0 instanceof VespeneGeyser) type = arg0.getInitialType();
            else type = Util.getType((PlayerUnit) arg0);
            if (Util.isEnemy(((PlayerUnit) arg0).getPlayer())) { // TODO VespeneGeyser morphs
                if (arg0 instanceof Building && !(arg0 instanceof GasMiningFacility)) {
                    if (!gs.enemyBuildingMemory.containsKey(arg0)) {
                        gs.inMap.updateMap(arg0, false);
                        gs.enemyBuildingMemory.put(arg0, new EnemyBuilding(arg0));
                    }
                }
            }
            if (arg0 instanceof Refinery && ((PlayerUnit) arg0).getPlayer().getId() == self.getId()) {
                for (Entry<GasMiningFacility, Integer> r : gs.refineriesAssigned.entrySet()) {
                    if (r.getKey().getTilePosition().equals(arg0.getTilePosition())) {
                        gs.map.updateMap(arg0.getTilePosition(), type, false);
                        gs.testMap = gs.map.clone();
                        break;
                    }
                }
                for (Entry<SCV, Pair<UnitType, TilePosition>> u : gs.workerBuild.entrySet()) {
                    if (u.getKey().equals(((Building) arg0).getBuildUnit()) && u.getValue().first == type) {
                        gs.workerBuild.remove(u.getKey());
                        gs.workerTask.put(u.getKey(), (Building) arg0);
                        gs.deltaCash.first -= type.mineralPrice();
                        gs.deltaCash.second -= type.gasPrice();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("onUnitMorph Exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onUnitDiscover(Unit arg0) {

    }

    @Override
    public void onUnitEvade(Unit arg0) {

    }

    @Override
    public void onUnitHide(Unit arg0) {
        if (gs.enemyCombatUnitMemory.contains(arg0)) {
            gs.enemyCombatUnitMemory.remove(arg0);
        }
    }

    @Override
    public void onUnitRenegade(Unit arg0) {

    }

    @Override
    public void onUnitShow(Unit arg0) {
        try {
            if (arg0 instanceof MineralPatch || arg0 instanceof VespeneGeyser || arg0 instanceof SpecialBuilding ||
                    arg0 instanceof Critter) return;
            UnitType type = Util.getType((PlayerUnit) arg0);
            Player p = ((PlayerUnit) arg0).getPlayer();
            if (p != null && Util.isEnemy(p)) {
                IntelligenceAgency.onShow(arg0, type);
                if (gs.enemyRace == Race.Unknown && getGs().players.size() == 3) { // TODO Check
                    gs.enemyRace = type.getRace();
                }
                if (!type.isBuilding() && (type.canAttack() || type.isSpellcaster() || (type.spaceProvided() > 0 && type != UnitType.Zerg_Overlord))) {
                    gs.enemyCombatUnitMemory.add(arg0);
                }
                if (type.isBuilding()) {
                    if (!gs.enemyBuildingMemory.containsKey(arg0)) {
                        gs.enemyBuildingMemory.put(arg0, new EnemyBuilding(arg0));
                        gs.inMap.updateMap(arg0, false);
                        gs.map.updateMap(arg0.getTilePosition(), type, false);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("OnUnitShow Exception");
            e.printStackTrace();
        }

    }
}
