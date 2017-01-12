/*
 * Dateiname: WollMuxEventHandler.java
 * Projekt  : WollMux
 * Funktion : Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
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
 * 24.10.2005 | LUT | Erstellung als EventHandler.java
 * 01.12.2005 | BNK | +on_unload() das die Toolbar neu erzeugt (böser Hack zum 
 *                  | Beheben des Seitenansicht-Toolbar-Verschwindibus-Problems)
 *                  | Ausgabe des hashCode()s in den Debug-Meldungen, um Events 
 *                  | Objekten zuordnen zu können beim Lesen des Logfiles
 * 27.03.2005 | LUT | neues Kommando openDocument
 * 21.04.2006 | LUT | +ConfigurationErrorException statt NodeNotFoundException bei
 *                    fehlendem URL-Attribut in Textfragmenten
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 *                    + Überarbeitung vieler Fehlermeldungen
 *                    + Zeilenumbrüche in showInfoModal, damit keine unlesbaren
 *                      Fehlermeldungen mehr ausgegeben werden.
 * 16.12.2009 | ERT | Cast XTextContent-Interface entfernt
 * 03.03.2010 | ERT | getBookmarkNamesStartingWith nach UnoHelper TextDocument verschoben
 * 03.03.2010 | ERT | Verhindern von Überlagern von insertFrag-Bookmarks
 * 23.03.2010 | ERT | [R59480] Meldung beim Ausführen von "Textbausteinverweis einfügen"
 * 02.06.2010 | BED | +handleSaveTempAndOpenExt
 * 08.05.2012 | jub | fakeSymLink behandlung eingebaut: auflösung und test der FRAG_IDs berücksichtigt
 *                    auch die möglichkeit, dass im conifg file auf einen fake SymLink verwiesen wird.
 * 11.12.2012 | jub | fakeSymLinks werden doch nicht gebraucht; wieder aus dem code entfernt                   
 *
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.star.beans.PropertyValue;
import com.sun.star.document.XEventListener;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.BasicEvent;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbdruck;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbout;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnButtonZuleitungszeilePressed;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCheckInstallation;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnDumpInfo;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnExecutePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFocusFormField;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormControllerInitCompleted;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormValueChanged;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormularMax4000Returned;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormularMax4000Show;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFunctionDialog;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToPlaceholder;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnKill;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnManagePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnMarkBlock;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpen;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPALChangedNotify;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrint;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrintPage;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnProcessMultiform;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnProcessTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRegisterDispatchInterceptor;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemovePALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnReprocessTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveAs;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveTempAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValueFinished;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetInsertValues;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetPrintBlocksPropsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetSender;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetVisibleState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetWindowPosSize;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetWindowVisible;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnShowDialogAbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnShowDialogPersoenlicheAbsenderlisteVerwalten;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentClosed;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextbausteinEinfuegen;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnZifferEinfuegen;

/**
 * Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WollMuxEventHandler
{
  /**
   * Name des OnWollMuxProcessingFinished-Events.
   */
  public static final String ON_WOLLMUX_PROCESSING_FINISHED =
    "OnWollMuxProcessingFinished";

  /**
   * Mit dieser Methode ist es möglich die Entgegennahme von Events zu blockieren.
   * Alle eingehenden Events werden ignoriert, wenn accept auf false gesetzt ist und
   * entgegengenommen, wenn accept auf true gesetzt ist.
   * 
   * @param accept
   */
  public static void setAcceptEvents(boolean accept)
  {
    EventProcessor.getInstance().setAcceptEvents(accept);
  }

  /**
   * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
   * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
   * hineingepackt und von einem einzigen eventProcessingThread sequentiell
   * abgearbeitet.
   * 
   * @author lut
   */
  public static class EventProcessor
  {
    /**
     * Gibt an, ob der EventProcessor überhaupt events entgegennimmt. Ist
     * acceptEvents=false, werden alle Events ignoriert.
     */
    private boolean acceptEvents = false;

    private List<WollMuxEvent> eventQueue = new LinkedList<WollMuxEvent>();

    private static EventProcessor singletonInstance;

    private static Thread eventProcessorThread;

    private static EventProcessor getInstance()
    {
      if (singletonInstance == null)
      {
        singletonInstance = new EventProcessor();
        singletonInstance.start();
      }
      return singletonInstance;
    }

    /**
     * Mit dieser Methode ist es möglich die Entgegennahme von Events zu blockieren.
     * Alle eingehenden Events werden ignoriert, wenn accept auf false gesetzt ist
     * und entgegengenommen, wenn accept auf true gesetzt ist.
     * 
     * @param accept
     */
    private void setAcceptEvents(boolean accept)
    {
      acceptEvents = accept;
      if (accept)
        Logger.debug(L.m("EventProcessor: akzeptiere neue Events."));
      else
        Logger.debug(L.m("EventProcessor: blockiere Entgegennahme von Events!"));
    }

    private EventProcessor()
    {
      // starte den eventProcessorThread
      eventProcessorThread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          Logger.debug(L.m("Starte EventProcessor-Thread"));
          try
          {
            while (true)
            {
              WollMuxEvent event;
              synchronized (eventQueue)
              {
                while (eventQueue.isEmpty())
                  eventQueue.wait();
                event = eventQueue.remove(0);
              }

              event.process();
            }
          }
          catch (InterruptedException e)
          {
            Logger.error(L.m("EventProcessor-Thread wurde unterbrochen:"));
            Logger.error(e);
          }
          Logger.debug(L.m("Beende EventProcessor-Thread"));
        }
      });
    }

    /**
     * Startet den {@link #eventProcessorThread}.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    private void start()
    {
      eventProcessorThread.start();
    }

    /**
     * Diese Methode fügt ein Event an die eventQueue an wenn der WollMux erfolgreich
     * initialisiert wurde und damit events akzeptieren darf. Anschliessend weckt sie
     * den EventProcessor-Thread.
     * 
     * @param event
     */
    private void addEvent(WollMuxEventHandler.WollMuxEvent event)
    {
      if (acceptEvents) synchronized (eventQueue)
      {
        eventQueue.add(event);
        eventQueue.notifyAll();
      }
    }
  }

  /**
   * Interface für die Events, die dieser EventHandler abarbeitet.
   */
  public interface WollMuxEvent
  {
    /**
     * Startet die Ausführung des Events und darf nur aus dem EventProcessor
     * aufgerufen werden.
     */
    public void process();
  }

  public static class CantStartDialogException extends WollMuxFehlerException
  {
    private static final long serialVersionUID = -1130975078605219254L;

    public CantStartDialogException(java.lang.Exception e)
    {
      super(
        L.m("Der Dialog konnte nicht gestartet werden!\n\nBitte kontaktieren Sie Ihre Systemadministration."),
        e);
    }
  }

  /**
   * Stellt das WollMuxEvent event in die EventQueue des EventProcessors.
   * 
   * @param event
   */
  private static void handle(WollMuxEvent event)
  {
    EventProcessor.getInstance().addEvent(event);
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Dialog AbsenderAuswaehlen startet.
   */
  public static void handleShowDialogAbsenderAuswaehlen()
  {
    handle(new OnShowDialogAbsenderAuswaehlen());
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Dialog
   * PersoenlichtAbsenderListe-Verwalten startet.
   */
  public static void handleShowDialogPersoenlicheAbsenderliste()
  {
    handle(new OnShowDialogPersoenlicheAbsenderlisteVerwalten());
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Funktionsdialog dialogName aufruft und
   * die zurückgelieferten Werte in die entsprechenden FormField-Objekte des
   * Dokuments doc einträgt.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgelöst.
   */
  public static void handleFunctionDialog(TextDocumentController documentController, String dialogName)
  {
    handle(new OnFunctionDialog(documentController, dialogName));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das einen modalen Dialog anzeigt, der wichtige
   * Versionsinformationen über den WollMux, die Konfiguration und die WollMuxBar
   * (nur falls wollmuxBarVersion nicht der Leersting ist) enthält. Anmerkung: das
   * WollMux-Modul hat keine Ahnung, welche WollMuxBar verwendet wird. Daher ist es
   * möglich, über den Parameter wollMuxBarVersion eine Versionsnummer der WollMuxBar
   * zu übergeben, die im Dialog angezeigt wird, falls wollMuxBarVersion nicht der
   * Leerstring ist.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgelöst, wenn die
   * WollMux-url "wollmux:about" aufgerufen wurde.
   */
  public static void handleAbout(String wollMuxBarVersion)
  {
    handle(new OnAbout(wollMuxBarVersion));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den FormularMax4000 aufruft für das Dokument
   * doc.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgelöst.
   */
  public static void handleFormularMax4000Show(TextDocumentController documentController)
  {
    handle(new OnFormularMax4000Show(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das aufgerufen wird, wenn ein FormularMax4000
   * beendet wird und die entsprechenden internen Referenzen gelöscht werden können.
   * 
   * Dieses Event wird vom EventProcessor geworfen, wenn der FormularMax zurückkehrt.
   */
  public static void handleFormularMax4000Returned(TextDocumentController documentController)
  {
    handle(new OnFormularMax4000Returned(documentController));
  }

  

  // *******************************************************************************************


  /**
   * Erzeugt ein neues WollMuxEvent, das Auskunft darüber gibt, dass ein TextDokument
   * geschlossen wurde und damit auch das TextDocumentModel disposed werden soll.
   * 
   * Dieses Event wird ausgelöst, wenn ein TextDokument geschlossen wird.
   * 
   * @param docInfo
   *          ein {@link DocumentManager.Info} Objekt, an dem das TextDocumentModel
   *          dranhängt des Dokuments, das geschlossen wurde. ACHTUNG! docInfo hat
   *          nicht zwingend ein TextDocumentModel. Es muss
   *          {@link DocumentManager.Info#hasTextDocumentModel()} verwendet werden.
   * 
   * 
   *          ACHTUNG! ACHTUNG! Die Implementierung wurde extra so gewählt, dass hier
   *          ein DocumentManager.Info anstatt direkt eines TextDocumentModel
   *          übergeben wird. Es kam nämlich bei einem Dokument, das schnell geöffnet
   *          und gleich wieder geschlossen wurde zu folgendem Deadlock:
   * 
   *          {@link OnProcessTextDocument} =>
   *          {@link de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo#getTextDocumentController()}
   *          => {@link TextDocumentModel#TextDocumentModel(XTextDocument)} =>
   *          {@link DispatchProviderAndInterceptor#registerDocumentDispatchInterceptor(XFrame)}
   *          => OOo Proxy =>
   *          {@link GlobalEventListener#notifyEvent(com.sun.star.document.EventObject)}
   *          ("OnUnload") =>
   *          {@link de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo#hasTextDocumentModel()}
   * 
   *          Da {@link TextDocumentInfo} synchronized ist kam es zum Deadlock.
   * 
   */
  public static void handleTextDocumentClosed(DocumentManager.Info docInfo)
  {
    handle(new OnTextDocumentClosed(docInfo));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die eigentliche Dokumentbearbeitung eines
   * TextDokuments startet.
   * 
   * @param documentController
   *          Das XTextDocument, das durch den WollMux verarbeitet werden soll.
   * @param visible
   *          false zeigt an, dass das Dokument (bzw. das zugehörige Fenster)
   *          unsichtbar ist.
   */
  public static void handleProcessTextDocument(TextDocumentController documentController,
      boolean visible)
  {
    handle(new OnProcessTextDocument(documentController, visible));
  }

  

  // *******************************************************************************************

  /**
   * Wird wollmux:Open mit der Option FORMGUIS "merged" gestartet, so werden zuerst
   * die Einzeldokumente geöffnet und dann dieses Event aufgerufen, das dafür
   * zuständig ist, die eine FormGUI für den MultiDocument-Modus zu erzeugen und zu
   * starten.
   * 
   * @param documentControllers
   *          Ein Vector of TextDocumentModels, die in einem Multiformular
   *          zusammengefasst werden sollen.
   */
  public static void handleProcessMultiform(
      List<TextDocumentController> documentControllers,
      ConfigThingy buttonAnpassung)
  {
    handle(new OnProcessMultiform(documentControllers, buttonAnpassung));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die Bearbeitung aller neu hinzugekommener
   * Dokumentkommandos übernimmt.
   * 
   * Dieses Event wird immer dann ausgelöst, wenn nach dem Öffnen eines Dokuments
   * neue Dokumentkommandos eingefügt wurden, die nun bearbeitet werden sollen - z.B.
   * wenn ein Textbaustein eingefügt wurde.
   * 
   * @param documentController
   *          Das TextDocumentModel, das durch den WollMux verarbeitet werden soll.
   */
  public static void handleReprocessTextDocument(TextDocumentController documentController)
  {
    handle(new OnReprocessTextDocument(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Obsolete, aber aus Kompatibilitätgründen noch vorhanden. Bitte handleOpen()
   * statt dessen verwenden.
   * 
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass ein Dokument geöffnet
   * wird.
   * 
   * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
   * Dispatch-Kommando wollmux:openTemplate bzw. wollmux:openDocument empfängt und
   * sort dafür, dass das entsprechende Dokument geöffnet wird.
   * 
   * @param fragIDs
   *          Eine List mit fragIDs, wobei das erste Element die FRAG_ID des zu
   *          öffnenden Dokuments beinhalten muss. Weitere Elemente werden in eine
   *          Liste zusammengefasst und als Parameter für das Dokumentkommando
   *          insertContent verwendet.
   * @param asTemplate
   *          true, wenn das Dokument als "Unbenannt X" geöffnet werden soll (also im
   *          "Template-Modus") und false, wenn das Dokument zum Bearbeiten geöffnet
   *          werden soll.
   */
  public static void handleOpenDocument(List<String> fragIDs, boolean asTemplate)
  {
    handle(new OnOpenDocument(fragIDs, asTemplate));
  }

  

  // *******************************************************************************************

  /**
   * Öffnet ein oder mehrere Dokumente anhand der Beschreibung openConfStr
   * (ConfigThingy-Syntax) und ist ausführlicher beschrieben unter
   * http://limux.tvc.muenchen
   * .de/wiki/index.php/Schnittstellen_des_WollMux_f%C3%BCr_Experten#wollmux:Open
   * 
   * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
   * Dispatch-Kommando wollmux:open empfängt.
   * 
   * @param openConfStr
   *          Die Beschreibung der zu öffnenden Fragmente in ConfigThingy-Syntax
   */
  public static void handleOpen(String openConfStr)
  {
    handle(new OnOpen(openConfStr));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass alle registrierten
   * XPALChangeEventListener geupdated werden.
   */
  public static void handlePALChangedNotify()
  {
    handle(new OnPALChangedNotify());
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum setzen des aktuellen Absenders.
   * 
   * @param senderName
   *          Name des Senders in der Form "Nachname, Vorname (Rolle)" wie sie auch
   *          der PALProvider bereithält.
   * @param idx
   *          der zum Sender senderName passende index in der sortierten Senderliste
   *          - dient zur Konsistenz-Prüfung, damit kein Sender gesetzt wird, wenn
   *          die PAL der setzenden Komponente nicht mit der PAL des WollMux
   *          übereinstimmt.
   */
  public static void handleSetSender(String senderName, int idx)
  {
    handle(new OnSetSender(senderName, idx));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle Formularfelder
   * Dokument auf den neuen Wert gesetzt werden. Bei Formularfeldern mit
   * TRAFO-Funktion wird die Transformation entsprechend durchgeführt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI der Wert des Formularfeldes fieldID geändert wurde und sorgt dafür,
   * dass die Wertänderung auf alle betroffenen Formularfelder im Dokument doc
   * übertragen werden.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          Die ID der Formularfelder, deren Werte angepasst werden sollen.
   * @param newValue
   *          Der neue untransformierte Wert des Formularfeldes.
   * @param funcLib
   *          Die Funktionsbibliothek, die zur Gewinnung der Trafo-Funktion verwendet
   *          werden soll.
   */
  public static void handleFormValueChanged(TextDocumentController documentController, String fieldId,
      String newValue)
  {
    handle(new OnFormValueChanged(documentController, fieldId, newValue));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle
   * Sichtbarkeitselemente (Dokumentkommandos oder Bereiche mit Namensanhang 'GROUPS
   * ...') im übergebenen Dokument, die einer bestimmten Gruppe groupId zugehören
   * ein- oder ausgeblendet werden.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI bestimmte Text-Teile des übergebenen Dokuments ein- oder
   * ausgeblendet werden sollen. Auch das PrintModel verwendet dieses Event, wenn
   * XPrintModel.setGroupVisible() aufgerufen wurde.
   * 
   * @param documentController
   *          Das TextDocumentModel, welches die Sichtbarkeitselemente enthält.
   * @param groupId
   *          Die GROUP (ID) der ein/auszublendenden Gruppe.
   * @param visible
   *          Der neue Sichtbarkeitsstatus (true=sichtbar, false=ausgeblendet)
   * @param listener
   *          Der listener, der nach Durchführung des Events benachrichtigt wird
   *          (kann auch null sein, dann gibt's keine Nachricht).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void handleSetVisibleState(TextDocumentController documentController, String groupId,
      boolean visible, ActionListener listener)
  {
    handle(new OnSetVisibleState(documentController, groupId, visible, listener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das den ViewCursor des Dokuments auf das aktuell in der
   * Formular-GUI bearbeitete Formularfeld setzt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI ein Formularfeld den Fokus bekommen hat und es sorgt dafür, dass
   * der View-Cursor des Dokuments das entsprechende FormField im Dokument anspringt.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          die ID des Formularfeldes das den Fokus bekommen soll. Besitzen mehrere
   *          Formularfelder diese ID, so wird bevorzugt das erste Formularfeld aus
   *          dem Vektor genommen, das keine Trafo enthält. Ansonsten wird das erste
   *          Formularfeld im Vektor verwendet.
   */
  public static void handleFocusFormField(TextDocumentController documentController, String fieldId)
  {
    handle(new OnFocusFormField(documentController, fieldId));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Position und Größe des übergebenen Dokument-Fensters
   * auf die vorgegebenen Werte setzt. ACHTUNG: Die Maßangaben beziehen sich auf die
   * linke obere Ecke des Fensterinhalts OHNE die Titelzeile und die
   * Fensterdekoration des Rahmens. Um die linke obere Ecke des gesamten Fensters
   * richtig zu setzen, müssen die Größenangaben des Randes der Fensterdekoration und
   * die Höhe der Titelzeile VOR dem Aufruf der Methode entsprechend eingerechnet
   * werden.
   * 
   * @param model
   *          Das XModel-Interface des Dokuments dessen Position/Größe gesetzt werden
   *          soll.
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position in
   *          Pixel, gezählt von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gezählt von links oben.
   * @param docWidth
   *          Die Größe des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Größe des Dokuments auf der Y-Achse in Pixel. Auch hier wird die
   *          Titelzeile des Rahmens nicht beachtet und muss vorher entsprechend
   *          eingerechnet werden.
   */
  public static void handleSetWindowPosSize(TextDocumentController documentController, int docX,
      int docY, int docWidth, int docHeight)
  {
    handle(new OnSetWindowPosSize(documentController, docX, docY, docWidth, docHeight));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Anzeige des übergebenen Dokuments auf sichtbar oder
   * unsichtbar schaltet. Dabei wird direkt die entsprechende Funktion der UNO-API
   * verwendet.
   * 
   * @param documentController
   *          Das XModel interface des dokuments, welches sichtbar oder unsichtbar
   *          geschaltet werden soll.
   * @param visible
   *          true, wenn das Dokument sichtbar geschaltet werden soll und false, wenn
   *          das Dokument unsichtbar geschaltet werden soll.
   */
  public static void handleSetWindowVisible(TextDocumentController documentController, boolean visible)
  {
    handle(new OnSetWindowVisible(documentController, visible));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument schließt.
   * 
   * @param documentController
   *          Das zu schließende TextDocumentModel.
   */
  public static void handleCloseTextDocument(TextDocumentController documentController)
  {
    handle(new OnCloseTextDocument(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument in eine temporäre Datei
   * speichert, eine externe Anwendung mit dieser aufruft und das Dokument dann
   * schließt, wobei der ExterneAnwendungen-Abschnitt zu ext die näheren Details wie
   * den FILTER regelt.
   * 
   * @param documentController
   *          Das an die externe Anwendung weiterzureichende TextDocumentModel.
   * @param ext
   *          identifiziert den entsprechenden Eintrag im Abschnitt
   *          ExterneAnwendungen.
   */
  public static void handleCloseAndOpenExt(TextDocumentController documentController, String ext)
  {
    handle(new OnCloseAndOpenExt(documentController, ext));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument in eine temporäre Datei speichert
   * und eine externe Anwendung mit dieser aufruft, wobei der
   * ExterneAnwendungen-Abschnitt zu ext die näheren Details wie den FILTER regelt.
   * 
   * @param documentController
   *          Das an die externe Anwendung weiterzureichende TextDocumentModel.
   * @param ext
   *          identifiziert den entsprechenden Eintrag im Abschnitt
   *          ExterneAnwendungen.
   */
  public static void handleSaveTempAndOpenExt(TextDocumentController documentController, String ext)
  {
    handle(new OnSaveTempAndOpenExt(documentController, ext));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das ggf. notwendige interaktive
   * Initialisierungen vornimmt. Derzeit wird vor allem die Konsitenz der
   * persönlichen Absenderliste geprüft und der AbsenderAuswählen Dialog gestartet,
   * falls die Liste leer ist.
   */
  public static void handleInitialize()
  {
    handle(new OnInitialize());
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren des übergebenen
   * XPALChangeEventListeners.
   * 
   * @param listener
   */
  public static void handleAddPALChangeEventListener(
      XPALChangeEventListener listener, Integer wollmuxConfHashCode)
  {
    handle(new OnAddPALChangeEventListener(listener, wollmuxConfHashCode));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den übergebenen XPALChangeEventListener
   * deregistriert.
   * 
   * @param listener
   *          der zu deregistrierende XPALChangeEventListener
   */
  public static void handleRemovePALChangeEventListener(
      XPALChangeEventListener listener)
  {
    handle(new OnRemovePALChangeEventListener(listener));
  }

  // *******************************************************************************************
  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren eines (frischen)
   * {@link DispatchProviderAndInterceptor} auf frame.
   * 
   * @param frame
   *          der {@link XFrame} auf den der {@link DispatchProviderAndInterceptor}
   *          registriert werden soll.
   */
  public static void handleRegisterDispatchInterceptor(TextDocumentController documentController)
  {
      handle(new OnRegisterDispatchInterceptor(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren des übergebenen XEventListeners
   * und wird vom WollMux-Service aufgerufen.
   * 
   * @param listener
   *          der zu registrierende XEventListener.
   */
  public static void handleAddDocumentEventListener(XEventListener listener)
  {
    handle(new OnAddDocumentEventListener(listener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den übergebenen XEventListener zu
   * deregistriert.
   * 
   * @param listener
   *          der zu deregistrierende XEventListener
   */
  public static void handleRemoveDocumentEventListener(XEventListener listener)
  {
    handle(new OnRemoveDocumentEventListener(listener));
  }

  

  // *******************************************************************************************

  /**
   * Über dieses Event werden alle registrierten DocumentEventListener (falls
   * listener==null) oder ein bestimmter registrierter DocumentEventListener (falls
   * listener != null) (XEventListener-Objekte) über Statusänderungen der
   * Dokumentbearbeitung informiert
   * 
   * @param listener
   *          der zu benachrichtigende XEventListener. Falls null werden alle
   *          registrierten Listener benachrichtig. listener wird auf jeden Fall nur
   *          benachrichtigt, wenn er zur Zeit der Abarbeitung des Events noch
   *          registriert ist.
   * @param eventName
   *          Name des Events
   * @param source
   *          Das von der Statusänderung betroffene Dokument (üblicherweise eine
   *          XComponent)
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void handleNotifyDocumentEventListener(XEventListener listener,
      String eventName, Object source)
  {
    handle(new OnNotifyDocumentEventListener(listener, eventName, source));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent das signaisiert, dass die Druckfunktion
   * aufgerufen werden soll, die im TextDocumentModel model aktuell definiert ist.
   * Die Methode erwartet, dass vor dem Aufruf geprüft wurde, ob model eine
   * Druckfunktion definiert. Ist dennoch keine Druckfunktion definiert, so erscheint
   * eine Fehlermeldung im Log.
   * 
   * Das Event wird ausgelöst, wenn der registrierte WollMuxDispatchInterceptor eines
   * Dokuments eine entsprechende Nachricht bekommt.
   */
  public static void handleExecutePrintFunctions(TextDocumentController documentController)
  {
    handle(new OnExecutePrintFunction(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Diese Methode erzeugt ein neues WollMuxEvent, mit dem die Eigenschaften der
   * Druckblöcke (z.B. allVersions) gesetzt werden können.
   * 
   * Das Event dient als Hilfe für die Komfortdruckfunktionen und wird vom
   * XPrintModel aufgerufen und mit diesem synchronisiert.
   * 
   * @param blockName
   *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
   * @param visible
   *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
   *          visible==false.
   * @param showHighlightColor
   *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur, wenn
   *          zu einem betroffenen Druckblock auch eine Hintergrundfarbe angegeben
   *          ist).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void handleSetPrintBlocksPropsViaPrintModel(XTextDocument doc,
      String blockName, boolean visible, boolean showHighlightColor,
      ActionListener listener)
  {
    handle(new OnSetPrintBlocksPropsViaPrintModel(doc, blockName, visible,
      showHighlightColor, listener));
  }

  

  // *******************************************************************************************

  /**
   * Diese Methode erzeugt ein neues WollMux-Event über das die Liste der dem
   * Dokument doc zugeordneten Druckfunktionen verwaltet werden kann; ist
   * remove==false, so wird die Druckfunktion functionName in die Liste der
   * Druckfunktionen für dieses Dokument aufgenommen; ist remove==true, so wird die
   * Druckfunktion aus der Liste entfernt.
   * 
   * @param doc
   *          beschreibt das Dokument dessen Druckfunktionen verwaltet werden sollen.
   * @param functionName
   *          der Name der Druckfunktion, die hinzugefügt oder entfernt werden soll.
   * @param remove
   *          ist remove==false, so wird die Druckfunktion functionName in die Liste
   *          der Druckfunktionen für dieses Dokument aufgenommen; ist remove==true,
   *          so wird die Druckfunktion aus der Liste entfernt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void handleManagePrintFunction(XTextDocument doc,
      String functionName, boolean remove)
  {
    handle(new OnManagePrintFunction(doc, functionName, remove));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine weitere Ziffer der
   * Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende Ziffer gelöscht
   * werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines Drucks
   * auf den Knopf der OOo-Symbolleiste ein "wollmux:ZifferEinfuegen" dispatch
   * erfolgte.
   */
  public static void handleButtonZifferEinfuegenPressed(TextDocumentController documentController)
  {
    handle(new OnZifferEinfuegen(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine Abdruckzeile der
   * Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende Abdruckzeile
   * gelöscht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines Drucks
   * auf den Knopf der OOo-Symbolleiste ein "wollmux:Abdruck" dispatch erfolgte.
   */
  public static void handleButtonAbdruckPressed(TextDocumentController documentController)
  {
    handle(new OnAbdruck(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine Zuleitungszeile der
   * Sachleitenden Verfügungen eingefügt werden, bzw. eine bestehende Zuleitungszeile
   * gelöscht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines Drucks
   * auf den Knopf der OOo-Symbolleiste ein "wollmux:Zuleitungszeile" dispatch
   * erfolgte.
   */
  public static void handleButtonZuleitungszeilePressed(TextDocumentController documentController)
  {
    handle(new OnButtonZuleitungszeilePressed(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das über den Bereich des viewCursors im Dokument
   * doc ein neues Bookmark mit dem Namen "WM(CMD'<blockname>')" legt, wenn nicht
   * bereits ein solches Bookmark im markierten Block definiert ist. Ist bereits ein
   * Bookmark mit diesem Namen vorhanden, so wird dieses gelöscht.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines Drucks
   * auf den Knopf der OOo-Symbolleiste ein "wollmux:markBlock#<blockname>" dispatch
   * erfolgte.
   * 
   * @param documentController
   *          Das Textdokument, in dem der Block eingefügt werden soll.
   * @param blockname
   *          Derzeit werden folgende Blocknamen akzeptiert "draftOnly",
   *          "notInOriginal", "originalOnly", "copyOnly" und "allVersions". Alle
   *          anderen Blocknamen werden ignoriert und keine Aktion ausgeführt.
   */
  public static void handleMarkBlock(TextDocumentController documentController, String blockname)
  {
    handle(new OnMarkBlock(documentController, blockname));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass eine Datei wollmux.dump
   * erzeugt wird, die viele für die Fehlersuche relevanten Informationen enthält wie
   * z.B. Versionsinfo, Inhalt der wollmux.conf, cache.conf, StringRepräsentation der
   * Konfiguration im Speicher und eine Kopie der Log-Datei.
   * 
   * Das Event wird von der WollMuxBar geworfen, die (speziell für Admins, nicht für
   * Endbenutzer) einen entsprechenden Button besitzt.
   */
  public static void handleDumpInfo()
  {
    handle(new OnDumpInfo());
  }

  /**
   * Druckt die aktuelle Seite eines Dokuments auf dem Standarddrucker aus.
   */
  public static void handlePrintPage(TextDocumentController documentController)
  {
    handle(new OnPrintPage(documentController));
  }

  


  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass das gesamte Office (und
   * damit auch der WollMux) OHNE Sicherheitsabfragen(!) beendet werden soll.
   * 
   * Das Event wird von der WollMuxBar geworfen, die (speziell für Admins, nicht für
   * Endbenutzer) einen entsprechenden Button besitzt.
   */
  public static void handleKill()
  {
    handle(new OnKill());
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass im Textdokument doc das
   * Formularfeld mit der ID id auf den Wert value gesetzt wird. Ist das Dokument ein
   * Formulardokument (also mit einer angezeigten FormGUI), so wird die Änderung über
   * die FormGUI vorgenommen, die zugleich dafür sorgt, dass von id abhängige
   * Formularfelder mit angepasst werden. Besitzt das Dokument keine
   * Formularbeschreibung, so wird der Wert direkt gesetzt, ohne Äbhängigkeiten zu
   * beachten. Nach der erfolgreichen Ausführung aller notwendigen Anpassungen wird
   * der unlockActionListener benachrichtigt.
   * 
   * Das Event wird aus den Implementierungen von XPrintModel (siehe
   * TextDocumentModel) und XWollMuxDocument (siehe compo.WollMux) geworfen, wenn
   * dort die Methode setFormValue aufgerufen wird.
   * 
   * @param doc
   *          Das Dokument, in dem das Formularfeld mit der ID id neu gesetzt werden
   *          soll.
   * @param id
   *          Die ID des Formularfeldes, dessen Wert verändert werden soll. Ist die
   *          FormGUI aktiv, so werden auch alle von id abhängigen Formularwerte neu
   *          gesetzt.
   * @param value
   *          Der neue Wert des Formularfeldes id
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle notwendigen
   *          Anpassungen durchgeführt wurden.
   */
  public static void handleSetFormValue(XTextDocument doc, String id, String value,
      ActionListener unlockActionListener)
  {
    handle(new OnSetFormValue(doc, id, value, unlockActionListener));
  }

  

  // *******************************************************************************************

  /**
   * Sammelt alle Formularfelder des Dokuments model auf, die nicht von
   * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden und
   * befüllt werden (derzeit c,s,s,t,textfield,Database-Felder).
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe TextDocumentModel)
   * geworfen, wenn dort die Methode collectNonWollMuxFormFields aufgerufen wird.
   * 
   * @param documentController
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle notwendigen
   *          Anpassungen durchgeführt wurden.
   */
  public static void handleCollectNonWollMuxFormFieldsViaPrintModel(
      TextDocumentController documentController, ActionListener listener)
  {
    handle(new OnCollectNonWollMuxFormFieldsViaPrintModel(documentController, listener));
  }

  

  // *******************************************************************************************

  /**
   * Dieses WollMuxEvent ist das Gegenstück zu handleSetFormValue und wird dann
   * erzeugt, wenn nach einer Änderung eines Formularwertes - gesteuert durch die
   * FormGUI - alle abhängigen Formularwerte angepasst wurden. In diesem Fall ist die
   * einzige Aufgabe dieses Events, den unlockActionListener zu informieren, den
   * handleSetFormValueViaPrintModel() nicht selbst informieren konnte.
   * 
   * Das Event wird aus der Implementierung vom OnSetFormValueViaPrintModel.doit()
   * erzeugt, wenn Feldänderungen über die FormGUI laufen.
   * 
   * @param unlockActionListener
   *          Der zu informierende unlockActionListener.
   */
  public static void handleSetFormValueFinished(ActionListener unlockActionListener)
  {
    handle(new OnSetFormValueFinished(unlockActionListener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass im Textdokument doc alle
   * insertValue-Befehle mit einer DB_SPALTE, die in der übergebenen
   * mapDbSpalteToValue enthalten sind neu für den entsprechenden Wert evaluiert und
   * gesetzt werden, unabhängig davon, ob sie den Status DONE besitzen oder nicht.
   * 
   * Das Event wird aus der Implementierung von XWollMuxDocument (siehe
   * compo.WollMux) geworfen, wenn dort die Methode setInsertValue aufgerufen wird.
   * 
   * @param doc
   *          Das Dokument, in dem das die insertValue-Kommandos neu gesetzt werden
   *          sollen.
   * @param mapDbSpalteToValue
   *          Enthält eine Zuordnung von DB_SPALTEn auf die zu setzenden Werte.
   *          Enthält ein betroffenes Dokumentkommando eine Trafo, so wird die Trafo
   *          mit dem zugehörigen Wert ausgeführt und das Transformationsergebnis als
   *          neuer Inhalt des Bookmarks gesetzt.
   * @param unlockActionListener
   *          Wird informiert, sobald das Event vollständig abgearbeitet wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void handleSetInsertValues(XTextDocument doc,
      Map<String, String> mapDbSpalteToValue, ActionListener unlockActionListener)
  {
    handle(new OnSetInsertValues(doc, mapDbSpalteToValue, unlockActionListener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das ein Textbaustein über den
   * Textbaustein-Bezeichner direkt ins Dokument eingefügt wird. Mit reprocess wird
   * übergeben, wann die Dokumentenkommandos ausgewertet werden soll. Mir reprocess =
   * true sofort.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen z.B über Druck eines
   * Tastenkuerzels oder Druck auf den Knopf der OOo-Symbolleiste ein
   * "wollmux:TextbausteinEinfuegen" dispatch erfolgte.
   */
  public static void handleTextbausteinEinfuegen(TextDocumentController documentController,
      boolean reprocess)
  {
    handle(new OnTextbausteinEinfuegen(documentController, reprocess));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das der nächste Platzhalter
   * ausgehend vom Cursor angesprungen wird * Das Event wird von
   * WollMux.dispatch(...) geworfen z.B über Druck eines Tastenkuerzels oder Druck
   * auf den Knopf der OOo-Symbolleiste ein "wollmux:PlatzhalterAnspringen" dispatch
   * erfolgte.
   */
  public static void handleJumpToPlaceholder(TextDocumentController documentController)
  {
    handle(new OnJumpToPlaceholder(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das die nächste Marke
   * 'setJumpMark' angesprungen werden soll. Wird im
   * DocumentCommandInterpreter.DocumentExpander.fillPlaceholders aufgerufen wenn
   * nach dem Einfügen von Textbausteine keine Einfügestelle vorhanden ist aber eine
   * Marke 'setJumpMark'
   */
  public static void handleJumpToMark(XTextDocument doc, boolean msg)
  {
    handle(new OnJumpToMark(doc, msg));
  }

  


  // *******************************************************************************************

  /**
   * Der Handler für das Drucken eines TextDokuments führt in Abhängigkeit von der
   * Existenz von Serienbrieffeldern und Druckfunktion die entsprechenden Aktionen
   * aus.
   * 
   * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
   * "Datei->Drucken" oder über die Symbolleiste die dispatch-url .uno:Print bzw.
   * .uno:PrintDefault abgesetzt wurde.
   */
  public static void handlePrint(TextDocumentController documentController, XDispatch origDisp,
      com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
  {
    handle(new OnPrint(documentController, origDisp, origUrl, origArgs));
  }

  

  // *******************************************************************************************

  /**
   * Der Handler für einen abgespeckten Speichern-Unter-Dialog des WollMux, der in
   * Abängigkeit von einer gesetzten FilenameGeneratorFunction über den WollMux
   * aufgrufen und mit dem generierten Filenamen vorbelegt wird.
   * 
   * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
   * "Datei->SaveAs" oder über die Symbolleiste die dispatch-url .uno:Save bzw.
   * .uno:SaveAs abgesetzt wurde.
   */
  public static void handleSaveAs(TextDocumentController documentController, DispatchHelper helper, boolean sync)
  {
    BasicEvent event = new OnSaveAs(documentController, helper);
    if (sync)
    {
      event.process();
    }
    else
    {
      handle(event);
    }
  }
  
  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass der FormController (der
   * zeitgleich mit einer FormGUI zum TextDocument model gestartet wird) vollständig
   * initialisiert ist und notwendige Aktionen wie z.B. das Zurücksetzen des
   * Modified-Status des Dokuments durchgeführt werden können. Vor dem Zurücksetzen
   * des Modified-Status, wird auf die erste Seite des Dokuments gesprungen.
   * 
   * Das Event wird vom FormModel erzeugt, wenn es vom FormController eine
   * entsprechende Nachricht erhält.
   */
  public static void handleFormControllerInitCompleted(TextDocumentController documentController)
  {
    handle(new OnFormControllerInitCompleted(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMux-Event, in dem geprüft wird, ob der WollMux korrekt
   * installiert ist und keine Doppel- oder Halbinstallationen vorliegen. Ist der
   * WollMux fehlerhaft installiert, erscheint eine Fehlermeldung mit entsprechenden
   * Hinweisen.
   * 
   * Das Event wird geworfen, wenn der WollMux startet.
   */
  public static void handleCheckInstallation()
  {
    handle(new OnCheckInstallation());
  }

  

  // *******************************************************************************************
  // Globale Helper-Methoden

  public static ConfigThingy requireLastSection(ConfigThingy cf, String sectionName)
      throws ConfigurationErrorException
  {
    try
    {
      return cf.query(sectionName).getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(L.m(
        "Der Schlüssel '%1' fehlt in der Konfigurationsdatei.", sectionName), e);
    }
  }
}
