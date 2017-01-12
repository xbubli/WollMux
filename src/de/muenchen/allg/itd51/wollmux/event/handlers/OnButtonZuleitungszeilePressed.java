package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnButtonZuleitungszeilePressed extends BasicEvent
{
  private TextDocumentController documentController;

  public OnButtonZuleitungszeilePressed(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor != null)
    {
      XTextRange vc = SachleitendeVerfuegung.insertZuleitungszeile(documentController, viewCursor);
      if (vc != null)
        viewCursor.gotoRange(vc, false);
    }

    stabilize();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}