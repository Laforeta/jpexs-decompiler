/*
 *  Copyright (C) 2010-2013 JPEXS
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
package com.jpexs.decompiler.flash.action.model;

import com.jpexs.decompiler.flash.action.swf4.ActionGetVariable;
import com.jpexs.decompiler.flash.ecma.Undefined;
import com.jpexs.decompiler.graph.GraphSourceItem;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.SourceGenerator;
import java.util.List;

public class GetVariableActionItem extends ActionItem {

    public GraphTargetItem name;
    private GraphTargetItem computedValue;
    private Object computedResult;
    private boolean computedCompiletime = false;
    private boolean computedVariableComputed = false;

    public GetVariableActionItem(GraphSourceItem instruction, GraphTargetItem value) {
        super(instruction, PRECEDENCE_PRIMARY);
        this.name = value;
    }

    @Override
    public String toString(ConstantPool constants) {
        return stripQuotes(name, constants);
    }

    @Override
    public List<com.jpexs.decompiler.graph.GraphSourceItemPos> getNeededSources() {
        List<com.jpexs.decompiler.graph.GraphSourceItemPos> ret = super.getNeededSources();
        ret.addAll(name.getNeededSources());
        return ret;
    }

    @Override
    public boolean isVariableComputed() {
        return true;
    }

    @Override
    public boolean isCompileTime() {
        if (computedValue == null) {
            return false;
        }
        return computedCompiletime;
    }

    @Override
    public Object getResult() {
        if (computedValue == null) {
            return new Undefined();
        }
        return computedResult;
    }

    public void setComputedValue(GraphTargetItem computedValue) {
        this.computedValue = computedValue;
        if (computedValue != null) {
            computedCompiletime = computedValue.isCompileTime();
            if (computedCompiletime) {
                computedResult = computedValue.getResult();
            }
            computedVariableComputed = computedValue.isVariableComputed();
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GetVariableActionItem other = (GetVariableActionItem) obj;
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }

    @Override
    public List<GraphSourceItem> toSource(List<Object> localData, SourceGenerator generator) {
        return toSourceMerge(localData, generator, name, new ActionGetVariable());
    }

    @Override
    public boolean hasReturnValue() {
        return true;
    }
}