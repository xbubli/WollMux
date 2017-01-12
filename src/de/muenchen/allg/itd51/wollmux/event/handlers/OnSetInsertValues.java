package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;
import java.util.Map;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnSetInsertValues extends BasicEvent
{
  private XTextDocument doc;

  private Map<String, String> mapDbSpalteToValue;

  private ActionListener listener;

  public OnSetInsertValues(XTextDocument doc, Map<String, String> mapDbSpalteToValue,
      ActionListener unlockActionListener)
  {
    this.doc = doc;
    this.mapDbSpalteToValue = mapDbSpalteToValue;
    this.listener = unlockActionListener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);

    for (DocumentCommand cmd : documentController.getModel().getDocumentCommands())
      // stellt sicher, dass listener am Schluss informiert wird
      try
      {
        if (cmd instanceof DocumentCommand.InsertValue)
        {
          DocumentCommand.InsertValue insVal = (DocumentCommand.InsertValue) cmd;
          String value = mapDbSpalteToValue.get(insVal.getDBSpalte());
          if (value != null)
          {
            value = documentController.getTransformedValue(insVal.getTrafoName(), value);
            if ("".equals(value))
            {
              cmd.setTextRangeString("");
            } else
            {
              cmd.setTextRangeString(insVal.getLeftSeparator() + value + insVal.getRightSeparator());
            }
          }
        }
      } catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", Nr.Values=" + mapDbSpalteToValue.size() + ")";
  }
}