package co.rsk.net.messages;

import co.rsk.blockchain.utils.BlockGenerator;
import org.ethereum.core.Block;
import org.ethereum.util.RLP;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SnapStateChunkRequestMessageTest {

    @Test
    void getMessageType_returnCorrectMessageType() {
        //given
        Block block = new BlockGenerator().getBlock(1);
        long id4Test = 42L;
        SnapStateChunkRequestMessage message = new SnapStateChunkRequestMessage(id4Test, block.getNumber(), 0L, 0L);

        //when
        MessageType messageType = message.getMessageType();

        //then
        assertThat(messageType).isEqualTo(MessageType.STATE_CHUNK_REQUEST_MESSAGE);
    }
    @Test
    void givenParameters4Test_assureExpectedValues() {
        //given
        Block block = new BlockGenerator().getBlock(1);
        long id4Test = 42L;
        long from = 5L;
        long chunkSize = 10L;

        //when
        SnapStateChunkRequestMessage message = new SnapStateChunkRequestMessage(id4Test, block.getNumber(), from, chunkSize);

        //then
        assertThat(message).extracting(SnapStateChunkRequestMessage::getId,
                SnapStateChunkRequestMessage::getBlockNumber, SnapStateChunkRequestMessage::getFrom, SnapStateChunkRequestMessage::getChunkSize)
                .containsExactly(id4Test, block.getNumber(), from, chunkSize);
    }


    @Test
    void getEncodedMessageWithoutId_returnExpectedByteArray() {
        //given
        long blockNumber = 1L;
        long id4Test = 42L;
        long from = 1L;
        long chunkSize = 20L;
        byte[] expectedEncodedMessage = RLP.encodeList(
                RLP.encodeBigInteger(BigInteger.valueOf(blockNumber)),
                RLP.encodeBigInteger(BigInteger.valueOf(from)),
                RLP.encodeBigInteger(BigInteger.valueOf(chunkSize)));

        SnapStateChunkRequestMessage message = new SnapStateChunkRequestMessage(id4Test, blockNumber, from, chunkSize);

        //when
        byte[] encodedMessage = message.getEncodedMessageWithoutId();

        //then
        assertThat(encodedMessage)
                .isEqualTo(expectedEncodedMessage);
    }

    @Test
    void getEncodedMessageWithId_returnExpectedByteArray() {
        //given
        long blockNumber = 1L;
        long id4Test = 42L;
        long from = 1L;
        long chunkSize = 20L;

        SnapStateChunkRequestMessage message = new SnapStateChunkRequestMessage(id4Test, blockNumber, from, chunkSize);
        byte[] expectedEncodedMessage = RLP.encodeList(
                RLP.encodeBigInteger(BigInteger.valueOf(id4Test)), message.getEncodedMessageWithoutId());

        //when
        byte[] encodedMessage = message.getEncodedMessage();

        //then
        assertThat(encodedMessage)
                .isEqualTo(expectedEncodedMessage);
    }

    @Test
    void givenAcceptIsCalled_messageVisitorIsAppliedForMessage() {
        //given
        long someId = 42;
        SnapStateChunkRequestMessage message = new SnapStateChunkRequestMessage(someId, 0L, 0L, 0L);
        MessageVisitor visitor = mock(MessageVisitor.class);

        //when
        message.accept(visitor);

        //then
        verify(visitor, times(1)).apply(message);
    }
}
