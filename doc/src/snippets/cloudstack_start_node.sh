#!/bin/bash
PATH=/sbin:/usr/sbin:/bin:/usr/bin; export PATH
umask 022
. /etc/rc.d/rc.functions

# read user data
rm_ip=$(wget -qO- http://$VIRTUAL_ROUTER_ADDRESS/latest/user-data)

# start one ProActive node, registering to the default node source
$PROACTIVE_INSTALLATION/bin/unix/rm-start-node -r rmi://$rm_ip:1099 -f config/authentication/rm.cred &