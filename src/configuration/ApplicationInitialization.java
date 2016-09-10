package configuration;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Servlet implements an Application initialisation before anything else is
 * done.
 */
public class ApplicationInitialization extends HttpServlet {

	private static final long serialVersionUID = -4356462604773999266L;
	private static Logger log = Logger.getLogger("file");

	public void init(ServletConfig config) throws ServletException {
		System.out.println("WEB Application is starting to initialize log4j");
		String log4jlocation = config.getInitParameter("log4j-properties-location");
		ServletContext context = config.getServletContext();
		System.setProperty("rootPath", context.getRealPath("/"));
		if (log4jlocation == null) {
			System.err.println("log4j properties not found. Start BasicConfigurator");
			BasicConfigurator.configure();
		} else {
			String webAppPath = context.getRealPath("/");
			String log4jpropPath = webAppPath + log4jlocation;
			System.out.println("file with log4j properties under " + log4jpropPath);
			File log4jFile = new File(log4jpropPath);
			
			if (log4jFile.exists()) {
				System.out.println("Configuring log4j from: " + log4jpropPath);
				PropertyConfigurator.configure(log4jpropPath);
			} else {
				System.err.println("Configuration file " + log4jpropPath + " not found. Initialize with basic configurator.");
				BasicConfigurator.configure();
			}
		}	
		setInitParameter(context);
		super.init(config);
	}


	private void setInitParameter(ServletContext ctx) throws ServletException {
		log.info("ApplicationInitialization.setInitParameter: start application initialization");
		String rootPath = null;

		if (SystemUtils.IS_OS_LINUX){
			log.info("ApplicationInitialization.setInitParameter: used Linux OS");
			rootPath = ctx.getInitParameter("root.path");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			log.info("ApplicationInitialization.setInitParameter: used Windows OS");
			rootPath = System.getProperty("catalina.home");
			log.info("ApplicationInitialization.setInitParameter: catalina.out path --> " + rootPath);
			rootPath = rootPath.substring(0, rootPath.indexOf(File.separator) + 1);
		} else {
			throw new ServletException("Application required Linux or Windows OS");
		}
		log.info("ApplicationInitialization.setInitParameter: files root path --> " + rootPath);
		
		String relativePath = ctx.getInitParameter("relative.path");
		log.info("ApplicationInitialization.setInitParameter: relative root path --> " + relativePath);
		
		File root = new File(rootPath);
		root.setExecutable(true);
		root.setReadable(true);
		root.setWritable(true);
		
		File fileDirectory = new File(rootPath + File.separator + relativePath);
		
		if (!fileDirectory.exists()){
			fileDirectory.setExecutable(true);
			fileDirectory.setWritable(true);
			fileDirectory.setReadable(true);
			log.info("File Directory: " + fileDirectory + " created to be used for storing files:" + fileDirectory.mkdirs());
		} else {
			log.info("File Directory: " + fileDirectory + " already exists");
		}
		
		ctx.setAttribute("FILES_DIR", fileDirectory);
		ctx.setAttribute("HOME_DIR", relativePath);
		log.info("ApplicationInitialization.setInitParameter: context attribute FILES_DIR --> " + fileDirectory);
		log.info("ApplicationInitialization.setInitParameter: context attribute HOME_DIR --> " + relativePath);
	}
}