package co.rsk.net;

import co.rsk.net.messages.StateChunkRequestMessage;
import co.rsk.net.messages.StateChunkResponseMessage;
import co.rsk.trie.TrieDTO;
import co.rsk.trie.TrieDTOInOrderIterator;
import co.rsk.trie.TrieDTOInOrderRecoverer;
import co.rsk.trie.TrieStore;
import co.rsk.util.HexUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public class SnapshotProcessor {

    private static final Logger logger = LoggerFactory.getLogger("snapshotprocessor");
    private static final String KBYTES = "kbytes";
    private static final int UNCOMPRESSED_FLAG = -1;

    private final Blockchain blockchain;
    private final TrieStore trieStore;
    private final int chunkSize;
    private final String chunkSizeType;
    private final PeersInformation peersInformation;

    private final boolean isCompressionEnabled;

    private long messageId = 0;
    private boolean enabled = false;
    private BigInteger stateSize = BigInteger.ZERO;
    private BigInteger stateChunkSize = BigInteger.ZERO;
    private SnapSyncState snapSyncState;
    private List<byte[]> elements;

    public SnapshotProcessor(Blockchain blockchain,
            TrieStore trieStore,
            PeersInformation peersInformation,
            int chunkSize, String chunkSizeType,
            boolean isCompressionEnabled) {
        this.blockchain = blockchain;
        this.trieStore = trieStore;
        this.peersInformation = peersInformation;
        this.chunkSize = chunkSize;
        this.chunkSizeType = chunkSizeType;
        this.iterators = Maps.newConcurrentMap();
        this.isCompressionEnabled = isCompressionEnabled;
        this.elements = Lists.newArrayList();
    }

    public void startSyncing(List<Peer> peers, SnapSyncState snapSyncState) {
        // TODO(snap-poc) temporary hack, code in this should be moved to SnapSyncState probably
        this.snapSyncState = snapSyncState;

        this.stateSize = BigInteger.ZERO;
        this.stateChunkSize = BigInteger.ZERO;

        // TODO(snap-poc) deal with multiple peers algorithm here
        Peer peer = peers.get(0);

        long peerBestBlock = peersInformation.getPeer(peer).getStatus().getBestBlockNumber();
        requestState(peer, 0L, 5544285l);
    }

    // TODO(snap-poc) should be called on errors too
    private void stopSyncing() {
        this.stateSize = BigInteger.ZERO;
        this.stateChunkSize = BigInteger.ZERO;

        this.snapSyncState.finished();
    }

    public void processStateChunkResponse(Peer peer, StateChunkResponseMessage message) {
        peersInformation.getOrRegisterPeer(peer);

        snapSyncState.newChunk();

        final RLPList trieElements = RLP.decodeList(message.getChunkOfTrieKeyValue());
        logger.debug(
                "Received state chunk of {} elements ({} bytes).",
                trieElements.size(),
                message.getChunkOfTrieKeyValue().length
        );

        // TODO(snap-poc) do whatever it's needed, reading just to check load
        for (int i = 0; i < trieElements.size(); i++) {
            final RLPList trieElement = (RLPList) trieElements.get(i);
            final byte[] key = trieElement.get(0).getRLPData();
            final int rawSize = ByteUtil.byteArrayToInt(trieElement.get(2).getRLPData());
            byte[] value = trieElement.get(1).getRLPData();

            boolean isCompressed = rawSize != UNCOMPRESSED_FLAG;
            if (isCompressed) {
                value = decompressLz4(value, rawSize);
            }

            if (logger.isTraceEnabled()) {
                final String keyString = ByteUtil.toHexString(key);
                final String valueString = value == null ? "null" : ByteUtil.toHexString(value);
                logger.trace("State chunk received - Key: {}, Value: {}", keyString, valueString);
            }
        }

        this.stateSize = this.stateSize.add(BigInteger.valueOf(trieElements.size()));
        this.stateChunkSize = this.stateChunkSize.add(BigInteger.valueOf(message.getChunkOfTrieKeyValue().length));
        for (int i = 0; i < trieElements.size(); i++) {
            this.elements.add(trieElements.get(i).getRLPData());
        }
        logger.debug("State progress: {} chunks ({} bytes)", this.stateSize.toString(), this.stateChunkSize.toString());
        if (!message.isComplete()) {
            // request another chunk
            requestState(peer, message.getTo(), message.getBlockNumber());
        } else {
            logger.debug("State Completed! {} chunks ({} bytes)", this.stateSize.toString(), this.stateChunkSize.toString());
            logger.debug("Mapping elements...");
            final TrieDTO[] nodeArray = this.elements.stream().map(TrieDTO::decodeFromSync).toArray(TrieDTO[]::new);
            logger.debug("Recovering trie...");
            Optional<TrieDTO> result = TrieDTOInOrderRecoverer.recoverTrie(nodeArray);
            logger.debug("Recovered root: {}", result.get().calculateHash());
            logger.debug("Starting again the infinite loop!");
            this.elements = Lists.newArrayList();
            this.stateSize = BigInteger.ZERO;
            this.stateChunkSize = BigInteger.ZERO;
            requestState(peer, 0l, 5544285l);
        }
    }

    public void processStateChunkRequest(Peer sender, StateChunkRequestMessage request) {
        long startChunk = System.currentTimeMillis();

        logger.debug("Processing state chunk request from node {}", sender.getPeerNodeID());

        Long blockNumber = request.getBlockNumber() > 0L ? request.getBlockNumber() : blockchain.getBestBlock().getNumber() - 10;

        List<byte[]> trieEncoded = new ArrayList<>();
        Block block = blockchain.getBlockByNumber(blockNumber);
        final long to = request.getFrom() + (request.getChunkSize() * 1024);
        TrieDTOInOrderIterator it = new TrieDTOInOrderIterator(trieStore, block.getStateRoot(), request.getFrom(), to);

        long rawSize = 0L;
        long compressedSize = 0L;

        long i = KBYTES.equals(this.chunkSizeType)? 0l : request.getFrom();
        long limit = KBYTES.equals(this.chunkSizeType)? chunkSize * 1024 : i + chunkSize;

        long totalCompressingTime = 0L;

        while (it.hasNext() && i < limit) {
            IterationElement e = it.next();
            if (logger.isTraceEnabled()) {
                logger.trace("Single node read.");
            }
            byte[] key = e.getNodeKey().encode();
            byte[] value = e.getNode().getValue();
            byte[] effectiveValue = value;
            int uncompressedSizeParam = UNCOMPRESSED_FLAG;
            if (value != null && isCompressionEnabled) {
                rawSize += value.length;

                long startCompress = System.currentTimeMillis();
                byte[] compressedValue = compressLz4(value);
                long totalCompress = System.currentTimeMillis() - startCompress;
                totalCompressingTime += totalCompress;

                validateCompression(key, value, compressedValue);

                boolean couldCompress = compressedValue.length < value.length;
                if (couldCompress) {
                    compressedSize += compressedValue.length;
                    uncompressedSizeParam = value.length;
                } else {
                    compressedSize += value.length;
                }

                effectiveValue = compressedValue;
            }

            final byte[] element = RLP.encodeList(RLP.encodeElement(key), RLP.encodeElement(effectiveValue), RLP.encodeInt(uncompressedSizeParam));
            trieEncoded.add(element);

            if (logger.isTraceEnabled()) {
                logger.trace("Single node calculated.");
            }

            i = KBYTES.equals(this.chunkSizeType) ? i + element.length : i + 1;
        }

        byte[] chunkBytes = RLP.encodeList(trieEncoded.toArray(new byte[0][0]));
        StateChunkResponseMessage responseMessage = new StateChunkResponseMessage(request.getId(), chunkBytes, blockNumber, request.getFrom(), !it.hasNext());

        long totalChunkTime = System.currentTimeMillis() - startChunk;

        double compressionFactor = (double) rawSize / (double) compressedSize;

        logger.debug("Sending state chunk of {} bytes to node {}, compressing time {}ms, totalTime {}ms, compressionFactor {}", chunkBytes.length, sender.getPeerNodeID(), totalCompressingTime, totalChunkTime, compressionFactor);
        sender.sendMessage(responseMessage);
    }

    private static void validateCompression(byte[] key, byte[] value, byte[] compressedValue) {
        // TODO(snap-poc) remove this when finishing with the compression validations
        if (logger.isTraceEnabled()) {
            if (Arrays.equals(decompressLz4(compressedValue, value.length), value)) {
                logger.trace("===== compressed value is equal to original value for key {}", ByteUtil.toHexString(key));
            } else {
                logger.trace("===== compressed value is different from original value for key {}", ByteUtil.toHexString(key));
            }
        }
    }

    private static byte[] compressLz4(byte[] src) {
        LZ4Factory lz4Factory = LZ4Factory.safeInstance();
        LZ4Compressor fastCompressor = lz4Factory.fastCompressor();
        int maxCompressedLength = fastCompressor.maxCompressedLength(src.length);
        byte[] dst = new byte[maxCompressedLength];
        int compressedLength = fastCompressor.compress(src, 0, src.length, dst, 0, maxCompressedLength);
        return Arrays.copyOf(dst, compressedLength);
    }

    private static byte[] decompressLz4(byte[] src, int expandedSize) {
        LZ4SafeDecompressor decompressor = LZ4Factory.safeInstance().safeDecompressor();
        byte[] dst = new byte[expandedSize];
        decompressor.decompress(src, dst);
        return  dst;
    }

    private void requestState(Peer peer, long from, long blockNumber) {
        logger.debug("Requesting state chunk to node {} - block {} - from {}", peer.getPeerNodeID(), blockNumber, from);
        StateChunkRequestMessage message = new StateChunkRequestMessage(messageId++, blockNumber, from, chunkSize);
        peer.sendMessage(message);
    }

}
