package com.consumimurigni.stellarj.scp.xdr;

import java.io.IOException;

import com.consuminurigni.stellarj.xdr.XdrDataInputStream;
import com.consuminurigni.stellarj.xdr.XdrDataOutputStream;

//=== xdr source ============================================================

//enum SCPStatementType
//{
//  SCP_ST_PREPARE = 0,
//  SCP_ST_CONFIRM = 1,
//  SCP_ST_EXTERNALIZE = 2,
//  SCP_ST_NOMINATE = 3
//};

//===========================================================================
public enum SCPStatementType  {
SCP_ST_PREPARE(0),
SCP_ST_CONFIRM(1),
SCP_ST_EXTERNALIZE(2),
SCP_ST_NOMINATE(3),
;
private int mValue;

SCPStatementType(int value) {
  mValue = value;
}

public int getValue() {
  return mValue;
}

static SCPStatementType decode(XdrDataInputStream stream) throws IOException {
int value = stream.readInt();
switch (value) {
  case 0: return SCP_ST_PREPARE;
  case 1: return SCP_ST_CONFIRM;
  case 2: return SCP_ST_EXTERNALIZE;
  case 3: return SCP_ST_NOMINATE;
  default:
    throw new RuntimeException("Unknown enum value: " + value);
}
}

static void encode(XdrDataOutputStream stream, SCPStatementType value) throws IOException {
stream.writeInt(value.getValue());
}
}
