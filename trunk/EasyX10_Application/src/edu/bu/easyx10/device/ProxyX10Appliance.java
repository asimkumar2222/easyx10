package edu.bu.easyx10.device;

//import java.util.*;
//import java.sql.Time;

/**
 * ProxyX10Appliance is derived from and identical to X10Appliance except 
 * that it overrides the X10Appliance constructor such that they don�t 
 * instantiate TriggerTimer OnTime and OffTime objects.  Instead, 
 * ProxyX10Appliancejust sets two Time object attributes with the same 
 * names; OnTime on OffTime. ProxyX10Appliance objects are instantiated
 * in the GUI and are passed off to DeviceManager. DeviceManager then
 * creates X10Appliance Objects by passing a proxy object into the constructor. 
 * ProxyX10Appliance does not contain any of it�s own methods; only overridden 
 * ones.
 * 
 * @author  Damon Gabrielle
 * @version please refer to subversion
 * @date:   11/06/08
 */

public class ProxyX10Appliance extends X10Appliance{
	
	/**
	 * Constructor
	 * 
	 * Construct for a new ProxyX10Applaince class.  The constructor is provided
	 * with a X10MAppliance object which contains all of the desired 
	 * attributes as defined by the higher level GUI.  The constructor must
	 * transfer all the attributes to the local object. The only real difference
	 * between an X10Appliance object and a ProxyX10Appliance is that
	 * Proxy objects do not send or receive events or instantiate timer objects.
	 */

	public ProxyX10Appliance ( X10Appliance appliance ){
		
		// Create the super X10Device class and pass to it its attributes
		super(  appliance.getName( ),
				appliance.getHouseCode( ),
				appliance.getDeviceCode( ) );
		
		
	}
	
}