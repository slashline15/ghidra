/* ###
 * IP: Public Domain
 */
package mobiledevices.dmg.btree;

import java.io.IOException;

import mobiledevices.dmg.decmpfs.DecmpfsHeader;
import mobiledevices.dmg.ghidra.GBinaryReader;
import mobiledevices.dmg.xattr.XattrConstants;

public class BTreeNodeRecord /*implements StructConverter*/ {

	private int    unknown0;
	private int    fileID;
	private int    unknown2;
	private String type;
	private int    unknown3;
	private int    unknown4;
	private int    unknown5;
	private int    recordLength;

	private short _typeLength;
	private BTreeNodeDescriptor _descriptor;
	private DecmpfsHeader _decmpfsHeader;
	private long _offset;

	BTreeNodeRecord( GBinaryReader reader, BTreeNodeDescriptor descriptor ) throws IOException {
		this._offset       =  reader.getPointerIndex();

		this.unknown0      =  reader.readNextInt();
		this.fileID        =  reader.readNextInt();
		this.unknown2      =  reader.readNextInt();

		this._typeLength   =  reader.readNextShort();

		this.type          =  readType( reader );
		this.unknown3      =  reader.readNextInt();

		switch ( descriptor.getKind() ) {
			case BTreeNodeKinds.kBTHeaderNode: {
				break;
			}
			case BTreeNodeKinds.kBTIndexNode: {
				break;
			}
			case BTreeNodeKinds.kBTLeafNode: {
				this.unknown4      =  reader.readNextInt();
				this.unknown5      =  reader.readNextInt();
				this.recordLength  =  reader.readNextInt();
				break;
			}
			case BTreeNodeKinds.kBTMapNode: {
				break;
			}
		}

		this._descriptor = descriptor;

		if ( descriptor.getKind() == BTreeNodeKinds.kBTLeafNode ) {
			if ( getType().equals( XattrConstants.DECMPFS_XATTR_NAME ) ) {
				this._decmpfsHeader = new DecmpfsHeader( reader, getRecordLength() );
			}
			else if ( getType().equals( XattrConstants.KAUTH_FILESEC_XATTR_NAME ) ) {
				//TODO
			}
		}
		else if ( descriptor.getKind() == BTreeNodeKinds.kBTIndexNode ) {
			if ( getType().equals( XattrConstants.DECMPFS_XATTR_NAME ) ) {
				//TODO
			}
		}
	}

	private String readType( GBinaryReader reader ) throws IOException {
		StringBuffer buffer = new StringBuffer();
		for ( int i = 0 ; i < this._typeLength ; ++i ) {
			reader.readNextByte();//skip it...
			buffer.append( (char) reader.readNextByte() );
		}
		return buffer.toString();
	}

	public String getType() {
		return this.type;
	}
	public int getRecordLength() {
		return this.recordLength;
	}
	public BTreeNodeDescriptor getDescriptor() {
		return this._descriptor;
	}
	public int getUnknown0() {
		return this.unknown0;
	}
	public int getUnknown2() {
		return this.unknown2;
	}
	public int getUnknown3() {
		return this.unknown3;
	}
	public int getUnknown4() {
		return this.unknown4;
	}
	public int getUnknown5() {
		return this.unknown5;
	}
	public int getFileID() {
		return this.fileID;
	}
	public DecmpfsHeader getDecmpfsHeader() {
		return this._decmpfsHeader;
	}
	public long getRecordOffset() {
		return this._offset;
	}

//	@Override
//	public DataType toDataType() throws DuplicateNameException, IOException {
//		String name = StructConverterUtil.parseName( BTreeNodeRecord.class );
//		Structure struct = new StructureDataType( name, 0 );
//		struct.add( DWORD, "unknown0", null );
//		struct.add( DWORD, "fileID", null );
//		struct.add( DWORD, "unknown2", null );
//		struct.add(  WORD, "typeLength", null );
//		struct.add( UNICODE, _typeLength * 2, "type", null );
//		struct.add( DWORD, "unknown3", null );
//		if ( _descriptor.getKind() == BTreeNodeKinds.kBTLeafNode ) {
//			struct.add( DWORD, "unknown4", null );
//			struct.add( DWORD, "unknown5", null );
//			struct.add( DWORD, "recordLength", null );
//		}
//		try {
//			struct.setName( name + '_' + struct.getLength() );
//		}
//		catch ( Exception e ) {
//		}
//		return struct;
//	}
}
