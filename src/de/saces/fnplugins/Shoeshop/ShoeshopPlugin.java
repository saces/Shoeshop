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
	private RequestManager requestManager;

	@Override
	public String getString(String key) {
		return intl.getBase().getString(key);
	}

	@Override
	public void setLanguage(LANGUAGE newLanguage) {
		if (intl == null) {
			intl = new PluginL10n(this, newLanguage);
			return;
		}
		intl.getBase().setLanguage(newLanguage);
	}

	@Override
	public long getRealVersion() {
		return Version.getRealVersion();
	}

	@Override
	public String getVersion() {
		return Version.getVersion();
	}

	@Override
	public String getL10nFilesBasePath() {
		return Constants.L10N_BASEPATH;
	}

	@Override
	public String getL10nFilesMask() {
		return Constants.L10N_FILEMASK;
	}

	@Override
	public String getL10nOverrideFilesMask() {
		return Constants.L10N_OVERRIDEFILEMASK;
	}

	@Override
	public ClassLoader getPluginClassLoader() {
		return this.getClass().getClassLoader();
	}

	@Override
	public void terminate() {
		requestManager.kill();
		requestManager = null;
		webInterface.kill();
		webInterface = null;
		pluginContext = null;
		intl = null;
	}

	@Override
	public void runPlugin(PluginRespirator pr) {
		if (intl == null) {
			intl = new PluginL10n(this);
		}

		L10nableError.setL10n(intl.getBase());

		pluginContext = new PluginContext(pr);
		requestManager = new RequestManager(pluginContext, intl);
		webInterface = new WebInterface(pluginContext);

		webInterface.addNavigationCategory(Constants.PLUGIN_URI+"/", Constants.PLUGIN_CATEGORY, Constants.PLUGIN_CATEGORYTOOLTIP, this);

		MainToadlet mainToadlet = new MainToadlet(pluginContext, intl, requestManager);
		webInterface.registerVisible(mainToadlet, Constants.PLUGIN_CATEGORY, "Menu.Shoeshop.title", "Menu.Shoeshop.tooltip");

		// Invisible pages
		StaticToadlet cssToadlet = new StaticToadlet(pluginContext, Constants.PLUGIN_URI, "css", "/data/css/", "text/css", intl);
		webInterface.registerInvisible(cssToadlet);
		StaticToadlet picToadlet = new StaticToadlet(pluginContext, Constants.PLUGIN_URI, "images", "/data/images/", "image/png", intl);
		webInterface.registerInvisible(picToadlet);

	}

}
