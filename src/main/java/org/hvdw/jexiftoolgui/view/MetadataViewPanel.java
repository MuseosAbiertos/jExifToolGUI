package org.hvdw.jexiftoolgui.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.hvdw.jexiftoolgui.MyVariables;
import org.hvdw.jexiftoolgui.TablePasteAdapter;
import org.hvdw.jexiftoolgui.Utils;
import org.hvdw.jexiftoolgui.controllers.SQLiteJDBC;
import org.hvdw.jexiftoolgui.facades.SystemPropertyFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.TransferHandler;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static org.hvdw.jexiftoolgui.facades.SystemPropertyFacade.SystemPropertyKey.LINE_SEPARATOR;

/**
 * Handle the metadata settings table which defines the metadata that may be
 * applied to photos.<br>
 * Usage: new Metadata();<br>
 * Gets initial metadata list and saves updated metadata list from/to
 * program preferences.<br>
 * Original @author Dennis Damico
 * Modified by Harry van der Wolf
 */
public class MetadataViewPanel extends JDialog implements TableModelListener {
    private final static Logger logger = LoggerFactory.getLogger(Utils.class);


    private JScrollPane aScrollPanel;
    private JTable metadataTable;
    private JButton saveasButton;
    private JButton saveButton;
    private JButton helpButton;
    private JPanel metadatapanel;
    private JPanel buttonpanel;
    private JComboBox customSetcomboBox;
    private JButton closeButton;

    private JPanel rootpanel;

    /**
     * Object to enable paste into the table.
     */
    private static TablePasteAdapter myPasteHandler = null;

    /**
     * Is object initialization complete?
     */
    private boolean initialized = false;

    private JPopupMenu myPopupMenu = new JPopupMenu();

    /**
     * Create the on-screen Metadata table.
     */
    public MetadataViewPanel() {

        setContentPane(metadatapanel);
        setModal(true);
        getRootPane().setDefaultButton(saveasButton);
        this.setIconImage(Utils.getFrameIcon());

        // Button listeners
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // First check whether we have data
                String haveData = haveTableData();
                if ("complete".equals(haveData)) {
                    String[] name_writetype = {"", ""};
                    String setName = customSetcomboBox.getSelectedItem().toString();
                    if (setName.isEmpty()) { // We never saved anything or did not select anything
                        name_writetype = getSetName();
                        if (!name_writetype[0].isEmpty()) {
                            // We have a name
                            saveMetadata(name_writetype[0], name_writetype[1]);
                        } // If not: do nothing
                    } else { // we have a setname and simply overwrite
                        saveMetadata(setName, "update");
                    }
                } // if incomplete (or empty): do nothing
            }
        });

        saveasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // First check whether we have data
                String haveData = haveTableData();
                if ("complete".equals(haveData)) {
                    String[] name_writetype = getSetName();
                    if (!"".equals(name_writetype[0])) {
                        saveMetadata(name_writetype[0], name_writetype[1]);
                    }
                }// if incomplete (or empty): do nothing
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
        customSetcomboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fillTable();
            }
        });
    }

    /*
    / Check if we have rows in our table, and if so: do table cells screen_label and tag contain data
    / returns haveData: being no data (no rows), empty (1-x empty rows), incomplete (some rows missing screen_label and/or tag), complete
    / sets  MyVariables.settableRowsCells
     */
    private String haveTableData() {
        String haveData = "";
        DefaultTableModel model = ((DefaultTableModel) (metadataTable.getModel()));
        List<String> cells = new ArrayList<String>();
        List<List> tableRowsCells = new ArrayList<List>();
        /*List<tableRow> tableRowList = new ArrayList<tableRow>();
        tableRowList.add( new tableRow(1, 2, 3)); */
        String[][] trows = new String[255][3];

        if ((model.getRowCount() == 0)) {
            haveData = "no data";
        } else {
            int incomplete_counter = 0;
            for (int count = 0; count < model.getRowCount(); count++) {
                cells = new ArrayList<String>();
                cells.add(model.getValueAt(count, 0).toString());
                trows[count][0] = model.getValueAt(count, 0).toString();
                cells.add(model.getValueAt(count, 1).toString());
                trows[count][1] = model.getValueAt(count, 1).toString();
                cells.add(model.getValueAt(count, 2).toString());
                trows[count][2] = model.getValueAt(count, 2).toString();
                logger.trace("row {} data: {}, {}, {}", count, model.getValueAt(count, 0).toString(), model.getValueAt(count, 1).toString(), model.getValueAt(count, 2).toString());
                if ((!cells.get(0).isEmpty()) && (!cells.get(1).isEmpty())) {
                    tableRowsCells.add(cells);
                } else if ((!cells.get(0).isEmpty()) && (cells.get(1).isEmpty()) || (cells.get(0).isEmpty()) && (!cells.get(1).isEmpty())) {
                    incomplete_counter += 1;
                }
                if (incomplete_counter == 0) {
                    haveData = "complete";
                } else if (incomplete_counter <= model.getColumnCount()) {
                    haveData = "incomplete";
                /*} else if (incomplete_counter == model.getRowCount()) {
                    haveData = "empty"; */
                }
                logger.trace("haveData: {}, incomplete_counter: {}", haveData, incomplete_counter);

                if ("complete".equals(haveData)) {
                    MyVariables.settableRowsCells(tableRowsCells);
                } else if ("incomplete".equals(haveData)) {
                    JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.incompletetext"), ResourceBundle.getBundle("translations/program_strings").getString("acv.incompletetitle"), JOptionPane.ERROR_MESSAGE);
                }
                //logger.info("row {} data: {}, {}, {}", String.valueOf(count), model.getValueAt(count, 0).toString(), model.getValueAt(count, 1).toString(), model.getValueAt(count, 2).toString());
            }
        }

        return haveData;
    }

    /*
    / get the name for the custom set. Being asked when nothing was selected or no setname available, or when "Save As" was clicked
    / returns String[] with Name and writetype (update or insert)
    /
     */
    private String[] getSetName() {
        String[] name_writetype = {"", ""};
        String chosenName = JOptionPane.showInputDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.inpdlgname"));
        if (!(chosenName == null)) { // null on cancel
            String[] checkNames = loadCurrentSets("");
            // We expect a new name here
            for (String name : checkNames) {
                if (name.equals(chosenName)) {
                    // We have this name already (so why did user select "Save As" anyway?
                    int result = JOptionPane.showConfirmDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.overwrite") + chosenName + "\"?",
                            ResourceBundle.getBundle("translations/program_strings").getString("acv.overwriteconfirm"), JOptionPane.OK_CANCEL_OPTION);
                    if (result == 0) { // user OK with overwrite
                        name_writetype[0] = chosenName;
                        name_writetype[1] = "update";
                        break;
                    } else {
                        name_writetype[0] = "";
                        name_writetype[1] = "";
                    }
                } else { // We have a new name
                    name_writetype[0] = chosenName;
                    name_writetype[1] = "insert";
                }
            }
        } else { //Cancelled; no name
            name_writetype[0] = "";
            name_writetype[1] = "";
        }
        return name_writetype;
    }

    /**
     * Add a popup menu to the metadata table to handle insertion and
     * deletion of rows.
     */
    private void addPopupMenu() {

        myPopupMenu = new JPopupMenu();
        // Menu items.
        JMenuItem menuItem = new JMenuItem(ResourceBundle.getBundle("translations/program_strings").getString("mct.insert")); // index 0
        menuItem.addActionListener(new MyMenuHandler());
        myPopupMenu.add(menuItem);

        menuItem = new JMenuItem(ResourceBundle.getBundle("translations/program_strings").getString("mct.delete"));           // index 1
        menuItem.addActionListener(new MyMenuHandler());
        myPopupMenu.add(menuItem);
        /*myPopupMenu.addSeparator();                     // index 2

        JMenuItem menuItem0 = new JMenuItem(HIER_CMD);  // index 3
        menuItem0.addActionListener(new MyMenuHandler());
        myPopupMenu.add(menuItem0);
        myPopupMenu.addSeparator();

        menuItem = new JMenuItem(UPDATE_CMD);
        menuItem.addActionListener(new MyMenuHandler());
        myPopupMenu.add(menuItem); */

        // Listen for menu invocation.
        metadataTable.addMouseListener(new MyPopupListener());
    }

    /**
     * Retrieve the metadata from the SQL database
     * via a selection popup and save it into the table model.
     */
    private void loadMetadata() {
        // Use my own sql load statement
    }

    /**
     * Save the metadata into the SQL database
     * input setName, writetype being insert or update, completeness
     * In case of insert we insert the name in CustomMetaDataSet and insert the values in CustomMetaDataSetLines
     * In case of update we simply remove all records from the setName from CustomMetaDataSetLines
     * and then insert new lines
     */
    private void saveMetadata(String setName, String writetype) {
        String sql;
        String queryresult;
        int queryresultcounter = 0;

        // We get the rowcells from the getter without checking. We would not be here if we had not checked already
        //List<String> cells = new ArrayList<String>();
        List<List> tableRowsCells = MyVariables.gettableRowsCells();
        for (List<String> cells : tableRowsCells) {
            logger.trace(cells.toString());
        }
        if ("insert".equals(writetype)) {
            sql = "insert into CustomMetadataset(customset_name) values('" + setName + "')";
            queryresult = SQLiteJDBC.insertUpdateQuery(sql);
            if (!"".equals(queryresult)) { //means we have an error
                JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorinserttext") + " " + setName, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorinserttitel"), JOptionPane.ERROR_MESSAGE);
                queryresultcounter += 1;
            } else {
                logger.info("no of tablerowcells {}", tableRowsCells.size());
                // inserting the setName went OK, now all the table fields
                for (List<String> cells : tableRowsCells) {
                    sql = "insert into CustomMetadatasetLines(customset_name, screen_label, tag, default_value) "
                            + "values('" + setName + "','" + cells.get(0) + "','" + cells.get(1) + "','" + cells.get(2) + "')";
                    logger.info(sql);
                    queryresult = SQLiteJDBC.insertUpdateQuery(sql);
                    if (!"".equals(queryresult)) { //means we have an error
                        JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorinserttext") + " " + setName, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorinserttitel"), JOptionPane.ERROR_MESSAGE);
                        queryresultcounter += 1;
                    }
                }
            }
            //Finally
            if (queryresultcounter == 0) {
                loadCurrentSets("fill_combo");
                customSetcomboBox.setSelectedItem(setName);
                JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.saved") + " " + setName, ResourceBundle.getBundle("translations/program_strings").getString("acv.savedb"), JOptionPane.INFORMATION_MESSAGE);
            }
        } else { // update
            queryresult = SQLiteJDBC.deleteCustomSetRows(setName);
            if (!"".equals(queryresult)) { //means we have an error
                JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorupdatetext") + " " + setName, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorupdatetitle"), JOptionPane.ERROR_MESSAGE);
            } else {
                logger.info("no of tablerowcells {}", tableRowsCells.size());
                // deleting the old records went OK, now (re)insert the rows
                for (List<String> cells : tableRowsCells) {
                    sql = "insert into CustomMetadatasetLines(customset_name, screen_label, tag, default_value) "
                            + "values('" + setName + "','" + cells.get(0) + "','" + cells.get(1) + "','" + cells.get(2) + "')";
                    logger.info(sql);
                    queryresult = SQLiteJDBC.insertUpdateQuery(sql);
                    if (!"".equals(queryresult)) { //means we have an error
                        JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorinserttext") + " " + setName, ResourceBundle.getBundle("translations/program_strings").getString("acv.errorinserttitel"), JOptionPane.INFORMATION_MESSAGE);
                        queryresultcounter += 1;
                    }
                }
            }
            //Finally
            if (queryresultcounter == 0) {
                loadCurrentSets("fill_combo");
                customSetcomboBox.setSelectedItem(setName);
                JOptionPane.showMessageDialog(rootpanel, ResourceBundle.getBundle("translations/program_strings").getString("acv.saved") + " " + setName, ResourceBundle.getBundle("translations/program_strings").getString("acv.savedb"), JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }


    /**
     * Reorder rows in the table model to match the on-screen view so that
     * rows get inserted or deleted where the user intends.
     */
    private void reorderRows() {
        DefaultTableModel model = ((DefaultTableModel) (metadataTable.getModel()));
        Vector modelRows = model.getDataVector();
        Vector screenRows = new Vector();
        for (int i = 0; i < modelRows.size(); i++) {
            screenRows.add(modelRows.get(metadataTable.convertRowIndexToModel(i)));
        }
        Vector headings = new Vector(Arrays.asList(ResourceBundle.getBundle("translations/program_strings").getString("mct.columnlabel"), ResourceBundle.getBundle("translations/program_strings").getString("mct.columntag"), ResourceBundle.getBundle("translations/program_strings").getString("mct.columndefault")));
        model.setDataVector(screenRows, headings);
        //metadataTable.getColumnModel().getColumn(2).setMaxWidth(100); // Checkbox.
    }

    /**
     * Save changed metadata to program preferences whenever the
     * metadata table changes.
     *
     * @param e the change event.
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        // Ignore events until initialization completes.
        /*if (initialized) {
            MyTableModel model = ((MyTableModel) (metadataTable.getModel()));
            Vector theMetadata = model.getDataVector();

            // Convert the vector to a linked list format
            LinkedList<MetadataRecord> theMetadataList = new LinkedList<>();
            for (int i=0; i<theMetadata.size(); i++) {
                Vector vv = (Vector) theMetadata.get(i);
                Object[] oo = (Object[])vv.toArray();
                if (oo[0] == null) oo[0] = "";
                if (oo[1] == null) oo[1] = "";
                if (oo[2] == null) oo[2] = "";
                if ( (boolean) vv.get(3)) oo[3] = "1"; else oo[3] = "0";

                MetadataRecord md = new MetadataRecord(
                        oo[0].toString(), oo[1].toString(), oo[2].toString(), oo[3].toString());
                theMetadataList.add(md);
            }
            // Tell prefs and the main window that metadata has changed.
            myPrefs.setMetadata(theMetadataList);
            ImageTagger.getTaggerWindow().metadataChanged();
        } */
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        metadatapanel = new JPanel();
        metadatapanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        metadatapanel.setMinimumSize(new Dimension(600, 300));
        metadatapanel.setPreferredSize(new Dimension(800, 500));
        buttonpanel = new JPanel();
        buttonpanel.setLayout(new GridLayoutManager(1, 5, new Insets(5, 5, 5, 5), -1, -1));
        metadatapanel.add(buttonpanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        saveasButton = new JButton();
        this.$$$loadButtonText$$$(saveasButton, this.$$$getMessageFromBundle$$$("translations/program_strings", "mct.buttonsaveas"));
        buttonpanel.add(saveasButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveButton = new JButton();
        this.$$$loadButtonText$$$(saveButton, this.$$$getMessageFromBundle$$$("translations/program_strings", "mct.buttonsave"));
        buttonpanel.add(saveButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        helpButton = new JButton();
        this.$$$loadButtonText$$$(helpButton, this.$$$getMessageFromBundle$$$("translations/program_strings", "mct.buttonhelp"));
        buttonpanel.add(helpButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        customSetcomboBox = new JComboBox();
        buttonpanel.add(customSetcomboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        closeButton = new JButton();
        this.$$$loadButtonText$$$(closeButton, this.$$$getMessageFromBundle$$$("translations/program_strings", "mct.buttonclose"));
        buttonpanel.add(closeButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aScrollPanel = new JScrollPane();
        metadatapanel.add(aScrollPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        metadataTable = new JTable();
        metadataTable.setAutoCreateRowSorter(true);
        metadataTable.setMinimumSize(new Dimension(600, 300));
        metadataTable.setPreferredScrollableViewportSize(new Dimension(700, 400));
        aScrollPanel.setViewportView(metadataTable);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return metadatapanel;
    }

    /**
     * When the metadata table is sorted update the model rows to match
     * the screen view.  This will generate a tableChanged event.
     */
    class MySortListener implements RowSorterListener {
        /**
         * Metadata table was sorted.
         */
        @Override
        public void sorterChanged(RowSorterEvent e) {
            if (e.getType() == RowSorterEvent.Type.SORTED) {
                reorderRows();
            }
        }
    }


    /**
     * Listen for popup menu invocation.
     * Need both mousePressed and mouseReleased for cross platform support.
     */
    class MyPopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // Enable or disable the "Delete Rows" menu item
                // depending on whether a row is selected.
                myPopupMenu.getComponent(1).setEnabled(
                        metadataTable.getSelectedRowCount() > 0);

                myPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Handle popup menu commands.
     */
    class MyMenuHandler implements ActionListener {
        /**
         * Popup menu actions.
         *
         * @param e the menu event.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            if (item.getText().equals(ResourceBundle.getBundle("translations/program_strings").getString("mct.insert"))) {
                insertRows(e);
            } else if (item.getText().equals(ResourceBundle.getBundle("translations/program_strings").getString("mct.delete"))) {
                deleteRows(e);
            }
        }

        /**
         * Insert one or more rows into the table at the clicked row.
         *
         * @param e
         */
        private void insertRows(ActionEvent e) {
            // Get the insert request.
            DefaultTableModel model = ((DefaultTableModel) (metadataTable.getModel()));
            int theRow = metadataTable.getSelectedRow();
            int rowCount = metadataTable.getSelectedRowCount();

            // Case of click outside table: Add one row to end of table.
            if ((theRow == -1) && (rowCount == 0)) {
                theRow = model.getRowCount() - 1;
                rowCount = 1;
            }

            // Reorder the rows in the model to match the user's view of them.
            reorderRows();

            // Add the new rows beneath theRow.
            for (int i = 0; i < rowCount; i++) {
                model.insertRow(theRow + 1 + i, new Object[]{"", "", "", false});
            }
        }

        /**
         * Delete one or more rows in the table at the clicked row.
         *
         * @param e
         */
        private void deleteRows(ActionEvent e) {
            // Get the delete request.
            DefaultTableModel model = ((DefaultTableModel) (metadataTable.getModel()));
            int theRow = metadataTable.getSelectedRow();
            int rowCount = metadataTable.getSelectedRowCount();

            // Reorder the rows in the model to match the user's view of them.
            reorderRows();

            // Delete the new rows beneath theRow.
            for (int i = 0; i < rowCount; i++) {
                model.removeRow(theRow);
            }
        }

    }


    /**
     * Enable paste into the metadata table.
     */
    private void enablePaste() {

        myPasteHandler = new TablePasteAdapter(metadataTable);
    }

    /**
     * Enable reordering of rows by drag and drop.
     */
    private void enableDragging() {
        metadataTable.setDragEnabled(true);
        metadataTable.setDropMode(DropMode.USE_SELECTION);
        metadataTable.setTransferHandler(new MyDraggingHandler());
    }

    class MyDraggingHandler extends TransferHandler {
        public MyDraggingHandler() {
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        /**
         * Build a transferable consisting of a string containing values of all
         * the columns in the selected row separated by newlines.
         * Replace null values with "". Delete the selected row.
         *
         * @param source
         * @return
         */
        @Override
        protected Transferable createTransferable(JComponent source) {
            int row = ((JTable) source).getSelectedRow();
            DefaultTableModel model = (DefaultTableModel) ((JTable) (source)).getModel();
            String rowValue = "";
            for (int c = 0; c < 2; c++) { // 3 columns in metadata table.
                Object aCell = (Object) model.getValueAt(row, c);
                if (aCell == null) aCell = "";
                rowValue = rowValue + aCell + "\n";
            }
            model.removeRow(row);
            return new StringSelection(rowValue);
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return true;
        }

        /**
         * Insert the transferable at the selected row. If row is outside the
         * table then insert at end of table.
         *
         * @param support
         * @return
         */
        @Override
        public boolean importData(TransferSupport support) {
            try {
                JTable jt = (JTable) support.getComponent();
                DefaultTableModel model = ((DefaultTableModel) (jt.getModel()));
                int row = jt.getSelectedRow();
                // Insert at end?
                if (row == -1) {
                    row = model.getRowCount();
                }

                // Get the transferable string and convert to vector.
                // When there are blank cells we may not get 3 items from the
                // transferable. Supply blanks.
                String rowValue = (String) support.getTransferable().
                        getTransferData(DataFlavor.stringFlavor);
                String[] rowValues = rowValue.split("\n");
                Vector aRow = new Vector();
                for (int c = 0; c < 2; c++) { // 3 columns in metadata table, 3 strings.
                    if (c < rowValues.length) {
                        aRow.add(rowValues[c]);
                    } else {
                        aRow.add("");
                    }
                }

                model.insertRow(row, aRow);
            } catch (Exception ex) {
                logger.info("import data failure {}", ex);
            }
            return super.importData(support);
        }
    }

    private String[] loadCurrentSets(String check) {
        String sqlsets = SQLiteJDBC.getdefinedCustomSets();
        logger.info("retrieved CustomSets: " + sqlsets);
        String[] views = sqlsets.split("\\r?\\n"); // split on new lines
        if ("fill_combo".equals(check)) { // We use this one also for "Save As" to check if user has chosen same name
            customSetcomboBox.setModel(new DefaultComboBoxModel(views));
        }
        return views;
    }

    private void fillTable() {
        DefaultTableModel model = ((DefaultTableModel) (metadataTable.getModel()));
        model.setColumnIdentifiers(new String[]{ResourceBundle.getBundle("translations/program_strings").getString("mct.columnlabel"),
                ResourceBundle.getBundle("translations/program_strings").getString("mct.columntag"),
                ResourceBundle.getBundle("translations/program_strings").getString("mct.columndefault")});
        model.setRowCount(0);
        Object[] row = new Object[1];

        logger.trace("combo numberof {}, selecteditem {}", customSetcomboBox.getItemCount(), customSetcomboBox.getSelectedItem());
        if ((customSetcomboBox.getItemCount() == 0) || (customSetcomboBox.getSelectedItem() == null) || ("".equals(customSetcomboBox.getSelectedItem()))) { // We do not have stored custom sets yet.
            // Try to set the defaults for artist, credit and copyrights in the table if prefs available
            String[] ArtCredCopyPrefs = Utils.checkPrefsArtistCreditsCopyRights();
            // artist 0; credit 1; copyrights 2

            model.addRow(new Object[]{"Creator", "exif:creator", ArtCredCopyPrefs[0]});
            model.addRow(new Object[]{"Creator", "xmp-dc:creator", ArtCredCopyPrefs[0]});
            model.addRow(new Object[]{"Credits", "xmp-dc:rights", ArtCredCopyPrefs[1]});
            model.addRow(new Object[]{"Copyrights", "exif:copyright", ArtCredCopyPrefs[2]});
            model.addRow(new Object[]{"Copyrights", "xmp:copyright", ArtCredCopyPrefs[2]});
        } else {
            String setName = customSetcomboBox.getSelectedItem().toString();
            String sql = "select screen_label, tag, default_value from custommetadatasetLines where customset_name='" + setName.trim() + "'";
            String queryResult = SQLiteJDBC.generalQuery(sql);
            if (queryResult.length() > 0) {
                String[] lines = queryResult.split(SystemPropertyFacade.getPropertyByKey(LINE_SEPARATOR));

                for (String line : lines) {
                    //String[] cells = lines[i].split(":", 2); // Only split on first : as some tags also contain (multiple) :
                    String[] cells = line.split("\\t", 4);
                    model.addRow(new Object[]{cells[0], cells[1], cells[2]});
                }
            }

        }
    }

    // The  main" function of this class
    public void showDialog(JPanel rootPanel) {
        //setSize(750, 500);
        //make sure we have a "local" rootpanel
        rootpanel = rootPanel;
        setTitle(ResourceBundle.getBundle("translations/program_strings").getString("mct.title"));
        pack();
        double x = getParent().getBounds().getCenterX();
        double y = getParent().getBounds().getCenterY();
        //setLocation((int) x - getWidth() / 2, (int) y - getHeight() / 2);
        setLocationRelativeTo(null);

        addPopupMenu();
        enablePaste();
        enableDragging();
        loadCurrentSets("fill_combo");
        fillTable();

        // Initialization is complete.
        initialized = true;

        setVisible(true);
    }
}
