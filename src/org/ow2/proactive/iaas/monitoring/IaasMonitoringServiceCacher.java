/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * %$ACTIVEEON_INITIAL_DEV$
 */

package org.ow2.proactive.iaas.monitoring;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import java.util.concurrent.TimeUnit;
import org.ow2.proactive.iaas.utils.Utils;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import org.ow2.proactive.iaas.IaasMonitoringApi;


public class IaasMonitoringServiceCacher implements IaasMonitoringApi, IaasNodesListener  {
    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaasMonitoringServiceCacher.class);

    /**
     * Cache for properties of hosts.
     */
    private LoadingCache<String, Map<String, String>> hostPropertiesCache;

    /**
     * Cache for properties of VMs.
     */
    private LoadingCache<String, Map<String, String>> vmPropertiesCache;

    /**
     * Refresh period for caches [seconds]. 
     */
    private int refreshPeriod = 60 * 2;

    /**
     * Maximum amount of cache entries. 
     */
    private int maximumCacheEntries = 200;

    /**
     * Expiration time for caches [seconds]. If a property of a VM or a host 
     * cannot be obtained, after this time the entry in the cache will be
     * removed.
     */
    private int expirationTime = 60 * 10;

    /**
     * Flag to refresh cache periodically. 
     */
    private boolean autoUpdate = false;

    /**
     * Loader of values. This loader will directly contact the API.
     */
    private IaasMonitoringServiceLoader loader;
    
    /**
     * Timer to execute regularly the refresh task. 
     */
    private Timer timer;
    
    /**
     * Name of the Node Source being monitored.
     */
    protected String nsname;

    public IaasMonitoringServiceCacher(IaasMonitoringServiceLoader loader)
            throws IaasMonitoringException {
        
        this.loader = loader;

    }

    public void configure(String nsName, String options) {
        
        this.nsname = nsName;
        
        loader.configure(nsName, options);
        

        String refreshPeriodStr = Utils.getValueFromParameters("refreshPeriodSeconds", options);
        if (refreshPeriodStr != null) {
            try {
                refreshPeriod = Integer.parseInt(refreshPeriodStr);
            } catch (NumberFormatException e) {
                // Ignore it, let the default value be. 
            }
        }

        String maximumCacheEntriesStr = Utils.getValueFromParameters("maximumCacheEntries", options);
        if (maximumCacheEntriesStr != null) {
            try {
                maximumCacheEntries = Integer.parseInt(maximumCacheEntriesStr);
            } catch (NumberFormatException e) {
                // Ignore it, let the default value be. 
            }
        }

        String expirationTimeStr = Utils.getValueFromParameters("expirationTimeSeconds", options);
        if (expirationTimeStr != null) {
            try {
                expirationTime = Integer.parseInt(expirationTimeStr);
            } catch (NumberFormatException e) {
                // Ignore it, let the default value be. 
            }
        }

        autoUpdate = Utils.isPresentInParameters("autoUpdateCache", options);

        logger.debug(String
                .format("[" + nsname + "] " +
                    "Monitoring params: refreshPeriod='%d' maximumCacheEntries='%d' expirationTime='%d' autoUpdateCache='%b'",
                        refreshPeriod, maximumCacheEntries, expirationTime, autoUpdate));

        createCaches();

    }

    private void createCaches() {
        hostPropertiesCache = CacheBuilder.newBuilder().maximumSize(maximumCacheEntries)
                .expireAfterWrite(expirationTime, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Map<String, String>>() {
                    public Map<String, String> load(String key) throws Exception {
                        return getHostPropertiesLoad(key);
                    }

                });

        vmPropertiesCache = CacheBuilder.newBuilder().maximumSize(maximumCacheEntries)
                .expireAfterWrite(expirationTime, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Map<String, String>>() {
                    public Map<String, String> load(String key) throws Exception {
                        return getVMPropertiesLoad(key);
                    }
                });

        if (autoUpdate) {
            TimerTask timertask = new TimerTask() {

                @Override
                public void run() {
                    logger.debug("[" + nsname + "] Monitoring info refresh started.");

                    try {
                        String[] hosts = getHosts();
                        for (String key : hosts) {
                            hostPropertiesCache.refresh(key);
                        }
                    } catch (Exception e) {
                        logger.warn("[" + nsname + "] Could not reload cache of host properties.", e);
                    }
                    logger.debug("[" + nsname + "] Entries in host cache: " + hostPropertiesCache.size());

                    try {
                        String[] vms = getVMs();
                        for (String key : vms) {
                            vmPropertiesCache.refresh(key);
                        }
                    } catch (Exception e) {
                        logger.warn("[" + nsname + "] Could not reload cache of VM properties.", e);
                    }
                    logger.debug("[" + nsname + "] Entries in VM cache: " + vmPropertiesCache.size());
                }
            };

            timer = new Timer();
            timer.scheduleAtFixedRate(timertask, 0, 1000 * refreshPeriod);

        }

    }

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        // Not cached.
        return loader.getHosts();
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        // Not cached.
        return loader.getVMs();
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        // Not cached.
        return loader.getVMs(hostId);
    }

    public Map<String, String> getHostPropertiesLoad(final String hostId)
            throws IaasMonitoringException {
        logger.debug("[" + nsname + "] " + "Loader, loading host properties: " + hostId);
        return loader.getHostProperties(hostId);
    }

    public Map<String, String> getVMPropertiesLoad(final String vmId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "] " + "Loader, loading VM properties: " + vmId);
        return loader.getVMProperties(vmId);
    }

    @Override
    public Map<String, String> getHostProperties(final String hostId) throws IaasMonitoringException {
        try {
            return hostPropertiesCache.get(hostId);
        } catch (ExecutionException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        try {
            return vmPropertiesCache.get(vmId);
        } catch (ExecutionException e) {
            throw new IaasMonitoringException(e);
        }
    }

    public void shutDown() {
        loader.shutDown();
        if (timer != null)
            timer.cancel();
    }

    @Override
    public void registerNode(String nodeid, String jmxurl, NodeType type) {
        registerNode(nodeid, jmxurl, type);
    }

    @Override
    public void unregisterNode(String nodeid, NodeType type) {
        unregisterNode(nodeid, type);
    }

    @Override
    public Map<String, Object> getVendorDetails() throws Exception {
        // Not cached.
        return loader.getVendorDetails();
    }
}
