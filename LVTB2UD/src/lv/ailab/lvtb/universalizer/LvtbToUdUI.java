package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.transformator.SentenceTransformator;
import org.w3c.dom.NodeList;

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
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class LvtbToUdUI
{
	public static String inputDataPath = "./data/pml/";
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
		File[] listOfFiles = folder.listFiles();
		int omited = 0;
		for (File f : listOfFiles)
		{
			String fileName = f.getName();
			if (f.isDirectory() || f.getName().startsWith("~")) continue;
			if (fileName.endsWith(".pml"))
			{
				System.out.printf("Processing file \"%s\", ", fileName);
				String outPath = outputDataPath + fileName.substring(0, fileName.length() - 3) + "conllu";
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outPath), "UTF8"));
				omited = omited + transformFile(f.getAbsolutePath(), out);
				out.flush();
				out.close();
			}
			else System.out.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
		}
		System.out.printf(
				"Everything is finished, %s trees was omited because of ellipsis.\n", omited);
	}

	public static int transformFile(String inputPath, BufferedWriter conllOut)
	throws Exception
	{
		int omited = 0;
		NodeList pmlTrees = PmlLoader.getTrees(inputPath);
		System.out.printf("%s trees found...\n", pmlTrees.getLength());
		String latestParId = null;
		if (pmlTrees.getLength() > 0)
		{
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
			} else System.err.println(
					"Node id \"" + firstSentLastId + "\" in first sentence does not match paragraph searching pattern.");
		}
		for (int i = 0; i < pmlTrees.getLength(); i++)
		{
			String conllTree = SentenceTransformator.treeToConll(pmlTrees.item(i));
			if (i > 0)
			{
				String thisSentId = Utils.getId(pmlTrees.item(i));
				String newParId = null;
				if (thisSentId.matches("a-.*?-p\\d+s\\d+"))
					newParId = thisSentId.substring(thisSentId.indexOf("-") + 1,
							thisSentId.lastIndexOf("s"));
				else System.err.println(
						"Sentence id \"" + thisSentId + "\"does not match paragraph searching pattern.");
				if (newParId!= null && !newParId.equals(latestParId))
				{
					conllOut.write("# newpar\n");
					latestParId = Utils.getId(Utils.getLastByOrd(
							Utils.getAllPMLDescendants(pmlTrees.item(i))));
					if (latestParId.matches("a-.*?-p\\d+s\\d+w\\d+"))
						latestParId = latestParId.substring(latestParId.indexOf("-") + 1,
								latestParId.lastIndexOf("s"));
					else System.err.println(
							"Node id \"" + latestParId + "\" does not match paragraph searching pattern.");
				}
			}
			if (conllTree != null)
				conllOut.write(conllTree);
			else
				omited++;
		}
		System.out.println("Finished.");
		return omited;
	}
}
