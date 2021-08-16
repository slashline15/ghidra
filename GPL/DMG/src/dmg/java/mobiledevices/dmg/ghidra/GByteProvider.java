/* ###
 * IP: Public Domain
 */
package mobiledevices.dmg.ghidra;


import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An implementation of ByteProvider where the underlying
 * bytes are supplied by a random access file.
 */
public class GByteProvider implements Closeable {
    private File file;
    private GRandomAccessFile randomAccessFile;

    /**
     * Constructs a byte provider using the specified file
     * @param file the file to open for random access
     * @throws FileNotFoundException if the file does not exist
     */
    public GByteProvider(File file) throws IOException {
        this.file = file;
        this.randomAccessFile = new GRandomAccessFile(file, "r");
    }

    /**
     * Constructs a byte provider using the specified file and permissions string
     * @param file the file to open for random access
     * @param string indicating permissions used for open
     * @throws FileNotFoundException if the file does not exist
     */
    public GByteProvider(File file, String permissions) throws IOException {
        this.file = file;
        this.randomAccessFile = new GRandomAccessFile(file, permissions);
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#getFile()
     */
    public File getFile() {
		return this.file;
	}

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#getName()
     */
    public String getName() {
        return this.file.getName();
    }

    public String getAbsolutePath() {
        return this.file.getAbsolutePath();
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#getInputStream(long)
     */
    public InputStream getInputStream(long index) throws IOException {
        FileInputStream is = new FileInputStream(this.file);
        is.skip(index);
        return is;
    }

    /**
     * Closes the underlying random-access file.
     * @throws IOException if an I/O error occurs
     */
	@Override
    public void close() throws IOException {
        this.randomAccessFile.close();
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#length()
     */
    public long length() throws IOException {
        return this.randomAccessFile.length();
    }

    public boolean isValidIndex(long index) {
    	try {
    		return index >= 0 && index < this.randomAccessFile.length();
    	}
    	catch (IOException e) {
    	}
    	return false;
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#readByte(long)
     */
    public byte readByte(long index) throws IOException {
        this.randomAccessFile.seek(index);
        return this.randomAccessFile.readByte();
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#readBytes(long, long)
     */
    public byte [] readBytes(long index, long length) throws IOException {
        this.randomAccessFile.seek(index);
        byte [] b = new byte[(int)length];
        int nRead = this.randomAccessFile.read(b);
        if (nRead != length) {
            throw new IOException("Unable to read "+length+" bytes");
        }
        return b;
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#writeByte(long, byte)
     */
    public void writeByte(long index, byte value) throws IOException {
        this.randomAccessFile.seek(index);
        this.randomAccessFile.write(value);
    }

    /**
     * @see GByteProvider.app.util.bin.ByteProvider#writeBytes(long, byte[])
     */
    public void writeBytes(long index, byte [] values) throws IOException {
        this.randomAccessFile.seek(index);
        this.randomAccessFile.write(values);
    }
}
