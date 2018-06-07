package jdwproxy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public abstract class IRP<T> {
	public int status = 0;
	public T payload;
	private HashSet<IRP> m_dependants;
	private HashSet<IRP> m_subscribers;
	private boolean m_isReady = false;

	public IRP(T payload) {
		this.payload = payload;
	}

	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return Objects.toString(payload);
	}

	private void add_subscriber(IRP irp) {
		if (m_subscribers == null) {
			m_subscribers = new HashSet<>();
		}
		m_subscribers.add(irp);
	}

	public void subscribe(IRP irp) {
		if (m_dependants == null) {
			m_dependants = new HashSet<>();
		}
		m_dependants.add(irp);
		status += 1;
		irp.add_subscriber(this);
	}

	public int alert() throws Exception {
		if (status > 0) {
			status -= 1;
		}
		if (status == 0) {
			onready(); // onready still has a chance to change subscription status at this point
		}
		if (status == 0) {
			m_isReady = true;
			if (m_subscribers != null) {
				for (IRP subscriber : m_subscribers.toArray(new IRP[0])) {
					subscriber.alert();
				}
				m_subscribers.clear();
				m_subscribers = null;
			}
		}
		return status;
	}

	public abstract void onready() throws Exception; // return 0 when succeeded

	public boolean isReady() {
		return m_isReady;
	}

	public IRP[] getDependantsSnapshot() {
		if (m_dependants == null) {
			return null;
		}
		return m_dependants.toArray(new IRP[0]);
	}
}
