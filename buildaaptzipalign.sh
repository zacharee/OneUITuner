#!/bin/sh
mkdir aaptstuff
cd aaptstuff

if  [[ ! $(which svn) ]]; then
	sudo apt install -y subversion
fi

svn checkout https://github.com/aosp-mirror/platform_frameworks_base/trunk/tools/