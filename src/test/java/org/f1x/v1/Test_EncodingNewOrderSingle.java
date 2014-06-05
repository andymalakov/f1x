package org.f1x.v1;

import org.f1x.SessionIDBean;
import org.f1x.api.FixVersion;
import org.f1x.api.message.MessageBuilder;
import org.f1x.api.message.fields.*;
import org.f1x.api.session.SessionID;
import org.f1x.io.OutputChannel;
import org.f1x.util.AsciiUtils;
import org.f1x.util.RealTimeSource;
import org.junit.Test;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.field.MsgType;

import java.io.IOException;
import java.util.Date;
import java.util.Scanner;

public class Test_EncodingNewOrderSingle {

    private static final int WARMUP = 20000;
    private static final int N = 1000000;

    private static final String SENDER_COMP_ID = "A12345B";
    private static final String SENDER_SUB_ID = "2DEFGH4";
    private static final String TARGET_COMP_ID = "COMPARO";
    private static final String TARGET_SUB_ID = "G";
    private static final int MSG_SEQ_NUM = 78;
    private static final String ACCOUNT = "AU,SY";
    private static final String SENDER_LOCATION_ID = "AU,SY";
    private static final int ORDER_ID = 4;
    private static final int QUANTITY = 50;
    private static final double PRICE = 400.5;
    private static final String SYMBOL = "OC";
    private static final String SECURITY_DESCRIPTION = "AOZ3 C02000";
    private static final String TEXT = "NIGEL";

    @Test
    public void testTinyFIXEncoding() throws IOException {
        MessageBuilder mb = new ByteBufferMessageBuilder(256, 2);
        RawMessageAssembler asm = new RawMessageAssembler(FixVersion.FIX44, 256, RealTimeSource.INSTANCE);
        SessionID sessionID = new SessionIDBean(SENDER_COMP_ID, SENDER_SUB_ID, TARGET_COMP_ID, TARGET_SUB_ID);
        NullOutputChannel out = new NullOutputChannel();

        for (int i = 0; i < WARMUP; i++)
            encode(mb, asm, sessionID, out);

        long start = System.nanoTime();
        for (int i = 0; i < N; i++)
            encode(mb, asm, sessionID, out);

        long end = System.nanoTime();

        System.out.println("Average time " + (end - start) / N + " ns. per encoding, dummy result: " + out);
    }

    @Test
    public void testQuickFIXJEncoding() {
        Message msg = new Message();
        String dummy = null;

        for (int i = 0; i < WARMUP; i++)
            dummy = encode(msg);

        long start = System.nanoTime();
        for (int i = 0; i < N; i++)
            dummy = encode(msg);

        long end = System.nanoTime();

        System.out.println("Average time " + (end - start) / N + " ns. per encoding, dummy result: " + dummy);
    }

    private static String encode(Message msg) {
        msg.clear();
        Message.Header header = msg.getHeader();
        header.setString(FixTags.BeginString, FixVersion.FIX44.getBeginString());
        header.setString(FixTags.MsgType, MsgType.ORDER_SINGLE);
        header.setInt(FixTags.MsgSeqNum, MSG_SEQ_NUM);
        header.setString(FixTags.SenderCompID, SENDER_COMP_ID);
        header.setString(FixTags.SenderSubID, SENDER_SUB_ID);
        header.setUtcTimeStamp(FixTags.SendingTime, new Date(), true);
        header.setString(FixTags.TargetCompID, TARGET_COMP_ID);
        header.setString(FixTags.TargetSubID, TARGET_SUB_ID);
        header.setString(FixTags.SenderLocationID, SENDER_LOCATION_ID);

        msg.setString(FixTags.Account, ACCOUNT);
        msg.setInt(FixTags.ClOrdID, ORDER_ID);
        msg.setChar(FixTags.HandlInst, quickfix.field.HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
        msg.setInt(FixTags.OrderQty, QUANTITY);
        msg.setChar(FixTags.OrdType, quickfix.field.OrdType.LIMIT);
        msg.setDouble(FixTags.Price, PRICE);
        msg.setChar(FixTags.Side, quickfix.field.Side.BUY);
        msg.setString(FixTags.Symbol, SYMBOL);
        msg.setString(FixTags.SecurityDesc, SECURITY_DESCRIPTION);
        msg.setString(FixTags.SecurityType, SecurityType.OPTION.getCode());
        msg.setString(FixTags.Text, TEXT);
        msg.setChar(FixTags.TimeInForce, quickfix.field.TimeInForce.DAY);
        msg.setUtcTimeStamp(FixTags.TransactTime, new Date(System.currentTimeMillis()), true);

        return msg.toString();
    }

    private static void encode(MessageBuilder mb, RawMessageAssembler asm, SessionID sessionID, OutputChannel out) throws IOException {
        mb.clear();
        mb.setMessageType(MsgType.ORDER_SINGLE);
        mb.add(FixTags.SenderLocationID, SENDER_LOCATION_ID);
        mb.add(FixTags.Account, ACCOUNT);
        mb.add(FixTags.ClOrdID, ORDER_ID);
        mb.add(FixTags.HandlInst, HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
        mb.add(FixTags.OrderQty, QUANTITY);
        mb.add(FixTags.OrdType, OrdType.LIMIT);
        mb.add(FixTags.Price, PRICE);
        mb.add(FixTags.Side, Side.BUY);
        mb.add(FixTags.Symbol, SYMBOL);
        mb.add(FixTags.Text, TEXT);
        mb.add(FixTags.TimeInForce, TimeInForce.DAY);
        mb.addUTCTimestamp(FixTags.TransactTime, System.currentTimeMillis());
        mb.add(FixTags.SecurityDesc, SECURITY_DESCRIPTION);
        mb.add(FixTags.SecurityType, SecurityType.OPTION);
        asm.send(sessionID, MSG_SEQ_NUM, mb, null, out);
    }

    public static void main(String[] args) throws InvalidMessage, IOException {
        Scanner sc = new Scanner(System.in);
        int mode = sc.nextInt();
        sc.nextLine();
        int iterations = sc.nextInt();
        sc.nextLine();
        switch (mode) {
            case 1:
                Message msg = new Message();
                for (int i = 0; i < iterations; i++)
                    encode(msg);

                break;
            case 2:
                MessageBuilder mb = new ByteBufferMessageBuilder(256, 2);
                RawMessageAssembler asm = new RawMessageAssembler(FixVersion.FIX44, 256, RealTimeSource.INSTANCE);
                SessionID sessionID = new SessionIDBean(SENDER_COMP_ID, SENDER_SUB_ID, TARGET_COMP_ID, TARGET_SUB_ID);
                NullOutputChannel out = new NullOutputChannel();
                for (int i = 0; i < iterations; i++)
                    encode(mb, asm, sessionID, out);

                System.out.println(out.toString());
                break;

        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static class NullOutputChannel implements OutputChannel {

        private int something;

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            this.something += length + offset + buffer.length; // just to trick optimizer
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public String toString() {
            return "Something:" + something;
        }

    }

}
