netio
=====

"NetIO" is a Java library which aims to simplify the communication with the NetIO230B, a device that allows the control of external devices connected to its power outputs through the network which is produced by [Koukaam](http://www.koukaam.se/kkm/index.php).
The "kshell" interface is used and currently firmware version 4.01 was sucessfully tested.

Example
-------

```java
NetIO netIO = new Builder("hostname", port).setUsername("username").setPassword("password").build();
// Get status of port 1
netIO.getPortStatus(1);
```

[![Build Status](https://travis-ci.org/crea-doo/netio.svg?branch=master)](https://travis-ci.org/crea-doo/netio)