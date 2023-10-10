#!/bin/bash                                                                                                                                                                                                                     

DIR=/root/.m2/repository/org/jacoco/jacoco-maven-plugin/0.8.7
(
    cd /tmp
    jar xvf $DIR/jacoco-maven-plugin-0.8.7.jar META-INF/maven/plugin.xml
    sed -i 's/HTML,XML,CSV/CSV/g' META-INF/maven/plugin.xml
    jar uf $DIR/jacoco-maven-plugin-0.8.7.jar META-INF/maven/plugin.xml
)
