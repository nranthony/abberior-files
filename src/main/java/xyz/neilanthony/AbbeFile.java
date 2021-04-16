/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.neilanthony;

import io.scif.gui.BufferedImageReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import java.nio.file.Path;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;

/**
 *
 * @author nelly
 */
public class AbbeFile {
    
    private String omexml;
    private int index = -1;
    private Path fPath = null;
    
    AbbeFile() {
        // constructor
    }

    public void setIndex () {

    }

    public void setPath (Path filePath) {
        fPath = filePath;
    }

    public void pullOMEXML () throws DependencyException, ServiceException, FormatException, IOException {

        OMEXMLService omexmlService = null;
        ServiceFactory factory = new ServiceFactory();
        omexmlService = factory.getInstance(OMEXMLService.class);

        MetadataStore omexmlStore = null;
        omexmlStore = omexmlService.createOMEXMLMetadata();
        
        IFormatReader reader = null;
        reader = new ImageReader();
        reader.setId(this.fPath.toString());
                
        MetadataStore readerMetaStore = reader.getMetadataStore();
        MetadataRetrieve retrieve = omexmlService.asRetrieve(readerMetaStore);
        this.omexml = omexmlService.getOMEXML(retrieve);
        
    }
    
    public void pullOMEXMLRaw () throws FileNotFoundException, IOException {
        File file = new File(this.fPath.toString());
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        
        long readLong = 0;
        byte[] readBytes = new byte[5];
        int n = 10;
        Boolean looking = true;
        while (looking) {            
            n += 1;
            raf.seek(file.length() - (4 + 4*n));
            readLong = raf.readLong();
            
            if (decodeUTF8(longToBytes(readLong)).contains("<?xml")) {
                looking = false;
            }
        }
//        String tmpStr;
//        tmpStr = "";
//        for (int i = 1; i < 5; i++) {
//            
//            raf.seek(file.length() - (4 + 4*i));
//            readLong = raf.readLong();
//            
//            tmpStr += decodeUTF8(longToBytes(readLong)) + System.lineSeparator();
//            
//        }
        raf.seek(file.length() - (8 + 4*n));
        byte[] rawXMLBytes = new byte[8 + 4*n];
        raf.read(rawXMLBytes);
        
        raf.close();
        this.omexml = decodeUTF8(rawXMLBytes);
    }   
    
    public String getOMEXML () {
        return this.omexml;
    }
    
    public void pullOMEXMLRawFast() throws FileNotFoundException, IOException {
        
        // get memory mapped buffer of last ~2MB of obf file
        // TODO - check location of xml within .msr files
        final FileChannel channel = new FileInputStream(this.fPath.toString()).getChannel();
        long mapSize;
        long fileSize;
        fileSize = channel.size();
        mapSize = (long) Math.min(fileSize, Math.pow(2, 21)); // 2,097,152   ~2MB
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, fileSize - mapSize, mapSize);
        
        // create integer from xml 4 chars at start
        byte[] xmlStartBytes = "<?xm".getBytes(Charset.forName("UTF-8"));
        ByteBuffer xmlStartByteBuf = ByteBuffer.wrap(xmlStartBytes);
        int xmlStartInt = xmlStartByteBuf.getInt();

        //  scan through int sized chunks of buffer and compare to xmlStartByteBuf
        int offset = -1; int compInt = 0; int xorInt = 1;
        for (int i = 0; i < mapSize - 8; i++) {
            compInt = buffer.getInt(i);
            xorInt = compInt ^ xmlStartInt;
            if (xorInt == 0) { // XOR bitwise operator equals zero if all bits the same
                offset = i;
                break;
            }
        }
        if (offset == -1) {
            this.omexml = "";
            channel.close();
            return;
        }
        
        int readLength = (int)(mapSize - offset);
        byte[] xmlBytes = new byte[readLength];
        buffer.position(offset-1);
        buffer.get(xmlBytes);
        
        this.omexml = decodeUTF8(xmlBytes);
        channel.close();
    }
    
    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
    
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    String decodeUTF8(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }
}

