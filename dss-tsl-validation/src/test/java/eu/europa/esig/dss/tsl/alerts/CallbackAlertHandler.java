package eu.europa.esig.dss.tsl.alerts;

class CallbackAlertHandler<T> implements AlertHandler<T> {
	
	private boolean called = false;

	@Override
	public void alert(T currentInfo) {
		called = true;
	}

	public boolean isCalled() {
		return called;
	}
	
}
