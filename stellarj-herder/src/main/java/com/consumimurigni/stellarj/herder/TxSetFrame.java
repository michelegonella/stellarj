package com.consumimurigni.stellarj.herder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.consumimurigni.stellarj.crypto.SHA256;
import com.consumimurigni.stellarj.ledger.LedgerManager;
import com.consumimurigni.stellarj.ledger.xdr.AccountID;
import com.consumimurigni.stellarj.ledger.xdr.LedgerHeaderHistoryEntry;
import com.consumimurigni.stellarj.ledger.xdr.SequenceNumber;
import com.consumimurigni.stellarj.ledger.xdr.TransactionSet;
import com.consumimurigni.stellarj.ledgerimpl.xdr.TransactionEnvelope;
import com.consumimurigni.stellarj.transactions.TransactionFrame;
import com.consuminurigni.stellarj.common.Database;
import com.consuminurigni.stellarj.metering.Metrics;
import com.consuminurigni.stellarj.xdr.Hash;
import com.consuminurigni.stellarj.xdr.Int64;
import com.consuminurigni.stellarj.xdr.Xdr;

//TODO mv to ledger ?
public class TxSetFrame {
	private static final Logger log = LogManager.getLogger();
    private boolean mHashIsValid = false;
    private Hash mHash = null;

    private final Hash mPreviousLedgerHash;

    private final List<TransactionFrame> mTransactions;
	//copy constructor
	public TxSetFrame(TxSetFrame other) {
		this.mHashIsValid = other.mHashIsValid;
		this.mHash = other.mHash;
		this.mPreviousLedgerHash = other.mPreviousLedgerHash;
		this.mTransactions = new LinkedList<>(other.mTransactions);
	}

	public TxSetFrame(Hash previousLedgerHash) {
		mHashIsValid = false;
		mPreviousLedgerHash = previousLedgerHash;
		mTransactions = new LinkedList<>();
	}

	public TxSetFrame(Hash networkID, TransactionSet xdrSet) {
		mTransactions = new LinkedList<>();
		mHashIsValid = false;
	    mPreviousLedgerHash = xdrSet.getPreviousLedgerHash();
		    for (TransactionEnvelope txEnvelope : xdrSet.getTxs())
		    {
		        TransactionFrame tx =
		            TransactionFrame.makeTransactionFromWire(networkID, txEnvelope);
		        mTransactions.add(tx);
		    }
	}

	public Hash getContentsHash() {
	    if (!mHashIsValid)
	    {
	        sortForHash();
	        SHA256 hasher = SHA256.create();
	        hasher.add(mPreviousLedgerHash.encode());
	        for (int n = 0; n < mTransactions.size(); n++)
	        {
	            hasher.add(mTransactions.get(n).getEnvelope().encode());
	        }
	        mHash = hasher.finish().toHash();
	        mHashIsValid = true;
	    }
	    return mHash;
	}

	// order the txset correctly
	// must take into account multiple tx from same account
	void sortForHash() {
	    Collections.sort(mTransactions, null/*TODO HashTxSorter*/);
	    mHashIsValid = false;
	}

	// need to make sure every account that is submitting a tx has enough to pay
	// the fees of all the tx it has submitted in this set
	// check seq num
	public boolean checkValid(LedgerManager ledgerManager, Metrics metrics) {
	    // Establish read-only transaction for duration of checkValid.
//	    soci::transaction sqltx(app.getDatabase().getSession());
//	    app.getDatabase().setCurrentTransactionReadOnly();

	    LedgerHeaderHistoryEntry lcl = ledgerManager.getLastClosedLedgerHeader();
	    // Start by checking previousLedgerHash
	    if (! lcl.getHash().equals(mPreviousLedgerHash))
	    {
	        log.debug("Herder Got bad txSet: {} ; expected: {}", mPreviousLedgerHash.hexAbbrev(),
	        		ledgerManager.getLastClosedLedgerHeader().getHash().hexAbbrev());
	        return false;
	    }

	    if (mTransactions.size() > lcl.getHeader().getMaxTxSetSize().intValue())
	    {
	        log.debug("Herder Got bad txSet: too many txs {} > {}", mTransactions.size(), lcl.getHeader().getMaxTxSetSize());
	        return false;
	    }

	    TreeMap<AccountID, List<TransactionFrame>> accountTxMap = new TreeMap<>();

	    Hash lastHash = Hash.createEmpty();//TODO INIT
	    for (TransactionFrame tx : mTransactions)
	    {
	        // make sure the set is sorted correctly
	        if (tx.getFullHash().lt(lastHash))
	        {
	            log.debug("Herder bad txSet: {} not sorted correctly", mPreviousLedgerHash.hexAbbrev());
	            return false;
	        }
	        List<TransactionFrame> l = accountTxMap.get(tx.getSourceID());
	        if(l == null) {
	        	l = new LinkedList<>();
	        	accountTxMap.put(tx.getSourceID(), l);
	        }
	        l.add(tx);
	        lastHash = tx.getFullHash();
	    }

	    for (List<TransactionFrame> item : accountTxMap.values())
	    {
	        // order by sequence number
	        Collections.sort(item, SeqSorter);

	        TransactionFrame lastTx = null;
	        SequenceNumber lastSeq = SequenceNumber.of(0);
	        Int64 totFee = Int64.of(0);
	        for (TransactionFrame tx : item)
	        {
	            if (!tx.checkValid(ledgerManager, metrics, lastSeq))
	            {
	                log.debug("Herder bad txSet: {} tx invalid lastSeq:{} tx:{} result:{} "
	                    , mPreviousLedgerHash.hexAbbrev(), lastSeq.toString(), Xdr.xdr_to_string(tx.getEnvelope())
	                    , tx.getResultCode().name());

	                return false;
	            }
	            totFee = totFee.plus(tx.getFee());

	            lastTx = tx;
	            lastSeq = tx.getSeqNum();
	        }
	        if (lastTx != null)
	        {
	            // make sure account can pay the fee for all these tx
	            Int64 newBalance =
	                lastTx.getSourceAccount().getBalance().minus(totFee);
	            if (newBalance.lt(lastTx.getSourceAccount().getMinimumBalance(
	            		ledgerManager)))
	            {
	                log.debug("Herder bad txSet: {} account can't pay fee tx:{}", mPreviousLedgerHash.hexAbbrev(),Xdr.xdr_to_string(lastTx.getEnvelope()));

	                return false;
	            }
	        }
	    }
	    return true;
	}

	//TODO should we return a copy ? check how it is used and make a copy if possible
	public List<TransactionFrame> getTransactions() {
		return mTransactions;
	}

	public List<TransactionFrame> trimInvalid(Database database, LedgerManager ledgerManager, Metrics metrics) {
//	    app.getDatabase().setCurrentTransactionReadOnly();
		return database.getTransactionTemplate().execute((t)->{
			List<TransactionFrame> trimmed = new LinkedList<>();
		    sortForHash();
		    TreeMap<AccountID, List<TransactionFrame>> accountTxMap = new TreeMap<>();
		    for (TransactionFrame tx : mTransactions)
		    {
		        List<TransactionFrame> l = accountTxMap.get(tx.getSourceID());
		        if(l == null) {
		        	l = new LinkedList<>();
		        }
		        l.add(tx);
		    }

		    for (List<TransactionFrame> item : accountTxMap.values())
		    {
		        // order by sequence number
		        Collections.sort(item, SeqSorter);

		        TransactionFrame lastTx = null;
		        SequenceNumber lastSeq = SequenceNumber.of(0);
		        Int64 totFee = Int64.of(0);
		        for (TransactionFrame tx : item)
		        {
		            if (!tx.checkValid(ledgerManager, metrics, lastSeq))
		            {
		                trimmed.add(tx);
		                removeTx(tx);
		                continue;
		            }
		            totFee = totFee.plus(tx.getFee());

		            lastTx = tx;
		            lastSeq = tx.getSeqNum();
		        }
		        if (lastTx != null)
		        {
		            // make sure account can pay the fee for all these tx
		            Int64 newBalance =
		                lastTx.getSourceAccount().getBalance().minus(totFee);
		            if (newBalance.lt(lastTx.getSourceAccount().getMinimumBalance(
		                                 ledgerManager)))
		            {
		                for (TransactionFrame tx2 : item)
		                {
		                    trimmed.add(tx2);
		                    removeTx(tx2);
		                }
		            }
		        }
		    }
			return trimmed;
		});
	}

	public Hash previousLedgerHash() {
	    mHashIsValid = false;
	    return mPreviousLedgerHash;
	}

	public void add(TransactionFrame tx) {
        mTransactions.add(tx);
        mHashIsValid = false;
	}

	public void surgePricingFilter(LedgerManager lm) {
	    int max = lm.getMaxTxSetSize();
	    if (mTransactions.size() > max)
	    { // surge pricing in effect!
	        log.warn("Herder surge pricing in effect! {}", mTransactions.size());

	        // determine the fee ratio for each account
	        TreeMap<AccountID, Double> accountFeeMap = new TreeMap<>();
	        for (TransactionFrame tx : mTransactions)
	        {
	            double r = tx.getFeeRatio(lm);
	            Double now = accountFeeMap.get(tx.getSourceID());
	            if (now == null || now == 0.0) {
	                accountFeeMap.put(tx.getSourceID(), r);
	            } else if (r < now) {
	                accountFeeMap.put(tx.getSourceID(), r);
	            }
	        }

	        // sort tx by amount of fee they have paid
	        // remove the bottom that aren't paying enough
	        List<TransactionFrame> tempList = new ArrayList<>(mTransactions);
	        Collections.sort(tempList, new SurgeSorter(accountFeeMap));
	        
	        for (int i = max; i < tempList.size(); i++)
	        {
	            removeTx(tempList.get(i));
	        }
	    }
		
	}

	private static class SurgeSorter implements Comparator<TransactionFrame> {
	    private final Map<AccountID, Double> mAccountFeeMap;
	    SurgeSorter(Map<AccountID, Double> afm)
	    {
	    	mAccountFeeMap = afm;
	    }
		@Override
		public int compare(TransactionFrame tx1, TransactionFrame tx2) {
	        if (tx1.getSourceID().equals(tx2.getSourceID())) {
	            return tx1.getSeqNum().lt(tx2.getSeqNum()) ? -1 : 1;
	        }
	        //TODO risk npe ??
	        double fee1 = mAccountFeeMap.get(tx1.getSourceID());
	        double fee2 = mAccountFeeMap.get(tx2.getSourceID());
	        if (fee1 == fee2) {
	            return tx1.getSourceID().lt(tx2.getSourceID()) ? -1 : 1;
	        }
	        return fee1 > fee2 ? -1 : 1;
		}
		
	}


    int size()
    {
        return mTransactions.size();
    }

    void removeTx(TransactionFrame tx) {
    	mTransactions.remove(tx);
        mHashIsValid = false;
    }

	public TransactionSet toXDR() {
		TransactionEnvelope[] tEnvs = new TransactionEnvelope[mTransactions.size()];
	    for (int n = 0; n < mTransactions.size(); n++)
	    {
	    	tEnvs[n] = mTransactions.get(n).getEnvelope();
	    }
		TransactionSet txSet = new TransactionSet();
		txSet.setTxs(tEnvs);
	    txSet.setPreviousLedgerHash(mPreviousLedgerHash);
	    return txSet;
	}

	private final Comparator<TransactionFrame> SeqSorter = new Comparator<TransactionFrame>() {
		@Override
		public int compare(TransactionFrame t1, TransactionFrame t2) {
			return t1.getSeqNum().lt(t2.getSeqNum())? -1
			: t2.getSeqNum().lt(t1.getSeqNum())? 1 
			: 0;
		}
	};

	/*
    Build a list of transaction ready to be applied to the last closed ledger,
    based on the transaction set.

    The order satisfies:
    * transactions for an account are sorted by sequence number (ascending)
    * the order between accounts is randomized
*/
	public List<TransactionFrame> sortForApply() {
	    List<TransactionFrame> tmpList = new LinkedList<>(mTransactions);

	    ArrayList<List<TransactionFrame>> txBatches = new ArrayList<>(4);
	    TreeMap<AccountID, Integer> accountTxCountMap = new TreeMap<>();
	    // sort all the txs by seqnum
		Collections.sort(tmpList, SeqSorter);

	    // build the txBatches
	    // batch[i] contains the i-th transaction for any account with
	    // a transaction in the transaction set
	    for (TransactionFrame tx : tmpList)
	    {
	        Integer v = accountTxCountMap.get(tx.getSourceID());
	        if(v == null) {
	        	v = 0;
	        }
	        accountTxCountMap.put(tx.getSourceID(), v + 1);
	        if (v >= txBatches.size())
	        {
	            txBatches.ensureCapacity(v + 4);
	        }
	        txBatches.get(v).add(tx);
//	        v++;
	    }

	    List<TransactionFrame> retList = new LinkedList<>();

	    for (List<TransactionFrame> batch : txBatches)
	    {
	        // randomize each batch using the hash of the transaction set
	        // as a way to randomize even more
	    	Collections.sort(batch, new ApplyTxSorter(getContentsHash()));
	        for (TransactionFrame tx : batch)
	        {
	            retList.add(tx);
	        }
	    }

	    return retList;
	}

	// We want to XOR the tx hash with the set hash.
	// This way people can't predict the order that txs will be applied in
	private static class ApplyTxSorter implements Comparator<TransactionFrame>
	{
	    private final Hash mSetHash;
	    public ApplyTxSorter(Hash h)
	    {
	    	mSetHash = h;
	    }
		@Override
		public int compare(TransactionFrame tx1, TransactionFrame tx2) {
	        // need to use the hash of whole tx here since multiple txs could have
	        // the same Contents
	        return Hash.lessThanXored(tx1.getFullHash(), tx2.getFullHash(), mSetHash) ? -1 : 1;
		};
	}
}
