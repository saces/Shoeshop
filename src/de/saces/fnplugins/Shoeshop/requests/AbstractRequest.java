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

	AbstractRequest(String identifier) {
		_identifier = identifier;
	}

	enum STATUS { IDLE, STARTING, RUNNING, DONE, ERROR };

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
		// TODO
		StringBuilder sb = new StringBuilder();
		sb.append(_identifier);
		sb.append("\nInsert: ");
		if (lastProgress != null)
			sb.append(lastProgress.getDescription());
		return sb.toString();
	}


}
