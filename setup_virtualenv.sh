#!/bin/bash -e

BASEDIR=`dirname $0`

if [ ! -d "$BASEDIR/ve" ]; then
    virtualenv -q $BASEDIR/ve --no-site-packages
    echo "Virtualenv created."
fi

if [ ! -f "$BASEDIR/ve/updated" -o $BASEDIR/requirements.pip -nt $BASEDIR/ve/updated ]; then
    $BASEDIR/ve/bin/pip -v install -r $BASEDIR/requirements.pip
    touch $BASEDIR/ve/updated
    echo "Requirements installed."
fi
