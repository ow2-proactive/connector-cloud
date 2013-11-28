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

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.IaasMonitoringApi;

import static org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceApiLoader.USE_API_FLAG;
import static org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceSigarLoader.USE_SIGAR_FLAG;


public class IaasMonitoringServiceFactory {

    /**
     * Flags in the options.
     */
    public static final String SKIP_CACHE_FLAG = "skipCache";

    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaasMonitoringServiceFactory.class);

    private IaasMonitoringServiceFactory() {
    }

    public static IaasMonitoringChainable getMonitoringService(IaasMonitoringApi api, String nsname,
            String options) throws IaasMonitoringException {
        IaasMonitoringChainable loader = null;

        if (Utils.isPresentInParameters(SKIP_CACHE_FLAG, options)) {
            if (Utils.isPresentInParameters(USE_API_FLAG, options)) {
                loader = (new IaasMonitoringServiceApiLoader(api));
                logger.debug("[" + nsname + "] Created ApiLoader (no cache).");
            } else if (Utils.isPresentInParameters(USE_SIGAR_FLAG, options)) {
                loader = (new IaasMonitoringServiceSigarLoader());
                logger.debug("[" + nsname + "] Created SigarLoader (no cache).");
            }
        } else {
            if (Utils.isPresentInParameters(USE_API_FLAG, options)) {
                loader = new IaasMonitoringServiceCacher(new IaasMonitoringServiceApiLoader(api));
                logger.debug("[" + nsname + "] Created cacher with ApiLoader.");
            } else if (Utils.isPresentInParameters(USE_SIGAR_FLAG, options)) {
                loader = new IaasMonitoringServiceCacher(new IaasMonitoringServiceSigarLoader());
                logger.debug("[" + nsname + "] Created cacher with SigarLoader.");
            }

        }

        if (loader == null) {
            throw new IaasMonitoringException("Monitoring configuration not valid: " + options);
        }

        loader.configure(nsname, options);

        return loader;
    }
}
