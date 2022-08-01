package org.ethereum.db;

import co.rsk.core.types.ints.Uint24;
import co.rsk.crypto.Keccak256;
import co.rsk.db.MutableTrieCache;
import co.rsk.storagerent.RentedNode;
import co.rsk.trie.MutableTrie;
import com.google.common.annotations.VisibleForTesting;
import org.ethereum.core.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static co.rsk.trie.Trie.NO_RENT_TIMESTAMP;
import static org.ethereum.db.OperationType.*;

public class MutableRepositoryTracked extends MutableRepository {

    // a set to track all the used trie-value-containing nodes in this repository (and its children repositories)
    private Set<RentedNode> trackedNodes;
    // a list of nodes tracked nodes that were rolled back (due to revert or OOG)
    private List<RentedNode> rollbackNodes;
    // parent repository to commit tracked nodes
    private MutableRepositoryTracked parentRepository;

    // default constructor
    protected MutableRepositoryTracked(MutableTrie mutableTrie, MutableRepositoryTracked parentRepository,
                                       Set<RentedNode> trackedNodes, List<RentedNode> rollbackNodes){
        super(mutableTrie);
        this.parentRepository = parentRepository;
        this.trackedNodes = trackedNodes;
        this.rollbackNodes = rollbackNodes;
    }

    // creates a tracked repository, all the child repositories (created with startTracking()) will also be tracked.
    // this should be only called from RepositoryLocator.trackedRepository
    public static MutableRepositoryTracked trackedRepository(MutableTrie mutableTrieCache) {
        return new MutableRepositoryTracked(mutableTrieCache, null, new HashSet<>(), new ArrayList<>());
    }

    @Override
    public synchronized Repository startTracking() {
        MutableRepositoryTracked mutableRepositoryTracked = new MutableRepositoryTracked(
            new MutableTrieCache(this.mutableTrie),
            this,
            new HashSet<>(),
            new ArrayList<>()
        );

        return mutableRepositoryTracked;
    }

    @Override
    public synchronized void commit() {
        super.commit();

        if(this.parentRepository != null) {
            this.parentRepository.mergeTrackedNodes(this.trackedNodes);
            this.parentRepository.addRollbackNodes(this.rollbackNodes);
        }
    }

    @Override
    public synchronized void rollback() {
        super.rollback();

        if(parentRepository != null) {
            this.parentRepository.addRollbackNodes(this.trackedNodes);
            this.trackedNodes.clear();
            this.rollbackNodes.clear();
        }
    }

    @VisibleForTesting
    public Set<RentedNode> getTrackedNodes() {
        return this.trackedNodes;
    }

    public List<RentedNode> getRollBackNodes() {
        return this.rollbackNodes.stream()
                .filter(trackedNode -> trackedNode.useForStorageRent())
                .collect(Collectors.toList());
    }

    /**
     * Fills up an already tracked node by providing the rent timestamp and the node size.
     *
     * @param trackedNode an already tracked node, this node doens't contains the relevant data to collect rent
     * @return a new fulfilled RentedNode ready to use for storage rent payment
     * */
    public RentedNode fillUpRentedNode(RentedNode trackedNode) {
        byte[] key = trackedNode.getKey().getData();

        // if we reach here, it will always get timestamp/valueLength from an existing key

        Long nodeSize = Long.valueOf(this.mutableTrie.getValueLength(key).intValue());
        Optional<Long> rentTimestamp = this.mutableTrie.getRentTimestamp(key);
        long lastRentPaidTimestamp = rentTimestamp.isPresent() ? rentTimestamp.get() : NO_RENT_TIMESTAMP;

        RentedNode rentedNode = new RentedNode(trackedNode.getKey(), trackedNode.getOperationType(),
                trackedNode.getNodeExistsInTrie(), nodeSize, lastRentPaidTimestamp);

        return rentedNode;
    }

    public void updateRents(Set<RentedNode> rentedNodes, long executionBlockTimestamp) {
        rentedNodes.forEach(node -> {
            long updatedRentTimestamp = node.getUpdatedRentTimestamp(executionBlockTimestamp);

            this.mutableTrie.putRentTimestamp(node.getKey().getData(), updatedRentTimestamp);
        });
    }

    public Set<RentedNode> getStorageRentNodes() {
        Map<ByteArrayWrapper, RentedNode> storageRentNodes = new HashMap<>();
        this.trackedNodes.stream()
                .filter(trackedNode -> trackedNode.useForStorageRent())
                .forEach(trackedNode -> {
                    ByteArrayWrapper key = new ByteArrayWrapper(trackedNode.getKey().getData());
                    RentedNode containedNode = storageRentNodes.get(key);

                    boolean isContainedNode = containedNode != null;
                    if(isContainedNode) {
                        long notRelevant = -1;
                        RentedNode nodeToBeReplaced = new RentedNode(containedNode.getKey(),
                                containedNode.getOperationType(),
                                containedNode.getNodeExistsInTrie(), notRelevant, notRelevant);
                        RentedNode newNode = new RentedNode(trackedNode.getKey(), trackedNode.getOperationType(),
                                trackedNode.getNodeExistsInTrie(),
                                notRelevant, notRelevant);
                        if(shouldBeReplaced(nodeToBeReplaced, newNode)) {
                            storageRentNodes.put(key, trackedNode);
                        }
                    } else {
                        storageRentNodes.put(key, trackedNode);
                    }
                });

        return new HashSet<>(storageRentNodes.values());
    }

    /**
     * Determines if a node should be replaced by another one due to different operation types,
     * the operation with the lowest threshold it's the one that leads the storage rent payment.
     * */
    public boolean shouldBeReplaced(RentedNode nodeToBeReplaced, RentedNode newNode) {
        return newNode.rentThreshold() < nodeToBeReplaced.rentThreshold();
    }

    // Internal methods contains node tracking

    @Override
    protected void internalPut(byte[] key, byte[] value) {
        super.internalPut(key, value);
        if(value == null) {
            trackNodeDeleteOperation(key);
        } else {
            trackNodeWriteOperation(key);
        }
    }

    @Override
    protected void internalDeleteRecursive(byte[] key) {
        super.internalDeleteRecursive(key);
        trackNodeDeleteOperation(key);
    }

    @Override
    protected byte[] internalGet(byte[] key, boolean readsContractCode) {
        byte[] value = super.internalGet(key, readsContractCode);
        boolean nodeExistsInTrie = value != null;

        if(readsContractCode) {
            trackNodeReadContractOperation(key, nodeExistsInTrie);
        } else {
            trackNodeReadOperation(key, nodeExistsInTrie);
        }

        return value;
    }

    @Override
    protected Optional<Keccak256> internalGetValueHash(byte[] key) {
        Optional<Keccak256> valueHash = super.internalGetValueHash(key);

        trackNodeReadOperation(key, valueHash.isPresent());

        return valueHash;
    }

    @Override
    protected Uint24 internalGetValueLength(byte[] key) {
        Uint24 valueLength = super.internalGetValueLength(key);

        trackNodeReadOperation(key, valueLength != Uint24.ZERO);

        return valueLength;
    }

    protected void trackNodeWriteOperation(byte[] key) {
        trackNode(key, WRITE_OPERATION, true);
    }

    protected void trackNodeDeleteOperation(byte[] key) {
        trackNode(key, DELETE_OPERATION, true);
    }

    protected void trackNodeReadOperation(byte[] key, boolean result) {
        trackNode(key, READ_OPERATION, result);
    }

    protected void trackNodeReadContractOperation(byte[] key, boolean nodeExistsInTrie) {
        trackNode(key, READ_CONTRACT_CODE_OPERATION, nodeExistsInTrie);
    }

    protected void trackNode(byte[] key, OperationType operationType, boolean nodeExistsInTrie) {
            RentedNode trackedNode = new RentedNode(
                    new ByteArrayWrapper(key),
                    operationType,
                    nodeExistsInTrie
            );
            this.trackedNodes.add(trackedNode);
    }

    private void mergeTrackedNodes(Set<RentedNode> trackedNodes) {
        this.trackedNodes.addAll(trackedNodes);
    }

    private void addRollbackNodes(Collection<RentedNode> trackedNodes) {
        this.rollbackNodes.addAll(trackedNodes);
    }

    public void clearTrackedNodes() {
        this.trackedNodes = new HashSet<>();
        this.rollbackNodes = new ArrayList<>();
    }
}
