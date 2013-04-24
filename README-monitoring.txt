### README MONITORING ###

Monitoring mechanisms can be activated to retrieve information from the IaaS infrastructure. 

Monitoring configuration parameters are passed as any other Infrastructure parameter while creating the 
Node Source. One example of monitoring parameter is as follows: 
 
   $ ./bin/unix/rm-client -c config/authentication/rm.cred -cn NSNAME --infrastructure org.ow2.proactive.iaas.nova.NovaInfrastructure rmi://localhost:1234/ 0 http://openstack.cloud.univcloud.fr:5000/v2.0 'monitoringEnabled,useApi,resolveSigar,useVMProcesses,cred=config/authentication/rm.cred,hostsfile=../monitoringHosts' admin activeeon Admin cfd45429-9535-40ca-921d-a1f8fa792d9b 1 "config/authentication/rm.cred" 

### MONITORING PARAMETERS

The format is as follows: 

   '<monitoringEnabled|monitoringDisabled>[,OPTION1][,OPTION2=VALUE][,OPTION3]...'

## Mandatory parameters

   <monitoringEnabled|monitoringDisabled>

      Tells if monitoring features will be enabled or not. If disabled no other parameter will be 
      taken into account.

## Optional parameters

# API parameters

   useApi

      Use the IaaS API to get monitoring information.

# Sigar monitoring parameters

   cred=</path/to/rm.cred>

      If present, the monitoring system will retrieve monitoring information from hosts/hypervisors 
      through Sigar. The Sigar MBean will be exposed by the standard RMNode. The RMNode Sigar MBean 
      will be contacted using the provided credential. Hosts/hypervisors monitored this way need to 
      run an RMNode connected to the RM, but withouth being added as a node (it means having the 
      parameter addNodeAttempts set to 0, as follows:  
      '<scheduler>/bin/unix/rm-start-node ... --addNodeAttempts 0 ...').

   hostsfile=</path/to/monitoringHosts>

      Use this file to list the set of hosts that will have to be monitored through Sigar. 
      The file format is a standard java Properties file. It contains entries with format name=jmxrourl. 

   resolveSigar

      If present, the monitoring system will not only provide the JMX URL of the hosts monitored, but also 
      monitoring information about them using Sigar. 

   useVMProcesses

      If present, when providing information about a VM, the system will retrieve information coming from 
      the list of processes of all the hosts, and try to enrich the VM information adding 
      real-cpu-consumption and real-memory-usage information.
      This parameter will slow down heavily the monitoring of VMs. 

# Cache parameters

Every VM or Host set of properties can be cached. There are 2 caches. A VM properties cache (a map vmid:mapofprops) and a Hosts properties cache (a map hostid:mapofprops).

   maximumCacheEntries=<integer>

      Maximum amount of cache entries for each cache.

   autoUpdateCache

      If present, each cache will be updated regularly even if no miss occurs. 

   refreshPeriodSeconds=<integer>

      Determines the period for refresh of each cache.

   expirationTimeSeconds=<integer>

      Determines the time it takes for a simple entry (of any cache) to be considered as stale (time measured from the last time a write for the entry took place). 


