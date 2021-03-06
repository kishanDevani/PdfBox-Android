package org.apache.pdfboxandroid.pdmodel.font;

import org.apache.pdfboxandroid.cos.COSDictionary;

/**
 * This is implementation of the Multiple Master Type1 Font.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.4 $
 */
public class PDMMType1Font extends PDSimpleFont {

	/**
     * Constructor.
     *
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    public PDMMType1Font( COSDictionary fontDictionary )
    {
        super( fontDictionary );
    }
}
