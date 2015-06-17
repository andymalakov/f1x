# Message Gaps #

During initialization, or in the middle of a FIX session, message gaps may occur which are detected via the tracking of incoming sequence numbers.
This page describes how F1X recover missing messages.

### Out of sequence message processing ###

  * If the incoming message has a sequence number less than expected and the PossDupFlag is not set, it indicates a serious error.  F1X terminates FIX connection and logs an error. By default F1X Initiators will attempt to re-establish FIX connectivity after short pause.

  * If the incoming sequence number is greater than expected, it indicates that messages were missed. F1X requests re-transmission of the messages via the Resend Request.

|NOTE: By default F1X does not implement _strictly_ ordered message processing. Inbound messages are processed in the order they are received.|
|:--------------------------------------------------------------------------------------------------------------------------------------------|

This was intentional design decision:

  * In many applications having the latest state is more important than state transitions. For example, in market data feeds the latest snapshot is sufficient (and in fact in some cases gap fill is not even required)

  * It is always possible to organize strictly ordered processing queue outside of core engine.

### How to detect out of order messages ###

Callback for application-level messages has `possDup` parameter:

```
protected void processInboundAppMessage(CharSequence msgType, int msgSeqNum, boolean possDup, MessageParser parser) {
   if (posDup) { ... }
}
```


---


[Back to Quick Start](QuickStart.md)