/*
 * Copyright (c) 2010. JPEXS
 */

package com.jpexs.asdec.abc.avm2.treemodel;

import com.jpexs.asdec.abc.avm2.ConstantPool;
import com.jpexs.asdec.abc.avm2.instructions.AVM2Instruction;


public class ThrowTreeItem extends TreeItem {
    public TreeItem value;

    public ThrowTreeItem(AVM2Instruction instruction, TreeItem value) {
        super(instruction, NOPRECEDENCE);
        this.value = value;
    }


    @Override
    public String toString(ConstantPool constants) {
        return hilight("throw ") + value.toString(constants);
    }

}
