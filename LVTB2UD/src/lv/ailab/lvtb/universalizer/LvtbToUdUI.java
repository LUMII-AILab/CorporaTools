package lv.ailab.lvtb.universalizer;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;

/**
 * Created on 2016-04-17.
 *
 * @author Lauma
 */
public class LvtbToUdUI
{
	public static String inputDataPath = "./data/pml/";
	public static String outputDataPath = "./data/conll-u/";

	public static void main(String[] args)
	throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException
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
		for (File f : listOfFiles)
		{
			String fileName = f.getName();
			if (f.isDirectory() || f.getName().startsWith("~")) continue;
			if (fileName.endsWith(".pml"))
			{
				System.out.printf("Processing file \"%s\"...\t", fileName);
				String outPath = outputDataPath + fileName.substring(0, fileName.length() - 3) + "conllu";
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outPath), "UTF8"));
				transformFile(f.getAbsolutePath(), out);
				out.flush();
				out.close();
			}
			else System.out.println(
						"Oops! Unexpected extension for file \"" + fileName + "\"!");
		}
		System.out.println("Everything is finished.");
	}

	public static void transformFile(String inputPath, BufferedWriter conllOut)
	throws SAXException, ParserConfigurationException, XPathExpressionException,
			IOException
	{
		NodeList pmlTrees = PmlLoader.getTrees(inputPath);
		System.out.printf("%s trees found...\t", pmlTrees.getLength());
		for (int i = 0; i < pmlTrees.getLength(); i++)
			conllOut.write(SentenceTransformator.treeToConll(pmlTrees.item(i)));
		System.out.println("Finished.");
	}
}
