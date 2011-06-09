package de.saces.fnplugins.Shoeshop.requests;

import com.db4o.ObjectContainer;


import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.client.events.SimpleEventProducer;
import freenet.keys.FreenetURI;
import freenet.node.RequestStarter;
import freenet.support.api.Bucket;
import freenet.support.plugins.helpers1.PluginContext;

public class InsertRequest extends AbstractRequest implements ClientPutCallback {

	private ClientPutter put;
	private final PluginContext _pluginContext;

	public InsertRequest(String identifier, PluginContext pluginContext) {
		super(identifier);
		_pluginContext = pluginContext;
	}

	@Override
	public void start(Bucket data) {
		InsertContext iCtx = new InsertContext(_pluginContext.hlsc.getInsertContext(true), new SimpleEventProducer());
		iCtx.eventProducer.addEventListener(this);
		iCtx.maxInsertRetries = 2;

		put = new ClientPutter(this, data, FreenetURI.EMPTY_CHK_URI, null, iCtx, RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, false,
				false, this, null, true, _pluginContext.clientCore.clientContext, null);
		try {
			put.start(false, null, _pluginContext.clientCore.clientContext);
		} catch (InsertException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new Exception("TODO").printStackTrace();
		}
	}

	@Override
	public void kill() {
		put.cancel(null, _pluginContext.clientCore.clientContext);
	}

	public void onGeneratedURI(FreenetURI uri, BaseClientPutter state, ObjectContainer container) {
		// ignore
	}

	public void onFetchable(BaseClientPutter state, ObjectContainer container) {
		// ignore
	}

	public void onSuccess(BaseClientPutter state, ObjectContainer container) {
		// TODO Auto-generated method stub
		new Exception("TODO").printStackTrace();
	}

	public void onFailure(InsertException e, BaseClientPutter state,
			ObjectContainer container) {
		// TODO Auto-generated method stub
		System.out.println(e.getLocalizedMessage());
		System.out.println(e.getMode());
		e.printStackTrace();
		new Exception("TODO").printStackTrace();
	}

}
