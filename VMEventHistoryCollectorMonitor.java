/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.events;

import com.vmware.common.annotations.Action;
import com.vmware.common.annotations.Option;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * VMEventHistoryCollectorMonitor
 *
 * This sample is responsible for creating EventHistoryCollector
 * filtered for a single VM and monitoring Events using the
 * latestPage attribute of the EventHistoryCollector.
 *
 * <b>Parameters:</b>
 * url        [required] : url of the web service
 * username   [required] : username for the authentication
 * password   [required] : password for the authentication
 * vmname     [required] : virtual machine name
 *
 * <b>Command Line:</b>
 * run.bat com.vmware.events.VMEventHistoryCollectorMonitor --url [webserviceurl]
 * --username [username] --password  [password] --vmname  [vm name]
 * </pre>
 */

@Sample(
        name = "vmevent-history-collector-monitor",
        description = "This sample is responsible for creating EventHistoryCollector " +
                "filtered for a single VM and monitoring Events using the " +
                "latestPage attribute of the EventHistoryCollector."
)
public class VMEventHistoryCollectorMonitor extends ConnectedVimServiceBase {

    private ManagedObjectReference propCollectorRef;
    private ManagedObjectReference rootFolderRef;
    private ManagedObjectReference eventHistoryCollectorRef;
    private ManagedObjectReference eventManagerRef;
    private ManagedObjectReference vmRef;

    private String vmName;

    @Option(name = "vmname", description = "virtual machine name")
    public void setVmName(String name) {
        this.vmName = name;
    }

    /**
     * Creates the event history collector.
     *
     * @throws Exception the exception
     */
    void createEventHistoryCollector() throws RuntimeFaultFaultMsg, InvalidStateFaultMsg {

        EventFilterSpecByEntity entitySpec = new EventFilterSpecByEntity();
        entitySpec.setEntity(vmRef);
        entitySpec.setRecursion(EventFilterSpecRecursionOption.SELF);
        EventFilterSpec eventFilter = new EventFilterSpec();
        eventFilter.setEntity(entitySpec);
        eventHistoryCollectorRef =
                vimPort.createCollectorForEvents(eventManagerRef, eventFilter);
    }

    /**
     * Creates the event filter Spec.
     *
     * @return the PropertyFilterSpec
     */
    PropertyFilterSpec createEventFilterSpec() {
        PropertySpec propSpec = new PropertySpec();
        propSpec.setAll(new Boolean(false));
        propSpec.getPathSet().add("latestPage");
        propSpec.setType(eventHistoryCollectorRef.getType());

        ObjectSpec objSpec = new ObjectSpec();
        objSpec.setObj(eventHistoryCollectorRef);
        objSpec.setSkip(new Boolean(false));

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(propSpec);
        spec.getObjectSet().add(objSpec);
        return spec;
    }

    /**
     * Uses the new RetrievePropertiesEx method to emulate the now deprecated
     * RetrieveProperties method.
     *
     * @param listpfs
     * @return list of object content
     * @throws Exception
     */
    List<ObjectContent> retrievePropertiesAllObjects(
            List<PropertyFilterSpec> listpfs) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

        RetrieveResult rslts =
                vimPort.retrievePropertiesEx(propCollectorRef, listpfs,
                        propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null
                && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts =
                    vimPort.continueRetrievePropertiesEx(propCollectorRef, token);
            token = null;
            if (rslts != null) {
                token = rslts.getToken();
                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcontent.addAll(rslts.getObjects());
                }
            }
        }

        return listobjcontent;
    }

    /**
     * Monitor events.
     *
     * @param spec the PropertyFilterSpec
     * @throws Exception the Exception
     */
    void monitorEvents(PropertyFilterSpec spec) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        ArrayList<PropertyFilterSpec> listpfs =
                new ArrayList<PropertyFilterSpec>();
        listpfs.add(spec);
        List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);
        if (listobjcont != null) {
            ObjectContent oc = listobjcont.get(0);
            ArrayOfEvent arrayEvents =
                    (ArrayOfEvent) (oc.getPropSet().get(0)).getVal();

            ArrayList<Event> eventList = (ArrayList<Event>) arrayEvents.getEvent();
            System.out.println("Events In the latestPage are: ");
            for (int i = 0; i < eventList.size(); i++) {
                Event anEvent = eventList.get(i);
                System.out.println("Event: " + anEvent.getClass().getName());
            }
        } else {
            System.out.println("No Events retrieved!");
        }
    }

    @Action
    public void run() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidStateFaultMsg {
        propCollectorRef = serviceContent.getPropertyCollector();
        rootFolderRef = serviceContent.getRootFolder();
        eventManagerRef = serviceContent.getEventManager();
        Map<String, ManagedObjectReference> vms = getMOREFs.inContainerByType(serviceContent
                .getRootFolder(), "VirtualMachine");
        vmRef = vms.get(vmName);
        if (vmRef != null) {
            createEventHistoryCollector();
            PropertyFilterSpec eventFilterSpec = createEventFilterSpec();
            monitorEvents(eventFilterSpec);
        } else {
            System.out.println("Virtual Machine " + vmName + " Not Found.");
            return;
        }
    }

}
