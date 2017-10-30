package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.PmlLoader;
import lv.ailab.lvtb.universalizer.pml.Utils;
import lv.ailab.lvtb.universalizer.util.XPathEngine;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		System.out.printf("%s trees. ", pmlTrees.getLength());
		warningsLog.printf("%s trees found...\n", pmlTrees.getLength());
		String paragraphId = "";
		// Print info in the file beginning.
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
			// Print out information about the start of the new document
			processed.append("# newdoc");
			String firstSentId = Utils.getId(pmlTrees.item(0));
			Matcher idMatcher = Pattern.compile("a-(.*-p\\d+)s\\d+").matcher(firstSentId);
			if (idMatcher.matches())
			{
				String dicIdForPrint = firstSentId.substring(firstSentId.indexOf("-") + 1,
						firstSentId.lastIndexOf("-"));
				if (changeIds)
					dicIdForPrint = dicIdForPrint.replace("LETA", "newswire");
				processed.append(" id = ");
				processed.append(dicIdForPrint);
				paragraphId = idMatcher.group(1);

			}
			processed.append("\n");
			// Print out information about the start of the first paragraph
			processed.append("# newpar");
			if (!paragraphId.isEmpty())
			{
				processed.append(" id = ");
				processed.append(paragraphId);
			}
			processed.append("\n");
		}
		// Process trees
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
			String conllTree = null;
			try
			{
				conllTree = SentenceTransformEngine.treeToConll(pmlTrees.item(i), warningsLog);
			} catch (Exception e)
			{
				String treeId = Utils.getId(pmlTrees.item(i));
				warningsLog.printf("A sentence %s failed with an exception: ", treeId);
				e.printStackTrace(warningsLog);
				System.out.printf("A sentence %s failed with an exception %s.\n", treeId, e.toString());
			}
			if (i > 0)
			{
				Matcher idMatcher = Pattern.compile("a-(.*-p\\d+)s\\d+").matcher(Utils.getId(pmlTrees.item(i)));
				if (idMatcher.matches())
				{
					String nextParaID = idMatcher.group(1);
					if (!nextParaID.isEmpty() && !paragraphId.equals(nextParaID))
					{
						processed.append("# newpar id = ");
						processed.append(nextParaID);
						processed.append("\n");
						paragraphId = nextParaID;
					}
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
