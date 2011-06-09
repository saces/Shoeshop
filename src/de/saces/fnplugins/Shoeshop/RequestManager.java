package de.saces.fnplugins.Shoeshop;

import java.util.Collection;
import java.util.HashMap;

import de.saces.fnplugins.Shoeshop.requests.AbstractRequest;
import de.saces.fnplugins.Shoeshop.requests.InsertRequest;

import freenet.l10n.PluginL10n;
import freenet.support.HTMLNode;
import freenet.support.api.HTTPUploadedFile;
import freenet.support.plugins.helpers1.PluginContext;

public class RequestManager {

	private HashMap<String, AbstractRequest> _requests;
	private final PluginContext _pluginContext;
	private final PluginL10n _intl;

	RequestManager(PluginContext pluginContext, PluginL10n intl) {
		_requests = new HashMap<String, AbstractRequest>();
		_pluginContext = pluginContext;
		_intl = intl;
	}

	protected String _(String key) {
		return _intl.getBase().getString(key);
	}

	public void kill() {
		for (AbstractRequest session:_requests.values()) {
			session.kill();
		}
		_requests.clear();
	}

	public boolean isQueueEmpty() {
		return _requests.isEmpty();
	}

	public Collection<AbstractRequest> getRequests() {
		return _requests.values();
	}

	public void insertFBlob(HTTPUploadedFile file) {
		final String id = "↑ ["+file.getFilename()+"] ("+System.currentTimeMillis()+')';
		InsertRequest ir = new InsertRequest(id, _pluginContext);
		_requests.put(id, ir);
		ir.start(file.getData());
	}

}
