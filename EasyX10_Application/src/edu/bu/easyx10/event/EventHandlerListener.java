package edu.bu.easyx10.event;

import java.util.*;

/**
 * The EventHandlerListener class is an implementation which
 * provides the receive methods for Events which are delivered
 * from the EventGenerator class.
 *
 * @author:  Jim Duda
 * @version: refer to EasyX10 subversion
 * @date:    10/30/08
 *
 */
public interface EventHandlerListener extends EventListener {
	public void processDeviceEvent( X10DeviceEvent e );
	public void processProtocolEvent( X10ProtocolEvent e );
	public void processTimerEvent( TimerEvent e );
}
