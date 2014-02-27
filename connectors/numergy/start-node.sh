#!/bin/bash

# Hardcoded script that starts the RMNode at boot time.
# The service that calls this script at boot time is 'myscript' service. You 
# can start it by doing: 
#   service myscript start
# and stop it by doing:
#   service myscript stop

# To configure
export METADATASERVER=10.200.101.161:9200

export SCHEDULER_HOME=/home/proactive/ProActiveScheduling-3.4.0_bin_full
export JAVA_HOME=/home/proactive/jdk


function getMetaDataFromServer() {
	PARAM="$1"
	unset http_proxy
	curl -X GET $METADATASERVER/server/metadata/$IP | python -c "import json,sys;obj=json.load(sys.stdin);print obj[\"_source\"][\"$PARAM\"]"
}

set -x

export IP=`ifconfig | grep -C 1  eth0 | grep inet | awk '{print $2}' | sed 's/addr://g' | sed 's/\./_/g'`

echo "Metadata server: $METADATASERVER"

echo "Current IP: $IP"

echo "Waiting for system to stabilize..."
sleep 120 # The VM is started while duplication, some configuration takes place and then it is shut down, and started again. To prevent the RMNode to get connected to the RM during this "configuration" phase we use this sleep.

echo "Getting data from metadata server..."

rm_url=`getMetaDataFromServer rm_url`
protocol=`getMetaDataFromServer protocol`
router_port=`getMetaDataFromServer router_port`
router_address=`getMetaDataFromServer router_address`
credentials=`getMetaDataFromServer credentials`
node_source_name=`getMetaDataFromServer node_source_name`
node_name=`getMetaDataFromServer node_name`
token=`getMetaDataFromServer token`

echo "Now node start begins."

$SCHEDULER_HOME/bin/unix/rm-start-node -r $rm_url -Dproactive.communication.protocol=$protocol -Dproactive.pamr.router.address=$router_address -Dproactive.pamr.router.port=$router_port -v $credentials -s $node_source_name -n $node_name -Dproactive.useIPaddress=true -Dproactive.net.nolocal=true -Dproactive.node.access.token=$token -Dproactive.agent.rank=0 &> /tmp/rm-start-node.log

