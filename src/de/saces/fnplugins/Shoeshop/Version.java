package de.saces.fnplugins.Shoeshop;

/**
 * @author saces
 *
 */
public class Version {

	/** SVN revision number. Only set if the plugin is compiled properly e.g. by emu. */
	private static final String gitRevision = "@custom@";

	/** Version number of the plugin for getRealVersion(). Increment this on making
	 * a major change, a significant bugfix etc. These numbers are used in auto-update
	 * etc, at a minimum any build inserted into auto-update should have a unique
	 * version.
	 */
	private static final long version = 0;

	private static final String longVersionString = "0.0.0α " + gitRevision;

	public static String getVersion() {
		return longVersionString;
	}

	public static long getRealVersion() {
		return version;
	}

}
