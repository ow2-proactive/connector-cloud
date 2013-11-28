#!/bin/bash
cd /home/azureuser/scheduling
./bin/unix/rm-client -Dproactive.pamr.router.address=109.26.74.25 -Dproactive.pamr.router.port=8090 -u pamr://4096/ -c config/authentication/rm.cred -cn NUMERGY_NODESOURCE --infrastructure org.ow2.proactive.iaas.numergy.NumergyInfrastructure pamr://4096 1 https://api2.numergy.com/v2.0/tokens monitoringDisabled FH6O3uqeAIssIkVNu0jrGKcQ4D2vAWY29jlBY1gj Ee6GFcwL9kecUGsmFesA2623BfmadgokDVbOuH0f 611ca26c-1a1f-11e3-8d40-005056992152 2a2b35d4-278a-11e3-8d40-005056992152 http://109.26.74.25:9200/ ./config/authentication/rm.cred 10.200.101.161 8090 pamr
