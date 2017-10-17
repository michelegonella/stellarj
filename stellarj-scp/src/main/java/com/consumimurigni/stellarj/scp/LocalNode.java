package com.consumimurigni.stellarj.scp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.crypto.SecretKey;
import com.consuminurigni.stellarj.scp.xdr.SCPQuorumSet;
import com.consuminurigni.stellarj.scp.xdr.SCPStatement;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.NodeID;


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
			mQSetHash =  new Hash(scp.getHashingFunction().apply(mQSet.encode()));
			log.info("SCP LocalNode::LocalNode @{} qSet:{}", mNodeID.getNodeID().toShortString(),mQSetHash.hexAbbrev());

			mSingleQSet = SCPQuorumSet.buildSingletonQSet(mNodeID);

			gSingleQSetHash =  new Hash(scp.getHashingFunction().apply(mSingleQSet.encode()));
		}


		void updateQuorumSet(SCPQuorumSet qSet)
		{
			mQSet.setStateFrom(qSet);
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

//	static void forAllNodesInternal(SCPQuorumSet qset, Consumer<NodeID> proc)
//	{
//	    for (PublicKey n : qset.getValidators())
//	    {
//	    	NodeID nid = new NodeID();
//	    	nid.setNodeID(n);
//	        proc.accept(nid);
//	    }
//	    for (SCPQuorumSet q : qset.getInnerSets())
//	    {
//	        forAllNodesInternal(q, proc);
//	    }
//	}
//
//	// runs proc over all nodes contained in qset
//	static void forAllNodes(SCPQuorumSet qset, Consumer<NodeID> proc)
//	{
//		final Equivalence<NodeID> eq = new Equivalence<NodeID>() {
//			@Override
//			protected boolean doEquivalent(NodeID nid0, NodeID nid1) {
//				
//				return XdrUtils.publicKeyEquals(nid0.getNodeID(), nid1.getNodeID())
//				;
//			}
//			@Override
//			protected int doHash(NodeID nid) {
//				return Arrays.hashCode(nid.getNodeID().getEd25519().getUint256());
//			}
//			
//		};
//	    Set<Equivalence.Wrapper<NodeID>> done = new HashSet<>();
//	    forAllNodesInternal(qset, (NodeID n) -> {
//	    	Wrapper<NodeID> wrappedNid = eq.wrap(n);
//	        if (! done.contains(wrappedNid))
//	        {
//	            proc.accept(n);
//	            done.add(wrappedNid);
//	        }
//	    });
//	}
//
//	// if a validator is repeated multiple times its weight is only the
//	// weight of the first occurrence
//	static Uint64 getNodeWeight(NodeID nodeID, SCPQuorumSet qset)
//	{
//	    BigInteger n = BigInteger.valueOf(qset.getThreshold().getUint32());
//	    BigInteger d = BigInteger.valueOf(qset.getInnerSets().length + qset.getValidators().length);
////	    Uint64 res = new Uint64();
//
//	    for (PublicKey qsetNode : qset.getValidators())
//	    {
//	        if (qsetNode.eq(nodeID))
//	        {
//	            return XdrInteger.bigDivide64(XdrInteger.UINT64_MAX, n, d, XdrInteger.Rounding.ROUND_DOWN);
//	        }
//	    }
//
//	    for (SCPQuorumSet q : qset.getInnerSets())
//	    {
//	    	Uint64 leafW = getNodeWeight(nodeID, q);
//	        if (leafW.neq(0))
//	        {
//	        	return XdrInteger.bigDivide64(leafW.toBigInteger(), n, d, XdrInteger.Rounding.ROUND_DOWN);
//	        }
//	    }
//	    return Uint64.ZERO;
//	}
//
//	private static boolean isQuorumSliceInternal(SCPQuorumSet qset, NodeSet nodeSet)
//	{
//	    int thresholdLeft = qset.getThreshold().getUint32();
//	    for (PublicKey validator : qset.getValidators())
//	    {
//	    	//TODO ugly contains() replace with set/list of elements implementing equals
//	    		if(nodeSet.contains(validator.toNodeID())) {
//		            thresholdLeft--;
//		            if (thresholdLeft <= 0)
//		            {
//		                return true;
//		            }
//	    		}
//	    }
//
//	    for (SCPQuorumSet inner : qset.getInnerSets())
//	    {
//	        if (isQuorumSliceInternal(inner, nodeSet))
//	        {
//	            thresholdLeft--;
//	            if (thresholdLeft <= 0)
//	            {
//	                return true;
//	            }
//	        }
//	    }
//	    return false;
//	}
//
//	static boolean isQuorumSlice(SCPQuorumSet qSet,NodeSet nodeSet)
//	{
//	    log.trace("SCP LocalNode::isQuorumSlice nodeSet.size:{}", nodeSet.size());
//
//	    return isQuorumSliceInternal(qSet, nodeSet);
//	}
//
//	// called recursively
//	static boolean isVBlockingInternal(SCPQuorumSet qset, NodeSet nodeSet)
//	{
//	    // There is no v-blocking set for {\empty}
//	    if (qset.getThreshold().getUint32() == 0)
//	    {
//	        return false;
//	    }
//
//	    int leftTillBlock =
//	        (int)((1 + qset.getValidators().length + qset.getInnerSets().length) -
//	              qset.getThreshold().getUint32());
//
//	    for (PublicKey validator : qset.getValidators())
//	    {
//	    	if(nodeSet.contains(validator))
//	        {
//	            leftTillBlock--;
//	            if (leftTillBlock <= 0)
//	            {
//	                return true;
//	            }
//	        }
//	    }
//	    for (SCPQuorumSet inner : qset.getInnerSets())
//	    {
//	        if (isVBlockingInternal(inner, nodeSet))
//	        {
//	            leftTillBlock--;
//	            if (leftTillBlock <= 0)
//	            {
//	                return true;
//	            }
//	        }
//	    }
//
//	    return false;
//	}
//
//	static boolean isVBlocking(SCPQuorumSet qSet, NodeSet nodeSet)
//	{
//	    log.trace("SCP LocalNode::isVBlocking nodeSet.size:{}", nodeSet.size());
//
//	    return isVBlockingInternal(qSet, nodeSet);
//	}
//
//	public static boolean isVBlocking(SCPQuorumSet qSet, Map<NodeID, SCPEnvelope> mLatestEnvelopes,
//			Predicate<SCPStatement> filter) {
//		NodeSet pNodes = new NodeSet();
//	    for (Entry<NodeID, SCPEnvelope> it : mLatestEnvelopes.entrySet())
//	    {
//	        if (filter.test(it.getValue().getStatement()))
//	        {
//	            pNodes.add(it.getKey());
//	        }
//	    }
//
//	    return isVBlocking(qSet, pNodes);
//	}
//
//	public static boolean isQuorum(SCPQuorumSet qSet, Map<NodeID, SCPEnvelope> mLatestEnvelopes, final Function<SCPStatement, SCPQuorumSet> qfun,
//			Predicate<SCPStatement> filter) {
//		NodeSet pNodes = new NodeSet();
//	    for (Entry<NodeID, SCPEnvelope> it : mLatestEnvelopes.entrySet())
//	    {
//	        if (filter.test(it.getValue().getStatement()))
//	        {
//	            pNodes.add(it.getKey());
//	        }
//	    }
//
//	    int count = 0;
//	    do
//	    {
//	    	
//	        count = pNodes.size();
//	        NodeSet fNodes = new NodeSet();
//	        //std::vector<NodeID> fNodes(pNodes.size());
//	        BiPredicate<NodeID, NodeSet> quorumFilter =(NodeID nodeID, NodeSet __pNodes__) -> {
//	        	SCPEnvelope scpEnvelope = mLatestEnvelopes.get(nodeID);
//	        	if(scpEnvelope == null) {
//	        		return false;//TODO needed ??
//	        	}
//				SCPQuorumSet qSetPtr = qfun.apply(scpEnvelope.getStatement());
//	            if (qSetPtr != null)
//	            {
//	                return isQuorumSlice(qSetPtr, __pNodes__);
//	            }
//	            else
//	            {
//	                return false;
//	            }
//	        };
//	        for(NodeID nid : pNodes) {
//	        	if(quorumFilter.test(nid, pNodes)) {
//	        		fNodes.add(nid);
//	        	}
//	        }
//	        pNodes = fNodes;
//	    } while (count != pNodes.size());
//
//	    return isQuorumSlice(qSet, pNodes);
//	}
//
//
////	std::vector<NodeID>
////	LocalNode::findClosestVBlocking(
////	    SCPQuorumSet const& qset, std::map<NodeID, SCPEnvelope> const& map,
////	    std::function<bool(SCPStatement const&)> const& filter,
////	    NodeID const* excluded)
////	{
////	    std::set<NodeID> s;
////	    for (auto const& n : map)
////	    {
////	        if (filter(n.second.statement))
////	        {
////	            s.emplace(n.first);
////	        }
////	    }
////	    return findClosestVBlocking(qset, s, excluded);
////	}
//
//	NodeSet findClosestVBlocking(SCPQuorumSet qset,
//			NodeSet nodes,
//	                                @Nullable NodeID excluded)
//	{
//	    int leftTillBlock =
//	        ((1 + qset.getValidators().length + qset.getInnerSets().length) - qset.getThreshold().getUint32());
//
//	    NodeSet res = new NodeSet();
//
//	    // first, compute how many top level items need to be blocked
//	    for (PublicKey validator : qset.getValidators())
//	    {
//	        if (excluded == null || !(validator.eq(excluded)))
//	        {
//	            if (! nodes.contains(validator.toNodeID()))
//	            {
//	                leftTillBlock--;
//	                if (leftTillBlock == 0)
//	                {
//	                    // already blocked
//	                    return new NodeSet();
//	                }
//	            }
//	            else
//	            {
//	            	NodeID nid = new NodeID();
//	            	nid.setNodeID(validator);
//	                // save this for later
//	                res.add(nid);
//	            }
//	        }
//	    }
//
////	    struct orderBySize
////	    {
////	        bool
////	        operator()(std::vector<NodeID> const& v1, std::vector<NodeID> const& v2)
////	        {
////	            return v1.size() < v2.size();
////	        }
////	    };
//
//	    List<NodeSet> resInternals = new LinkedList<>();
//
//	    for (SCPQuorumSet inner : qset.getInnerSets())
//	    {
//	    	NodeSet v = findClosestVBlocking(inner, nodes, excluded);
//	        if (v.size() == 0)
//	        {
//	            leftTillBlock--;
//	            if (leftTillBlock == 0)
//	            {
//	                // already blocked
//	                return new NodeSet();
//	            }
//	        }
//	        else
//	        {
//	            resInternals.add(v);
//	        }
//	    }
//	    Collections.sort(resInternals, NodeSet.BY_SIZE_COMPARATOR);
//	    leftTillBlock -= res.size();
//
//	    // use subsets to get closer, using the smallest ones first
//	    Iterator<NodeSet> it = resInternals.iterator();
//	    while (leftTillBlock != 0 && it.hasNext())
//	    {
//	        res.addAll(it.next());
//	        leftTillBlock--;
//	    }
//	    return res;
//	}

//	public JsonElement toJson(SCPQuorumSet qSet)
//	{
//		JsonObject res = new JsonObject();
//		res.addProperty("t", qSet.getThreshold().getUint32());
//		JsonArray varr = new JsonArray();
//	    for (PublicKey v : qSet.getValidators())
//	    {
//	        varr.add(mSCP.getDriver().toShortString(v));
//	    }
//	    for (SCPQuorumSet s : qSet.getInnerSets())
//	    {
//	    	varr.add(toJson(s));
//	    }
//	    res.add("v", varr);
//	  return res;
//	}
//
//	String to_string(SCPQuorumSet qSet)
//	{
//	    return toJson(qSet).toString();
//	}

	public NodeID getNodeID()
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
	        if (c.equals(node))
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
	            qset.forAllNodes((NodeID n) -> {
	                if (! visited.contains(n))
	                {
	                    backlog.add(n);
	                }
	            });
	        }
	    }
	    return res;
	}

}
