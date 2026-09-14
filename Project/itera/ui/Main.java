package itera.ui;

import itera.model.Bloater;
import itera.model.Building;
import itera.model.Character;
import itera.model.Civilian;
import itera.model.ConvenienceStore;
import itera.model.Hospital;
import itera.model.Human;
import itera.model.Medic;
import itera.model.PoliceStation;
import itera.model.Resource;
import itera.model.Runner;
import itera.model.SafePoint;
import itera.model.Soldier;
import itera.model.Stalker;
import itera.model.Vector2D;
import itera.model.Zombie;
import itera.simulation.World;
import itera.simulation.ZombieWave;
import itera.simulation.SimulationSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings({"serial", "this-escape"})
public class Main extends JPanel {

    private static final int WORLD_WIDTH = 1200;
    private static final int WORLD_HEIGHT = 800;

    private static final int BUILDING_SIZE = 180;
    private static final int BOTTOM_BUILDING_OFFSET = 70;

    private final Random random = new Random();

    private final World world;
    private final SafePoint safePoint;
    private final FastForward fastForward;
    private final SimulationSettings settings;

    private final ArrayList<Building> buildings =
        new ArrayList<>();

    private final ArrayList<Resource> resources =
        new ArrayList<>();

    private Timer timer;

    /*
     * Zombie wave settings.
     */
    private static final long WAVE_INTERVAL = 15000;

    private static final int BASE_ZOMBIES_PER_WAVE = 9;

    private static final int ZOMBIES_ADDED_PER_WAVE = 3;

    private long lastWaveTime;

    private int waveNumber = 1;

    public Main() {
        this(createDefaultSettings());
    }

    public Main(SimulationSettings settings) {

        this.settings = settings;

        setPreferredSize(
            new Dimension(
                WORLD_WIDTH,
                WORLD_HEIGHT
            )
        );

        world = new World();

        /*
         * SAFE POINT
         */
        safePoint = new SafePoint(
            0,
            0,
            BUILDING_SIZE,
            BUILDING_SIZE,
            10
        );

        world.addSafePoint(safePoint);

        /*
         * BUILDINGS
         */

        Hospital hospital =
            new Hospital(
                WORLD_WIDTH - BUILDING_SIZE,
                0
            );

        PoliceStation policeStation =
            new PoliceStation(
                0,
                WORLD_HEIGHT
                    - BUILDING_SIZE
                    - BOTTOM_BUILDING_OFFSET
            );

        ConvenienceStore convenienceStore =
            new ConvenienceStore(
                WORLD_WIDTH - BUILDING_SIZE,
                WORLD_HEIGHT
                    - BUILDING_SIZE
                    - BOTTOM_BUILDING_OFFSET
            );

        buildings.add(hospital);
        buildings.add(policeStation);
        buildings.add(convenienceStore);

        world.addBuilding(hospital);
        world.addBuilding(policeStation);
        world.addBuilding(convenienceStore);

        /*
         * RESOURCES
         */

        for (Building building : buildings) {
            resources.addAll(building.getResources());
        }
        for (Resource resource : resources) world.addResource(resource);

        addStartingHumans();

        /*
         * INITIAL ZOMBIES
         */

        // Zombies spread across the centre. Bosses still begin on boss waves.
        int zombieColumns = Math.min(
            10,
            Math.max(5, (int) Math.ceil(Math.sqrt(settings.getStartingZombies())))
        );
        int zombieRows = (int) Math.ceil(
            settings.getStartingZombies() / (double) zombieColumns
        );
        for (int i = 0; i < settings.getStartingZombies(); i++) {
            int x = WORLD_WIDTH / 2 - (zombieColumns - 1) * 35
                + (i % zombieColumns) * 70;
            int y = WORLD_HEIGHT / 2 - (zombieRows - 1) * 35
                + (i / zombieColumns) * 70;
            Zombie zombie = switch (i % 4) {
                case 1 -> new Runner(x, y);
                case 2 -> new Stalker(x, y);
                case 3 -> new Bloater(x, y);
                default -> new Zombie(x, y);
            };
            world.addCharacter(zombie);
        }

        /*
         * Fast-forward control.
         */
        fastForward = new FastForward();

        /*
         * Main simulation timer.
         */
        timer =
            new Timer(
                30,
                e -> {

                    try {
                        timer.setDelay(
                            fastForward.getDelay()
                        );
                        updateSimulation();
                        repaint();
                    } catch (RuntimeException exception) {
                        timer.stop();
                        JOptionPane.showMessageDialog(
                            this,
                            "The simulation stopped because of an unexpected error:\n"
                                + exception.getMessage(),
                            "Simulation Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            );
    }

    /**
     * Uses the original 30/40/30 civilian/soldier/medic balance while scaling
     * each role to the population selected in the setup screen.
     */
    private void addStartingHumans() {
        int civilianCount = settings.getStartingHumans() * 3 / 10;
        int medicCount = settings.getStartingHumans() * 3 / 10;
        int soldierCount = settings.getStartingHumans()
            - civilianCount - medicCount;
        for (int i = 0; i < settings.getStartingHumans(); i++) {
            Vector2D spawn = randomHumanPosition();
            int x = (int) spawn.getX();
            int y = (int) spawn.getY();
            Human human;
            if (i < civilianCount) {
                human = new Civilian(x, y);
            } else if (i < civilianCount + soldierCount) {
                human = new Soldier(x, y);
            } else {
                human = new Medic(x, y);
            }
            world.addCharacter(human);
        }
    }

    private static SimulationSettings createDefaultSettings() {
        return new SimulationSettings(
            SimulationSettings.DEFAULT_HUMANS,
            SimulationSettings.DEFAULT_ZOMBIES);
    }

    /*
     * Finds a random position
     * for a human.
     */
    private Vector2D randomHumanPosition() {

        int x;
        int y;
        boolean invalid;

        do {

            x =
                random.nextInt(
                    WORLD_WIDTH - 100
                ) + 40;

            y =
                random.nextInt(
                    WORLD_HEIGHT - 120
                ) + 40;

            /*
             * Check safe point.
             */
            invalid =
                safePoint.contains(
                    x,
                    y,
                    15
                );

            /*
             * Check other buildings.
             */
            for (Building building : buildings) {

                if (building.overlaps(x, y, 15)) {

                    invalid = true;

                    break;
                }
            }

        } while (invalid);

        return new Vector2D(x, y);
    }

    /*
     * Creates a new zombie wave
     * every 15 seconds.
     */
    private void updateTimedWaves() {

        long now =
            world.getTime();

        if (
            now - lastWaveTime
                < WAVE_INTERVAL
        ) {

            return;
        }

        /*
         * Move to next wave.
         */
        waveNumber++;

        // Surviving zombies, including bosses, remain when a wave is added.

        /*
         * Check whether this is
         * a boss wave.
         */
        boolean bossWave =
            waveNumber % 5 == 0;

        ZombieWave wave;

        /*
         * BOSS WAVE
         */
        if (bossWave) {

            wave =
                new ZombieWave(
                    1,
                    true
                );

        } else {

            /*
             * NORMAL WAVE
             *
             * Normal waves gradually
             * increase in size.
             */
            int zombiesThisWave =
                BASE_ZOMBIES_PER_WAVE
                    + (waveNumber - 1)
                    * ZOMBIES_ADDED_PER_WAVE;

            wave =
                new ZombieWave(
                    zombiesThisWave,
                    false
                );
        }

        /*
         * Zombie spawn points.
         */
        wave.addSpawnPoint(
            new Vector2D(
                300,
                30
            )
        );

        wave.addSpawnPoint(
            new Vector2D(
                WORLD_WIDTH - 300,
                30
            )
        );

        wave.addSpawnPoint(
            new Vector2D(
                220,
                WORLD_HEIGHT / 2
            )
        );

        wave.addSpawnPoint(
            new Vector2D(
                WORLD_WIDTH - 220,
                WORLD_HEIGHT / 2
            )
        );

        world.addWave(wave);

        world.spawnWave(wave);
        addHumanReinforcements();

        lastWaveTime = now;
    }

    /** Each new wave brings two humans of each role, including boss waves. */
    private void addHumanReinforcements() {
        for (int role = 0; role < 6; role++) {
            Vector2D spawn = randomHumanPosition();
            int x = (int) spawn.getX();
            int y = (int) spawn.getY();
            Human human = switch (role % 3) {
                case 0 -> new Medic(x, y);
                case 1 -> new Civilian(x, y);
                default -> new Soldier(x, y);
            };
            world.addCharacter(human);
        }
    }

    /*
     * Updates all simulation behaviour.
     */
    private void updateSimulation() {

        /*
         * Make sure panel size
         * is available.
         */
        if (
            getWidth() <= 0
                || getHeight() <= 0
        ) {

            return;
        }

        /*
         * Update waves.
         */
        // One movement step represents 30 simulation milliseconds. The 1x/2x/5x
        // controls run these steps every 30/15/6 real milliseconds respectively.
        world.advanceTime(30);
        updateTimedWaves();

        long now = world.getTime();
        for (Resource resource : resources) {
            resource.updateRespawn(now);
        }

        var humans =
            world.getHumans();

        var zombies =
            world.getZombies();

        /*
         * UPDATE HUMANS
         */
        for (Human human : humans) {

            human.setEnvironment(buildings, humans);
            human.update(
                getWidth(),
                getHeight(),
                zombies,
                safePoint
            );

            /*
             * Building interaction.
             */
            for (Building building : buildings) {

                if (
                    building.contains(
                        human.getX(),
                        human.getY()
                    )
                ) {

                    building.interact(human);
                }
            }
        }

        /*
         * Store humans that were killed
         * during this update.
         */
        ArrayList<Human> convertedHumans =
            new ArrayList<>();

        /*
         * UPDATE ZOMBIES
         */
        for (Zombie zombie : zombies) {

            zombie.setBuildings(buildings);

            /*
             * Bloater explosion.
             */
            if (
                zombie instanceof Bloater bloater
                    && bloater.shouldExplode(humans)
            ) {

                ArrayList<Human> explosionDeaths =
                    bloater.explode(humans);

                for (
                    Human human
                        : explosionDeaths
                ) {

                    if (
                        !convertedHumans
                            .contains(human)
                    ) {

                        convertedHumans
                            .add(human);
                    }
                }

                continue;
            }

            /*
             * Normal zombie behaviour.
             *
             * MutantBoss also uses this
             * because it extends Zombie.
             */
            Human deadHuman =
                zombie.update(
                    getWidth(),
                    getHeight(),
                    humans,
                    safePoint
                );

            /*
             * Prevent duplicate conversions.
             */
            if (
                deadHuman != null
                    && !convertedHumans
                        .contains(deadHuman)
            ) {

                convertedHumans
                    .add(deadHuman);
            }
        }

        /*
         * Convert dead humans
         * into random zombie types.
         */
        for (
            Human human
                : convertedHumans
        ) {

            int zombieType =
                random.nextInt(4);

            Zombie newZombie;

            if (zombieType == 0) {

                newZombie =
                    new Zombie(
                        human.getX(),
                        human.getY()
                    );

            } else if (zombieType == 1) {

                newZombie =
                    new Runner(
                        human.getX(),
                        human.getY()
                    );

            } else if (zombieType == 2) {

                newZombie =
                    new Stalker(
                        human.getX(),
                        human.getY()
                    );

            } else {

                newZombie =
                    new Bloater(
                        human.getX(),
                        human.getY()
                    );
            }

            world.addCharacter(
                newZombie
            );
        }

        /*
         * Remove dead characters.
         */
        world.update();

        /*
         * SIMULATION END CONDITION
         *
         * If no humans remain,
         * update the screen first.
         */
        if (world.getHumans().isEmpty()) {

            repaint();

            /*
             * Delay the popup until
             * Swing has processed the repaint.
             */
            SwingUtilities.invokeLater(
                () -> endSimulation()
            );

            return;
        }
    }

    /*
     * Stops the simulation when all
     * humans have died.
     *
     * The user can restart or exit.
     */
    private void endSimulation() {

        timer.stop();

        int choice =
            JOptionPane.showConfirmDialog(
                this,
                "Simulation Complete: "
                    + "No Humans Remaining.\n"
                    + "Wave reached: "
                    + waveNumber
                    + "\n\n"
                    + "Would you like to restart "
                    + "the simulation?",
                "Simulation Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );

        /*
         * YES
         *
         * Restart simulation.
         */
        if (
            choice
                == JOptionPane.YES_OPTION
        ) {

            java.awt.Window currentWindow =
                SwingUtilities.getWindowAncestor(
                    this
                );

            if (currentWindow != null) {

                currentWindow.dispose();
            }

            /*
             * Start a completely
             * new simulation.
             */
            try {
                createAndStartSimulation();
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(
                    null,
                    "The replacement simulation could not start:\n"
                        + exception.getMessage(),
                    "Simulation Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }

        } else {

            /*
             * NO
             *
             * Close application.
             */
            System.exit(0);
        }
    }

    /*
     * Starts simulation.
     */
    public void startSimulation() {

        lastWaveTime =
            world.getTime();

        timer.start();
    }

    /*
     * Returns fast-forward controls.
     */
    public FastForward getFastForward() {

        return fastForward;
    }

    /*
     * Calculates how many seconds
     * remain until the next wave.
     */
    private long getNextWaveSeconds() {

        if (lastWaveTime == 0) {

            return WAVE_INTERVAL / 1000;
        }

        long elapsed =
            world.getTime()
                - lastWaveTime;

        long remaining =
            Math.max(
                0,
                WAVE_INTERVAL - elapsed
            );

        return (
            remaining + 999
        ) / 1000;
    }

    /*
     * Draw simulation.
     */
    @Override
    protected void paintComponent(
        Graphics g
    ) {

        super.paintComponent(g);

        /*
         * BACKGROUND
         */
        g.setColor(
            new Color(
                220,
                220,
                220
            )
        );

        g.fillRect(
            0,
            0,
            getWidth(),
            getHeight()
        );

        /*
         * BUILDINGS
         */
        for (
            Building building
                : buildings
        ) {

            building.draw(g);
        }

        /*
         * RESOURCES
         */
        for (
            Resource resource
                : resources
        ) {

            resource.draw(g);
        }

        /*
         * SAFE POINT
         */
        safePoint.draw(g);

        /*
         * CHARACTERS
         */
        for (
            Character character
                : world.getCharacters()
        ) {

            character.draw(g);
        }

        /*
         * STATUS INFORMATION
         */
        int humanCount =
            world.getHumans().size();

        int zombieCount =
            world.getZombies().size();

        long nextWaveSeconds =
            getNextWaveSeconds();

        g.setColor(
            Color.BLACK
        );

        g.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                14
            )
        );

        String humanText =
            "Humans: "
                + humanCount;

        String zombieText =
            "Zombies: "
                + zombieCount;

        String waveText =
            "Wave: "
                + waveNumber;

        String nextWaveText =
            "Next wave: "
                + nextWaveSeconds
                + "s";

        FontMetrics fm =
            g.getFontMetrics();

        int gap = 22;

        int totalWidth =
            fm.stringWidth(
                humanText
            )
                + fm.stringWidth(
                    zombieText
                )
                + fm.stringWidth(
                    waveText
                )
                + fm.stringWidth(
                    nextWaveText
                )
                + gap * 3;

        /*
         * Centre status text.
         */
        int startX =
            (
                getWidth()
                    - totalWidth
            ) / 2;

        int textY = 28;

        int currentX =
            startX;

        /*
         * Humans
         */
        g.drawString(
            humanText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(
                humanText
            ) + gap;

        /*
         * Zombies
         */
        g.drawString(
            zombieText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(
                zombieText
            ) + gap;

        /*
         * Wave
         */
        g.drawString(
            waveText,
            currentX,
            textY
        );

        currentX +=
            fm.stringWidth(
                waveText
            ) + gap;

        /*
         * Next wave timer
         */
        g.drawString(
            nextWaveText,
            currentX,
            textY
        );

        /*
         * BOSS WAVE WARNING
         *
         * Display a large warning
         * during waves:
         *
         * 5, 10, 15, 20...
         */
        if (waveNumber % 5 == 0) {

            g.setColor(
                new Color(
                    120,
                    0,
                    120
                )
            );

            g.setFont(
                new Font(
                    "Arial",
                    Font.BOLD,
                    26
                )
            );

            String bossText =
                "BOSS WAVE";

            FontMetrics bossFm =
                g.getFontMetrics();

            int bossX =
                (
                    getWidth()
                        - bossFm.stringWidth(
                            bossText
                        )
                ) / 2;

            g.drawString(
                bossText,
                bossX,
                65
            );
        }
    }

    /*
     * Creates a completely fresh
     * simulation window.
     *
     * Used at program start and restart.
     */
    private static void createAndStartSimulation() {

        SimulationSettings settings = SimulationSettingsDialog.showDialog(null);
        if (settings == null) {
            return;
        }

        JFrame frame =
            new JFrame(
                "Zombie Survival Simulation"
            );

        Main simulation =
            new Main(settings);

        frame.setLayout(
            new BorderLayout()
        );

        frame.add(
            simulation,
            BorderLayout.CENTER
        );

        /*
         * Fast-forward controls
         * at bottom.
         */
        frame.add(
            simulation.getFastForward(),
            BorderLayout.SOUTH
        );

        frame.setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        frame.pack();

        frame.setLocationRelativeTo(
            null
        );

        frame.setVisible(
            true
        );

        simulation.startSimulation();
    }

    /*
     * Main method.
     */
    public static void main(
        String[] args
    ) {

        SwingUtilities.invokeLater(
            () -> {

                try {

                    createAndStartSimulation();

                } catch (
                    RuntimeException exception
                ) {

                    JOptionPane.showMessageDialog(
                        null,
                        "The simulation could "
                            + "not start:\n"
                            + exception.getMessage(),
                        "Simulation Error",
                        JOptionPane.ERROR_MESSAGE
                    );

                    exception.printStackTrace();
                }
            }
        );
    }
}
