package edu.bu.easyx10.device;

import edu.bu.easyx10.event.*;
import edu.bu.easyx10.event.X10Event.*;
import edu.bu.easyx10.device.timer.*;
import gnu.io.CommPortIdentifier;

import java.util.*;
import java.sql.Time;
import java.util.Calendar;
import java.util.concurrent.Semaphore;

/**
 * 
 * The X10Motion sensor class is where X10 motion sensor devices 
 * are created. In addition to inheriting from X10Device and 
 * Device, X10MotionSensor contains a WaitTimer, that stores the 
 * number of seconds to wait before deeming a motion 
 * sensor inactive. When the timer reaches zero, it fires an event 
 * that is processed by the X10MotionSensor that sets it�s state 
 * to inactive. It also contains an ApplianceList, so that it 
 * knows what devices to send DeviceEvents to when motion is detected,
 * a StartTime and EndTime to mark the window during which motion 
 * events should be processed and a boolean so that it can be checked 
 * whether a window time has been defined.
 * 
 * @author  Damon Gabrielle
 * @version please refer to subversion
 * @date:   11/06/08
 */
public class X10MotionSensor extends X10Device {

	//Declare Private Member Variables
	protected int mInactivityTime;              // Seconds before Sensor is marked inactive

	private WaitTimer mInactivityTimer;	      // Timer to instantiate with mInactivitTime

	protected Set<String> mApplianceList;       // List of Appliances to send events to

	protected Time mStartTime;                  // Beginning of Motion Detection Period

	protected Time mEndTime;                    // End of motion Detection Period

	protected boolean mDetectionPeriodEnabled;  // If Enabled Motion Detector will only 
	// send Device events between mStartime
	// and mEndTime. 
	// Incoming motion events still
	// can change the Motion sensors state 

	private boolean mDetectionWindowTrigger;   // Simple state variable for Detection Window logic
	private List<String> mDetectionWindowList; // Set of appliances which have been turned on

	private Calendar calendar;                 // system time

	private static Semaphore mListSemaphore;   // Use a semaphore to protect mApplianceList

	/**
	 * Construct for a new X10MotionSensor class.  The constructor is provided
	 * with a ProxyX10MotionSensor object which contains all of the desired 
	 * attributes as defined by the higher level gui.  The constructor must
	 * transfer all the attributes to the local object.  Part of construction
	 * is to always register with the EventGenerator in order to receive inbound 
	 * events.
	 */
	public X10MotionSensor ( ProxyX10MotionSensor proxyDevice ) {

		// Create the super X10Device class and pass to it its attributes
		super (proxyDevice.getName( ),
				proxyDevice.getHouseCode( ),
				proxyDevice.getDeviceCode( ) );

		// create any required member classes
		mApplianceList = new HashSet<String>( );
		mDetectionWindowList = new ArrayList<String>( );

		// create a Mutex using the static semaphore
		mListSemaphore = new Semaphore(1);

		// load our member variables from the ProxyDevice
		setInactivityTime ( proxyDevice.getInactivityTime( ) );
		setDetectionPeriodEnabled ( proxyDevice.getDetectionPeriodEnabled( ) );
		setStartTime ( proxyDevice.getStartTime( ) );
		setEndTime ( proxyDevice.getEndTime( ) );
		setApplianceList ( proxyDevice.getApplianceList( ) );

		// initialize some miscellaneous state variables.
		mDetectionWindowTrigger = false;

		// create a new TimerEvent for the WaitTimer to send
		TimerEvent timerEvent = new TimerEvent ( getName( ), "EXPIRED" );

		// Instantiate the WaitTimer to track inactivity time
		mInactivityTimer = new WaitTimer ( getInactivityTime( ), timerEvent );

		// Setup the calendar for time
		calendar = Calendar.getInstance( );

		//set the initial device state
		mState =  X10DeviceState.OFF;
	}
	
	/**
	 * This implicit constructor is required by the ProxyX10MotionSensor 
	 */
	public X10MotionSensor ( ) {
		
	}

	/*
	 * Defined access methods for all of the member variables.
	 */

	/**
	 * This method loads the Inactivity Time attribute.  The Inactivity Time
	 * attribute defines the number of seconds for which a Motion Sensor will
	 * be in the MOTION state.  
	 * 
	 * @param int seconds Inactivity Time
	 */
	public void setInactivityTime (int seconds) {

		// store the inactivity time in our private member variable
		mInactivityTime = seconds;

		// update the WaitTimer with the new inactivity time
		mInactivityTimer.setSecondsToWait(mInactivityTime);
	}

	/**
	 * This method returns the Inactivity Time attribute.  The Inactivity Time
	 * attribute defines the number of seconds for which a Motion Sensor will
	 * be in the MOTION state.  
	 * 
	 * @return int Inactivity Time
	 */
	public int getInactivityTime( ) {
		return mInactivityTime;
	}

	/**
	 * This method loads the Detection Period Enable attribute.  This attribute
	 * determines if the Motion Sensor is to determine if the current time is
	 * within the Detection Period, which is bound by Start Time and End Time
	 * attributes.  If the time is within the Detection Window and the Detection
	 * Period is Enabled, the Motion Sensor will turn on any Appliances defined
	 * by the Appliance List attribute.
	 * 
	 * @param boolean isEnabled
	 */
	public void setDetectionPeriodEnabled(boolean isEnabled){
		mDetectionPeriodEnabled = isEnabled;
	}

	/**
	 * This method returns the Detection Period Enable attribute.  This attribute
	 * determines if the Motion Sensor is to determine if the current time is
	 * within the Detection Period, which is bound by Start Time and End Time
	 * attributes.  If the time is within the Detection Window and the Detection
	 * Period is Enabled, the Motion Sensor will turn on any Appliances defined
	 * by the Appliance List attribute.
	 * 
	 * @return boolean isEnabled
	 */
	public boolean getDetectionPeriodEnabled(){
		return mDetectionPeriodEnabled;
	}

	/**
	 * This method loads the Start Time attribute.  When the Motion Detection
	 * Period Enable attribute is set TRUE, this attribute defines the start
	 * time for a window in which motion activity for this object can cause
	 * an Appliance object to be turned ON.
	 * 
	 * @param Time startTime Beginning time for Motion Activity Window
	 */
	public void setStartTime(Time startTime) {
		mStartTime = startTime;
	}

	/**
	 * This method returns the Start Time attribute.  When the Motion Detection
	 * Period Enable attribute is set TRUE, this attribute defines the start
	 * time for a window in which motion activity for this object can cause
	 * an Appliance object to be turned ON.
	 * 
	 * @return Time Beginning time for Motion Activity Window
	 */
	public Time getStartTime() {
		return mStartTime;
	}

	/**
	 * This method loads the End Time attribute.  When the Motion Detection
	 * Period Enable attribute is set TRUE, this attribute defines the end
	 * time for a window in which motion activity for this object can cause
	 * an Appliance object to be turned ON.
	 * 
	 * @param Time startTime Beginning time for Motion Activity Window
	 */
	public Time getEndTime() {
		return mEndTime;
	}

	/**
	 * This method returns the End Time attribute.  When the Motion Detection
	 * Period Enable attribute is set TRUE, this attribute defines the end
	 * time for a window in which motion activity for this object can cause
	 * an Appliance object to be turned ON.
	 * 
	 * @return Time Ending time for Motion Activity Window
	 */
	public void setEndTime(Time endTime) {
		mEndTime = endTime;
	}

	/**
	 * This method loads the Appliance List attribute.  The Appliance List
	 * is a Set of Strings where each String identifies a Device Name of
	 * an associated Appliance.  Each associated appliance can be controlled
	 * by this Motion Sensor when motion activity is detected.
	 * 
	 * @param applianceList the mApplianceList to set
	 */
	public void setApplianceList(Set<String> applianceList) {

		// First, let's acquire the Mutex to allow only one updater of the list
		mListSemaphore.acquireUninterruptibly();

		// clear out the old list
		mApplianceList.clear( );

		// All all the new ones
		mApplianceList.addAll ( applianceList );

		// return the Mutex now
		mListSemaphore.release();
	}

	/**
	 * This method loads the Appliance List attribute.  The Appliance List
	 * is a Set of Strings where each String identifies a Device Name of
	 * an associated Appliance.  Each associated appliance can be controlled
	 * by this Motion Sensor when motion activity is detected.
	 * 
	 * @return Set<String> 
	 */
	public Set<String> getApplianceList() {
		return mApplianceList;
	}

	/**
	 * This method is used to change the state of the X10MotionSensor.  The ON state 
	 * is used to convey motion activity, the OFF state is used to convey motion 
	 * inactivity.  When state changes, this method must iterate through the ApplianceList
	 * and alter the state of any X10Appliances when required.
	 * 
	 * @param X10DeviceState state - new state of ON (motion) or OFF
	 */
	public synchronized void setState( X10DeviceState state ) {

		// update our local member variable 
		mState = state;

		/* when ON occurs, we need to check for the Detection Period.  If we
		 * are within our Detection Period, and the Period is Enabled, then we
		 * need to send an ON event to all of the associated appliances.
		 */
		if ( getState( ) == X10DeviceState.ON ) {

			// reset the activity timer
			mInactivityTimer.startTimer( );

			// On transition to MOTION, we need to turn on appliances.
			if (mDetectionWindowTrigger = false) {

				// Check the detection window
				if (getDetectionPeriodEnabled( ) && calendar.after(getStartTime( )) && calendar.before(getEndTime( )) ) {

					// First, let's acquire the Mutex to allow only one updater of the list
					mListSemaphore.acquireUninterruptibly();

					// Iterate through our mApplianceList and turn on all the Appliances now
					Iterator<String> i = mApplianceList.iterator( );
					while (i.hasNext()) {
						String deviceName = i.next();
						// Fetch the appliance reference from the DeviceManager
						X10Appliance appliance = DeviceManager.getDevice ( deviceName );
						appliance.setState ( X10DeviceState.ON );
					}
					/* set a member variable to identify that we have turned ON all
					 * of the associated appliance devices.  Also, we need to remember
					 * which appliance devices we have turned off. 
					 */
					mDetectionWindowTrigger = true;
					mDetectionWindowList.clear( );
					mDetectionWindowList.addAll ( mApplianceList );

					// return the Mutex now
					mListSemaphore.release();
				}
			}
		}

		/*
		 * When OFF occurs, we need to turn off all of the Appliances
		 * defined by ApplianceList, only if we had previously turned
		 * then on with an ON event.
		 */
		if ( (getState( ) == X10DeviceState.OFF) && (mDetectionWindowTrigger == true) ) {
			// clear the state variable which indicates that motion has been triggered
			mDetectionWindowTrigger = false;
			// Iterate through all the devices we previously turned on.
			Iterator<String> i = mDetectionWindowList.iterator( );
			while (i.hasNext()) {
				String deviceName = i.next();
				// Fetch the appliance reference from the DeviceManager
				X10Appliance appliance = DeviceManager.getDevice ( deviceName );
				appliance.setState ( X10DeviceState.OFF );
			}
		}
	}

	/**
	 * This method returns the current state as an X10DeviceState
	 * enumeration.  It has values ON and OFF.
	 * 
	 * @return X10DeviceState ( ON, OFF )
	 */
	public X10DeviceState getState( ) {	
		return mState;
	}

	/** 
	 * Implementation of the toString method.  This method
	 * will return a String representation of the current
	 * state.
	 *
	 * @return String
	 */
	public String toString ( ) {
		String state;
		switch (getState( )) {
		case ON:  state = "ON"; break;
		case OFF: state = "OFF"; break;
		default: state = "UNKNOWN"; break;
		}
		return ( state );
	}

	/**
	 * This method returns a new ProxyX10MotionSensor object
	 * using the attributes from the local object.
	 * 
	 * @return ProxyX10MotionSensor
	 */
	public Device getProxyDevice( ) {

		return new ProxyX10MotionSensor(this);
	}

	/**
	 * This method receives a ProxyX10MotionSensor object.  This 
	 * method updates the local object attributes with the values
	 * from the Proxy object.
	 * 
	 * @param ProxyX10MotionSensor
	 */
	public void updateDevice(Device proxyDevice) {

		/*
		 * We need to crash if we get an update for a device which
		 * is not a ProxyX10MotionSensor.  This means we found a bug.
		 */
		if (!(proxyDevice instanceof ProxyX10MotionSensor) ) {
			assert(false);
		}
		/*
		 * We need to crash if we get an update for a device with a 
		 * different name.  This means we found a bug.
		 */
		if ( proxyDevice.getName( ) != getName( ) ) {
			assert(false);
		}

		// Update the various attributes from the Proxy object
		setHouseCode ( ((ProxyX10MotionSensor)proxyDevice).getHouseCode( ) );
		setDeviceCode ( ((ProxyX10MotionSensor)proxyDevice).getDeviceCode( ) );
		setApplianceList ( ((ProxyX10MotionSensor)proxyDevice).getApplianceList( ) );
		setInactivityTime ( ((ProxyX10MotionSensor)proxyDevice).getInactivityTime( ) );
		setDetectionPeriodEnabled ( ((ProxyX10MotionSensor)proxyDevice).getDetectionPeriodEnabled( ) );
		setStartTime ( ((ProxyX10MotionSensor)proxyDevice).getStartTime( ) );
		setEndTime ( ((ProxyX10MotionSensor)proxyDevice).getEndTime( ) );
	}

	/**
	 * The processDeviceEvent method is used to listen for events from the Protocol
	 * class.  We simply watch for events which have a matching House and Device code
	 * and act upon all MOTION events.
	 * 
	 * @param Event e
	 */
	public void processDeviceEvent(Event e) {

		/* We only deal with X10DeviceEvents with a matching House and Device code and
		 *those events which are ON indicating motion.
		 */
		if (e instanceof X10DeviceEvent &&
				((X10DeviceEvent)e).getHouseCodeChar( ) == getHouseCode( ) &&
				((X10DeviceEvent)e).getDeviceCodeInt( ) == getDeviceCode( ) &&
				((X10DeviceEvent)e).getEventCode( ) == X10_EVENT_CODE.X10_ON ) {
			setState ( X10DeviceState.ON );
		}
	}

	/**
	 * The processTimeEvent method is used to listen for events from the Timer class.
	 * We simply watch for events which match the same device Name.
	 */
	public void processTimerEvent(Event e) {
		/* We only deal with timerEvents attached to this device name.  When
		 * we get a timer event, we set our state to OFF to indicate no motion.
		 */
		if (e instanceof TimerEvent && e.getDeviceName( ) == getName( ) ) {
			setState ( X10DeviceState.OFF);
		}
	}

	/**
	 * The processProtocolEvent is not used by this class.  We simply need
	 * to override it.
	 */
	public void processProtocolEvent(Event e) { 

	}

}
