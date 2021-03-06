package org.apache.pdfboxandroid.pdmodel.interactive.form;

import org.apache.pdfboxandroid.cos.COSBase;
import org.apache.pdfboxandroid.cos.COSDictionary;
import org.apache.pdfboxandroid.cos.COSName;

/**
 * A class for handling the PDF field as a checkbox.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @author sug
 * @version $Revision: 1.11 $
 */
public class PDCheckbox extends PDChoiceButton {
	private static final COSName KEY = COSName.getPDFName("AS");
    private static final COSName OFF_VALUE = COSName.getPDFName("Off");

    private COSName value;
	
	/**
     * @see PDField#PDField(PDAcroForm,COSDictionary)
     *
     * @param theAcroForm The acroForm for this field.
     * @param field The checkbox field dictionary
     */
    public PDCheckbox( PDAcroForm theAcroForm, COSDictionary field)
    {
        super( theAcroForm, field);
        COSDictionary ap = (COSDictionary) field.getDictionaryObject(COSName.getPDFName("AP"));
        if( ap != null )
        {
            COSBase n = ap.getDictionaryObject(COSName.getPDFName("N"));

            if( n instanceof COSDictionary )
            {
                for( COSName name : ((COSDictionary)n).keySet() )
                {
                    if( !name.equals( OFF_VALUE ))
                    {
                        value = name;
                    }
                }

            }
        }
        else
        {
            value = (COSName)getDictionary().getDictionaryObject( "V" );
        }
    }
    
    public void setValue(String newValue)
    {
        getDictionary().setName( "V", newValue );
        if( newValue == null )
        {
            getDictionary().setItem( KEY, OFF_VALUE );
        }
        else
        {
            getDictionary().setName( KEY, newValue );
        }
    }
    
    /**
     * This will get the value of the radio button.
     *
     * @return The value of the radio button.
     */
    public String getOnValue()
    {
        String retval = null;
        COSDictionary ap = (COSDictionary) getDictionary().getDictionaryObject(COSName.getPDFName("AP"));
        COSBase n = ap.getDictionaryObject(COSName.getPDFName("N"));

        //N can be a COSDictionary or a COSStream
        if( n instanceof COSDictionary )
        {
            for( COSName key :((COSDictionary)n).keySet() )
            {
                if( !key.equals( OFF_VALUE) )
                {
                    retval = key.getName();
                }
            }
        }
        return retval;
    }
    
    /**
     * Checks the radiobutton.
     */
    public void check()
    {
        getDictionary().setItem(KEY, value);
    }

    /**
     * Unchecks the radiobutton.
     */
    public void unCheck()
    {
        getDictionary().setItem(KEY, OFF_VALUE);
    }
}
