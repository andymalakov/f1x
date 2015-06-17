# Performance #

This test compares performance of message decoding and message encoding between QuickFIX/J and F1X.

## Test Environment ##
Test platform has the following specifications:
  * Intel(R) Core(TM) i7-5960X (20M L3 Cache, 3.4GHz), Asus X99
  * 48 GB RAM DDR4
  * Microsoft Windows 8.1 Pro 64-bit
  * JDK 1.7 64 (build 1.7.0\_71-b14)
  * JVM starts with options: -Xms1G -Xms1G –server –verbose:gc

## Comparison to Quick FIX ##

This section compares F1X with QuickFIX/J version 1.5.3.

Test used the following New Order Single (D) message as a sample:
```
8=FIX.4.4|9=196|35=D|34=78|49=A12345B|50=2DEFGH4|52=20140603-11:53:03.922|56=COMPARO|57=G|142=AU,SY|1=AU,SY|11=4|21=1|38=50|40=2|44=400.5|54=1|55=OC|58=NIGEL|59=0|60=20140603-11:53:03.922|107=AOZ3 C02000|167=OPT|10=116|
```

Each test has warm up phase (20000 operations) and main phase (1000000 operations). Warm up time is not counted.

Tests were run 10 times. The average result is shown in the table.

| **Library**  | **Operation** | **Messages/sec** | **Average time per message (nanos)** | **F1X Advantage** |
|:-------------|:--------------|:-----------------|:-------------------------------------|:------------------|
| QuickFIX/J   | encoding      | 119904           | 5152                                 | -                 |
|              | decoding      | 341763           | 1446                                 | -                 |
| F1X          | encoding      | 1156069          | 498                                  | 10.34x            |
|              | decoding      | 1342282          | 210                                  | 6.85x             |




## Wire-to-wire latency of "Echo Server" ##

This section measures wire-to-wire latency of FIX processing.

Test setup involved 2 machines:

  * Server (Intel i7-5960 described above)
  * Client (Intel Atom 330 - low-power 'netbook' class server - [Supermicro 5015A-H](http://www.supermicro.com/products/system/1U/5015/SYS-5015A-H.cfm?typ=H))

In this test client was sending NewOrderSingleFIX(D) message 1000 times per second. Server was running EchoServer sample. This simple FIX Acceptor that reads each new FIX message from socket, decodes it, encodes it back, and sends back to the client over the same socket. Test measured **10 million messages**.

Wire-to-wire latency was measured using special utility described [here](https://github.com/andymalakov/libpcap-latency-meter/wiki/Overview). At a nutshell this utility accurately measures time difference between inbound and outbound **TCP packets** containing FIX messages.

The following printout shows test results (numbers are in microseconds)
```
MIN: 8
MAX: 2416
MEDIAN: 9
99.000%: 11
99.900%: 15
99.990%: 25
99.999%: 45
99.9999%: 158
99.99999%:2416
```

During entire time server processed 10 million messages JVM had **0 garbage collections** (process was running with "-Xmx1G -Xmx1G -server" parameters).
