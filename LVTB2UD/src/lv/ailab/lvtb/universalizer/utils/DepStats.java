package lv.ailab.lvtb.universalizer.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class DepStats {

	HashMap<LocalTreeConfig, HashSet<String>> data = new HashMap<>();

	public void add(String parentRole, String parentTag, String childRole, String childTag, String childId)
	{
		LocalTreeConfig config = new LocalTreeConfig(childRole, childTag, parentRole, parentTag);
		add (config, childId);
	}

	public void add(LocalTreeConfig config, String childId)
	{
		HashSet<String> ids = data.get(config);
		if (ids == null) ids = new HashSet<>();
		ids.add(childId);
		data.put(config, ids);
	}

	public static class LocalTreeConfig implements Comparable<DepStats.LocalTreeConfig>
	{
		public String childRole;
		public String childTag;
		public String parentRole;
		public String parentTag;

		public LocalTreeConfig(String childRole, String childTag, String parentRole, String parentTag) {
			this.childRole = childRole;
			this.childTag = childTag;
			this.parentRole = parentRole;
			this.parentTag = parentTag;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 17 * hash + Objects.hashCode(this.childRole);
			hash = 17 * hash + Objects.hashCode(this.childTag);
			hash = 17 * hash + Objects.hashCode(this.parentRole);
			hash = 17 * hash + Objects.hashCode(this.parentTag);
			return hash;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == null) return false;

			if (getClass() != o.getClass()) return false;

			final LocalTreeConfig other = (LocalTreeConfig) o;
			if (!Objects.equals(this.childRole, other.childRole)) return false;
			if (!Objects.equals(this.parentRole, other.parentRole)) return false;
			if (!Objects.equals(this.childTag, other.childTag)) return false;
			return Objects.equals(this.parentTag, other.parentTag);
		}

		@Override
		public int compareTo(LocalTreeConfig o) {
			if (!Objects.equals(this.childRole, o.childRole))
				return Objects.compare(this.childRole, o.childRole, Comparator.naturalOrder());
			if (!Objects.equals(this.parentRole, o.parentRole))
				return Objects.compare(this.parentRole, o.parentRole, Comparator.naturalOrder());
			if (!Objects.equals(this.childTag, o.childTag))
				return Objects.compare(this.childTag, o.childTag, Comparator.naturalOrder());
			return Objects.compare(this.parentTag, o.parentTag, Comparator.naturalOrder());

		}

	}
}
