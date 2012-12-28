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
package org.ow2.proactive.iaas.cloudstack;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasVM;
import org.ow2.proactive.scheduler.common.task.TaskResult;

import java.io.Serializable;

public class CloudstackDeployExecutable extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        CloudStackAPI api = (CloudStackAPI) createApi(args);

        IaasVM vm = api.startVm(args);

        waitUntilVMRunning(api, vm);
        api.attachVolume(vm.getVmId(), "cf48db13-a1d9-4c2a-9df7-22a88b7ab885");
        api.reboot(vm.getVmId());
        Thread.sleep(30000); // TODO wait until reboot order is ack
        waitUntilVMRunning(api, vm);

        return vm.getVmId();
    }

    private void waitUntilVMRunning(CloudStackAPI api, IaasVM vm) throws Exception {
        while (true) {
            if (api.isVmStarted(vm.getVmId())) {
                break;
            }
            Thread.sleep(10000);
        }
    }
}
