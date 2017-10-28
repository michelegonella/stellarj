package com.consuminurigni.stellarj.xdr;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import com.consuminurigni.stellarj.scp.xdr.SCPEnvelope;

public class Xdr {
////////////////////////////////////////////////////////////////
//XDR containers (xvector, xarrray, pointer) and bytes (xstring,
//opaque_vec, opaque_array)
////////////////////////////////////////////////////////////////

//! Maximum length of vectors.  (The RFC says 0xffffffff, but out of
//! paranoia for integer overflows we chose something that still fits
//! in 32 bits when rounded up to a multiple of four.)
public static final Uint32 XDR_MAX_LEN = Uint32.of2ComplRepr(0xfffffffc);

	public static byte[] pack(byte[]... bufs) {
		 int size = 0;

		    for (byte[] bytes : bufs)
		    {
		        size += bytes.length;
		    }

		    ByteBuffer byteBuffer = ByteBuffer.allocate(size);

		    for (byte[] bytes : bufs)
		    {
		        byteBuffer.put(bytes);
		    }

		    return byteBuffer.array();
	}

	@SafeVarargs
	public static byte[] packObjects(Collection<? extends Encodable>... xdrObjs) {
		return null;
	}


	//TODO ????
	public static boolean isSorted(Value[] vals) {
		if(vals.length < 2) {
			return true;
		}
		byte[] prec = vals[0].getValue();
		for(int i = 1; i < vals.length; i++) {
			byte[] cur = vals[i].getValue();
			if(Arrays.equals(prec, cur)) {
				//TODO correct ?
				prec = cur;
				continue;
			} else {
				for(int j = 0; j < prec.length; j++) {
					if(cur.length > j) {
						if(Byte.toUnsignedInt(prec[j]) > Byte.toUnsignedInt(cur[j])) {
							return false;
						}
					}
				}
				prec = cur;
			}
		}
		return true;
	}

	//! Return a std::string containing a pretty-printed version an XDR
	//! data type.  The string will contain multiple lines and end with a
	//! newline.  \arg name if non-NULL, the string begins with the name
	//! and an equals sign.  \arg indent specifies a non-zero minimum
	//! indentation.
	public static String xdr_to_string(Encodable xdrObj) {
		return null;
//	xdr_to_string(const T &t, const char *name = nullptr, int indent = 0)
//	{
//	  detail::Printer p(indent);
//	  archive(p, t, name);
//	  p.buf_ << std::endl;
//	  return p.buf_.str();
	}

}
