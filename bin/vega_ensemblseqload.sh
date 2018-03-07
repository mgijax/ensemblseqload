#!/bin/sh
#
#  vega_ensemblseqload.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the vega and ensembl 
#            transcript and protein sequence load. For a given provider, 
#	     determined by the config file parameter1:
#	     1) load sequences if parameter2 = true
#	     2) load associations btwn transcripts and genomic sequences 
#	        and associations proteins and transcripts if parameter2 = true
#	     3) load marker associations regardless of the value of 
#	         parameter2
#	     Note: during development you can avoid loading marker associations
#	         by setting *assocload.config ASSOC_JNUMBER=0
#
  Usage="Usage: vega_ensemblseqload.sh config_file load_seqs? [true|false]"
#
#     e.g. "vega_ensemblseqload.sh vega_proteinseqload.config true
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
#      - Specific sequence load configuration file - 
#	   e.g. vega_proteinseqload.config
#      - Specific marker assocload configuration file - 
#          e.g. vega_proteinassocload.config
#      - Sequence input file 
#      - Sequence/marker association file (created from sequence input file 
#	   and the database)
#
#  Outputs:
#
#      - An archive file
#      - Log files defined by the environment variables ${LOG_PROC},
#        ${LOG_DIAG}, ${LOG_CUR} and ${LOG_VAL}
#      - assocload input file for marker associations
#      - BCP files for inserts to each database table to be loaded
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
if [ $# -ne 2 ]
then
    echo ${Usage} | tee -a ${LOG}
    exit 1
fi

#
#  Establish the configuration file names.
#
CONFIG_LOAD=`pwd`/$1
LOAD_SEQS=$2
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

for file in ${APP_INFILES} 
do
    if [ ! -r ${file} ]
    then
	# set STAT for endJobStream.py called from postload in shutDown
	STAT=1
	checkStatus ${STAT} "Input file: ${file} does not exist"
    fi
done

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

echo "Running vega_ensemblseqload ${CONFIG_LOAD}. Loading sequences = ${LOAD_SEQS}" | tee -a ${LOG_DIAG} ${LOG_PROC}


# log time and input files to process
echo "" >> ${LOG_DIAG} ${LOG_PROC}
echo "`date`" >> ${LOG_DIAG} ${LOG_PROC}

echo "Processing input file ${APP_INFILES}" >> ${LOG_DIAG} ${LOG_PROC}

# run the load
${APP_CAT_METHOD} ${APP_INFILES} | \
${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
-DCONFIG=${CONFIG_MASTER},${CONFIG_LOAD_COMMON},${CONFIG_LOAD} \
-DLOAD_SEQS=${LOAD_SEQS} \
-DJOBKEY=${JOBKEY} ${DLA_START}

STAT=$?
checkStatus ${STAT} "${VEGA_ENSEMBLSEQLOAD}"

# update serialization on mgi_reference_assoc, seq_source_assoc
cat - <<EOSQL | ${PG_DBUTILS}/bin/doisql.csh $0 | tee -a ${LOG_DIAG}

select setval('mgi_reference_assoc_seq', (select max(_Assoc_key) from MGI_Reference_Assoc));
select setval('seq_source_assoc_seq', (select max(_Assoc_key) from SEQ_Source_Assoc));

EOSQL

if [ ${LOAD_SEQS} = "true" ]
then
    echo "Running the Sequence-Sequence Association Load" | 
	tee -a ${LOG_DIAG} ${LOG_PROC}
    echo "" >> ${LOG_PROC}
    echo "`date`" >> ${LOG_PROC}

    # create input file for then run seqseqassocload
    ${APP_CAT_METHOD} ${APP_INFILES} |
	${VEGA_ENSEMBLSEQLOAD}/bin/createSeqAssocInputFile.py
    STAT=$?
    checkStatus ${STAT} "createSeqAssocInputFile"

    ${SEQSEQASSOCLOAD}/bin/seqseqassocload.sh ${CONFIG_SEQASSOC}
    STAT=$?
    checkStatus ${STAT} "seqseqassocload"
fi
# run sequence/marker assocload 
# use ASSOC_JNUMBER=0 to not run the assocload during development
if [ ${ASSOC_JNUMBER} != "0" ]
then
    echo "Running the Marker assocload" | tee -a ${LOG_DIAG} ${LOG_PROC}
    echo "" >> ${LOG_PROC}
    echo "`date`" >> ${LOG_PROC}

    ${ASSOCLOADER_SH} ${CONFIG_LOAD} ${CONFIG_ASSOCLOAD}
    STAT=$?
    checkStatus ${STAT} "${ASSOCLOADER_SH}"

fi

#
# run postload cleanup and email logs
#
shutDown

exit 0

