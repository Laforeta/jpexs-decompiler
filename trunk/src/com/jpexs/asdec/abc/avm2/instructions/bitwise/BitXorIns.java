/*
 * Copyright (c) 2010. JPEXS
 */

package com.jpexs.asdec.abc.avm2.instructions.bitwise;

import com.jpexs.asdec.abc.avm2.ConstantPool;
import com.jpexs.asdec.abc.avm2.LocalDataArea;
import com.jpexs.asdec.abc.avm2.instructions.AVM2Instruction;
import com.jpexs.asdec.abc.avm2.instructions.InstructionDefinition;
import com.jpexs.asdec.abc.avm2.treemodel.TreeItem;
import com.jpexs.asdec.abc.avm2.treemodel.operations.BitXorTreeItem;
import com.jpexs.asdec.abc.types.MethodInfo;

import java.util.List;
import java.util.Stack;


public class BitXorIns extends InstructionDefinition {

    public BitXorIns() {
        super(0xaa, "bitxor", new int[]{});
    }

    @Override
    public void execute(LocalDataArea lda, ConstantPool constants, List arguments) {
        Long value2 = (Long) lda.operandStack.pop();
        Long value1 = (Long) lda.operandStack.pop();
        Long value3 = value1 ^ value2;
        lda.operandStack.push(value3);
    }

    @Override
    public void translate(boolean isStatic, int classIndex, java.util.HashMap<Integer, TreeItem> localRegs, Stack<TreeItem> stack, java.util.Stack<TreeItem> scopeStack, ConstantPool constants, AVM2Instruction ins, MethodInfo[] method_info, List<TreeItem> output, com.jpexs.asdec.abc.types.MethodBody body, com.jpexs.asdec.abc.ABC abc) {
        TreeItem v2 = (TreeItem) stack.pop();
        TreeItem v1 = (TreeItem) stack.pop();
        stack.push(new BitXorTreeItem(ins, v1, v2));
    }
}
