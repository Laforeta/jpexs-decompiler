/*
 * Copyright (c) 2010. JPEXS
 */

package com.jpexs.asdec.abc.avm2.treemodel.operations;

import com.jpexs.asdec.abc.avm2.instructions.AVM2Instruction;
import com.jpexs.asdec.abc.avm2.treemodel.TreeItem;


public class NegTreeItem extends UnaryOpTreeItem {

    public NegTreeItem(AVM2Instruction instruction, TreeItem value) {
        super(instruction, PRECEDENCE_UNARY, value, "-");
    }
}
