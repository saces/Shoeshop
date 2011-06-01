package de.saces.fnplugins.Shoeshop;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import freenet.clients.http.PageNode;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.l10n.PluginL10n;
import freenet.support.HTMLNode;
import freenet.support.api.HTTPRequest;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.WebInterfaceToadlet;

public class MainToadlet extends WebInterfaceToadlet {

	private final PluginL10n _intl;
	private final static String PARAM_URI = "key";
	private final static String URI_WIDTH = "70";
	private final static String CMD_SITEEXPORT = "siteexport";
	private final static String CMD_SITEEXPORTEX = "siteexportex";
	private final static String CMD_FILEEXPORT = "fileexport";
	private final static String CMD_BLOBIMPORT = "blobimport";

	protected MainToadlet(PluginContext pluginContext2, PluginL10n intl) {
		super(pluginContext2, Constants.PLUGIN_URI, "");
		_intl = intl;
	}

	protected String _(String key) {
		return _intl.getBase().getString(key);
	}

	protected String _(String key, String pattern, String value) {
		return _intl.getBase().getString(key, pattern, value);
	}

	public void handleMethodGET(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		if (!req.getPath().toString().equals(path())) {
			sendErrorPage(ctx, 404, _("HTTP.404.title"), _("HTTP.404.description", "URI", uri.toString()));
			return;
		}
		makePage(ctx, null);
	}

	private void makePage(ToadletContext ctx, List<String> errors) throws ToadletContextClosedException, IOException {
		PageNode pageNode = pluginContext.pageMaker.getPageNode(_("MainToadlet.title"), ctx);
		HTMLNode outer = pageNode.outer;
		HTMLNode contentNode = pageNode.content;

		HTMLNode box12 = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadled.ExportSite"), contentNode);
		HTMLNode box12Form = pluginContext.pluginRespirator.addFormChild(box12, path(), "uriForm");
		box12Form.addChild("#", _("MainToadlet.SiteURI"));
		box12Form.addChild("#", "\u00a0");
		box12Form.addChild("input", new String[] { "type", "name", "size" }, new String[] { "text", PARAM_URI, URI_WIDTH });
		box12Form.addChild("#", "\u00a0");
		box12Form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_SITEEXPORT, _("Common.Export") });

		HTMLNode box12a = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadlet.ExportHistory"), contentNode);
		HTMLNode box12aForm = pluginContext.pluginRespirator.addFormChild(box12a, path(), "uriForm");
		box12aForm.addChild("#", _("MainToadlet.SiteURI"));
		box12aForm.addChild("#", "\u00a0 ");
		box12aForm.addChild("input", new String[] { "type", "name", "size" }, new String[] { "text", PARAM_URI, URI_WIDTH });
		box12aForm.addChild("#", "\u00a0");
		box12aForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_SITEEXPORTEX, _("Common.Export") });

		HTMLNode box12b = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadlet.ExportSingle"), contentNode);
		HTMLNode box12bForm = pluginContext.pluginRespirator.addFormChild(box12b, path(), "uriForm");
		box12bForm.addChild("#", _("MainToadlet.FileURI"));
		box12bForm.addChild("#", "\u00a0 ");
		box12bForm.addChild("input", new String[] { "type", "name", "size" }, new String[] { "text", PARAM_URI, URI_WIDTH });
		box12bForm.addChild("#", "\u00a0");
		box12bForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_FILEEXPORT, _("Common.Export") });

		HTMLNode box13 = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadlet.Import"), contentNode);
		HTMLNode box13Form = pluginContext.pluginRespirator.addFormChild(box13, path(), "uriForm");
		box13Form.addChild("#", _("MainToadlet.FileName"));
		box13Form.addChild("#", "\u00a0 ");
		box13Form.addChild("input", new String[] { "type", "name", "size" }, new String[] { "text", PARAM_URI, URI_WIDTH });
		box13Form.addChild("#", "\u00a0");
		box13Form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_BLOBIMPORT, _("Common.Import") });

		HTMLNode flattrBox = pluginContext.pageMaker.getInfobox("infobox-information", "Flattr", contentNode);
		flattrBox.addChild("a", "href", "/?_CHECKED_HTTP_=https://flattr.com/thing/247369/saces-on-Flattr", "Flattr");
		writeHTMLReply(ctx, 200, "OK", outer.generate());
	}
}
