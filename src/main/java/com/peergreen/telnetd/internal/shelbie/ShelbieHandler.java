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

package com.peergreen.telnetd.internal.shelbie;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.shelbie.core.branding.BrandingService;
import org.ow2.shelbie.core.console.JLineConsole;
import org.ow2.shelbie.core.history.HistoryFileProvider;
import org.ow2.shelbie.core.prompt.PromptService;

import com.peergreen.telnetd.internal.Connection;
import com.peergreen.telnetd.internal.Handler;
import jline.console.completer.Completer;

/**
 * User: guillaume
 * Date: 04/03/13
 * Time: 09:50
 */
public class ShelbieHandler implements Handler, ServiceTrackerCustomizer<Object,Object> {
    private final BundleContext bundleContext;
    private final ServiceTracker<Object, Object> tracker;

    private BrandingService brandingService;
    private PromptService promptService;
    private HistoryFileProvider historyProvider;
    private Completer completer;

    private Set<JLineConsole> consoles = new HashSet<>();

    public ShelbieHandler(BundleContext bundleContext) throws InvalidSyntaxException {
        this.bundleContext = bundleContext;
        tracker = new ServiceTracker<Object, Object>(bundleContext, createFilter(), this);
    }

    private Filter createFilter() throws InvalidSyntaxException {
        String filter = format("(|(objectclass=%s)(objectclass=%s)(objectclass=%s)(&(objectclass=%s)(type=commands)))",
                BrandingService.class.getName(),
                PromptService.class.getName(),
                HistoryFileProvider.class.getName(),
                Completer.class.getName());
        return bundleContext.createFilter(filter);
    }

    public void start() {
        tracker.open();
    }

    public void stop() {
        tracker.close();
        consoles.clear();
    }

    private boolean hasAllDependencies() {
        return (brandingService != null) && (promptService != null) && (historyProvider != null) && (completer != null);
    }

    @Override
    public Connection handle(CommandProcessor processor, Socket socket) throws IOException {
        if (!hasAllDependencies()) {
            PrintStream stream = new PrintStream(socket.getOutputStream());
            stream.printf("Telnet console handler is missing dependencies and cannot accept remote connections.%n");
            stream.printf("Please try to re-connect later.%n");
            return null;
        }

        ShelbieConnection c = new ShelbieConnection(processor, socket);
        c.setBrandingService(brandingService);
        c.setCompleter(completer);
        c.setHistoryProvider(historyProvider);
        c.setPromptService(promptService);
        return c;
    }

    @Override
    public Object addingService(ServiceReference<Object> reference) {
        Object o = bundleContext.getService(reference);
        // Only accept services coming from our launcher
        if ((o instanceof BrandingService) && (reference.getBundle().getBundleId() == 0)) {
            brandingService = (BrandingService) o;
            return brandingService;
        }

        if ((o instanceof PromptService) && (reference.getBundle().getBundleId() == 0)) {
            promptService = (PromptService) o;
            return promptService;
        }

        if (o instanceof HistoryFileProvider) {
            historyProvider = (HistoryFileProvider) o;
            return historyProvider;
        }

        if (o instanceof Completer) {
            completer = (Completer) o;
            return completer;
        }

        return null;
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference, Object service) {

    }

    @Override
    public void removedService(ServiceReference<Object> reference, Object o) {

        // Close all running console
        for (JLineConsole console : consoles) {
            console.close();
        }
        
        if (o instanceof BrandingService) {
            brandingService = null;
            return;
        }

        if (o instanceof PromptService) {
            promptService = null;
            return;
        }

        if (o instanceof HistoryFileProvider) {
            historyProvider = null;
            return;
        }

        if (o instanceof Completer) {
            completer = null;
            return;
        }
    }

     /*
    public class CrLfToCrFilterInputStream extends FilterInputStream {

        private boolean lastWasCr;

        public CrLfToCrFilterInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            byte b = (byte) in.read();

            if (lastWasCr && b == '\n') {
                // Skip '\n'
                b = (byte) in.read();
            }

            lastWasCr = (b == '\r');
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {

            // Perform the read read operation
            int read = in.read(b, off, len);
            if (read == -1) {
                return -1;
            }

            // Count number of '\r' characters
            int crs = 0;
            for (int i = 0; i < read; i++) {
                if (b[i] == '\r') {
                    crs++;
                }
            }

            // Strip off '\r'
            // TODO make sure the next char is '\n'
            int copy = 0;
            for (int i = 0; i < read; i++) {
                // Current char is '\r'
                // Do that only if following char is '\n'
                if (b[i] != '\r' && ((i+1) < read) && (b[i+1] == '\n')) {
                    b[copy++] = b[i];
                }
            }

            return read - crs;
        }
    }
         */
}
