/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.action.swf5;

import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.action.Action;
import com.jpexs.decompiler.flash.action.parser.ActionParseException;
import com.jpexs.decompiler.flash.action.parser.pcode.ASMParsedSymbol;
import com.jpexs.decompiler.flash.action.parser.pcode.FlasmLexer;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.TranslateStack;
import com.jpexs.helpers.Helper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ActionConstantPool extends Action {

    public List<String> constantPool = new ArrayList<>();

    public ActionConstantPool(List<String> constantPool) {
        super(0x88, 0);
        this.constantPool = constantPool;
    }

    public ActionConstantPool(int actionLength, SWFInputStream sis, int version) throws IOException {
        super(0x88, actionLength);
        //sis = new SWFInputStream(new ByteArrayInputStream(sis.readBytes(actionLength)), version);
        int count = sis.readUI16("count");
        for (int i = 0; i < count; i++) {
            constantPool.add(sis.readString("constant"));
        }
    }

    public ActionConstantPool(FlasmLexer lexer) throws IOException, ActionParseException {
        super(0x88, 0);
        while (true) {
            ASMParsedSymbol symb = lexer.yylex();
            if (symb.type == ASMParsedSymbol.TYPE_STRING) {
                constantPool.add((String) symb.value);
            } else {
                lexer.yypushback(lexer.yylength());
                break;
            }
        }
    }

    @Override
    public byte[] getBytes(int version) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SWFOutputStream sos = new SWFOutputStream(baos, version);
        try {
            sos.writeUI16(constantPool.size());
            for (String s : constantPool) {
                sos.writeString(s);
            }
            sos.close();
        } catch (IOException e) {
            throw new Error("This should never happen.", e);
        }
        return surroundWithAction(baos.toByteArray(), version);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("ConstantPool");
        for (int i = 0; i < constantPool.size(); i++) {
            ret.append(" \"").append(Helper.escapeActionScriptString(constantPool.get(i))).append("\"");
        }
        return ret.toString();
    }

    @Override
    public void translate(TranslateStack stack, List<GraphTargetItem> output, HashMap<Integer, String> regNames, HashMap<String, GraphTargetItem> variables, HashMap<String, GraphTargetItem> functions, int staticOperation, String path) {
    }
}
