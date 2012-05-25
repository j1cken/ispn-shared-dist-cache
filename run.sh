#!/bin/bash
export JBOSS_HOME=`dirname $0`/node1/jboss-as-7.1.1.Final
rm -rf node1/jboss-as-7.1.1.Final/standalone/{data,tmp,log}
rm -rf node2/jboss-as-7.1.1.Final/standalone/{data,tmp,log}
mvn clean test -PJBOSS_AS_MANAGED_7.X

echo
echo "Key distribution:"
grep Owner node?/jboss-as-7.1.1.Final/standalone/log/server.log
exit 0

