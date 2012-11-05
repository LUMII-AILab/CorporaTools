package lv.morphology.corpora;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;

import lv.semti.morphology.analyzer.*;
import lv.semti.morphology.attributes.AttributeNames;
import lv.semti.morphology.attributes.AttributeValues;
import lv.semti.morphology.corpus.Statistics;

import lv.morphology.corpora.tests.*;
import lv.morphology.corpora.util.MorphoEntry;
/**
 * Meaningfull handling of abbriveations (tag: y) currently not implemented.
 */
public class CorpusVerificator
{	
	Analyzer anal;
	Statistics morphStat;
	SingleTokenTests singleTests;
	ContinousTests contTests;
	public int accumLength = 4;
	
	/**
	 * CLI.
	 */
	public static void main(String[] args)
	throws Exception
	{
		if (args.length < 2 || args.length > 4 ||
			(args[0].equalsIgnoreCase("-plain") || args[0].equalsIgnoreCase("-p"))
			&& args.length == 2)
		{
			System.out.println("Programm for verifying morphological corpora.");
			System.out.println("AILab, IMCS, UL, 2012-01-18.\r\n");
			
			System.out.println(
				"Usage: [-flag] input_file_or_dir output_file_or_dir [delimiter]");
			System.out.println("Avialable flags:");
			System.out.println(
				"    -plain or -p    process plain-text file (XML by default).");
			System.out.println(
				"For plain-text procesing, default delimiter ir space.");
			
			return;
		}
		
		CorpusVerificator cv = new CorpusVerificator();
		
		if (args[0].equalsIgnoreCase("-plain") || args[0].equalsIgnoreCase("-p"))
		{
		//	if (args.length == 3)
		//		cv.processPlainText(args[1], args[2], " ");
		//	else
		//		cv.processPlainText(args[1], args[2], args[3]);
			System.out.println("Sorry, not available right now.");
		} else
		{
			File inPath = new File(args[0]);
			File outPath = new File(args[1]);
			if (inPath.isDirectory())
			{
				if (!outPath.exists())
					outPath.mkdir();
				for (File in : inPath.listFiles())
				{
					System.out.println("Processing " + in.getName() + "...");
					cv.processPmlMFile(
						in.getAbsolutePath(), args[1] + "/" + in.getName());
				}
			}
			else cv.processPmlMFile(args[0], args[1]);
		}
		return;
	}

	/**
	 * Constructor;
	 */
	public CorpusVerificator()
	throws Exception
	{
		anal = new Analyzer("lib/morphology/Lexicon.xml");
		
//		Word w = anal.analyze("no alkohola");
//		System.out.println("noalkoholu atpazina? " + w.isRecognized());
		
		morphStat = new Statistics("lib/morphology/Statistics.xml");
		singleTests = new SingleTokenTests(anal);
		contTests = new ContinousTests(anal);
	}
	
	/**
	 * Fully process first token in the given string. If necessary 1st token
	 * will be concatinated with some of following tokens.
	 */
	public ArrayList<String> processFirst(ArrayList<MorphoEntry> string)
	{
		// We want to handle false residuals and unannotated tokens poprely in
		// terms of concatenation.
		ArrayList<String> res = process(string.get(0));
		String contRes = contTests.concatFirst(string);
		
		if (contRes != null)
		{
			//res = process(string.get(0));
			res.add(0, contRes);
		}
		return res;
	}
	
	/**
	 * Process single entry - verify, capitalize, etc.
	 */
	public ArrayList<String> process(MorphoEntry me)
	{
		ArrayList<String> res = new ArrayList<String>();
		boolean falseResidual = false;
		
		if (me.attributes == null) res.add("No Tag");
		else if (MarkupConverter.toKamolsMarkupNoDefaults(me.attributes).contains("_"))
			res.add("Unfilled Tag Positions");
		
		if (singleTests.tokenizationError(me)) res.add("Tokenization Error");
		if (singleTests.falseResidual(me))
		{
			res.add("False Residual");
			falseResidual = true;
		}
		if (!singleTests.validLemma(me)) res.add("Invalid Lemma");
		
		
		String ref = refine(me, falseResidual);
		if (ref != null) res.add(ref);
		String capit = capitalizeLemma(me);
		if (capit != null) res.add(capit);
		return res;
	}
	
	/**
	 * Try to fill empty spaces in tag.
	 */
	public String refine(MorphoEntry me, boolean ignoreOldTag)
	{
		// No need to refine.
		if (me.attributes != null && 
			!MarkupConverter.toKamolsMarkupNoDefaults(me.attributes).contains("_"))
			return null;

		// Could not refine.
		Word w = anal.analyze(me.token);
		if (!w.isRecognized()) return null;


		// Wordform comparison based on statistics.
		Comparator<Wordform> wComp = new Comparator<Wordform>()
		{
    		public int compare(Wordform w1, Wordform w2)
    		{
    			double e1 = morphStat.getEstimate(w1);
    			double e2 = morphStat.getEstimate(w2);
    			if (e1 < e2) return 1;
    			else if (e1 == e2) return 0;
    			else return -1;
    		}
		};
		
		// Sort wordforms.
		Wordform[] sortedWfs = w.wordforms.toArray(new Wordform[w.wordforms.size()]);
		Arrays.sort(sortedWfs, wComp);
		
		//System.out.println(morphStat.getEstimate(sortedWfs[0]) + " > "
		//	+ morphStat.getEstimate(sortedWfs[sortedWfs.length - 1]));
		
			
		if (me.attributes == null || ignoreOldTag)
		{
			// To include defaults provided by annotator
			me.attributes = MarkupConverter.fromKamolsMarkup(
				MarkupConverter.toKamolsMarkup(sortedWfs[0]));
			me.setLemma(sortedWfs[0].getValue(AttributeNames.i_Lemma));
			return "Auto-annotated";
		} else
		{
			String oldTag = MarkupConverter.toKamolsMarkupNoDefaults(me.attributes);
			
			for (Wordform wf : sortedWfs)
			{
				if (wf.isMatchingWeak(me.attributes))
				{
					String propTag = MarkupConverter.toKamolsMarkup(wf);
					// Merge old tag with new.
					StringBuffer newTag = new StringBuffer();
					if (propTag.length() != oldTag.length())
						System.out.println("Tags " + oldTag + " and "
							+ propTag + " does not match by length.");
					for (int i = 0; i < oldTag.length(); i++)
					{
						if (oldTag.charAt(i) == '_' ) 
							newTag.append(propTag.charAt(i));
						else newTag.append(oldTag.charAt(i));	
					}
					if (!newTag.toString().equals(oldTag))
					{
						//me.attributes = MarkupConverter.fromKamolsMarkup(
						//	newTag.toString());
						me.setAttributes(newTag.toString());
						
						// Update lemma.
						String lemmaUpd = "";
						if (me.lemma == null)
						{
							me.setLemma(sortedWfs[0].getValue(
								AttributeNames.i_Lemma));
							lemmaUpd = " and Lemmma";
						}
						return "Tag" + lemmaUpd +" Updated (" + oldTag + ")";
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Capitalizes first letter of the proper noun lemmas.
	 */
	private String capitalizeLemma(MorphoEntry me)
	{
		if (me.lemma == null) return null;
		if (me.lemma.toLowerCase().equals(me.lemma.toUpperCase())) return null;
		
		String first = me.lemma.substring(0, 1);
		String tail = me.lemma.substring(1);
		//if (tail == null) tail = "";
		if (me.attributes == null) return null;
		
		// Process lemmas with iNVERTED capitalization.
		boolean iNVERTED = false;
		if (first.toLowerCase().equals(first)
			&& tail.toUpperCase().equals(tail)
			&& ! me.attributes.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Punctuation))
		{
			me.lemma = me.lemma.toLowerCase();
			first = me.lemma.substring(0, 1);
			tail = me.lemma.substring(1);
			iNVERTED = true;
		}
		
		// Recapitalize lemmas of proper nouns.
		if (me.attributes != null &&
			me.attributes.isMatchingStrong(
				AttributeNames.i_PartOfSpeech, AttributeNames.v_Noun) &&
			me.attributes.isMatchingStrong(
				AttributeNames.i_NounType, AttributeNames.v_ProperNoun))
		{
				
			if (me.lemma.toLowerCase().equals(me.lemma))
			{
				me.lemma = first.toUpperCase() + tail;
				return "Lemma Recapitalized";
			}
			
		}
		
		if (iNVERTED) return "Lemma Recapitalized";
		else return null;

	}
	
	/**
	 * Runs verification tests and lemma capitalization on plain-text file.
	 */
	public void processPmlMFile(String inFile, String outFile)
	throws IOException, SAXException, ParserConfigurationException	
	{
		// Open.
		InputSource in = new InputSource(new InputStreamReader(
			new FileInputStream(inFile), "UTF8"));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
			new FileOutputStream(outFile), "UTF8"));
		in.setEncoding("UTF8");

		// Parse.
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		PmlMHandler handler = new PmlMHandler(this, out, accumLength);
    	parser.parse(in, handler); 
    	
    	// Close.
    	out.flush();
    	out.close();
    }
	
	
	/**
	 * Run verification tests and lemma capitalization on plain-text file.
	 *
	 * TODO use continous tests.
	 */
/*	public void processPlainText (
		String inFile, String outFile, String delimiter)
	throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(
			new FileInputStream(inFile), "UTF8"));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
			new FileOutputStream(outFile), "UTF8"));
		
		// Process each line (coresponds to one token).
		String line = in.readLine();
		while (line != null)
		{
			// Input.
			// Split.
			String[] splited = line.split(delimiter);
			if (splited.length % 2 == 0 || splited.length < 3)
				throw new IOException ("Could not parse \"" + line + "\"!");
			
			// If delimiter splits in more than 3 pieces, it is assumed that
			// tag is the middle piece.
			int mid = (splited.length + 1) / 2;
			String token = "";
			String tag = splited[mid];
			String lemma = "";
			for (int i = 0; i < mid; i++)
			{
				token = token + delimiter + splited[i];
				lemma = lemma + delimiter + splited[mid + 1 + i];
			}
			
			token = token.trim();
			tag = tag.trim();
			lemma = lemma.trim();
			
			// Process data
			ArrayList<String> res = process(
				new MorphoEntry(token, tag, lemma, false));
			
			// Output.
			out.write(token + "\t" + tag + "\t" + lemma);
			if (res != null) for (String r : res)
			{
				out.write("\t" + r);
			}
			out.newLine();
			
			line = in.readLine();
		}
			
		in.close();
		out.flush();
		out.close();
	}//*/
}