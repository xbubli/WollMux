package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.sun.star.lang.XComponent;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.document.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.InvalidIdentifierException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class OnOpen extends BasicEvent
{
  private String openConfStr;

  public OnOpen(String openConfStr)
  {
    this.openConfStr = openConfStr;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    boolean asTemplate = true;
    boolean merged = false;
    ConfigThingy conf;
    ConfigThingy fragConf;
    ConfigThingy buttonAnpassung = null;
    try
    {
      conf = new ConfigThingy("OPEN", null, new StringReader(openConfStr));
      fragConf = conf.get("Fragmente");
    } catch (java.lang.Exception e)
    {
      throw new WollMuxFehlerException(L.m("Fehlerhaftes Kommando 'wollmux:Open'"), e);
    }

    try
    {
      asTemplate = conf.get("AS_TEMPLATE", 1).toString().equalsIgnoreCase("true");
    } catch (java.lang.Exception x)
    {
    }

    try
    {
      buttonAnpassung = conf.get("Buttonanpassung", 1);
    } catch (java.lang.Exception x)
    {
    }

    try
    {
      merged = conf.get("FORMGUIS", 1).toString().equalsIgnoreCase("merged");
    } catch (java.lang.Exception x)
    {
    }

    Iterator<ConfigThingy> iter = fragConf.iterator();
    List<TextDocumentController> docs = new ArrayList<TextDocumentController>();
    while (iter.hasNext())
    {
      ConfigThingy fragListConf = iter.next();
      List<String> fragIds = new Vector<String>();
      Iterator<ConfigThingy> fragIter = fragListConf.iterator();
      while (fragIter.hasNext())
      {
        fragIds.add(fragIter.next().toString());
      }
      if (!fragIds.isEmpty())
      {
        TextDocumentController documentController = openTextDocument(fragIds, asTemplate, merged);
        if (documentController.getModel() != null)
          docs.add(documentController);
      }
    }

    if (merged)
    {
      WollMuxEventHandler.handleProcessMultiform(docs, buttonAnpassung);
    }
  }

  /**
   * 
   * @param fragIDs
   * @param asTemplate
   * @param asPartOfMultiform
   * @return
   * @throws WollMuxFehlerException
   */
  private TextDocumentController openTextDocument(List<String> fragIDs, boolean asTemplate, boolean asPartOfMultiform)
      throws WollMuxFehlerException
  {
    // das erste Argument ist das unmittelbar zu landende Textfragment und
    // wird nach urlStr aufgelöst. Alle weiteren Argumente (falls vorhanden)
    // werden nach argsUrlStr aufgelöst.
    String loadUrlStr = "";
    String[] fragUrls = new String[fragIDs.size() - 1];
    String urlStr = "";

    Iterator<String> iter = fragIDs.iterator();
    for (int i = 0; iter.hasNext(); ++i)
    {
      String frag_id = iter.next();

      // Fragment-URL holen und aufbereiten:
      Vector<String> urls = new Vector<String>();

      java.lang.Exception error = new ConfigurationErrorException(
          L.m("Das Textfragment mit der FRAG_ID '%1' ist nicht definiert!", frag_id));
      try
      {
        urls = VisibleTextFragmentList.getURLsByID(WollMuxFiles.getWollmuxConf(), frag_id);
      } catch (InvalidIdentifierException e)
      {
        error = e;
      }
      if (urls.size() == 0)
      {
        throw new WollMuxFehlerException(
            L.m("Die URL zum Textfragment mit der FRAG_ID '%1' kann nicht bestimmt werden:", frag_id), error);
      }

      // Nur die erste funktionierende URL verwenden. Dazu werden alle URL zu
      // dieser FRAG_ID geprüft und in die Variablen loadUrlStr und fragUrls
      // übernommen.
      String errors = "";
      boolean found = false;
      Iterator<String> iterUrls = urls.iterator();
      while (iterUrls.hasNext() && !found)
      {
        urlStr = iterUrls.next();

        // URL erzeugen und prüfen, ob sie aufgelöst werden kann
        URL url;
        try
        {
          url = WollMuxFiles.makeURL(urlStr);
          urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
          url = WollMuxFiles.makeURL(urlStr);
          WollMuxSingleton.checkURL(url);
        } catch (MalformedURLException e)
        {
          Logger.log(e);
          errors += L.m("Die URL '%1' ist ungültig:", urlStr) + "\n" + e.getLocalizedMessage() + "\n\n";
          continue;
        } catch (IOException e)
        {
          Logger.log(e);
          errors += e.getLocalizedMessage() + "\n\n";
          continue;
        }

        found = true;
      }

      if (!found)
      {
        throw new WollMuxFehlerException(
            L.m("Das Textfragment mit der FRAG_ID '%1' kann nicht aufgelöst werden:", frag_id) + "\n\n" + errors);
      }

      // URL in die in loadUrlStr (zum sofort öffnen) und in argsUrlStr (zum
      // später öffnen) aufnehmen
      if (i == 0)
      {
        loadUrlStr = urlStr;
      } else
      {
        fragUrls[i - 1] = urlStr;
      }
    }

    // open document as Template (or as document):
    TextDocumentController documentController = null;
    try
    {
      XComponent doc = UNO.loadComponentFromURL(loadUrlStr, asTemplate, true);

      if (UNO.XTextDocument(doc) != null)
      {
        documentController = DocumentManager.getTextDocumentController(UNO.XTextDocument(doc));
        documentController.getModel().setFragUrls(fragUrls);
        if (asPartOfMultiform)
          documentController.getModel().setPartOfMultiformDocument(asPartOfMultiform);
      }
    } catch (java.lang.Exception x)
    {
      // sollte eigentlich nicht auftreten, da bereits oben geprüft.
      throw new WollMuxFehlerException(L.m("Die Vorlage mit der URL '%1' kann nicht geöffnet werden.", loadUrlStr), x);
    }
    return documentController;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "('" + openConfStr + "')";
  }
}