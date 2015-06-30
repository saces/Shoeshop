package de.saces.fnplugins.Shoeshop.requests;

import java.io.IOException;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.async.BinaryBlobWriter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;
import freenet.support.plugins.helpers1.PluginContext;

public class FileRequest extends AbstractRequest<FreenetURI> implements ClientGetCallback {

	private ClientGetter _get;
	private final PluginContext _pluginContext;
	private Bucket _result;

	public FileRequest(String identifier, PluginContext pluginContext) {
		super(identifier, TYPE.FILE);
		_pluginContext = pluginContext;
	}

	@Override
	public void kill() {
		if (_get != null) {
			_get.cancel(_pluginContext.clientCore.clientContext);
			_get = null;
		}
	}

	@Override
	public void start(FreenetURI uri) {
		FetchContext fCtx = new FetchContext(_pluginContext.hlsc.getFetchContext(), FetchContext.IDENTICAL_MASK, false, null);
		fCtx.eventProducer.addEventListener(this);
		fCtx.maxNonSplitfileRetries = -1;
		fCtx.maxSplitfileBlockRetries = -1;
		try {
			_result = _pluginContext.clientCore.tempBucketFactory.makeBucket(-1);
		} catch (IOException e) {
			Logger.error(this, "Error while creating bucket for resulting data", e);
			setStatusError(e);
			_result = null;
			return;
		}
		_get = new ClientGetter(this, uri, fCtx, RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, null, new BinaryBlobWriter(_result), null);
		try {
			_get.start(_pluginContext.clientCore.clientContext);
			setStatusRunning();
		} catch (FetchException e) {
			setStatusError(e);
			e.printStackTrace();
			new Exception("TODO").printStackTrace();
		}
	}

	@Override
	public void onSuccess(FetchResult result, ClientGetter state) {
		setStatusSuccess();
		_get = null;
	}

	@Override
	public void onFailure(FetchException e, ClientGetter state) {
		setStatusError(e);
		_get = null;
	}

	@Override
	public Bucket getResult() {
		return _result;
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
