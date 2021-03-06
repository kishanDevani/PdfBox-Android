package org.apache.pdfboxandroid.cos;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import org.apache.pdfboxandroid.exceptions.COSVisitorException;

/**
 * This class represents a floating point number in a PDF document.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * 
 */
public class COSFloat extends COSNumber {
	private BigDecimal value;
	private String valueAsString;
	
	/**
     * Constructor.
     *
     * @param aFloat The primitive float object that this object wraps.
     */
    public COSFloat( float aFloat )
    {
        setValue(aFloat);
    }
	
	/**
     * Constructor.
     *
     * @param aFloat The primitive float object that this object wraps.
     *
     * @throws IOException If aFloat is not a float.
     */
    public COSFloat( String aFloat ) throws IOException
    {
        try
        {
            valueAsString = aFloat; 
            value = new BigDecimal( valueAsString );
        }
        catch( NumberFormatException e )
        {
            throw new IOException( "Error expected floating point number actual='" +aFloat + "'" );
        }
    }
    
    /**
     * Set the value of the float object.
     *
     * @param floatValue The new float value.
     */
    public void setValue( float floatValue )
    {
        // use a BigDecimal as intermediate state to avoid 
        // a floating point string representation of the float value
        value = new BigDecimal(String.valueOf(floatValue));
        valueAsString = removeNullDigits(value.toPlainString());
    }
    
    private String removeNullDigits(String value)
    {
        // remove fraction digit "0" only
        if (value.indexOf(".") > -1 && !value.endsWith(".0"))
        {
            while (value.endsWith("0") && !value.endsWith(".0"))
            {
                value = value.substring(0,value.length()-1);
            }
        }
        return value;
    }
    
    /**
     * The value of the float object that this one wraps.
     *
     * @return The value of this object.
     */
    public float floatValue()
    {
        return value.floatValue();
    }

	/**
     * This will get the long value of this object.
     *
     * @return The long value of this object,
     */
    public long longValue()
    {
        return value.longValue();
    }
    
    /**
     * This will get the integer value of this object.
     *
     * @return The int value of this object,
     */
    public int intValue()
    {
        return value.intValue();
    }
    
    /**
     * This will output this string as a PDF object.
     *
     * @param output The stream to write to.
     * @throws IOException If there is an error writing to the stream.
     */
    public void writePDF( OutputStream output ) throws IOException
    {
        output.write(valueAsString.getBytes("ISO-8859-1"));
    }

    /**
     * visitor pattern double dispatch method.
     *
     * @param visitor The object to notify when visiting this object.
     * @return any object, depending on the visitor implementation, or null
     * @throws COSVisitorException If an error occurs while visiting this object.
     */
    public Object accept(ICOSVisitor visitor) throws COSVisitorException
    {
        return visitor.visitFromFloat(this);
    }
}
