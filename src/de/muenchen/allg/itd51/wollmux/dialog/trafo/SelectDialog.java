/*
 * Dateiname: Select.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion Select mit mehrere Wenn-Dann Feldes.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 26.03.2015 | Simona Loi | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Simona Loi (I23)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;

/**
 * Erlaubt die Bearbeitung der Funktion eines Select-Feldes.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1), Simona Loi (I23)
 */
public class SelectDialog extends TrafoDialog
{
  /**
   * Auswahl des zu vergleichenden Feldes.
   */
  private JComboBox<String> fieldSelector;

  /**
   * Der Button um einzelnen Fall zu löschen.
   */
  private JButton cancelIf;

  /**
   * Das Panel, das die JCasePanels enthält.
   */
  private JPanel casesPanel;

  /**
   * Das Objekt, das den Startinhalt des Dialogs spezifiziert (und am Ende verwendet
   * wird, um den Rückgabewert zu speichern).
   */
  private TrafoDialogParameters params;

  /**
   * Wenn der Dialog angezeigt wird ist dies der zugehörige JDialog.
   */
  private JDialog myDialog;

  /**
   * Der WindowListener, der an {@link #myDialog} hängt.
   */
  private MyWindowListener oehrchen;

  /**
   * Die Liste, die die JCasePanel enthält.
   */
  private List<JCasePanel> list;

  /**
   * Der Sonst-Teil.
   */

  private ConditionalResult elseResult = new ConditionalResult();


  private static class ConditionalResult
  {
    /**
     * Texteingabe für den Dann. Ist niemals null.
     */
    public TextComponentTags text;

    /**
     * ScrollPane in der sich {@link #text} befindet. Niemals null.
     */
    public JScrollPane scrollPane;

  }

  public SelectDialog(TrafoDialogParameters params)
  {
    this.params = params;
    if (!params.isValid || params.conf == null || params.fieldNames == null
      || params.fieldNames.size() == 0) throw new IllegalArgumentException();

    params.isValid = false; // erst bei Beendigung mit Okay werden sie wieder valid

    fieldSelector = new JComboBox<String>(new Vector<String>(params.fieldNames));
    fieldSelector.setEditable(false);

    cancelIf = new JButton("-");

    casesPanel = new JPanel();
    casesPanel.setLayout(new BoxLayout(casesPanel, BoxLayout.Y_AXIS));

    list = new ArrayList<>();

    parse(params.conf, params.fieldNames);

  }

  private static class JCasePanel extends JPanel
  {

    /**
     * Kanitverstan.
     */
    private static final long serialVersionUID = -3752064852698886087L;

    private static class TestType
    {
      public String label;

      public String func;

      public TestType(String label, String func)
      {
        this.label = label;
        this.func = func;
      }

      public String toString()
      {
        return label;
      }
    }

    /**
     * Die Einträge für {@link #testSelector}.
     */
    private static final TestType[] testTypes =
      {
        new TestType(L.m("genau ="), "STRCMP"),
        new TestType(L.m("numerisch ="), "NUMCMP"),
        new TestType(L.m("numerisch <"), "LT"),
        new TestType(L.m("numerisch <="), "LE"),
        new TestType(L.m("numerisch >"), "GT"),
        new TestType(L.m("numerisch >="), "GE"),
        new TestType(L.m("regulärer A."), "MATCH") };

    /**
     * Auswahl des zu vergleichenden Feldes.
     */
    private JComboBox<String> fieldSelector;

    private JButton cancelIf;

    /**
     * Auswahl zwischen "" und "nicht".
     */
    private JComboBox<String> notSelector;

    /**
     * Auswahl des anzuwendenden Tests.
     */
    private JComboBox<TestType> testSelector;

    /**
     * Eingabefeld für den Vergleichswert.
     */
    private JTextField compareTo;

    /**
     * Der Dann-Teil.
     */
    private ConditionalResult thenResult = new ConditionalResult();

    /**
     * Der Radio Button um die Fälle zu selektieren.
     */
    private JRadioButton radio;


    /**
     * Erzeugt eine Dialog-Komponente, die mit den Werten aus conf vorbelegt ist,
     * wobei fieldNames die angebotenen Feldnamen als Strings enthält. Der oberste
     * Knoten von conf ist ein beliebiger Bezeichner (typischwerweise der
     * Funktionsname).
     * 
     * @throws IllegalArgumentException
     *           falls conf nicht verstanden wird.
     * 
     * @author Matthias Benkmann (D-III-ITD D.10), Simona Loi (I23)
     */
    public JCasePanel(ConfigThingy conf, List<String> fieldNames, JComboBox<String> fieldSelector,
        JButton cancelIf)
    {
      this.fieldSelector = fieldSelector;
      this.cancelIf = cancelIf;

      if (conf.count() == 2)
      {
        Iterator<ConfigThingy> iter = conf.iterator();
        ConfigThingy ifConf = iter.next();
        ConfigThingy thenConf = iter.next();
        if (thenConf.getName().equals("THEN"))
        {
          parseCondition(ifConf, fieldNames);
          parseThen(thenConf, thenResult, fieldNames);
          buildGUI(fieldNames); // GUI Elemente in this einfügen (wir erben ja von JPanel).
          return;
        }
      }
    }

    public ConditionalResult getThenResult()
    {
      return thenResult;
    }

    private void buildGUI(List<String> fieldNames)
    {
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      Box ifBox = Box.createHorizontalBox();
      ifBox.setBorder(new EmptyBorder(10, 8, 0, 8));
      this.add(ifBox);

      radio = new JRadioButton("");
      radio.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e) {
          cancelIf.setEnabled(true);
        }
      });
      ifBox.add(radio);
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(new JLabel(L.m("falls")));
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(notSelector);
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(testSelector);
      ifBox.add(Box.createHorizontalStrut(10));
      ifBox.add(compareTo);
      
      Box thenBox = Box.createHorizontalBox();
      thenBox.setBorder(new EmptyBorder(10, 8, 0, 8));
      thenBox.add(new JLabel(L.m("dann")));
      thenBox.add(Box.createHorizontalStrut(450));
      this.add(thenBox);
      this.add(Box.createVerticalStrut(5));
      Box scrollBox = Box.createHorizontalBox();
      scrollBox.add(Box.createHorizontalStrut(38));
      scrollBox.add(thenResult.scrollPane);
      this.add(scrollBox);
    }


    /**
     * Erzeugt {@link #fieldSelector}, {@link #notSelector}, {@link #testSelector}
     * und {@link #compareTo} auf Basis von conf. Falls das Vergleichsfeld nicht in
     * fieldNames gelistet ist wird es hinzugefügt. genau = STRCMP(VALUE "feld"
     * "vergleichswert") numerisch = NUMCMP(VALUE "feld" "vergleichswert") numerisch <
     * LT(VALUE "feld" "vergleichswert") numerisch > GT(VALUE "feld"
     * "vergleichswert") numerisch <= LE(VALUE "feld" "vergleichswert") numerisch >=
     * GE(VALUE "feld" "vergleichswert") regulärer A. MATCH(VALUE "feld"
     * "vergleichswert")
     * 
     * @throws IllegalArgumentException
     *           falls conf nicht verstanden wird.
     */
    private void parseCondition(ConfigThingy conf, List<String> fieldNames)
    {
      notSelector = new JComboBox<String>(new String[] {
        "", L.m("nicht") });
      if (conf.getName().equals("NOT"))
      {
        try
        {
          conf = conf.getFirstChild();
        }
        catch (NodeNotFoundException e)
        {
          throw new IllegalArgumentException(e);
        }
        notSelector.setSelectedIndex(1);
      }

      testSelector = new JComboBox<TestType>(testTypes);
      determineTest: while (true)
      {
        for (int i = 0; i < testTypes.length; ++i)
        {
          if (testTypes[i].func.equals(conf.getName()))
          {
            testSelector.setSelectedItem(testTypes[i]);
            break determineTest;
          }
        }
        throw new IllegalArgumentException();
      }

      if (conf.count() == 2)
      {
        try
        {
          ConfigThingy value = conf.getFirstChild();
          if (value.getName().equals("VALUE") && value.count() == 1
            && value.getFirstChild().count() == 0)
          {
            String compareConf = conf.getLastChild().toString();
            compareTo = new JTextField(compareConf, 20);
            String fieldName = value.toString();
            Iterator<String> iter = fieldNames.iterator();
            findFieldName: while (true)
            {
              for (int i = 0; iter.hasNext(); ++i)
              {
                if (fieldName.equals(iter.next()))
                {
                  fieldSelector.setSelectedIndex(i);
                  break findFieldName;
                }
              }
              fieldNames.add(fieldName);
              fieldSelector.addItem(fieldName);
              fieldSelector.setSelectedItem(fieldName);
              break findFieldName;
            }
          }
          else
            throw new IllegalArgumentException();
        }
        catch (NodeNotFoundException e)
        {
          throw new IllegalArgumentException(e);
        }
      }
      else
        throw new IllegalArgumentException();
    }

    /**
     * Initialisiert res anhand von conf (was THEN Knoten sein muss).
     * 
     * @param fieldNames
     *          die Feldnamen, die in eventuellen Subpanels angeboten werden sollen.
     * 
     * @throws IllegalArgumentException
     *           falls conf nicht verstanden wird.
     * 
     * @author Matthias Benkmann (D-III-ITD D.10), Simona Loi (I23)
     */
    private void parseThen(ConfigThingy conf, ConditionalResult res,
        List<String> fieldNames)
    {
      try
      {
        if (conf.count() == 1
          && (conf.getName().equals("THEN")))
        {
          ConfigThingy innerConf = conf.getFirstChild();

          JTextArea textArea = new JTextArea(3, 40);
          textArea.setLineWrap(true);
          res.scrollPane =
            new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
          res.text = new TextComponentTags(textArea);

          if (innerConf.count() == 0 || innerConf.getName().equals("CAT"))
          {
            if (innerConf.getName().equals("CAT"))
              res.text.setContent(TextComponentTags.CAT_VALUE_SYNTAX, innerConf);
            else
              textArea.setText(innerConf.toString());
            return;
          }
        }
      }
      catch (NodeNotFoundException e)
      {
        throw new IllegalArgumentException(e);
      }

      throw new IllegalArgumentException();
    }

    /**
     * Liefert ein frisches ConfigThingy, das die von diesem Panel repräsentierte
     * Trafo darstellt. Oberster Knoten ist immer "IF".
     * 
     * @author Matthias Benkmann (D-III-ITD D.10), Simona Loi (I23)
     */
    public ConfigThingy getConf()
    {
      ConfigThingy conf = new ConfigThingy("IF");
      ConfigThingy conditionConf = conf;
      if (notSelector.getSelectedIndex() == 1) conditionConf = conf.add("NOT");

      TestType test = (TestType) testSelector.getSelectedItem();
      conditionConf = conditionConf.add(test.func);
      conditionConf.add("VALUE").add(fieldSelector.getSelectedItem().toString());
      conditionConf.add(compareTo.getText());

      ConfigThingy thenConf = conf.add("THEN");
      thenConf.addChild(thenResult.text.getContent(TextComponentTags.CAT_VALUE_SYNTAX));
      return conf;
    }

    public boolean isSelected() {
       return this.radio.isSelected();
    }
  }

  private ConfigThingy getElseConf() {
    ConfigThingy elseConf = new ConfigThingy("ELSE");
    elseConf.addChild(elseResult.text.getContent(TextComponentTags.CAT_VALUE_SYNTAX));
    return elseConf;
  }

  /**
   * Aktualisiert {@link #params},conf anhand des aktuellen Dialogzustandes und
   * setzt params,isValid auf true.
   * 
   */
  private void updateTrafoConf()
  {
    params.conf = new ConfigThingy(params.conf.getName());
    for(JCasePanel pan : list)
      params.conf.addChild(pan.getConf());
    params.conf.addChild(getElseConf());
    params.isValid = true;
  }

  private List<Action> makeInsertThenActions (List<String> fieldNames,
    final List<JCasePanel> casesPan) {
        List<Action> actions = new Vector<Action>();
        Iterator<String> iter = fieldNames.iterator();
        while (iter.hasNext())
        {
          final String name = iter.next();
          Action action = new AbstractAction(name)
          {
            private static final long serialVersionUID = -9123184290299840565L;

            public void actionPerformed(ActionEvent e)
            {
              for(JCasePanel pan : casesPan) {
                if(pan.isSelected()) {
                  pan.getThenResult().text.insertTag(name);
                }
              }
            }
          };
          actions.add(action);
        }
        return actions;
  }


  /**
   * Fügt {@link #selectPanel} in dialog ein und zeigt ihn an.
   * 
   * @param dialog
   * @author Matthias Benkmann (D-III-ITD D.10), Simona Loi (I23)
   */
  private void show(String windowTitle, JDialog dialog)
  {
    dialog.setAlwaysOnTop(true);
    dialog.setTitle(windowTitle);
    oehrchen = new MyWindowListener();
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(oehrchen);

    JPanel myPanel = new JPanel();
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
    myPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
    dialog.add(myPanel);

    Box titleBox = Box.createHorizontalBox();
    Border border =
      BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
        L.m("Fallunterscheidung für"));
    border = new CompoundBorder(border, new EmptyBorder(2, 5, 5, 5));
    titleBox.setBorder(border);
    titleBox.add(fieldSelector);
    myPanel.add(titleBox);

    JScrollPane scrollPane = new JScrollPane(casesPanel);
    scrollPane.setBorder(null);
    myPanel.add(scrollPane);

    myPanel.add(Box.createVerticalStrut(10));

    Box elseBox = Box.createHorizontalBox();
    elseBox.setBorder(new EmptyBorder(10, 8, 0, 8));
    elseBox.add(new JLabel(L.m("Sonst")));
    elseBox.add(Box.createHorizontalStrut(500));
    myPanel.add(elseBox);
    myPanel.add(Box.createVerticalStrut(10));

    myPanel.add(elseResult.scrollPane);
    
    Box lowerButtons = Box.createHorizontalBox();
    lowerButtons.setBorder(new EmptyBorder(10, 4, 5, 4));
    myPanel.add(lowerButtons);
    
    JButton cancel = new JButton(L.m("Abbrechen"));
    cancel.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        abort();
      }
    });

    cancelIf.setEnabled(false);
    cancelIf.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        deleteCondition();
      }
    });

    JButton newIf = new JButton("+");
    newIf.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertNewCondition();
      }
    });

    JPotentiallyOverlongPopupMenuButton butt =
        new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
            makeInsertThenActions(params.fieldNames , list));

    butt.setFocusable(false);

    JButton insert = new JButton(L.m("OK"));
    insert.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        updateTrafoConf();
        abort();
      }
    });
    lowerButtons.add(cancel);
    lowerButtons.add(Box.createRigidArea(new Dimension(15,0)));
    lowerButtons.add(cancelIf);
    lowerButtons.add(Box.createRigidArea(new Dimension(4,0)));
    lowerButtons.add(newIf);
    lowerButtons.add(Box.createRigidArea(new Dimension(4,0)));
    lowerButtons.add(butt);
    lowerButtons.add(Box.createRigidArea(new Dimension(10,0)));
    lowerButtons.add(Box.createHorizontalGlue());
    lowerButtons.add(insert);
    this.myDialog = dialog;
    repack();
  }

  protected void insertNewCondition()
  {
    try
    {
      ConfigThingy  ifConf = params.conf.getFirstChild();
      JCasePanel pan = new JCasePanel(ifConf, params.fieldNames, fieldSelector,
        cancelIf);
      this.casesPanel.add(pan);
      repack();
      list.add(pan);
    }
    catch (NodeNotFoundException e)
    {
      throw new IllegalArgumentException();
    }
  }

  protected void deleteCondition()
  {
    Iterator<JCasePanel> i = list.iterator();
    while (i.hasNext()) {
      JCasePanel pan = i.next();
      if(pan.isSelected()) {
        this.casesPanel.remove(pan);
        this.casesPanel.validate();
        this.casesPanel.repaint();
        list.remove(pan);
        i = list.iterator();
      }
    }
    this.myDialog.pack();
    cancelIf.setEnabled(false);
  }

  private void parse(ConfigThingy conf, List<String> fieldNames)
  {
    if (!conf.getName().equals("SELECT")) throw new IllegalArgumentException();
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      conf = iter.next();
      if (conf.getName().equals("IF")){
        JCasePanel caseIf = new JCasePanel(conf, params.fieldNames,
          fieldSelector, cancelIf);
        casesPanel.add(caseIf);
        list.add(caseIf);
      }
      else if (conf.getName().equals("ELSE")) {
        try
        {
          ConfigThingy elseConf = conf.getFirstChild();
          JTextArea textArea = new JTextArea(3, 40);
          textArea.setLineWrap(true);
          elseResult.scrollPane =
            new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
          elseResult.text = new TextComponentTags(textArea);
          if (elseConf.count() == 0 || elseConf.getName().equals("CAT"))
          {
            if (elseConf.getName().equals("CAT"))
              elseResult.text.setContent(TextComponentTags.CAT_VALUE_SYNTAX, elseConf);
            else
              textArea.setText(elseConf.toString());
            return;
          }
        }
        catch (NodeNotFoundException e)
        {
          throw new IllegalArgumentException(e);
        }
      } else {
        throw new IllegalArgumentException();
      }
    }
  }

  /**
   * Führt myDialog.pack() aus (falls myDialog nicht null) und setzt ihn sichtbar in
   * der Mitte des Bildschirms.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  private void repack()
  {
    if (myDialog == null) return;
    myDialog.setVisible(false);
    myDialog.pack();
    int frameWidth = myDialog.getWidth();
    int frameHeight = myDialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myDialog.setLocation(x, y);
    myDialog.setVisible(true);
  }

  public void show(String windowTitle, Dialog owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  public void show(String windowTitle, Frame owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  public TrafoDialogParameters getExitStatus()
  {
    return params;
  }

  private void abort()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen nötig, damit der Dialog gc'ed werden kann. Die Befehle sorgen dafür,
     * dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager) indirekt über
     * den JFrame den MailMerge kennt.
     */
    if (myDialog != null)
    {
      myDialog.removeWindowListener(oehrchen);
      myDialog.getContentPane().remove(0);
      myDialog.setJMenuBar(null);

      myDialog.dispose();
      myDialog = null;
    }

    if (params.closeAction != null)
      params.closeAction.actionPerformed(new ActionEvent(this, 0, ""));
  }

  public void dispose()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            abort();
          }
          catch (Exception x)
          {}
          ;
        }
      });
    }
    catch (Exception x)
    {}
  };

  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      abort();
    }

    public void windowClosed(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowDeactivated(WindowEvent e)
    {}

  }

  public static void main(String[] args) throws Exception
  {
    ConfigThingy funConf =
      new ConfigThingy(
        "SELECT",
        "IF(STRCMP(VALUE \"Elemente\", \"Erde\") THEN(\"braun\"))" +
        "IF(STRCMP(VALUE \"Elemente\", \"Wasser\") THEN(\"blau\"))" +
        "ELSE(\"rot\")");
    Vector<String> fieldNames = new Vector<String>();
    fieldNames.add("Select:Elemente");
    fieldNames.add("Erde");
    fieldNames.add("Wasser");
    fieldNames.add("braun");
    fieldNames.add("blau");
    final TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = funConf;
    params.fieldNames = fieldNames;
    params.closeAction = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (params.isValid)
          System.out.println(params.conf.stringRepresentation());
        else
          System.out.println("ABORTED!");
      }
    };
    SelectDialog dialog = new SelectDialog(params);
    dialog.show("Select-Test", (Dialog) null);
  }

}
