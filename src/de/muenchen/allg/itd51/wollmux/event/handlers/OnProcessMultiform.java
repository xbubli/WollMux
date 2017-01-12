package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.List;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.FormModel;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.InvalidFormDescriptorException;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.MultiDocumentFormModel;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnProcessMultiform extends BasicEvent
{
  private ConfigThingy buttonAnpassung;

  private List<TextDocumentController> documentControllers;

  public OnProcessMultiform(List<TextDocumentController> documentControllers, ConfigThingy buttonAnpassung)
  {
    this.documentControllers = documentControllers;
    this.buttonAnpassung = buttonAnpassung;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    FormModel fm;
    try
    {
      fm = MultiDocumentFormModel.createMultiDocumentFormModel(documentControllers, buttonAnpassung);
    } catch (InvalidFormDescriptorException e)
    {
      throw new WollMuxFehlerException(L.m("Fehler bei der Dokumentbearbeitung."), new WMCommandsFailedException(L.m(
          "Die Vorlage bzw. das Formular enthält keine gültige Formularbeschreibung\n\nBitte kontaktieren Sie Ihre Systemadministration.")));
    }

    // FormModel in allen Dokumenten registrieren:
    for (TextDocumentController documentController : documentControllers)
    {
      DocumentManager.getDocumentManager().setFormModel(documentController.getModel().doc, fm);
    }

    // FormGUI starten:
    fm.startFormGUI();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentControllers + ")";
  }
}