package de.saces.fnplugins.Shoeshop;

import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.l10n.PluginL10n;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginBaseL10n;
import freenet.pluginmanager.FredPluginL10n;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.WebInterface;

public class ShoeshopPlugin implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginThreadless, FredPluginL10n, FredPluginBaseL10n {

	private PluginL10n intl;
	private WebInterface webInterface;
	private PluginContext pluginContext;

	public String getString(String key) {
		return intl.getBase().getString(key);
	}

	public void setLanguage(LANGUAGE newLanguage) {
		if (intl == null) {
			intl = new PluginL10n(this, newLanguage);
			return;
		}
		intl.getBase().setLanguage(newLanguage);
	}

	public long getRealVersion() {
		return Version.getRealVersion();
	}

	public String getVersion() {
		return Version.getVersion();
	}

	public String getL10nFilesBasePath() {
		return Constants.L10N_BASEPATH;
	}

	public String getL10nFilesMask() {
		return Constants.L10N_FILEMASK;
	}

	public String getL10nOverrideFilesMask() {
		return Constants.L10N_OVERRIDEFILEMASK;
	}

	public ClassLoader getPluginClassLoader() {
		return this.getClass().getClassLoader();
	}

	public void terminate() {
		webInterface.kill();
		webInterface = null;
		pluginContext = null;
		intl = null;
	}

	public void runPlugin(PluginRespirator pr) {
		if (intl == null) {
			intl = new PluginL10n(this);
		}

		pluginContext = new PluginContext(pr);
		webInterface = new WebInterface(pluginContext);
		webInterface.addNavigationCategory(Constants.PLUGIN_URI+"/", Constants.PLUGIN_CATEGORY, Constants.PLUGIN_CATEGORYTOOLTIP, this);

		MainToadlet mainToadlet = new MainToadlet(pluginContext, intl);
		webInterface.registerVisible(mainToadlet, Constants.PLUGIN_CATEGORY, "Menu.Shoeshop.title", "Menu.Shoeshop.tooltip");

	}

}
