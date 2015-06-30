package de.saces.fnplugins.Shoeshop.requests;

import de.saces.fnplugins.Shoeshop.L10nableError;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.client.events.SimpleEventProducer;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.ResumeFailedException;
import freenet.support.plugins.helpers1.PluginContext;

public class InsertRequest extends AbstractRequest<Bucket> implements ClientPutCallback {

	private ClientPutter _put;
	private final PluginContext _pluginContext;

	public InsertRequest(String identifier, PluginContext pluginContext) {
		super(identifier, TYPE.INSERT);
		_pluginContext = pluginContext;
	}

	@Override
	public void start(Bucket data) {
		InsertContext iCtx = new InsertContext(_pluginContext.hlsc.getInsertContext(true), new SimpleEventProducer());
		iCtx.eventProducer.addEventListener(this);
		iCtx.maxInsertRetries = 2;

		_put = new ClientPutter(this, (RandomAccessBucket) data, FreenetURI.EMPTY_CHK_URI, null, iCtx, RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, false,
				null, false, _pluginContext.clientCore.clientContext, null, 0);
		try {
			_put.start(false, _pluginContext.clientCore.clientContext);
			setStatusRunning();
		} catch (InsertException e) {
			setStatusError(e);
			// TODO
			e.printStackTrace();
			new Exception("TODO").printStackTrace();
		}
	}

	@Override
	public void kill() {
		if (_put != null) {
			_put.cancel(_pluginContext.clientCore.clientContext);
			_put = null;
		}
	}

	@Override
	public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
		// ignore
	}

	@Override
	public void onFetchable(BaseClientPutter state) {
		// ignore
	}

	@Override
	public void onSuccess(BaseClientPutter state) {
		setStatusSuccess();
		_put = null;
	}

	@Override
	public void onFailure(InsertException e, BaseClientPutter state) {
		if (e.getMode() == InsertException.InsertExceptionMode.BINARY_BLOB_FORMAT_ERROR) {
			setStatusError(new L10nableError("Errors.NoFblobFile"));
		} else {
			//System.out.println("ErrorCode: "+e.getMode());
			//e.printStackTrace();
			setStatusError(e);
		}
		_put = null;
	}

	@Override
	public Bucket getResult() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onResume(ClientContext context) throws ResumeFailedException {
		// TODO Auto-generated method stub
	}

	@Override
	public RequestClient getRequestClient() {
		return this;
	}

}
