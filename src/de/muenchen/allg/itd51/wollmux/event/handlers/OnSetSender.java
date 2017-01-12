package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird ausgelöst, wenn im WollMux-Service die methode setSender
 * aufgerufen wird. Es sort dafür, dass ein neuer Absender gesetzt wird.
 * 
 * @author christoph.lutz
 */
public class OnSetSender extends BasicEvent
{
  private String senderName;

  private int idx;

  public OnSetSender(String senderName, int idx)
  {
    this.senderName = senderName;
    this.idx = idx;
  }

  @Override
  protected void doit()
  {
    String[] pal = PersoenlicheAbsenderliste.getInstance().getPALEntries();

    // nur den neuen Absender setzen, wenn index und sender übereinstimmen,
    // d.h.
    // die Absenderliste der entfernten WollMuxBar konsistent war.
    if (idx >= 0 && idx < pal.length && pal[idx].toString().equals(senderName))
    {
      DJDatasetListElement[] palDatasets = PersoenlicheAbsenderliste.getInstance().getSortedPALEntries();
      palDatasets[idx].getDataset().select();
    } else
    {
      Logger.error(
          L.m("Setzen des Senders '%1' schlug fehl, da der index '%2' nicht mit der PAL übereinstimmt (Inkonsistenzen?)",
              senderName, idx));
    }

    WollMuxEventHandler.handlePALChangedNotify();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + senderName + ", " + idx + ")";
  }
}