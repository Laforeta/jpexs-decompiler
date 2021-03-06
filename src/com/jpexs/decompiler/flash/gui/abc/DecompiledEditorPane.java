/*
 *  Copyright (C) 2010-2015 JPEXS
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.gui.abc;

import com.jpexs.decompiler.flash.gui.editor.LineMarkedEditorPane;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.CachedDecompilation;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import com.jpexs.decompiler.flash.abc.avm2.AVM2Code;
import com.jpexs.decompiler.flash.abc.avm2.instructions.AVM2Instruction;
import com.jpexs.decompiler.flash.abc.avm2.instructions.construction.ConstructSuperIns;
import com.jpexs.decompiler.flash.abc.avm2.instructions.executing.CallSuperIns;
import com.jpexs.decompiler.flash.abc.avm2.instructions.executing.CallSuperVoidIns;
import com.jpexs.decompiler.flash.abc.avm2.parser.script.Reference;
import com.jpexs.decompiler.flash.abc.types.ClassInfo;
import com.jpexs.decompiler.flash.abc.types.InstanceInfo;
import com.jpexs.decompiler.flash.abc.types.Multiname;
import com.jpexs.decompiler.flash.abc.types.ScriptInfo;
import com.jpexs.decompiler.flash.abc.types.traits.Trait;
import com.jpexs.decompiler.flash.abc.types.traits.TraitFunction;
import com.jpexs.decompiler.flash.abc.types.traits.TraitMethodGetterSetter;
import com.jpexs.decompiler.flash.abc.types.traits.TraitSlotConst;
import com.jpexs.decompiler.flash.gui.AppStrings;
import com.jpexs.decompiler.flash.helpers.hilight.HighlightData;
import com.jpexs.decompiler.flash.helpers.hilight.HighlightSpecialType;
import com.jpexs.decompiler.flash.helpers.hilight.Highlighting;
import com.jpexs.decompiler.flash.tags.ABCContainerTag;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.Token;
import jsyntaxpane.TokenType;

public class DecompiledEditorPane extends LineMarkedEditorPane implements CaretListener {

    private List<Highlighting> highlights = new ArrayList<>();

    private List<Highlighting> specialHighlights = new ArrayList<>();

    private List<Highlighting> traitHighlights = new ArrayList<>();

    private List<Highlighting> methodHighlights = new ArrayList<>();

    private List<Highlighting> classHighlights = new ArrayList<>();

    private Highlighting currentMethodHighlight;

    private Highlighting currentTraitHighlight;

    private ScriptPack script;

    public int lastTraitIndex = 0;

    public boolean ignoreCarret = false;

    private boolean reset = false;

    private final ABCPanel abcPanel;

    private int classIndex = -1;

    private boolean isStatic = false;

    private final List<Runnable> scriptListeners = new ArrayList<>();

    public void addScriptListener(Runnable l) {
        scriptListeners.add(l);
    }

    public ABCPanel getAbcPanel() {
        return abcPanel;
    }

    public void removeScriptListener(Runnable l) {
        scriptListeners.remove(l);
    }

    public void fireScript() {
        for (Runnable scriptListener : scriptListeners) {
            scriptListener.run();
        }
    }

    public Trait getCurrentTrait() {
        return script.abc.findTraitByTraitId(classIndex, lastTraitIndex);
    }

    public ScriptPack getScriptLeaf() {
        return script;
    }

    public boolean getIsStatic() {
        return isStatic;
    }

    public void setNoTrait() {
        abcPanel.detailPanel.showCard(DetailPanel.UNSUPPORTED_TRAIT_CARD, null);
    }

    public void hilightSpecial(HighlightSpecialType type, long index) {
        int startPos;
        int endPos;
        if (currentMethodHighlight == null) {
            if (currentTraitHighlight == null) {
                return;
            }
            startPos = currentTraitHighlight.startPos;
            endPos = currentTraitHighlight.startPos + currentTraitHighlight.len;
        } else {
            startPos = currentMethodHighlight.startPos;
            endPos = currentMethodHighlight.startPos + currentMethodHighlight.len;
        }

        List<Highlighting> allh = new ArrayList<>();
        for (Highlighting h : traitHighlights) {
            if (h.getProperties().index == lastTraitIndex) {
                for (Highlighting sh : specialHighlights) {
                    if (sh.startPos >= h.startPos && (sh.startPos + sh.len < h.startPos + h.len)) {
                        allh.add(sh);
                    }
                }
            }
        }
        if (currentMethodHighlight != null) {
            for (Highlighting h : specialHighlights) {
                if (h.startPos >= startPos && (h.startPos + h.len < endPos)) {
                    allh.add(h);
                }
            }
        }
        for (Highlighting h : allh) {
            if (h.getProperties().subtype.equals(type) && (h.getProperties().index == index)) {
                ignoreCarret = true;
                if (h.startPos <= getDocument().getLength()) {
                    setCaretPosition(h.startPos);
                }
                getCaret().setVisible(true);
                ignoreCarret = false;
                break;
            }
        }
    }

    public void hilightOffset(long offset) {
        if (currentMethodHighlight == null) {
            return;
        }
        for (Highlighting h : traitHighlights) {
            if (h.getProperties().index == lastTraitIndex) {
                Highlighting h2 = Highlighting.searchOffset(highlights, offset, h.startPos, h.startPos + h.len);
                if (h2 != null) {
                    ignoreCarret = true;
                    if (h2.startPos <= getDocument().getLength()) {
                        setCaretPosition(h2.startPos);
                    }
                    getCaret().setVisible(true);
                    ignoreCarret = false;
                }

            }
        }
    }

    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    private boolean displayMethod(int pos, int methodIndex, String name, Trait trait, boolean isStatic) {
        ABC abc = getABC();
        if (abc == null) {
            return false;
        }
        int bi = abc.findBodyIndex(methodIndex);
        if (bi == -1) {
            return false;
        }

        //fix for inner functions:
        if (trait instanceof TraitMethodGetterSetter) {
            TraitMethodGetterSetter tm = (TraitMethodGetterSetter) trait;
            if (tm.method_info != methodIndex) {
                trait = null;
            }
        }
        if (trait instanceof TraitFunction) {
            TraitFunction tf = (TraitFunction) trait;
            if (tf.method_info != methodIndex) {
                trait = null;
            }
        }
        abcPanel.detailPanel.showCard(DetailPanel.METHOD_TRAIT_CARD, trait);
        MethodCodePanel methodCodePanel = abcPanel.detailPanel.methodTraitPanel.methodCodePanel;
        if (reset || (methodCodePanel.getBodyIndex() != bi)) {
            methodCodePanel.setBodyIndex(bi, abc, name, trait, script.scriptIndex);
            abcPanel.detailPanel.setEditMode(false);
            this.isStatic = isStatic;
        }
        boolean success = false;
        Highlighting h = Highlighting.searchPos(highlights, pos);
        if (h != null) {
            methodCodePanel.hilighOffset(h.getProperties().offset);
            success = true;
        }
        Highlighting sh = Highlighting.searchPos(specialHighlights, pos);
        if (sh != null) {
            methodCodePanel.hilighSpecial(sh.getProperties().subtype, sh.getProperties().specialValue);
            success = true;
        }
        return success;
    }

    public void displayClass(int classIndex, int scriptIndex) {
        if (abcPanel.navigator.getClassIndex() != classIndex) {
            abcPanel.navigator.setClassIndex(classIndex, scriptIndex);
        }
    }

    public void resetEditing() {
        reset = true;
        caretUpdate(null);
        reset = false;
    }

    public int getMultinameUnderMouseCursor(Point pt) {
        return getMultinameAtPos(viewToModel(pt));
    }

    public int getMultinameUnderCaret() {
        return getMultinameAtPos(getCaretPosition());
    }

    public int getLocalDeclarationOfPos(int pos, Reference<String> type) {
        Highlighting sh = Highlighting.searchPos(specialHighlights, pos);
        Highlighting h = Highlighting.searchPos(highlights, pos);

        if (h == null) {
            return -1;
        }

        List<Highlighting> tms = Highlighting.searchAllPos(methodHighlights, pos);
        if (tms.isEmpty()) {
            return -1;
        }
        for (Highlighting tm : tms) {

            List<Highlighting> tm_tms = Highlighting.searchAllIndexes(methodHighlights, tm.getProperties().index);
            //is it already declaration?
            if (h.getProperties().declaration || (sh != null && sh.getProperties().declaration)) {
                return -1; //no jump
            }

            String lname = h.getProperties().localName;
            if ("this".equals(lname)) {
                Highlighting ch = Highlighting.searchPos(classHighlights, pos);
                int cindex = (int) ch.getProperties().index;
                ABC abc = getABC();
                type.setVal(abc.instance_info.get(cindex).getName(abc.constants).getNameWithNamespace(abc.constants, true));
                return ch.startPos;
            }

            HighlightData hData = h.getProperties();
            HighlightData search = new HighlightData();
            search.declaration = hData.declaration;
            search.declaredType = hData.declaredType;
            search.localName = hData.localName;
            search.specialValue = hData.specialValue;
            if (search.isEmpty()) {
                return -1;
            }
            search.declaration = true;

            for (Highlighting tm1 : tm_tms) {
                Highlighting rh = Highlighting.search(highlights, search, tm1.startPos, tm1.startPos + tm1.len);
                if (rh == null) {
                    rh = Highlighting.search(specialHighlights, search, tm1.startPos, tm1.startPos + tm1.len);
                }
                if (rh != null) {
                    type.setVal(rh.getProperties().declaredType);
                    return rh.startPos;
                }
            }
        }

        return -1;
    }

    public boolean getPropertyTypeAtPos(int pos, Reference<Integer> abcIndex, Reference<Integer> classIndex, Reference<Integer> traitIndex, Reference<Boolean> classTrait, Reference<Integer> multinameIndex) {

        int m = getMultinameAtPos(pos, true);
        if (m <= 0) {
            return false;
        }
        SyntaxDocument sd = (SyntaxDocument) getDocument();
        Token t = sd.getTokenAt(pos + 1);
        Token lastToken = t;
        Token prev;
        while (t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD || t.type == TokenType.REGEX) {
            prev = sd.getPrevToken(t);
            if (prev != null) {
                if (!".".equals(prev.getString(sd))) {
                    break;
                }
                t = sd.getPrevToken(prev);
            } else {
                break;
            }
        }
        if (t.type != TokenType.IDENTIFIER && t.type != TokenType.KEYWORD || t.type == TokenType.REGEX) {
            return false;
        }
        Reference<String> locTypeRef = new Reference<>("");
        getLocalDeclarationOfPos(t.start, locTypeRef);
        String currentType = locTypeRef.getVal();
        if (currentType.equals("*")) {
            return false;
        }
        boolean found;
        t = sd.getNextToken(t);
        while (t != lastToken && !currentType.equals("*")) {
            t = sd.getNextToken(t);
            String ident = t.getString(sd);
            found = false;
            List<ABCContainerTag> abcList = getABC().getSwf().getAbcList();
            loopi:
            for (int i = 0; i < abcList.size(); i++) {
                ABC a = abcList.get(i).getABC();
                int cindex = a.findClassByName(currentType);
                if (cindex > -1) {
                    InstanceInfo ii = a.instance_info.get(cindex);
                    for (int j = 0; j < ii.instance_traits.traits.size(); j++) {
                        Trait tr = ii.instance_traits.traits.get(j);
                        if (ident.equals(tr.getName(a).getName(a.constants, new ArrayList<>(), false /*NOT RAW!*/))) {
                            classIndex.setVal(cindex);
                            abcIndex.setVal(i);
                            traitIndex.setVal(j);
                            classTrait.setVal(false);
                            multinameIndex.setVal(tr.name_index);
                            currentType = ii.getName(a.constants).getNameWithNamespace(a.constants, true);
                            found = true;
                            break loopi;
                        }
                    }

                    ClassInfo ci = a.class_info.get(cindex);
                    for (int j = 0; j < ci.static_traits.traits.size(); j++) {
                        Trait tr = ci.static_traits.traits.get(j);
                        if (ident.equals(tr.getName(a).getName(a.constants, new ArrayList<>(), false /*NOT RAW!*/))) {
                            classIndex.setVal(cindex);
                            abcIndex.setVal(i);
                            traitIndex.setVal(j);
                            classTrait.setVal(true);
                            multinameIndex.setVal(tr.name_index);
                            currentType = ii.getName(a.constants).getNameWithNamespace(a.constants, true);
                            found = true;
                            break loopi;
                        }
                    }
                }
            }
            if (!found) {
                return false;
            }

            t = sd.getNextToken(t);
            if (!".".equals(t.getString(sd))) {
                break;
            }
        }
        return true;
    }

    public int getMultinameAtPos(int pos) {
        return getMultinameAtPos(pos, false);
    }

    public int getMultinameAtPos(int pos, boolean codeOnly) {
        Highlighting tm = Highlighting.searchPos(methodHighlights, pos);
        Trait currentTrait = null;
        int currentMethod = -1;
        ABC abc = getABC();
        if (tm != null) {

            int mi = (int) tm.getProperties().index;
            currentMethod = mi;
            int bi = abc.findBodyIndex(mi);
            Highlighting h = Highlighting.searchPos(highlights, pos);
            if (h != null) {
                List<AVM2Instruction> list = abc.bodies.get(bi).getCode().code;
                AVM2Instruction lastIns = null;
                long inspos = 0;
                AVM2Instruction selIns = null;
                for (AVM2Instruction ins : list) {
                    if (h.getProperties().offset == ins.getOffset()) {
                        selIns = ins;
                        break;
                    }
                    if (ins.getOffset() > h.getProperties().offset) {
                        inspos = h.getProperties().offset - lastIns.offset;
                        selIns = lastIns;
                        break;
                    }
                    lastIns = ins;
                }
                if (selIns != null) {
                    if (!codeOnly && ((selIns.definition instanceof ConstructSuperIns) || (selIns.definition instanceof CallSuperIns) || (selIns.definition instanceof CallSuperVoidIns))) {
                        Highlighting tc = Highlighting.searchPos(classHighlights, pos);
                        if (tc != null) {
                            int cindex = (int) tc.getProperties().index;
                            if (cindex > -1) {
                                return abc.instance_info.get(cindex).super_index;
                            }
                        }
                    } else {
                        for (int i = 0; i < selIns.definition.operands.length; i++) {
                            if (selIns.definition.operands[i] == AVM2Code.DAT_MULTINAME_INDEX) {
                                return selIns.operands[i];
                            }
                        }
                    }
                }
            }

        }
        if (codeOnly) {
            return -1;
        }

        Highlighting ch = Highlighting.searchPos(classHighlights, pos);
        if (ch != null) {
            Highlighting th = Highlighting.searchPos(traitHighlights, pos);
            if (th != null) {
                currentTrait = abc.findTraitByTraitId((int) ch.getProperties().index, (int) th.getProperties().index);
            }
        }

        if (currentTrait instanceof TraitMethodGetterSetter) {
            currentMethod = ((TraitMethodGetterSetter) currentTrait).method_info;
        }
        Highlighting sh = Highlighting.searchPos(specialHighlights, pos);
        if (sh != null) {
            switch (sh.getProperties().subtype) {
                case TYPE_NAME:
                    String typeName = sh.getProperties().specialValue;
                    for (int i = 1; i < abc.constants.constant_multiname.size(); i++) {
                        Multiname m = abc.constants.constant_multiname.get(i);
                        if (m != null) {
                            if (typeName.equals(m.getNameWithNamespace(abc.constants, true))) {
                                return i;
                            }
                        }
                    }
                case TRAIT_TYPE_NAME:
                    if (currentTrait instanceof TraitSlotConst) {
                        TraitSlotConst ts = (TraitSlotConst) currentTrait;
                        return ts.type_index;
                    }
                    break;
                case TRAIT_NAME:
                    if (currentTrait != null) {
                        //return currentTrait.name_index;
                    }
                    break;
                case RETURNS:
                    if (currentMethod > -1) {
                        return abc.method_info.get(currentMethod).ret_type;
                    }
                    break;
                case PARAM:
                    if (currentMethod > -1) {
                        return abc.method_info.get(currentMethod).param_types[(int) sh.getProperties().index];
                    }
                    break;
            }
        }
        return -1;
    }

    @Override
    public void caretUpdate(final CaretEvent e) {
        ABC abc = getABC();
        if (abc == null) {
            return;
        }
        if (ignoreCarret) {
            return;
        }

        getCaret().setVisible(true);
        int pos = getCaretPosition();
        abcPanel.detailPanel.methodTraitPanel.methodCodePanel.setIgnoreCarret(true);
        try {
            classIndex = -1;
            Highlighting cm = Highlighting.searchPos(classHighlights, pos);
            if (cm != null) {
                classIndex = (int) cm.getProperties().index;
                displayClass(classIndex, script.scriptIndex);
            }
            Highlighting tm = Highlighting.searchPos(methodHighlights, pos);
            if (tm != null) {
                String name = "";
                if (classIndex > -1) {
                    name = abc.instance_info.get(classIndex).getName(abc.constants).getNameWithNamespace(abc.constants, false);
                }

                Trait currentTrait = null;
                currentTraitHighlight = Highlighting.searchPos(traitHighlights, pos);
                if (currentTraitHighlight != null) {
                    lastTraitIndex = (int) currentTraitHighlight.getProperties().index;
                    if (classIndex != -1) {
                        currentTrait = getCurrentTrait();
                        isStatic = abc.isStaticTraitId(classIndex, lastTraitIndex);
                        if (currentTrait != null) {
                            name += ":" + currentTrait.getName(abc).getName(abc.constants, new ArrayList<>(), false);
                        }
                    }
                }

                displayMethod(pos, (int) tm.getProperties().index, name, currentTrait, isStatic);
                currentMethodHighlight = tm;
                return;
            }

            if (classIndex == -1) {
                abcPanel.navigator.setClassIndex(-1, script.scriptIndex);
                setNoTrait();
                return;
            }
            Trait currentTrait;
            currentTraitHighlight = Highlighting.searchPos(traitHighlights, pos);
            if (currentTraitHighlight != null) {
                lastTraitIndex = (int) currentTraitHighlight.getProperties().index;
                currentTrait = getCurrentTrait();
                if (currentTrait != null) {
                    if (currentTrait instanceof TraitSlotConst) {
                        abcPanel.detailPanel.slotConstTraitPanel.load((TraitSlotConst) currentTrait, abc,
                                abc.isStaticTraitId(classIndex, lastTraitIndex));
                        abcPanel.detailPanel.showCard(DetailPanel.SLOT_CONST_TRAIT_CARD, currentTrait);
                        abcPanel.detailPanel.setEditMode(false);
                        currentMethodHighlight = null;
                        Highlighting spec = Highlighting.searchPos(specialHighlights, pos, currentTraitHighlight.startPos, currentTraitHighlight.startPos + currentTraitHighlight.len);
                        if (spec != null) {
                            abcPanel.detailPanel.slotConstTraitPanel.hilightSpecial(spec);
                        }

                        return;
                    }
                }
                currentMethodHighlight = null;
                currentTrait = null;
                String name = abc.instance_info.get(classIndex).getName(abc.constants).getNameWithNamespace(abc.constants, false);
                currentTrait = getCurrentTrait();
                isStatic = abc.isStaticTraitId(classIndex, lastTraitIndex);
                if (currentTrait != null) {
                    name += ":" + currentTrait.getName(abc).getName(abc.constants, new ArrayList<>(), false);
                }

                displayMethod(pos, abc.findMethodIdByTraitId(classIndex, lastTraitIndex), name, currentTrait, isStatic);
                return;
            }
            setNoTrait();
        } finally {
            abcPanel.detailPanel.methodTraitPanel.methodCodePanel.setIgnoreCarret(false);
        }
    }

    public void gotoLastTrait() {
        gotoTrait(lastTraitIndex);
    }

    public void gotoTrait(int traitId) {
        if (traitId == -1) {
            return;
        }

        Highlighting tc = Highlighting.searchIndex(classHighlights, classIndex);
        if (tc != null) {
            Highlighting th = Highlighting.searchIndex(traitHighlights, traitId, tc.startPos, tc.startPos + tc.len);
            int pos;
            if (th != null) {
                if (th.len > 1) {
                    ignoreCarret = true;
                    int startPos = th.startPos + th.len - 1;
                    if (startPos <= getDocument().getLength()) {
                        setCaretPosition(startPos);
                    }
                    ignoreCarret = false;
                }
                pos = th.startPos;
            } else {
                pos = tc.startPos;
            }

            final int fpos = pos;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (fpos <= getDocument().getLength()) {
                        setCaretPosition(fpos);
                    }
                }
            }, 100);
        }
    }

    public DecompiledEditorPane(ABCPanel abcPanel) {
        super();
        setEditable(false);
        getCaret().setVisible(true);
        addCaretListener(this);
        this.abcPanel = abcPanel;
    }

    public void clearScript() {
        script = null;
    }

    public void setScript(ScriptPack scriptLeaf) {
        abcPanel.scriptNameLabel.setText(scriptLeaf.getClassPath().toString());
        int scriptIndex = scriptLeaf.scriptIndex;
        ScriptInfo script = null;
        ABC abc = scriptLeaf.abc;
        if (scriptIndex > -1) {
            script = abc.script_info.get(scriptIndex);
        }
        if (script == null) {
            highlights = new ArrayList<>();
            specialHighlights = new ArrayList<>();
            traitHighlights = new ArrayList<>();
            methodHighlights = new ArrayList<>();
            this.script = scriptLeaf;
            return;
        }
        setText("// " + AppStrings.translate("pleasewait") + "...");

        this.script = scriptLeaf;
        CachedDecompilation cd = null;
        try {
            cd = SWF.getCached(scriptLeaf);
        } catch (InterruptedException ex) {
        }

        if (cd != null) {
            String hilightedCode = cd.text;
            highlights = cd.getInstructionHighlights();
            specialHighlights = cd.getSpecialHighligths();
            traitHighlights = cd.getTraitHighlights();
            methodHighlights = cd.getMethodHighlights();
            classHighlights = cd.getClassHighlights();
            setText(hilightedCode);

            if (classHighlights.size() > 0) {
                setCaretPosition(classHighlights.get(0).startPos);
            }
        }
        fireScript();
    }

    public void reloadClass() {
        int ci = classIndex;
        SWF.uncache(script);
        if (script != null && getABC() != null) {
            setScript(script);
        }
        setNoTrait();
        setClassIndex(ci);
    }

    public int getClassIndex() {
        return classIndex;
    }

    private ABC getABC() {
        return script == null ? null : script.abc;
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        setCaretPosition(0);
    }
}
