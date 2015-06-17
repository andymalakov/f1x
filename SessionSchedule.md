You can call `FixSession.close()` to initiate FIX Logout and close the socket.


## Standard implementation ##
F1X provides session schedule similar QuickFIX.

```
SessionSchedule schedule = 
   new SimpleSessionSchedule(
      Calendar.SUNDAY, Calendar.FRIDAY, 
      "17:30:00", "17:00:00", true,
     TimeZone.getTimeZone("America/New_York"));
initiator.setSchedule(schedule);
```

An additional option allows to have daily vs. weekly session (e.g. sessions that span more than 24h, say Sunday-Friday).

## Custom implementations ##
You can implement your own FIX Session schedule using interface `org.f1x.v1.schedule.SessionSchedule`.




---

[Back to Quick Start](QuickStart.md)