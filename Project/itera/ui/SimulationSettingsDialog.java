package itera.ui;

import itera.simulation.SimulationSettings;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

/** Two-step introduction and setup screen shown before a simulation is created. */
@SuppressWarnings("serial")
public final class SimulationSettingsDialog extends JDialog {

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JSpinner humans = new JSpinner(new SpinnerNumberModel(
        SimulationSettings.DEFAULT_HUMANS, 1,
        SimulationSettings.MAX_STARTING_POPULATION, 1));
    private final JSpinner zombies = new JSpinner(new SpinnerNumberModel(
        SimulationSettings.DEFAULT_ZOMBIES, 0,
        SimulationSettings.MAX_STARTING_POPULATION, 1));
    private SimulationSettings settings;

    private SimulationSettingsDialog(Frame owner) {
        super(owner, "Simulation Setup", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        cardPanel.add(createIntroductionPanel(), "information");
        cardPanel.add(createSettingsPanel(), "settings");
        add(cardPanel);
        pack();
        setMinimumSize(new Dimension(520, 360));
        setLocationRelativeTo(owner);
    }

    public static SimulationSettings showDialog(Frame owner) {
        SimulationSettingsDialog dialog = new SimulationSettingsDialog(owner);
        dialog.setVisible(true);
        return dialog.settings;
    }

    private JPanel createIntroductionPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.add(new JLabel("<html><h1>Zombie Survival: How it works</h1></html>"),
            BorderLayout.NORTH);

        JTextArea explanation = new JTextArea(
            "Your goal is to keep humans alive for as long as possible.\n\n"
                + "• Civilians, soldiers and medics roam the world. Soldiers can collect "
                + "weapons; medics can collect medicine; everyone can use food.\n"
                + "• Injured humans can shelter and recover at the safe point. Zombies cannot enter it.\n"
                + "• Zombies hunt unprotected humans. A killed human becomes a new zombie.\n"
                + "• A stronger zombie wave arrives every 15 seconds; every fifth wave is a boss wave.\n\n"
                + "Next, choose the populations that begin this simulation.");
        explanation.setEditable(false);
        explanation.setOpaque(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setFont(new JLabel().getFont());
        panel.add(explanation, BorderLayout.CENTER);

        JButton next = new JButton("Choose populations");
        next.addActionListener(event -> cards.show(cardPanel, "settings"));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(next);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        panel.add(new JLabel("<html><h1>Starting populations</h1></html>"), constraints);
        constraints.gridwidth = 1;
        constraints.gridy++;
        panel.add(new JLabel("Humans (minimum 1):"), constraints);
        constraints.gridx = 1;
        panel.add(humans, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        panel.add(new JLabel("Zombies (minimum 0):"), constraints);
        constraints.gridx = 1;
        panel.add(zombies, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        panel.add(new JLabel("Each population is limited to 100 for a smooth simulation."), constraints);

        JButton back = new JButton("Back");
        back.addActionListener(event -> cards.show(cardPanel, "information"));
        JButton start = new JButton("Start simulation");
        start.addActionListener(event -> saveSettings());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(back);
        buttons.add(start);
        constraints.gridy++;
        constraints.anchor = GridBagConstraints.EAST;
        panel.add(buttons, constraints);
        return panel;
    }

    private void saveSettings() {
        try {
            settings = new SimulationSettings(
                ((Number) humans.getValue()).intValue(),
                ((Number) zombies.getValue()).intValue());
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                "Invalid simulation settings", JOptionPane.ERROR_MESSAGE);
        }
    }
}
