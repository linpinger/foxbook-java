/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.awt.event.KeyEvent;
import javax.swing.JDialog;
import javax.swing.JScrollBar;

/**
 *
 * @author guanli
 */
public class Jpanel_ShowPage extends javax.swing.JPanel {
    private JDialog thisWin ;
    private String content;

    /**
     * Creates new form Jpanel_ShowPage
     */
    public Jpanel_ShowPage(String nr, JDialog xx) {
        this.thisWin = xx;
        this.content = nr.replace("\n", "\n　　");
        initComponents();
        uContent.setText(this.content);
        uContent.setCaretPosition(0);
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
        uContent = new javax.swing.JTextPane();

        uContent.setEditable(false);
        uContent.setFont(new java.awt.Font("宋体", 0, 24)); // NOI18N
        uContent.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                uContentKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(uContent);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void uContentKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_uContentKeyPressed
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
            JScrollBar ss = jScrollPane1.getVerticalScrollBar();
            ss.setValue(ss.getValue() + ss.getBlockIncrement(JScrollBar.VERTICAL) - 25);
        }
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            this.thisWin.dispose();
        }
    }//GEN-LAST:event_uContentKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane uContent;
    // End of variables declaration//GEN-END:variables
}
