package com.consuminurigni.stellarj.scp.xdr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import com.consuminurigni.stellarj.xdr.Value;

public class ValueSet implements Iterable<Value> {

	@Override
	public Iterator<Value> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSorted() {
		// TODO Auto-generated method stub
		return false;
	}

	//TODO ????
	private static boolean isSorted(Value[] vals) {
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

	//TODO ugly passing notEqual by ref maybe better returning 1 > 0 eq -1 else
	//TODO are supposed sorted ?? if so check and throw
    public boolean isSubsetOf(ValueSet _v, AtomicBoolean notEqual)
    {
    	Value[] p = null;//TODO this
    	Value[] v = null;//TODO
        boolean res;
        if (p.length <= v.length)
        {
            res = true;//TODO std::includes(v.begin(), v.end(), p.begin(), p.end());
            for(int i = 0; i < p.length; i++) {
            	if(! Arrays.equals(p[i].getValue(), v[i].getValue())) {
            		res = false;
            	}
            }
            if (res)
            {
                notEqual.set(p.length != v.length);
            }
            else
            {
                notEqual.set(true);
            }
        }
        else
        {
            notEqual.set(true);
            res = false;
        }
        return res;
    }

	public boolean contains(Value valueToNominate) {
		// TODO Auto-generated method stub
		return false;
	}

	//TODO here we use add for emplace. ordering problems ??
	public boolean add(Value v) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

}
