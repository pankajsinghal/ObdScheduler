package com.bng.ticketmanager.archiving;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class FileUncompressing {
	
	public File unCompressGZIPFiles(File baseDir, File fileToUnCompressWithExtension, String unCompressNameWithOutExtension) {
		File tgzFile = null;
        
		try {

			FileInputStream fin = new FileInputStream(fileToUnCompressWithExtension);
			BufferedInputStream in = new BufferedInputStream(fin);
			tgzFile = new File(baseDir, unCompressNameWithOutExtension+".tar");
			FileOutputStream out = new FileOutputStream(tgzFile);
			GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
			final byte[] buffer = new byte[1024];
			int n = 0;
			while (-1 != (n = gzIn.read(buffer))) {
				out.write(buffer, 0, n);
			}
			out.close();
			gzIn.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return tgzFile;
	}

}
