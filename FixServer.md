# FIX Server #

This section explains how to program a FIX server(FIX session _acceptor_).

### FIX Acceptor types ###
Currently FIX server can be two types:
  1. Single Server Acceptor
  1. Multi Server Acceptor

### Single Server Acceptor ###
Supports one acceptor per connection(server socket). First we create session id. FIX session acceptor needs to know bind host and bind port on which it will listen for inbound connections:

```java

SessionIDBean sessionID = new SessionIDBean("Receiver", "Sender");
ServerSocketSessionAcceptor acceptor = 
        new SingleSessionAcceptor("localhost", 9999, FixVersion.FIX44, sessionID, new FixAcceptorSettings());
```

### Multi Server Acceptor ###
Supports several acceptors per connection(server socket). First we create session manager. Session manager needs to know how much inbound connections simultaneously to handle and which session ids to use:

```java

SessionManager manager = new SimpleSessionManager(10);
SessionIDBean sessionID = new SessionIDBean("Receiver", "Sender");
manager.add(sessionID, new SampleSessionState());
```

You can add session id on fly as well. Further we create simple factory for creation of session acceptors(communicators):

```java

ObjectFactory<FixSessionAcceptor> acceptorFactory = new ObjectFactory<FixSessionAcceptor>(){
  @Override
  public FixSessionAcceptor create() {
    return new FixSessionAcceptor(FixVersion.FIX44, new FixAcceptorSettings());
  }
};
```

FIX session acceptor needs to know bind host and bind port on which it will listen for inbound connections as well as logon timeout and logout timeout:

```java

ServerSocketSessionAcceptor acceptor = new MultiSessionAcceptor("localhost", 9999, 1000, 1000, acceptorFactory, manager);
```

FIX Session instances that we created implement `Runnable`:

```java

new Thread(acceptor).start();
```

---
Missing something? Ask us a question
