/* 
 * Dateiname: SachleitendeVerfuegungenDruckdialog.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Dialog zum Drucken von Sachleitenden Verfügungen
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
 * 09.10.2006 | LUT | Erstellung (basierend auf AbsenderAuswaehlen.java)
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.Verfuegungspunkt;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung
 * einen Dialog zum Drucken von Sachleitenden Verfügungen. Die private-Funktionen
 * dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
 */
public class SachleitendeVerfuegungenDruckdialog
{
  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der Dialog
   * über den Drucken-Knopf geschlossen wird.
   */
  public static final String CMD_SUBMIT = "submit";

  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der Dialog
   * über den Abbrechen oder "X"-Knopf geschlossen wird.
   */
  public static final String CMD_CANCEL = "cancel";

  /**
   * Anzahl der Zeichen, nach der der Text der Verfügungspunkte abgeschnitten wird,
   * damit der Dialog nicht platzt.
   */
  private final static int CONTENT_CUT = 75;

  /**
   * ActionListener für Buttons mit der ACTION "printElement".
   */
  private ActionListener actionListener_printElement = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() instanceof JButton)
        getCurrentSettingsForElement((JButton) e.getSource());
      abort(CMD_SUBMIT);
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "printAll".
   */
  private ActionListener actionListener_printAll = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printOrderAsc = getSelectedPrintOrderAsc();
      getCurrentSettingsForAllElements();
      abort(CMD_SUBMIT);
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      abort(CMD_CANCEL);
    }
  };

  /**
   * ChangeListener für Änderungen an den Spinnern.
   */
  private ChangeListener spinnerChangeListener = new ChangeListener()
  {
    public void stateChanged(ChangeEvent event)
    {
      allElementCountTextField.setText("" + getAllElementCount());
    }
  };

  /**
   * ChangeListener für Änderungen an den ComboBoxen, der eine Änderung des
   * ausgewählten Elements unmöglich macht.
   */
  private ItemListener cboxItemListener = new ItemListener()
  {
    public void itemStateChanged(ItemEvent event)
    {
      Object source = event.getSource();
      if (source != null && source instanceof JComboBox<?>)
      {
        @SuppressWarnings("unchecked")
        JComboBox<String> cbox = (JComboBox<String>) source;
        if (cbox.getSelectedIndex() != 0) cbox.setSelectedIndex(0);
      }
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
  private JPanel mainPanel;

  /**
   * Die Array mit allen comboBoxen, die elementCount beinhalten.
   */
  private List<JSpinner> elementCountSpinner;

  /**
   * Die Array mit allen buttons auf printElement-Actions
   */
  private List<JButton> printElementButtons;

  /**
   * Enthält das TextFeld, das die Summe aller Ausfertigungen anzeigt.
   */
  private JTextField allElementCountTextField;

  /**
   * Die Checkbox zur Reihenfolge des Ausdrucks
   */
  private JCheckBox printOrder;

  /**
   * Der dem
   * {@link #AbsenderAuswaehlen(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener)
   * Konstruktor} übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Vector of Verfuegungspunkt, der die Beschreibungen der Verfügungspunkte enthält.
   */
  private List<Verfuegungspunkt> verfuegungspunkte;

  /**
   * Nach jedem Aufruf von printAll oder printElement enthält diese Methode die
   * aktuelle Liste Einstellungen für die zu druckenden Verfügungspunkte.
   */
  private List<VerfuegungspunktInfo> currentSettings;

  /**
   * Enthält die Information ob die Methode printAll in auf- oder absteigender Reihenfolge drucken soll.
   */
  private boolean printOrderAsc;

  /**
   * Erzeugt einen neuen Dialog.
   * 
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des
   *          "Fenster"-Knotens.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der Dialog
   *          geschlossen wurde. Das actionCommand des ActionEvents gibt die Aktion
   *          an, die das Beenden des Dialogs veranlasst hat.
   * @param verfuegungspunkte
   *          Vector of Verfuegungspunkt, der die Beschreibungen der Verfügungspunkte
   *          enthält.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unmöglich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schlüssel fehlt.
   */
  public SachleitendeVerfuegungenDruckdialog(ConfigThingy conf,
      List<Verfuegungspunkt> verfuegungspunkte, ActionListener dialogEndListener)
      throws ConfigurationErrorException
  {
    this.verfuegungspunkte = verfuegungspunkte;
    this.dialogEndListener = dialogEndListener;
    this.currentSettings = new ArrayList<VerfuegungspunktInfo>();
    this.printOrder = new JCheckBox();

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
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Enthält die Einstellungen, die zu einem Verfügungspunkt im Dialog getroffen
   * wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class VerfuegungspunktInfo
  {
    public final int verfPunktNr;

    public final short copyCount;

    public final boolean isDraft;

    public final boolean isOriginal;

    public VerfuegungspunktInfo(int verfPunktNr, short copyCount, boolean isDraft,
        boolean isOriginal)
    {
      this.verfPunktNr = verfPunktNr;
      this.copyCount = copyCount;
      this.isDraft = isDraft;
      this.isOriginal = isOriginal;
    }

    public String toString()
    {
      return "VerfuegungspunktInfo(verfPunkt=" + verfPunktNr + ", copyCount="
        + copyCount + ", isDraft=" + isDraft + ", isOriginal=" + isOriginal + ")";
    }
  }

  /**
   * Liefert die aktuellen in diesem Dialog getroffenen Einstellung zur Reihenfolge des Ausdrucks.
   * @return true falls in aufsteigender Reihenfloge gedruckt werden soll, false sonst.
   *
   * @author ulrich.kitzinger
   */
  public boolean getPrintOrderAsc()
  {
    return printOrderAsc;
  }

  /**
   * Liefert die aktuellen in diesem Dialog getroffenen Einstellungen als Liste von
   * VerfuegungspunktInfo-Objekten zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public List<VerfuegungspunktInfo> getCurrentSettings()
  {
    return currentSettings;
  }

  /**
   * Erzeugt das GUI.
   * 
   * @param fensterDesc
   *          die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  private void createGUI()
  {
    Common.setLookAndFeelOnce();

    // element
    elementCountSpinner = new ArrayList<JSpinner>();
    printElementButtons = new ArrayList<JButton>();

    String title = L.m("WollMux Komfortdruck");
    closeAction = actionListener_abort;

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    // WollMux-Icon für das Fenster
    Common.setWollMuxIcon(myFrame);

    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.getContentPane().add(mainPanel);
    
    mainPanel.add(addUIElements(), BorderLayout.CENTER);

    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
    myFrame.setAlwaysOnTop(true);
    myFrame.requestFocus();
  }
  
  private JComponent addUIElements() 
  {
    Box panel = new Box(BoxLayout.PAGE_AXIS);
    
    Box panelHeaders = Box.createHorizontalBox();

    JLabel label = new JLabel(L.m("Ausdrucke"));
    label.setPreferredSize(new Dimension(243, 28));
    panelHeaders.add(label);

    panelHeaders.add(Box.createRigidArea(new Dimension(5, 0)));
    
    JLabel label1 = new JLabel(L.m("Kopien"));
    panelHeaders.add(label1);

    panelHeaders.add(Box.createHorizontalGlue());

    panel.add(panelHeaders);
    
    for (Verfuegungspunkt vp : verfuegungspunkte)
    {
      List<String> zuleitungszeilen = vp.getZuleitungszeilen();

      Box panelVerfuegungspunkt = Box.createHorizontalBox();

      JComboBox<String> combo = new JComboBox<String>();
      combo.addItemListener(cboxItemListener);
      combo.setPreferredSize(new Dimension(243, 24));
      combo.setMinimumSize(new Dimension(243, 24));
      combo.setMaximumSize(new Dimension(243, 24));
      
      Vector<String> content = new Vector<String>();
      content.add(cutContent(vp.getHeading()));
      if (zuleitungszeilen.size() > 0)
        content.add(cutContent(L.m("------- Zuleitung an --------")));
      for (String zuleitung : zuleitungszeilen) 
      {
        content.add(cutContent(zuleitung));
      }
      
      combo.setModel(new DefaultComboBoxModel<String>(content));

      panelVerfuegungspunkt.add(combo);

      panelVerfuegungspunkt.add(Box.createRigidArea(new Dimension(5, 0)));

      JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 0));
      spinner.setPreferredSize(new Dimension(45, 0));
      spinner.setMaximumSize(new Dimension(45, 24));
      SpinnerNumberModel model =
          new SpinnerNumberModel(vp.getNumberOfCopies(), 0, 50, 1);
      spinner.setModel(model);
      spinner.addChangeListener(spinnerChangeListener);
      elementCountSpinner.add(spinner);
      panelVerfuegungspunkt.add(spinner);

      panelVerfuegungspunkt.add(Box.createHorizontalGlue());

      JButton button = new JButton(L.m("Drucken"));
      button.addActionListener(actionListener_printElement);
      printElementButtons.add(button);
      panelVerfuegungspunkt.add(button);

      panel.add(panelVerfuegungspunkt);
      
      panel.add(Box.createRigidArea(new Dimension(0, 5)));
    }
    
    panel.add(new JSeparator(SwingConstants.HORIZONTAL));
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    
    Box panelAllElements = Box.createHorizontalBox();

    JLabel label3 = new JLabel(L.m("Summe aller Ausdrucke"));
    label3.setPreferredSize(new Dimension(243, 24));
    label3.setMaximumSize(new Dimension(243, 24));
    panelAllElements.add(label3);

    panelAllElements.add(Box.createRigidArea(new Dimension(5, 0)));

    JTextField textField = new JTextField("" + getAllElementCount());
    textField.setPreferredSize(new Dimension(45, 24));
    textField.setMinimumSize(new Dimension(45, 24));
    textField.setMaximumSize(new Dimension(45, 24));
    textField.setEditable(false);
    textField.setHorizontalAlignment(JTextField.CENTER);
    allElementCountTextField = textField;
    panelAllElements.add(textField);

    panelAllElements.add(Box.createHorizontalGlue());
    
    panel.add(panelAllElements);

    panel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelReihenfolge = Box.createHorizontalBox();

    JCheckBox checkBox = new JCheckBox();
    checkBox.setText("Ausdruck in umgekehrter Reihenfolge");
    printOrder = checkBox;
    panelReihenfolge.add(checkBox);
    
    panelReihenfolge.add(Box.createHorizontalGlue());

    panel.add(panelReihenfolge);

    panel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelButtons = Box.createHorizontalBox();

    JButton button1 = new JButton(L.m("Abbrechen"));
    button1.setMnemonic('A');
    button1.addActionListener(actionListener_abort);
    panelButtons.add(button1);

    panelButtons.add(Box.createHorizontalGlue());

    JButton button2 = new JButton(L.m("Alle drucken"));
    button2.setMnemonic('D');
    button2.addActionListener(actionListener_printAll);
    panelButtons.add(button2);

    panel.add(panelButtons);

    return panel;
  }

  /**
   * Wenn value mehr als CONTENT_CUT Zeichen besitzt, dann wird eine gekürzte Form
   * von value zurückgeliefert (mit "..." ergänzt) oder ansonsten value selbst.
   * 
   * @param value
   *          der zu kürzende String
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String cutContent(String value)
  {
    if (value.length() > CONTENT_CUT)
      return value.substring(0, CONTENT_CUT) + " ...";
    else
      return value;
  }

  /**
   * Berechnet die Summe aller Ausfertigungen aller elementCountSpinner.
   */
  private int getAllElementCount()
  {
    int count = 0;
    for (int i = 0; i < elementCountSpinner.size(); i++)
    {
      count += new Integer(elementCountSpinner.get(i).getValue().toString()).intValue();
    }
    return count;
  }

  /**
   * Beendet den Dialog und informiert den dialogEndListener (wenn dieser != null
   * ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort(String cmdStr)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this, 0, cmdStr));
  }

  /**
   * Ermittelt ob in aufsteigender oder absteigender Reihenfolge gedruckt werden soll.
   * @return true falls in aufsteigender Reihenfolge gedruckt werden soll, false sonst
   *
   * @author ulrich.kitzinger
   */
  private boolean getSelectedPrintOrderAsc(){
    return !printOrder.isSelected();
  }

  /**
   * Löscht currentSettings und schreibt für alle Verfügungspunkte entsprechende
   * VerfuegungspunktInfo-Objekte nach currentSettings.
   * 
   * @author christoph.lutz
   */
  private void getCurrentSettingsForAllElements()
  {
    currentSettings.clear();
    for (int verfPunkt = 1; verfPunkt <= verfuegungspunkte.size(); ++verfPunkt)
    {
      currentSettings.add(getVerfuegungspunktInfo(verfPunkt));
    }
  }

  /**
   * Bestimmt die Nummer des Verfügungspunktes, dem JButton button zugeordnet ist und
   * schreibt dessen VerfuegungspunktInfo als einziges Element nach currentSettings.
   * 
   * @author christoph.lutz
   */
  private void getCurrentSettingsForElement(JButton button)
  {
    currentSettings.clear();
    for (int i = 0; i < printElementButtons.size(); i++)
    {
      if (printElementButtons.get(i) == button)
      {
        currentSettings.add(getVerfuegungspunktInfo(i + 1));
      }
    }
  }

  /**
   * Ermittelt die Druckdaten (Verfügungspunkt, Anzahl-Ausfertigungen, ...) zum
   * Verfügungspunkt verfPunkt und liefert sie als VerfuegungspunktInfo-Objekt
   * zurück.
   * 
   * @author christoph.lutz
   */
  private VerfuegungspunktInfo getVerfuegungspunktInfo(int verfPunkt)
  {
    // Anzahl der Kopien bestimmen:
    short numberOfCopies = 0;
    try
    {
      numberOfCopies =
        new Short(elementCountSpinner.get(verfPunkt - 1).getValue().toString()).shortValue();
    }
    catch (Exception e)
    {
      Logger.error(L.m("Kann Anzahl der Ausfertigungen nicht bestimmen."), e);
    }

    boolean isDraft = (verfPunkt == verfuegungspunkte.size());
    boolean isOriginal = (verfPunkt == 1);

    return new VerfuegungspunktInfo(verfPunkt, numberOfCopies, isDraft, isOriginal);
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

  public static void main(String[] args) throws Exception
  {
    UNO.init();
    WollMuxSingleton.initialize(UNO.defaultContext);
    List<VerfuegungspunktInfo> info =
      SachleitendeVerfuegung.callPrintDialog(UNO.XTextDocument(UNO.desktop.getCurrentComponent()));
    for (VerfuegungspunktInfo v : info)
    {
      System.out.println(v);
    }
    System.exit(0);
  }
}
