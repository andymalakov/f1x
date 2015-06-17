# 3-minute F1X introduction #

This page assumes reader is familiar with basics of [FIX Protocol](http://en.wikipedia.org/wiki/Financial_Information_eXchange).
## FIX Client ##

This section explains how to program a FIX client (FIX session _initiator_).

### Establish FIX connection ###

First we create FIX session. Sessions are identified by SessionID. FIX session initiator session needs to know FIX acceptor's host and port:

```java

SessionID sessionID = new SessionIDBean("SENDER-COMP-ID", "TARGET-COMP-ID");
FixSession session = new FixSessionInitiator(host, port, FixVersion.FIX44, sessionID);
```

FIX Session instance that we created implements `Runnable`. Standard implementation establishes socket connection and exchange FIX Logon messages with FIX counter party. Once FIX-protocol level connectivity is established, FIX Session instance runs a message loop that processes inbound messages.

```java

new Thread(session).start();
```

### Send a FIX message ###

Let's send a FIX message as soon as both sides exchanged LOGON requests and FIX connection is established.

```java

session.setEventListener(new SessionEventListener() {
  @Override
  public void onStateChanged(SessionID sessionID, SessionState oldState, SessionState newState) {
    if (newState == SessionState.ApplicationConnected)
      sendSampleMessage(session);
  }
```

F1X uses Builder pattern to construct F1X messages. Our approach is very similar to `java.lang.StringBuilder` or `java.lang.Appendable`.

```java

private static void sendSampleMessage(FixSession client) throws IOException {
  MessageBuilder mb = client.createMessageBuilder(); // sample only
  mb.setMessageType(MsgType.ORDER_SINGLE);
  mb.add(FixTags.ClOrdID, 123);
  mb.add(FixTags.HandlInst, HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
  mb.add(FixTags.OrderQty, 1);
  mb.add(FixTags.OrdType, OrdType.LIMIT);
  mb.add(FixTags.Price, 1.43);
  mb.add(FixTags.Side, Side.BUY);
  mb.add(FixTags.Symbol, "EUR/USD");
  mb.add(FixTags.SecurityType, SecurityType.FOREIGN_EXCHANGE_CONTRACT);
  mb.add(FixTags.TimeInForce, TimeInForce.DAY);
  mb.add(FixTags.ExDestination, "HOTSPOT");
  mb.addUTCTimestamp(FixTags.TransactTime, System.currentTimeMillis());
  client.send(mb);
}
```


### Process inbound FIX messages ###

In order to process inbound messages override `processInboundAppMessage()` method:


```java

FixSession session = new FixSessionInitiator("localhost", 9999, FixVersion.FIX44, sessionID) {
  @Override
  protected void processInboundAppMessage(CharSequence msgType, MessageParser parser) throws IOException {
    if(Tools.equals(MsgType.MARKET_DATA_REQUEST, msgType))
      processMarketDataRequest(parser);
  }
};
```

F1X uses Iterator pattern to process inbound FIX messages.

General idea is:
  1. Call `MessageParser.next()` to advance parser to the next tag in the current FIX message.
  1. Lookup current tag using `MessageParser.getTagNum()`.
  1. Extract value of the current tag according to its data type.
  1. Move to the next tag.

F1X provides a number of convenience methods to extract tag values without creating new  instances. For example:

```java

private static final ByteEnumLookup<SubscriptionRequestType> subscrTypeLookup =
              new ByteEnumLookup<>(SubscriptionRequestType.class);
private final ByteArrayReference symbol = new ByteArrayReference();

private void processMarketDataRequest(MessageParser parser) throws IOException {
  SubscriptionRequestType subscrType = null;
  int depth = -1;
  while (parser.next()) {
    switch (parser.getTagNum()) {
      case FixTags.Symbol:
        parser.getByteSequence(symbol);
        break;
      case FixTags.MarketDepth:
        depth = parser.getIntValue();
        break;
      case FixTags.SubscriptionRequestType:
        subscrType = subscrTypeLookup.get(parser.getByteValue());
      ...

```

Parser supports all commonly used data types in FIX protocol. Enumerated FIX types can be mapped to Java enums.

### Next steps ###

  * SessionSchedule
  * MessageLogging
  * MessageStore
  * FixServer
  * MessageGaps

---
Missing something? Ask us a question
