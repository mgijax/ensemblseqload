package org.jax.mgi.app.vega_ensemblseqloader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
    
import org.jax.mgi.dbs.mgd.TranslationTypeConstants;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.shr.cache.CacheConstants;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.cache.FullCachedLookup;
import org.jax.mgi.shr.cache.KeyNotFoundException;
import org.jax.mgi.shr.cache.KeyValue;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.dbutils.MultiRowInterpreter;
import org.jax.mgi.shr.dbutils.RowDataInterpreter;
import org.jax.mgi.shr.dbutils.RowReference;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;

/**
 * @is a FullCachedLookup for caching marker keys by their
 * MGI IDs
 * @has a RowDataCacheStrategy of type FULL_CACHE used for creating the
 * cache and performing the cache lookup
 * @does provides a lookup method for getting the marker key for a marker 
 *  MGI ID
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */
public class MarkerMGIIDLookupByAssocObjectID extends FullCachedLookup
{
  // provide a static cache so that all instances share one cache
  private static HashMap cache = new HashMap();

  // indicator of whether or not the cache has been initialized
  private static boolean hasBeenInitialized = false;

  // logicalDB of the sequences to lookup
  private String objectLdbKey;

  // division of sequences, if applicable
  private String division = null;

   /**
   * constructor
   * @throws CacheException thrown if there is an error with the cache
   * @throws DBException thrown if there is an error accessing the db
   * @throws ConfigException thrown if there is an error accessing the
   * configuration file
   */
  public MarkerMGIIDLookupByAssocObjectID(Integer oLdbKey)
      throws CacheException, DBException, ConfigException {
    super(SQLDataManagerFactory.getShared(SchemaConstants.MGD));
    // since cache is static make sure you do not reinit
    if (!hasBeenInitialized) {
      initCache(cache);
    }
    hasBeenInitialized = true;

    this.objectLdbKey = oLdbKey.toString();
    //System.out.println("ldbKey: " + oLdbKey);
  }

  /**
   * look up the marker key for a marker MGI ID
   * @param mgiID MGI ID to lookup
   * @return the marker Key
   * @throws CacheException thrown if there is an error accessing the cache
   * @throws ConfigException thrown if there is an error accessing the
   * configuration
   * @throws DBException thrown if there is an error accessing the
   * database
   */
  public HashSet lookup(String objectID) throws CacheException,
      DBException, ConfigException {
           return (HashSet)super.lookupNullsOk(objectID);
  }

  /**
   * get the full initialization query which is called by the CacheStrategy
   * class when performing cache initialization
   * @assumes nothing
   * @effects nothing
   * @return the full initialization query
   */
 public String getFullInitQuery() {
    /**
     * assumption is that an MGI ID is associated with only one marker
     * this is true except for some of the old MGD_MRK-##### format IDs
     * We include non-preferred IDs so we can get the most current marker 
     * MGI ID for merged/withdrawn markers
     */
    String s = "SELECT a1.accID as mgiID, a2.accID as objectID " +
	    "from ACC_Accession a1, ACC_Accession a2 " +
	    "where a1._MGIType_key = 2 " +
	    "and a1._LogicalDB_key = 1 " +
	    "and a1.preferred = 1 " +
	    "and a1._Object_key = a2._Object_key " +
	    "and a2._MGIType_key = 2 " +
	    "and a2._LogicalDB_key =  " + objectLdbKey + " " +
	    "and a2.preferred = 1 " +
	    "order by a2.accID";
        return s;
    }


  /**
   * get the RowDataInterpreter which is required by the CacheStrategy to
   * read the results of a database query.
   * @assumes nothing
   * @effects nothing
   * @return the partial initialization query
   */
  public RowDataInterpreter getRowDataInterpreter() {
      class Interpreter implements MultiRowInterpreter {
	    public Object interpret(RowReference row) throws DBException {
		//return new KeyValue(row.getString(1), row.getString(2));
		return new RowData(row);
	    }

	    public Object interpretKey(RowReference ref) throws DBException {
                return ref.getString(2);
            }

	    public Object interpretRows(Vector v) {
		RowData rd = (RowData)v.get(0);
		String objectID = rd.objectID; 
		HashSet mgiIDSet = new HashSet();
		for (Iterator it = v.iterator(); it.hasNext();) {
		    rd = (RowData)it.next();
		    mgiIDSet.add(rd.mgiID);
		}
		//System.out.println("objectID: " + objectID + " mgiIDSet: " + mgiIDSet.toString());
		return new KeyValue(objectID, mgiIDSet);
	    }
      }
    return new Interpreter();
  }
    /**
     * Simple data object representing a row of data from the query
     */
    class RowData {
        protected String objectID;
        protected String mgiID;
        public RowData (RowReference row) throws DBException {
            mgiID = row.getString(1);
            objectID = row.getString(2);
	    //System.out.println("mgiID: " + mgiID + "objectID: " + objectID);
        }
    }
}

