#!/bin/sh
#
#  vega_ensemblseqload.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the vega and ensembl 
#            transcript and protein sequence loads and optionally 
#	     the association loads
#
  Usage="vega_ensemblseqload.sh config_file"
#
#     e.g. "vega_ensemblseqload.sh vega_proteinseqload.config
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Common configuration file -
#		/usr/local/mgi/live/mgiconfig/master.config.sh
#      - Common load configuration file - common.config
#      - Specific load configuration file - e.g. vega_proteinseqload.config
#      - sequence input file 
#      - sequence/marker association file (optional)
#
#  Outputs:
#
#      - An archive file
#      - Log files defined by the environment variables ${LOG_PROC},
#        ${LOG_DIAG}, ${LOG_CUR} and ${LOG_VAL}
#      - BCP files for for inserts to each database table to be loaded
#      - Records written to the database tables
#      - Exceptions written to standard error
#      - Configuration and initialization errors are written to a log file
#        for the shell script
#
#  Exit Codes:
#
#      0:  Successful completion
#      1:  Fatal error occurred
#      2:  Non-fatal error occurred
#
#  Assumes:  Nothing
#
#  Implementation:  
#
#  Notes:  None
#
###########################################################################

#
#  Set up a log file for the shell script in case there is an error
#  during configuration and initialization.
#

cd `dirname $0`/..
LOG=`pwd`/vega_ensemblseqload.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -ne 1 ]
then
    echo ${Usage} | tee -a ${LOG}
    exit 1
fi

#
#  Establish the configuration file names.
#
CONFIG_LOAD=`pwd`/$1
CONFIG_LOAD_COMMON=`pwd`/common.config

#
#  Make sure the configuration files are readable
#

if [ ! -r ${CONFIG_LOAD} ]
then
    echo "Cannot read configuration file: ${CONFIG_LOAD}"
    exit 1
fi

if [ ! -r ${CONFIG_LOAD_COMMON} ]
then
    echo "Cannot read configuration file: ${CONFIG_LOAD_COMMON}"
    exit 1
fi

#
# source config files - order is important
#
. ${CONFIG_LOAD_COMMON}
. ${CONFIG_LOAD}

#
#  Make sure the master configuration file is readable
#

if [ ! -r ${CONFIG_MASTER} ]
then
    echo "Cannot read configuration file: ${CONFIG_MASTER}"
    exit 1
fi

# reality check for important configuration vars
echo "javaruntime:${JAVARUNTIMEOPTS}"
echo "classpath:${CLASSPATH}"
echo "dbserver:${MGD_DBSERVER}"
echo "database:${MGD_DBNAME}"
echo "APP_INFILES: ${APP_INFILES}"
#
#  Source the DLA library functions.
#
if [ "${DLAJOBSTREAMFUNC}" != "" ]
then
    if [ -r ${DLAJOBSTREAMFUNC} ]
    then
        . ${DLAJOBSTREAMFUNC}
    else
        echo "Cannot source DLA functions script: ${DLAJOBSTREAMFUNC}"
        exit 1
    fi
else
    echo "Environment variable DLAJOBSTREAMFUNC has not been defined."
    exit 1
fi

#
# check that APP_INFILES has been set and exists
#
if [ "${APP_INFILES}" = "" ]
then
     # set STAT for endJobStream.py called from postload in shutDown
    STAT=1
    checkStatus ${STAT} "APP_INFILES not defined"
fi

if [ ! -r ${APP_INFILES} ]
then
    # set STAT for endJobStream.py called from postload in shutDown
    STAT=1
    checkStatus ${STAT} "Input file: ${APP_INFILES} does not exist"
fi

##################################################################
##################################################################
#
# main
#
##################################################################
##################################################################

#
# createArchive including OUTPUTDIR, startLog, getConfigEnv, get job key
#

preload ${OUTPUTDIR}

#
# rm files and dirs from OUTPUTDIR and RPTDIR
#

cleanDir ${OUTPUTDIR} ${RPTDIR}

#
# Run the vega_ensembl sequence load
#

echo "Running vega_ensemblseqload ${CONFIG_LOAD}" | tee -a ${LOG_DIAG} ${LOG_PROC}


# log time and input files to process
echo "\n`date`" >> ${LOG_DIAG} ${LOG_PROC}

echo "Processing input file ${APP_INFILES}" >> ${LOG_DIAG} ${LOG_PROC}

# run the load
${APP_CAT_METHOD} ${APP_INFILES} | \
${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
-DCONFIG=${CONFIG_MASTER},${CONFIG_LOAD_COMMON},${CONFIG_LOAD} \
-DJOBKEY=${JOBKEY} ${DLA_START}

STAT=$?
checkStatus ${STAT} "${VEGA_ENSEMBLSEQLOAD}"

# run sequence/marker assocload 
if [ ${ASSOC_JNUMBER} != "0" ]
then
    echo "Running the assocload" | tee -a ${LOG_DIAG} ${LOG_PROC}
    echo "\n`date`" >> ${LOG_PROC}

    ${ASSOCLOADER_SH} ${CONFIG_LOAD} ${CONFIG_ASSOCLOAD}
    STAT=$?
    checkStatus ${STAT} "${ASSOCLOADER_SH}"

fi

#
# run postload cleanup and email logs
#
shutDown

exit 0

