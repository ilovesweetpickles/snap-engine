package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Form for geographic collocation dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationForm extends JPanel {

    private CollocationFormModel model;

    private SourceProductSelector masterProductSelector;
    private SourceProductSelector slaveProductSelector;

    private JCheckBox renameMasterComponentsCheckBox;
    private JCheckBox renameSlaveComponentsCheckBox;
    private JTextField masterComponentPatternField;
    private JTextField slaveComponentPatternField;
    private JComboBox resamplingComboBox;
    private TargetProductSelector targetProductSelector;

    public CollocationForm(final CollocationFormModel model, TargetProductSelector targetProductSelector,
                           AppContext appContext) {
        this.model = model;

        this.targetProductSelector = targetProductSelector;
        masterProductSelector = new SourceProductSelector(appContext,
                                                          "Master (pixel values are conserved):");
        slaveProductSelector = new SourceProductSelector(appContext,
                                                         "Slave (pixel values are resampled onto the master grid):");
        renameMasterComponentsCheckBox = new JCheckBox("Rename master components:");
        renameSlaveComponentsCheckBox = new JCheckBox("Rename slave components:");
        masterComponentPatternField = new JTextField();
        slaveComponentPatternField = new JTextField();
        resamplingComboBox = new JComboBox(model.getResamplingComboBoxModel());

        slaveProductSelector.getProductNameComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.adaptResamplingComboBoxModel();
            }
        });

        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        };
        renameMasterComponentsCheckBox.addActionListener(listener);
        renameSlaveComponentsCheckBox.addActionListener(listener);

        createComponents();
        bindComponents();
    }

    public void prepareShow() {
        masterProductSelector.initProducts();
        if (masterProductSelector.getProductCount() > 0) {
            masterProductSelector.setSelectedIndex(0);
        }
        slaveProductSelector.initProducts();
        if (slaveProductSelector.getProductCount() > 1) {
            slaveProductSelector.setSelectedIndex(1);
        }
    }

    private void updateUIState() {
        masterComponentPatternField.setEnabled(renameMasterComponentsCheckBox.isSelected());
        slaveComponentPatternField.setEnabled(renameSlaveComponentsCheckBox.isSelected());
    }

    public void prepareHide() {
        masterProductSelector.releaseProducts();
        slaveProductSelector.releaseProducts();
    }

    private void createComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createSourceProductPanel());
        add(createTargetProductPanel());
        add(createRenamingPanel());
        add(createResamplingPanel());
    }

    private void bindComponents() {
        final SwingBindingContext sbc = new SwingBindingContext(model.getValueContainer());

        sbc.bind(masterProductSelector.getProductNameComboBox(), "masterProduct");
        sbc.bind(slaveProductSelector.getProductNameComboBox(), "slaveProduct");
        sbc.bind(renameMasterComponentsCheckBox, "renameMasterComponents");
        sbc.bind(renameSlaveComponentsCheckBox, "renameSlaveComponents");
        sbc.bind(masterComponentPatternField, "masterComponentPattern");
        sbc.bind(slaveComponentPatternField, "slaveComponentPattern");
    }

    private JPanel createSourceProductPanel() {
        final JPanel masterPanel = new JPanel(new BorderLayout(3, 3));
        masterPanel.add(masterProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        masterProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        masterPanel.add(masterProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        masterPanel.add(masterProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final JPanel slavePanel = new JPanel(new BorderLayout(3, 3));
        slavePanel.add(slaveProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        slavePanel.add(slaveProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        slavePanel.add(slaveProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        panel.add(masterPanel);
        panel.add(slavePanel);

        return panel;
    }

    private JPanel createTargetProductPanel() {
        return targetProductSelector.createDefaultPanel();
    }

    private JPanel createRenamingPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Component Renaming"));
        panel.add(renameMasterComponentsCheckBox);
        panel.add(masterComponentPatternField);
        panel.add(renameSlaveComponentsCheckBox);
        panel.add(slaveComponentPatternField);

        return panel;
    }

    private JPanel createResamplingPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Resampling"));
        panel.add(new JLabel("Method:"));
        panel.add(resamplingComboBox);
        panel.add(new JLabel());

        return panel;
    }

}
