#!/bin/bash
##############################################################
# this script uses pandoc to convert the .mediawiki pages
# to html. This could be used to create a local preview
# of the mediawiki-pages without the need of a full mediawiki
# on the local machine. NOTE: this is only a rough preview
# since pandoc might in details behave different than the
# original mediawiki engine.
##############################################################
preview="/tmp/wollmux_wiki_preview"

mkdir "$preview" 2>/dev/null

cd doc/wiki
for i in *.mediawiki
do
  echo converting $i to html...

  # Some preprocessing before pandoc is called:
  # In case of table start elements "{|", we have to remove trailing
  # arguments like "{| border=1" - they are currently not
  # recognized by pandoc.
  cat "$i" | \
    sed "s/{|.*/{|/" | \
    pandoc --toc -s -f mediawiki -t html -o "$preview/${i/.mediawiki/}"
done

echo "finished. Now please open \"file://$preview\" in the browser"
