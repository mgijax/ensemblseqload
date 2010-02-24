#!/usr/local/bin/python

##########################################################################
#
# Purpose:
#       From sequence input file create sequence association input file
#
Usage="createSeqAssocInputFile.py"
# Env Vars:
#	 1. INFILE_SEQASSOCLOAD
#	 2. SEQ_POSITION
#	 3. QUALIFIER
#
# Inputs:
# 1. FASTA sequence file. We parse the description line
#  VEGA transcript
#  >OTTMUST00000086906 cdna:all chromosome:VEGA37:11:3028975:3030149:1 Gene:OTTMUSG00000000759
#
#  VEGA protein
#  >OTTMUSP00000000697 pep:all chromosome:VEGA37:11:3031884:3093466:-1 Gene:OTTMUSG00000005013 Transcript:OTTMUST00000001465
#
#  Ensembl transcript
#  >ENSMUST00000082392 cdna:known chromosome:NCBIM37:MT:2751:3707:1 gene:ENSMUSG000064341 
#
#  Ensembl protein
#  >ENSMUSP00000080991 pep:known chromosome:NCBIM37:MT:2751:3707:1 gene:ENSMUSG00000064341 transcript:ENSMUST00000082392 
#
# 2. Configuration 
#
# Outputs:
#	 1. tab delimited file in seqseqassocload format:
#           1. seqId1
#           2. qualifier
#           3. seqId2
#	 2. log file
# 
# Exit Codes:
#
#      0:  Successful completion
#      1:  An exception occurred
#
#  Assumes:  Nothing
#
#  Notes:  None
#
###########################################################################

import sys
import os
import mgi_utils
import loadlib
import string
import db

#
# CONSTANTS
#
TAB= '\t'
CRT = '\n'
GT = '>'
SPACE = ' '
COLON = ':'

#
# GLOBALS
#

# file descriptors
inFile = ''
outFile = ''

seq1Position = 0
seq2Position = ''
qualifier = ''

def init():
    global inFile, outFile, seq2Position, qualifier

    print '%s' % mgi_utils.date()
    print 'Initializing'

    inFile = sys.stdin
    if len(sys.argv) != 1:
            print Usage
            sys.exit(1)
    
    outFilePath = os.environ['INFILE_SEQASSOCLOAD']
    try:
        outFile = open(outFilePath, 'w')
    except:
        'Could not open file for writing %s\n' % outFilePath
        sys.exit(1)
    seq2Position = int(os.environ['SEQ_POSITION'])
    qualifier = os.environ['QUALIFIER']

def run():
    print 'Creating association file'
    for line in inFile.readlines():
	if not line[0:1] == GT:
	    continue
        else:
	    tokenList = string.split(line, SPACE)
	    seq1 = tokenList[seq1Position]
	    # remove leading GT
	    seq1 = string.strip(seq1[1:])
	    seq2 = tokenList[seq2Position]
	    seq2 = string.strip((string.split(seq2, COLON))[1])
            #print '%s %s %s %s' % (seq1, qualifier, seq2, CRT)
	    outFile.write('%s%s%s%s%s%s' %  
		(seq1, TAB, qualifier, TAB, seq2, CRT) )
    inFile.close()
    outFile.close()
#
# Main
#
init()
run()
sys.exit(0)
