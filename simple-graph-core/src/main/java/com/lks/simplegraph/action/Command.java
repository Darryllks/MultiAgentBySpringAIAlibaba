package com.lks.simplegraph.action;

/**
 * 命令类，用于表示图执行中的下一步操作
 */
public class Command {

    private final String gotoNode;

    public Command(String gotoNode) {
        this.gotoNode = gotoNode;
    }

    public String gotoNode() {
        return gotoNode;
    }
}