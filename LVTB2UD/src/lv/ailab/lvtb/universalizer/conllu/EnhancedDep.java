package lv.ailab.lvtb.universalizer.conllu;

import lv.ailab.lvtb.universalizer.utils.Tuple;

import java.util.Objects;

/**
 * Description of one enhanced dependency link - head ID's string
 * representation, role and additional information for sorting.
 *
 * Created on 2017-09-04.
 * @author Lauma
 */
public class EnhancedDep {
	public double sortValue = -1;
	public String headID = null;
	public UDv2Relations role = null;
	public String rolePostfix = null;

	public EnhancedDep(){}
	public EnhancedDep(Token head, UDv2Relations role)
	{
		headID = head.getFirstColumn();
		sortValue = 0.1 * head.idSub + head.idBegin;
		this.role = role;
	}

	public EnhancedDep(Token head, UDv2Relations role, String postfix)
	{
		headID = head.getFirstColumn();
		sortValue = 0.1 * head.idSub + head.idBegin;
		this.role = role;
		rolePostfix = postfix == null ? null : postfix.trim();
	}
	public EnhancedDep(Token head, Tuple<UDv2Relations, String> role)
	{
		headID = head.getFirstColumn();
		sortValue = 0.1 * head.idSub + head.idBegin;
		this.role = role.first;
		rolePostfix = role.second;
	}

	public boolean isRootDep()
	{
		return ((headID == null || headID.equals("0")) &&  role == UDv2Relations.ROOT);
	}

	public static EnhancedDep root()
	{
		EnhancedDep res = new EnhancedDep();
		res.sortValue = 0;
		res.headID = "0";
		res.role = UDv2Relations.ROOT;
		res.rolePostfix = null;
		return res;
	}

	public String toConllU()
	{
		if (rolePostfix != null && !rolePostfix.isEmpty())
			return headID + ":" + role.strRep + ":" + rolePostfix;
		return headID + ":" + role.strRep;
	}

	public Tuple<UDv2Relations, String> getRoleTuple()
	{
		return Tuple.of(role, rolePostfix);
	}

	@Override
	public boolean equals (Object o)
	{
		if (o == null) return false;
		if (this.getClass() != o.getClass()) return false;
		if (this == o) return true;
		EnhancedDep other = (EnhancedDep) o;
		return (sortValue == other.sortValue &&
				(Objects.equals(headID, other.headID)) &&
				(Objects.equals(rolePostfix, other.rolePostfix)) &&
				role == other.role);
	}

	@Override
	public int hashCode()
	{
		return 1777 * Double.hashCode(sortValue) +
				977 *(headID == null ? 1 : headID.hashCode()) +
				7* (role == null ? 1 : role.hashCode());
	}

	public String toString()
	{
		return "head ID: " + headID + ", full role: " + role + ":" + rolePostfix + ", sort value: " + sortValue;
	}
}
