/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Apache Commons (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.webapp

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import org.apache.commons.io.FilenameUtils
import java.text.DateFormat

class ZiphandlerTagLib {
	static namespace="Ziphandler"
	def grailsApplication
	 
	class ZipVisitor extends SimpleFileVisitor<Path> {
		boolean jsOutput=true;
		StringBuilder builder;
		Path zipfile;
		
		public ZipVisitor(boolean js, StringBuilder b, Path zip) {
			jsOutput=js
			builder=b;
			zipfile=zip;
		}
		
		
		
		@Override
		public FileVisitResult visitFile(Path visiting, BasicFileAttributes attrs) throws IOException {
			try
			{
				if (jsOutput) {
					visitFileJS(visiting, attrs);
				}
				return FileVisitResult.CONTINUE;
			}
			catch(Exception e) {
				e.printStackTrace()
			}
		}
	
	}
	
	private void visitFileJS(StringBuilder builder, Path zipfile, Path visiting, BasicFileAttributes attrs) {
			builder.append("fileData[\"")
			builder.append(zipfile.getFileName().toString())
			builder.append(visiting.toString())
			builder.append("\"]=new Object();")
			addFileAttributesJS(builder, zipfile.getFileName().toString()+visiting.toString(), "isInternal", "true", false); 
			addFileAttributesJS(builder, zipfile.getFileName().toString()+visiting.toString(), "Name", FilenameUtils.getName(visiting.toString()), true); 
			addFileAttributesJS(builder, zipfile.getFileName().toString()+visiting.toString(), "Size", "window.readablizeBytes(${attrs.size()});", false); 
			if (attrs.lastModifiedTime()) {
				addFileAttributesJS(builder, zipfile.getFileName().toString()+visiting.toString(), "Last_Modified", "${new Date(attrs.lastModifiedTime().toMillis())}".toString(), true); 
			}
	}
	
	private void addFileAttributesJS(StringBuilder builder, String filename,String prop, String value, boolean quotes) {
		builder.append("fileData[\"").append(filename).
				append("\"].").append(prop).append("=");
		if (quotes) {
			builder.append("'")
		}
		builder.append(value);
		if (quotes) {
			builder.append("'")
		}
		builder.append(";")
	}
	
	private void handleZip(StringBuilder builder, boolean JS, String filePath, def loadedZips, def zipSupported) {
		 try {
  				Path zipfile = Paths.get(filePath);
		 	 	FileSystem fs = null;
	 			if (!loadedZips.containsKey(zipfile.toString())) {
	 				fs=FileSystems.newFileSystem(zipfile, null);
	 				loadedZips.put(zipfile.toString(), fs)
	 			}
	 			else {
	 				fs=loadedZips.get(zipfile.toString())
	 			}
  	 			final Path root = fs.getPath("/");
  	 			Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path visiting, BasicFileAttributes attrs) throws IOException {
                    	if (JS) {
                    		visitFileJS(builder, zipfile, visiting, attrs)
                    		return FileVisitResult.CONTINUE;
                    	}
                    	else {
                    		builder.append('''<li><a><span class="pointerhere">''')
                    		builder.append(zipfile.getFileName().toString())
                    		builder.append(visiting.toString())
                    		builder.append("</span></a></li>");
                    		return FileVisitResult.CONTINUE;
                    	}
                    }
                });
  	 			zipSupported[filePath]=true
  	 	 }
  	 	 catch(Exception e) {
  	 	 	 e.printStackTrace()
  	 	 	 zipSupported[filePath]=false;
  	 	 }
	}
	
	private void processFilesJS(def repFiles, StringBuilder builder, def loadedZips, def zipSupported) {
		try
        {
            repFiles.each {
				File file=new File(it.path)
				BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
				builder.append("fileData[\"").append(file.name).append("\"]=new Object();");
				addFileAttributesJS(builder, file.name, "Name", FilenameUtils.getName(it.path), true); 
				addFileAttributesJS(builder, file.name, "Size", "window.readablizeBytes(${attr.size()})", false); 
				addFileAttributesJS(builder, file.name, "showPreview", "${attr.size() > grailsApplication.config.jummp.web.file.preview};", false); 
				if (!it.mainFile) {
					addFileAttributesJS(builder, file.name, "Description",
                            it.description.encodeAsJavaScript(), true)
				}
				addFileAttributesJS(builder, file.name, "mime", it.mimeType, true); 
				addFileAttributesJS(builder,file.name,"isInternal","false", false) 
				
				if (it.mimeType.contains('zip')) {
						 handleZip(builder, true, it.path, loadedZips, zipSupported)
				}
     		}
     		builder.append("</script>")
        }
        catch(Exception e) {
        	e.printStackTrace()
        }
	
	}
	
	def outputFileInfoAsJS = { attrs ->
        StringBuilder builder=new StringBuilder('''<script>function readablizeBytes(bytes) {
			var s = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'];
			var e = Math.floor(Math.log(bytes) / Math.log(1024));
			return (bytes / Math.pow(1024, e)).toFixed(2) + " " + s[e]; 
		}var fileData=new Array();''')
		if (!attrs.repFiles || attrs.loadedZips==null || attrs.zipSupported==null) {
        	return
        }
        def loadedZips=attrs.loadedZips;
        def zipSupported=attrs.zipSupported;
        processFilesJS(attrs.repFiles, builder, loadedZips, zipSupported)	
		out<<builder.toString()
	}
	
	
	def outputFileInfoAsHtml = { attrs ->
		StringBuilder builder=new StringBuilder()
		try {
			if (!attrs.repFiles || attrs.loadedZips==null || attrs.zipSupported==null || attrs.mainFile==null) {
        		return
        	}
        	def loadedZips=attrs.loadedZips;
        	def zipSupported=attrs.zipSupported;
        	attrs.repFiles.each {
        		if (attrs.mainFile==it.mainFile) {
        			File f=new File(it.path);
        			builder.append('''<li rel="file"><a title="''')
        			builder.append(f.name)
        			builder.append('''"><span class="pointerhere">''')
        			String filename=f.name
        			if (filename.length()>20) {
        				filename=filename.substring(0,20)+"..."
        			}
        			builder.append(filename).append("</span></a>")
        			if (it.mimeType.contains('zip')) {
        				builder.append("<ul>")
        				handleZip(builder, false, it.path, loadedZips, zipSupported)
        				builder.append("</ul>")
	 			    }
	 			    builder.append("</li>")
        		}
        	}
		}
		catch(Exception e) {
			e.printStackTrace()
		}
		out<<builder.toString();
	}
	
	
}
