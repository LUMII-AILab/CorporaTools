package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.PmlLoader;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;

/**
 * Transformation wrapper for a single file.
 */
// TODO: make omittWholeOnError class variable and do not process any more
// TODO  sentences after first error, if omittWholeOnError = true
public class FileTransformator
{
	/**
	 * Rename newswire IDs' document part.
	 */
	public boolean changeIds;
	public StringBuilder processed;
	public int omitted;
	private int added;
	public int all;

	public FileTransformator(boolean changeIds)
	{
		this.changeIds = changeIds;
		processed = new StringBuilder();
		omitted = 0;
		added = 0;
		all = 0;
	}

	/**
	 * Transform a single knitted LV TreeBank PML file to UD.
	 * @param inputPath   path to PML file
	 * @param warningsLog log for warnings
	 */
	public void readAndTransform(
			String inputPath, PrintWriter warningsLog)
			throws SAXException, ParserConfigurationException, XPathExpressionException, IOException
	{
		NodeList pmlTrees = PmlLoader.getTrees(inputPath);
		System.out.printf("%s trees found...\n", pmlTrees.getLength());
		warningsLog.printf("%s trees found...\n", pmlTrees.getLength());
		String latestParId = null;
		if (pmlTrees.getLength() > 0)
		{
			all = pmlTrees.getLength();
			String firstComment = XPathEngine.get().evaluate("./comment", pmlTrees.item(0));
			if (firstComment != null && firstComment.startsWith("AUTO"))
			{
				warningsLog.println("File starts with \"AUTO\" comment, everything is ommited!");
				System.out.println("File starts with \"AUTO\" comment, everything is ommited!");
				omitted = pmlTrees.getLength();
				return;
			}
			/*for (int i = 0; i < pmlTrees.getLength(); i++)
			{
				String comment = XPathEngine.get().evaluate("./comment", pmlTrees.item(i));
				if (comment != null && comment.startsWith("FIXME"))
				{
					warningsLog.println("File contains with \"FIXME\" comment, everything is ommited!");
					System.out.println("File contains with \"FIXME\" comment, everything is ommited!");
					return pmlTrees.getLength();
				}
			}*/
			// Print out information about the start of the new document
			processed.append("# newDoc");
			String firstSentId = Utils.getId(pmlTrees.item(0));
			if (firstSentId.matches("a-.*-p\\d+s\\d+"))
			{
				String dicIdForPrint = firstSentId.substring(firstSentId.indexOf("-") + 1,
						firstSentId.lastIndexOf("-"));
				if (changeIds)
					dicIdForPrint = dicIdForPrint.replace("LETA", "newswire");
				processed.append(" id=");
				processed.append(dicIdForPrint);

			}
			processed.append("\n");
			// Print out information about the start of the first paragraph
			processed.append("# newpar\n");
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
			String comment = XPathEngine.get().evaluate("./comment", pmlTrees.item(i));
			if (comment != null && comment.startsWith("FIXME"))
			{
				warningsLog.println("A sentence with \"FIXME\" ommited.");
				System.out.println("A sentence with \"FIXME\" ommited.");
				omitted++;
				continue;
			}
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
				if (newParId != null && !newParId.equals(latestParId))
				{
					processed.append("# newpar\n");
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
			{
				processed.append(conllTree);
				added++;
			}
			else omitted++;
		}
	}

	/**
	 * Make new file and print out the transformation results. Do not make
	 * an empty file or a file containing no sentences
	 * @param conllOut    		path for the new result file
	 * @param warningsLog 		log for warnings
	 * @param omitWholeOnError   if true, then in case of at least one omitted
	 *                           tree, whole file will be omitted.
	 * @return	if the file was actually written
	 */
	public boolean writeResult(
			String conllOut, PrintWriter warningsLog, boolean omitWholeOnError)
	throws IOException
	{
		if (omitted + added != all)
		{
			throw new IllegalStateException("Algorithmic error! Omitted Trees + Good Trees != All Trees!");
		}
		if (omitWholeOnError && omitted > 0 || all - omitted < 1)
		{
			System.out.println("Finished - nothing to write.");
			warningsLog.println("Finished - nothing to write.");
			return false;
		}
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(conllOut), "UTF8"));
		out.write(processed.toString());
		out.flush();
		out.close();
		System.out.println("Finished.");
		warningsLog.println("Finished.");
		return true;
	}
}
