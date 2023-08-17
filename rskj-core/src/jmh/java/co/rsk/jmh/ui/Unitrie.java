/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/Application.java to edit this template
 */
package co.rsk.ui;

import co.rsk.NodeRunner;
import co.rsk.RskContext;
import co.rsk.core.RskAddress;
import co.rsk.crypto.Keccak256;
import co.rsk.trie.*;
import co.rsk.util.HexUtils;
import co.rsk.util.PreflightChecksUtils;
import com.google.common.collect.Lists;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.db.TrieKeyMapper;
import org.ethereum.vm.trace.Op;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * @author patricio
 */
public class Unitrie extends javax.swing.JFrame {

    private RskContextState context;
    private DefaultMutableTreeNode root;
    private NodeRunner runner;
    private List<TrieDTO> nodes;


    /**
     * Creates new form Unitrie
     */
    public Unitrie() {
        this.context = new RskContextState();
        context.setup(getArgs());
        initComponents();
        this.blocknumberMinText.setText("" + this.context.getContext().getBlockStore().getMinNumber());
        this.blocknumberText.setText("" + this.context.getBlockchain().getBestBlock().getNumber());
        readTrie();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        unitrie = initTree();
        jScrollPane2 = new javax.swing.JScrollPane();
        findKey = new javax.swing.JTextPane();
        findButton = new javax.swing.JButton();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        unitrieDescription = new javax.swing.JTextArea();
        blocknumberText = new javax.swing.JTextField();
        blocknumberButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        blocknumberMinText = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        recoverButton = new javax.swing.JButton();
        recoverLabel = new javax.swing.JLabel();
        nodesReloadButton = new javax.swing.JButton();
        recoverHashLabel = new javax.swing.JLabel();
        recoverHashField = new javax.swing.JTextField();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentsMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        unitrie.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                unitrieValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(unitrie);

        findKey.setText("0x2C165446Cc51BBd5A194A4b3D7919Fa457B6420e");
        findKey.setToolTipText("");
        jScrollPane2.setViewportView(findKey);

        findButton.setText("Find");
        findButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findButtonActionPerformed(evt);
            }
        });

        startButton.setText("Start Node");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("Stop Node");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        unitrieDescription.setColumns(20);
        unitrieDescription.setRows(5);
        unitrieDescription.setWrapStyleWord(true);
        jScrollPane3.setViewportView(unitrieDescription);

        blocknumberText.setText("0");

        blocknumberButton.setText("Refresh");
        blocknumberButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blocknumberButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Block #");

        blocknumberMinText.setText("0");

        jLabel2.setText("-");

        recoverButton.setText("Recover");
        recoverButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recoverButtonActionPerformed(evt);
            }
        });

        recoverLabel.setText("0");

        nodesReloadButton.setText("Reload");
        nodesReloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodesReloadButtonActionPerformed(evt);
            }
        });

        recoverHashLabel.setText("-");

        recoverHashField.setText("0fea2d4c0b83844e3f6ca45bdd0629ce846f7dc6372984e80a9084fda9343b6e");

        fileMenu.setMnemonic('f');
        fileMenu.setText("File");

        openMenuItem.setMnemonic('o');
        openMenuItem.setText("Open");
        fileMenu.add(openMenuItem);

        saveMenuItem.setMnemonic('s');
        saveMenuItem.setText("Save");
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('a');
        saveAsMenuItem.setText("Save As ...");
        saveAsMenuItem.setDisplayedMnemonicIndex(5);
        fileMenu.add(saveAsMenuItem);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('e');
        editMenu.setText("Edit");

        cutMenuItem.setMnemonic('t');
        cutMenuItem.setText("Cut");
        editMenu.add(cutMenuItem);

        copyMenuItem.setMnemonic('y');
        copyMenuItem.setText("Copy");
        editMenu.add(copyMenuItem);

        pasteMenuItem.setMnemonic('p');
        pasteMenuItem.setText("Paste");
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setMnemonic('d');
        deleteMenuItem.setText("Delete");
        editMenu.add(deleteMenuItem);

        menuBar.add(editMenu);

        helpMenu.setMnemonic('h');
        helpMenu.setText("Help");

        contentsMenuItem.setMnemonic('c');
        contentsMenuItem.setText("Contents");
        helpMenu.add(contentsMenuItem);

        aboutMenuItem.setMnemonic('a');
        aboutMenuItem.setText("About");
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(findButton)
                                .addGap(112, 112, 112))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 526, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(blocknumberMinText, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(blocknumberText, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(blocknumberButton)
                                .addGap(66, 66, 66)
                                .addComponent(startButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(stopButton))
                            .addComponent(jScrollPane3)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(recoverHashField, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                            .addComponent(recoverHashLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(recoverButton)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(nodesReloadButton)
                                .addGap(43, 43, 43)
                                .addComponent(recoverLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(findButton)
                        .addComponent(blocknumberText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(blocknumberButton)
                        .addComponent(startButton)
                        .addComponent(stopButton)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(blocknumberMinText)
                        .addComponent(jLabel2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                    .addComponent(jScrollPane3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nodesReloadButton)
                    .addComponent(recoverLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(recoverHashField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(recoverButton)
                    .addComponent(recoverHashLabel))
                .addGap(21, 21, 21))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        // TODO add your handling code here:
        this.stopNode();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void unitrieValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_unitrieValueChanged

        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                unitrie.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        TrieDTO trie = (TrieDTO) nodeInfo;
        if (!(trie.isTerminal() || node.children().hasMoreElements())) {
            if (trie.getLeftHash() != null) {
                node.add(new DefaultMutableTreeNode(context.getNode(trie.getLeftHash()).get()));
            } else if (trie.getLeft() != null) {
                node.add(new DefaultMutableTreeNode(TrieDTO.decodeFromMessage(trie.getLeft(), context.getContext().getTrieStore())));
            }
            if (trie.getRightHash() != null) {
                node.add(new DefaultMutableTreeNode(context.getNode(trie.getRightHash()).get()));
            } else if (trie.getRight() != null) {
                node.add(new DefaultMutableTreeNode(TrieDTO.decodeFromMessage(trie.getRight(), context.getContext().getTrieStore())));
            }
        }

        unitrieDescription.setText(trie.toDescription());        // TODO add your handling code here:
    }//GEN-LAST:event_unitrieValueChanged

    private void findButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findButtonActionPerformed
        byte[] key = new TrieKeyMapper().getAccountKey(new RskAddress(this.findKey.getText()));
        final long blockNumber = getBlockNumber();
        final Optional<TrieDTO> rootByBlockNumber = this.context.getRootByBlockNumber(blockNumber);
        final DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode(rootByBlockNumber.get());
        unitrie.setModel(new DefaultTreeModel(newRoot));
        TrieDTO result = find(newRoot, TrieKeySlice.fromKey(key));
        if (result != null) {
            this.root.removeAllChildren();
            this.root = newRoot;
            unitrie.updateUI();
        } else {
            unitrie.setModel(new DefaultTreeModel(this.root));
            unitrie.updateUI();
            JOptionPane.showMessageDialog(this, "Not found.", "Result", JOptionPane.WARNING_MESSAGE);
            this.unitrieDescription.setText("Not found.");
        }
    }//GEN-LAST:event_findButtonActionPerformed

    private long getBlockNumber() {
        return Long.parseLong(this.blocknumberText.getText());
    }

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        // TODO add your handling code here:
        this.startNode();
    }//GEN-LAST:event_startButtonActionPerformed

    private void blocknumberButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blocknumberButtonActionPerformed
        // TODO add your handling code here:
        this.blocknumberMinText.setText("" + this.context.getContext().getBlockStore().getMinNumber());
        this.blocknumberText.setText("" + this.context.getBlockchain().getBestBlock().getNumber());
    }//GEN-LAST:event_blocknumberButtonActionPerformed

    private void recoverButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recoverButtonActionPerformed
        // TODO add your handling code here:
        new Thread(() -> {
            TrieDTO[] nodeArray = nodes.toArray(new TrieDTO[]{});
            Optional<TrieDTO> recovered = TrieDTOInOrderRecoverer.recoverTrie(nodeArray);
            byte[] recoveredBytes = recovered.get().toMessage();
            Keccak256 hash = getHash(recoveredBytes);
            recoverHashLabel.setText(hash.toHexString());
            recoverHashLabel.updateUI();
        }).start();
    }//GEN-LAST:event_recoverButtonActionPerformed

    private static Keccak256 getHash(byte[] recoveredBytes) {
        return new Keccak256(Keccak256Helper.keccak256(recoveredBytes));
    }
    private static String getHashString(byte[] recoveredBytes) {
        return getHash(recoveredBytes).toHexString();
    }


    private void nodesReloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nodesReloadButtonActionPerformed
        // TODO add your handling code here:
        this.readTrie();
    }//GEN-LAST:event_nodesReloadButtonActionPerformed

    private void readTrie() {
        new Thread(()->{

            TrieDTOInOrderIterator iterator = new TrieDTOInOrderIterator(Unitrie.this.context.getContext().getTrieStore(),
                    Hex.decode(recoverHashField.getText()),
                    0);
            List<TrieDTO> nodes = Lists.newArrayList();
            int i = 0;
            while (iterator.hasNext()) {
                nodes.add(TrieDTO.decodeFromSync(iterator.next().getEncoded()));
                i++;
                if (i%10000 == 0) {
                    recoverLabel.setText("" + i);
                    recoverLabel.updateUI();
                }
            }
            recoverLabel.setText("" + i);
            recoverLabel.updateUI();
            this.nodes = nodes;
        }).start();
    }

    private TrieDTO find(DefaultMutableTreeNode uiNode, TrieKeySlice key) {
        TrieDTO node = (TrieDTO) uiNode.getUserObject();
        final TrieKeySlice sharedPath = node.getPath();
        if (sharedPath.length() > key.length()) {
            return null;
        }
        int commonPathLength = key.commonPath(sharedPath).length();
        if (commonPathLength < sharedPath.length()) {
            return null;
        }

        if (commonPathLength == key.length()) {
            unitrie.setSelectionPath(new TreePath(uiNode.getPath()));
            return node;
        }
        final boolean isLeft = key.get(commonPathLength) == (byte) 0;

        TrieDTO result = null;
        final Optional<TrieDTO> leftNode = getNode(node.getLeftHash(), node.getLeft());
        if (leftNode.isPresent()) {
            final DefaultMutableTreeNode left = new DefaultMutableTreeNode(leftNode.get());
            uiNode.add(left);
            if (isLeft) {
                result = find(left, key.slice(commonPathLength + 1, key.length()));
                TreePath path = new TreePath(left.getPath());
                unitrie.expandPath(path);
            }
        }

        final Optional<TrieDTO> rightNode = getNode(node.getRightHash(), node.getRight());
        if (rightNode.isPresent()) {
            final DefaultMutableTreeNode right = new DefaultMutableTreeNode(rightNode.get());
            uiNode.add(right);
            if (!isLeft) {
                result = find(right, key.slice(commonPathLength + 1, key.length()));
                TreePath path = new TreePath(right.getPath());
                unitrie.expandPath(path);
            }
        }

        return result;

    }

    private Optional<TrieDTO> getNode(byte[] hash, byte[] value) {
        if (value != null) {
            return Optional.of(TrieDTO.decodeFromMessage(value, this.context.getContext().getTrieStore()));
        } else if (hash != null) {
            return this.context.getNode(hash);
        }
        return Optional.empty();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Unitrie.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Unitrie.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Unitrie.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Unitrie.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Unitrie().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton blocknumberButton;
    private javax.swing.JLabel blocknumberMinText;
    private javax.swing.JTextField blocknumberText;
    private javax.swing.JMenuItem contentsMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JButton findButton;
    private javax.swing.JTextPane findKey;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton nodesReloadButton;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JButton recoverButton;
    private javax.swing.JTextField recoverHashField;
    private javax.swing.JLabel recoverHashLabel;
    private javax.swing.JLabel recoverLabel;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JTree unitrie;
    private javax.swing.JTextArea unitrieDescription;
    // End of variables declaration//GEN-END:variables

    private JTree initTree() {
        DefaultMutableTreeNode top = createNodes();
        //Create a tree that allows one selection at a time.
        return new JTree(top);
    }


    private DefaultMutableTreeNode createNodes() {
        final Optional<TrieDTO> root = context.getRoot();
        if (root.isPresent()) {
            System.out.println("Has root!");
            this.root = new DefaultMutableTreeNode(root.get());
            this.root.add(new DefaultMutableTreeNode(context.getNode(root.get().getLeftHash()).get()));
            this.root.add(new DefaultMutableTreeNode(context.getNode(root.get().getRightHash()).get()));
        } else {

            System.out.println("Not has root!");
        }
        return this.root;
    }

    private String[] getArgs() {
        return new String[]{"--main"};
    }


    public void startNode() {
        try {
            this.context.getContext().close();
            this.context = new RskContextState();
            this.context.setup(getArgs());
            runNode(Runtime.getRuntime(), new PreflightChecksUtils(this.context.getContext()), this.context.getContext());
        } catch (Exception e) {
            System.err.println("The RSK node main thread failed, closing program");
        }
    }

    public void stopNode() {
        this.runner.stop();
    }

    private void runNode(@Nonnull Runtime runtime, @Nonnull PreflightChecksUtils preflightChecks, @Nonnull RskContext ctx) throws Exception {
        preflightChecks.runChecks();

        this.runner = ctx.getNodeRunner();

        runtime.addShutdownHook(new Thread(ctx::close, "stopper"));

        this.runner.run();
    }

}
