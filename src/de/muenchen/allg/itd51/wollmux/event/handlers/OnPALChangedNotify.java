package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.IOException;
import java.util.Iterator;

import com.sun.star.lang.EventObject;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;

/**
 * Dieses Event wird immer dann erzeugt, wenn ein Dialog zur Bearbeitung der PAL
 * geschlossen wurde und immer dann wenn die PAL z.B. durch einen
 * wollmux:setSender-Befehl geändert hat. Das Event sorgt dafür, dass alle im
 * WollMuxSingleton registrierten XPALChangeListener geupdatet werden.
 * 
 * @author christoph.lutz
 */
public class OnPALChangedNotify extends BasicEvent
{
  @Override
  protected void doit()
  {
    // registrierte PALChangeListener updaten
    Iterator<XPALChangeEventListener> i = PersoenlicheAbsenderliste.getInstance().iterator();
    while (i.hasNext())
    {
      Logger.debug2("OnPALChangedNotify: Update XPALChangeEventListener");
      EventObject eventObject = new EventObject();
      eventObject.Source = PersoenlicheAbsenderliste.getInstance();
      try
      {
        i.next().updateContent(eventObject);
      } catch (java.lang.Exception x)
      {
        i.remove();
      }
    }

    // Cache und LOS auf Platte speichern.
    try
    {
      DatasourceJoinerFactory.getDatasourceJoiner().saveCacheAndLOS(WollMuxFiles.getLosCacheFile());
    } catch (IOException e)
    {
      Logger.error(e);
    }
  }
}