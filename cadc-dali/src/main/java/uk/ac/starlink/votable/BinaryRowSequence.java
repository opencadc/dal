package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64InputStream;
import uk.ac.starlink.table.RowSequence;

/**
 * RowSequence implementation which reads streamed data in VOTable BINARY
 * format.
 *
 * @author   Mark Taylor
 * @since    31 Jul 2006
 */
public class BinaryRowSequence implements RowSequence {

    private final PushbackInputStream pIn_;
    private final DataInput dataIn_;
    private final int ncol_;
    private final RowReader rowReader_;
    private Object[] row_;

    /**
     * Constructs a new row sequence from a set of decoders and a
     * possibly encoded input stream.
     *
     * @param  n-element array of decoders for decoding n-column data
     * @param  in  input stream containing binary data
     * @param  encoding  encoding string as per <tt>encoding</tt> attribute
     *         of STREAM element ("gzip" or "base64", else assumed none)
     * @param  isBinary2 true for BINARY2 format, false for BINARY
     */
    public BinaryRowSequence( final Decoder[] decoders, InputStream in,
                              String encoding, boolean isBinary2 )
            throws IOException {
        ncol_ = decoders.length;
        if ( "gzip".equals( encoding ) ) {
            in = new GZIPInputStream( in );
        }
        else if ( "base64".equals( encoding ) ) {
            in = new Base64InputStream(in );
        }
        pIn_ = new PushbackInputStream( in );
        dataIn_ = new DataInputStream( pIn_ );
        rowReader_ = isBinary2
            ? new RowReader() {
                  final boolean[] nullFlags = new boolean[ ncol_ ];
                  public void readRow( Object[] row ) throws IOException {
                      FlagIO.readFlags( dataIn_, nullFlags );
                      for ( int icol = 0; icol < ncol_; icol++ ) {
                          Decoder decoder = decoders[ icol ];
                          final Object cell;
                          if ( nullFlags[ icol ] ) {
                              decoder.skipStream( dataIn_ );
                              cell = null;
                          }
                          else {
                              cell = decoder.decodeStream( dataIn_ );
                          }
                          row[ icol ] = cell;
                      }
                  }
              }
            : new RowReader() {
                  public void readRow( Object[] row ) throws IOException {
                      for ( int icol = 0; icol < ncol_; icol++ ) {
                          row[ icol ] = decoders[ icol ]
                                       .decodeStream( dataIn_ );
                      }
                  }
              };
    }

    public boolean next() throws IOException {
        final int b;
        try {
            b = pIn_.read();
        }
        catch ( EOFException e ) {
            return false;
        }
        if ( b < 0 ) {
            return false;
        }
        else {
            pIn_.unread( b );
            Object[] row = new Object[ ncol_ ];
            rowReader_.readRow( row );
            row_ = row;
            return true;
        }
    }

    public Object[] getRow() {
        if ( row_ != null ) {
            return row_;
        }
        else {
            throw new IllegalStateException( "No next() yet" );
        }
    }

    public Object getCell( int icol ) {
        if ( row_ != null ) {
            return row_[ icol ];
        }
        else {
            throw new IllegalStateException( "No next() yet" );
        }
    }

    public void close() throws IOException {
        pIn_.close();
    }

    /**
     * Interface for an object that can read a row from a binary stream.
     */
    private interface RowReader {

        /**
         * Populates a given row array with cell values for the next
         * data row available.
         *
         * @param  row  array of objects to be filled
         */
        void readRow( Object[] row ) throws IOException;
    }
}
