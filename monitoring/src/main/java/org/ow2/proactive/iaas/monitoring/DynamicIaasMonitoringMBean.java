package org.ow2.proactive.iaas.monitoring;

import javax.management.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DynamicIaasMonitoringMBean implements DynamicMBean {

    public static final String HOST_PREFIX = "Host.";
    public static final String VM_PREFIX = "VM.";

    private final IaasMonitoringService service;
    private Boolean initialization;

    public DynamicIaasMonitoringMBean(IaasMonitoringService service) throws IOException {

        if (service == null)
            throw new IllegalArgumentException("service cannot be null");

        this.service = service;
        this.initialization = true;

    }

    public synchronized Object getAttribute(String name) throws AttributeNotFoundException {

        try {
            return invokeServiceMethod("get" + name);
        } catch (NoSuchMethodException e) {
            try {
                return mapAttributeToMethods(name);
            } catch (IaasMonitoringException f) {
                throw new AttributeNotFoundException(f.getMessage());
            }
        } catch (InvocationTargetException e) {
            throw new AttributeNotFoundException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new AttributeNotFoundException(e.getMessage());
        }

    }

    public synchronized Object mapAttributeToMethods(String name) throws IaasMonitoringException {
        if (name.startsWith(HOST_PREFIX)) {
            String entity = getEntityId(name);
            return service.getHostProperties(entity);
        } else if (name.startsWith(VM_PREFIX)) {
            String entity = getEntityId(name);
            return service.getVMProperties(entity);
        } else {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
    }

    private String getEntityId(String name) {
        int dot = name.indexOf('.');
        if (dot >= 0)
            return name.substring(dot + 1);
        else
            throw new IllegalArgumentException("Illegal name: " + name);
    }

    public synchronized void setAttribute(Attribute attribute)
            throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
        throw new InvalidAttributeValueException("Read-only attribute");
    }

    public synchronized AttributeList getAttributes(String[] names) {

        AttributeList list = new AttributeList();
        for (String name : names) {
            try {
                Object value = getAttribute(name);
                if (value != null)
                    list.add(new Attribute(name, value));
            } catch (AttributeNotFoundException e) {
                // Ignore
            }
        }
        return list;
    }

    public synchronized AttributeList setAttributes(AttributeList list) {
        return new AttributeList();
    }

    public Object invoke(String name, Object[] args, String[] sig)
            throws MBeanException, ReflectionException {

        if (args.length != 1)
            throw new ReflectionException(new NoSuchMethodException(name));

        try {
            String param = (String) args[0];
            return invokeServiceMethod(name, param);
        } catch (InvocationTargetException e) {
            throw new MBeanException(e);
        } catch (NoSuchMethodException e) {
            throw new MBeanException(e);
        } catch (IllegalAccessException e) {
            throw new MBeanException(e);
        }

    }

    private Object invokeServiceMethod(String methodName, String singleArgument) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Method m = service.getClass().getMethod(methodName, new Class[]{String.class});
        return m.invoke(service, singleArgument);
    }

    private Object invokeServiceMethod(String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Method m = service.getClass().getMethod(methodName, new Class[]{});
        return m.invoke(service);

    }

    public synchronized MBeanInfo getMBeanInfo() {
        List<String> attributeNames = new ArrayList<String>();

        getListOfAttributes(attributeNames);

        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[attributeNames.size()];
        Iterator<String> it = attributeNames.iterator();
        for (int i = 0; i < attrs.length; i++) {
            String name = it.next();
            attrs[i] = getDefaultAttributeInfo(name);
        }

        MBeanOperationInfo[] operations = getListOfOperations();

        return new MBeanInfo(
                this.getClass().getName(),
                "IaaS Monitoring MBean (" + service.getNodeSourceName() + ")",
                attrs,
                null,       // constructors
                operations, // there are operations, but discouraged to be used
                null);      // notifications
    }

    private MBeanAttributeInfo getDefaultAttributeInfo(String name) {
        return new MBeanAttributeInfo(
                name,
                String.class.getName(),
                "Property " + name,
                true,   // isReadable
                false,   // isWritable
                false); // isIs
    }

    private void getListOfAttributes(List<String> attributeNames) {
        getDefaultListOfAttributes(attributeNames);
        if (!initialization)
            try {
                getListOfAttributesFromArray(attributeNames, service.getHosts(), HOST_PREFIX);
                getListOfAttributesFromArray(attributeNames, service.getVMs(), VM_PREFIX);
            } catch (IaasMonitoringException e) {
                // Ignore it.
            }
        initialization = false;
    }

    private MBeanOperationInfo[] getListOfOperations() {

        List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();

        Class c = service.getClass();
        for (Method method : c.getDeclaredMethods()) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 1) {
                MBeanOperationInfo op = new MBeanOperationInfo(
                        method.getName(),
                        "",
                        new MBeanParameterInfo[]{
                                new MBeanParameterInfo("arg1", String.class.getName(), "")},
                        Object.class.getCanonicalName(),
                        MBeanOperationInfo.UNKNOWN
                );
                ops.add(op);
            }
        }
        return ops.toArray(new MBeanOperationInfo[]{});
    }

    private void getDefaultListOfAttributes(List<String> attributeNames) {
        Class c = service.getClass();
        for (Method method : c.getDeclaredMethods()) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0)
                attributeNames.add(method.getName().substring("get".length()));
        }
    }

    private void getListOfAttributesFromArray(List<String> attributeNames, String[] array, String prefix) {
        for (String name : array)
            attributeNames.add(prefix + name);
    }

}