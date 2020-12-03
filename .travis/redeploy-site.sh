#!/usr/bin/env bash

echo "START building and moving scaladocs..."
mvn scala:doc -f lib/pom.xml
if [ -d "docs/scaladocs" ]; then rm -fr docs/scaladocs; fi
mv lib/target/site/scaladocs docs/
echo "DONE building and moving scaladocs"

echo "START deploying docs to GitHub pages..."
mkdocs gh-deploy --force
echo "DONE deploying docs to GitHub pages."
