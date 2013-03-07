package org.ow2.proactive.iaas.eucalyptus;



import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasApiFactory;
import org.ow2.proactive.iaas.IaasInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.util.log.ProActiveLogger;

import com.xerox.amazonws.ec2.AvailabilityZone;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;


/**
 *
 * Eucalyptus Connector: requires the proper eucalyptus credentials
 *
 * @author The ProActive Team
 *
 */


public class EucalyptusConnector implements java.io.Serializable, IaasApi {

	/** logger */
    protected static Logger logger = ProActiveLogger.getLogger(EucalyptusConnector.class);

	/** Access Key */
	private String EUC_AKEY;

	/** Secret Key */
	private String EUC_SKEY;

	/** User Id */
	private String EUC_USER;

	/** KeyPair name */
    private String keyName;

    /**
     * Once an image descriptor is retrieved, cache it
     */
    private Map<String, ImageDescription> cachedImageDescriptors = Collections
            .synchronizedMap(new HashMap<String, ImageDescription>());


	/**
	 * Eucalyptus server URL - needed to connect to the server
	 */
	private String eucaHost = null;

	/**
	 * Eucalyptus server Port
	 * */

	private int eucaPort;


	/**
	 * Typica object reference
	 * */

	private Jec2 EC2Request = null;



	/**
	 * Constructor of the connector
	 */
	public EucalyptusConnector(){

	}

	/**
	 * Constructor of the connector with the credentials
	 *
	 * @param euc_accesskey
	 * 			Eucalyptus access key
	 *
	 * @param euc_secretkey
	 * 			Eucalyptus secret key
	 *
	 * @param euc_user
	 * 			Eucalyptus user id
	 *
	 *
	 * @param eucaHost
	 * 			Eucalyptus service url
	 *
	 *
	 * @param eucaPort
	 * 			Eucalyptus service port
	 *
	 *
	 */
	public EucalyptusConnector(String eucaAccesskey, String eucaSecretkey, String eucaUser, String eucaHost, int eucaPort){
		this();
		setHost(eucaHost, eucaPort);
		resetKeys(eucaAccesskey, eucaSecretkey, eucaUser);
	}

    public EucalyptusConnector(Map<String, String> args) {
        this(
                args.get("eucaAccesskey"),
                args.get("eucaSecretkey"),
                args.get("eucaUser"),
                args.get("eucaHost"),
                Integer.parseInt(args.get("eucaPort"))
        );
    }

    /**
	 * @return Returns the Eucalyputs host URL
	 *
	 * */


	public String getHost(){
		return this.eucaHost;
	}


	/**
	 * @return Returns the Eucalyputs host port
	 *
	 * */


	public int getPort(){
		return this.eucaPort;
	}


	/**
	 * @param host
	 * 			Eucalyptus host URL
	 *
	 * @param port
	 * 			Eucalyptus host port
	 *
	 * */

	public void setHost(String host, int port){
		this.eucaHost = host;
		this.eucaPort = port;

	}


	/**
	 *
	 * Reset Eucalyptus keys
	 *
	 *
	 * @param euc_accesskey
	 * 			Accesskey to access Eucalyptus
	 *
	 * @param euc_secretkey
	 * 			Secret key to access Eucalyptus
	 *
	 * @param euc_user
	 * 			Username
	 *
	 *
	 * @return Returns a Jec2 Typica object that can be used to perform the operations on the amazon cloud.
	 *
	 *
	 */
	public Jec2 resetKeys(String eucaAccesskey, String eucaSecretkey, String eucaUser){  //FIXME: I don't think it's a good idea to return this internal object to the caller of the method.
		Jec2 EucaRequester;

		this.EUC_AKEY = eucaAccesskey;
		this.EUC_SKEY = eucaSecretkey;
		this.EUC_USER = eucaUser;

		//EucaRequester = new Jec2(this.EUC_AKEY, this.EUC_SKEY);

	    EucaRequester = new Jec2(this.EUC_AKEY, this.EUC_SKEY, false, this.eucaHost, this.eucaPort);
        EucaRequester.setResourcePrefix("/services/Eucalyptus"); //TODO: see if this is the same for every eucalyptus server or if we need to provide a method to set it.
        EucaRequester.setSignatureVersion(1); //TODO: check if signature value is required to be 1 (setting to 1 is the most likely use case)

        this.EC2Request = EucaRequester;
		return EucaRequester;
	}


	  /** Returns a list of availability zones and their status.
	   *
	   * @param zones
	   * 			a list of zones to limit the results, or null
	   * @return a list of zones and their availability
	   *
	   * @throws Proactive exceptions
	   **/

	public List<AvailabilityZone> describeAvailabilityZones(List<String> zones) throws ProActiveException{
		try {
			System.out.println(EC2Request.getUrl());
			return EC2Request.describeAvailabilityZones(zones);

		} catch (EC2Exception e) {
			logger.error("Unable to get the availability zones", e);
			throw new ProActiveException("Unable to get the availability zones");
		}
	}

	 /**
	   * Describe the available EMIs.
	   *
	   * @param all if true all the images will be returned, if false just the ones for the current user.
	   *
	   * @return A list of {@link ImageDescription} instances describing each EMI ID.
	   *
	   * @throws EC2Exception wraps checked exceptions
	   */

	public List<ImageDescription> getAllAvailableImages(boolean all) throws ProActiveException{

		List<String> params = new ArrayList<String>();

		List<ImageDescription> images = null;

		if (!all)
			params.add(EUC_USER);

		try{
			images = this.EC2Request.describeImagesByOwner(params);
		}
		catch(EC2Exception e){
			logger.error("Unable to get image description", e);
			throw new ProActiveException("Unable to get image description.");
		}

		return images;
	}

	/**
	 * Retrieves the first image with emiId
	 *
	 * @param amiId
	 * 				a unique EMI id
	 *
	 * @param all
	 * 				if true Get all EMI, if false, get only user's EMI
	 *
	 * @return First EMI from Eucalyptus corresponding to pattern
	 *
	 * @throws ProActiveException
	 */
	public ImageDescription getAvailableImage(String emiId, boolean all) throws ProActiveException {

		synchronized (cachedImageDescriptors) {
			if (cachedImageDescriptors.containsKey(emiId))
				return cachedImageDescriptors.get(emiId);
		}

		// get all the images available
		List<ImageDescription> imgs = this.getAllAvailableImages(all);

		// find the image with id emiId from the list of available images
		if (imgs != null) {

			for (ImageDescription img : imgs) {
				if (img.getImageId().equals(emiId)) {
					// cache it
					cachedImageDescriptors.put(emiId, img);
					return img;
				}
			}
		}

		return null;
	}

	/**
    * Gets a set of running instances
    *
    * @return a set of instances
    *
    * @throws ProActiveException if the list of instances cannot be retrieved.
    */
   public List<Instance> getInstances() throws ProActiveException {

       List<String> params = new ArrayList<String>();
       List<ReservationDescription> resInstances = null;
       List<Instance> instances = new ArrayList<Instance>();

       try {
           resInstances = this.EC2Request.describeInstances(params);
       } catch (EC2Exception e) {
           logger.error("Unable to get instances list", e);
           throw new ProActiveException("Unable to get instances list. " + e);
       }

       for (ReservationDescription rdesc : resInstances) {
           instances.addAll(rdesc.getInstances());
       }

       return instances;
   }



   /**
    *
    * @param instanceId
    * 			The id of the instance
    *
    * @return the instance with id "instanceId"
    *
    * @throws ProActiveException
    *
    */

   public Instance getInstanceWithId(String instanceId) throws ProActiveException {

	   try {
		   for (ReservationDescription desc : EC2Request.describeInstances(new String[] {})) {
			   for (Instance inst : desc.getInstances()) {
				   if (instanceId.equals(inst.getInstanceId())){
					   return inst;
				   }
			   }
		   }
	   } catch (EC2Exception e){
		   throw new ProActiveException("Instance with id: " + instanceId + "does not exist.");
	   }

	   return null;
   }

   /**
    * Returns the hostname of a running instance
    * If the instance is not running, will return an empty string
    *
    * @param id the unique id of the instance
    * @return the hostname of the running instance corresponding to the id,
    *         or an empty string
 * @throws ProActiveException
    */
   public String getInstanceHostname(String instanceId) throws ProActiveException {

	   Instance inst = getInstanceWithId(instanceId);

	   if (inst != null)
		   return inst.getDnsName();

       return "";
   }


   /**
    * Runs an instance
    *
    * @param imageId
    * 			Id of the image to run
    *
    * @param minInst
    * 			minimal number of instances to deploy
    *
    * @param maxInst
    * 			maximal number of instances to deploy
    *
    * @return the list of instances deployed
    */
	public List<Instance> start(String imageId, int minInst, int maxInst)
			throws ProActiveException{

		try{
			//check if this image "imageId" exists
			if (getAvailableImage(imageId, true) != null){
				ReservationDescription rdesc = EC2Request.runInstances(imageId, minInst, maxInst, new ArrayList<String>(), this.EUC_USER, this.keyName);
				return rdesc.getInstances();
			}
			return null;
		} catch (EC2Exception e) {
			throw new ProActiveException("Could not run an instance with image id " + imageId + " " + e);
		}
	}


	/**
	 * Terminate an instance
	 *
	 * @param instanceId
	 * 			Id of the instance to terminate
	 *
	 * @return true upon success, or false otherwise
	 * @throws ProActiveException
	 */
	public boolean stop(String instanceId) throws ProActiveException {

		try {
			EC2Request.terminateInstances(new String[] {instanceId});
			return true;
		}catch(EC2Exception e) {
			logger.error("Failed to terminate instance: " + instanceId, e);
			throw new ProActiveException("Failed to terminate instance " + instanceId + " " + e);
		}

		//return false;
	}

	/**
	 * Terminate all instances
	 *
	 * @param nothing
	 *
	 * @return true upon terminating all instances
	 * @throws ProActiveException
	 */
	public boolean stopAll() throws ProActiveException{
		try{
			for (ReservationDescription desc : EC2Request.describeInstances(new String[] {})) {
	               for (Instance inst : desc.getInstances()) {
	                   this.stop(inst.getInstanceId());
	               }
	         }
			return true;
		}catch(EC2Exception e) {
			logger.error("Failed to terminate all instance: ", e);
			return false;
		}

	}


	/**
	 * Restart a given instance
	 *
	 * @param istanceId
	 * 			Id of the instance to restart
	 *
	 * @return true if the restarted, false otherwise
	 *
	 * */



	public boolean restart(String instanceId)throws ProActiveException{
		List<String> toReboot = new LinkedList<String>();
		toReboot.add(instanceId);
		try {
			EC2Request.rebootInstances(toReboot);
		} catch (EC2Exception e) {
			throw new ProActiveException("Failed to reboot the give instance: "+ instanceId);

		}
		return true;


	}


    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
        return new IaasInstance(start(arguments.get("imageId"), 1, 1).get(0).getInstanceId());
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        stop(instance.getInstanceId());
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getName() {
        return IaasApiFactory.IaasProvider.EUCALYPTUS.name();
    }
}
