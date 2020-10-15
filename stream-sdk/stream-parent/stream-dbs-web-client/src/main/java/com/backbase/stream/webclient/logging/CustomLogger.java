package com.backbase.stream.webclient.logging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.ByteBufFormat;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import static io.netty.buffer.ByteBufUtil.appendPrettyHexDump;
import static io.netty.util.internal.PlatformDependent.allocateUninitializedArray;
import static io.netty.util.internal.StringUtil.NEWLINE;
import static java.lang.Math.max;
import static java.nio.charset.Charset.defaultCharset;

public class CustomLogger  extends LoggingHandler {
    private static final LogLevel DEFAULT_LEVEL = LogLevel.DEBUG;

    protected final InternalLogger logger;
    protected final InternalLogLevel internalLevel;
    protected final InternalLogLevel eventLogLevel = InternalLogLevel.TRACE;

    protected final boolean writeRequestToFile;
    protected final boolean writeResponseToFile;


    private final LogLevel level;
    private final ByteBufFormat byteBufFormat;

    /**
     * Creates a new instance with the specified logger name and with hex dump
     * enabled.
     *
     * @param clazz the class type to generate the logger for
     */
    public CustomLogger(Class<?> clazz) {
        this(clazz, DEFAULT_LEVEL);
    }

    /**
     * Creates a new instance with the specified logger name.
     *
     * @param clazz the class type to generate the logger for
     * @param level the log level
     */
    public CustomLogger(Class<?> clazz, LogLevel level) {
        this(clazz, level, ByteBufFormat.HEX_DUMP);
    }

    /**
     * Creates a new instance with the specified logger name.
     *
     * @param clazz the class type to generate the logger for
     * @param level the log level
     * @param byteBufFormat the ByteBuf format
     */
    public CustomLogger(Class<?> clazz, LogLevel level, ByteBufFormat byteBufFormat) {
        ObjectUtil.checkNotNull(clazz, "clazz");
        this.level = ObjectUtil.checkNotNull(level, "level");
        this.byteBufFormat = ObjectUtil.checkNotNull(byteBufFormat, "byteBufFormat");
        logger = InternalLoggerFactory.getInstance(clazz);
        internalLevel = level.toInternalLevel();
        this.writeRequestToFile = false;
        this.writeResponseToFile = false;
    }


    /**
     * Returns the {@link LogLevel} that this handler uses to log
     */
    public LogLevel level() {
        return level;
    }

    /**
     * Returns the {@link ByteBufFormat} that this handler uses to log
     */
    public ByteBufFormat byteBufFormat() {
        return byteBufFormat;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "REGISTERED"));
        }
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "UNREGISTERED"));
        }
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "ACTIVE"));
        }
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "INACTIVE"));
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "EXCEPTION", cause), cause);
        }
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "USER_EVENT", evt));
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "BIND", localAddress));
        }
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(
        ChannelHandlerContext ctx,
        SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "CONNECT", remoteAddress, localAddress));
        }
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "DISCONNECT"));
        }
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "CLOSE"));
        }
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "DEREGISTER"));
        }
        ctx.deregister(promise);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "READ COMPLETE"));
        }
        ctx.fireChannelReadComplete();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "READ", msg));
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, "\n" + format(ctx, "WRITE", msg));
        }
        ctx.write(msg, promise);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (logger.isEnabled(eventLogLevel)) {
            logger.log(internalLevel, format(ctx, "WRITABILITY CHANGED"));
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * Formats an event and returns the formatted message.
     *
     * @param eventName the name of the event
     */
    protected String format(ChannelHandlerContext ctx, String eventName) {
        String chStr = ctx.channel().toString();
        return new StringBuilder(chStr.length() + 1 + eventName.length())
            .append(chStr)
            .append(' ')
            .append(eventName)
            .toString();
    }

//    /**
//     * Formats an event and returns the formatted message.
//     *
//     * @param eventName the name of the event
//     * @param arg       the argument of the event
//     */
//    protected String format(ChannelHandlerContext ctx, String eventName, Object arg) {
//        if (arg instanceof ByteBuf) {
//            return formatByteBuf(ctx, eventName, (ByteBuf) arg);
//        } else if (arg instanceof ByteBufHolder) {
//            return formatByteBufHolder(ctx, eventName, (ByteBufHolder) arg);
//        } else {
//            return formatSimple(ctx, eventName, arg);
//        }
//    }


    @Override
    protected String format(ChannelHandlerContext ctx, String event, Object arg) {
        if (arg instanceof ByteBuf) {
            ByteBuf msg = (ByteBuf) arg;
            return decode(msg, msg.readerIndex(), msg.readableBytes(), defaultCharset());
        }
        return super.format(ctx, event, arg);
    }

    /**
     * Formats an event and returns the formatted message.  This method is currently only used for formatting
     * {@link ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}.
     *
     * @param eventName the name of the event
     * @param firstArg  the first argument of the event
     * @param secondArg the second argument of the event
     */
    protected String format(ChannelHandlerContext ctx, String eventName, Object firstArg, Object secondArg) {
        if (secondArg == null) {
            return formatSimple(ctx, eventName, firstArg);
        }

        String chStr = ctx.channel().toString();
        String arg1Str = String.valueOf(firstArg);
        String arg2Str = secondArg.toString();
        StringBuilder buf = new StringBuilder(
            chStr.length() + 1 + eventName.length() + 2 + arg1Str.length() + 2 + arg2Str.length());
        buf.append(chStr).append(' ').append(eventName).append(": ").append(arg1Str).append(", ").append(arg2Str);
        return buf.toString();
    }

    /**
     * Generates the default log message of the specified event whose argument is a {@link ByteBuf}.
     */
    private String formatByteBuf(ChannelHandlerContext ctx, String eventName, ByteBuf msg) {
        String chStr = ctx.channel().toString();
        int length = msg.readableBytes();
        if (length == 0) {
            StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 4);
            buf.append(chStr).append(' ').append(eventName).append(": 0B");
            return buf.toString();
        } else {
            int outputLength = chStr.length() + 1 + eventName.length() + 2 + 10 + 1;
            if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
                int rows = length / 16 + (length % 15 == 0? 0 : 1) + 4;
                int hexDumpLength = 2 + rows * 80;
                outputLength += hexDumpLength;
            }
            StringBuilder buf = new StringBuilder(outputLength);
            buf.append(chStr).append(' ').append(eventName).append(": ").append(length).append('B');
            if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
                buf.append(NEWLINE);
                appendPrettyHexDump(buf, msg);
            }

            return buf.toString();
        }
    }

    /**
     * Generates the default log message of the specified event whose argument is a {@link ByteBufHolder}.
     */
    private String formatByteBufHolder(ChannelHandlerContext ctx, String eventName, ByteBufHolder msg) {
        String chStr = ctx.channel().toString();
        String msgStr = msg.toString();
        ByteBuf content = msg.content();
        int length = content.readableBytes();
        if (length == 0) {
            StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length() + 4);
            buf.append(chStr).append(' ').append(eventName).append(", ").append(msgStr).append(", 0B");
            return buf.toString();
        } else {
            int outputLength = chStr.length() + 1 + eventName.length() + 2 + msgStr.length() + 2 + 10 + 1;
            if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
                int rows = length / 16 + (length % 15 == 0? 0 : 1) + 4;
                int hexDumpLength = 2 + rows * 80;
                outputLength += hexDumpLength;
            }
            StringBuilder buf = new StringBuilder(outputLength);
            buf.append(chStr).append(' ').append(eventName).append(": ")
                .append(msgStr).append(", ").append(length).append('B');
            if (byteBufFormat == ByteBufFormat.HEX_DUMP) {
                buf.append(NEWLINE);
                appendPrettyHexDump(buf, content);
            }

            return buf.toString();
        }
    }

    /**
     * Generates the default log message of the specified event whose argument is an arbitrary object.
     */
    private static String formatSimple(ChannelHandlerContext ctx, String eventName, Object msg) {
        String chStr = ctx.channel().toString();
        String msgStr = String.valueOf(msg);
        StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + msgStr.length());
        return buf.append(chStr).append(' ').append(eventName).append(": ").append(msgStr).toString();
    }


    private String decode(ByteBuf src, int readerIndex, int len, Charset charset) {
        if (len != 0) {
            byte[] array;
            int offset;
            if (src.hasArray()) {
                array = src.array();
                offset = src.arrayOffset() + readerIndex;
            } else {
                array = allocateUninitializedArray(max(len, 1024));
                offset = 0;
                src.getBytes(readerIndex, array, 0, len);
            }
            return new String(array, offset, len, charset);
        }
        return "";
    }
}
