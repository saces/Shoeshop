package de.saces.fnplugins.Shoeshop;

import java.util.Collection;
import java.util.HashMap;

import de.saces.fnplugins.Shoeshop.requests.AbstractRequest;
import de.saces.fnplugins.Shoeshop.requests.FileRequest;
import de.saces.fnplugins.Shoeshop.requests.InsertRequest;
import de.saces.fnplugins.Shoeshop.requests.SiteRequest;

import freenet.keys.FreenetURI;
import freenet.l10n.PluginL10n;
import freenet.support.api.Bucket;
import freenet.support.api.HTTPUploadedFile;
import freenet.support.plugins.helpers1.PluginContext;

public class RequestManager {

	@SuppressWarnings("rawtypes")
	private HashMap<String, AbstractRequest> _requests;
	private final PluginContext _pluginContext;
	private final PluginL10n _intl;

	@SuppressWarnings("rawtypes")
	RequestManager(PluginContext pluginContext, PluginL10n intl) {
		_requests = new HashMap<String, AbstractRequest>();
		_pluginContext = pluginContext;
		_intl = intl;
	}

	protected String _(String key) {
		return _intl.getBase().getString(key);
	}

	public void kill() {
		for (AbstractRequest<?> session:_requests.values()) {
			session.kill();
		}
		_requests.clear();
	}

	public boolean isQueueEmpty() {
		return _requests.isEmpty();
	}

	@SuppressWarnings("rawtypes")
	public Collection<AbstractRequest> getRequests() {
		return _requests.values();
	}

	public void insertFBlob(HTTPUploadedFile file) {
		final String id = file.getFilename()+'('+System.currentTimeMillis()+')';
		InsertRequest ir = new InsertRequest(id, _pluginContext);
		_requests.put(id, ir);
		ir.start(file.getData());
	}

	public boolean isValidIdentifier(String id) {
		return _requests.containsKey(id);
	}

	public void cancelRequest(String id) {
		_requests.get(id).kill();
	}

	public void removeRequest(String id) {
		_requests.remove(id);
	}

	public void exportFile(FreenetURI uri) {
		final String id = uri.toShortString()+'('+System.currentTimeMillis()+')';
		FileRequest fr = new FileRequest(id, _pluginContext);
		_requests.put(id, fr);
		fr.start(uri);
	}

	public void exportSite(FreenetURI uri) {
		final String id = uri.toShortString()+'('+System.currentTimeMillis()+')';
		SiteRequest fr = new SiteRequest(id, _pluginContext);
		_requests.put(id, fr);
		fr.start(uri);
	}

	public Bucket grabData(String id) {
		return _requests.get(id).getResult();
	}

}
