# FAQ #

## Where are FIX Dictionaries? ##
We do not provide FIX Dictionaries or any way to automatically validate FIX messages. Based on our experience, most FIX brokers do not have strictly defined FIX dialect (some undocumented tags may appear without notice; tags described as required may be sometimes omitted). If you think otherwise, custom validation can be implemented on top of our low-level API.

## How to specify text value in non-ASCII encoding? ##

One way of doing this is to use `MessageBuilder.addRaw()` method:

``` java
mb.add(FixTags.MessageEncoding, MessageEncoding.SHIFT_JIS);
mb.add(FixTags.Text, "Hello");
String hello = new String ("\u3053\u3093\u306b\u3061\u306f");
byte [] helloEncoded = hello.getBytes("Shift_JIS");

mb.add(FixTags.EncodedTextLen, helloEncoded.length);
mb.addRaw(FixTags.EncodedText, helloEncoded, 0, helloEncoded.length);
```

## How to define a repeating group? ##

You need to add repeating group tags in the order they will appear in the FIX message, starting with tag that specifies number of entries:
``` java
mb.add(FixTags.NoMDEntries, 2);

mb.add(FixTags.MDEntryType, MDEntryType.BID);
mb.add(FixTags.MDEntryPx, 12.32);
mb.add(FixTags.MDEntrySize, 100);
mb.add(FixTags.QuoteEntryID, "BID123");

mb.add(FixTags.MDEntryType, MDEntryType.OFFER);
mb.add(FixTags.MDEntryPx, 12.32);
mb.add(FixTags.MDEntrySize, 100);
mb.add(FixTags.QuoteEntryID, "OFFER123");
```
