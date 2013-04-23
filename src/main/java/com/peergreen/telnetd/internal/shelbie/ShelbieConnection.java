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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.ow2.shelbie.core.branding.BrandingService;
import org.ow2.shelbie.core.console.JLineConsole;
import org.ow2.shelbie.core.history.HistoryFileProvider;
import org.ow2.shelbie.core.prompt.PromptService;

import com.peergreen.telnetd.internal.Connection;
import com.peergreen.telnetd.internal.security.RolesPrincipal;
import com.peergreen.telnetd.internal.security.UserPrincipal;
import jline.Terminal;
import jline.console.completer.Completer;

/**
 * User: guillaume
 * Date: 05/03/13
 * Time: 14:27
 */
public class ShelbieConnection implements Connection {
    private final CommandProcessor processor;
    private final Socket socket;
    private final Date since = new Date();
    private Completer completer;
    private HistoryFileProvider historyProvider;
    private PromptService promptService;
    private BrandingService brandingService;

    public ShelbieConnection(CommandProcessor processor, Socket socket) {
        this.processor = processor;
        this.socket = socket;
    }

    public void setCompleter(Completer completer) {
        this.completer = completer;
    }

    public void setHistoryProvider(HistoryFileProvider historyProvider) {
        this.historyProvider = historyProvider;
    }

    public void setPromptService(PromptService promptService) {
        this.promptService = promptService;
    }

    public void setBrandingService(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @Override
    public Date getSince() {
        return since;
    }

    @Override
    public Date getLastActivity() {
        return new Date();
    }

    @Override
    public String getClientInfo() {
        return socket.getRemoteSocketAddress().toString();
    }

    @Override
    public Void call() throws Exception {
        // Start the console
        JLineConsole console = null;
        Terminal terminal = new AnsiUnsupportedTerminal();
        try {
            // Transform '\n' into '\r\n' to satisfy telnet windows clients
            PrintStream out = new PrintStream(new LfToCrLfFilterOutputStream(socket.getOutputStream()));
            console = new JLineConsole(
                    processor,
                    completer,
                    socket.getInputStream(),
                    out,
                    out,
                    terminal);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }

        Subject subject = createLocalSubject();
        console.setHistoryFile(historyProvider.getHistoryFile(subject));
        console.setPromptService(promptService);
        console.setBrandingService(brandingService);

        // Store some global properties
        console.getSession().put(Subject.class.getName(), subject);
        console.getSession().put("terminal", terminal);
        console.getSession().put("application.name", "peergreen-platform");

        console.run();

        socket.close();

        return null;
    }

    private Subject createLocalSubject() {
        Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("anonymous"));
        subject.getPrincipals().add(new RolesPrincipal("admin"));
        return subject;
    }

    private class LfToCrLfFilterOutputStream extends FilterOutputStream {

        private boolean lastWasCr;

        public LfToCrLfFilterOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (!lastWasCr && b == '\n') {
                out.write('\r');
                out.write('\n');
            } else {
                out.write(b);
            }
            lastWasCr = b == '\r';
        }

    }


}
