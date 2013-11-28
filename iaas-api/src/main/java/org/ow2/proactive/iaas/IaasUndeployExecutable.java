/*
 *  *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
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
 *  * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.iaas;

import org.ow2.proactive.scheduler.common.task.TaskResult;

import java.io.Serializable;
import java.util.Map;

/**
 * A simple executable to be used as a task that will stop a running instance on a Iaas.
 * <p/>
 * Executable should be parametrized will all parameters necessary to use the target Iaas API, including parameters required by {@link IaasExecutable}.
 */
public class IaasUndeployExecutable extends IaasExecutable {

    public static final String INSTANCE_ID_KEY = "instanceId";

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        String instanceId;

        instanceId = getInstanceId(args, results);

        createApi(args).stopInstance(new IaasInstance(instanceId));
        return "DONE";
    }

    private String getInstanceId(Map<String, String> args, TaskResult[] results) throws Throwable {
        String instanceId;

        instanceId = args.get(INSTANCE_ID_KEY);
        if (instanceId != null)
            return instanceId;

        if (results.length > 0)
            return (String) results[0].value();

        throw new IllegalArgumentException("InstanceId not provided.");
    }
}
