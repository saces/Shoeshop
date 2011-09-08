package de.saces.fnplugins.Shoeshop;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import de.saces.fnplugins.Shoeshop.requests.AbstractRequest;
import freenet.client.async.BinaryBlob;
import freenet.clients.http.InfoboxNode;
import freenet.clients.http.PageNode;
import freenet.clients.http.RedirectException;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.keys.FreenetURI;
import freenet.l10n.PluginL10n;
import freenet.support.HTMLNode;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.api.HTTPRequest;
import freenet.support.api.HTTPUploadedFile;
import freenet.support.io.BucketTools;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.WebInterfaceToadlet;

public class MainToadlet extends WebInterfaceToadlet {

	private static volatile boolean logDEBUG;

	static {
		Logger.registerClass(MainToadlet.class);
	}

	private final PluginL10n _intl;
	private final static String PARAM_FILENAME = "filename";
	private final static String PARAM_URI = "key";
	private final static String PARAM_IDENTIFIER = "identifier";
	private final static String URI_WIDTH = "70";
	private final static String CMD_SITEEXPORT = "siteexport";
	private final static String CMD_SITEEXPORTEX = "siteexportex";
	private final static String CMD_FILEEXPORT = "fileexport";
	private final static String CMD_BLOBIMPORT = "blobimport";
	private final static String CMD_CANCEL = "cancel";
	private final static String CMD_REMOVE = "remove";
	private final static String CMD_GRAB = "grab";

	private final RequestManager _requestManager;

	protected MainToadlet(PluginContext pluginContext2, PluginL10n intl, RequestManager requestManager) {
		super(pluginContext2, Constants.PLUGIN_URI, "");
		_intl = intl;
		_requestManager = requestManager;
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

	private static class MethodHandlerError extends Exception {
		private static final long serialVersionUID = 1L;
	}

	@SuppressWarnings("unused")
	public void handleMethodPOST(URI uri, HTTPRequest request, final ToadletContext ctx) throws ToadletContextClosedException, IOException, RedirectException, URISyntaxException {
		List<String> errors = new LinkedList<String>();

		if (!isFormPassword(request)) {
			sendErrorPage(ctx, 403, _("HTTP.403.title"), _("Common.InvalidFormpassword"));
			return;
		}

		Bucket result = null;
		try {
			result = innerHandlePost(request, errors);
		} catch (MethodHandlerError e) {
			if (logDEBUG) Logger.debug(this, "MethodHandlerError", e);
		}

		if (result == null) {
			makePage(ctx, errors);
		} else {
			// copy the bucket, the origin data should not be freed here.
			Bucket tmp = ctx.getBucketFactory().makeBucket(result.size());
			BucketTools.copy(result, tmp);
			result = null;
			ctx.sendReplyHeaders(200, "OK", null, BinaryBlob.MIME_TYPE, tmp.size());
			ctx.writeData(tmp);
		}
	}

	private Bucket innerHandlePost(HTTPRequest request, List<String> errors) throws MethodHandlerError {
		if (request.isPartSet(CMD_BLOBIMPORT)) {
			final HTTPUploadedFile file = request.getUploadedFile("filename");
			if (file == null || file.getFilename().trim().length() == 0) {
				errors.add(_("Common.NoFileSelected"));
			} else {
				_requestManager.insertFBlob(file);
			}
			return null;
		}
		if (request.isPartSet(CMD_SITEEXPORT)) {
			FreenetURI uri = checkParamURI(request, errors);
			checkURINonUSK(uri, errors);
			_requestManager.exportSite(uri);
			return null;
		}
		if (request.isPartSet(CMD_FILEEXPORT)) {
			FreenetURI uri = checkParamURI(request, errors);
			_requestManager.exportFile(uri);
			return null;
		}
		if (request.isPartSet(CMD_CANCEL)) {
			String id = checkParamID(request, errors);
			_requestManager.cancelRequest(id);
			return null;
		}
		if (request.isPartSet(CMD_REMOVE)) {
			String id = checkParamID(request, errors);
			_requestManager.removeRequest(id);
			return null;
		}
		if (request.isPartSet(CMD_GRAB)) {
			String id = checkParamID(request, errors);
			return _requestManager.grabData(id);
		}
		errors.add(_("Common.MalformedRequest"));
		return null;
	}

	private void checkURIUSK(FreenetURI uri, List<String> errors) throws MethodHandlerError {
		if (!uri.isUSK()) {
			errors.add(_("Common.URImustbeUSK"));
			throw new MethodHandlerError();
		}
	}

	private void checkURINonUSK(FreenetURI uri, List<String> errors) throws MethodHandlerError {
		if (uri.isUSK()) {
			errors.add(_("Common.USKmustbeSSK", "URI", uri.sskForUSK().toString()));
			throw new MethodHandlerError();
		}
	}

	private FreenetURI checkParamURI(HTTPRequest request, List<String> errors) throws MethodHandlerError {
		if (!request.isPartSet(PARAM_URI)) {
			errors.add(_("Common.MalformedRequest"));
			throw new MethodHandlerError();
		}
		@SuppressWarnings("deprecation")
		String uri = request.getPartAsString(PARAM_URI, 1024);
		FreenetURI furi;
		try {
			furi = new FreenetURI(uri);
		} catch (MalformedURLException e) {
			errors.add(e.getLocalizedMessage());
			throw new MethodHandlerError();
		}
		return furi;
	}

	private String checkParamID(HTTPRequest request, List<String> errors) throws MethodHandlerError {
		if (!request.isPartSet(PARAM_IDENTIFIER)) {
			errors.add(_("Common.MalformedRequest"));
			throw new MethodHandlerError();
		}
		@SuppressWarnings("deprecation")
		String id = request.getPartAsString(PARAM_IDENTIFIER, 1024);
		if (!_requestManager.isValidIdentifier(id)) {
			errors.add(_("Common.NoSuchIdentifier"));
			throw new MethodHandlerError();
		}
		return id;
	}

	private void makePage(ToadletContext ctx, List<String> errors) throws ToadletContextClosedException, IOException {
		PageNode pageNode = pluginContext.pageMaker.getPageNode(_("MainToadlet.title"), ctx);
		HTMLNode outer = pageNode.outer;
		HTMLNode contentNode = pageNode.content;

		if ((errors != null) && (errors.size() > 0)) {
			contentNode.addChild(createErrorBox(errors));
			errors.clear();
		}

		contentNode.addChild(createExportSiteBox());
		contentNode.addChild(createExportHistoryBox());
		contentNode.addChild(createExportSingleBox());
		contentNode.addChild(createImportBox());
		contentNode.addChild(createRequestBox());
		contentNode.addChild(createDonateBox());

		writeHTMLReply(ctx, 200, "OK", outer.generate());
	}

	private InfoboxNode createBox(String title) {
		return pluginContext.pageMaker.getInfobox(title);
	}

	private HTMLNode createExportSiteBox() {
		InfoboxNode box = createBox(_("MainToadlet.ExportSite"));
		HTMLNode outerBox = box.outer;
		HTMLNode boxContent = box.content;
		HTMLNode form = pluginContext.pluginRespirator.addFormChild(boxContent, path(), "uriForm");
		form.addChild("#", _("MainToadlet.SiteURI"));
		form.addChild("#", "\u00a0");
		form.addChild("input", new String[] { "type", "name", "size" }, new String[] { "text", PARAM_URI, URI_WIDTH });
		form.addChild("#", "\u00a0");
		form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_SITEEXPORT, _("Common.Export") });
		return outerBox;
	}

	private HTMLNode createExportHistoryBox() {
		InfoboxNode box = createBox(_("MainToadlet.ExportHistory"));
		HTMLNode outerBox = box.outer;
		HTMLNode boxContent = box.content;
		HTMLNode form = pluginContext.pluginRespirator.addFormChild(boxContent, path(), "uriForm");
		form.addChild("#", _("MainToadlet.SiteURI"));
		form.addChild("#", "\u00a0 ");
		form.addChild("input", new String[] { "type", "name", "size", "disabled" }, new String[] { "text", PARAM_URI, URI_WIDTH, "disabled" });
		form.addChild("#", "\u00a0");
		form.addChild("input", new String[] { "type", "name", "value", "disabled" }, new String[] { "submit", CMD_SITEEXPORTEX, _("Common.Export"), "disabled" });
		return outerBox;
	}

	private HTMLNode createExportSingleBox() {
		InfoboxNode box = createBox(_("MainToadlet.ExportSingle"));
		HTMLNode outerBox = box.outer;
		HTMLNode boxContent = box.content;
		HTMLNode form = pluginContext.pluginRespirator.addFormChild(boxContent, path(), "uriForm");
		form.addChild("#", _("MainToadlet.FileURI"));
		form.addChild("#", "\u00a0 ");
		form.addChild("input", new String[] { "type", "name", "size" }, new String[] { "text", PARAM_URI, URI_WIDTH });
		form.addChild("#", "\u00a0");
		form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_FILEEXPORT, _("Common.Export") });
		return outerBox;
	}

	private HTMLNode createImportBox() {
		InfoboxNode box = createBox(_("MainToadlet.Import"));
		HTMLNode outerBox = box.outer;
		HTMLNode boxContent = box.content;
		HTMLNode form = pluginContext.pluginRespirator.addFormChild(boxContent, path(), "uriForm");
		form.addChild("#", _("MainToadlet.FileName"));
		form.addChild("#", "\u00a0 ");
		form.addChild("input", new String[] { "type", "name", "size" }, new String[] { "file", PARAM_FILENAME, URI_WIDTH });
		form.addChild("#", "\u00a0");
		form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_BLOBIMPORT, _("Common.Import") });
		return outerBox;
	}

	private HTMLNode createRequestBox() {
		InfoboxNode box = createBox(_("MainToadlet.Requests"));
		HTMLNode outerBox = box.outer;
		HTMLNode boxContent = box.content;
		createStatusTables(boxContent);
		return outerBox;
	}

	private HTMLNode createDonateBox() {
		InfoboxNode box = createBox(_("MainToadlet.Donate"));
		HTMLNode outerBox = box.outer;
		HTMLNode boxContent = box.content;
		HTMLNode flattr = new HTMLNode("p");
		HTMLNode flattrlink = flattr.addChild(new HTMLNode("a", "href", "/external-link/?_CHECKED_HTTP_=http://flattr.com/thing/376087/Shoeshop"));
		flattrlink.addChild(new HTMLNode("img", new String[] {"src", "align"}, new String[] {"images/flattr-badge-large.png", "middle"}));
		HTMLNode small = new HTMLNode("small");
		small.addChild("#", "\u00a0http://flattr.com/thing/376087/Shoeshop");
		flattr.addChild(small);
		boxContent.addChild(flattr);

		HTMLNode btc = new HTMLNode("p");
		btc.addChild(new HTMLNode("img", new String[] {"src", "align"}, new String[] {"images/th_Bitcoinorg_100x35_new.png", "middle"}));
		btc.addChild("#", "\u00a01L4QP8hd9ztVXLinabXtdgyetBuRJ8dsCF");
		boxContent.addChild(btc);

		return outerBox;
	}

	void createStatusTables(HTMLNode parent) {
		if (_requestManager.isQueueEmpty()) {
			parent.addChild("#", _("MainToadlet.Requests.EmptyQueue"));
			return;
		}

		HTMLNode table = parent.addChild("table");
		HTMLNode tableHead = table.addChild("thead");
		HTMLNode headRow = tableHead.addChild("tr");
		HTMLNode nextTableCell = headRow.addChild("th");
		nextTableCell.addChild("#", "\u00a0");
		nextTableCell = headRow.addChild("th");
		nextTableCell.addChild("#", _("MainToadlet.RequestTable.Type"));
		nextTableCell = headRow.addChild("th");
		nextTableCell.addChild("#", _("MainToadlet.RequestTable.Status"));
		nextTableCell = headRow.addChild("th");
		nextTableCell.addChild("#", _("MainToadlet.RequestTable.ID"));
		nextTableCell = headRow.addChild("th");
		nextTableCell.addChild("#", "\u00a0");
		nextTableCell = headRow.addChild("th");
		nextTableCell.addChild("#", _("MainToadlet.RequestTable.Progress"));

		for (AbstractRequest<?> req: _requestManager.getRequests()) {
			HTMLNode tableRow = table.addChild("tr");
			tableRow.addChild(makeButtonCell(req));
			tableRow.addChild(makeTypeCell(req));
			tableRow.addChild(makeStatusCell(req));
			tableRow.addChild(makeIDCell(req));
			if (req.isTypeInsert() || !req.isStarted()) {
				tableRow.addChild(makeEmptyCell());
			} else {
				tableRow.addChild(makeGrabCell(req));
			}
			tableRow.addChild(makeProgressCell(req));
		}
	}

	private HTMLNode makeButtonCell(AbstractRequest<?> req) {
		HTMLNode cell = new HTMLNode("td");
		HTMLNode form = pluginContext.pluginRespirator.addFormChild(cell, path(), "uriForm");
		form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "hidden", PARAM_IDENTIFIER, req.getID() });
		if (req.isRunning()) {
			form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_CANCEL, _("Common.Cancel") });
		} else {
			form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_REMOVE, _("Common.Remove") });
		}
		return cell;
	}

	private HTMLNode makeGrabCell(AbstractRequest<?> req) {
		HTMLNode cell = new HTMLNode("td");
		HTMLNode form = pluginContext.pluginRespirator.addFormChild(cell, path(), "uriForm");
		form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "hidden", PARAM_IDENTIFIER, req.getID() });
		form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_GRAB, _("Common.Grab") });
		return cell;
	}

	private HTMLNode makeTypeCell(AbstractRequest<?> req) {
		HTMLNode cell = new HTMLNode("td");
		cell.addChild("#", _("MainToadlet.Requests.Type." + req.getType()));
		return cell;
	}

	private HTMLNode makeStatusCell(AbstractRequest<?> req) {
		HTMLNode cell = new HTMLNode("td");
		cell.addChild("#", _("MainToadlet.Requests.Status." + req.getStatus()));
		return cell;
	}

	private HTMLNode makeIDCell(AbstractRequest<?> req) {
		HTMLNode cell = new HTMLNode("td");
		cell.addChild("#", req.getID());
		return cell;
	}

	private HTMLNode makeProgressCell(AbstractRequest<?> req) {
		HTMLNode cell = new HTMLNode("td");
		if (req.isRunning()) {
			cell.addChild("%", req.getRequestInfo());
		} else if (req.isError()) {
			cell.addChild("#", req.getErrorInfo());
		} else {
			cell.addChild("#", "\\o/");
		}
		return cell;
	}

	private HTMLNode makeEmptyCell() {
		HTMLNode cell = new HTMLNode("td");
		cell.addChild("#", "\u00a0");
		return cell;
	}
}
