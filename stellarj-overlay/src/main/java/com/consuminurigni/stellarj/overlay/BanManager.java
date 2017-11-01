package com.consuminurigni.stellarj.overlay;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.consumimurigni.stellarj.crypto.KeyUtils;
import com.consuminurigni.stellarj.common.Database;
import com.consuminurigni.stellarj.xdr.NodeID;

public class BanManager {

	private final Database db;
	private final JdbcTemplate tpl;
	public BanManager(Database database) {
		this.db = database;
		this.tpl = db.getJdbcTemplate();
	}

	public void banNode(NodeID nodeID)
	{
	    String nodeIDString = KeyUtils.toStrKey(nodeID);
	    //TODO auto timer = mApp.getDatabase().getInsertTimer("ban");
	    tpl.update("INSERT INTO ban (nodeid) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM ban WHERE nodeid = ?", nodeIDString, nodeIDString);
	}

	public void unbanNode(NodeID nodeID)
	{
	    String nodeIDString = KeyUtils.toStrKey(nodeID);
	    //TODO auto timer = mApp.getDatabase().getDeleteTimer("ban");
	    tpl.update("DELETE FROM ban WHERE nodeid = ?", nodeIDString);
	}

	public boolean isBanned(NodeID nodeID)
	{
	    String nodeIDString = KeyUtils.toStrKey(nodeID);
	    //TODO auto timer = mApp.getDatabase().getSelectTimer("ban");
	    return 1 == tpl.queryForObject("SELECT count(*) FROM ban WHERE nodeid = ?", Integer.class, nodeIDString);
	}

	public List<String> getBans()
	{
	    //TODO auto timer = mApp.getDatabase().getSelectTimer("ban");
	    return tpl.queryForList("SELECT nodeid FROM ban", String.class);
	}

	public void dropAll(Database db)
	{
		tpl.update("DROP TABLE IF EXISTS ban");
		tpl.update("CREATE TABLE ban (nodeid CHARACTER(56) NOT NULL PRIMARY KEY)");
	}

}
