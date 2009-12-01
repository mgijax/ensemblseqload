#!/bin/sh -x
#
#  run_assocload.sh
###########################################################################
#
#  Purpose: Run just the associations
#
  Usage="run_assocload.sh config_file"
#
#     e.g. "run_assocload.sh vega_proteinassocload.config
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
#      - Specific load configuration file - e.g. vega_proteinassocload.config
#      - sequence/marker association file 
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
LOG=`pwd`/run_assocload.log
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

# filename of the assocload config file
CONFIG_NAME=$1

# full path to the assocload config file
CONFIG_ASSOCLOAD=`pwd`/$1

echo "config name: ${CONFIG_NAME}"

# determine the load config file; need to source for additional info
if [ "${CONFIG_NAME}" = "ensembl_proteinassocload.config" ]
then
    echo "in if"
    CONFIG_LOAD=`pwd`/ensembl_proteinseqload.config
elif [ "${CONFIG_NAME}" = "ensembl_transcriptassocload.config" ]
then
    CONFIG_LOAD=`pwd`/ensembl_transcriptseqload.config
elif [ "${CONFIG_NAME}" = "vega_proteinassocload.config" ]
then
    CONFIG_LOAD=`pwd`/vega_proteinseqload.config
elif [ "${CONFIG_NAME}" = "vega_transcriptassocload.config" ]
then
    CONFIG_LOAD=`pwd`/vega_transcriptseqload.config
else
    echo "Unrecognized config file: ${CONFIG_NAME}" 
    exit 1
fi

CONFIG_LOAD_COMMON=`pwd`/common.config

#
#  Make sure the configuration files are readable
#

if [ ! -r ${CONFIG_ASSOCLOAD} ]
then
    echo "Cannot read configuration file: ${CONFIG_ASSOCLOAD}"
    exit 1
fi

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
. ${CONFIG_ASSOCLOAD}

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


##################################################################
##################################################################
#
# main
#
##################################################################
##################################################################

#
# rm files and dirs from OUTPUTDIR and RPTDIR
#

cleanDir ${OUTPUTDIR} ${RPTDIR}

# run sequence/marker assocload 
echo "Running the assocload" | tee -a ${LOG_DIAG} ${LOG_PROC}
echo "\n`date`" >> ${LOG_PROC}

#${ASSOCLOADER_SH} ${CONFIG_LOAD} ${CONFIG_ASSOCLOAD}
${ASSOCLOADER_SH} ${CONFIG_ASSOCLOAD}
STAT=$?
checkStatus ${STAT} "${ASSOCLOADER_SH}"

exit 0
