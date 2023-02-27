/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.core.parallel;

import co.rsk.config.TestSystemProperties;
import co.rsk.test.World;
import co.rsk.test.dsl.DslParser;
import co.rsk.test.dsl.DslProcessorException;
import co.rsk.test.dsl.WorldDslProcessor;
import com.typesafe.config.ConfigValueFactory;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.db.ReceiptStore;
import org.ethereum.db.ReceiptStoreImpl;
import org.ethereum.vm.GasCost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParallelExecutionStateTest {
    private World createWorld(String dsl, int rskip144) throws DslProcessorException {
        ReceiptStore receiptStore = new ReceiptStoreImpl(new HashMapDB());
        TestSystemProperties config = new TestSystemProperties(rawConfig ->
                rawConfig.withValue("blockchain.config.consensusRules.rskip144", ConfigValueFactory.fromAnyRef(rskip144))
        );

        World world = new World(receiptStore, config);

        DslParser parser = new DslParser(dsl);
        WorldDslProcessor processor = new WorldDslProcessor(world);
        processor.processCommands(parser);

        return world;
    }

    private byte[] getStateRoot (World world) {
        return world.getBlockChain().getBestBlock().getHeader().getStateRoot();
    }

    /**
     * compares the state root of a blockchain with and without rskip 144
     * @param dsl the dsl string of the blockchain
     * @param expectedEdges the tx execution edges to assert equals
     * @throws DslProcessorException
     */
    private void testProcessingPreAndPostRSKIP144(String dsl, short[] expectedEdges) throws DslProcessorException {
        World parallel = this.createWorld(dsl, 0);
        World series = this.createWorld(dsl, -1);

        Assertions.assertArrayEquals(
                this.getStateRoot(series),
                this.getStateRoot(parallel)
        );

        Assertions.assertArrayEquals(expectedEdges, parallel.getBlockChain().getBestBlock().getHeader().getTxExecutionSublistsEdges());
    }

    @Test
    void empty() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144("block_chain g00", null);
    }

    @Test
    void oneTx() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144("account_new acc1 10000000\n" +
                "account_new acc2 0\n" +
                "\n" +
                "transaction_build tx01\n" +
                "    sender acc1\n" +
                "    receiver acc2\n" +
                "    value 1000\n" +
                "    build\n" +
                "\n" +
                "block_build b01\n" +
                "    parent g00\n" +
                "    transactions tx01\n" +
                "    build\n" +
                "\n" +
                "block_connect b01\n" +
                "\n" +
                "assert_best b01\n" +
                "assert_tx_success tx01\n" +
                "assert_balance acc2 1000\n" +
                "\n", new short[]{ 1 });
    }

    /**
     * // SPDX-License-Identifier: UNLICENSED
     * pragma solidity ^0.8.9;
     *
     * contract ReadWrite {
     *     uint x;
     *     uint another;
     *
     *     receive() external payable {}
     *     function read() external { another = x; }
     *     function write(uint value) external { x = value; }
     *     function update(uint increment) external { x += increment; }
     *
     *     function readWithRevert() external { another = x; revert(); }
     *     function writeWithRevert(uint value) external { x = value; revert(); }
     *     function updateWithRevert(uint increment) external { x += increment; revert(); }
     *
     *     mapping (uint => uint) r;
     *
     *     function wasteGas(uint gas, uint writeToX, uint writeToY) external {
     *         uint i = uint(keccak256(abi.encode(0x12349876)));
     *         r[writeToX] = i;
     *         r[writeToY] = i;
     *         uint gasLeft = gasleft();
     *         while (gasLeft < gas + gasleft()) {
     *             unchecked {
     *                 i = (i / 7 + 10) * 8;
     *             }
     *         }
     *     }
     * }
     */

    private final String creationData = "608060405234801561001057600080fd5b50610473806100206000396000f3fe6080604052600436106100745760003560e01c806334b091931161004e57806334b09193146100e957806357de26a41461011257806382ab890a14610129578063e2033a13146101525761007b565b80630d2a2d8d146100805780631b892f87146100975780632f048afa146100c05761007b565b3661007b57005b600080fd5b34801561008c57600080fd5b5061009561017b565b005b3480156100a357600080fd5b506100be60048036038101906100b991906102bb565b610188565b005b3480156100cc57600080fd5b506100e760048036038101906100e291906102bb565b6101a4565b005b3480156100f557600080fd5b50610110600480360381019061010b91906102e8565b6101ae565b005b34801561011e57600080fd5b5061012761024f565b005b34801561013557600080fd5b50610150600480360381019061014b91906102bb565b61025a565b005b34801561015e57600080fd5b50610179600480360381019061017491906102bb565b610275565b005b6000546001819055600080fd5b80600080828254610199919061036a565b925050819055600080fd5b8060008190555050565b600063123498766040516020016101c591906103f3565b6040516020818303038152906040528051906020012060001c905080600260008581526020019081526020016000208190555080600260008481526020019081526020016000208190555060005a90505b5a85610222919061036a565b811015610248576008600a6007848161023e5761023d61040e565b5b0401029150610216565b5050505050565b600054600181905550565b8060008082825461026b919061036a565b9250508190555050565b806000819055600080fd5b600080fd5b6000819050919050565b61029881610285565b81146102a357600080fd5b50565b6000813590506102b58161028f565b92915050565b6000602082840312156102d1576102d0610280565b5b60006102df848285016102a6565b91505092915050565b60008060006060848603121561030157610300610280565b5b600061030f868287016102a6565b9350506020610320868287016102a6565b9250506040610331868287016102a6565b9150509250925092565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b600061037582610285565b915061038083610285565b92508282019050808211156103985761039761033b565b5b92915050565b6000819050919050565b600063ffffffff82169050919050565b6000819050919050565b60006103dd6103d86103d38461039e565b6103b8565b6103a8565b9050919050565b6103ed816103c2565b82525050565b600060208201905061040860008301846103e4565b92915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601260045260246000fdfea264697066735822122081578079990ee4f4eaa55ebeeedcb31b8f178ab346b989e62541a894e60d381164736f6c63430008110033";

    // call data
    // write(10)
    private final String writeTen = "2f048afa000000000000000000000000000000000000000000000000000000000000000a";
    // read()
    private final String readData = "57de26a4";
    // update(10)
    private final String updateByTen = "82ab890a000000000000000000000000000000000000000000000000000000000000000a";
    // writeWithRevert(10)
    private final String writeTenWithRevert = "0d2a2d8d";
    // readWithRevert()
    private final String readDataWithRevert = "e2033a13000000000000000000000000000000000000000000000000000000000000000a";
    // updateWithRevert(10)
    private final String updateByTenWithRevert = "1b892f87000000000000000000000000000000000000000000000000000000000000000a";
    // wasteGas(2000000, 0, 0)
    private final String wasteTwoMillionGas = "34b0919300000000000000000000000000000000000000000000000000000000001e848000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    // wasteGas(100000, 0, 0)
    private final String wasteHundredThousandGas = "34b0919300000000000000000000000000000000000000000000000000000000000186a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    // wasteGas(100000, 1, 1)
    private final String writeToX = "34b0919300000000000000000000000000000000000000000000000000000000000186a000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001";
    // wasteGas(100000, 2, 2)
    private final String writeToY = "34b0919300000000000000000000000000000000000000000000000000000000000186a000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002";
    // wasteGas(100000, 1, 2)
    private final String writeToXAndY = "34b0919300000000000000000000000000000000000000000000000000000000000186a000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002";
    // wasteGas(2000000, 1, 1)
    private final String writeToXWastingGas = "34b0919300000000000000000000000000000000000000000000000000000000001e848000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001";
    // wasteGas(2000000, 2, 2)
    private final String writeToYWastingGas = "34b0919300000000000000000000000000000000000000000000000000000000001e848000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002";
    // wasteGas(2000000, 1, 2)
    private final String writeToXAndYWastingGas = "34b0919300000000000000000000000000000000000000000000000000000000001e848000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002";

    // substrings for dsl text
    private final String createThreeAccounts = "account_new acc1 10000000\n" +
            "account_new acc2 10000000\n" +
            "account_new acc3 10000000\n" +
            "\n";

    private final String createContractInBlock01 =
            "transaction_build tx01\n" +
                    "    sender acc3\n" +
                    "    receiverAddress 00\n" +
                    "    data " + creationData + "\n" +
                    "    gas 1200000\n" +
                    "    build\n" +
                    "\n" +
                    "block_build b01\n" +
                    "    parent g00\n" +
                    "    transactions tx01\n" +
                    "    build\n" +
                    "\n" +
                    "block_connect b01\n" +
                    "\n";

    private final String buildBlockWithToTxs = "block_build b02\n" +
            "    parent b01\n" +
            "    transactions tx02 tx03\n" +
            "    build\n" +
            "\n" +
            "block_connect b02\n" +
            "\n" +
            "assert_best b02\n" +
            "\n";

    private final String validateTxs = "assert_tx_success tx01\n" +
            "assert_tx_success tx02\n" +
            "assert_tx_success tx03\n" +
            "\n";

    private String skeleton(String txs, boolean validate) {
        return createThreeAccounts +
                createContractInBlock01 +
                txs +
                buildBlockWithToTxs +
                (validate ? validateTxs : "");
    }

    private void createContractAndTestCallWith(String firstCall, String secondCall, short[] edges) throws DslProcessorException {
        createContractAndTestCallWith(firstCall, secondCall, edges, true);
    }

    /**
     * creates the contract, performs two calls from different accounts
     * tests the state root and the tx edges
     * @param firstCall call data for the first tx
     * @param secondCall call data for the second tx
     * @param edges expected tx edges
     * @param validate allows to prevent validating txs are successful
     * @throws DslProcessorException
     */
    private void createContractAndTestCallWith(String firstCall, String secondCall, short[] edges, boolean validate) throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144(skeleton(
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + firstCall + "\n" +
                "    gas 100000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + secondCall + "\n" +
                "    gas 100000\n" +
                "    build\n" +
                "\n", validate), edges);
    }

    // 1. A and B have the same sender account
    @Test
    void sameSender() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144("account_new acc1 10000000\n" +
                "account_new acc2 0\n" +
                "\n" +
                "transaction_build tx01\n" +
                "    sender acc1\n" +
                "    receiver acc2\n" +
                "    value 1000\n" +
                "    nonce 0\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    receiver acc2\n" +
                "    value 1000\n" +
                "    nonce 1\n" +
                "    build\n" +
                "\n" +
                "block_build b01\n" +
                "    parent g00\n" +
                "    transactions tx01 tx02\n" +
                "    build\n" +
                "\n" +
                "block_connect b01\n" +
                "\n" +
                "assert_best b01\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_balance acc2 2000\n" +
                "\n", new short[]{ 2 });
    }

    // 2. A and B transfer value to the same destination account
    @Test
    void sameRecipient() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144("account_new acc1 10000000\n" +
                "account_new acc2 10000000\n" +
                "account_new acc3 0\n" +
                "\n" +
                "transaction_build tx01\n" +
                "    sender acc1\n" +
                "    receiver acc3\n" +
                "    value 1000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx02\n" +
                "    sender acc2\n" +
                "    receiver acc3\n" +
                "    value 1000\n" +
                "    build\n" +
                "\n" +
                "block_build b01\n" +
                "    parent g00\n" +
                "    transactions tx01 tx02\n" +
                "    build\n" +
                "\n" +
                "block_connect b01\n" +
                "\n" +
                "assert_best b01\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_balance acc3 2000\n" +
                "\n", new short[]{ 2 });
    }

    // 3. A and B transfer value to the same smart contract
    @Test
    void sameContractRecipient() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144(createThreeAccounts +
                createContractInBlock01 +
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    value 1000\n" +
                "    gas 100000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    value 1000\n" +
                "    gas 100000\n" +
                "    build\n" +
                "\n"
                + buildBlockWithToTxs + validateTxs, new short[]{ 2 });
    }

    // 4. B reads a smart contract variable that A writes
    @Test
    void readWrite() throws DslProcessorException {
        this.createContractAndTestCallWith(writeTen, readData, new short[]{ 2 });
    }

    @Test
    void readWriteWithRevert() throws DslProcessorException {
        this.createContractAndTestCallWith(writeTen, readDataWithRevert, new short[]{ 2 }, false);
    }

    // 5. B reads a smart contract variable that A updates (i.e., +=)
    @Test
    void readUpdate() throws DslProcessorException {
        this.createContractAndTestCallWith(updateByTen, readData, new short[]{ 2 });
    }

    @Test
    void readUpdateWithRevert() throws DslProcessorException {
        this.createContractAndTestCallWith(updateByTen, readDataWithRevert, new short[]{ 2 }, false);
    }


    //6. B writes a smart contract variable that A writes
    @Test
    void writeWrite() throws DslProcessorException {
        this.createContractAndTestCallWith(writeTen, writeTen, new short[]{ 2 });
    }

    @Test
    void writeWriteWithRevert() throws DslProcessorException {
        this.createContractAndTestCallWith(writeTen, writeTenWithRevert, new short[]{ 2 }, false);
    }

    // 7. B writes a smart contract variable that A updates
    @Test
    void writeUpdate() throws DslProcessorException {
        this.createContractAndTestCallWith(updateByTen, writeTen, new short[]{ 2 });
    }

    @Test
    void writeUpdateWithRevert() throws DslProcessorException {
        this.createContractAndTestCallWith(updateByTen, writeTenWithRevert, new short[]{ 2 }, false);
    }

    // 8. B writes a smart contract variable that A reads
    @Test
    void writeRead() throws DslProcessorException {
        this.createContractAndTestCallWith(readData, writeTen, new short[]{ 2 });
    }

    @Test
    void writeReadWithRevert() throws DslProcessorException {
        this.createContractAndTestCallWith(readData, writeTenWithRevert, new short[]{ 2 }, false);
    }

    // 9. B updates a smart contract variable that A writes
    @Test
    void updateWrite() throws DslProcessorException {
        this.createContractAndTestCallWith(writeTen, updateByTen, new short[]{ 2 });
    }

    @Test
    void updateWriteWithRevert() throws DslProcessorException {
        this.createContractAndTestCallWith(writeTen, updateByTenWithRevert, new short[]{ 2 }, false);
    }

    // 10. B updates a smart contract variable that A reads
    @Test
    void updateRead() throws DslProcessorException {
        this.createContractAndTestCallWith(readData, updateByTen, new short[]{ 2 });
    }

    // 11. B updates a smart contract variable that A updates
    @Test
    void updateUpdate() throws DslProcessorException {
        this.createContractAndTestCallWith(updateByTen, updateByTen, new short[]{ 2 });
    }

    // 12. B calls a smart contract that A creates
    @Test
    void callCreatedContract() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144("account_new acc1 10000000\n" +
                "account_new acc2 10000000\n" +
                "\n" +
                "transaction_build tx01\n" +
                "    sender acc1\n" +
                "    receiverAddress 00\n" +
                "    data " + creationData + "\n" +
                "    gas 1200000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx02\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + writeTen + "\n" +
                "    gas 100000\n" +
                "    build\n" +
                "\n" +
                "block_build b01\n" +
                "    parent g00\n" +
                "    transactions tx01 tx02\n" +
                "    build\n" +
                "\n" +
                "block_connect b01\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "\n", new short[]{ 2 });
    }

    // 13. B transfers value to a smart contract that A creates
    @Test
    void sendToCreatedContract() throws DslProcessorException {
        this.testProcessingPreAndPostRSKIP144("account_new acc1 10000000\n" +
                "account_new acc2 10000000\n" +
                "\n" +
                "transaction_build tx01\n" +
                "    sender acc1\n" +
                "    receiverAddress 00\n" +
                "    data " + creationData + "\n" +
                "    gas 1200000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx02\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    value 10000\n" +
                "    gas 100000\n" +
                "    build\n" +
                "\n" +
                "block_build b01\n" +
                "    parent g00\n" +
                "    transactions tx01 tx02\n" +
                "    build\n" +
                "\n" +
                "block_connect b01\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_balance tx01 10000\n" +
                "\n", new short[]{ 2 });
    }

    // 2. A is in a parallel sublist without enough gas available: B is placed in the sequential sublist
    @Test
    void useSequentialForGas() throws DslProcessorException {
        World parallel = this.createWorld(skeleton("transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + wasteTwoMillionGas + "\n" +
                "    gas 2700000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + wasteTwoMillionGas + "\n" +
                "    gas 2500000\n" +
                "    build\n" +
                "\n", true), 0);

        Assertions.assertEquals(3000000L, GasCost.toGas(parallel.getBlockChain().getBestBlock().getHeader().getGasLimit()));
        Assertions.assertEquals(2, parallel.getBlockChain().getBestBlock().getTransactionsList().size());
        Assertions.assertArrayEquals(new short[] { 1 }, parallel.getBlockChain().getBestBlock().getHeader().getTxExecutionSublistsEdges());
    }

    // 3. A is in the sequential sublist: B is placed in the sequential sublist
    @Test
    void useSequentialForCollisionWithSequential() throws DslProcessorException {
        World parallel = this.createWorld(createThreeAccounts +
                createContractInBlock01 +
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + wasteTwoMillionGas + "\n" +
                "    gas 2500000\n" +
                "    nonce 0\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + wasteTwoMillionGas + "\n" +
                "    gas 2500000\n" +
                "    nonce 1\n" +
                "    build\n" +
                "\n" + // goes to sequential
                "transaction_build tx04\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + wasteHundredThousandGas + "\n" +
                "    gas 200000\n" +
                "    build\n" +
                "\n" +
                "block_build b02\n" +
                "    parent b01\n" +
                "    transactions tx02 tx03 tx04\n" +
                "    build\n" +
                "\n" +
                "block_connect b02\n" +
                "\n" +
                "assert_best b02\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_tx_success tx03\n" +
                "assert_tx_success tx04\n" +
                "\n", 0);

        Assertions.assertEquals(3000000L, GasCost.toGas(parallel.getBlockChain().getBestBlock().getHeader().getGasLimit()));
        Assertions.assertEquals(3, parallel.getBlockChain().getBestBlock().getTransactionsList().size());
        Assertions.assertArrayEquals(new short[] { 1 }, parallel.getBlockChain().getBestBlock().getHeader().getTxExecutionSublistsEdges());
    }

    // 1. A and B are in different parallel sublists with enough gas: C is placed in the sequential sublist
    @Test
    void useSequentialForCollisionWithTwoParallel() throws DslProcessorException {
        World parallel = this.createWorld(createThreeAccounts +
                "account_new acc4 10000000\n" +
                createContractInBlock01 +
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + writeToX + "\n" +
                "    gas 200000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + writeToY + "\n" +
                "    gas 200000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx04\n" +
                "    sender acc4\n" +
                "    contract tx01\n" +
                "    data " + writeToXAndY + "\n" +
                "    gas 200000\n" +
                "    build\n" +
                "\n" +
                "block_build b02\n" +
                "    parent b01\n" +
                "    transactions tx02 tx03 tx04\n" +
                "    build\n" +
                "\n" +
                "block_connect b02\n" +
                "\n" +
                "assert_best b02\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_tx_success tx03\n" +
                "assert_tx_success tx04\n" +
                "\n", 0);

        Assertions.assertEquals(3000000L, GasCost.toGas(parallel.getBlockChain().getBestBlock().getHeader().getGasLimit()));
        Assertions.assertEquals(3, parallel.getBlockChain().getBestBlock().getTransactionsList().size());
        Assertions.assertArrayEquals(new short[] { 1, 2 }, parallel.getBlockChain().getBestBlock().getHeader().getTxExecutionSublistsEdges());
    }

    // 2. A and B are in different parallel sublists without enough gas: C is placed in the sequential sublist
    @Test
    void useSequentialForCollisionWithTwoParallelWithoutGas() throws DslProcessorException {
        World parallel = this.createWorld(createThreeAccounts +
                "account_new acc4 10000000\n" +
                createContractInBlock01 +
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + writeToXWastingGas + "\n" +
                "    gas 2500000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + writeToYWastingGas + "\n" +
                "    gas 2500000\n" +
                "    build\n" +
                "\n" + // goes to sequential
                "transaction_build tx04\n" +
                "    sender acc4\n" +
                "    contract tx01\n" +
                "    data " + writeToXAndYWastingGas + "\n" +
                "    gas 2500000\n" +
                "    build\n" +
                "\n" +
                "block_build b02\n" +
                "    parent b01\n" +
                "    transactions tx02 tx03 tx04\n" +
                "    build\n" +
                "\n" +
                "block_connect b02\n" +
                "\n" +
                "assert_best b02\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_tx_success tx03\n" +
                "assert_tx_success tx04\n" +
                "\n", 0);

        Assertions.assertEquals(3000000L, GasCost.toGas(parallel.getBlockChain().getBestBlock().getHeader().getGasLimit()));
        Assertions.assertEquals(3, parallel.getBlockChain().getBestBlock().getTransactionsList().size());
        Assertions.assertArrayEquals(new short[] { 1, 2 }, parallel.getBlockChain().getBestBlock().getHeader().getTxExecutionSublistsEdges());
    }

    // 3. A is in a parallel sublist and B is in the sequential sublist: C is placed in the sequential sublist
    @Test
    void useSequentialForCollisionWithSequentialAndParallel() throws DslProcessorException {
        World parallel = this.createWorld(createThreeAccounts +
                "account_new acc4 10000000\n" +
                createContractInBlock01 +
                "transaction_build tx02\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + writeToXWastingGas + "\n" +
                "    gas 2500000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx03\n" +
                "    sender acc1\n" +
                "    contract tx01\n" +
                "    data " + writeToXWastingGas + "\n" +
                "    gas 2500000\n" +
                "    nonce 1\n" +
                "    build\n" +
                "\n" + // goes to sequential
                "transaction_build tx04\n" +
                "    sender acc2\n" +
                "    contract tx01\n" +
                "    data " + writeToY + "\n" +
                "    gas 200000\n" +
                "    build\n" +
                "\n" +
                "transaction_build tx05\n" +
                "    sender acc4\n" +
                "    contract tx01\n" +
                "    data " + writeToXAndY + "\n" +
                "    gas 200000\n" +
                "    build\n" +
                "\n" +
                "block_build b02\n" +
                "    parent b01\n" +
                "    transactions tx02 tx03 tx04 tx05\n" +
                "    build\n" +
                "\n" +
                "block_connect b02\n" +
                "\n" +
                "assert_best b02\n" +
                "assert_tx_success tx01\n" +
                "assert_tx_success tx02\n" +
                "assert_tx_success tx03\n" +
                "assert_tx_success tx04\n" +
                "assert_tx_success tx05\n" +
                "\n", 0);

        Assertions.assertEquals(3000000L, GasCost.toGas(parallel.getBlockChain().getBestBlock().getHeader().getGasLimit()));
        Assertions.assertEquals(4, parallel.getBlockChain().getBestBlock().getTransactionsList().size());
        Assertions.assertArrayEquals(new short[] { 1, 2 }, parallel.getBlockChain().getBestBlock().getHeader().getTxExecutionSublistsEdges());
    }
}