package org.jreactive.iso8583.netty.pipeline;

import com.solab.iso8583.IsoMessage;
import io.netty.channel.ChannelHandlerContext;
import org.jreactive.iso8583.IsoMessageListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CompositeIsoMessageHandlerTest {

    @Mock
    IsoMessageListener<IsoMessage> listener1;
    @Mock
    IsoMessageListener<IsoMessage> listener2;
    @Mock
    IsoMessageListener<IsoMessage> listener3;
    @Mock
    IsoMessage message;
    @Mock
    ChannelHandlerContext ctx;
    private CompositeIsoMessageHandler<IsoMessage> handler = new CompositeIsoMessageHandler<>();

    @Before
    public void setUp() throws Exception {
        //noinspection unchecked
        handler.addListeners(listener1, listener2, listener3);
    }

    @Test
    public void testHandleWithAppropriateHandler() throws Exception {
        //given
        when(listener1.applies(message)).thenReturn(false);
        when(listener2.applies(message)).thenReturn(true);
        when(listener3.applies(message)).thenReturn(false);
        when(listener1.onMessage(ctx, message)).thenReturn(true);
        when(listener2.onMessage(ctx, message)).thenReturn(true);
        when(listener3.onMessage(ctx, message)).thenReturn(true);

        //when
        handler.channelRead(ctx, message);

        //then
        verify(listener1, never()).onMessage(ctx, message);
        verify(listener2).onMessage(ctx, message);
        verify(listener3, never()).onMessage(ctx, message);
    }

    @Test
    public void testStopProcessing() throws Exception {
        //given
        when(listener1.applies(message)).thenReturn(true);
        when(listener2.applies(message)).thenReturn(true);
        when(listener3.applies(message)).thenReturn(true);
        when(listener1.onMessage(ctx, message)).thenReturn(true);
        when(listener2.onMessage(ctx, message)).thenReturn(false);
        when(listener3.onMessage(ctx, message)).thenReturn(true);

        //when
        handler.channelRead(ctx, message);

        //then
        verify(listener1).onMessage(ctx, message);
        verify(listener2).onMessage(ctx, message);
        verify(listener3, never()).onMessage(ctx, message);
    }

    @Test
    public void testDontFailOnExceptionInFailsafeMode() throws Exception {
        //given
        handler = new CompositeIsoMessageHandler<>(false);
        //noinspection unchecked
        handler.addListeners(listener1, listener2, listener3);

        when(listener1.applies(message)).thenReturn(true);
        when(listener2.applies(message)).thenThrow(new RuntimeException("Expected"));
        when(listener3.applies(message)).thenReturn(true);
        when(listener1.onMessage(ctx, message)).thenReturn(true);
        when(listener2.onMessage(ctx, message)).thenReturn(true);
        when(listener3.onMessage(ctx, message)).thenReturn(true);

        // when
        handler.channelRead(ctx, message);

        //then
        verify(listener1).onMessage(ctx, message);
        verify(listener2, never()).onMessage(ctx, message);
        verify(listener3).onMessage(ctx, message);
    }
}