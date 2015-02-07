#!/usr/bin/python
# coding=utf-8
import urllib2
import xml.etree.ElementTree as ET
from subprocess import call, check_output
import sys

WIKI="http://www.wollmux.net/wiki"
ALL_PAGES_URL=WIKI + "/Spezial:Alle_Seiten"
DEST="doc/wiki"

# onl start this if current index is empty
index = check_output(['git', 'diff', '--cached', '--name-only'])
if index != "":
	print "You have uncommitted changes in your index."
	print "please commit them or reset your index first!"
	sys.exit(1)

# reading article overview
response = urllib2.urlopen(ALL_PAGES_URL)
xml = ET.fromstring( response.read() )
anchors = xml.findall("body/" 
	+ "div[@id='content']/"
	+ "div[@id='bodyContent']/"
	+ "div[@id='mw-content-text']/"
	+ "table[@class='mw-allpages-table-chunk']/"
	+ "tr/td/a"
)
articles = []
for link in anchors:
	article = urllib2.unquote( link.get('href') )
	article = article[6:] # strip away "/wiki/"
	articles.append(article)

# fetch articles
for i in articles:
	print "fetching " + i
	response = urllib2.urlopen(WIKI + '/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=' + i)
	xml = ET.fromstring( response.read() )
	mwText = xml.find('query/pages/page/revisions/rev').text
	fo = open(DEST + "/" + i + ".mediawiki", "wb")
	fo.write( mwText.encode('utf8') )
	fo.close()

# add wiki-changes to the index and commit them
call(['git', 'add', '--all', DEST])
call(['git', 'commit', '-m', 'update from wollmux-wiki'])
