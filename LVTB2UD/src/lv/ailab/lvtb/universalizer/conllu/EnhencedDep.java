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

	public EnhencedDep(){};
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

	@Override
	public boolean equals (Object o)
	{
		if (o == null) return false;
		if (this.getClass() != o.getClass()) return false;
		if (this == o) return true;
		EnhencedDep other = (EnhencedDep) o;
		return (sortValue == other.sortValue &&
				(headID == other.headID || headID != null && headID.equals(other.headID)) &&
				role == other.role);
	}

	@Override
	public int hashCode()
	{
		return 1777 * Double.hashCode(sortValue) +
				977 *(headID == null ? 1 : headID.hashCode()) +
				7* (role == null ? 1 : role.hashCode());
	}
}
