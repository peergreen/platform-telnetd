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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.felix.service.command.CommandProcessor;

/**
 * User: guillaume
 * Date: 25/02/13
 * Time: 15:49
 */
public class Telnetd implements Runnable {
    public static final int DEFAULT_PORT = 10023;

    private final CommandProcessor processor;

    private ExecutorService executors;
    private ExecutorService daemonExecutor;
    private Handler handler;
    private Set<Integer> ports = Collections.singleton(DEFAULT_PORT);
    private Selector selector;
    private List<ServerSocketChannel> servers = new ArrayList<>();

    private final Date since = new Date();
    private int accepted = 0;
    private int refused = 0;
    private int failed = 0;

    private List<Connection> connections = new ArrayList<>();

    public Telnetd(CommandProcessor processor) {
        this.processor = processor;
    }

    public void setExecutors(ExecutorService executors) {
        this.executors = executors;
    }

    public void setDaemonExecutor(ExecutorService daemonExecutor) {
        this.daemonExecutor = daemonExecutor;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setPorts(Set<Integer> ports) {
        this.ports = ports;
    }

    public void start() throws IOException {
        selector = Selector.open();

        for (int port : ports) {
            ServerSocketChannel server = null;
            try {
                server = ServerSocketChannel.open();
                server.configureBlocking(false);

                server.socket().bind(new InetSocketAddress(port));
                // we are only interested when accept events occur on this socket
                server.register(selector, SelectionKey.OP_ACCEPT);

                servers.add(server);
            } catch (IOException e) {
                // Something went wrong during registration
                close(server);
                // TODO Log
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        daemonExecutor.submit(this);
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignored) {
                    // Ignore
                }
            }
        }
    }

    public void stop() {
        for (ServerSocketChannel server : servers) {
            close(server);
        }
        close(selector);
        executors.shutdownNow();
    }

    @Override
    public void run() {

        while (selector.isOpen()) {
            try {
                selector.select();
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                for (SelectionKey key : readyKeys) {
                    if (key.isAcceptable()) {
                        try {
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept();
                            Socket socket = client.socket();
                            final Connection c = handler.handle(processor, socket);
                            if (c != null) {
                                accepted++;
                                // handler was able to produce a client
                                executors.submit(new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        connections.add(c);
                                        try {
                                            return c.call();
                                        } finally {
                                            connections.remove(c);
                                        }
                                    }
                                });
                            } else {
                                refused++;
                                close(socket);
                            }
                        } catch (IOException e) {
                            failed++;
                            // Cannot handle this client's connection
                            // Move to next iteration
                            // TODO log

                        }
                    }
                }
            } catch (IOException e) {
                // TODO log
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void info() {
        System.out.printf("Peergreen Telnet Daemon%n");
        System.out.printf("------------------------------------------%n");
        System.out.printf("  Listening ports:       %s%n", ports);
        System.out.printf("  Since:                 %tc%n", since);
        System.out.printf("  Running:               %s%n", selector.isOpen());
        System.out.printf("  Max connections:       %d%n", Activator.MAX_CLIENTS);
        System.out.printf("  Current connections:   %d%n", connections.size());
        System.out.printf("  Failed connections:    %d%n", failed);
        System.out.printf("  Accepted connections:  %d%n", accepted);
        System.out.printf("  Refused connections:   %d%n", refused);
    }

    public void connections() {
        for (Connection connection : connections) {
            System.out.printf("Telnet Connection%n");
            System.out.printf("------------------------------------------%n");
            System.out.printf("  Since:         %tc%n", connection.getSince());
            System.out.printf("  Last Activity: %tc%n", connection.getLastActivity());
            System.out.printf("  Origin:        %s%n", connection.getClientInfo());
        }
    }

}
