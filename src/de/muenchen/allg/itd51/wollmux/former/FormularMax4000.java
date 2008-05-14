/*
 * Dateiname: FormularMax4000.java
 * Projekt  : WollMux
 * Funktion : Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 * 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), version 1.0.
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
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 03.08.2006 | BNK | Erstellung
 * 08.08.2006 | BNK | Viel Arbeit reingesteckt.
 * 28.08.2006 | BNK | kommentiert
 * 31.08.2006 | BNK | Code-Editor-Fenster wird jetzt in korrekter Gr��e dargestellt
 *                  | Das Hauptfenster passt sein Gr��e an, wenn Steuerelemente dazukommen oder verschwinden
 * 06.09.2006 | BNK | Hoch und Runterschieben funktionieren jetzt.
 * 19.10.2006 | BNK | Quelltexteditor nicht mehr in einem eigenen Frame
 * 20.10.2006 | BNK | R�ckschreiben ins Dokument erfolgt jetzt automatisch.
 * 26.10.2006 | BNK | Magische gender: Syntax unterst�tzt. 
 * 30.10.2006 | BNK | Men�struktur ge�ndert; Datei/Speichern (unter...) hinzugef�gt
 * 05.02.2007 | BNK | [R5214]Formularmerkmale entfernen hat fast leere Formularnotiz �briggelassen
 * 11.04.2007 | BNK | [R6176]Nicht-WM-Bookmarks killen
 *                  | Nicht-WM-Bookmarks killen Funktion derzeit auskommentiert wegen Zerst�rung von Referenzen
 * 10.07.2007 | BNK | [P1403]abort() verbessert, damit FM4000 gemuellentsorgt werden kann
 * 19.07.2007 | BNK | [R5406]Views und Teile der Views k�nnen nach Benutzerwunsch ein- oder ausgeblendet werden
 *                  | �nderung der Men�struktur (Einf�hrung Ansicht und Bearbeiten Men�, Einf�gen wandert nach Bearbeiten)
 *                  | JSplitPane besser initialisiert, um verschieben des Dividers zu verbessern.
 * 01.08.2007 | BNK | FunctionTester eingebaut      
 * 10.12.2007 | BNK | [R11302]intelligentere Behandlung von Leerzeichen am Ende von gender-Dropdown-Listen                             
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XInterface;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Container;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.DropdownFormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.TextRange;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Visitor;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionTester;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;

/**
 * Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormularMax4000
{
  public static final String STANDARD_TAB_NAME = L.m("Reiter");

  /*
   * Die Namen der Parameter, die die Gender-Trafo erwartet. ACHTUNG! Diese m�ssen
   * exakt mit den Parametern der Gender()-Funktion aus der WollMux-Konfig
   * �bereinstimmen. Insbesondere d�rfen sie nicht �bersetzt werden, ohne dass die
   * Gender()-Funktion angepasst wird. Und falls die Gender()-Funktion ge�ndert wird,
   * dann funktionieren existierende Formulare nicht mehr.
   */
  private static final String[] GENDER_TRAFO_PARAMS = new String[] {
    "Falls_Anrede_HerrN", "Falls_Anrede_Frau", "Falls_sonstige_Anrede", "Anrede" };

  /**
   * Regex f�r Test ob String mit Buchstabe oder Underscore beginnt. ACHTUNG! Das .*
   * am Ende ist notwendig, da String.matches() immer den ganzen String testet.
   */
  private static final String STARTS_WITH_LETTER_RE = "^[a-zA-Z_].*";

  /**
   * Der Standard-Formulartitel, solange kein anderer gesetzt wird.
   */
  private static final String GENERATED_FORM_TITLE =
    L.m("Generiert durch FormularMax 4000");

  /**
   * Maximale Anzahl Zeichen f�r ein automatisch generiertes Label.
   */
  private static final int GENERATED_LABEL_MAXLENGTH = 30;

  /**
   * Wird als Label gesetzt, falls kein sinnvolles Label automatisch generiert werden
   * konnte.
   */
  private static final String NO_LABEL = "";

  /**
   * Wird tempor�r als Label gesetzt, wenn kein Label ben�tigt wird, weil es sich nur
   * um eine Einf�gestelle handelt, die nicht als Formularsteuerelement erfasst
   * werden soll.
   */
  private static final String INSERTION_ONLY = "<<InsertionOnly>>";

  /**
   * URL des Quelltexts f�r den Standard-Empf�ngerauswahl-Tab.
   */
  private final URL EMPFAENGER_TAB_URL =
    this.getClass().getClassLoader().getResource(
      "data/empfaengerauswahl_controls.conf");

  /**
   * URL des Quelltexts f�r die Standardbuttons f�r einen mittleren Tab.
   */
  private final URL STANDARD_BUTTONS_MIDDLE_URL =
    this.getClass().getClassLoader().getResource("data/standardbuttons_mitte.conf");

  /**
   * URL des Quelltexts f�r die Standardbuttons f�r den letzten Tab.
   */
  private final URL STANDARD_BUTTONS_LAST_URL =
    this.getClass().getClassLoader().getResource("data/standardbuttons_letztes.conf");

  /**
   * Beim Import neuer Formularfelder oder Checkboxen schaut der FormularMax4000 nach
   * speziellen Hinweisen/Namen/Eintr�gen, die diesem Muster entsprechen. Diese
   * Zusatzinformationen werden herangezogen um Labels, IDs und andere Informationen
   * zu bestimmen.
   * 
   * >>>>Eingabefeld<<<<: Als "Hinweis" kann "Label<<ID>>" angegeben werden und
   * wird beim Import entsprechend ber�cksichtigt. Wird nur "<<ID>>" angegeben, so
   * markiert das Eingabefeld eine reine Einf�gestelle (insertValue oder
   * insertContent) und beim Import wird daf�r kein Formularsteuerelement erzeugt.
   * Wird ID ein "glob:" vorangestellt, so wird gleich ein insertValue-Bookmark
   * erstellt.
   * 
   * >>>>>Eingabeliste/Dropdown<<<<<: Als "Name" kann "Label<<ID>>" angegeben
   * werden und wird beim Import ber�cksichtigt. Als Spezialeintrag in der Liste kann "<<Freitext>>"
   * eingetragen werden und signalisiert dem FM4000, dass die ComboBox im Formular
   * auch die Freitexteingabe erlauben soll. Wie bei Eingabefeldern auch ist die
   * Angabe "<<ID>>" ohne Label m�glich und signalisiert, dass es sich um eine
   * reine Einf�gestelle handelt, die kein Formularelement erzeugen soll. Wird als
   * "Name" die Spezialsyntax "<<gender:ID>>" verwendet, so wird eine reine
   * Einf�gestelle erzeugt, die mit einer Gender-TRAFO versehen wird, die abh�ngig
   * vom Formularfeld ID einen der Werte des Dropdowns ausw�hlt, und zwar bei "Herr"
   * oder "Herrn" den ersten Eintrag, bei "Frau" den zweiten Eintrag und bei allem
   * sonstigen den dritten Eintrag. Hat das Dropdown nur 2 Eintr�ge, so wird im
   * sonstigen Fall das Feld ID untransformiert �bernommen. Falls vorhanden werden
   * bis zu N-1 Leerzeichen am Ende eines Eintrages der Dropdown-Liste entfernt,
   * wobei N die Anzahl der Eintr�ge ist, die bis auf folgende Leerzeichen identisch
   * zu diesem Eintrag sind. Dies erm�glicht es, das selbe Wort mehrfach in die Liste
   * aufzunehmen.
   * 
   * >>>>>Checkbox<<<<<: Bei Checkboxen kann als "Hilfetext" "Label<<ID>>"
   * angegeben werden und wird beim Import entsprechend ber�cksichtigt.
   * 
   * Technischer Hinweis: Auf dieses Pattern getestet wird grunds�tzlich der String,
   * der von {@link DocumentTree.FormControl#getDescriptor()} geliefert wird.
   * 
   */
  private static final Pattern MAGIC_DESCRIPTOR_PATTERN =
    Pattern.compile("\\A(.*)<<(.*)>>\\z");

  /**
   * Pr�fix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen,
   * dass ein insertValue anstatt eines insertFormValue erzeugt werden soll.
   */
  private static final String GLOBAL_PREFIX = "glob:";

  /**
   * Pr�fix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen,
   * dass ein insertFormValue mit Gender-TRAFO erzeugt werden soll.
   */
  private static final String GENDER_PREFIX = "gender:";

  /**
   * Der {@link IDManager}-Namensraum f�r die IDs von {@link FormControlModel}s.
   */
  public static final Integer NAMESPACE_FORMCONTROLMODEL = new Integer(0);

  /**
   * Der {@link IDManager}-Namensraum f�r die DB_SPALTE-Angaben von
   * {@link InsertionModel}s.
   */
  public static final Integer NAMESPACE_DB_SPALTE = new Integer(1);

  /**
   * ActionListener f�r Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der FM4000 geschlossen
   * wurde.
   */
  private ActionListener abortListener = null;

  /**
   * Das Haupt-Fenster des FormularMax4000.
   */
  private JFrame myFrame;

  /**
   * Oberster Container der FM4000 GUI-Elemente. Wird direkt in die ContentPane von
   * myFrame gesteckt.
   */
  private JSplitPane mainContentPanel;

  /**
   * Die normale Gr��e des Dividers von {@link #mainContentPanel}.
   */
  private int defaultDividerSize;

  /**
   * Oberster Container f�r den Quelltexteditor. Wird direkt in die ContentPane von
   * myFrame gesteckt.
   */
  private JPanel editorContentPanel;

  /**
   * Der �bercontainer f�r die linke H�lfte des FM4000.
   */
  private LeftPanel leftPanel;

  /**
   * Der �bercontainer f�r die rechte H�lfte des FM4000.
   */
  private RightPanel rightPanel;

  /**
   * Ein JPanel mit minimaler und bevorzugter Gr��e 0, das f�r die rechte Seite des
   * FM4000 verwendet wird, wenn diese ausgeblendet sein soll.
   */
  private JPanel nonExistingRightPanel;

  /**
   * Beschreibt die aktuellen Sichtbarkeitseinstellungen des Benutzers.
   */
  private ViewVisibilityDescriptor viewVisibilityDescriptor =
    new ViewVisibilityDescriptor();

  /**
   * GUI zum interaktiven Zusammenbauen und Testen von Funktionen.
   */
  private FunctionTester functionTester = null;

  /**
   * Der Titel des Formulars.
   */
  private String formTitle = GENERATED_FORM_TITLE;

  /**
   * Das TextDocumentModel, zu dem das Dokument doc geh�rt.
   */
  private TextDocumentModel doc;

  /**
   * Verwaltet die IDs von Objekten.
   * 
   * @see #NAMESPACE_FORMCONTROLMODEL
   */
  private IDManager idManager = new IDManager();

  /**
   * Verwaltet die FormControlModels dieses Formulars.
   */
  private FormControlModelList formControlModelList;

  /**
   * Verwaltet die {@link InsertionModel}s dieses Formulars.
   */
  private InsertionModelList insertionModelList;

  /**
   * Verwaltet die {@link GroupModel}s dieses Formulars.
   */
  private GroupModelList groupModelList;

  /**
   * Funktionsbibliothek, die globale Funktionen zur Verf�gung stellt.
   */
  private FunctionLibrary functionLibrary;

  /**
   * Verantwortlich f�r das �bersetzen von TRAFO, PLAUSI und AUTOFILL in
   * {@link FunctionSelection}s.
   */
  private FunctionSelectionProvider functionSelectionProvider;

  /**
   * Verantwortlich f�r das �bersetzen von Gruppennamen in {@link FunctionSelection}s
   * anhand des Sichtbarkeit-Abschnitts.
   */
  private FunctionSelectionProvider visibilityFunctionSelectionProvider;

  /**
   * Der globale Broadcast-Kanal wird f�r Nachrichten verwendet, die verschiedene
   * permanente Objekte erreichen m�ssen, die aber von (transienten) Objekten
   * ausgehen, die mit diesen globalen Objekten wegen des Ausuferns der Verbindungen
   * nicht in einer Beziehung stehen sollen. Diese Liste enth�lt alle
   * {@link BroadcastListener}, die auf dem globalen Broadcast-Kanal horchen. Dies
   * d�rfen nur permanente Objekte sein, d.h. Objekte deren Lebensdauer nicht vor
   * Beenden des FM4000 endet.
   */
  private List<BroadcastListener> broadcastListeners =
    new Vector<BroadcastListener>();

  /**
   * Wird auf myFrame registriert, damit zum Schlie�en des Fensters abort()
   * aufgerufen wird.
   */
  private MyWindowListener oehrchen;

  /**
   * Die Haupt-Men�leiste des FM4000.
   */
  private JMenuBar mainMenuBar;

  /**
   * Die Men�leiste, die angezeigt wird wenn der Quelltexteditor offen ist.
   */
  private JMenuBar editorMenuBar;

  /**
   * Der Quelltexteditor.
   */
  private JEditorPane editor;

  /**
   * Die Namen aller Druckfunktionen, die zur Auswahl stehen.
   */
  private Vector<String> printFunctionNames;

  /**
   * Wird bei jeder �nderung von Formularaspekten gestartet, um nach einer
   * Verz�gerung die �nderungen in das Dokument zu �bertragen.
   */
  private Timer writeChangesTimer;

  /**
   * Der XSelectionSupplier des Dokuments.
   */
  private XSelectionSupplier selectionSupplier;

  /**
   * Wird auf {@link #selectionSupplier} registriert, um �nderungen der
   * Cursorselektion zu beobachten.
   */
  private MyXSelectionChangedListener myXSelectionChangedListener;

  /**
   * Sendet die Nachricht b an alle Listener, die auf dem globalen Broadcast-Kanal
   * registriert sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void broadcast(Broadcast b)
  {
    Iterator<BroadcastListener> iter = broadcastListeners.iterator();
    while (iter.hasNext())
    {
      b.sendTo(iter.next());
    }
  }

  /**
   * listener wird �ber globale {@link Broadcast}s informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void addBroadcastListener(BroadcastListener listener)
  {
    if (!broadcastListeners.contains(listener)) broadcastListeners.add(listener);
  }

  /**
   * Wird von {@link FormControlModel#setItems(String[])} auf model aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void comboBoxItemsHaveChanged(FormControlModel model)
  {
    insertionModelList.fixComboboxInsertions(model);
  }

  /**
   * Wird bei jeder �nderung einer internen Datenstruktur aufgerufen, die ein Updaten
   * des Dokuments erforderlich macht um persistent zu werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void documentNeedsUpdating()
  {
    writeChangesTimer.restart();
  }

  /**
   * Liefert den {@link IDManager}, der f�r Objekte im Formular verwendet wird.
   * 
   * @see #NAMESPACE_FORMCONTROLMODEL
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public IDManager getIDManager()
  {
    return idManager;
  }

  /**
   * Startet eine Instanz des FormularMax 4000 f�r das Dokument des
   * TextDocumentModels model.
   * 
   * @param abortListener
   *          (falls nicht null) wird aufgerufen, nachdem der FormularMax 4000
   *          geschlossen wurde.
   * @param funcLib
   *          Funktionsbibliothek, die globale Funktionen zur Verf�gung stellt.
   * @param printFuncLib
   *          Funktionsbibliothek, die Druckfunktionen zur Verf�gung stellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000(TextDocumentModel model, ActionListener abortListener,
      FunctionLibrary funcLib, PrintFunctionLibrary printFuncLib)
  {
    XEnumeration xenu =
      UNO.XTextFieldsSupplier(model.doc).getTextFields().createEnumeration();
    boolean dothrow = false;
    while (xenu.hasMoreElements())
    {
      try
      {
        XDependentTextField tf = UNO.XDependentTextField(xenu.nextElement());
        if (tf == null) continue;

        if (UNO.supportsService(tf, "com.sun.star.text.TextField.InputUser"))
        {
          dothrow = true;
          break;
        }
      }
      catch (Exception x)
      {}
    }

    if (dothrow)
    {
      JOptionPane.showMessageDialog(
        null,
        "Der FormularMax 4000 kann Dokumente mit Seriendruckfeldern leider nicht verarbeiten.",
        "Fehler!", JOptionPane.ERROR_MESSAGE);
      throw new RuntimeException(
        "Der FormularMax 4000 kann Dokumente mit Seriendruckfeldern leider nicht verarbeiten.");
    }

    this.doc = model;
    this.abortListener = abortListener;
    this.functionLibrary = funcLib;
    this.printFunctionNames = new Vector<String>(printFuncLib.getFunctionNames());

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
          {
            Logger.error(x);
          }
          ;
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  private void createGUI()
  {
    Common.setLookAndFeelOnce();

    formControlModelList = new FormControlModelList(this);
    insertionModelList = new InsertionModelList(this);
    groupModelList = new GroupModelList(this);

    // Create and set up the window.
    myFrame = new JFrame("FormularMax 4000");
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    // der WindowListener sorgt daf�r, dass auf windowClosing mit abort reagiert
    // wird
    myFrame.addWindowListener(oehrchen);

    leftPanel = new LeftPanel(insertionModelList, formControlModelList, this);
    rightPanel =
      new RightPanel(insertionModelList, formControlModelList, functionLibrary, this);

    // damit sich Slider von JSplitPane vern�nftig bewegen l�sst.
    rightPanel.JComponent().setMinimumSize(new Dimension(100, 0));
    nonExistingRightPanel = new JPanel();
    nonExistingRightPanel.setMinimumSize(new Dimension(0, 0));
    nonExistingRightPanel.setPreferredSize(nonExistingRightPanel.getMinimumSize());
    nonExistingRightPanel.setMaximumSize(nonExistingRightPanel.getMinimumSize());
    mainContentPanel =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel.JComponent(),
        nonExistingRightPanel);
    mainContentPanel.setResizeWeight(1.0);
    defaultDividerSize = mainContentPanel.getDividerSize();
    mainContentPanel.setDividerSize(0);

    myFrame.getContentPane().add(mainContentPanel);

    mainMenuBar = new JMenuBar();
    // ========================= Datei ============================
    JMenu menu = new JMenu(L.m("Datei"));

    JMenuItem menuItem = new JMenuItem(L.m("Speichern"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        save(doc);
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Speichern unter..."));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        saveAs(doc);
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Beenden"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        abort();
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);
    // ========================= Bearbeiten ============================
    menu = new JMenu(L.m("Bearbeiten"));

    // ========================= Bearbeiten/Einf�gen ============================
    JMenu submenu = new JMenu(L.m("Standardelemente einf�gen"));
    menuItem = new JMenuItem(L.m("Empf�ngerauswahl-Tab"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertStandardEmpfaengerauswahl();
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen, <-Zur�ck, Weiter->"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtonsMiddle();
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen, <-Zur�ck, PDF, Drucken"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtonsLast();
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menu.add(submenu);
    // =================== Bearbeiten (Fortsetzung) ============================

    menu.addSeparator();

    menuItem = new JMenuItem(L.m("Checkboxen zu ComboBox"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ComboboxMergeDescriptor desc = leftPanel.mergeCheckboxesIntoCombobox();
        if (desc != null) insertionModelList.mergeCheckboxesIntoCombobox(desc);
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);
    // ========================= Ansicht ============================
    menu = new JMenu(L.m("Ansicht"));

    menuItem = new JCheckBoxMenuItem("ID");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewId =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewId);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("LABEL");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewLabel =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewLabel);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TYPE");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewType =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewType);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem(L.m("Elementspezifische Felder"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewAdditional =
          ((AbstractButton) e.getSource()).isSelected();
        broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewAdditional);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TRAFO, PLAUSI, AUTOFILL");
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (((AbstractButton) e.getSource()).isSelected())
        {
          mainContentPanel.setDividerSize(defaultDividerSize);
          mainContentPanel.setRightComponent(rightPanel.JComponent());
          mainContentPanel.setResizeWeight(0.6);
        }
        else
        {
          mainContentPanel.setDividerSize(0);
          mainContentPanel.setRightComponent(nonExistingRightPanel);
          mainContentPanel.setResizeWeight(1.0);
        }
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menu.addSeparator();
    menuItem = new JMenuItem(L.m("Funktionstester"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (functionTester == null)
        {
          functionTester = new FunctionTester(functionLibrary, new ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
              functionTester = null;
            }
          }, idManager, NAMESPACE_FORMCONTROLMODEL);
        }
        else
        {
          functionTester.toFront();
        }
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);
    // ========================= Formular ============================
    menu = new JMenu(L.m("Formular"));

    menuItem = new JMenuItem(L.m("Formularfelder aus Dokument einlesen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        scan(doc.doc);
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Formulartitel setzen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        setFormTitle();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Druckfunktionen setzen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        setPrintFunction();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("WollMux-Formularmerkmale aus Dokument entfernen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        deForm(doc);
      }
    });
    menu.add(menuItem);

    /*
     * Das Entfernen von Bookmarks kann Referenzfelder (Felder die Kopien anderer
     * Teile des Dokuments enthalten) zerst�ren, da diese dann ins Leere greifen.
     * Solange dies nicht erkannt wird, muss die Funktion deaktiviert sein.
     * 
     */
    if (new Integer(3).equals(new Integer(0)))
    {
      menuItem = new JMenuItem(L.m("Ladezeit des Dokuments optimieren"));
      menuItem.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          removeNonWMBookmarks(doc);
        }
      });
      menu.add(menuItem);
    }

    menuItem = new JMenuItem(L.m("Formularbeschreibung editieren"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        editFormDescriptor();
      }
    });
    menu.add(menuItem);

    mainMenuBar.add(menu);

    myFrame.setJMenuBar(mainMenuBar);

    writeChangesTimer = new Timer(500, new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        updateDocument(doc);
      }
    });
    writeChangesTimer.setCoalesce(true);
    writeChangesTimer.setRepeats(false);

    initEditor();

    selectionSupplier = UNO.XSelectionSupplier(doc.doc.getCurrentController());
    myXSelectionChangedListener = new MyXSelectionChangedListener();
    selectionSupplier.addSelectionChangeListener(myXSelectionChangedListener);

    initModelsAndViews(doc.getFormDescription());

    writeChangesTimer.stop();

    setFrameSize();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }

  /**
   * Wertet formDescription sowie die Bookmarks von {@link #doc} aus und
   * initialisiert alle internen Strukturen entsprechend. Dies aktualisiert auch die
   * entsprechenden Views.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void initModelsAndViews(ConfigThingy formDescription)
  {
    formControlModelList.clear();
    parseGlobalFormInfo(formDescription);

    ConfigThingy fensterAbschnitte =
      formDescription.query("Formular").query("Fenster");
    Iterator fensterAbschnittIterator = fensterAbschnitte.iterator();
    while (fensterAbschnittIterator.hasNext())
    {
      ConfigThingy fensterAbschnitt = (ConfigThingy) fensterAbschnittIterator.next();
      Iterator tabIter = fensterAbschnitt.iterator();
      while (tabIter.hasNext())
      {
        ConfigThingy tab = (ConfigThingy) tabIter.next();
        parseTab(tab, -1);
      }
    }

    /*
     * Immer mindestens 1 Tab in der Liste.
     */
    if (formControlModelList.isEmpty())
    {
      String id = formControlModelList.makeUniqueId(STANDARD_TAB_NAME);
      FormControlModel separatorTab = FormControlModel.createTab(id, id, this);
      formControlModelList.add(separatorTab, 0);
    }

    insertionModelList.clear();
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc.doc);
    String[] bookmarks = bmSupp.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarks.length; ++i)
    {
      try
      {
        String bookmark = bookmarks[i];
        if (InsertionModel.INSERTION_BOOKMARK.matcher(bookmark).matches())
          insertionModelList.add(new InsertionModel(bookmark, bmSupp,
            functionSelectionProvider, this));
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    groupModelList.clear();
    ConfigThingy visibilityConf =
      formDescription.query("Formular").query("Sichtbarkeit");
    Iterator sichtbarkeitsAbschnittIterator = visibilityConf.iterator();
    while (sichtbarkeitsAbschnittIterator.hasNext())
    {
      ConfigThingy sichtbarkeitsAbschnitt =
        (ConfigThingy) sichtbarkeitsAbschnittIterator.next();
      Iterator sichtbarkeitsFunktionIterator = sichtbarkeitsAbschnitt.iterator();
      while (sichtbarkeitsFunktionIterator.hasNext())
      {
        ConfigThingy sichtbarkeitsFunktion =
          (ConfigThingy) sichtbarkeitsFunktionIterator.next();
        String groupName = sichtbarkeitsFunktion.getName();
        FunctionSelection funcSel =
          visibilityFunctionSelectionProvider.getFunctionSelection(groupName);
        groupModelList.add(new GroupModel(groupName, funcSel, this));
      }
    }

    setFrameSize();
  }

  /**
   * Bringt einen modalen Dialog zum Bearbeiten des Formulartitels.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFormTitle()
  {
    String newTitle =
      JOptionPane.showInputDialog(myFrame, L.m("Bitte Formulartitel eingeben"),
        formTitle);
    if (newTitle != null)
    {
      formTitle = newTitle;
      documentNeedsUpdating();
    }
  }

  /**
   * Speichert die aktuelle Formularbeschreibung im Dokument und aktualisiert
   * Bookmarks etc.
   * 
   * @return die aktualisierte Formularbeschreibung
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private ConfigThingy updateDocument(TextDocumentModel doc)
  {
    Logger.debug(L.m("�bertrage Formularbeschreibung ins Dokument"));
    Map<String, ConfigThingy> mapFunctionNameToConfigThingy =
      new HashMap<String, ConfigThingy>();
    insertionModelList.updateDocument(mapFunctionNameToConfigThingy);
    ConfigThingy conf = buildFormDescriptor(mapFunctionNameToConfigThingy);
    doc.setFormDescription(new ConfigThingy(conf));
    return conf;
  }

  /**
   * Ruft {@link #updateDocument(TextDocumentModel)} auf, falls noch �nderungen
   * anstehen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void flushChanges()
  {
    if (writeChangesTimer.isRunning())
    {
      Logger.debug(L.m("Schreibe wartende �nderungen ins Dokument"));
      writeChangesTimer.stop();
      try
      {
        updateDocument(doc);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      ;
    }
  }

  /**
   * Liefert ein ConfigThingy zur�ck, das den aktuellen Zustand der
   * Formularbeschreibung repr�sentiert. Zum Exportieren der Formularbeschreibung
   * sollte {@link #updateDocument(XTextDocument)} verwendet werden.
   * 
   * @param mapFunctionNameToConfigThingy
   *          bildet einen Funktionsnamen auf ein ConfigThingy ab, dessen Wurzel der
   *          Funktionsname ist und dessen Inhalt eine Funktionsdefinition ist. Diese
   *          Funktionen ergeben den Funktionen-Abschnitt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private ConfigThingy buildFormDescriptor(Map mapFunctionNameToConfigThingy)
  {
    ConfigThingy conf = new ConfigThingy("WM");
    ConfigThingy form = conf.add("Formular");
    form.add("TITLE").add(formTitle);
    form.addChild(formControlModelList.export());
    form.addChild(groupModelList.export());
    if (!mapFunctionNameToConfigThingy.isEmpty())
    {
      ConfigThingy funcs = form.add("Funktionen");
      Iterator iter = mapFunctionNameToConfigThingy.values().iterator();
      while (iter.hasNext())
      {
        funcs.addChild((ConfigThingy) iter.next());
      }
    }
    return conf;
  }

  /**
   * Extrahiert aus conf die globalen Eingenschaften des Formulars wie z,B, den
   * Formulartitel oder die Funktionen des Funktionen-Abschnitts.
   * 
   * @param conf
   *          der WM-Knoten der �ber einer beliebigen Anzahl von Formular-Knoten
   *          sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void parseGlobalFormInfo(ConfigThingy conf)
  {
    ConfigThingy tempConf = conf.query("Formular").query("TITLE");
    if (tempConf.count() > 0) formTitle = tempConf.toString();
    tempConf = conf.query("Formular").query("Funktionen");
    if (tempConf.count() >= 1)
    {
      try
      {
        tempConf = tempConf.getFirstChild();
      }
      catch (Exception x)
      {}
    }
    else
    {
      tempConf = new ConfigThingy("Funktionen");
    }
    functionSelectionProvider =
      new FunctionSelectionProvider(functionLibrary, tempConf, getIDManager(),
        NAMESPACE_FORMCONTROLMODEL);

    tempConf = conf.query("Formular").query("Sichtbarkeit");
    if (tempConf.count() >= 1)
    {
      try
      {
        tempConf = tempConf.getFirstChild();
      }
      catch (Exception x)
      {}
    }
    else
    {
      tempConf = new ConfigThingy("Sichtbarkeit");
    }
    visibilityFunctionSelectionProvider =
      new FunctionSelectionProvider(null, tempConf, getIDManager(),
        NAMESPACE_FORMCONTROLMODEL);
  }

  /**
   * F�gt am Anfang der Liste eine Standard-Empfaengerauswahl-Tab ein.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardEmpfaengerauswahl()
  {
    try
    {
      ConfigThingy conf = new ConfigThingy("Empfaengerauswahl", EMPFAENGER_TAB_URL);
      parseTab(conf, 0);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * H�ngt die Standardbuttons f�r einen mittleren Tab an das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardButtonsMiddle()
  {
    try
    {
      ConfigThingy conf = new ConfigThingy("Buttons", STANDARD_BUTTONS_MIDDLE_URL);
      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * H�ngt die Standardbuttons f�r den letzten Tab an das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardButtonsLast()
  {
    try
    {
      ConfigThingy conf = new ConfigThingy("Buttons", STANDARD_BUTTONS_LAST_URL);
      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Parst das Tab conf und f�gt entsprechende FormControlModels der
   * {@link #formControlModelList} hinzu.
   * 
   * @param conf
   *          der Knoten direkt �ber "Eingabefelder" und "Buttons".
   * @param idx
   *          falls >= 0 werden die Steuerelemente am entsprechenden Index der Liste
   *          in die Formularbeschreibung eingef�gt, ansonsten ans Ende angeh�ngt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void parseTab(ConfigThingy conf, int idx)
  {
    String id = conf.getName();
    String label = id;
    String action = FormControlModel.NO_ACTION;
    String tooltip = "";
    char hotkey = 0;

    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = (ConfigThingy) iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("TITLE"))
        label = str;
      else if (name.equals("CLOSEACTION"))
        action = str;
      else if (name.equals("TIP"))
        tooltip = str;
      else if (name.equals("HOTKEY")) hotkey = str.length() > 0 ? str.charAt(0) : 0;
    }

    FormControlModel tab = FormControlModel.createTab(label, id, this);
    tab.setAction(action);
    tab.setTooltip(tooltip);
    tab.setHotkey(hotkey);

    if (idx >= 0)
    {
      formControlModelList.add(tab, idx++);
      idx += parseGrandchildren(conf.query("Eingabefelder"), idx, true);
      parseGrandchildren(conf.query("Buttons"), idx, false);
    }
    else
    {
      formControlModelList.add(tab);
      parseGrandchildren(conf.query("Eingabefelder"), -1, true);
      parseGrandchildren(conf.query("Buttons"), -1, false);
    }

    documentNeedsUpdating();
  }

  /**
   * Parst die Kinder der Kinder von grandma als Steuerelemente und f�gt der
   * {@link #formControlModelList} entsprechende FormControlModels hinzu.
   * 
   * @param idx
   *          falls >= 0 werden die Steuerelemente am entsprechenden Index der Liste
   *          in die Formularbeschreibung eingef�gt, ansonsten ans Ende angeh�ngt.
   * @param killLastGlue
   *          falls true wird das letzte Steuerelement entfernt, wenn es ein glue
   *          ist.
   * @return die Anzahl der erzeugten Steuerelemente.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private int parseGrandchildren(ConfigThingy grandma, int idx, boolean killLastGlue)
  {
    if (idx < 0) idx = formControlModelList.size();

    boolean lastIsGlue = false;
    FormControlModel model = null;
    int count = 0;
    Iterator grandmaIter = grandma.iterator();
    while (grandmaIter.hasNext())
    {
      Iterator iter = ((ConfigThingy) grandmaIter.next()).iterator();
      while (iter.hasNext())
      {
        model =
          new FormControlModel((ConfigThingy) iter.next(),
            functionSelectionProvider, this);
        lastIsGlue = model.isGlue();
        ++count;
        formControlModelList.add(model, idx++);
      }
    }
    if (killLastGlue && lastIsGlue)
    {
      formControlModelList.remove(model);
      --count;
    }

    documentNeedsUpdating();

    return count;
  }

  /**
   * Scannt das Dokument doc durch und erzeugt {@link FormControlModel}s f�r alle
   * Formularfelder, die noch kein umschlie�endes WollMux-Bookmark haben.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void scan(XTextDocument doc)
  {
    try
    {
      XDocumentInfo info = UNO.XDocumentInfoSupplier(doc).getDocumentInfo();
      try
      {
        String tit = ((String) UNO.getProperty(info, "Title")).trim();
        if (formTitle == GENERATED_FORM_TITLE && tit.length() > 0) formTitle = tit;
      }
      catch (Exception x)
      {}
      DocumentTree tree = new DocumentTree(doc);
      Visitor visitor = new ScanVisitor();
      visitor.visit(tree);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Fehler w�hrend des Scan-Vorgangs"), x);
    }

    documentNeedsUpdating();
  }

  private class ScanVisitor extends DocumentTree.Visitor
  {
    private Map<String, InsertionBookmark> insertions =
      new HashMap<String, InsertionBookmark>();

    private StringBuilder text = new StringBuilder();

    private StringBuilder fixupText = new StringBuilder();

    private FormControlModel fixupCheckbox = null;

    private void fixup()
    {
      if (fixupCheckbox != null && fixupCheckbox.getLabel() == NO_LABEL)
      {
        fixupCheckbox.setLabel(makeLabelFromStartOf(fixupText,
          2 * GENERATED_LABEL_MAXLENGTH));
        fixupCheckbox = null;
      }
      fixupText.setLength(0);
    }

    public boolean container(Container container, int count)
    {
      fixup();

      if (container.getType() != DocumentTree.PARAGRAPH_TYPE) text.setLength(0);

      return true;
    }

    public boolean textRange(TextRange textRange)
    {
      String str = textRange.getString();
      text.append(str);
      fixupText.append(str);
      return true;
    }

    public boolean insertionBookmark(InsertionBookmark bookmark)
    {
      if (bookmark.isStart())
        insertions.put(bookmark.getName(), bookmark);
      else
        insertions.remove(bookmark.getName());

      return true;
    }

    public boolean formControl(FormControl control)
    {
      fixup();

      if (insertions.isEmpty())
      {
        FormControlModel model = registerFormControl(control, text);
        if (model != null && model.getType() == FormControlModel.CHECKBOX_TYPE)
          fixupCheckbox = model;
      }

      return true;
    }
  }

  /**
   * F�gt der {@link #formControlModelList} ein neues {@link FormControlModel} hinzu
   * f�r das {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * control, wobei text der Text sein sollte, der im Dokument vor control steht.
   * Dieser Text wird zur Generierung des Labels herangezogen. Es wird ebenfalls der
   * {@link #insertionModelList} ein entsprechendes {@link InsertionModel}
   * hinzugef�gt. Zus�tzlich wird immer ein entsprechendes Bookmark um das Control
   * herumgelegt, das die Einf�gestelle markiert.
   * 
   * @return null, falls es sich bei dem Control nur um eine reine Einf�gestelle
   *         handelt. In diesem Fall wird nur der {@link #insertionModelList} ein
   *         Element hinzugef�gt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerFormControl(FormControl control,
      StringBuilder text)
  {
    String label;
    String id;
    String descriptor = control.getDescriptor();
    Matcher m = MAGIC_DESCRIPTOR_PATTERN.matcher(descriptor);
    if (m.matches())
    {
      label = m.group(1).trim();
      if (label.length() == 0) label = INSERTION_ONLY;
      id = m.group(2).trim();
    }
    else
    {
      if (control.getType() == DocumentTree.CHECKBOX_CONTROL)
        label = NO_LABEL; // immer fixUp-Text von hinter der Checkbox benutzen, weil
      // meist bessere Ergebnisse als Text von vorne
      else
        label = makeLabelFromEndOf(text, GENERATED_LABEL_MAXLENGTH);
      id = descriptor;
    }

    id = makeControlId(label, id);

    FormControlModel model = null;

    if (label != INSERTION_ONLY)
    {
      switch (control.getType())
      {
        case DocumentTree.CHECKBOX_CONTROL:
          model = registerCheckbox(control, label, id);
          break;
        case DocumentTree.DROPDOWN_CONTROL:
          model = registerDropdown((DropdownFormControl) control, label, id);
          break;
        case DocumentTree.INPUT_CONTROL:
          model = registerInput(control, label, id);
          break;
        default:
          Logger.error(L.m("Unbekannter Typ Formular-Steuerelement"));
          return null;
      }
    }

    boolean doGenderTrafo = false;

    String bookmarkName = insertFormValue(id);
    if (label == INSERTION_ONLY)
    {
      if (id.startsWith(GLOBAL_PREFIX))
      {
        id = id.substring(GLOBAL_PREFIX.length());
        bookmarkName = insertValue(id);
      }
      else if (id.startsWith(GENDER_PREFIX))
      {
        id = id.substring(GENDER_PREFIX.length());
        bookmarkName = insertFormValue(id);
        if (control.getType() == DocumentTree.DROPDOWN_CONTROL)
          doGenderTrafo = true;
      }
    }

    bookmarkName = control.surroundWithBookmark(bookmarkName);

    try
    {
      InsertionModel imodel =
        new InsertionModel(bookmarkName, UNO.XBookmarksSupplier(doc.doc),
          functionSelectionProvider, this);
      if (doGenderTrafo) addGenderTrafo(imodel, (DropdownFormControl) control);
      insertionModelList.add(imodel);
    }
    catch (Exception x)
    {
      Logger.error(L.m("Es wurde ein fehlerhaftes Bookmark generiert: \"%1\"",
        bookmarkName), x);
    }

    return model;
  }

  /**
   * Verpasst model eine Gender-TRAFO, die ihre Herr/Frau/Anders-Texte aus den Items
   * von control bezieht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addGenderTrafo(InsertionModel model, DropdownFormControl control)
  {
    String[] items = control.getItems();
    FunctionSelection genderTrafo =
      functionSelectionProvider.getFunctionSelection("Gender");

    for (int i = 0; i < 3 && i < items.length; ++i)
    {
      String item = items[i];

      /*
       * Bestimme die maximal am Ende des Eintrags zu entfernende Anzahl Leerzeichen.
       * Dies ist die Anzahl an Eintr�gen, die bis auf folgende Leerzeichen identisch
       * sind MINUS 1.
       */
      String item1 = item;
      while (item1.endsWith(" "))
        item1 = item1.substring(0, item1.length() - 1);
      int n = 0;
      for (int j = 0; j < items.length; ++j)
      {
        String item2 = items[j];
        while (item2.endsWith(" "))
          item2 = item2.substring(0, item2.length() - 1);
        if (item1.equals(item2)) ++n;
      }

      // bis zu N-1 Leerzeichen am Ende l�schen, um mehrere gleiche Eintr�ge zu
      // erlauben.
      for (; n > 1 && item.endsWith(" "); --n)
        item = item.substring(0, item.length() - 1);
      genderTrafo.setParameterValue(GENDER_TRAFO_PARAMS[i], ParamValue.literal(item));
    }

    model.setTrafo(genderTrafo);
  }

  /**
   * Bastelt aus dem Ende des Textes text ein Label das maximal maxlen Zeichen lang
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromEndOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(str.length() - len);
    if (label.length() < 2) label = NO_LABEL;
    return label;
  }

  /**
   * Bastelt aus dem Start des Textes text ein Label, das maximal maxlen Zeichen lang
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromStartOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(0, len);
    if (label.length() < 2) label = NO_LABEL;
    return label;
  }

  /**
   * F�gt {@link #formControlModelList} ein neues {@link FormControlModel} f�r eine
   * Checkbox hinzu und liefert es zur�ck.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerCheckbox(FormControl control, String label,
      String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createCheckbox(label, id, this);
    if (control.getString().equalsIgnoreCase("true"))
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add("true");
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
    }
    formControlModelList.add(model);
    return model;
  }

  /**
   * F�gt {@link #formControlModelList} ein neues {@link FormControlModel} f�r eine
   * Auswahlliste hinzu und liefert es zur�ck.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerDropdown(DropdownFormControl control,
      String label, String id)
  {
    FormControlModel model = null;
    String[] items = control.getItems();
    boolean editable = false;
    for (int i = 0; i < items.length; ++i)
    {
      if (items[i].equalsIgnoreCase("<<Freitext>>"))
      {
        String[] newItems = new String[items.length - 1];
        System.arraycopy(items, 0, newItems, 0, i);
        System.arraycopy(items, i + 1, newItems, i, items.length - i - 1);
        items = newItems;
        editable = true;
        break;
      }
    }
    model = FormControlModel.createComboBox(label, id, items, this);
    model.setEditable(editable);
    String preset = unicodeTrim(control.getString());
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
    }
    formControlModelList.add(model);
    return model;
  }

  /**
   * F�gt {@link #formControlModelList} ein neues {@link FormControlModel} f�r ein
   * Eingabefeld hinzu und liefert es zur�ck.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerInput(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createTextfield(label, id, this);
    String preset = unicodeTrim(control.getString());
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
    }
    formControlModelList.add(model);
    return model;
  }

  /**
   * Liefert str zur�ck minus f�hrende und folgende Whitespace (wobei
   * Unicode-Leerzeichen) korrekt ber�cksichtigt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private String unicodeTrim(String str)
  {
    if (str.length() == 0) return str;

    if (Character.isWhitespace(str.charAt(0))
      || Character.isWhitespace(str.charAt(str.length() - 1)))
    {
      int i = 0;
      while (i < str.length() && Character.isWhitespace(str.charAt(i)))
        ++i;
      int j = str.length() - 1;
      while (j >= 0 && Character.isWhitespace(str.charAt(j)))
        --j;
      if (i > j) return "";
      return str.substring(i, j + 1);
    }
    else
      return str;
  }

  /**
   * Macht aus str einen passenden Bezeichner f�r ein Steuerelement. Falls label ==
   * {@link #INSERTION_ONLY}, so muss der Bezeichner nicht eindeutig sein (dies ist
   * der Marker f�r eine reine Einf�gestelle, f�r die kein Steuerelement erzeugt
   * werden muss).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeControlId(String label, String str)
  {
    if (label == INSERTION_ONLY)
    {
      String prefix = "";
      if (str.startsWith(GLOBAL_PREFIX))
      {
        prefix = GLOBAL_PREFIX;
        str = str.substring(GLOBAL_PREFIX.length());
      }
      else if (str.startsWith(GENDER_PREFIX))
      {
        prefix = GENDER_PREFIX;
        str = str.substring(GENDER_PREFIX.length());
      }
      str = str.replaceAll("[^a-zA-Z_0-9]", "");
      if (str.length() == 0) str = "Einfuegung";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      return prefix + str;
    }
    else
    {
      str = str.replaceAll("[^a-zA-Z_0-9]", "");
      if (str.length() == 0) str = "Steuerelement";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      return formControlModelList.makeUniqueId(str);
    }
  }

  private static class NoWrapEditorKit extends DefaultEditorKit
  {
    private static final long serialVersionUID = -2741454443147376514L;

    private ViewFactory vf = null;

    public ViewFactory getViewFactory()
    {
      if (vf == null) vf = new NoWrapFactory();
      return vf;
    };

    private class NoWrapFactory implements ViewFactory
    {
      public View create(Element e)
      {
        return new PlainView(e);
      }

    };
  };

  /**
   * Initialisiert die GUI f�r den Quelltexteditor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void initEditor()
  {
    JMenu menu;
    JMenuItem menuItem;
    editorMenuBar = new JMenuBar();
    // ========================= Datei ============================
    menu = new JMenu(L.m("Datei"));

    menuItem = new JMenuItem(L.m("Speichern"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          ConfigThingy conf =
            new ConfigThingy("", null, new StringReader(editor.getText()));
          myFrame.setJMenuBar(mainMenuBar);
          myFrame.getContentPane().remove(editorContentPanel);
          myFrame.getContentPane().add(mainContentPanel);
          initModelsAndViews(conf);
          documentNeedsUpdating();
        }
        catch (Exception e1)
        {
          JOptionPane.showMessageDialog(myFrame, e1.getMessage(),
            L.m("Fehler beim Parsen der Formularbeschreibung"),
            JOptionPane.WARNING_MESSAGE);
        }
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen"));
    menuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        myFrame.setJMenuBar(mainMenuBar);
        myFrame.getContentPane().remove(editorContentPanel);
        myFrame.getContentPane().add(mainContentPanel);
        setFrameSize();
      }
    });
    menu.add(menuItem);

    editorMenuBar.add(menu);

    editor = new JEditorPane("text/plain", "");
    editor.setEditorKit(new NoWrapEditorKit());

    editor.setFont(new Font("Monospaced", Font.PLAIN, editor.getFont().getSize() + 2));
    JScrollPane scrollPane =
      new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    editorContentPanel = new JPanel(new BorderLayout());
    editorContentPanel.add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * �ffnet ein Fenster zum Editieren der Formularbeschreibung. Beim Schliessend des
   * Fensters wird die ge�nderte Formularbeschreibung neu geparst, falls sie
   * syntaktisch korrekt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editFormDescriptor()
  {
    editor.setCaretPosition(0);
    editor.setText(updateDocument(doc).stringRepresentation());
    myFrame.getContentPane().remove(mainContentPanel);
    myFrame.getContentPane().add(editorContentPanel);
    myFrame.setJMenuBar(editorMenuBar);
    setFrameSize();
  }

  private void setPrintFunction()
  {
    final JList printFunctionCurrentList =
      new JList(new Vector<String>(doc.getPrintFunctions()));
    JPanel printFunctionEditorContentPanel = new JPanel(new BorderLayout());
    printFunctionEditorContentPanel.add(printFunctionCurrentList,
      BorderLayout.CENTER);

    final JComboBox printFunctionComboBox = new JComboBox(printFunctionNames);
    printFunctionComboBox.setEditable(true);

    printFunctionEditorContentPanel.add(printFunctionComboBox, BorderLayout.NORTH);
    final JDialog dialog = new JDialog(myFrame, true);

    ActionListener removeFunc = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Object[] todel = printFunctionCurrentList.getSelectedValues();
        for (int i = 0; i < todel.length; i++)
          doc.removePrintFunction("" + todel[i]);
        printFunctionCurrentList.setListData(new Vector<String>(
          doc.getPrintFunctions()));
      }
    };

    ActionListener addFunc = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        String newFunctionName = printFunctionComboBox.getSelectedItem().toString();
        doc.addPrintFunction(newFunctionName);
        printFunctionCurrentList.setListData(new Vector<String>(
          doc.getPrintFunctions()));
      }
    };

    JButton wegDamit = new JButton(L.m("Entfernen"));
    wegDamit.addActionListener(removeFunc);

    JButton machDazu = new JButton(L.m("Hinzuf�gen"));
    machDazu.addActionListener(addFunc);

    JButton ok = new JButton(L.m("OK"));
    ok.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });

    Box buttons = Box.createHorizontalBox();
    buttons.add(wegDamit);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(machDazu);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(ok);
    printFunctionEditorContentPanel.add(buttons, BorderLayout.SOUTH);

    dialog.setTitle(L.m("Druckfunktion setzen"));
    dialog.add(printFunctionEditorContentPanel);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    if (frameHeight < 200) frameHeight = 200;

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setBounds(x, y, frameWidth, frameHeight);
    dialog.setVisible(true);
  }

  /**
   * Liefert "WM(CMD'insertValue' DB_SPALTE '&lt;id>').
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertValue(String id)
  {
    return "WM(CMD 'insertValue' DB_SPALTE '" + id + "')";
  }

  /**
   * Liefert "WM(CMD'insertFormValue' ID '&lt;id>').
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertFormValue(String id)
  {
    return "WM(CMD 'insertFormValue' ID '" + id + "')";
  }

  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeNonWMBookmarks(TextDocumentModel doc)
  {
    doc.removeNonWMBookmarks();
  }

  /**
   * Entfernt die WollMux-Formularmerkmale aus dem Dokument.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void deForm(TextDocumentModel doc)
  {
    doc.deForm();
    initModelsAndViews(new ConfigThingy(""));
  }

  /**
   * Ruft die Datei/Speichern Funktion von OpenOffice.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void save(TextDocumentModel doc)
  {
    flushChanges();
    UNO.dispatch(doc.doc, ".uno:Save");
  }

  /**
   * Ruft die Datei/Speichern unter... Funktion von OpenOffice.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void saveAs(TextDocumentModel doc)
  {
    flushChanges();
    UNO.dispatch(doc.doc, ".uno:SaveAs");
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    flushChanges();

    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen n�tig, damit der FormularMax4000 gc'ed werden kann. Die Befehle
     * sorgen daf�r, dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager)
     * indirekt �ber den JFrame den FM4000 kennt.
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);

    myFrame.dispose();
    myFrame = null;

    if (functionTester != null) functionTester.abort();

    try
    {
      selectionSupplier.removeSelectionChangeListener(myXSelectionChangedListener);
    }
    catch (Exception x)
    {}

    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }

  /**
   * Schliesst den FM4000 und alle zugeh�rigen Fenster.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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
  }

  /**
   * Bringt den FormularMax 4000 in den Vordergrund.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void toFront()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            myFrame.toFront();
          }
          catch (Exception x)
          {}
          ;
        }
      });
    }
    catch (Exception x)
    {}
  }

  /**
   * Workaround f�r Problem unter Windows, dass das Layout bei myFrame.pack() die
   * Taskleiste nicht ber�cksichtigt (das Fenster also dahinter verschwindet),
   * zumindest solange nicht bis man die Taskleiste mal in ihrer Gr��e ver�ndert hat.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFrameSize()
  {
    myFrame.pack();
    fixFrameSize(myFrame);
  }

  /**
   * Sorgt daf�r, dass die Ausdehnung von frame nicht die maximal erlaubten
   * Fensterdimensionen �berschreitet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void fixFrameSize(JFrame frame)
  {
    Rectangle maxWindowBounds;

    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();
    String lafName = UIManager.getSystemLookAndFeelClassName();
    if (!lafName.contains("plaf.windows.")) maxWindowBounds.height -= 32; // Sicherheitsabzug
    // f�r KDE
    // Taskleiste

    Rectangle frameBounds = frame.getBounds();
    if (frameBounds.x < maxWindowBounds.x)
    {
      frameBounds.width -= (maxWindowBounds.x - frameBounds.x);
      frameBounds.x = maxWindowBounds.x;
    }
    if (frameBounds.y < maxWindowBounds.y)
    {
      frameBounds.height -= (maxWindowBounds.y - frameBounds.y);
      frameBounds.y = maxWindowBounds.y;
    }
    if (frameBounds.width > maxWindowBounds.width)
      frameBounds.width = maxWindowBounds.width;
    if (frameBounds.height > maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.height;
    frame.setBounds(frameBounds);
  }

  /**
   * Nimmt eine Menge von XTextRange Objekten, sucht alle umschlossenen Bookmarks und
   * broadcastet eine entsprechende Nachricht, damit sich die entsprechenden Objekte
   * selektieren.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void selectionChanged(XIndexAccess access)
  {
    Set<String> bookmarkNames = null; // wird lazy initialisiert

    int count = access.getCount();
    for (int i = 0; i < count; ++i)
    {
      XEnumerationAccess enuAccess = null;
      try
      {
        enuAccess = UNO.XEnumerationAccess(access.getByIndex(i));
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      try
      {
        if (enuAccess != null)
        {
          XEnumeration paraEnu = enuAccess.createEnumeration();
          while (paraEnu.hasMoreElements())
          {
            Object nextEle = paraEnu.nextElement();
            if (nextEle == null)
              throw new NullPointerException(
                L.m("nextElement() == null obwohl hasMoreElements()==true"));
            XEnumerationAccess xs = UNO.XEnumerationAccess(nextEle);
            if (xs == null)
              throw new NullPointerException(
                L.m("Paragraph unterst�tzt nicht XEnumerationAccess?!?"));
            XEnumeration textportionEnu = xs.createEnumeration();
            while (textportionEnu.hasMoreElements())
            {
              Object textportion = textportionEnu.nextElement();
              if ("Bookmark".equals(UNO.getProperty(textportion, "TextPortionType")))
              {
                XNamed bookmark = null;
                try
                {
                  // boolean isStart = ((Boolean)UNO.getProperty(textportion,
                  // "IsStart")).booleanValue();
                  bookmark = UNO.XNamed(UNO.getProperty(textportion, "Bookmark"));
                }
                catch (Exception x)
                {
                  continue;
                }

                String name = bookmark.getName();
                if (bookmarkNames == null) bookmarkNames = new HashSet<String>();
                bookmarkNames.add(name);
              }
            }
          }
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    if (bookmarkNames != null && !bookmarkNames.isEmpty())
      broadcast(new BroadcastObjectSelectionByBookmarks(bookmarkNames));
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
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

  private class MyXSelectionChangedListener implements XSelectionChangeListener
  {
    public void selectionChanged(EventObject arg0)
    {
      try
      {
        Object selection =
          AnyConverter.toObject(XInterface.class, selectionSupplier.getSelection());
        final XIndexAccess access = UNO.XIndexAccess(selection);
        if (access == null) return;
        try
        {
          javax.swing.SwingUtilities.invokeLater(new Runnable()
          {
            public void run()
            {
              try
              {
                FormularMax4000.this.selectionChanged(access);
              }
              catch (Exception x)
              {}
              ;
            }
          });
        }
        catch (Exception x)
        {}
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(L.m("Kann Selection nicht in Objekt umwandeln"), e);
      }
    }

    public void disposing(EventObject arg0)
    {}
  }

  /**
   * Ruft den FormularMax4000 f�r das aktuelle Vordergrunddokument auf, falls dieses
   * ein Textdokument ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    WollMuxFiles.setupWollMuxDir();
    Logger.init(System.err, Logger.DEBUG);
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
    Map<Object, Object> context = new HashMap<Object, Object>();
    DialogLibrary dialogLib =
      WollMuxFiles.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, context);
    new FormularMax4000(new TextDocumentModel(doc), null,
      WollMuxFiles.parseFunctions(WollMuxFiles.getWollmuxConf(), dialogLib, context,
        null), WollMuxFiles.parsePrintFunctions(WollMuxFiles.getWollmuxConf()));
  }

}
