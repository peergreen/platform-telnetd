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

import java.io.IOException;
import java.net.Socket;

import org.apache.felix.service.command.CommandProcessor;

/**
 * User: guillaume
 * Date: 25/02/13
 * Time: 16:30
 */
public interface Handler {
    /**
     * Creates a {@link Connection} instance for running client.
     * @param processor Command session factory
     * @param socket the client
     * @return a new Connection or {@literal null} if handler cannot produce clients.
     * @throws IOException
     */
    Connection handle(CommandProcessor processor, Socket socket) throws IOException;
}
