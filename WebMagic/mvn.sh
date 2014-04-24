#!/bin/sh
echo "Based on Maven Build or Compiler"
currentPath=${PWD}
echo "execute [ $0 $1 $2 ] on $currentPath"
case "$1" in 
	eclipse|-eclipse)
	mvn clean:clean
	mvn eclipse:clean
	mvn eclipse:eclipse -DdownloadSources=true #-DdownloadJavadocs=true
	;;
	clean|-clean)
	mvn clean:clean
	;;
	install|-install)
	mvn clean install -Pprd -Dmaven.test.skip=true
	;;
	package|-package)
	mvn clean package -Pprd -Dmaven.test.skip=true
	;;
	source|-source)
	mvn dependency:sources
	;;
esac

echo "All Done."
