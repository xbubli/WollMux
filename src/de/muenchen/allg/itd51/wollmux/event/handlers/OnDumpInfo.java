package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class OnDumpInfo extends BasicEvent
{

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    final String title = L.m("Fehlerinfos erstellen");

    String name = WollMuxFiles.dumpInfo();

    if (name != null)
      ModalDialogs.showInfoModal(title,
          L.m("Die Fehlerinformationen des WollMux wurden erfolgreich in die Datei '%1' geschrieben.", name));
    else
      ModalDialogs.showInfoModal(title, L.m(
          "Die Fehlerinformationen des WollMux konnten nicht geschrieben werden\n\nDetails siehe Datei wollmux.log!"));
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}