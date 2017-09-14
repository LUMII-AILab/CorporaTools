package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.SentenceTransformEngine;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
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
	public static boolean CHANGE_IDS = true;

	public static void main(String[] args) throws Exception
	{
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
		PrintWriter statusOut = new PrintWriter(new PrintWriter(logFolder + "/log.txt", "UTF-8"), true);
		File[] listOfFiles = folder.listFiles();
		int omited = 0;
		for (File f : listOfFiles)
		{
			String fileName = f.getName();
			if (f.isDirectory() || f.getName().startsWith("~")) continue;
			if (fileName.endsWith(".pml"))
			{
				System.out.printf("Processing file \"%s\", ", fileName);
				statusOut.printf("Processing file \"%s\", ", fileName);
				String outPath = outputDataPath + fileName.substring(0, fileName.length() - 3) + "conllu";
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outPath), "UTF8"));
				omited = omited + transformFile(f.getAbsolutePath(), out, statusOut);
				out.flush();
				out.close();
			}
			else
			{
				System.out.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
				statusOut.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
			}
		}
		System.out.printf(
				"Everything is finished, %s trees was omited.\n", omited);
		statusOut.printf(
				"Everything is finished, %s trees was omited.\n", omited);
		statusOut.flush();
		statusOut.close();
	}

	/**
	 * Transform a single knitted LV TreeBank PML file to UD.
	 * @param inputPath		path to PML file
	 * @param conllOut		writer for output data
	 * @param warningsLog	log for warnings
	 * @return	count of ommited trees
	 */
	public static int transformFile(
			String inputPath, BufferedWriter conllOut, PrintWriter warningsLog)
	throws SAXException, ParserConfigurationException, XPathExpressionException, IOException
	{
		int omited = 0;
		NodeList pmlTrees = PmlLoader.getTrees(inputPath);
		System.out.printf("%s trees found...\n", pmlTrees.getLength());
		warningsLog.printf("%s trees found...\n", pmlTrees.getLength());
		String latestParId = null;
		if (pmlTrees.getLength() > 0)
		{
			String firstComment = XPathEngine.get().evaluate("./comment", pmlTrees.item(0));
			if (firstComment != null && firstComment.startsWith("AUTO"))
			{
				warningsLog.println("File starts with \"AUTO\" comment, everything is ommited!");
				System.out.println("File starts with \"AUTO\" comment, everything is ommited!");
				return pmlTrees.getLength();
			}
			for (int i = 0; i < pmlTrees.getLength(); i++)
			{
				String comment = XPathEngine.get().evaluate("./comment", pmlTrees.item(i));
				if (comment != null && comment.startsWith("FIXME"))
				{
					warningsLog.println("File contains with \"FIXME\" comment, everything is ommited!");
					System.out.println("File contains with \"FIXME\" comment, everything is ommited!");
					return pmlTrees.getLength();
				}
			}
			// Print out information about the start of the new document
			conllOut.write("# newDoc");
			String firstSentId = Utils.getId(pmlTrees.item(0));
			if (firstSentId.matches("a-.*-p\\d+s\\d+"))
			{
				String dicIdForPrint = firstSentId.substring(firstSentId.indexOf("-") + 1,
						firstSentId.lastIndexOf("-"));
				if (CHANGE_IDS) dicIdForPrint = dicIdForPrint.replace("LETA", "newswire");
				conllOut.write(" id=" + dicIdForPrint);

			}
			conllOut.newLine();
			// Print out information about the start of the first paragraph
			conllOut.write("# newpar\n");
			String firstSentLastId = Utils.getId(Utils.getLastByOrd(
					Utils.getAllPMLDescendants(pmlTrees.item(0))));
			if (firstSentLastId.matches("a-.*?-p\\d+s\\d+w\\d+"))
			{
				latestParId = firstSentLastId.substring(firstSentLastId.indexOf("-") + 1,
						firstSentLastId.lastIndexOf("s"));
				//conllOut.write(" id=" + latestParId);
			} else warningsLog.println(
					"Node id \"" + firstSentLastId + "\" in first sentence does not match paragraph searching pattern!");
		}
		for (int i = 0; i < pmlTrees.getLength(); i++)
		{
			String conllTree = SentenceTransformEngine.treeToConll(pmlTrees.item(i), warningsLog);
			if (i > 0)
			{
				String thisSentId = Utils.getId(pmlTrees.item(i));
				String newParId = null;
				if (thisSentId.matches("a-.*?-p\\d+s\\d+"))
					newParId = thisSentId.substring(thisSentId.indexOf("-") + 1,
							thisSentId.lastIndexOf("s"));
				else warningsLog.println(
						"Sentence id \"" + thisSentId + "\"does not match paragraph searching pattern!");
				if (newParId!= null && !newParId.equals(latestParId))
				{
					conllOut.write("# newpar\n");
					latestParId = Utils.getId(Utils.getLastByOrd(
							Utils.getAllPMLDescendants(pmlTrees.item(i))));
					if (latestParId.matches("a-.*?-p\\d+s\\d+w\\d+"))
						latestParId = latestParId.substring(latestParId.indexOf("-") + 1,
								latestParId.lastIndexOf("s"));
					else warningsLog.println(
							"Node id \"" + latestParId + "\" does not match paragraph searching pattern!");
				}
			}
			if (conllTree != null)
				conllOut.write(conllTree);
			else omited++;
		}
		System.out.println("Finished.");
		warningsLog.println("Finished.");
		return omited;
	}
}
