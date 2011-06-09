package de.saces.fnplugins.Shoeshop.requests;

import com.db4o.ObjectContainer;

import freenet.client.async.ClientContext;
import freenet.client.events.ClientEvent;
import freenet.client.events.ClientEventListener;
import freenet.client.events.SplitfileProgressEvent;
import freenet.node.RequestClient;
import freenet.support.api.Bucket;

public abstract class AbstractRequest implements ClientEventListener, RequestClient {

	protected SplitfileProgressEvent lastProgress;
	public abstract void kill();
	private final String _identifier;
	private Exception _lastError;

	private STATUS status = STATUS.NONE;
	private final TYPE _type;

	AbstractRequest(String identifier, TYPE type) {
		_identifier = identifier;
		_type = type;
	}

	public enum STATUS { NONE, RUNNING, DONE, ERROR };
	public enum TYPE { FILE, SITE, INSERT };

	public abstract void start(Bucket data);

	public void onRemoveEventProducer(ObjectContainer container) {
		new Exception("TODO").printStackTrace();
	}

	public void receive(ClientEvent ce, ObjectContainer maybeContainer, ClientContext context) {
		if (ce instanceof SplitfileProgressEvent) {
			lastProgress = (SplitfileProgressEvent) ce;
			return;
		}
		new Exception("TODO: "+ce.getDescription()).printStackTrace();
	}

	public boolean persistent() {
		return false;
	}

	public void removeFrom(ObjectContainer container) {
		throw new UnsupportedOperationException();
	}

	public boolean realTimeFlag() {
		return true;
	}

	public void onMajorProgress(@SuppressWarnings("unused") ObjectContainer container) {
		// ignore
	}

	public String getRequestInfo() {
		StringBuilder sb = new StringBuilder();
		if (lastProgress != null)
			sb.append(lastProgress.getDescription());
		return sb.toString();
	}

	void setStatusRunning() {
		status = STATUS.RUNNING;
	}

	void setStatusError(Exception e) {
		_lastError = e;
		status = STATUS.ERROR;
	}

	void setStatusSuccess() {
		status = STATUS.DONE;
	}

	public String getStatus() {
		return status.name();
	}

	public boolean isRunning() {
		return status == STATUS.RUNNING;
	}

	public String getID() {
		return _identifier;
	}

	public String getType() {
		return _type.name();
	}

	public String getErrorInfo() {
		return _lastError.getLocalizedMessage();
	}

	public boolean isError() {
		return status == STATUS.ERROR;
	}
}
