package com.jpexs.asdec.action.swf4;

import com.jpexs.asdec.SWFInputStream;
import com.jpexs.asdec.SWFOutputStream;
import com.jpexs.asdec.action.Action;
import com.jpexs.asdec.action.parser.FlasmLexer;
import com.jpexs.asdec.action.parser.ParseException;
import com.jpexs.asdec.helpers.Helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActionIf extends Action {
    public int offset;
    public String identifier;

    public ActionIf(SWFInputStream sis) throws IOException {
        super(0x9D, 2);
        offset = sis.readSI16();
    }

    @Override
    public List<Long> getAllRefs(int version) {
        List<Long> ret = new ArrayList<Long>();
        ret.add(getRef(version));
        return ret;
    }

    public long getRef(int version) {
        return getAddress() + getBytes(version).length + offset;
    }

    public byte[] getBytes(int version) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SWFOutputStream sos = new SWFOutputStream(baos, version);
        try {
            sos.writeSI16(offset);
            sos.close();
        } catch (IOException e) {

        }
        return surroundWithAction(baos.toByteArray(), version);
    }

    @Override
    public String getASMSource(List<Long> knownAddreses, List<String> constantPool, int version) {
        return "If loc" + Helper.formatAddress(getAddress() + getBytes(version).length + offset);
    }

    public ActionIf(FlasmLexer lexer) throws IOException, ParseException {
        super(0x9D, 0);
        identifier = lexIdentifier(lexer);
    }

    @Override
    public List<Action> getAllIfsOrJumps() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(this);
        return ret;
    }
}