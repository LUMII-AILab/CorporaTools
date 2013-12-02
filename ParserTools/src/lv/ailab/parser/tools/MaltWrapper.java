package lv.ailab.parser.tools;

import java.io.*;
import java.util.ArrayList;
import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

public class MaltWrapper
{
	private MaltParserService maltServ;
	
	public MaltWrapper (String modelName) 
	throws MaltChainedException
	{
		 maltServ = new MaltParserService();
		 maltServ.initializeParserModel(
		 	"-c " + modelName + " -m parse -w . -lfi parser.log");
	}
	
	public DependencyStructure parse(String[] conllRows)
	throws MaltChainedException
	{
		return maltServ.parse(conllRows);
	}
	
	public static void main (String[] args)
	throws IOException, MaltChainedException
	{
		if (args.length < 1 || args[0] == null || args[0].equals(""))
		{
			System.out.println ("Piping wrapper for MaltParser v1.7x\n");
			
			System.out.println ("Usage:");
			System.out.println ("\tsystem input - conll-formed sentence,");
			System.out.println ("\t\tempty line - sentence end,");
			System.out.println ("\t\two empty lines - end service;");
			System.out.println ("\tsystem output - conll-formed output.\n");
			
			System.out.println ("Please, provide model name as the first parameter.\n");
			
			System.out.println ("Latvian Treebank project, LUMII, 2013, provided under GPL");
			
			return;
		}
		
		MaltWrapper wr = new MaltWrapper (args[0]);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String prev = null;
		ArrayList<String> sent = new ArrayList<String>();
		
		System.err.println ("MaltParser loaded.");
		
		while (true)
		{
			String line = in.readLine();
			if (line == null || line.trim().length() == 0)
			{
				if (line == null || prev != null && prev.trim().length() == 0) // "line == null" happens if stdin breaks.
				{
					System.err.println ("MaltWrapper ended.");
					return;
				}
								
				if (sent == null || sent.size() < 1)
				{
					prev = line;
					continue;
				}
				
				String[] conll = sent.toArray(new String[sent.size()]);
				DependencyStructure graph = wr.parse(conll);
				
				for (int i = 1; i <= graph.getHighestDependencyNodeIndex(); i++)
				{
					DependencyNode node = graph.getDependencyNode(i);
					
					StringBuilder outputRow = new StringBuilder();
					
					if (node != null)
					{
						for (SymbolTable table : node.getLabelTypes())
						{
							//System.out.print(node.getLabelSymbol(table) + "\t");
							outputRow.append(node.getLabelSymbol(table));
							outputRow.append("\t");
						}
						
						if (node.hasHead())
						{
							Edge  e = node.getHeadEdge();
							//System.out.print(e.getSource().getIndex() + "\t");
							outputRow.append(e.getSource().getIndex());
							outputRow.append("\t");
							if (e.isLabeled())
							{
								for (SymbolTable table : e.getLabelTypes())
								{
									//System.out.print(e.getLabelSymbol(table) + "\t");
									outputRow.append(e.getLabelSymbol(table));
									outputRow.append("\t");
								}
							}
							else
							{
								for (SymbolTable table : graph.getDefaultRootEdgeLabels().keySet()) {
									//System.out.print(graph.getDefaultRootEdgeLabelSymbol(table) + "\t");
									outputRow.append(graph.getDefaultRootEdgeLabelSymbol(table));
									outputRow.append("\t");
								}
							}
						}
						String output = outputRow.toString().replaceAll("#false#", "_");
						System.out.println(output);
					}
				}
				System.out.println();
				sent = new ArrayList<String>();
				
			}
			else
			{
				sent.add(line);
			}
			prev = line;
		}
	}
}