package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.transformator.PmlXmlFileTransformator;
import lv.ailab.lvtb.universalizer.transformator.StandardLogger;
import lv.ailab.lvtb.universalizer.transformator.TransformationParams;

import java.io.File;

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
	public static TransformationParams params = new TransformationParams();

	public static void main(String[] args) throws Exception
	{
		boolean validParams = updateParamsFormArgs(args);
		if (!validParams)
		{
			printMan();
			return;
		}
		File folder = new File(inputDataPath);
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
		//StandardLogger.l = new Logger(logFolder + "/status.log", logFolder + "/ids.log");
		StandardLogger.initialize(logFolder);
		File[] listOfFiles = folder.listFiles();
		int omittedTrees = 0;
		int omittedFiles = 0;
		int autoFiles = 0;
		int fixmeFiles = 0;
		int crashFiles = 0;
		int depBaseRoleSent = 0;
		int depBaseRoleSum = 0;
		int depEnhRoleSent = 0;
		int depEnhRoleSum = 0;
		for (File f : listOfFiles)
		{
			String fileName = f.getName();
			if (f.isDirectory() || f.getName().startsWith("~")) continue;
			PmlXmlFileTransformator ft = new PmlXmlFileTransformator(params);
			if (fileName.endsWith(".pml")) try
			{
				System.out.printf("Processing file \"%s\", ", fileName);
				StandardLogger.l.startFile(fileName);
				String outPath = outputDataPath + fileName.substring(0, fileName.length() - 3) + "conllu";
				ft.readAndTransform(f.getAbsolutePath());
				boolean madeFile = ft.writeResult(outPath);
				if (madeFile) omittedTrees = omittedTrees + ft.omitted;
				else
				{
					omittedTrees = omittedTrees + ft.all;
					omittedFiles++;
				}
				if (ft.hasCrashSent) crashFiles++;
				if (ft.hasFixme) fixmeFiles++;
				if (ft.hasAuto) autoFiles++;
				depBaseRoleSent = depBaseRoleSent + ft.depRoleBaseSent;
				depBaseRoleSum = depBaseRoleSum + ft.depRoleBaseSum;
				depEnhRoleSent = depEnhRoleSent+ ft.depRoleEnhSent;
				depEnhRoleSum = depEnhRoleSum + ft.depRoleEnhSum;
			} catch (Exception e)
			{
				System.out.printf("File failed with exception %s.\n", e.toString());
				StandardLogger.l.finishFileWithException(e);
				omittedTrees = omittedTrees + ft.all;
				omittedFiles++;
				crashFiles++;
			}
			else
			{
				System.out.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
				StandardLogger.l.finishFileWithBadExt(fileName);
			}

		}
		if (omittedFiles == 0 && omittedTrees == 0)
			System.out.println("Everything is finished, nothing was omited.");
		else if (omittedFiles == 0)
			System.out.printf(
					"Everything is finished, %s trees was omited.\n", omittedTrees);
		else
			System.out.printf(
					"Everything is finished, %s file(s) and %s tree(s) was omited.\n",
					omittedFiles, omittedTrees);
		if (autoFiles > 0)
			System.out.printf("%s file(s) have AUTOs.\n", autoFiles);
		if (fixmeFiles > 0)
			System.out.printf("%s file(s) have FIXMEs.\n", fixmeFiles);
		if (crashFiles > 0)
			System.out.printf("%s file(s) have crashing sentences.\n", crashFiles);
		if (depBaseRoleSent > 0)
			System.out.printf(
					"%s sentence(s) have altogether %s 'dep' role(s) in basic dependency layer.\n",
					depBaseRoleSent, depBaseRoleSum);
		if (depEnhRoleSent > 0)
			System.out.printf(
					"%s sentence(s) have altogether %s 'dep' role(s) in enhanced dependency layer.\n",
					depEnhRoleSent, depEnhRoleSum);
		StandardLogger.l.finalStatsAndClose(omittedFiles, omittedTrees,
				autoFiles, fixmeFiles, crashFiles, depBaseRoleSent,
				depBaseRoleSum, depEnhRoleSent, depEnhRoleSum);
	}

	/**
	 * Process parameters provided for main function. For description see
	 * printMan().
	 * @return if the parameter processing was successful
	 */
	protected static boolean updateParamsFormArgs(String[] args)
	{
		boolean hasMandatory = false;
		if (args.length < 1) return false;
		try
		{
			for (String arg : args)
			{
				String key = arg.substring(0, arg.indexOf("=")).trim().toLowerCase();
				String valueStr = arg.substring(arg.indexOf("=") + 1).trim().toLowerCase();
				if (key.isEmpty() || valueStr.isEmpty()) return false;
				Boolean value = null;
				boolean isBool = true;

				switch (valueStr)
				{
					case "true":
					case "1":
						value = true;
						break;
					case "false":
					case "0":
						value = false;
						break;
					default: isBool = false;
				}

				switch (key)
				{
					case "add_node_ids":
						if (isBool)
						{
							params.ADD_NODE_IDS = value;
							hasMandatory = true;
						}
						else return false;
						break;
					case "split_ellipsis":
						if (isBool) params.SPLIT_NONEMPTY_ELLIPSIS = value;
						else return false;
						break;
					case "debug":
						if (isBool) params.DEBUG = value;
						else return false;
						break;
					case "warn_ellipsis":
						if (isBool) params.WARN_ELLIPSIS = value;
						else return false;
						break;
					case "warn_omissions":
						if (isBool) params.WARN_OMISSIONS = value;
						else return false;
						break;
//					case "do_enhanced":
//						if (isBool) params.DO_ENHANCED = value;
//						else return false;
//						break;
					case "induce_phrase_tags":
						if (isBool) params.INDUCE_PHRASE_TAGS = value;
						else return false;
						break;
					case "omit_whole_files":
						if (isBool) params.OMIT_WHOLE_FILES = value;
						else return false;
						break;
					case "input":
						if (!isBool) inputDataPath = valueStr;
						else return false;
						break;
					case "output":
						if (!isBool) outputDataPath = valueStr;
						else return false;
						break;
					case "log":
						if (!isBool) logPath = valueStr;
						else return false;
						break;
					default:
						return false;
				}
			}
		}
		catch (NullPointerException|IndexOutOfBoundsException e)
		{
			return false;
		}
		return hasMandatory;
	}

	/**
	 * Print information about parameters.
	 */
	protected static void printMan()
	{
		System.out.print(
				"Script for transforming Latvian Treebank PML files to Universal Dependency\n" +
				"CoNLL-U.\n" +
				"Parameter passing format: key1=value key2=value ...\n" +
				"Possible key values (case insensitive):\n" +
				"  add_node_ids   [bool, mandatory] - should Misc column contain node IDs from\n" +
				"                                   LVTB?\n" +
				"  split_ellipsis [bool, true by default] - treat ellipsis node with morphology\n" +
				"                                         as two nodes.\n" +
				"  debug          [bool, false by default] - print debug message for each node.\n" +
				"  warn_ellipsis  [bool, false by default] - warn on ellipsis.\n" +
				"  warn_omissions [bool, true  by default]  - warn if a sentence is omitted.\n" +
//				"  do_enhanced    [bool, true  by default]  - create enhanced dependency graph.\n" +
				"  induce_phrase_tags [bool, true by default] - for already processed nodes\n" +
				"                                   without the phrase tag induce a tag based\n" +
				"                                   on node chosen as substructure root.\n" +
				"  omit_whole_files [bool, false by default] - omit all trees in the file if\n" +
				"                                   at least one fails.\n" +
				"  input  [string, ./data/pml/     by default] - input data folder.\n" +
				"  output [string, ./data/log/     by default] - log folder.\n" +
				"  log    [string, ./data/conll-u/ by default] - output folder.\n" +
				"Recognized boolean values (case insensitive):\n" +
				"  true, 1   - for true\n" +
				"  false, 0  - for false\n" +
				"\n" +
				"Lauma, Latvian Treebank, AILab, IMCS UL, 2015-2017.\n"
		);
	}

}
