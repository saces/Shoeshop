package de.saces.fnplugins.Shoeshop.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.Metadata;
import freenet.client.async.BinaryBlobWriter;
import freenet.client.async.BinaryBlobWriter.BinaryBlobAlreadyClosedException;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.SnoopMetadata;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.support.HTMLEncoder;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;
import freenet.support.plugins.helpers1.PluginContext;

public class SiteRequest extends AbstractRequest<FreenetURI> implements ClientGetCallback {

	private static volatile boolean logDEBUG;

	static {
		Logger.registerClass(SiteRequest.class);
	}

	private static class MetaSnoop implements SnoopMetadata {

		Metadata _meta;

		@Override
		public boolean snoopMetadata(Metadata meta, ClientContext context) {
			if (meta.isSimpleManifest()) {
				_meta = meta;
				return true;
			}
			return false;
		}
		
	}

	private class SubFileRequest extends AbstractRequest<FreenetURI> implements ClientGetCallback {

		private ClientGetter __get;

		public SubFileRequest(String identifier) {
			super(identifier, TYPE.FILE);
		}

		@Override
		public void kill() {
			if (__get != null) {
				__get.cancel(_pluginContext.clientCore.clientContext);
				__get = null;
			}
		}

		@Override
		public void start(FreenetURI uri) {
			FetchContext fCtx = new FetchContext(_pluginContext.hlsc.getFetchContext(), FetchContext.IDENTICAL_MASK, false, null);
			fCtx.eventProducer.addEventListener(this);
			fCtx.maxNonSplitfileRetries = -1;
			fCtx.maxSplitfileBlockRetries = -1;
			__get = new ClientGetter(this, uri, fCtx, RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, null, _result, true, null);
			try {
				__get.start(_pluginContext.clientCore.clientContext);
				setStatusRunning();
			} catch (FetchException e) {
				setStatusError(e);
				e.printStackTrace();
				new Exception("TODO").printStackTrace();
			}
		}

		@Override
		public Bucket getResult() {
			return null;
		}

		private void removeMe() {
			boolean result = _requests.remove(this);
			if (!result) new Exception("Was Not in list!").printStackTrace();
		}

		@Override
		public void onSuccess(FetchResult result, ClientGetter state) {
			__get = null;
			removeMe();
			trySetStatusSuccess();
		}

		@Override
		public void onFailure(FetchException e, ClientGetter state) {
			e.printStackTrace();
			if (e.mode == FetchException.FetchExceptionMode.TOO_MANY_PATH_COMPONENTS) {
				System.out.println("Retry: "+e.newURI.toString());
			}
			__get = null;
			removeMe();
		}

		@Override
		public String getRequestInfo() {
			if (_lastProgress == null) {
				return "";
			}
			return _lastProgress.getDescription();
		}

		@Override
		public void onResume(ClientContext context)
				throws ResumeFailedException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public RequestClient getRequestClient() {
			return this;
		}
	}

	byte a = 0x0A;
	byte b = 0x0B;
	int c = (a << 8) | b;
	private Vector<SubFileRequest> _requests;
	private ClientGetter _rootGetter;
	private final PluginContext _pluginContext;
	private BinaryBlobWriter _result;
	private MetaSnoop _ms;
	private boolean _killed = false;
	FreenetURI _uri;

	public SiteRequest(String identifier, PluginContext pluginContext) {
		super(identifier, TYPE.SITE);
		_pluginContext = pluginContext;
		_requests = new Vector<SubFileRequest>();
	}

	@Override
	public synchronized void kill() {
		_killed = true;
		_rootGetter.cancel(_pluginContext.clientCore.clientContext);
		for (SubFileRequest req : _requests) {
			req.kill();
		}
		//_requests.clear();
	}

	@Override
	public void start(FreenetURI uri) {
		_uri = uri;
		FetchContext fCtx = new FetchContext(_pluginContext.hlsc.getFetchContext(), FetchContext.IDENTICAL_MASK, false, null);
		fCtx.eventProducer.addEventListener(this);
		fCtx.maxNonSplitfileRetries = -1;
		fCtx.maxSplitfileBlockRetries = -1;
		_result = new BinaryBlobWriter(_pluginContext.clientCore.tempBucketFactory);
		_rootGetter = new ClientGetter(this, uri, fCtx, RequestStarter.BULK_SPLITFILE_PRIORITY_CLASS, null, _result, true, null);
		_ms = new MetaSnoop();
		_rootGetter.setMetaSnoop(_ms);
		try {
			_rootGetter.start(_pluginContext.clientCore.clientContext);
			setStatusRunning();
		} catch (FetchException e) {
			setStatusError(e);
			e.printStackTrace();
			new Exception("TODO").printStackTrace();
		}
	}

	@Override
	public Bucket getResult() {
		if (_result.isFinalized())
			return _result.getFinalBucket();
		Bucket b;
		try {
			b = _pluginContext.clientCore.tempBucketFactory.makeBucket(-1);
			_result.getSnapshot(b);
			return b;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BinaryBlobAlreadyClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public void onSuccess(FetchResult result, ClientGetter state) {
		throw new UnsupportedOperationException();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFailure(FetchException e, ClientGetter state) {
		if (e.mode == FetchException.FetchExceptionMode.CANCELLED && !_killed) {
			if (_ms._meta != null) {
				parseMetadata(_ms._meta.getDocuments(), "/", _uri.setMetaString(null));
				trySetStatusSuccess();
				return;
			}
		}
		setStatusError(e);
		_rootGetter = null;
	}

	private void trySetStatusSuccess() {
		if (_requests.size() == 0) {
			setStatusSuccess();
		}
	}

	private void addFileRequest(String id, FreenetURI uri) {
		SubFileRequest sfr = new SubFileRequest(id);
		_requests.add(sfr);
		//System.out.println("URI: "+uri.toString(false, false));
		sfr.start(uri);
	}

	private void parseMetadata(HashMap<String, Metadata> docs, String prefix, FreenetURI uri) {
		for (Entry<String, Metadata> entry : docs.entrySet()) {
			String name = entry.getKey();
			Metadata md = entry.getValue();
			if (md.isSimpleManifest()) {
				parseMetadata(md.getDocuments(), prefix + name + '/', uri.pushMetaString(name));
				continue;
			}

			final String tempName = prefix + name;

			if (md.isArchiveInternalRedirect()) {
				// its in the same container, ignore
				if (logDEBUG) Logger.debug(this, "Item '"+tempName + "' is in same container, ignore.");
				continue;
			}
			if (md.isSimpleRedirect()) {
				if (logDEBUG) Logger.debug(this, "Item '"+tempName + "' is simple redirect, add FileRequest for it.");
				//System.out.println("Name: "+name);
				addFileRequest(tempName, uri.pushMetaString(name));
				continue;
			}
			System.out.println("Shoeshop: Unknown/unhandled item: "+tempName);

//			FetchContext context = _hlsc.getFetchContext();
//			ProgressMonitor pm = new ProgressMonitor();
//			context.eventProducer.addEventListener(pm);
//			_statusByName.put(tempName, pm);
//			ClientGetter get = new ClientGetter(this, uri.pushMetaString(name), context, RequestStarter.INTERACTIVE_PRIORITY_CLASS, (RequestClient)_hlsc, null, null);
//			_getter2nameMap.put(get, tempName);
//			itemsTotal++;
//			itemsLeft++;
//			try {
//				get.start(null, _clientContext);
//			} catch (FetchException e) {
//				onFailure(e, get, null);
//			}
		}
	}

	@Override
	public String getRequestInfo() {
		if (_requests.size() == 0) {
			// still searching/fetching the root manifest/container
			return super.getRequestInfo();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Running subrequests: "+_requests.size());
		for (SubFileRequest req : _requests) {
			sb.append("<br />");
			HTMLEncoder.encodeToBuffer(req.getRequestInfo(), sb);
		}
		return sb.toString();
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
