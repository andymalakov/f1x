## MessageStore ##

Message Store is used to support FIX [ResendRequest](http://btobits.com/fixopaedia/fixdic44/index.html?message_Resend_Request_2.html) functionality.

By default F1X has no message store. All inbound ResendRequest will get a GapFill response.

Out of the box, F1X provides in-memory implementation of message store backed by fixed circular buffer.

```
MessageStore messageStore = new InMemoryMessageStore(1 << 22); // 4Mb
initiator.setMessageStore(messageStore);
```

## Custom implementations ##
You can implement your own FIX Session schedule using interface `org.f1x.store.MessageStore`.



---


[Back to Quick Start](QuickStart.md)