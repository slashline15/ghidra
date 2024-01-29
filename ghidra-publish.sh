#!/usr/bin/env bash
set -e #stop on error
set -o pipefail

# this script downloads a ghidra release from ghidra-sre and publishes it to
# sonatype, so that we can promote it to maven central:
# https://repo1.maven.org/maven2/io/joern/ghidra/
# see also https://github.com/NationalSecurityAgency/ghidra/issues/799

VERSION=11.0_PUBLIC_20231222
VERSION_SHORTER=11.0
VERSION_SHORT=${VERSION_SHORTER}_PUBLIC

SONATYPE_URL=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/
# the server id from your local ~/.m2/settings.xml
REPO_ID=sonatype-nexus-staging-joern

DISTRO_URL=https://github.com/NationalSecurityAgency/ghidra/releases/download/Ghidra_${VERSION_SHORTER}_build/ghidra_${VERSION}.zip
echo "download and unzip ghidra distribution from $DISTRO_URL"
wget $DISTRO_URL
unzip ghidra_$VERSION.zip
rm ghidra_$VERSION.zip
cd ghidra_${VERSION_SHORT}
support/buildGhidraJar

# add classes from ByteViewer.jar - those are looked up at runtime via reflection
# context: lookup happens transitively by loading classes from _Root/Ghidra/EXTENSION_POINT_CLASSES
unzip Ghidra/Features/ByteViewer/lib/ByteViewer.jar -d byteviewer
cd byteviewer
zip -r ../ghidra.jar *
cd ..

# install into local maven repo, mostly to generate a pom
mvn install:install-file -DgroupId=io.joern -DartifactId=ghidra -Dpackaging=jar -Dversion=$VERSION -Dfile=ghidra.jar -DgeneratePom=true
cp ~/.m2/repository/io/joern/ghidra/$VERSION/ghidra-$VERSION.pom pom.xml

# add pom-extra to pom.xml, to make sonatype happy
head -n -1 pom.xml > pom.tmp
cat pom.tmp ../pom-extra > pom.xml
rm pom.tmp

# create empty jar for "sources" - just to make sonatype happy
zip empty.jar LICENSE

# sign and upload artifacts to sonatype staging
mvn gpg:sign-and-deploy-file -Durl=$SONATYPE_URL -DrepositoryId=$REPO_ID -DpomFile=pom.xml -Dclassifier=sources -Dfile=empty.jar
mvn gpg:sign-and-deploy-file -Durl=$SONATYPE_URL -DrepositoryId=$REPO_ID -DpomFile=pom.xml -Dclassifier=javadoc -Dfile=docs/GhidraAPI_javadoc.zip
mvn gpg:sign-and-deploy-file -Durl=$SONATYPE_URL -DrepositoryId=$REPO_ID -DpomFile=pom.xml -Dfile=ghidra.jar

echo "artifacts are now published to sonatype staging. next step: log into https://s01.oss.sonatype.org -> staging repositories -> select the right one -> close -> release"
echo "once it's synchronised to maven central, then, update the ghidra version in `joern/joern-cli/frontends/ghidra2cpg/build.sbt`"
echo "don't forget to commit and push the local changes in this repo to https://github.com/joernio/ghidra"
