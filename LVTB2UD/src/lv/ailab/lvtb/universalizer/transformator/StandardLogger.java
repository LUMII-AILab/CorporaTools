package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.utils.Logger;

import java.io.*;

public class StandardLogger
{
	public static Logger l;

	public static void initialize(Logger logger)
	{
		l = logger;
	}

	public static void initialize(File logFolder)
			throws FileNotFoundException, UnsupportedEncodingException
	{
		l = new Logger(logFolder + "/status.log", logFolder + "/ids.log");
	}

	public static void initialize(String statusLogPath, String idLogPath)
			throws FileNotFoundException, UnsupportedEncodingException
	{
		l = new Logger(statusLogPath, idLogPath);
	}

	public static void initializeAllToConsole (String consoleEncoding)
			throws UnsupportedEncodingException
	{
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out, consoleEncoding));
		l = new Logger(pw, pw);
	}

	public static void initializeLogToConsole (String consoleEncoding)
			throws UnsupportedEncodingException
	{
		l = new Logger(
				new PrintWriter(new OutputStreamWriter(System.out, consoleEncoding)),
				null);
	}

}
