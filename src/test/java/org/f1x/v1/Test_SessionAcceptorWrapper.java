package org.f1x.v1;

import org.f1x.SessionIDBean;
import org.f1x.TestCommon;
import org.f1x.api.FixAcceptorSettings;
import org.f1x.api.session.SessionManager;
import org.f1x.api.session.SessionState;
import org.f1x.util.AsciiUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;


public class Test_SessionAcceptorWrapper extends TestCommon {

    private SessionAcceptorWrapper wrapper;
    private int onStopInvocationCount;
    private SessionManager manager;

    // Mocks
    private FixSessionAcceptor acceptor;
    private Socket socket;

    @Before
    public void init() {
        onStopInvocationCount = 0;
        manager = new SimpleSessionManager(10);
        acceptor = mock(FixSessionAcceptor.class);
        when(acceptor.getSettings()).thenReturn(new FixAcceptorSettings());
        wrapper = new SessionAcceptorWrapper(acceptor, manager, 500) {

            @Override
            void onStop() {
                onStopInvocationCount++;
                assertWrapperStateOnStop();
            }

            private void assertWrapperStateOnStop() {
                assertNull(this.socket);
                assertEquals(new SessionIDBean(), sessionID);
            }

        };

        socket = mock(Socket.class);
        wrapper.setSocket(socket);
    }

    @Test
    public void testEmptyMessage() throws Exception {
        simulateMessageOnSocketRead("");
        simulateWrapperRun();
        assertThatAcceptorWasNotStarted();
    }

    @Test(timeout = 300)
    public void testSocketException() throws IOException {
        throwExceptionOnSocketRead(SocketException.class);
        simulateWrapperRun();
        assertThatAcceptorWasNotStarted();
    }

    @Test(timeout = 300)
    public void testSocketTimeoutException() throws IOException {
        throwExceptionOnSocketRead(SocketTimeoutException.class);
        simulateWrapperRun();
        assertThatAcceptorWasNotStarted();
    }

    @Test
    public void testInvalidLogon() throws IOException {
        String logonWithoutBodyLength = "8=FIX.4.4|35=A|34=1|49=SC|50=SS|52=20140101-10:10:10.100|56=TC|57=TS|98=0|108=30|141=Y|383=8192|10=080|";
        simulateMessageOnSocketRead(logonWithoutBodyLength);
        simulateWrapperRun();
        assertThatAcceptorWasNotStarted();
    }

    @Test
    public void testVeryLongLogonMessage() throws IOException {
        String veryLongLogon = "8=FIX.4.4|9="
                + (acceptor.getSettings().getMaxInboundMessageSize() + 1)
                + "|35=A|34=1|49=SC|50=SS|52=20140101-10:10:10.100|56=TC|57=TS|98=0|108=30|141=Y|383=8192|10=080|";
        simulateMessageOnSocketRead(veryLongLogon);
        simulateWrapperRun();
        assertThatAcceptorWasNotStarted();
    }

    @Test
    public void testLogonWithUnregisteredSessionID() throws IOException {
        String logon = "8=FIX.4.4|9=82|35=A|34=1|49=SC|50=SS|52=20140101-10:10:10.100|56=TC|57=TS|98=0|108=30|141=Y|383=8192|10=080|NEXT MESSAGE";
        simulateMessageOnSocketRead(logon);
        simulateWrapperRun();
        assertThatAcceptorWasNotStarted();
    }

    @Test
    public void testLogonWithRegisteredSessionID() throws IOException {
        String logon = "8=FIX.4.4|9=74|35=A|34=1|49=SC|52=20140101-10:10:10.100|56=TC|98=0|108=30|141=Y|383=8192|10=080|NEXT MESSAGE";
        simulateMessageOnSocketRead(logon);
        manager.add(new SessionIDBean("SC", "TC"), createTestSessionState());
        simulateWrapperRun();
        assertThatAcceptorWasStarted(logon);
    }

    @After
    public void assertOnStopInvocation() {
        assertEquals(1, onStopInvocationCount);
    }

    private void throwExceptionOnSocketRead(Class<? extends Throwable> exceptionClass) throws IOException {
        InputStream in = mock(InputStream.class);
        when(in.read(any(byte[].class), anyInt(), anyInt())).thenThrow(exceptionClass);
        when(socket.getInputStream()).thenReturn(in);
    }

    private void simulateMessageOnSocketRead(String message) throws IOException {
        message = message.replace('|', '\u0001');
        ByteArrayInputStream simulatedInputStream = new ByteArrayInputStream(AsciiUtils.getBytes(message));
        when(socket.getInputStream()).thenReturn(simulatedInputStream);
    }

    private void simulateWrapperRun() {
        wrapper.run();
    }

    private void assertThatAcceptorWasNotStarted() throws IOException {
        verify(acceptor, never()).run(any(byte[].class), anyInt());
        verify(socket).close();
    }

    private void assertThatAcceptorWasStarted(final String expectedMessage) {
        verify(acceptor).run(argThat(new BaseMatcher<byte[]>() {

            private final byte[] expectedByteMessage = AsciiUtils.getBytes(expectedMessage.replace('|', '\u0001'));

            @Override
            public boolean matches(Object o) {
                if (o instanceof byte[]) {
                    byte[] actualByteMessage = (byte[]) o;
                    for (int index = 0; index < expectedByteMessage.length; index++)
                        if (expectedByteMessage[index] != actualByteMessage[index])
                            return false;

                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Arrays.toString(expectedByteMessage));
            }

        }), eq(expectedMessage.length()));
    }

    private static SessionState createTestSessionState() {
        return mock(SessionState.class);
    }

}
