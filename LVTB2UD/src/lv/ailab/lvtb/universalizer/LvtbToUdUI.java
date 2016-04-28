package lv.ailab.lvtb.universalizer;

import lv.ailab.lvtb.universalizer.transformator.SentenceTransformator;
import org.w3c.dom.NodeList;

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
		for (int i = 0; i < pmlTrees.getLength(); i++)
		{
			String conllTree = SentenceTransformator.treeToConll(pmlTrees.item(i));
			if (conllTree != null) conllOut.write(conllTree);
			else
				omited++;
		}
		System.out.println("Finished.");
		return omited;
	}
}
