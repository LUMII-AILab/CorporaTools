package lv.ailab.parser.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.io.dataformat.DataFormatSpecification;
import org.maltparser.core.symbol.trie.TrieSymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.core.syntaxgraph.node.Root;
import org.maltparser.core.syntaxgraph.reader.TabReader;
import org.maltparser.core.syntaxgraph.writer.TabWriter;

import org.maltparser.core.exception.MaltChainedException;



public final class ConllStats
{
	
	public static void main (String[] args)
	throws MaltChainedException
	{
		// TODO: Parameter check
		if (args == null || args.length < 2 || !(new File (args[0])).exists())
		{
			System.out.println ("Tool for collecting staistics about sintax graphs in conll file.\n");
			
			System.out.println ("Usage:");
			System.out.println ("   <filename.conll> --nonproj --multiroot --conll2009\n");
			
			System.out.println ("   --nonproj    collect statistics about nonprojectivity,");
			System.out.println ("   --multiroot  collect statistics about single/multi-rooted sentences,");
			System.out.println ("   --conll2009  switch to \"large\" output (default is CoNLL-X).");
			System.out.println ("All flags are optional but at least one must be present.\n");
			
			System.out.println ("Latvian Treebank project, LUMII, 2013, provided under GPL");
			return;			
		}
		
		ArrayList <String> params = new ArrayList(Arrays.asList(args));
		String fileName = params.remove(0);
		boolean nonproj = false;
		boolean multiroot = false;
		String formatFile = "conllx.xml";
		if (params.contains("--nonproj")) nonproj = true;
		if (params.contains("--multiroot")) multiroot = true;
		if (params.contains("--conll2009")) formatFile = "conll2009.xml";
		
		// Symbol table is used every now and then.
		TrieSymbolTableHandler symbolTable = new TrieSymbolTableHandler(
			TrieSymbolTableHandler.ADD_NEW_TO_TRIE);
			
		DataFormatInstance format = initDataFormat(formatFile, symbolTable);
		
		// Initialize CONLL reader.
		TabReader reader = new TabReader();
		reader.setDataFormatInstance(format);
		reader.open(fileName, "UTF-8");
		
		// Initialize CONLL writters.
		TabWriter projWriter = null, nonprojWriter = null;
		TabWriter multirtWriter = null, singlertWriter = null;
		String outputName = fileName;
		if (fileName.endsWith(".conll"))
			outputName = outputName.substring(
				0, outputName.length() - ".conll".length());
		if (nonproj)
		{
			projWriter = initWritter(format, outputName + ".proj.conll");
			nonprojWriter = initWritter(format, outputName + ".nonproj.conll");
		}
		if (multiroot)
		{
			multirtWriter = initWritter(format, outputName + ".multiroot.conll");
			singlertWriter = initWritter(format, outputName + ".singleroot.conll");
		}
		
		int projGraphs = 0, nonprojGraphs = 0;
		int multirtGraphs = 0, singlertGraphs = 0;
		int allGraphs = 0;
		
		// Process all trees one by one.
		DependencyGraph inputGraph = new DependencyGraph(symbolTable);
		boolean hasNext = true;
		while (hasNext)
		{
			hasNext = reader.readSentence(inputGraph);
			if (inputGraph.hasTokens())
			{
				allGraphs++;
				
				if (nonproj)
				{
					if (inputGraph.isProjective())
					{
						projWriter.writeSentence(inputGraph);
						projGraphs++;
					}
					else
					{
						nonprojWriter.writeSentence(inputGraph);
						nonprojGraphs++;
					}
				}
				
				if (multiroot)
				{
					Root root = (Root)inputGraph.getDependencyRoot();
					if (inputGraph.isSingleHeaded() &&
						root.getLeftDependentCount() + root.getRightDependentCount() == 1)
					{
						singlertWriter.writeSentence(inputGraph);
						singlertGraphs++;
					}
					else
					{
						multirtWriter.writeSentence(inputGraph);
						multirtGraphs++;
					}
				}		
			}
		}
		
		// Print stats.
		System.out.println("Proceesing " + fileName + " finished!");
		System.out.println(allGraphs + " sentences found.");
		System.out.println("Projective:      " + projGraphs);
		System.out.println("Non-projective:  " + nonprojGraphs);
		System.out.println("Single-rooted:   " + singlertGraphs);
		System.out.println("Multi-rooted:    " + multirtGraphs);

		// Close all data streams.
		reader.close();
		if (nonproj)
		{
			projWriter.close();
			nonprojWriter.close();

		}
		if (multiroot)
		{
			multirtWriter.close();
			singlertWriter.close();
		}
	}
	
	private static DataFormatInstance initDataFormat (
		String formatFile, TrieSymbolTableHandler symbolTable)
	throws MaltChainedException
	{
		// Initialize CONLL data format.
		DataFormatSpecification spec = new DataFormatSpecification();
		spec.parseDataFormatXMLfile(formatFile);
		DataFormatInstance instance = 
			spec.createDataFormatInstance(symbolTable, "none");
		return instance;
	}
	
	private static TabWriter initWritter (
		DataFormatInstance format, String fileName)
	throws MaltChainedException
	{
		TabWriter writer = new TabWriter();
		writer.setDataFormatInstance(format);
		writer.open(fileName, "UTF-8");
		return writer;	
	}
}