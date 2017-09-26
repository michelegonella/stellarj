package com.consumimurigni.stellarj.core;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.stellar.sdk.xdr.Hash;
import org.stellar.sdk.xdr.NodeID;
import org.stellar.sdk.xdr.PublicKey;
import org.stellar.sdk.xdr.SCPEnvelope;
import org.stellar.sdk.xdr.SCPQuorumSet;
import org.stellar.sdk.xdr.SCPStatement;
import org.stellar.sdk.xdr.Uint64;

import com.consumimurigni.stellarj.crypto.CryptoUtils;
import com.consumimurigni.stellarj.crypto.KeyUtils;
import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consumimurigni.stellarj.xdr.XdrUtils;
import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class LocalNode {
	private static final Logger log = LogManager.getLogger();
		protected final NodeID mNodeID = new NodeID();
		protected final SecretKey mSecretKey;
		protected final boolean mIsValidator;
		protected final SCPQuorumSet mQSet;
		protected final Hash mQSetHash;

		    // alternative qset used during externalize {{mNodeID}}
		protected Hash gSingleQSetHash;                      // hash of the singleton qset
		protected SCPQuorumSet mSingleQSet; // {{mNodeID}}

		protected SCP mSCP;

		public LocalNode(SecretKey secretKey, boolean isValidator, SCPQuorumSet qSet, SCP scp)
		{
			mNodeID.setNodeID(secretKey.getPublicKey());
			mSecretKey = secretKey;
			mIsValidator = isValidator;
			mQSet = qSet;
			mSCP = scp;

			QuorumSetUtils.normalizeQSet(mQSet);
			mQSetHash =  new Hash();
			byte[] barr = XdrUtils.xdrEncode(mQSet, (t, u) -> {
				try {
					SCPQuorumSet.encode(t, u);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			mQSetHash.setHash(CryptoUtils.sha256(barr));
			log.info("SCP LocalNode::LocalNode @{} qSet:{}", KeyUtils.toShortString(mNodeID.getNodeID()),CryptoUtils.hexAbbrev(mQSetHash.getHash()));

			mSingleQSet = buildSingletonQSet(mNodeID);

			gSingleQSetHash =  new Hash();
			byte[] barr2 = XdrUtils.xdrEncode(mSingleQSet, (t, u) -> {
				try {
					SCPQuorumSet.encode(t, u);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			gSingleQSetHash.setHash(CryptoUtils.sha256(barr2));
		}

		private static SCPQuorumSet buildSingletonQSet(NodeID nodeID)
		{
		    SCPQuorumSet qSet = new SCPQuorumSet();
		    qSet.setThreshold(XdrUtils.newUint32(1));
		    qSet.setValidators(new PublicKey[] {nodeID.getNodeID()});
		    return qSet;
		}

		void updateQuorumSet(SCPQuorumSet qSet)
		{
			XdrUtils.scpQuorumSetCopy(qSet, mQSet);
		}

	public SCPQuorumSet getQuorumSet() {
		return mQSet;
	}

	public Hash getQuorumSetHash() {
	    return mQSetHash;
	}

	SecretKey getSecretKey()
	{
	    return mSecretKey;
	}

	static SCPQuorumSet getSingletonQSet(NodeID nodeID)
	{
	    return buildSingletonQSet(nodeID);
	}

	static void forAllNodesInternal(SCPQuorumSet qset, Consumer<NodeID> proc)
	{
	    for (PublicKey n : qset.getValidators())
	    {
	    	NodeID nid = new NodeID();
	    	nid.setNodeID(n);
	        proc.accept(nid);
	    }
	    for (SCPQuorumSet q : qset.getInnerSets())
	    {
	        forAllNodesInternal(q, proc);
	    }
	}

	// runs proc over all nodes contained in qset
	static void forAllNodes(SCPQuorumSet qset, Consumer<NodeID> proc)
	{
		final Equivalence<NodeID> eq = new Equivalence<NodeID>() {
			@Override
			protected boolean doEquivalent(NodeID nid0, NodeID nid1) {
				
				return XdrUtils.publicKeyEquals(nid0.getNodeID(), nid1.getNodeID())
				;
			}
			@Override
			protected int doHash(NodeID nid) {
				return Arrays.hashCode(nid.getNodeID().getEd25519().getUint256());
			}
			
		};
	    Set<Equivalence.Wrapper<NodeID>> done = new HashSet<>();
	    forAllNodesInternal(qset, (NodeID n) -> {
	    	Wrapper<NodeID> wrappedNid = eq.wrap(n);
	        if (! done.contains(wrappedNid))
	        {
	            proc.accept(n);
	            done.add(wrappedNid);
	        }
	    });
	}

	// if a validator is repeated multiple times its weight is only the
	// weight of the first occurrence
	static Uint64 getNodeWeight(NodeID nodeID, SCPQuorumSet qset)
	{
	    BigInteger n = BigInteger.valueOf(qset.getThreshold().getUint32());
	    BigInteger d = BigInteger.valueOf(qset.getInnerSets().length + qset.getValidators().length);
//	    Uint64 res = new Uint64();

	    for (PublicKey qsetNode : qset.getValidators())
	    {
	        if (XdrUtils.publicKeyEquals(qsetNode, nodeID.getNodeID()))
	        {
	            return XdrUtils.bigDivide64(XdrUtils.UINT64_MAX, n, d, XdrUtils.Rounding.ROUND_DOWN);
	        }
	    }

	    for (SCPQuorumSet q : qset.getInnerSets())
	    {
	    	Uint64 leafW = getNodeWeight(nodeID, q);
	        if (leafW.getUint64() != 0)
	        {
	        	return XdrUtils.bigDivide64(BigInteger.valueOf(leafW.getUint64()), n, d, XdrUtils.Rounding.ROUND_DOWN);
	        }
	    }
	    Uint64 zero = new Uint64();
	    zero.setUint64(0L);
	    return zero;
	}

	private static boolean isQuorumSliceInternal(SCPQuorumSet qset, LinkedHashSet<NodeID> nodeSet)
	{
	    int thresholdLeft = qset.getThreshold().getUint32();
	    for (PublicKey validator : qset.getValidators())
	    {
	    	//TODO ugly contains() replace with set/list of elements implementing equals
	    		if(XdrUtils.contains(nodeSet, validator)) {
		            thresholdLeft--;
		            if (thresholdLeft <= 0)
		            {
		                return true;
		            }
	    		}
	    }

	    for (SCPQuorumSet inner : qset.getInnerSets())
	    {
	        if (isQuorumSliceInternal(inner, nodeSet))
	        {
	            thresholdLeft--;
	            if (thresholdLeft <= 0)
	            {
	                return true;
	            }
	        }
	    }
	    return false;
	}

	static boolean isQuorumSlice(SCPQuorumSet qSet,LinkedHashSet<NodeID> nodeSet)
	{
	    log.trace("SCP LocalNode::isQuorumSlice nodeSet.size:{}", nodeSet.size());

	    return isQuorumSliceInternal(qSet, nodeSet);
	}

	// called recursively
	static boolean isVBlockingInternal(SCPQuorumSet qset, LinkedHashSet<NodeID> nodeSet)
	{
	    // There is no v-blocking set for {\empty}
	    if (qset.getThreshold().getUint32() == 0)
	    {
	        return false;
	    }

	    int leftTillBlock =
	        (int)((1 + qset.getValidators().length + qset.getInnerSets().length) -
	              qset.getThreshold().getUint32());

	    for (PublicKey validator : qset.getValidators())
	    {
	    	if(XdrUtils.contains(nodeSet, validator))
	        {
	            leftTillBlock--;
	            if (leftTillBlock <= 0)
	            {
	                return true;
	            }
	        }
	    }
	    for (SCPQuorumSet inner : qset.getInnerSets())
	    {
	        if (isVBlockingInternal(inner, nodeSet))
	        {
	            leftTillBlock--;
	            if (leftTillBlock <= 0)
	            {
	                return true;
	            }
	        }
	    }

	    return false;
	}

	static boolean isVBlocking(SCPQuorumSet qSet, LinkedHashSet<NodeID> nodeSet)
	{
	    log.trace("SCP LocalNode::isVBlocking nodeSet.size:{}", nodeSet.size());

	    return isVBlockingInternal(qSet, nodeSet);
	}

	public static boolean isVBlocking(SCPQuorumSet qSet, Map<NodeID, SCPEnvelope> mLatestEnvelopes,
			StatementPredicate filter) {
	    LinkedHashSet<NodeID> pNodes = new LinkedHashSet<>();
	    for (Entry<NodeID, SCPEnvelope> it : mLatestEnvelopes.entrySet())
	    {
	        if (filter.test(it.getValue().getStatement()))
	        {
	            pNodes.add(it.getKey());
	        }
	    }

	    return isVBlocking(qSet, pNodes);
	}

	public static boolean isQuorum(SCPQuorumSet qSet, Map<NodeID, SCPEnvelope> mLatestEnvelopes, final Function<SCPStatement, SCPQuorumSet> qfun,
			StatementPredicate filter) {
	    LinkedHashSet<NodeID> pNodes = new LinkedHashSet<>();
	    for (Entry<NodeID, SCPEnvelope> it : mLatestEnvelopes.entrySet())
	    {
	        if (filter.test(it.getValue().getStatement()))
	        {
	            pNodes.add(it.getKey());
	        }
	    }

	    int count = 0;
	    do
	    {
	    	
	        count = pNodes.size();
	        LinkedHashSet<NodeID> fNodes = new LinkedHashSet<>();
	        //std::vector<NodeID> fNodes(pNodes.size());
	        BiPredicate<NodeID, LinkedHashSet<NodeID>> quorumFilter =(NodeID nodeID, LinkedHashSet<NodeID> __pNodes__) -> {
	        	SCPEnvelope scpEnvelope = mLatestEnvelopes.get(nodeID);
	        	if(scpEnvelope == null) {
	        		return false;//TODO needed ??
	        	}
				SCPQuorumSet qSetPtr = qfun.apply(scpEnvelope.getStatement());
	            if (qSetPtr != null)
	            {
	                return isQuorumSlice(qSetPtr, __pNodes__);
	            }
	            else
	            {
	                return false;
	            }
	        };
	        for(NodeID nid : pNodes) {
	        	if(quorumFilter.test(nid, pNodes)) {
	        		fNodes.add(nid);
	        	}
	        }
	        pNodes = fNodes;
	    } while (count != pNodes.size());

	    return isQuorumSlice(qSet, pNodes);
	}


//	std::vector<NodeID>
//	LocalNode::findClosestVBlocking(
//	    SCPQuorumSet const& qset, std::map<NodeID, SCPEnvelope> const& map,
//	    std::function<bool(SCPStatement const&)> const& filter,
//	    NodeID const* excluded)
//	{
//	    std::set<NodeID> s;
//	    for (auto const& n : map)
//	    {
//	        if (filter(n.second.statement))
//	        {
//	            s.emplace(n.first);
//	        }
//	    }
//	    return findClosestVBlocking(qset, s, excluded);
//	}

	LinkedHashSet<NodeID> findClosestVBlocking(SCPQuorumSet qset,
	                                Set<NodeID> nodes,
	                                @Nullable NodeID excluded)
	{
	    int leftTillBlock =
	        ((1 + qset.getValidators().length + qset.getInnerSets().length) - qset.getThreshold().getUint32());

	    LinkedHashSet<NodeID> res = new LinkedHashSet<>();

	    // first, compute how many top level items need to be blocked
	    for (PublicKey validator : qset.getValidators())
	    {
	        if (excluded == null || !(XdrUtils.publicKeyEquals(validator, excluded.getNodeID())))
	        {
	            if (! XdrUtils.contains(nodes, validator))
	            {
	                leftTillBlock--;
	                if (leftTillBlock == 0)
	                {
	                    // already blocked
	                    return new LinkedHashSet<NodeID>();
	                }
	            }
	            else
	            {
	            	NodeID nid = new NodeID();
	            	nid.setNodeID(validator);
	                // save this for later
	                res.add(nid);
	            }
	        }
	    }

//	    struct orderBySize
//	    {
//	        bool
//	        operator()(std::vector<NodeID> const& v1, std::vector<NodeID> const& v2)
//	        {
//	            return v1.size() < v2.size();
//	        }
//	    };

	    List<LinkedHashSet<NodeID>> resInternals = new LinkedList<>();

	    for (SCPQuorumSet inner : qset.getInnerSets())
	    {
	    	LinkedHashSet<NodeID> v = findClosestVBlocking(inner, nodes, excluded);
	        if (v.size() == 0)
	        {
	            leftTillBlock--;
	            if (leftTillBlock == 0)
	            {
	                // already blocked
	                return new LinkedHashSet<NodeID>();
	            }
	        }
	        else
	        {
	            resInternals.add(v);
	        }
	    }
	    Collections.sort(resInternals, new Comparator<LinkedHashSet<NodeID>>() {
			@Override
			public int compare(LinkedHashSet<NodeID> o1, LinkedHashSet<NodeID> o2) {
				return Integer.valueOf(o1.size()).compareTo(o2.size());//TODO correct or reverse ??
			}
		});
	    // use the top level validators to get closer
//	    if (res.size() > leftTillBlock)
//	    {
//	        res.resize(leftTillBlock);
//	    }
	    leftTillBlock -= res.size();

	    // use subsets to get closer, using the smallest ones first
	    Iterator<LinkedHashSet<NodeID>> it = resInternals.iterator();
	    while (leftTillBlock != 0 && it.hasNext())
	    {
	        res.addAll(it.next());
	        leftTillBlock--;
	    }
	    return res;
	}

	public JsonElement toJson(SCPQuorumSet qSet)
	{
		JsonObject res = new JsonObject();
		res.addProperty("t", qSet.getThreshold().getUint32());
		JsonArray varr = new JsonArray();
	    for (PublicKey v : qSet.getValidators())
	    {
	        varr.add(mSCP.getDriver().toShortString(v));
	    }
	    for (SCPQuorumSet s : qSet.getInnerSets())
	    {
	    	varr.add(toJson(s));
	    }
	    res.add("v", varr);
	  return res;
	}

	String to_string(SCPQuorumSet qSet)
	{
	    return toJson(qSet).toString();
	}

	NodeID getNodeID()
	{
	    return mNodeID;
	}

	boolean isValidator()
	{
	    return mIsValidator;
	}

	SCP.TriBool isNodeInQuorum(
	    NodeID node,
	    Function<SCPStatement,SCPQuorumSet> qfun,
	    Map<NodeID, List<SCPStatement>> map)
	{
	    // perform a transitive search, starting with the local node
	    // the order is not important, so we can use sets to keep track of the work
	    HashSet<NodeID> backlog = new HashSet<>();
	    HashSet<NodeID> visited = new HashSet<>();
	    backlog.add(mNodeID);

	    SCP.TriBool res = SCP.TriBool.TB_FALSE;

	    while (backlog.size() != 0)
	    {
	    	Iterator<NodeID> it = backlog.iterator();
	        NodeID c = it.next();
	        if (XdrUtils.nodeIDEquals(c, node))
	        {
	            return SCP.TriBool.TB_TRUE;
	        }
	        backlog.remove(c);
	        visited.add(c);

	        List<SCPStatement> ite = map.get(c);
	        if (ite == null)
	        {
	            // can't lookup information on this node
	            res = SCP.TriBool.TB_MAYBE;
	            continue;
	        }
	        for (SCPStatement st : ite)
	        {
	        	SCPQuorumSet qset = qfun.apply(st);
	            if (qset == null)
	            {
	                // can't find the quorum set
	                res = SCP.TriBool.TB_MAYBE;
	                continue;
	            }
	            // see if we need to explore further
	            forAllNodes(qset, (NodeID n) -> {
	                if (! XdrUtils.contains(visited, n.getNodeID()))
	                {
	                    backlog.add(n);
	                }
	            });
	        }
	    }
	    return res;
	}

}
