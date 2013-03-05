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

package com.peergreen.telnetd.internal.gogo;

import java.net.Socket;
import java.util.Date;

import org.apache.felix.service.command.CommandSession;

import com.peergreen.telnetd.internal.Connection;

/**
 * User: guillaume
 * Date: 05/03/13
 * Time: 14:04
 */
public class GoshConnection implements Connection {

    private final CommandSession session;
    private final Socket socket;
    private final Date since = new Date();

    public GoshConnection(CommandSession session, Socket socket) {
        this.session = session;
        this.socket = socket;
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
        try {
            session.execute("gosh --login --noshutdown");
        } finally {
            session.close();
            socket.close();
        }
        return null;
    }

}
