package com.bng.ticketmanager.archiving;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.bng.ticketmanager.util.LogValues;
import com.bng.ticketmanager.util.Logger;
import com.bng.ticketmanager.util.coreException;

public class FileCompressing {
	
	public File archiveTARFiles(File baseDir, String dirToArchive, String archiveNameWithOutExtension) {

		File tarFile = null;
		try {
			
			File cdrDir = new File(baseDir, dirToArchive);
			if(cdrDir.exists()){
				
				File[] list = (new File(baseDir, dirToArchive)).listFiles();
				tarFile = new File(baseDir, archiveNameWithOutExtension + ".tar");
				byte[] buf = new byte[1024];
				int len;			
				// --------- TAR
				{
					TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarFile));
					tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
					for (File file : list) {
						TarArchiveEntry tarEntry = new TarArchiveEntry(file.getName());
						tarEntry.setSize(file.length());

						FileInputStream fin = new FileInputStream(file);
						BufferedInputStream in = new BufferedInputStream(fin);
						tos.putArchiveEntry(tarEntry);

						while ((len = in.read(buf)) != -1) {
							tos.write(buf, 0, len);
						}

						in.close();
						tos.closeArchiveEntry();
						// file.delete();
					}//end for loop
					tos.close();
				}
				Logger.sysLog(LogValues.info, FileCompressing.class.getName(), "FileArch :: File .tar conversion complete");
			}else{
				
				Logger.sysLog(LogValues.error, FileCompressing.class.getName(), "FileArch :: Hourly Cdr Directory not found");
			}
		
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, FileCompressing.class.getName(), "FileArch :: File .tar conversion failed\n"+coreException.GetStack(e));
		}
		return tarFile;
	}

	public File compressGZIPFiles(File baseDir, File fileToCompressWithExtension, String compressNameWithOutExtension) {
		File tgzFile = null;
		
		try {
			tgzFile = new File(baseDir, compressNameWithOutExtension + ".tar.gz");

			byte[] buf = new byte[1024];
			int len;	
			// --------- GZIP
			{
				GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(tgzFile));
				FileInputStream in = new FileInputStream(fileToCompressWithExtension);

				while ((len = in.read(buf)) > 0) {
					gz.write(buf, 0, len);
				}
				in.close();
				gz.finish();
				gz.close();
				// Delete the TAR file
				//tarFile.delete();
			}
			Logger.sysLog(LogValues.info, FileCompressing.class.getName(), "FileArch :: File .tar.gz conversion complete");
			
		} catch (IOException e) {
			Logger.sysLog(LogValues.error, FileCompressing.class.getName(), "FileArch :: File .tar.gz conversion failed");
			System.exit(0);
		}
		return tgzFile;
	}

}
