## Included loggers ##

F1X provides several out-of-the-box implementations for FIX message loggers:

  * Basic file logger that writes into `BufferedOutputStream` and can flush buffer asynchronously. There is a variation that starts a new file each midnight. Another variation cycles between N files of fixed size.
  * Fixed-size logger backed by memory mapped file.
  * FIX message logger that stores messages into GFLogger.


### Examples ###
Loggers are configured using their `MessageLogFactory`.

```
File logDir = ...
fix.setLogFactory(new DailyFileMessageLogFactory(logDir));
```

Logging parameter can be customized via log factory:

```
File logDir = ...
DailyFileMessageLogFactory logFactory = new DailyFileMessageLogFactory(logDir);
logFactory.setFlushPeriod(15000);
logFactory.setLogFormatter(new CustomLogFormatter());
fix.setLogFactory(logFactory);
```

Most of included implementations support custom formatting via `LogFormatter` interface. File-based loggers allow to customize output file names via `FileNameGenerator` interface. Stream-based loggers allow to select appropriate output settings (like buffer size) via `OutputStreamFactory` interface.

## Custom loggers ##

Custom implementations should implement interfaces `MessageLog` and `MessageLogFactory`.



---

[Back to Quick Start](QuickStart.md)