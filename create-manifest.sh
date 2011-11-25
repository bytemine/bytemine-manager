#!/bin/sh

# Copyright (c) 2009 bytemine
# Author: Bernd Ahlers <ahlers@bytemine.net>
# Author: Felix Kronlage <kronlage@bytemine.net>
# http://www.bytemine.net/

VERSION=$1
JAR=$2
SIZE=0
SHA1=`which sha1sum`

usage() {
	echo "Wrong number of arguements. abort."
	echo "Usage: $(basename $0) 1.3.5 bytemine-manager-1.3.5.jar"
	exit 1
}

if [ -z "$VERSION" -o -z "$JAR" ]; then
	usage
fi

SIZE=`ls -al $JAR | awk -F " " '{print $5}'`
CHECKSUM=$($SHA1 < $JAR)
TIMESTAMP=$(date +%s)

echo
echo "----------------------------------------------------------------"
echo "Put this into the repo.yml file on applianceupdate.bytemine.net:"

cat <<__EOF > /tmp/repo-$VERSION.yml

- version: "${VERSION%-*}"
  filename: $JAR
  size: $(($SIZE / 1024))
  checksum: $CHECKSUM
  timestamp: $TIMESTAMP

__EOF

cat /tmp/repo-$VERSION.yml

