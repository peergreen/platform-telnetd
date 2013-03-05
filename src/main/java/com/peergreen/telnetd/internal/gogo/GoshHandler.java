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

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;

import com.peergreen.telnetd.internal.Connection;
import com.peergreen.telnetd.internal.Handler;

/**
 * User: guillaume
 * Date: 25/02/13
 * Time: 16:32
 */
public class GoshHandler implements Handler {
    @Override
    public Connection handle(CommandProcessor processor, Socket socket) throws IOException {
        PrintStream out = new PrintStream(socket.getOutputStream());
        CommandSession session = processor.createSession(socket.getInputStream(), out, out);
        return new GoshConnection(session, socket);
    }
}
