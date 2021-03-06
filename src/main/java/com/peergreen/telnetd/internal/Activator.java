/*
 * Copyright 2013 Peergreen S.A.S.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peergreen.telnetd.internal;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.peergreen.telnetd.internal.shelbie.ShelbieHandler;

/**
 * This activator tracks CommandProcessor and when found creates a Telnetd instance.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<CommandProcessor, Telnetd> {
    /**
     * Specify the list of ports the telnet daemon will be bound to (comma ',' separated).
     */
    public static final String TELNETD_PORTS_PROPERTY = "com.peergreen.telnetd.ports";

    /**
     * Max number of concurrent connections.
     */
    public static final int MAX_CLIENTS = 2;
    private ServiceTracker<CommandProcessor, Telnetd> tracker;
    private BundleContext bundleContext;
    private ThreadGroup threadGroup;
    private ShelbieHandler handler;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        initThreadGroup();

        handler = new ShelbieHandler(bundleContext);
        handler.start();

        tracker = new ServiceTracker<CommandProcessor, Telnetd>(context, CommandProcessor.class, this);
        tracker.open();
    }

    private void initThreadGroup() {
        ServiceTracker<ThreadGroup, ThreadGroup> groupTracker = new ServiceTracker<ThreadGroup, ThreadGroup>(bundleContext, ThreadGroup.class, null);
        groupTracker.open();
        ThreadGroup parent = groupTracker.getService();
        if (parent == null) {
            threadGroup = new ThreadGroup("Peergreen Telnetd");
        } else {
            threadGroup = new ThreadGroup(parent, "Peergreen Telnetd");
        }
        threadGroup.setDaemon(true);
        groupTracker.close();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
        handler.stop();
        // If the user tries to stop this bundle himself, we deliberately shutdown the entire platform
        context.getBundle(0).stop();
    }

    @Override
    public Telnetd addingService(ServiceReference<CommandProcessor> reference) {
        if (tracker.getTracked().isEmpty()) {
            // Protect against multiple CommandProcessor services
            // Should not happen, but anyway ...
            CommandProcessor processor = bundleContext.getService(reference);
            Telnetd telnetd = null;
            try {
                telnetd = new Telnetd(processor);
                telnetd.setExecutors(Executors.newFixedThreadPool(MAX_CLIENTS, new TelnetConnectionThreadFactory()));
                telnetd.setDaemonExecutor(Executors.newSingleThreadExecutor(new TelnetdThreadFactory()));
                telnetd.setHandler(handler);
                telnetd.setPorts(getPorts());
                telnetd.start();

                registerTelnetCommands(telnetd);

                return telnetd;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                telnetd.stop();
            }
        }
        return null;
    }

    private void registerTelnetCommands(Telnetd telnetd) {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("osgi.command.scope", "telnetd");
        properties.put("osgi.command.function", new String[]{"info", "connections"});
        bundleContext.registerService(Telnetd.class.getName(), telnetd, properties);
    }

    private Set<Integer> getPorts() {
        Set<Integer> ports = new TreeSet<>();

        // Add configured ports (if any)
        String secondaries = System.getProperty(TELNETD_PORTS_PROPERTY);
        if (secondaries != null) {
            String[] fragments = secondaries.split(",");
            for (String fragment : fragments) {
                ports.add(Integer.valueOf(fragment.trim()));
            }
        } else {
            ports.add(Telnetd.DEFAULT_PORT);
        }
        return ports;
    }

    @Override
    public void modifiedService(ServiceReference<CommandProcessor> reference, Telnetd service) {}

    @Override
    public void removedService(ServiceReference<CommandProcessor> reference, Telnetd service) {
        service.stop();
    }

    private class TelnetdThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(threadGroup, r);
            thread.setName(format("%s Daemon", threadGroup.getName()));
            thread.setDaemon(threadGroup.isDaemon());
            return thread;
        }
    }

    private class TelnetConnectionThreadFactory implements ThreadFactory {

        private AtomicInteger index = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(threadGroup, r);
            thread.setName(format("%s Connection %d", threadGroup.getName(), index.getAndIncrement()));
            thread.setDaemon(threadGroup.isDaemon());
            return thread;
        }
    }
}
