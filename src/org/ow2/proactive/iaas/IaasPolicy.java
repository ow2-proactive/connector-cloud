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

import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.frontend.RMEventListener;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.utils.RMNodeStarter;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.SchedulerState;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.task.CommonAttribute;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.scheduler.resourcemanager.nodesource.policy.SchedulerAwarePolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.objectweb.proactive.extensions.annotation.ActiveObject;

import static org.ow2.proactive.iaas.IaasPolicy.GenericInformation.InstanceType.LARGE;
import static org.ow2.proactive.iaas.IaasPolicy.GenericInformation.InstanceType.MEDIUM;
import static org.ow2.proactive.iaas.IaasPolicy.GenericInformation.InstanceType.SMALL;
import static org.ow2.proactive.iaas.IaasPolicy.GenericInformation.Operation.*;

@ActiveObject
public class IaasPolicy extends SchedulerAwarePolicy implements InitActive, RMEventListener {

    @Configurable(description = "Id of a small sized instance, will be sent to the Iaas API.")
    protected String smallInstanceId = "";
    @Configurable(description = "Id of a medium sized instance, will be sent to the Iaas API.")
    protected String mediumInstanceId = "";
    @Configurable(description = "Id of a large sized instance, will be sent to the Iaas API.")
    protected String largeInstanceId = "";

    private IaasPolicy thisStub;

    public void initActivity(Body body) {
        thisStub = (IaasPolicy) PAActiveObject.getStubOnThis();
    }

    @Override
    public BooleanWrapper configure(Object... params) {
        super.configure(params);
        smallInstanceId = (String) params[4];
        mediumInstanceId = (String) params[5];
        largeInstanceId = (String) params[6];

        return new BooleanWrapper(true);
    }

    @Override
    public void jobSubmittedEvent(JobState job) {
        acquireNodesForJob(job);
        for (TaskState eligibleTask : getEligibleTasks(job).values()) {
            try {
                acquireNodesIfNeeded(job, eligibleTask, eligibleTask.getId().value());
            } catch (Exception e) {
                logger.error("Failed to retrieve job state", e);
            }
        }
    }

    private void acquireNodesForJob(JobState job) {
        if (job == null) {
            return;
        }
        acquireNodesIfNeeded(job, job, job.getId().value());
    }

    private void acquireNodesIfNeeded(JobState job, CommonAttribute target, String id) {
        if (isRequiredNodeSource(target)
                && target.getGenericInformations().containsKey(GenericInformation.TOKEN)
                && isDeployOperation(target.getGenericInformations())) {
            acquireNodes(job, target);
        } else {
            logger.debug("This job or task " + id + " requires another node source, not provisioning nodes.");
        }
    }

    private Map<String, TaskState> getEligibleTasks(JobState jobState) {
        // tasks that can be executed
        Map<String, TaskState> eligibleTasks = new HashMap<String, TaskState>();
        // tasks with FINISHED state. They have terminated with success
        Map<String, TaskState> finishedTasks = new HashMap<String, TaskState>();

        // first we consider all tasks waiting for execution are eligible.
        // then we remove the tasks that have unsatisfied dependencies
        List<TaskState> tasks = jobState.getTasks();
        for (TaskState ts : tasks) {
            switch (ts.getStatus()) {
                case SUBMITTED:
                case PENDING:
                case WAITING_ON_ERROR:
                case WAITING_ON_FAILURE:
                    eligibleTasks.put(ts.getId().value(), ts);
                    break;
                case FINISHED:
                    finishedTasks.put(ts.getId().value(), ts);

                case ABORTED:
                case FAILED:
                case FAULTY:
                case NOT_RESTARTED:
                case NOT_STARTED:
                case SKIPPED:

            }
        }
        // remove from eligible all tasks that have dependencies not contained
        // in finished tasks list
        Iterator<Map.Entry<String, TaskState>> it = eligibleTasks.entrySet()
                .iterator();
        while (it.hasNext()) {
            TaskState ts = it.next().getValue();
            if (ts.getDependences() != null) {
                for (TaskState dep : ts.getDependences()) {
                    if (!finishedTasks.containsKey(dep.getId().value())) {
                        it.remove();
                        break;
                    }
                }
            }
        }
        return eligibleTasks;
    }

    @Override
    public void taskStateUpdatedEvent(NotificationData<TaskInfo> notification) {
        switch (notification.getEventType()) {
            case TASK_RUNNING_TO_FINISHED:
                try {
                    JobState jobState = scheduler.getJobState(notification.getData().getJobId());
                    TaskState taskState = jobState.getHMTasks().get(notification.getData().getTaskId());
                    removeTokenResourcesUsedByTask(taskState, jobState);

                    for (TaskState eligibleTask : getEligibleTasks(jobState).values()) {
                        JobState job = scheduler.getJobState(eligibleTask.getJobId());
                        acquireNodesIfNeeded(job, eligibleTask, eligibleTask.getId().value());
                    }
                } catch (Exception e) {
                    logger.error("Failed to retrieve job state", e);
                }
                break;
        }
    }

    private void removeTokenResourcesUsedByTask(TaskState task, JobState jobOfTask) {
        if ((isRequiredNodeSource(task) || isRequiredNodeSource(jobOfTask))
                && isUndeployOperation(task.getGenericInformations())) {
            // token is mandatory
            logger.debug("Removing nodes used by task " + task.getId());
            removeTokenResources(getTokenFromTaskOrTaskJob(task, jobOfTask));
        }
    }

    private String getTokenFromTaskOrTaskJob(TaskState task, JobState jobOfTask) {
        Map<String, String> taskGenericInformation = task.getGenericInformations();
        return taskGenericInformation.containsKey(GenericInformation.TOKEN)
                ? taskGenericInformation.get(GenericInformation.TOKEN)
                : jobOfTask.getGenericInformations().get(GenericInformation.TOKEN);
    }

    private void removeAllTokenResourcesUsedByTask(TaskState taskState) {
        Map<String, String> genericInformation = taskState.getGenericInformations();
        if (isRequiredNodeSource(taskState)) {
            logger.debug("Removing nodes used by task " + taskState.getId());
            removeTokenResources(genericInformation.get(GenericInformation.TOKEN));
        }
    }

    private boolean isRequiredNodeSource(CommonAttribute job) {
        return nodeSource.getName().equals(job.getGenericInformations().get(GenericInformation.NODE_SOURCE));
    }

    private void acquireNodes(JobState job, CommonAttribute target) {
        if (job == null || target == null) {
            return;
        }

        Map<String, String> nodeConfiguration = new HashMap<String, String>();
        nodeConfiguration.put(GenericInformation.INSTANCE_TYPE, resolveInstanceType(target.getGenericInformations().get(GenericInformation.INSTANCE_TYPE)));
        nodeConfiguration.put(GenericInformation.IMAGE_ID, target.getGenericInformations().get(GenericInformation.IMAGE_ID));
        String jobToken = target.getGenericInformations().get(GenericInformation.TOKEN);
        nodeConfiguration.put(GenericInformation.TOKEN, jobToken);
        int nbOfInstances = Integer.parseInt(target.getGenericInformations().get(GenericInformation.INSTANCE_NB));

        acquireNodes(nbOfInstances, nodeConfiguration);
    }

    @Override
    public void jobStateUpdatedEvent(NotificationData<JobInfo> notification) {
        switch (notification.getEventType()) {
            case JOB_PENDING_TO_FINISHED:
            case JOB_RUNNING_TO_FINISHED:
                try {
                    JobState job = scheduler.getJobState(notification.getData().getJobId());
                    removeTokenResourcesUsedByJob(job);
                    for (TaskState task : job.getTasks()) {
                        removeAllTokenResourcesUsedByTask(task);
                    }
                } catch (Exception e) {
                    logger.error("Failed to retrieve job state", e);
                }
        }
    }

    private void removeTokenResourcesUsedByJob(JobState jobState) {
        Map<String, String> genericInformation = jobState.getGenericInformations();
        if (isRequiredNodeSource(jobState) && genericInformation.containsKey(GenericInformation.TOKEN)) {
            removeTokenResources(genericInformation.get(GenericInformation.TOKEN));
        }
    }

    private void removeTokenResources(String token) {
        logger.debug("Removing nodes using token=" + token);
        removeNodes(token);
    }

    private void removeNodes(String token) {
        List<Node> nodesHavingJobToken = findNodesHavingToken(nodeSource.getAliveNodes(), token);

        for (Node node : nodesHavingJobToken) {
            String nodeURL = node.getNodeInformation().getURL();
            logger.debug("Removing node " + nodeURL);
            removeNode(nodeURL, false);
        }
    }

    private List<Node> findNodesHavingToken(LinkedList<Node> nodes, String token) {
        List<Node> nodesWithToken = new ArrayList<Node>();
        for (Node node : nodes) {
            try {
                if (token.equals(readTokenFromNode(node))) {
                    nodesWithToken.add(node);
                }
            } catch (ProActiveException e) {
                logger.warn("Failed to retrieve property", e);
            }
        }
        return nodesWithToken;
    }

    private String resolveInstanceType(String instanceTypeFromJob) {
        if (SMALL.name().equals(instanceTypeFromJob)) {
            return smallInstanceId;
        } else if (MEDIUM.name().equals(instanceTypeFromJob)) {
            return mediumInstanceId;
        } else if (LARGE.name().equals(instanceTypeFromJob)) {
            return largeInstanceId;
        }
        logger.warn("Instance type unknown, fallback to SMALL instance");
        return smallInstanceId;
    }

    private String readTokenFromNode(Node node) throws ProActiveException {
        return node.getProperty(RMNodeStarter.NODE_ACCESS_TOKEN);
    }

    @Override
    protected SchedulerEvent[] getEventsList() {
        return new SchedulerEvent[]{SchedulerEvent.JOB_RUNNING_TO_FINISHED, SchedulerEvent.JOB_SUBMITTED,
                SchedulerEvent.TASK_RUNNING_TO_FINISHED, SchedulerEvent.JOB_PENDING_TO_FINISHED};
    }

    @Override
    protected SchedulerEventListener getSchedulerListener() {
        return thisStub;
    }

    @Override
    public String getDescription() {
        return "A Iaas policy, aware of the Scheduler and looking for generic information about the kind of instance to start";
    }

    @Override
    public void rmEvent(RMEvent event) {
    }

    @Override
    public void nodeSourceEvent(RMNodeSourceEvent event) {
    }

    @Override
    public void nodeEvent(RMNodeEvent event) {
        switch (event.getEventType()) {
            case NODE_ADDED:
                Node node = filterByNodeUrl(nodeSource.getAliveNodes(), event.getNodeUrl());
                try {
                    String nodeToken = readTokenFromNode(node);
                    if (tokenNotUsedByActiveJobs(nodeToken)) {
                        // node probably registered after job execution
                        logger.warn(String.format("This node %s registered too late, remove it.", node));
                        removeNode(event.getNodeUrl(), false);
                    }
                } catch (ProActiveException e) {
                    logger.warn("Failed to retrieve property", e);
                }
                break;
        }
    }

    private boolean tokenNotUsedByActiveJobs(String token) {
        Vector<JobState> activeJobs = state.getPendingJobs();
        activeJobs.addAll(state.getRunningJobs());

        for (JobState activeJob : activeJobs) {
            if (jobUsesToken(activeJob, token)) {
                return false;
            }
        }
        return true;
    }

    private boolean jobUsesToken(JobState job, String token) {
        if (token.equals(job.getGenericInformations().get(GenericInformation.TOKEN))) {
            return true;
        }
        for (TaskState task : job.getTasks()) {
            if (token.equals(task.getGenericInformations().get(GenericInformation.TOKEN))) {
                return true;
            }
        }
        return false;
    }

    private Node filterByNodeUrl(LinkedList<Node> nodes, String nodeUrl) {
        for (Node node : nodes) {
            if (nodeUrl.equals(node.getNodeInformation().getURL())) {
                return node;
            }
        }
        throw new IllegalStateException("There should always be one node matching");
    }

    void setSchedulerForTests(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    void setSchedulerStateForTests(SchedulerState state) {
        this.state = state;
    }

    public static class GenericInformation {
        public static final String TOKEN = Scheduler.NODE_ACCESS_TOKEN;
        public static final String NODE_SOURCE = "IAAS_NODE_SOURCE";
        public static final String INSTANCE_NB = "IAAS_INSTANCE_NB";
        public static final String IMAGE_ID = "IAAS_IMAGE_ID";
        public static final String INSTANCE_TYPE = "IAAS_INSTANCE_TYPE";
        public static final String OPERATION = "IAAS_OPERATION";

        public enum Operation {
            DEPLOY, UNDEPLOY, DEPLOY_AND_UNDEPLOY;

            public static boolean isDeployOperation(Map<String, String> genericInformation) {
                return DEPLOY.name().equals(genericInformation.get(GenericInformation.OPERATION))
                        || DEPLOY_AND_UNDEPLOY.name().equals(genericInformation.get(OPERATION));
            }

            public static boolean isUndeployOperation(Map<String, String> genericInformation) {
                return UNDEPLOY.name().equals(genericInformation.get(GenericInformation.OPERATION))
                        || DEPLOY_AND_UNDEPLOY.name().equals(genericInformation.get(OPERATION));
            }
        }

        public enum InstanceType {
            SMALL, MEDIUM, LARGE
        }
    }
}
