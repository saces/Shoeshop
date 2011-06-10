package de.saces.fnplugins.Shoeshop;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import de.saces.fnplugins.Shoeshop.requests.AbstractRequest;
import freenet.client.async.BinaryBlob;
import freenet.clients.http.PageNode;
import freenet.clients.http.RedirectException;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.keys.FreenetURI;
import freenet.l10n.PluginL10n;
import freenet.support.HTMLNode;
import freenet.support.api.Bucket;
import freenet.support.api.HTTPRequest;
import freenet.support.api.HTTPUploadedFile;
import freenet.support.io.BucketTools;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.WebInterfaceToadlet;

public class MainToadlet extends WebInterfaceToadlet {

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
			// ignore
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

		HTMLNode box12 = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadled.ExportSite"), contentNode);
		HTMLNode box12Form = pluginContext.pluginRespirator.addFormChild(box12, path(), "uriForm");
		box12Form.addChild("#", _("MainToadlet.SiteURI"));
		box12Form.addChild("#", "\u00a0");
		box12Form.addChild("input", new String[] { "type", "name", "size", "disabled" }, new String[] { "text", PARAM_URI, URI_WIDTH, "disabled" });
		box12Form.addChild("#", "\u00a0");
		box12Form.addChild("input", new String[] { "type", "name", "value", "disabled" }, new String[] { "submit", CMD_SITEEXPORT, _("Common.Export"), "disabled" });

		HTMLNode box12a = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadlet.ExportHistory"), contentNode);
		HTMLNode box12aForm = pluginContext.pluginRespirator.addFormChild(box12a, path(), "uriForm");
		box12aForm.addChild("#", _("MainToadlet.SiteURI"));
		box12aForm.addChild("#", "\u00a0 ");
		box12aForm.addChild("input", new String[] { "type", "name", "size", "disabled" }, new String[] { "text", PARAM_URI, URI_WIDTH, "disabled" });
		box12aForm.addChild("#", "\u00a0");
		box12aForm.addChild("input", new String[] { "type", "name", "value", "disabled" }, new String[] { "submit", CMD_SITEEXPORTEX, _("Common.Export"), "disabled" });

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
		box13Form.addChild("input", new String[] { "type", "name", "size" }, new String[] { "file", PARAM_FILENAME, URI_WIDTH });
		box13Form.addChild("#", "\u00a0");
		box13Form.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", CMD_BLOBIMPORT, _("Common.Import") });

		HTMLNode requestBox = pluginContext.pageMaker.getInfobox("infobox-information", _("MainToadlet.Requests"), contentNode);
		createStatusTables(requestBox);

		HTMLNode flattrBox = pluginContext.pageMaker.getInfobox("infobox-information", "Flattr", contentNode);
		flattrBox.addChild("a", "href", "/?_CHECKED_HTTP_=https://flattr.com/thing/247369/saces-on-Flattr", "Flattr");
		writeHTMLReply(ctx, 200, "OK", outer.generate());
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
			if (req.isTypeInsert() || !req.isDone()) {
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
			cell.addChild("#", req.getRequestInfo());
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
