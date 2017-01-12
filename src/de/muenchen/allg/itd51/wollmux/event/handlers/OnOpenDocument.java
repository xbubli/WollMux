package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class OnOpenDocument extends BasicEvent
{
  private boolean asTemplate;

  private List<String> fragIDs;

  public OnOpenDocument(List<String> fragIDs, boolean asTemplate)
  {
    this.fragIDs = fragIDs;
    this.asTemplate = asTemplate;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // Baue ein ConfigThingy (als String), das die neue open-Methode versteht
    // und leite es weiter an diese.
    Iterator<String> iter = fragIDs.iterator();
    StringBuffer fragIdStr = new StringBuffer();
    while (iter.hasNext())
    {
      fragIdStr.append("'");
      fragIdStr.append(iter.next());
      fragIdStr.append("' ");
    }
    WollMuxEventHandler.handleOpen("AS_TEMPLATE '" + asTemplate + "' FORMGUIS 'independent' Fragmente( FRAG_ID_LIST ("
        + fragIdStr.toString() + "))");
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + ((asTemplate) ? "asTemplate" : "asDocument") + ", " + fragIDs + ")";
  }
}