package org.apache.pdfboxandroid.pdfparser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfboxandroid.cos.COSBase;
import org.apache.pdfboxandroid.cos.COSBoolean;
import org.apache.pdfboxandroid.cos.COSDictionary;
import org.apache.pdfboxandroid.cos.COSName;
import org.apache.pdfboxandroid.cos.COSNull;
import org.apache.pdfboxandroid.cos.COSNumber;
import org.apache.pdfboxandroid.cos.COSObject;
import org.apache.pdfboxandroid.cos.COSStream;
import org.apache.pdfboxandroid.io.RandomAccess;
import org.apache.pdfboxandroid.util.ImageParameters;
import org.apache.pdfboxandroid.util.PDFOperator;

/**
 * This will parse a PDF byte stream and extract operands and such.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.32 $
 */
public class PDFStreamParser extends BaseParser {
	private List<Object> streamObjects = new ArrayList<Object>( 100 );
    private RandomAccess file;
    private PDFOperator lastBIToken = null;
    
    /**
     * Constructor that takes a stream to parse.
     *
     * @since Apache PDFBox 1.3.0
     * @param stream The stream to read data from.
     * @param raf The random access file.
     * @param forceParsing flag to skip malformed or otherwise unparseable
     *                     input where possible
     * @throws IOException If there is an error reading from the stream.
     */
    public PDFStreamParser(
            InputStream stream, RandomAccess raf, boolean forceParsing)
            throws IOException 
    {
        super(stream, forceParsing);
        file = raf;
    }

    /**
     * Constructor that takes a stream to parse.
     *
     * @param stream The stream to read data from.
     * @param raf The random access file.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    public PDFStreamParser(InputStream stream, RandomAccess raf)
            throws IOException 
    {
        this(stream, raf, FORCE_PARSING);
    }

//    /**
//     * Constructor.
//     *
//     * @param stream The stream to parse.
//     *
//     * @throws IOException If there is an error initializing the stream.
//     */
//    public PDFStreamParser( PDStream stream ) throws IOException
//    {
//       this( stream.createInputStream(), stream.getStream().getScratchFile() );
//    }

//    /**
//     * Constructor.
//     *
//     * @since Apache PDFBox 1.3.0
//     * @param stream The stream to parse.
//     * @param forceParsing flag to skip malformed or otherwise unparseable
//     *                     input where possible
//     * @throws IOException If there is an error initializing the stream.
//     */
//    public PDFStreamParser(COSStream stream, boolean forceParsing)
//            throws IOException 
//    {
//       this(stream.getUnfilteredStream(), stream.getScratchFile(), forceParsing);
//    }

    /**
     * Constructor.
     *
     * @param stream The stream to parse.
     *
     * @throws IOException If there is an error initializing the stream.
     */
    public PDFStreamParser( COSStream stream ) throws IOException
    {
       this( stream.getUnfilteredStream(), stream.getScratchFile() );
    }
    
    /**
     * This will parse the tokens in the stream.  This will close the
     * stream when it is finished parsing.
     *
     * @throws IOException If there is an error while parsing the stream.
     */
    public void parse() throws IOException
    {
        try
        {
            Object token = null;
            while( (token = parseNextToken()) != null )
            {
                streamObjects.add( token );
                //logger().fine( "parsed=" + token );
            }
        }
        finally
        {
            pdfSource.close();
        }
    }
    
    /**
     * This will parse the next token in the stream.
     *
     * @return The next token in the stream or null if there are no more tokens in the stream.
     *
     * @throws IOException If an io error occurs while parsing the stream.
     */
    private Object parseNextToken() throws IOException
    {
        Object retval = null;

        skipSpaces();
        int nextByte = pdfSource.peek();
        if( ((byte)nextByte) == -1 )
        {
            return null;
        }
        char c = (char)nextByte;
        switch(c)
        {
            case '<':
            {
                int leftBracket = pdfSource.read();//pull off first left bracket
                c = (char)pdfSource.peek(); //check for second left bracket
                pdfSource.unread( leftBracket ); //put back first bracket
                if(c == '<')
                {

                    COSDictionary pod = parseCOSDictionary();
                    skipSpaces();
                    if((char)pdfSource.peek() == 's')
                    {
                        retval = parseCOSStream( pod, file );
                    }
                    else
                    {
                        retval = pod;
                    }
                }
                else
                {
                    retval = parseCOSString(false);
                }
                break;
            }
            case '[': // array
            {
                retval = parseCOSArray();
                break;
            }
            case '(': // string
                retval = parseCOSString(false);
                break;
            case '/':   // name
                retval = parseCOSName();
                break;
            case 'n':   // null
            {
                String nullString = readString();
                if( nullString.equals( "null") )
                {
                    retval = COSNull.NULL;
                }
                else
                {
                    retval = PDFOperator.getOperator( nullString );
                }
                break;
            }
            case 't':
            case 'f':
            {
                String next = readString();
                if( next.equals( "true" ) )
                {
                    retval = COSBoolean.TRUE;
                    break;
                }
                else if( next.equals( "false" ) )
                {
                    retval = COSBoolean.FALSE;
                }
                else
                {
                    retval = PDFOperator.getOperator( next );
                }
                break;
            }
            case 'R':
            {
                String line = readString();
                if( line.equals( "R" ) )
                {
                    retval = new COSObject( null );
                }
                else
                {
                    retval = PDFOperator.getOperator( line );
                }
                break;
            }
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '+':
            case '.':
            {
                /* We will be filling buf with the rest of the number.  Only
                 * allow 1 "." and "-" and "+" at start of number. */
                StringBuffer buf = new StringBuffer();
                buf.append( c );
                pdfSource.read();

                boolean dotNotRead = (c != '.');
                while( Character.isDigit(( c = (char)pdfSource.peek()) ) || (dotNotRead && (c == '.')) )
                {
                    buf.append( c );
                    pdfSource.read();

                    if (dotNotRead && (c == '.'))
                    {
                        dotNotRead = false;
                    }
                }
                retval = COSNumber.get( buf.toString() );
                break;
            }
            case 'B':
            {
                String next = readString();
                retval = PDFOperator.getOperator( next );

                if( next.equals( "BI" ) )
                {
                    lastBIToken = (PDFOperator)retval;
                    COSDictionary imageParams = new COSDictionary();
                    lastBIToken.setImageParameters( new ImageParameters( imageParams ) );
                    Object nextToken = null;
                    while( (nextToken = parseNextToken()) instanceof COSName )
                    {
                        Object value = parseNextToken();
                        imageParams.setItem( (COSName)nextToken, (COSBase)value );
                    }
                    //final token will be the image data, maybe??
                    PDFOperator imageData = (PDFOperator)nextToken;
                    lastBIToken.setImageData( imageData.getImageData() );
                }
                break;
            }
            case 'I':
            {
                //ImageParameters imageParams = lastBIToken.getImageParameters();

                //int expectedBytes = (int)Math.ceil(imageParams.getHeight() * imageParams.getWidth() *
                //                    (imageParams.getBitsPerComponent()/8) );
                //Special case for ID operator
                String id = "" + (char)pdfSource.read() + (char)pdfSource.read();
                if( !id.equals( "ID" ) )
                {
                    throw new IOException( "Error: Expected operator 'ID' actual='" + id + "'" );
                }
                ByteArrayOutputStream imageData = new ByteArrayOutputStream();
                //boolean foundEnd = false;
                if( this.isWhitespace() )
                {
                    //pull off the whitespace character
                    pdfSource.read();
                }
                int twoBytesAgo = 0;
                int lastByte = pdfSource.read();
                int currentByte = pdfSource.read();
                int count = 0;
                //PDF spec is kinda unclear about this.  Should a whitespace
                //always appear before EI? Not sure, I found a PDF
                //(UnderstandingWebSphereClassLoaders.pdf) which has EI as part
                //of the image data and will stop parsing prematurely if there is
                //not a check for <whitespace>EI<whitespace>.
                while( !(isWhitespace( twoBytesAgo ) &&
                         lastByte == 'E' &&
                         currentByte == 'I' &&
                         isWhitespace() //&&
                         //amyuni2_05d__pdf1_3_acro4x.pdf has image data that
                         //is compressed, so expectedBytes is useless here.
                         //count >= expectedBytes
                         ) &&
                       !pdfSource.isEOF() )
                {
                    imageData.write( lastByte );
                    twoBytesAgo = lastByte;
                    lastByte = currentByte;
                    currentByte = pdfSource.read();
                    count++;
                }
                pdfSource.unread( 'I' ); //unread the EI operator
                pdfSource.unread( 'E' );
                retval = PDFOperator.getOperator( "ID" );
                ((PDFOperator)retval).setImageData( imageData.toByteArray() );
                break;
            }
            case ']':
            {
                // some ']' around without its previous '['
                // this means a PDF is somewhat corrupt but we will continue to parse.
                pdfSource.read();
                retval = COSNull.NULL;  // must be a better solution than null...
                break;
            }
            default:
            {
                //we must be an operator
                String operator = readOperator();
                if( operator.trim().length() == 0 )
                {
                    //we have a corrupt stream, stop reading here
                    retval = null;
                }
                else
                {
                    retval = PDFOperator.getOperator( operator );
                }
            }

        }

        return retval;
    }
    
    /**
     * This will read an operator from the stream.
     *
     * @return The operator that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected String readOperator() throws IOException
    {
        skipSpaces();

        //average string size is around 2 and the normal string buffer size is
        //about 16 so lets save some space.
        StringBuffer buffer = new StringBuffer(4);
        int nextChar = pdfSource.peek();
        while(
            nextChar != -1 && // EOF
            !isWhitespace(nextChar) &&
            !isClosing(nextChar) &&
            nextChar != '[' &&
            nextChar != '<' &&
            nextChar != '(' &&
            nextChar != '/' &&
            (nextChar < '0' ||
             nextChar > '9' ) )
        {
            char currentChar = (char)pdfSource.read();
            nextChar = pdfSource.peek();
            buffer.append( currentChar );
            // Type3 Glyph description has operators with a number in the name
            if (currentChar == 'd' && (nextChar == '0' || nextChar == '1') ) 
            {
                buffer.append( (char)pdfSource.read() );
                nextChar = pdfSource.peek();
            }
        }
        return buffer.toString();
    }
    
    /**
     * This will get the tokens that were parsed from the stream.
     *
     * @return All of the tokens in the stream.
     */
    public List<Object> getTokens()
    {
        return streamObjects;
    }
}
