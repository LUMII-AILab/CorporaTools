package lv.ailab.lvtb.universalizer.transformator;

import lv.ailab.lvtb.universalizer.PmlLoader;
import lv.ailab.lvtb.universalizer.pml.utils.NodeFieldUtils;
import lv.ailab.lvtb.universalizer.utils.Logger;
import lv.ailab.lvtb.universalizer.utils.XPathEngine;
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
	public StringBuilder processed;
	protected TransformationParams params;
	public int omitted;
	private int added;
	public int all;

	public FileTransformator(TransformationParams params)
	{
		this.params = params;
		processed = new StringBuilder();
		omitted = 0;
		added = 0;
		all = 0;
	}

	/**
	 * Transform a single knitted LV TreeBank PML file to UD.
	 * @param inputPath   path to PML file
	 * @param logger	 log for warnings and IDs
	 */
	public void readAndTransform(
			String inputPath, Logger logger)
			throws SAXException, ParserConfigurationException, XPathExpressionException, IOException
	{
		NodeList pmlTrees = PmlLoader.getTrees(inputPath);
		System.out.printf("%s trees. ", pmlTrees.getLength());
		logger.printFoundTreesCount(pmlTrees.getLength());
		//warningsLog.printf("%s trees found...\n", pmlTrees.getLength());
		String paragraphId = "";
		// Print info in the file beginning.
		if (pmlTrees.getLength() > 0)
		{
			all = pmlTrees.getLength();
			String firstComment = XPathEngine.get().evaluate("./comment", pmlTrees.item(0));
			if (firstComment != null && firstComment.startsWith("AUTO"))
			{
				//warningsLog.println("File starts with \"AUTO\" comment, everything is ommited!");
				System.out.println("File starts with \"AUTO\" comment, everything is ommited!");
				logger.failFileForAUTO();
				omitted = pmlTrees.getLength();
				return;
			}
			// Print out information about the start of the new document
			processed.append("# newdoc");
			String firstSentId = NodeFieldUtils.getId(pmlTrees.item(0));
			Matcher idMatcher = Pattern.compile("a-(.*-p\\d+)s\\d+").matcher(firstSentId);
			if (idMatcher.matches())
			{
				String dicIdForPrint = firstSentId.substring(firstSentId.indexOf("-") + 1,
						firstSentId.lastIndexOf("-"));
				//if (params.CHANGE_IDS != null && params.CHANGE_IDS)
				//	dicIdForPrint = dicIdForPrint.replace("LETA", "newswire");
				processed.append(" id = ");
				processed.append(dicIdForPrint);
				paragraphId = idMatcher.group(1);

			}
			processed.append("\n");
			// Print out information about the start of the first paragraph.
			processed.append("# newpar");
			if (!paragraphId.isEmpty())
			{
				processed.append(" id = ");
				processed.append(paragraphId);
			}
			processed.append("\n");
		}
		// Process all trees, one by one...
		for (int i = 0; i < pmlTrees.getLength(); i++)
		{
			// However, there is no use to continue processing, if in case of
			// an error the whole file will be ommited and there already has
			// been an error.
			if (params.OMIT_WHOLE_FILES && omitted > 0)
			{
				omitted = pmlTrees.getLength();
				break;
			}

			// A "FIXME" comment mean unfinished and thus untransformable sentence.
			String comment = XPathEngine.get().evaluate("./comment", pmlTrees.item(i));
			if (comment != null && comment.startsWith("FIXME"))
			{
				//warningsLog.println("A sentence with \"FIXME\" ommited.");
				System.out.println("A sentence with \"FIXME\" ommited.");
				logger.failSentenceForFIXME();
				omitted++;
				continue;
			}

			// Try transforming tree.
			String conllTree = null;
			try
			{
				conllTree = SentenceTransformEngine.treeToConll(
						pmlTrees.item(i), params, logger);
			} catch (Exception e)
			{
				String treeId = NodeFieldUtils.getId(pmlTrees.item(i));
				//warningsLog.printf("A sentence %s failed with an exception: ", treeId);
				//e.printStackTrace(warningsLog);
				System.out.printf("Transforming sentence %s completely failed! Check structure and try again.\n", treeId);
				e.printStackTrace();
				logger.failSentenceForException(treeId, e, false);
			}

			// Has a new paragraph started?
			if (i > 0)
			{
				Matcher idMatcher = Pattern.compile("a-(.*-p\\d+)s\\d+").matcher(NodeFieldUtils.getId(pmlTrees.item(i)));
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

			// Store obtained results and update stats.
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
	 * @param conllOut    	path for the new result file
	 * @param logger 		log for warnings and ID mappings
	 * @return	if the file was actually written
	 */
	public boolean writeResult(
			String conllOut, Logger logger)
	throws IOException
	{
		if (omitted + added != all)
		{
			throw new IllegalStateException(
					"Algorithmic error! Omitted Trees + Good Trees != All Trees!");
		}
		if (params.OMIT_WHOLE_FILES && omitted > 0 || all - omitted < 1)
		{
			System.out.println("Finished - nothing to write.");
			logger.finishFile(true);
			//warningsLog.println("Finished - nothing to write.");
			return false;
		}
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(conllOut), "UTF8"));
		out.write(processed.toString());
		out.flush();
		out.close();
		System.out.println("Finished.");
		//warningsLog.println("Finished.");
		logger.finishFile(false);
		return true;
	}
}
