package lv.ailab.lvtb.universalizer.conllu;

/**
 * Description of one enhanced dependency link - head ID's string
 * representation, role and aditional information for sorting.
 *
 * Created on 2017-09-04.
 * @author Lauma
 */
public class EnhencedDep {
	public double sortValue = -1;
	public String headID = null;
	public UDv2Relations role = null;

	EnhencedDep(){};
	public EnhencedDep (Token head, UDv2Relations role)
	{
		headID = head.getFirstColumn();
		sortValue = 0.1 * head.idSub + head.idBegin;
		this.role = role;
	}

	public static EnhencedDep root()
	{
		EnhencedDep res = new EnhencedDep();
		res.sortValue = 0;
		res.headID = "0";
		res.role = UDv2Relations.ROOT;
		return res;
	}

	public String toConllU()
	{
		return headID + ":" + role.strRep;
	}
}
