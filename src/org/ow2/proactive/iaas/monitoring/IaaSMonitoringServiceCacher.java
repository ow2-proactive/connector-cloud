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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.iaas.utils.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


public class IaaSMonitoringServiceCacher extends IaaSMonitoringServiceLoader {

    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaaSMonitoringServiceCacher.class);

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
    private int refreshPeriod = 60;

    /**
     * Expiration time for caches [seconds]. After this time the stale 
     * entry will expire and refresh will no longer take place.
     */
    private int expirationTime = 60 * 5;

    /**
     * Flag to refresh cache periodically. 
     */
    private boolean autoUpdate = false;

    /**
     * Flag that indicates that this monitoring system should shutdown.
     */
    private boolean stop = false;

    public IaaSMonitoringServiceCacher(IaaSMonitoringApi iaaSMonitoringApi)
            throws IaaSMonitoringServiceException {
        super(iaaSMonitoringApi);

    }

    public void configure(String nsName, String options) {
        super.configure(nsName, options);

        String refreshPeriodStr = Utils.getValueFromParameters("refreshPeriodSeconds", options);
        if (refreshPeriodStr != null) {
            try {
                refreshPeriod = Integer.parseInt(refreshPeriodStr);
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

        logger.debug(String.format(
                "Monitoring params: refreshPeriod='%s' expirationTime='%s' autoUpdateCache='%b'",
                refreshPeriod, expirationTime, autoUpdate));

        createCaches();

    }

    private void createCaches() {
        hostPropertiesCache = CacheBuilder.newBuilder().maximumSize(1000)
                .expireAfterAccess(expirationTime, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Map<String, String>>() {
                    public Map<String, String> load(String key) throws Exception {
                        return getHostPropertiesLoad(key);
                    }
                });

        vmPropertiesCache = CacheBuilder.newBuilder().maximumSize(1000)
                .expireAfterAccess(expirationTime, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Map<String, String>>() {
                    public Map<String, String> load(String key) throws Exception {
                        return getVMPropertiesLoad(key);
                    }
                });

        if (autoUpdate) {
            Runnable updater = new Runnable() {
                public void run() {
                    while (stop == false) {
                        for (String key : hostPropertiesCache.asMap().keySet()) {
                            hostPropertiesCache.refresh(key);
                        }
                        for (String key : vmPropertiesCache.asMap().keySet()) {
                            vmPropertiesCache.refresh(key);
                        }
                        try {
                            Thread.sleep(1000 * refreshPeriod);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            };

            Thread updaterThread = new Thread(updater, "IaaSMonitoringUpdater-" + nsname);
            updaterThread.start();
        }

    }

    @Override
    public String[] getHosts() throws IaaSMonitoringServiceException {
        return super.getHosts();
    }

    @Override
    public String[] getVMs() throws IaaSMonitoringServiceException {
        return super.getVMs();
    }

    @Override
    public String[] getVMs(String hostId) throws IaaSMonitoringServiceException {
        return super.getVMs(hostId);
    }

    public Map<String, String> getHostPropertiesLoad(final String hostId)
            throws IaaSMonitoringServiceException {
        logger.debug("API: getting host properties: " + hostId);
        return super.getHostProperties(hostId);
    }

    public Map<String, String> getVMPropertiesLoad(final String vmId) throws IaaSMonitoringServiceException {
        logger.debug("API: getting VM properties: " + vmId);
        return super.getVMProperties(vmId);
    }

    @Override
    public Map<String, String> getHostProperties(final String hostId) throws IaaSMonitoringServiceException {
        try {
            return hostPropertiesCache.get(hostId);
        } catch (ExecutionException e) {
            throw new IaaSMonitoringServiceException(e);
        }
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaaSMonitoringServiceException {
        try {
            return vmPropertiesCache.get(vmId);
        } catch (ExecutionException e) {
            throw new IaaSMonitoringServiceException(e);
        }
    }

    public void shutDown() {
        super.shutDown();
        stop = true;
    }
}
