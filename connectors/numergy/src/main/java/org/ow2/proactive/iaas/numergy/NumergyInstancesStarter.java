package org.ow2.proactive.iaas.numergy;

import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class NumergyInstancesStarter {

    private static final Logger logger = Logger.getLogger(NumergyInstancesStarter.class);
    private static final long START_SERVERS_PERIOD = TimeUnit.MINUTES.toMillis(1);

    private NumergyAPI api;
    protected ConcurrentLinkedQueue<StartItem> unstartedServers;
    private Timer timer;

    public NumergyInstancesStarter(NumergyAPI api) {
        this.api = api;
        this.unstartedServers = new ConcurrentLinkedQueue();
        this.timer = initializeServerStarter();
    }

    public void addServerToWatchAndStart(String serverId, JSONObject metadata) {
        unstartedServers.add(new StartItem(serverId, metadata));
    }

    private Timer initializeServerStarter() {
        Timer timer = new Timer("timer-instances-starter");
        timer.schedule(new Starter(), 0, START_SERVERS_PERIOD);
        return timer;
    }

    public void shutdown() {
        timer.cancel();
    }

    static class StartItem {
        String instanceId;
        JSONObject metadata;

        StartItem(String instanceId, JSONObject metadata) {
            this.instanceId = instanceId;
            this.metadata = metadata;
        }
    }


    class Starter extends TimerTask {
        @Override
        public void run() {
            Iterator<StartItem> it = unstartedServers.iterator();

            logger.debug(unstartedServers.size() + " pending VMs to start...");

            while (it.hasNext()) {
                StartItem vm = it.next();
                try {

                    logger.debug("VM " + vm.instanceId + " is under treatment...");
                    String vmStatus = api.instanceStatus(vm.instanceId);

                    if (!"STOPPED".equals(vmStatus)) {
                        logger.debug("VM " + vm.instanceId + " is not ready yet : " + vmStatus);
                        continue;
                    } else {
                        startVm(vm);
                        it.remove();
                    }
                } catch (Throwable e) {
                    logger.debug("Error while trying to start VM (to retry in " +
                                         START_SERVERS_PERIOD + "ms): " + vm.instanceId, e);
                }
            }
        }

        private void startVm(StartItem vm) throws IOException {
            api.updateMetadataInfo(vm.instanceId, vm.metadata);
            logger.debug("VM " + vm.instanceId +
                                 " metadata was put in server: " + vm.metadata.toJSONString());
            String requestId = api.startCreatedServer(vm.instanceId);
            logger.debug("VM " + vm.instanceId + " has started: " + requestId);
        }
    }
}
