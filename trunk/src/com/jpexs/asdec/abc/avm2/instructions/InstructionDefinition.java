/*
 * Copyright (c) 2010. JPEXS
 */

package com.jpexs.asdec.abc.avm2.instructions;

import com.jpexs.asdec.abc.avm2.AVM2Code;
import com.jpexs.asdec.abc.avm2.ConstantPool;
import com.jpexs.asdec.abc.avm2.LocalDataArea;
import com.jpexs.asdec.abc.avm2.treemodel.FullMultinameTreeItem;
import com.jpexs.asdec.abc.avm2.treemodel.TreeItem;
import com.jpexs.asdec.abc.types.MethodInfo;
import com.jpexs.asdec.helpers.Highlighting;

import java.util.List;
import java.util.Stack;


public class InstructionDefinition {


    protected String hilighOffset(String text, long offset) {
        return Highlighting.hilighOffset(text, offset);
    }

    public int operands[];
    public String instructionName = "";
    public int instructionCode = 0;

    public static String localRegName(int reg) {
        if (reg == 0) return "this";
        return "_loc" + reg + "_";
    }

    public InstructionDefinition(int instructionCode, String instructionName, int operands[]) {
        this.instructionCode = instructionCode;
        this.instructionName = instructionName;
        this.operands = operands;
    }

    @Override
    public String toString() {
        String s = instructionName;
        for (int i = 0; i < operands.length; i++) {
            if ((operands[i] & 0xff00) == AVM2Code.OPT_U30) {
                s += " U30";
            }
            if ((operands[i] & 0xff00) == AVM2Code.OPT_U8) {
                s += " U8";
            }
            if ((operands[i] & 0xff00) == AVM2Code.OPT_BYTE) {
                s += " BYTE";
            }
            if ((operands[i] & 0xff00) == AVM2Code.OPT_S24) {
                s += " S24";
            }
            if ((operands[i] & 0xff00) == AVM2Code.OPT_CASE_OFFSETS) {
                s += " U30 S24,[S24]...";
            }
        }
        return s;
    }

    public void execute(LocalDataArea lda, ConstantPool constants, List arguments) {
    }

    public void translate(boolean isStatic, int classIndex, java.util.HashMap<Integer, TreeItem> localRegs, Stack<TreeItem> stack, java.util.Stack<TreeItem> scopeStack, ConstantPool constants, AVM2Instruction ins, MethodInfo[] method_info, List<TreeItem> output, com.jpexs.asdec.abc.types.MethodBody body, com.jpexs.asdec.abc.ABC abc) {

    }

    protected FullMultinameTreeItem resolveMultiname(Stack<TreeItem> stack, ConstantPool constants, int multinameIndex, AVM2Instruction ins) {
        TreeItem ns = null;
        TreeItem name = null;
        if (constants.constant_multiname[multinameIndex].needsNs()) {
            ns = (TreeItem) stack.pop();
        }
        if (constants.constant_multiname[multinameIndex].needsName()) {
            name = (TreeItem) stack.pop();
        }
        return new FullMultinameTreeItem(ins, multinameIndex, name, ns);
    }

    protected int resolvedCount(ConstantPool constants, int multinameIndex) {
        int pos = 0;
        if (constants.constant_multiname[multinameIndex].needsNs()) {
            pos++;
        }
        if (constants.constant_multiname[multinameIndex].needsName()) {
            pos++;
        }
        return pos;

    }

    protected String resolveMultinameNoPop(int pos, Stack<TreeItem> stack, ConstantPool constants, int multinameIndex, AVM2Instruction ins) {
        String ns = "";
        String name = "";
        if (constants.constant_multiname[multinameIndex].needsNs()) {
            ns = "[" + stack.get(pos) + "]";
            pos++;
        }
        if (constants.constant_multiname[multinameIndex].needsName()) {
            name = stack.get(pos).toString();
        } else {
            name = hilighOffset(constants.constant_multiname[multinameIndex].getName(constants), ins.offset);
        }
        return name + ns;
    }
}
