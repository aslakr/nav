"""
$Id: setup.py,v 1.2 2003/06/19 12:50:34 magnun Exp $
"""
import os
os.sys.path.append(os.path.split(os.path.realpath(os.sys.argv[0]))[0]+"/lib")
os.sys.path.append(os.path.split(os.path.realpath(os.sys.argv[0]))[0]+"/lib/handler")
import abstractChecker
import re,getopt,sys,config,psycopg,checkermap,string,db
from service import Service

HEADER = '#sysname              handler    args'


def parseLine(line):
	line = line.strip()
	w = line.split()
	sysname = w[0]
	handler = w[1]
	args = w[2:]
	try:
		args=dict(map(lambda x: tuple(x.split('=')), args))
	except ValueError:
		print "Argumentet har ikke riktig syntax: %s" % args
		args=""

	if not checkermap.get(handler):
		msg = "no such handler/type: (%s)" % handler
		raise TypeError(msg)

	#Handle boxless services
	if sysname == 'none':
		sysname = ""

	return Service(sysname, handler, args)

def fromFile(file):
	new = []
	for i in open(file).read().split('\n'):
		i = i.split('#')[0]
		if i:
			service = parseLine(i)
			new += [service]
	return new


def newFile(file,conf):
	conf = config.dbconf(configfile=conf)
	database=db.db(conf)

	print 'fetching services from db'
	services = database.getServices()

	print 'creating ' + file
	file = open(file,'w')
	file.write(HEADER + '\n')
	for i in services:
		file.write('%s\n' % i)
	
		
def main(file,conf):
	conf = config.dbconf(configfile=conf)
	database = db.db(conf)

	print 'parsing file'
	fileEntries = fromFile(file)
	print "Entries in file: %i" % len(fileEntries)
	
	dbEntries = database.getServices()
	print "Entries in db: %i" % len(dbEntries)

	delete = filter(lambda x: x not in fileEntries, dbEntries)
	new = filter(lambda x: x not in dbEntries, fileEntries)

	if delete:
		print "Elements to be deleted: %i" % len(delete)
		for i in delete:
			print i
		s = 0
		while s not in ('yes','no'):
			print '\nare you sure you want to delete? (yes/no)'
			s = raw_input()
		if s == 'yes':
			for i in delete:
				database.deleteService(i)
		else:
			print "Continueing..."
	print 'updating db'

	print "Elements to add: %i" % len(new)
	for each in new:
		print "Adding service: %s" % each
		database.insertService(each)

def help():
	print """Setup program for NAV service monitor.

	valid options:
		-h 	- shows help
		-f file	- default services.conf
		-c file	- default db.conf
		-n	- generates a new service.conf from db
		-u	- updates the db from services.conf

Written by Erik Gorset and Magnus Nordseth, 2002
"""
if __name__=='__main__':
	try:
		opts, args = getopt.getopt(sys.argv[1:], 'hnuf:c:')
		opts = dict(opts)
		if '-h' in opts:
			help()
			sys.exit()
		file = opts.get('-f','services.conf')
		conf = opts.get('-c','db.conf')
		if not opts:
			help()
		elif not os.path.exists(conf):
			msg = 'cant find file: ' + conf 
			raise IOError(msg)
		if '-n' in opts:
			if os.path.exists(file):
				print 'creating backup: services.backup'
				open('services.backup','w').write(open(file).read())
			newFile(file,conf)
		elif '-u' in opts:
			if not os.path.exists(file):
					msg = 'cant find file: ' + file
					raise IOError(msg)
			else:
				print 'creating backup: services.backup'
				open('services.backup','w').write(open(file).read())
			main(file,conf)
		sys.exit(0)
	except (getopt.error):
		pass
	sys.exit(2)
