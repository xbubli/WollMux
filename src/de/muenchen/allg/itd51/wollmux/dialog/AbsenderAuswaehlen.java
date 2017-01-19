/* 
 * Dateiname: AbsenderAuswaehlen.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Absenderdaten auswählen Dialog des BKS
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 25.10.2005 | BNK | Erstellung
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 02.11.2005 | BNK | Absenderliste nicht mehr mit Vorname = M* befüllen,
 *                    weil jetzt der TestDJ schon eine Absenderliste
 *                    mit Einträgen hat.
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 03.01.2005 | BNK | Bug korrigiert;  .gridy = x  sollte .gridx = x sein.
 * 19.05.2006 | BNK | [R1898]Wenn die Liste leer ist, dann gleich den PAL Verwalten Dialog aufrufen
 * 26.02.2010 | BED | WollMux-Icon für das Fenster
 * 08.04.2010 | BED | [R52334] Anzeige über DISPLAY-Attribut konfigurierbar
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung
 * einen Dialog zum Auswählen eines Eintrages aus der Persönlichen Absenderliste. Die
 * private-Funktionen dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen
 * werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AbsenderAuswaehlen
{
  /**
   * Default-Wert dafür, wie die Personen in der Absenderliste angezeigt werden
   * sollen, wenn es nicht explizit in der Konfiguration über das DISPLAY-Attribut
   * für eine listbox festgelegt ist. %{Spalte}-Syntax um entsprechenden Wert des
   * Datensatzes einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige
   * "Meier, Hans" etc.
   * 
   * An dieser Stelle einen Default-Wert hardzucodieren (der noch dazu LHM-spezifisch
   * ist!) ist sehr unschön und wurde nur gemacht um abwärtskompatibel zu alten
   * WollMux-Konfigurationen zu bleiben. Sobald sichergestellt ist, dass überall auf
   * eine neue WollMux-Konfiguration geupdatet wurde, sollte man diesen Fallback
   * wieder entfernen.
   */
  private static final String DEFAULT_DISPLAYTEMPLATE =
    "%{Nachname}, %{Vorname} (%{Rolle})";

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "back".
   */
  private ActionListener actionListener_back = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      back();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "editList".
   */
  private ActionListener actionListener_editList = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      editList();
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;

  /**
   * Das JPanel der obersten Hierarchiestufe.
   */
  private JComponent mainPanel;

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  /**
   * Die Listbox mit der persönlichen Absenderliste.
   */
  private JList<Object> palJList;

  /**
   * Gibt an, wie die Suchresultate in der {@link #palJList} angezeigt werden sollen.
   * Der Wert wird in der Konfiguration bei der "listbox" mit ID "suchanfrage" durch
   * Angeben des DISPLAY-Attributs konfiguriert. %{Spalte}-Syntax um entsprechenden
   * Wert des Datensatzes einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige
   * "Meier, Hans" etc.
   */
  private String palDisplayTemplate;

  /**
   * Der dem
   * {@link #AbsenderAuswaehlen(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener)
   * Konstruktor} übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Das ConfigThingy, das diesen Dialog spezifiziert.
   */
  private ConfigThingy myConf;

  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten der Absenderliste spezifiziert.
   */
  private ConfigThingy verConf;

  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten eines Datensatzes der
   * Absenderliste spezifiziert.
   */
  private ConfigThingy abConf;

  /**
   * Überwacht Änderungen in der Auswahl und wählt den entsprechenden Datensatz im
   * DJ.
   */
  private MyListSelectionListener myListSelectionListener =
    new MyListSelectionListener();

  /**
   * Erzeugt einen neuen Dialog.
   * 
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des
   *          "Fenster"-Knotens.
   * @param abConf
   *          das ConfigThingy, das den Dialog zum Bearbeiten eines Datensatzes
   *          beschreibt.
   * @param verConf
   *          das ConfigThingy, das den Absenderliste Verwalten Dialog beschreibt.
   * @param dj
   *          der DatasourceJoiner, der die PAL verwaltet.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der Dialog
   *          geschlossen wurde. Das actionCommand des ActionEvents gibt die Aktion
   *          an, die das Beenden des Dialogs veranlasst hat.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unmöglich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schlüssel fehlt.
   */
  public AbsenderAuswaehlen(ConfigThingy conf, ConfigThingy verConf,
      ConfigThingy abConf, DatasourceJoiner dj, ActionListener dialogEndListener)
      throws ConfigurationErrorException
  {
    this.dj = dj;
    this.myConf = conf;
    this.abConf = abConf;
    this.verConf = verConf;
    this.dialogEndListener = dialogEndListener;
    this.palDisplayTemplate = DEFAULT_DISPLAYTEMPLATE;

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            createGUI();
          }
          catch (Exception x)
          {}
          ;
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Erzeugt das GUI.
   * 
   * @param fensterDesc
   *          die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void createGUI()
  {
    Common.setLookAndFeelOnce();

    palJList = new JList<Object>(new DefaultListModel<Object>());

    String title = L.m("Absender Auswählen (WollMux)");
    closeAction = getAction("abort");

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    // WollMux-Icon für AbsenderAuswaehlen-Frame
    Common.setWollMuxIcon(myFrame);

    mainPanel = Box.createVerticalBox();
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.getContentPane().add(mainPanel);

    addUIElements();

    QueryResults palEntries = dj.getLOS();
    if (palEntries.isEmpty())
    {
      editList();
    }
    else
    {
      setListElements(palJList, dj.getLOS(), palDisplayTemplate);
      selectSelectedDataset(palJList);

      myFrame.pack();
      int frameWidth = myFrame.getWidth();
      int frameHeight = myFrame.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width / 2 - frameWidth / 2;
      int y = screenSize.height / 2 - frameHeight / 2;
      myFrame.setLocation(x, y);
      myFrame.setResizable(false);
      myFrame.setAlwaysOnTop(true);
      myFrame.setVisible(true);
      myFrame.requestFocus();
    }
  }
  
  private void addUIElements()
  {
    Box absenderliste = new Box(BoxLayout.PAGE_AXIS);
    Box buttons = Box.createHorizontalBox();

    mainPanel.add(absenderliste);
    mainPanel.add(buttons);

    JLabel label = new JLabel(L.m("Welchen Absender möchten Sie für Ihre Briefköpfe verwenden ?"));
    JList<Object> list = palJList;
    palDisplayTemplate = "%{Nachname}, %{Vorname} (%{Rolle})";
    
    list.setVisibleRowCount(10);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setLayoutOrientation(JList.VERTICAL);
    list.setPrototypeCellValue("Hello World!");

    list.addListSelectionListener(myListSelectionListener);

    JScrollPane scrollPane = new JScrollPane(list);
    
    ActionListener actionListener = getAction("back");
    if (actionListener != null) 
    {
      list.addMouseListener(new MyActionMouseListener(list, actionListener));
    }
    
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    
    absenderliste.add(label);
    absenderliste.add(Box.createRigidArea(new Dimension(0,5)));
    absenderliste.add(scrollPane);
    
    JButton editButton = new JButton(L.m("Bearbeiten..."));
    editButton.setMnemonic('B');

    actionListener = getAction("editList");
    if (actionListener != null) editButton.addActionListener(actionListener);

    buttons.add(editButton);
    
    Box glue = Box.createHorizontalBox();
    glue.add(Box.createHorizontalGlue());

    buttons.add(glue);

    JButton closeButton = new JButton(L.m("Schließen"));
    closeButton.setMnemonic('C');

    actionListener = getAction("abort");
    if (actionListener != null) closeButton.addActionListener(actionListener);

    buttons.add(closeButton);
  }

  /**
   * Wartet auf Doppelklick und führt dann die actionPerformed() Methode eines
   * ActionListeners aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MyActionMouseListener extends MouseAdapter
  {
    private JList<Object> list;

    private ActionListener action;

    public MyActionMouseListener(JList<Object> list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }

    public void mouseClicked(MouseEvent e)
    {
      if (e.getClickCount() == 2)
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0) return;
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location)) return;
        action.actionPerformed(null);
      }
    }
  }

  /**
   * Übersetzt den Namen einer ACTION in eine Referenz auf das passende
   * actionListener_... Objekt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ActionListener getAction(String action)
  {
    if (action.equals("abort"))
    {
      return actionListener_abort;
    }
    else if (action.equals("back"))
    {
      return actionListener_back;
    }
    else if (action.equals("editList"))
    {
      return actionListener_editList;
    }
    else if (action.equals(""))
    {
      return null;
    }
    else
      Logger.error(L.m("Ununterstützte ACTION: ", action));

    return null;
  }

  /**
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link DJDatasetListElement}-Objekte
   * gewrappt, deren Inhalt entsprechend des übergebenen displayTemplates angezeigt
   * wird.
   * 
   * @param list
   *          die Liste deren Wertliste geändert werden soll
   * @param data
   *          enthält die Datensätze, mit denen die Liste gefüllt werden soll
   * @param displayTemplate
   *          gibt an wie die Datensätze in der Liste als Strings repräsentiert
   *          werden sollen, siehe z.B. {@link #DEFAULT_DISPLAYTEMPLATE}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList<Object> list, QueryResults data, String displayTemplate)
  {
    Object[] elements = new Object[data.size()];
    Iterator<Dataset> iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] =
        new DJDatasetListElement((DJDataset) iter.next(), displayTemplate);
    Arrays.sort(elements, new Comparator<Object>()
    {
      public int compare(Object o1, Object o2)
      {
        return o1.toString().compareTo(o2.toString());
      }
    });

    DefaultListModel<Object> listModel = (DefaultListModel<Object>) list.getModel();
    listModel.clear();
    for (i = 0; i < elements.length; ++i)
      listModel.addElement(elements[i]);
  }

  private void selectSelectedDataset(JList<?> list)
  {
    DefaultListModel<?> listModel = (DefaultListModel<?>) list.getModel();
    for (int i = 0; i < listModel.size(); ++i)
      if (((DJDatasetListElement) listModel.get(i)).getDataset().isSelectedDataset())
        list.setSelectedValue(listModel.get(i), true);
  }

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden wenn
   * kein Eintrag selektiert ist.
   */
  private class MyListSelectionListener implements ListSelectionListener
  {
    public void valueChanged(ListSelectionEvent e)
    {
      @SuppressWarnings("unchecked")
      JList<Object> list = (JList<Object>) e.getSource();
      if (list != palJList) return;

      DJDatasetListElement ele = (DJDatasetListElement) list.getSelectedValue();
      if (ele == null)
        selectSelectedDataset(list);
      else
        ele.getDataset().select();
    }
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    dialogEnd("abort");
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void back()
  {
    dialogEnd("back");
  }

  /**
   * Beendet den Dialog und liefer actionCommand an den dialogEndHandler zurück
   * (falls er nicht null ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this, 0, actionCommand));
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editList()
  {
    ActionListener del =
      new MyDialogEndListener(this, myConf, verConf, abConf, dj, dialogEndListener,
        null);
    dialogEndListener = null;
    abort();
    try
    {
      new PersoenlicheAbsenderlisteVerwalten(verConf, abConf, dj, del);
    }
    catch (ConfigurationErrorException x)
    {
      Logger.error(x);
    }
  }

  private static class MyDialogEndListener implements ActionListener
  {
    private ConfigThingy conf;

    private ConfigThingy abConf;

    private ConfigThingy verConf;

    private DatasourceJoiner dj;

    private ActionListener dialogEndListener;

    private String actionCommand;

    private AbsenderAuswaehlen mySource;

    /**
     * Falls actionPerformed() mit getActionCommand().equals("back") aufgerufen wird,
     * wird ein neuer AbsenderAuswaehlen Dialog mit den übergebenen Parametern
     * erzeugt. Ansonsten wird der dialogEndListener mit actionCommand aufgerufen.
     * Falls actionCommand null ist wird das action command des ActionEvents
     * weitergereicht, der actionPerformed() übergeben wird. Falls actionPerformed ==
     * null wird auch die source weitergereicht, ansonsten wird die übergebene source
     * verwendet.
     */
    public MyDialogEndListener(AbsenderAuswaehlen source, ConfigThingy conf,
        ConfigThingy verConf, ConfigThingy abConf, DatasourceJoiner dj,
        ActionListener dialogEndListener, String actionCommand)
    {
      this.conf = conf;
      this.verConf = verConf;
      this.abConf = abConf;
      this.dj = dj;
      this.dialogEndListener = dialogEndListener;
      this.actionCommand = actionCommand;
      this.mySource = source;
    }

    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals("back"))
        try
        {
          new AbsenderAuswaehlen(conf, verConf, abConf, dj, dialogEndListener);
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      else
      {
        Object source = mySource;
        if (actionCommand == null)
        {
          actionCommand = e.getActionCommand();
          source = e.getSource();
        }
        if (dialogEndListener != null)
          dialogEndListener.actionPerformed(new ActionEvent(source, 0, actionCommand));
      }
    }
  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion auf
   * den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener()
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowClosed(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }

    public void windowDeactivated(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowOpened(WindowEvent e)
    {}
  }

  /**
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren Aufrufe
   * von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt asynchron. Wurde dem
   * Konstruktor ein entsprechender ActionListener übergeben, so wird seine
   * actionPerformed() Funktion aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    // GUI im Event-Dispatching Thread zerstören wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          abort();
        }
      });
    }
    catch (Exception x)
    {/* Hope for the best */}
  }
}
