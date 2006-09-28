/*
* Dateiname: ParamValue.java
* Projekt  : WollMux
* Funktion : Repr�sentiert einen vom Benutzer konfigurierten Parameter f�r eine Funktion.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 25.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/

package de.muenchen.allg.itd51.wollmux.former;

/**
 * Repr�sentiert einen vom Benutzer konfigurierten Parameter f�r eine Funktion.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ParamValue
{
  /**
   * Der Wert wurde vom Benutzer nicht spezifiziert.
   */
  public static final int UNSPECIFIED = 0;
  /**
   * Als Wert soll der Wert des Feldes mit ID {@link #idStr} verwendet werden.
   */
  public static final int FIELD = 1;
  /**
   * Als Wert soll {@link #idStr} als Literal verwendet werden.
   */
  public static final int LITERAL = 2;
  
  /**
   * Je nach {@link #type} enth�lt dies ein Literal oder eine Feld-ID.
   */
  private String idStr;
  
  /**
   * Der Typ dieses Parameter-Wertes.
   */
  private int type;
  
  private ParamValue(int type, String str)
  {
    this.type = type;
    this.idStr = str;
  }
  
  /**
   * Liefert einen unspezifizierten ParamValue.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue unspecified()
  {
    return new ParamValue(UNSPECIFIED,"");
  }
  
  /**
   * Liefert einen ParamValue der eine Referenz auf das Feld mit ID id darstellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue field(String id)
  {
    return new ParamValue(FIELD,id);
  }
  
  /**
   * Liefert einen ParamValue, der das String-Literal str darstellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue literal(String str)
  {
    return new ParamValue(LITERAL,str);
  }
}