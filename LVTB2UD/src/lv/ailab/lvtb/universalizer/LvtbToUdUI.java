package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.transformator.FileTransformator;
import lv.ailab.lvtb.universalizer.transformator.SentenceTransformEngine;

import java.io.*;

/**
 * Overview
 *
 * Transformation tool for obtaining Latvian UD Treebank in CoNLL-U format from
 * knitted-in Latvian Treebank PMLs with normalized ord values (only token nodes
 * should be numbered).
 * Transformation on each tree separately. First a CoNLL-U table containing
 * only tokens and morphological information is built, then syntactic
 * information is added. To bild UD syntax tree, PML syntax tree is traversed
 * recursively, starting from the root phrase. For each phrase first its
 * constituents are processed, then phrase itself is transformed to UD
 * substructure, and at last phrase dependents are processed. During the process
 * a n-to-one mapping between pml nodes and CoNLL-U tokens is maintained:
 * at first mappings between PML token nodes and CoNLL tokens is added, then
 * each time a root token for phrase representing substructure is determined,
 * appropriate pairing is added to the mapping.
 *
 * Transformation is done according to UD v2 guidelines.
 *
 * NB! Transformator ignores files, where first sentence contains comment
 * starting with 'AUTO'. Transformator also ignores files where any sentence
 * contains comment starting with 'FIXME'.
 *
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class LvtbToUdUI
{
	public static String inputDataPath = "./data/pml/";
	public static String logPath = "./data/log/";
	public static String outputDataPath = "./data/conll-u/";
	public static boolean ADD_NODE_IDS = true;
	/**
	 * Rename newswire IDs' document part.
	 */
	public static boolean CHANGE_IDS = false;
	/**
	 * What to when a file contains an untransformable tree? For true - whole
	 * file is omitted; for false - only specific tree.
	 */
	public static boolean OMIT_WHOLE_FILES = true;

	public static void main(String[] args) throws Exception
	{
		File folder = new File(inputDataPath);
		SentenceTransformEngine.ADD_NODE_IDS = ADD_NODE_IDS;
		if (!folder.exists())
		{
			System.out.println(
					"Oops! Input data folder \"" + inputDataPath + "\" cannot be found!");
			return;
		}

		File outFolder = new File(outputDataPath);
		if (!outFolder.exists()) outFolder.mkdirs();
		File logFolder = new File(logPath);
		if (!logFolder.exists()) logFolder.mkdirs();
		PrintWriter statusOut = new PrintWriter(new PrintWriter(logFolder + "/log.txt", "UTF-8"), true);
		File[] listOfFiles = folder.listFiles();
		int omittedTrees = 0;
		int omittedFiles = 0;
		for (File f : listOfFiles)
		{
			String fileName = f.getName();
			if (f.isDirectory() || f.getName().startsWith("~")) continue;
			FileTransformator ft = new FileTransformator(CHANGE_IDS);
			if (fileName.endsWith(".pml")) try
			{
				System.out.printf("Processing file \"%s\", ", fileName);
				statusOut.printf("Processing file \"%s\", ", fileName);
				String outPath = outputDataPath + fileName.substring(0, fileName.length() - 3) + "conllu";
				ft.readAndTransform(f.getAbsolutePath(), statusOut);
				boolean madeFile = ft.writeResult(outPath, statusOut, OMIT_WHOLE_FILES);
				if (madeFile) omittedTrees = omittedTrees + ft.omitted;
				else
				{
					omittedTrees = omittedTrees + ft.all;
					omittedFiles++;
				}
			} catch (Exception e)
			{
				System.out.printf("File failed with exception %s.\n", e.toString());
				statusOut.print("File failed with exception: ");
				e.printStackTrace(statusOut);
				omittedTrees = omittedTrees + ft.all;
				omittedFiles++;
			}
			else
			{
				System.out.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
				statusOut.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
			}
		}
		if (omittedFiles == 0 && omittedTrees == 0)
		{
			System.out.println("Everything is finished, nothing was omited.");
			statusOut.println("Everything is finished, nothing was omited.");
		} else if (omittedFiles == 0)
		{
			System.out.printf(
					"Everything is finished, %s trees was omited.\n", omittedTrees);
			statusOut.printf(
					"Everything is finished, %s trees was omited.\n", omittedTrees);
		} else
		{
			System.out.printf(
					"Everything is finished, %s files and at least %s trees was omited.\n",
					omittedFiles, omittedTrees);
			statusOut.printf(
					"Everything is finished, %s files and at least %s trees was omited.\n",
					omittedFiles, omittedTrees);
		}
		statusOut.flush();
		statusOut.close();
	}

}
