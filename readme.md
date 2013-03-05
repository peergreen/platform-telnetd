Peergreen Telnet Daemon
=========================

Installation
------------------
The telnet daemon should already be installed on the platform.
If not, install with the following command:

    start mvn:com.peergreen.telnetd/platform-telnetd/1.0.0-SNAPSHOT


Configuration
------------------
Listening ports can be configured through a System property `com.peergreen.telnetd.ports`.

By default (or if no port is specified), the daemon is bound to the `10023` port (all interfaces).

Maximum concurrent number of clients cannot be changed and is `20`.

Connecting
------------------
When a connection attempt is made to one of the bound address, the daemon simply creates
a new shell console and connects connection's input and output stream into the console.

Any `telnet` client can be used to connect to this daemon.
Type `exit` for exiting the session (without shutdowning the platform).

    >$ telnet 127.0.01 10023
    Trying 127.0.0.1...
    Connected to localhost.
    Escape character is '^]'.
         ___                                                       ___  _         _     __
        / _ \  ___   ___  _ __   __ _  _ __   ___   ___  _ __     / _ \| |  __ _ | |_  / _|  ___   _ __  _ __ ___
       / /_)/ / _ \ / _ \| '__| / _` || '__| / _ \ / _ \| '_ \   / /_)/| | / _` || __|| |_  / _ \ | '__|| '_ ` _ \
      / ___/ |  __/|  __/| |   | (_| || |   |  __/|  __/| | | | / ___/ | || (_| || |_ |  _|| (_) || |   | | | | | |
      \/      \___| \___||_|    \__, ||_|    \___| \___||_| |_| \/     |_| \__,_| \__||_|   \___/ |_|   |_| |_| |_|
                                |___/
                                               ___                          _ _          ___    _ _ _   _
                                              / __|___ _ __  _ __ _  _ _ _ (_) |_ _  _  | __|__| (_) |_(_)___ _ _
                                             | (__/ _ \ '  \| '  \ || | ' \| |  _| || | | _|/ _` | |  _| / _ \ ' \
                                              \___\___/_|_|_|_|_|_\_,_|_||_|_|\__|\_, | |___\__,_|_|\__|_\___/_||_|
                                                                                  |__/
    guillaume@peergreen-platform$ <your typing here>


Commands
------------------
Peergreen Telnet daemon is bundled with shell commands:

### telnetd:info
Produce information about the daemon itself.

    guillaume@peergreen-platform$ telnetd:info
    Peergreen Telnet Daemon
    ------------------------------------------
      Listening ports:       [10023, 51264]
      Since:                 mar. mars 05 16:37:31 CET 2013
      Running:               true
      Max connections:       20
      Current connections:   1
      Failed connections:    0
      Accepted connections:  1
      Refused connections:   0

### telnetd:connections
Produce details about each running connections.

    guillaume@peergreen-platform$ telnetd:connections
    Telnet Connection
    ------------------------------------------
      Since:         mar. mars 05 16:37:52 CET 2013
      Last Activity: mar. mars 05 17:14:50 CET 2013
      Origin:        /127.0.0.1:60420

