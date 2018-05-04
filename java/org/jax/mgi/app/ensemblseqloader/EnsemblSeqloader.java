package org.jax.mgi.app.ensemblseqloader;

import org.jax.mgi.shr.config.EnsemblSeqloadCfg;
import org.jax.mgi.shr.dla.input.fasta.FASTAData;
import org.jax.mgi.shr.dla.loader.FASTALoader;
import org.jax.mgi.shr.dla.loader.DLALoaderException;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.dbs.mgd.loads.SeqSrc.MSRawAttributes;
import org.jax.mgi.dbs.mgd.MGITypeConstants;
import org.jax.mgi.dbs.mgd.MGIRefAssocTypeConstants;
import org.jax.mgi.dbs.mgd.loads.Acc.AccessionRawAttributes;
import org.jax.mgi.shr.dla.input.SequenceInput;
import org.jax.mgi.dbs.mgd.loads.SeqRefAssoc.RefAssocRawAttributes;
import org.jax.mgi.dbs.mgd.loads.Seq.SequenceRawAttributes;
import org.jax.mgi.shr.dla.loader.seq.SeqloaderConstants;
import org.jax.mgi.dbs.mgd.lookup.LogicalDBLookup;
import org.jax.mgi.dbs.mgd.lookup.SequenceKeyLookupBySeqID;
import org.jax.mgi.shr.config.SequenceLoadCfg;
import org.jax.mgi.shr.ioutils.OutputDataFile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.HashSet;
/**
 * A FASTALoader for loading Ensembl Transcript and Protein sequences
 * @has MSRawAttributes, AccessionRawAttributes, SequenceRawAttributes,
 * RefAssocRawAttributes which are used as input to the SequenceInput object
 * which eventually gets processed by the SequenceProcessor. It also has
 * a SequenceLoadCfg performing configuration and a
 * @does loads sequence objects from Ensembl fasta files
 * @company The Jackson Laboratory
 * @author sc
 */

public class EnsemblSeqloader extends FASTALoader
{

    private MSRawAttributes msRaw = null;
    private AccessionRawAttributes accRaw = null;
    private SequenceRawAttributes seqRaw = null;
    private RefAssocRawAttributes refRaw = null;
    private SequenceLoadCfg seqCfg = null;
    private EnsemblSeqloadCfg loadCfg = null;
    private Boolean loadSeqs = Boolean.FALSE;
    private int seqCtr = 0;
    private int assocCtr = 0;
    private BufferedWriter assocFileWriter = null;
    private MarkerMGIIDLookupByAssocObjectID mgiIDLookup = null;
    private SequenceKeyLookupBySeqID seqKeyLookup = null;
    // set of gm Ids with no sequence in MGI
    private HashSet gmIdsNotInMGI = null;
    // set of gm Ids with sequence in MGI, but not associated with a marker
    private HashSet gmIdsNoMarkerInMGI = null;

    /**
     * constructor
     * @throws DLALoaderException thrown from the base class
     */
    public EnsemblSeqloader() throws DLALoaderException
    {
        super();
    }

    /**
     * initialize the instance variable
     * @assumes nothing
     * @effects nothing
     * @throws MGIException thrown if any error occurs during initialization
     */
    public void initialize() throws MGIException
    {
		super.initialize();
		logger.logdInfo("EnsemblSeqloader initializing", true);
		seqCfg = new SequenceLoadCfg();
		loadCfg = new EnsemblSeqloadCfg();
		loadSeqs = loadCfg.getLoadSeqs();
	        String assocLoadFile = loadCfg.getAssocLoadFileName();
		String gmLdb = loadCfg.getGeneModelLogicalDBName();
		String seqLdb = seqCfg.getLogicalDB();
		// lookup logical db key given logical db name
		LogicalDBLookup lookup = new LogicalDBLookup();
		Integer gmLdbKey = lookup.lookup(gmLdb);
	        try {
		    assocFileWriter = new BufferedWriter(
			new FileWriter(assocLoadFile));
		    assocFileWriter.write("MGI\t" + seqLdb + "\n");
		}
		catch (IOException e) {
		    throw new MGIException(e.getMessage());
		}
		mgiIDLookup = new
		    MarkerMGIIDLookupByAssocObjectID(gmLdbKey);
		mgiIDLookup.initCache();
		seqKeyLookup = new SequenceKeyLookupBySeqID(gmLdbKey.intValue());
		seqKeyLookup.initCache();
		gmIdsNotInMGI = new HashSet();
		gmIdsNoMarkerInMGI = new HashSet();
		// set molecular source attributes
		msRaw = new MSRawAttributes();
		msRaw.setCellLine(seqCfg.getCellLine());
		msRaw.setGender(seqCfg.getGender());
		msRaw.setLibraryName(null);
		msRaw.setOrganism(seqCfg.getOrganism());
		msRaw.setStrain(seqCfg.getStrain());
		msRaw.setTissue(seqCfg.getTissue());
		// set accession attributes
		accRaw = new AccessionRawAttributes();
		accRaw.setIsPreferred(new Boolean(true));
		accRaw.setIsPrivate(new Boolean(false));
		accRaw.setLogicalDB(seqLdb);
		accRaw.setMgiType(new Integer(MGITypeConstants.SEQUENCE));
		// set reference attributes
		refRaw = new RefAssocRawAttributes();
		refRaw.setMgiType(new Integer(MGITypeConstants.SEQUENCE));
		refRaw.setRefAssocType(new Integer(MGIRefAssocTypeConstants.PROVIDER));
		refRaw.setRefId(seqCfg.getJnumber());
		// set reusable sequence attributes
		// other record base attributes are set in the load method
		seqRaw = new SequenceRawAttributes();
		seqRaw.setAge(seqCfg.getAge());
		seqRaw.setCellLine(seqCfg.getCellLine());
		seqRaw.setDivision(null);
		seqRaw.setLibrary(null);
		seqRaw.setNumberOfOrganisms(0);
		seqRaw.setProvider(seqCfg.getProvider());
		seqRaw.setQuality(seqCfg.getQuality());
		seqRaw.setRawOrganisms(seqCfg.getOrganism());
		seqRaw.setRecord(null);
		seqRaw.setSeqDate(seqCfg.getReleaseDate());
		seqRaw.setSeqRecDate(seqCfg.getReleaseDate());
		seqRaw.setSex(seqCfg.getGender());
		seqRaw.setStrain(seqCfg.getStrain());
		seqRaw.setTissue(seqCfg.getTissue());
		seqRaw.setStatus(SeqloaderConstants.ACTIVE_STATUS);
		seqRaw.setType(seqCfg.getSeqType());
		seqRaw.setVersion(seqCfg.getReleaseNo());
		seqRaw.setVirtual(seqCfg.getVirtual());
		logger.logdInfo("EnsemblSeqloader completed initialization", true);
    }

    /**
     * runs the delete of existing data for this load
     * @assumes nothing
     * @effects the existing records form previous runs of this load will be
     * deleted
     * @throws MGIException thrown if any MGOException occurs during the delete
     */
    public void preprocess() throws MGIException
    {
      if (loadSeqs.equals(Boolean.TRUE) ) {
	  logger.logdInfo("deleting existing load data from previous runs", true);
	  seqProcessor.deleteSequences();
      }
    }

    /**
     * loads a fasta record into the database
     * @assumes the instance variables have been initialized
     * @effects the given fast record will be loaded into the database
     * @param data the incoming fasta record
     * @throws MGIException thrown if any error occurs during processing
     */
    public void load(FASTAData data)
        throws MGIException
    {
        SequenceInput seqin = new SequenceInput();
	String descript = data.getDescription();
	String seqID = data.getAccid();
	logger.logDebug("Processing sequence " + seqID);
	/** 
	 *parse descript for GM ID, resolve to marker MGI ID if possible
	 * write out MGI ID and seqID to assocload file
	 * Example of description we are tokenizing, Gene is 3rd
	 * whitespace delimitted token:
	 * pep:tot chromosome:????:11:3031890:3089055:-1 
	 *    Gene:????
	 * 	 Transcript:????
	 */
	StringTokenizer s = new StringTokenizer(descript);
	HashSet mgiIDSet = null;
	if (s.countTokens() < 3) {
           logger.logdInfo("Description has less than 3 tokens: " + descript, false);
           return;
        }
	// discard first token
	//System.out.println("First token: " + (String)s.nextToken());
	String discard = (String)s.nextToken();
	//System.out.println("First token: " + discard);
	discard = (String)s.nextToken();
	//System.out.println("Second token: " + discard);
	String t = (String)s.nextToken();
	String gmId = (t.substring(5));
	//System.out.println("gmID: '" + gmId + "'");
	//System.out.println("lookup: " + mgiIDLookup);
	
	// remove the version from gmId by splitting the token at '.'
        String[] gmIdSplit = gmId.split("\\.");	
	gmId = gmIdSplit[0];
	//System.out.println("gmID: '" + gmId + "'");
	
	// remove the version from seqID by splitting the token at '.'
        String[] seqIdSplit = seqID.split("\\.");	
	seqID = seqIdSplit[0];
	//System.out.println("seqID: '" + seqID + "'");
	
	// Is the Gene Model sequence in the database? If not report it, 
	// if so, is it associated with a marker? If not report it, if so
	// create association between marker and protein/transcript sequence
	Integer sequenceKey  = seqKeyLookup.lookup(gmId);
	if (sequenceKey == null) {
	    //logger.logcInfo("GM ID not in the database: " + gmId, false);
	    gmIdsNotInMGI.add(gmId);
	}
	else {
	    mgiIDSet = (HashSet)mgiIDLookup.lookup(gmId);
	    if (mgiIDSet != null) {
		for (Iterator i = mgiIDSet.iterator();i.hasNext();) {
		    String mgiID = (String)i.next();
		    //System.out.println("Gene assoc with GM ID: " + mgiID);
		    try {
			assocFileWriter.write(mgiID + "\t" +  seqID + "\n");    
		       assocCtr++;
		    }
		    catch (IOException e) {
			throw new MGIException(e.getMessage());
		    }
		}
	    }
	    else {
	       //logger.logcInfo("GM ID not associated with marker: " + gmId + 
		   // " therefore SeqID not associated with marker: " + seqID, false);
		gmIdsNoMarkerInMGI.add("GM ID not associated with marker: " + 
		    gmId + " therefore SeqID not associated with marker: " + seqID);
	    }
	}
	// Now finish building the sequence input object and send to processor
        seqRaw.setDescription(descript);
        seqRaw.setLength(new Integer(data.getSeqLength()).toString());
        accRaw.setAccid(seqID);
        seqin.addMSource(msRaw);
        seqin.setPrimaryAcc(accRaw);
        seqin.setSeq(seqRaw);
        seqin.addRef(refRaw);
	if (loadSeqs.equals(Boolean.TRUE) ) {
            super.seqProcessor.processInput(seqin);
	}
	seqCtr++;
  }
  /**
   * closes the SQLStreams reports stats
   * @throws MGIException thrown if an MGIException is thrown during post
   * processing
   */
  protected void postprocess() throws MGIException
  {
	if (loadSeqs.equals(Boolean.TRUE) ) {
	    logger.logdInfo("Total Sequences Loaded: " + seqCtr, false);
	    logger.logdInfo("Total Associations written to assocload file: " +
		 assocCtr, false);
	    logger.logcInfo(gmIdsNotInMGI.size() + 
		" Gene Models not found In MGI:", false);
	    for (Iterator i = gmIdsNotInMGI.iterator(); i.hasNext(); ) {
		logger.logcInfo( (String)i.next(), false);
	    }
	    logger.logcInfo(gmIdsNoMarkerInMGI.size() + 
		" Gene Models In MGI but no Marker Association:", false);
	    for (Iterator i = gmIdsNoMarkerInMGI.iterator(); i.hasNext(); ) {
		logger.logcInfo( (String)i.next(), false);
	    }
	    logger.logpInfo("Total Sequences Loaded: " + seqCtr, false);
            logger.logpInfo("Total Associations written to assocload file: " +
                 assocCtr, false);
        }
	else {
            logger.logdInfo("Total Associations written to assocload file: " +
                 assocCtr, false);
            logger.logpInfo("Total Associations written to assocload file: " +
                 assocCtr, false);
	}
	try {
	    assocFileWriter.close();
	} catch (IOException e) {
            throw new MGIException(e.getMessage());
	}
	if (loadSeqs.equals(Boolean.TRUE) ) {
	    super.postprocess();
       }
  }

}
