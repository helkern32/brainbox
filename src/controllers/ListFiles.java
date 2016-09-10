package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import utility.Sender;
import entity.Link;

@Controller
public class ListFiles {

	@Autowired
	ServletContext ctx;
	
	@Autowired
	private Environment env;
	
	private static Logger log = Logger.getLogger("file");

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView listRootFiles(HttpServletRequest req) {
		ModelAndView model = new ModelAndView();
		ArrayList<Link> list = new ArrayList<>();
		String roothPath = ctx.getAttribute("FILES_DIR").toString();
		log.info("ListFiles.listRootFiles: current rooth path --> " + roothPath );
		getRootFolders(list, roothPath);
		model.addObject("list", list);
		model.addObject("showUploadLink", false);
		model.addObject("directory", "Directories");
		model.addObject("pass", env.getProperty("delete.pass"));
		log.info("ListFiles.listRootFiles: go to main page" );
		model.setViewName("pages/index");
		return model;
	}

	private void getRootFolders(ArrayList<Link> list, String directory) {
		log.info("ListFiles.getRootFolders: list all folders" );
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			String fileName = listOfFiles[i].getName();
			list.add(new Link(fileName, fileName));
		}
	}

	@RequestMapping(value = { "/{fileName}" }, method = RequestMethod.GET)
	public ModelAndView listDirectory(@PathVariable("fileName") String fileName, HttpServletResponse resp, HttpServletRequest req) {
		String directory = getDirectory(req);
		File folder = new File(directory);
		if (folder.isDirectory()) {
			ModelAndView model = new ModelAndView();
			ArrayList<Link> list = new ArrayList<>();
			getList(list, directory, req.getRequestURL().toString());
			model.addObject("directory", fileName);
			model.addObject("list", list);
			model.addObject("showUploadLink", true);
			model.addObject("pass", env.getProperty("delete.pass"));
			model.addObject("back", File.separator + ctx.getAttribute("HOME_DIR").toString());
			model.setViewName("pages/index");
			log.info("ListFiles.listDirectory: show all files in directory --> " + directory );
			return model;
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private String getDirectory(HttpServletRequest req) {
		String fileFolder = ctx.getAttribute("FILES_DIR").toString();
		String servletPath = req.getServletPath();
		String directory = fileFolder + servletPath.replace(File.separator, File.separator);
		log.info("ListFiles.getDirectory: current directory --> " + directory );
		return directory;
	}

	private void getList(ArrayList<Link> list, String directory, String url) {
		log.info("ListFiles.getList: get file list from directory --> " + directory);
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			String name = listOfFiles[i].getName();
			if (name.endsWith(".tmp") || name.startsWith("~$")) {
				continue;
			}
			list.add(new Link(url + File.separator + name, name));
		}
	}

	@RequestMapping(value = "*/{fileName:.+}", method = RequestMethod.GET)
	public void listFiles(@PathVariable("fileName") String fileName, HttpServletResponse response, HttpServletRequest req) {
		try {
			String directory = getDirectory(req);
			File folder = new File(directory);
			if (folder.isFile()) {
				log.info("ListFiles.listFiles: show file --> " + directory );
				InputStream is = new FileInputStream(directory);
				org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
				response.flushBuffer();
			} else {
				throw new UnsupportedOperationException();
			}
		} catch (IOException ex) {
			throw new RuntimeException("IOError writing file to output stream");
		}
	}

	@RequestMapping(value = "*/upload", method = RequestMethod.POST)
	public String uploadFile(HttpServletResponse response, HttpServletRequest request) throws Exception {
		log.info("ListFiles.uploadFile: start upload file");
		String directory = getDirectory(request);
		
		if (SystemUtils.IS_OS_LINUX){
			directory = directory.substring(0, directory.lastIndexOf(File.separator));
		} else {
			directory = directory.replace("/", "\\");
			directory = directory.substring(0, directory.lastIndexOf(File.separator));
		}
		
		log.info("ListFiles.uploadFile: current file directory --> " + directory);
		DiskFileItemFactory fileFactory = new DiskFileItemFactory();
		File filesDir = new File(directory);
		fileFactory.setRepository(filesDir);
		ServletFileUpload uploader = new ServletFileUpload(fileFactory);
		if (!ServletFileUpload.isMultipartContent(request)) {
			throw new ServletException("Content type is not multipart/form-data");
		}

		FileItem loadItem = null;
		FileItem fileNameItem = null;
		try {
			List<FileItem> fileItemsList = uploader.parseRequest(request);
			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();
			while (fileItemsIterator.hasNext()) {
				FileItem fileItem = fileItemsIterator.next();
				if (fileItem.isFormField()){
					fileNameItem = fileItem;
				} else {
					loadItem = fileItem;
				}
			}
			if (fileNameItem == null || loadItem == null){
				throw new ServletException("Content type doesn't have multipart/form-data and html/plain parts");
			}
			writeFile(fileNameItem, loadItem, directory, request);
		} catch (Exception e) {
			throw new Exception("Exception in uploading file.");
		}
		return "redirect:/" + getPage(directory);
	}

	private String getPage(String directory) {
		int firstIndex = directory.lastIndexOf(File.separator) + 1;
		int lastIndex = directory.length();
		String page = directory.substring(firstIndex, lastIndex);
		log.info("ListFiles.getPage: go to page--> " + page);
		return page;
	}

	private void writeFile(FileItem fileNameItem, FileItem loadItem, String directory, HttpServletRequest request) throws Exception {
		String fileName = fileNameItem.getString();
		log.info("ListFiles.writeFile: start upload file --> " + fileName);
		
		if (isWindowsClient(request)) {
			log.info("ListFiles.writeFile: uploaded from Windows OS which uses separator --> " + "\\");
			fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
		} else {
			log.info("ListFiles.writeFile: uploaded from other then Windows OS which uses separator --> " + File.separator);
			fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
		}
		
		log.info("ListFiles.writeFile: full filename --> " + fileName);
		String name = fileName.substring(0, fileName.lastIndexOf("."));
		log.info("ListFiles.writeFile: filename --> " + name);
		String extension = fileName.substring(fileName.lastIndexOf("."), fileName.length());
		log.info("ListFiles.writeFile: extension --> " + extension);
		fileName = StringEscapeUtils.unescapeHtml(name);
		if (isCyrillic(fileName)){
			fileName = transliterate(fileName);
			log.info("ListFiles.writeFile: filename was generated to use latin symbol --> " + fileName);
		}
		fileName = fileName + extension;
		log.info("ListFiles.writeFile: FileName=" + fileName);
		log.info("ListFiles.writeFile: ContentType=" + loadItem.getContentType());
		log.info("ListFiles.writeFile: Size in bytes=" + loadItem.getSize());
		File file = new File(directory + File.separator + fileName);
		log.info("ListFiles.writeFile: absolute path at server --> " + file.getAbsolutePath());
		loadItem.write(file);
		log.info("ListFiles.writeFile: file " + fileName + " was uploaded successfully.");
	}

	private boolean isWindowsClient(HttpServletRequest request) {
		String browserDetails = request.getHeader("User-Agent");
		boolean isWindows = false;
		if (browserDetails.toLowerCase().indexOf("windows") >= 0 ){
            isWindows = true;
		} else {
			isWindows = false;
		}
		return isWindows;
	}

	@RequestMapping(value = "/createdir", method = RequestMethod.POST)
	public ModelAndView createDir(HttpServletResponse response, HttpServletRequest request) {
		String foldername = request.getParameter("foldername");
		foldername = StringEscapeUtils.unescapeHtml(foldername);
		if (isCyrillic(foldername)){
			foldername = transliterate(foldername);
			log.info("ListFiles.createDir: directory was generated to use latin symbol --> " + foldername);
		}
		log.info("ListFiles.createDir: new folder name --> " + foldername);
		String newDirectory = ctx.getAttribute("FILES_DIR").toString() + File.separator + foldername;
		log.info("ListFiles.createDir: full folder path --> " + newDirectory);
		File directory = new File(newDirectory);
		boolean isCreated = false;
		if (!directory.exists()){
			isCreated = directory.mkdirs();
		}
		log.info("ListFiles.createDir: directory " + directory + (isCreated ? " successfully created" : " not created"));
		return listRootFiles(request);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public String delete(HttpServletResponse response, HttpServletRequest request) throws IOException {
		log.info("ListFiles.delete: start deleting files");
		Enumeration<String> parameterNames = request.getParameterNames();
		String link = "";
		while (parameterNames.hasMoreElements()) {
			String item = parameterNames.nextElement();
			if (item.equals("email")){
				continue;
			}
			log.info("ListFiles.delete: full item directory --> " + item);
			String requestURL = request.getRequestURL().toString();
			int lastIndex = requestURL.lastIndexOf("/");
			requestURL = requestURL.substring(0,  lastIndex + 1);
			item = item.replace(requestURL, "");

			log.info("ListFiles.delete: part item directory --> " + item);
			
			if (item.indexOf(File.separator) > 0){
				log.info("ListFiles.delete: file is going to be deleted");
			} else {
				log.info("ListFiles.delete: directory is going to be deleted");
			}
			
			log.info("ListFiles.delete: item name --> " + item);
			String directory = ctx.getAttribute("FILES_DIR").toString() + File.separator + item;
			log.info("ListFiles.delete: current file/directory will be deleted --> " + directory);
			File file = new File(directory);
			
			if (file.isFile()) {
				boolean isDeleted = file.delete();
				lastIndex = item.indexOf(File.separator);
				link = item.substring(0, lastIndex);
				log.info("ListFiles.delete: file " + file.getAbsolutePath() + (isDeleted ? " was deleted" : " was not deleted"));
			} else {
				FileUtils.deleteDirectory(file);
				log.info("ListFiles.delete: directory " + file.getAbsolutePath() + " should be deleted now");
			}
		}
		log.info("ListFiles.delete: go to page --> " + link);
		return "redirect:/" + link;
	}
	
	@RequestMapping(value = "/send", method = RequestMethod.POST)
	public String sendToKindle(HttpServletResponse response, HttpServletRequest request) throws IOException {
		log.info("ListFiles.sendToKindle: check files to send to device");
		String link = "";
		String email = request.getParameter("email");
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String item = parameterNames.nextElement();
			if (item.equals("email")){
				continue;
			}
			String servletPath = request.getServletPath().substring(1,  request.getServletPath().length());
			String path = request.getRequestURL().toString().replace(servletPath, "") ;
			String fileFolder = ctx.getAttribute("FILES_DIR").toString();
			item = item.replace(path, "");
			link = item.substring(0, item.indexOf(File.separator));
			item = fileFolder + File.separator + item; 
			Sender.sendEmail(new File(item), email);
		}
		return "redirect:/" + link;
	}
	
	@RequestMapping(value = "/unzip", method = RequestMethod.POST)
    public String unzip(HttpServletResponse response, HttpServletRequest request) throws IOException {
          log.info("ListFiles.unzip: unzip files in current directory");
          String link = "";
          Enumeration<String> parameterNames = request.getParameterNames();
          while (parameterNames.hasMoreElements()) {
                 String item = parameterNames.nextElement();
                 if (item.equals("email")) {
                        continue;
                 }
                 String servletPath = request.getServletPath().substring(1, request.getServletPath().length());
                 String path = request.getRequestURL().toString().replace(servletPath, "");
                 String fileFolder = ctx.getAttribute("FILES_DIR").toString();
                 item = item.replace(path, "");
                 link = item.substring(0, item.indexOf(File.separator));
                 item = fileFolder + File.separator + item;
                 unZip(item, fileFolder + File.separator + link);
          }
          return "redirect:/" + link;
    }

    public void unZip(String zipFile, String outputFolder) {
          byte[] buffer = new byte[1024];
          try {
                 File folder = new File(outputFolder);
                 if (!folder.exists()) {
                        folder.mkdir();
                 }
                 ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                 ZipEntry ze = zis.getNextEntry();
                 while (ze != null) {
                        String fileName = ze.getName();
                        File newFile = new File(outputFolder + File.separator + fileName);
                        log.info("ListFiles.unZip: file unzip : " + newFile.getAbsoluteFile());
                        new File(newFile.getParent()).mkdirs();
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                               fos.write(buffer, 0, len);
                        }
                        fos.close();
                        ze = zis.getNextEntry();
                 }
                 zis.closeEntry();
                 zis.close();
          } catch (IOException ex) {
                 ex.printStackTrace();
          }
    }
	
	public static String transliterate(String name){
		  
	    char[] abcCyr =   {' ','à','á','â','ã','ä','å', '¸', 'æ','ç','è','é','ê','ë','ì','í','î','ï','ð','ñ','ò','ó','ô','õ', 'ö', '÷', 'ø',  'ù','ú','û','ü','ý', 'þ', 'ÿ'
	           ,'À','Á','Â','Ã','Ä','Å', '¨', 'Æ','Ç','È','É','Ê','Ë','Ì','Í','Î','Ï','Ð','Ñ','Ò','Ó','Ô','Õ', 'Ö', '×', 'Ø',  'Ù','Ú','Û','Á','Ý', 'Þ', 'ß'
	           ,'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'
	           ,'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
	  
	    String[] abcLat = {" ","a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch", "","i", "","e","ju","ja"
	           ,"A","B","V","G","D","E","E","Zh","Z","I","Y","K","L","M","N","O","P","R","S","T","U","F","H","Ts","Ch","Sh","Sch", "","I", "","E","Ju","Ja"
	           ,"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"
	           ,"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
	  
	    StringBuilder builder = new StringBuilder();
	  
	    for (int i = 0; i < name.length(); i++) {
	     for(int x = 0; x < abcCyr.length; x++ )
	      if (name.charAt(i) == abcCyr[x]) {
	       builder.append(abcLat[x]);
	      }
	    }
	    return builder.toString();
	}
	
	public static boolean isCyrillic(String text) {
		for(int i = 0; i < text.length(); i++) {
		    if(Character.UnicodeBlock.of(text.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
		        return true;
		    }
		}
		return false;
	}
}