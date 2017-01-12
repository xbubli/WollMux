package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.TextModule;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class OnTextbausteinEinfuegen extends BasicEvent
{
  private boolean reprocess;
  private TextDocumentController documentController;

  public OnTextbausteinEinfuegen(TextDocumentController documentController, boolean reprocess)
  {
    this.documentController = documentController;
    this.reprocess = reprocess;

  }

  @Override
  protected void doit()
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    try
    {
      TextModule.createInsertFragFromIdentifier(documentController.getModel().doc, viewCursor, reprocess);
      if (reprocess)
        WollMuxEventHandler.handleReprocessTextDocument(documentController);
      if (!reprocess)
        ModalDialogs.showInfoModal(L.m("WollMux"), L.m("Der Textbausteinverweis wurde eingefügt."));
    } catch (WollMuxFehlerException e)
    {
      ModalDialogs.showInfoModal(L.m("WollMux-Fehler"), e.getMessage());
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ", " + reprocess + ")";
  }
}