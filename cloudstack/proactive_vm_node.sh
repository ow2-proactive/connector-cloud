#!/bin/bash

# This script has to started when the template is started.
# (/etc/rc.d/rc.startup for ttyLinux)

#make sure we have have a proper PATH
PATH=/sbin:/usr/sbin:/bin:/usr/bin; export PATH
umask 022
. /etc/rc.d/rc.functions

# JAVA should be preinstalled on the template
export JAVA_HOME=/root/data/java/jdk1.6.0_38/
export PATH=$PATH:$JAVA_HOME/bin

# ProActive too should be preinstalled on the template
cd /root/data/ProActiveScheduling-3.3.2_bin_full

# Try to find the router VM address
router_ip=$(cat /etc/resolv.conf | tail -n 1 | cut -d" " -f2)

# Retrieve user data that we set when sending the deploy command in Cloudstack infrastructure
userdata=$(wget -qO- http://$router_ip/latest/user-data)

# Read user data
rm_ip=$(echo $userdata | cut -d" " -f1)
node_source_name=$(echo $userdata | cut -d" " -f2)
node_name=$(echo $userdata | cut -d" " -f3)
token=$(echo $userdata | cut -d" " -f4)
local_ip=$(ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')

# Dirty way of getting up to data ProActive jars, a HTTP server should be running with the Scheduler
# for instance, run "python -m SimpleHTTPServer 80" from dist/lib
jars="ProActive_annotations_CTree.jar ProActive_examples.jar ProActive.jar ProActive_ResourceManager-client.jar ProActive_ResourceManager.jar ProActive_Scheduler-client.jar ProActive_Scheduler-core.jar ProActive_Scheduler-fsm.jar ProActive_Scheduler-mapreduce.jar ProActive_Scheduler-worker.jar ProActive_SRM-common-client.jar ProActive_SRM-common.jar ProActive_tests.jar ProActive_utils.jar"
cd dist/lib
for jar in $jars
do
 wget -O $jar http://$rm_ip/$jar
done
cd ../..

# Start the RM node
bin/unix/rm-start-node -r rmi://$rm_ip:1099 -f config/authentication/rm.cred -Dproactive.hostname=$local_ip -s $node_source_name -n $node_name -Dproactive.node.access.token=$token &