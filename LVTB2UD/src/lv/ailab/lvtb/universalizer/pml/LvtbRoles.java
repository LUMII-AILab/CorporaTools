package lv.ailab.lvtb.universalizer.pml;

/**
 * Possible values for PML node/role field (incomplete).
 * Created on 2016-04-20.
 *
 * @author Lauma
 */
public class LvtbRoles
{
	// phrase parts
	public final static String PRED = "pred";
	public final static String MOD = "mod";
	public final static String AUXVERB = "auxVerb";
	public final static String BASELEM = "basElem";
	public final static String CRDPART = "crdPart";
	public final static String CONJ = "conj";
	public final static String PUNCT = "punct";
	public final static String NO = "no";
	public final static String PREP = "prep";

	// simple dependencies
	public final static String SUBJ = "subj";
	public final static String OBJ = "obj";
	public final static String SPC = "spc";
	public final static String ATTR = "attr";
	public final static String ADV = "adv";
	public final static String SIT = "sit";
	public final static String DET = "det";

	// clausal dependencies
	public final static String PREDCL = "predCl";
	public final static String SUBJCL = "subjCl";
	public final static String OBJCL = "objCl";
	public final static String ATTRCL = "attrCl";
	public final static String APPCL = "appCl";

	public final static String PLACECL = "placeCl";
	public final static String TIMECL = "timeCl";
	public final static String MANCL = "manCl";
	public final static String DEGCL = "degCl";
	public final static String CAUSCL = "causCl";
	public final static String PURPCL = "purpCl";
	public final static String CONDCL = "condCl";
	public final static String CNSECCL = "cnsecCl";
	public final static String CNCESCL = "cncesCl";
	public final static String MOTIVCL = "motivCl";
	public final static String COMPCL = "compCl";
	public final static String QUASICL = "quasiCl";

	// semi-clausal dependencies
	public final static String INS = "ins";
	public final static String DIRSP = "dirSp";

	// other
	public final static String REPEAT = "repeat";
	public final static String ELLIPSIS_TOKEN = "ellipsisTok";
}
