package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.text.XTextCursor;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;
import de.muenchen.allg.ooo.TextDocument;

public class OnMarkBlock extends BasicEvent
{
  private String blockname;
  private TextDocumentController documentController;

  public OnMarkBlock(TextDocumentController documentController, String blockname)
  {
    this.documentController = documentController;
    this.blockname = blockname;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (UNO.XBookmarksSupplier(documentController.getModel().doc) == null || blockname == null)
      return;

    ConfigThingy slvConf = WollMuxFiles.getWollmuxConf().query("SachleitendeVerfuegungen");
    Integer highlightColor = null;

    XTextCursor range = documentController.getModel().getViewCursor();

    if (range == null)
      return;

    if (range.isCollapsed())
    {
      ModalDialogs.showInfoModal(L.m("Fehler"), L.m("Bitte wählen Sie einen Bereich aus, der markiert werden soll."));
      return;
    }

    String markChange = null;
    if (blockname.equalsIgnoreCase("allVersions"))
    {
      markChange = L.m("wird immer gedruckt");
      highlightColor = getHighlightColor(slvConf, "ALL_VERSIONS_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("draftOnly"))
    {
      markChange = L.m("wird nur im Entwurf gedruckt");
      highlightColor = getHighlightColor(slvConf, "DRAFT_ONLY_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("notInOriginal"))
    {
      markChange = L.m("wird im Original nicht gedruckt");
      highlightColor = getHighlightColor(slvConf, "NOT_IN_ORIGINAL_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("originalOnly"))
    {
      markChange = L.m("wird ausschließlich im Original gedruckt");
      highlightColor = getHighlightColor(slvConf, "ORIGINAL_ONLY_HIGHLIGHT_COLOR");
    } else if (blockname.equalsIgnoreCase("copyOnly"))
    {
      markChange = L.m("wird ausschließlich in Abdrucken gedruckt");
      highlightColor = getHighlightColor(slvConf, "COPY_ONLY_HIGHLIGHT_COLOR");
    } else
      return;

    String bookmarkStart = "WM(CMD '" + blockname + "'";
    String hcAtt = "";
    if (highlightColor != null)
    {
      String colStr = "00000000";
      colStr += Integer.toHexString(highlightColor.intValue());
      colStr = colStr.substring(colStr.length() - 8, colStr.length());
      hcAtt = " HIGHLIGHT_COLOR '" + colStr + "'";
    }
    String bookmarkName = bookmarkStart + hcAtt + ")";

    Pattern bookmarkPattern = DocumentCommands.getPatternForCommand(blockname);
    Set<String> bmNames = TextDocument.getBookmarkNamesMatching(bookmarkPattern, range);

    if (bmNames.size() > 0)
    {
      // bereits bestehende Blöcke löschen
      Iterator<String> iter = bmNames.iterator();
      while (iter.hasNext())
      {
        bookmarkName = iter.next();
        try
        {
          Bookmark b = new Bookmark(bookmarkName, UNO.XBookmarksSupplier(documentController.getModel().doc));
          if (bookmarkName.contains("HIGHLIGHT_COLOR"))
            UNO.setPropertyToDefault(b.getTextCursor(), "CharBackColor");
          b.remove();
        } catch (NoSuchElementException e)
        {
        }
      }
      ModalDialogs.showInfoModal(L.m("Markierung des Blockes aufgehoben"),
          L.m("Der ausgewählte Block enthielt bereits eine Markierung 'Block %1'. Die bestehende Markierung wurde aufgehoben.",
              markChange));
    } else
    {
      // neuen Block anlegen
      documentController.getModel().addNewDocumentCommand(range, bookmarkName);
      if (highlightColor != null)
      {
        UNO.setProperty(range, "CharBackColor", highlightColor);
        // ViewCursor kollabieren, da die Markierung die Farben verfälscht
        // darstellt.
        XTextCursor vc = documentController.getModel().getViewCursor();
        if (vc != null)
          vc.collapseToEnd();
      }
      ModalDialogs.showInfoModal(L.m("Block wurde markiert"), L.m("Der ausgewählte Block %1.", markChange));
    }

    // PrintBlöcke neu einlesen:
    documentController.getModel().getDocumentCommands().update();
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(documentController, WollMuxFiles.isDebugMode());
    dci.scanGlobalDocumentCommands();
    dci.scanInsertFormValueCommands();

    stabilize();
  }

  /**
   * Liefert einen Integer der Form AARRGGBB (hex), der den Farbwert
   * repräsentiert, der in slvConf im Attribut attribute hinterlegt ist oder
   * null, wenn das Attribut nicht existiert oder der dort enthaltene
   * String-Wert sich nicht in eine Integerzahl konvertieren lässt.
   * 
   * @param slvConf
   * @param attribute
   */
  private static Integer getHighlightColor(ConfigThingy slvConf, String attribute)
  {
    try
    {
      String highlightColor = slvConf.query(attribute).getLastChild().toString();
      if (highlightColor.equals("") || highlightColor.equalsIgnoreCase("none"))
        return null;
      int hc = Integer.parseInt(highlightColor, 16);
      return Integer.valueOf(hc);
    } catch (NodeNotFoundException e)
    {
      return null;
    } catch (NumberFormatException e)
    {
      Logger.error(L.m("Der angegebene Farbwert im Attribut '%1' ist ungültig!", attribute));
      return null;
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", '" + blockname + "')";
  }
}