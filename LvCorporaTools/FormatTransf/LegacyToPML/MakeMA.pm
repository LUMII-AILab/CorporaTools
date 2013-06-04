#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::LegacyToPML::MakeMA;

use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(makeMA roles);

our $vers = 0.4;
our $progname = "m un a līmeņa automātiskais konvertors, $vers";
#our $metainfo = 'Nezināms korpuss';
our %roles = ('teikuma priekšmets' => 'subj',
				'izteicējs' => 'pred',
				'apzīmētājs' => 'attr',
				'vietas apstāklis' => 'adv',
				'laika apstāklis' => 'adv',
				'laika apstāklis multi' => 'adv',
				'veida apstāklis multi' => 'adv',
				'mēra apstāklis' => 'adv',
				'cēloņa apstāklis' => 'adv',
				'veida apstāklis' => 'adv',
				'netiešais papildinātājs' => 'obj',
				'tiešais papildinātājs' => 'obj',
				'spk' => 'spc',
				'apvienojums' => 'basElem',
				'prievārdeklis' => 'prep',
				'pielikums' => 'app',
				'īpašības vārds' => 'attr',
				'pusprievārds' => 'prep',
				'nomināls_izteicējs' => 'pred',
				'modāls_izteicējs' => 'pred',
				'redukcija' => 'N/A',
				'skaitlis' => 'N/A',
				'xyz' => 'N/A',
				);

###############################################################################
# This program creates PML M and A files, if data containing sentence
# boundries and morphology and w files are provided. All input files must be
# UTF-8.
#
# Input parameters: sentence dir, w dir, morpho dir, otput dir, [parse as
# Annotator output (1) or as simply vertical format].
#
# Developed on ActivePerl 5.10.1.1007, tested on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub makeMA
{
	if (not @_ or @_ le 4)
	{
		print <<END;
Script for creating PML M and A files, if data containing sentence boundries
and morphology and w files are provided. All input files must be UTF-8.
Corresponding files must have corresponding filenames.

Params:
   sentence directory (.txt files)
   w files directory (.w files)
   morphology directory (.txt files)
   output directory
   parse morphology as Annotator output (1) or as simple vertical format (0)
      [opt, default 0]
   log directory [opt, ./log by default]

Latvian Treebank project, LUMII, 2011-2012, provided under GPL
END
		exit 1;
	}
	my $sentDir = shift; #$ARGV[0] ? $ARGV[0] : 'teik';
	my $wDir = shift; #$ARGV[1] ? $ARGV[1] : 'wrez';
	my $morphoDir = shift; #$ARGV[2] ? $ARGV[2] : 'morfo';
	my $outDir = shift; #$ARGV[3] ? $ARGV[3] : 'rez';
	my $inputFormAnnot = shift; #$ARGV[4];
	my $logDir = (shift or 'log');

	#if (open META, "<:encoding(UTF-8)", $ARGV[4]) {
	#	$metainfo = <META>;
	#	close META;
	#};

	#opendir(TKDIR, $sentDir) or die "dir $!";
	opendir(W_DIR, $wDir) or die "dir $!";
	mkdir $outDir;
	mkdir $logDir;
	while (defined(my $wFile = readdir(W_DIR)))
	{
		if (! -d "$wDir\\$wFile")
		{
			$wFile =~ /^(.*)\.w$/;
			my $docId = $1;
			print "Processing $docId.\n";
			open W_IN, "<:encoding(UTF-8)", "$wDir\\$wFile"
				or die "w file error $wFile: $!";
			#open SENT_IN, "<:encoding(UTF-8)", "$sentDir\\rez-${docId}.txt"
			#	or die "sentence file error rez-${docId}.txt: $!";
			#open SENT_IN, "<:encoding(UTF-8)", "$sentDir\\${docId}-teikumos.txt"
			#	or die "sentence file error ${docId}-teikumos.txt: $!";
			open SENT_IN, "<:encoding(UTF-8)", "$sentDir\\${docId}.txt"
				or die "sentence file error ${docId}.txt: $!";
			#open MORPHO_IN, "<:encoding(UTF-8)", "$morphoDir\\${docId}_galigais.txt"
			#	or die "morphology file error ${docId}_galigais.txt: $!";
			open MORPHO_IN, "<:encoding(UTF-8)", "$morphoDir\\${docId}.txt"
				or die "morphology file error ${docId}.txt: $!";
			open M_OUT, ">:encoding(UTF-8)", "$outDir\\$docId.m"
				or die "m file error: $!";
			open A_OUT, ">:encoding(UTF-8)", "$outDir\\$docId.a"
				or die "a file error: $!";
			open LOG_OUT, ">:encoding(UTF-8)", "$logDir\\$docId.txt";
					
			_printMBegin(\*M_OUT, $docId);
			_printABegin(\*A_OUT, $docId);
			my $sentId = 0;
			my $doPrintSentId = 1;
			my $morpho = <MORPHO_IN>;
			while (defined($morpho) and $morpho =~ /^\s*$/) {
				print LOG_OUT "bad morpho $morpho";
				$morpho = <MORPHO_IN>;
			}
			# Ciklaa apstraadaa katru teikumu.
			while (<SENT_IN>)
			{
				/^\s*(.*?)\s*$/;
				my $sentence = $1;
				my $tokId = 0;
				$sentId++;
				$doPrintSentId = 1;
				my ($token, $wTag);
				my $doReadNewToken = 1;
				# Ciklaa apstraadaa visus tokenus dotajaa teikumaa.
				#while (!$doReadNewToken or $sentence =~ m#(\w+|\.+|!+|\S)#g)
				while (!$doReadNewToken or
					$sentence =~ m#(\d+|\p{L}+|\.+|!+|\?+|\S)#g)
					#$sentence =~ m#(\d+|[a-zA-ZāčēģīķļņōŗšūžĀČĒĢĪĶĻŅŌŖŠŪŽ]+|\.+|!+|\S)#g)
				{
					if ($doReadNewToken)
					{
						# Nolasa jaunu tokenu no .w faila.
						$token = $1;
						$tokId++;
						$wTag = <W_IN>;
						while($wTag !~ m#</w>#) {
							$wTag .= <W_IN>;
						}
					}
					# Izparsee no <w id="..">...</w> identifikatoru un tokenu.
					my ($wId, $wToken) = ($wTag =~ m#<w id="(.*?)".*<token>(.*?)</token>#s);
					# Izparsee rindkopas numuru.
					$wId =~ /w-${docId}-p(.+)w\d+/;
					my $parId = $1;
					# Izdrukaa teikuma saakuma tagus .a un .m failos (ja tas ir nepiecieshams.
					if ($doPrintSentId)
					{
						print M_OUT qq#\t<s id="m-${docId}-p${parId}s${sentId}">\n#;
						_printASentBegin (\*A_OUT, $docId, $parId, $sentId);

						$doPrintSentId = 0;
					}
					
					# Izparsee morfologjijas informaaciju.
					my($mToken, $tag, $lemma, $l1, $l2, $a1, $a2) = 
						$inputFormAnnot ?
							_parseAnnotatorLine ($morpho) : _parsePlainLine ($morpho);
					my $normToken = _normalize ($token);
					my $normW = _normalize ($wToken);
					my $normM = _normalize ($mToken);
					print LOG_OUT "$token\t$wToken\t$wId\t$mToken\t$morpho";
					if (!defined ($mToken))
					{
						print "morpho|$morpho|$mToken, $tag, $lemma, $l1, $l2, $a1\n";
					}
					# Apstraadaa gadiijumus, kad visi tokeni atbilst viens otram.
					#if ($normToken eq $normM and $normToken eq $normW)
					if ($normToken =~ /^\Q$normM\E$/ and $normToken =~ /^\Q$normW\E$/)
					# vaards, iistais w liimenja tokens un iistais gramatikas ierksts
					{
						# Izvada rezultaatus.
						_printMData (\*M_OUT, $docId, $parId, $sentId, $tokId, $wId, $token, $lemma, $tag);
						my $role = (($l1 =~ /^\s*'?\s*'?\s*$/ or $l1 =~ /null/i) ? $l2 : $l1);
						$role = (($role =~ /^\s*'?\s*'?\s*$/ or $role =~ /null/i) ? 'N/A' : $role);
						my $pmlRole = $roles{$role};
						if (!defined($pmlRole) or ($pmlRole eq ""))
						{
							$pmlRole = $role;
						}
						_printADataSimple (\*A_OUT, $docId, $parId, $sentId, $tokId, $pmlRole, $token);
						
						# Nolasa jauno morfologjiju.
						$morpho = <MORPHO_IN>;
						while (defined($morpho) and $morpho =~ /^\s*$/) {
							print LOG_OUT "bad morpho $morpho";
							$morpho = <MORPHO_IN>;
						}
						$doReadNewToken = 1;
					}
					# Apstraadaa gadiijumus, kad iistais morfologjijas elements veel jaapiemeklee.
					elsif ($normToken eq $normW)	#jaapiemeklee iistais morfo elements
					{
						# Plaanajaaa leduu tiek sistemaatiski izlaistas domuziimes un pēdiņas.
						# Juridiskajā korpusā tiek sistemaatiski izlaisti skaitļi un '(ii' no '(ii)'.
						if (not $inputFormAnnot and (
							$normToken eq '-' or $normToken eq '"' or $normToken =~/^\d+$/
							or $normToken =~/^\(|i+$/))
						{
							$doReadNewToken = 1;
						}
						
						# Ja apstraadaajamais tokens ir morfotokena beigaas.
						elsif ($normM =~ /\Q$normToken\E\s*$/ and
							scalar (() = $normM =~ /\Q$normToken\E/g) < 2)
						{
							# Nolasa jauno morfologjiju.
							$morpho = <MORPHO_IN>;
							while (defined($morpho) and $morpho =~ /^\s*$/) {
								print LOG_OUT "bad morpho $morpho";
								$morpho = <MORPHO_IN>;
							}
							$doReadNewToken = 1;
						}
						# Ja morfologjija atbilst x-vaardam.
						elsif ($normM !~ /\Q$normToken\E/)
						{
							# Nolasa jauno morfologjiju.
							$morpho = <MORPHO_IN>;
							while (defined($morpho) and $morpho =~ /^\s*$/) {
								print LOG_OUT "bad morpho $morpho";
								$morpho = <MORPHO_IN>;
							}
							$doReadNewToken = 0;
						}
						# Ja apstraadaajamais tokens ir morfotokena viduu.
						else {
							$doReadNewToken = 1;
							$morpho =~ s/\Q$normToken\E//;
						}
						
						# Izvada rezultaatus.
						if ($doReadNewToken)
						{
							_printMData (\*M_OUT, $docId, $parId, $sentId, $tokId, $wId, $token, 'N/A', 'N/A');
							_printADataSimple (\*A_OUT, $docId, $parId, $sentId, $tokId, 'N/A', $token);
						}
						print LOG_OUT "doesn't match \n";
					}
					else
					{
						# shim notikt nevajadzeetu
						# jaapaarbauda, vai tokenizators darbojaas pareizi
						die "|$token| doesn't match with |$wToken|";
					}
				}
				if (!$doPrintSentId)
				{
					print M_OUT "\t</s>\n";
					_printASentEnd(\*A_OUT);
				}
				$doPrintSentId = 1;
			}
			print M_OUT "</lvmdata>\n";
			print A_OUT "\t</trees>\n</lvadata>\n";
			
			close W_IN;
			close SENT_IN;
			close MORPHO_IN;
			close M_OUT;
			close A_OUT;
			close LOG_OUT;
		}
	}
	#closedir TKDIR;
	closedir W_DIR;

}

# Normalizing quotes and dashes, lowercase.
sub _normalize
{
	my $v = shift;
	$v =~ tr/'\x{2018}\x{2019}\x{201C}\x{201D}\x{201E}\x{00AB}\x{00BB}\x{2013}\x{2014}/""""""""\-\-/;
	return lc $v;
}

# Parsing "pirmā mosfsn pirmā" or "pirmā <mosfsn> pirmā" - lines coming form
# "Plāns ledus" and other older sources.
sub _parsePlainLine
{
	my $mf = shift;
	my @rez;
	$mf =~s/^\s*(.*?)\s*$/$1/s;
	if ($mf =~ /^(.*?) +([a-zA-Z0-9\-_?<>]+) +(.*?)$/)
	{
		@rez = ($1, $2, $3);
		$rez[1] =~ s\^<\\;
		$rez[1] =~ s\>$\\;
	}
	else
	{
		@rez = ($mf);
	}
	return @rez;	
}

# Parsing "Kas <,'','teikuma priekšmets','izteicējs',1,2>" - lines coming form
# Annotator.
sub _parseAnnotatorLine
{
	my $mf = shift;
	$mf =~ /(.+?)\s*			#wordform
			   <\s*
				(\[.*\]|(?:'\s*')?)\s*,	#tag
				\s*('?)(.*?)\3\s*,		#lemma
				\s*('?)(.*?)\5\s*,\s*('?)(.*?)\7\s*(?:,		#some kind of roles
				\s*('?)(\d*)\9\s*)?(?:,\s*('?)(\d*)\11\s*)?		#dependencies
			   >/x;
	my @res = ($1, $2, $4, $6, $8, $10, $12);
	$res[1] =~ s/,//g;
	$res[1] =~ s/^\[(.*)\]$/$1/;

	return @res;
}

sub _printMBegin
{
	my ($output, $docId) = @_;
	my $timeNow = localtime time;
	print $output <<BEIGAS;	
<lvmdata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvmschema.xml" />
		<references>
			<reffile id="w" name="wdata" href="$docId.w" />
		</references>
	</head>
	<meta>
		<lang>lv</lang>
		<annotation_info id="semi-automatic">$progname,  $timeNow</annotation_info>
	</meta>

BEIGAS
}

sub _printMData
{
	my ($output, $docId, $parId, $sentId, $tokId, $wId, $token, $lemma, $tag) = @_;
	$lemma = 'N/A' unless $lemma;
	$tag = 'N/A' unless $tag;	
	print $output <<BEIGAS;
		<m id="m-$docId-p${parId}s${sentId}w$tokId">
			<src.rf>$docId</src.rf>
			<w.rf>w#$wId</w.rf>
			<form>$token</form>
			<lemma>$lemma</lemma>
			<tag>$tag</tag>
		</m>
BEIGAS
}

sub _printABegin
{
	my ($output, $docId) = @_;
	my $timeNow = localtime time;
	print $output <<BEIGAS;
<?xml version="1.0" encoding="utf-8"?>

<lvadata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvaschema.xml" />
		<references>
			<reffile id="m" name="mdata" href="$docId.m" />
			<reffile id="w" name="wdata" href="$docId.w" />
		</references>
	</head>
	<meta>
		<annotation_info>
			<desc>$progname, $timeNow</desc>
		</annotation_info>
	</meta>
	
	<trees>
BEIGAS
}

sub _printASentBegin
{
	my ($output, $docId, $parId, $sentId) = @_;
	print $output <<BEIGAS;

		<LM id="a-${docId}-p${parId}s${sentId}">
			<s.rf>m#m-${docId}-p${parId}s${sentId}</s.rf>
			<children>
				<pmcinfo>
					<pmctype>sent</pmctype>
					<children>
BEIGAS
}

sub _printADataSimple
{
	my ($output, $docId, $parId, $sentId, $tokId, $role, $token) = @_;
	print $output <<BEIGAS;
						<node id="a-${docId}-p${parId}s${sentId}w$tokId">\t<!-- $token -->
							<m.rf>m#m-${docId}-p${parId}s${sentId}w$tokId</m.rf>
							<role>$role</role>
							<ord>$tokId</ord>
						</node>
BEIGAS
}

sub _printASentEnd
{
	my $output = shift;
	print $output <<BEIGAS;
					</children>
				</pmcinfo>
			</children>  
		</LM>
BEIGAS
}

1;
