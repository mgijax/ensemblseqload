package org.jax.mgi.shr.config;

import org.jax.mgi.shr.config.Configurator;
import org.jax.mgi.shr.config.ConfigException;

/**
 * An object that retrieves Configuration parameters for the 
 * VegaEnsemblSeqload
 * @has Nothing
 * <UL>
 * <LI> a configuration manager
 * </UL>
 * @does
 * <UL>
 * <LI> provides methods to retrieve Configuration parameters that are
 * specific to the VegaEnsemblSeqload
 * </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class VegaEnsemblSeqloadCfg extends Configurator {

    /**
    * Constructs a VegaEnsemblSeqload Configuration object
    * @assumes Nothing
    * @effects Nothing
    * @throws ConfigException if a configuration manager cannot be 
    * obtained
    */

    public VegaEnsemblSeqloadCfg() throws ConfigException {
    }


    /**
     * Gets the full path the the assocload input file
     * @return the full path the the assocload input file
     * @throws ConfigException if "INFILE_ASSOCLOAD" 
     * not found in configuration file
     */
    public String getAssocLoadFileName() throws ConfigException {
        return getConfigString("INFILE_ASSOCLOAD");
    }

    /**
     * Gets the sequence logical db name
     * @return the sequence logical db name
     * @throws ConfigException if "SEQ_LOGICALDB"
     * not found in configuration file
     */
    public String getSeqLogicalDBName() throws ConfigException {
        return getConfigString("SEQ_LOGICALDB");
    }
    /**
     * Gets the gene model logical db name
     * @return the gene model logical db name
     * @throws ConfigException if "GM_LOGICALDB"
     * not found in configuration file
     */
    public String getGeneModelLogicalDBName() throws ConfigException {
        return getConfigString("GM_LOGICALDB");
    }

}
