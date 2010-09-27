@echo off
echo ----------------------
echo JAVA_HOME=%JAVA_HOME%
echo ----------------------

%JAVA_HOME%\bin\java -server -cp lib\*;.;src;bin cn.org.rapid_framework.tools.google_wiki_to_html.GoogleWikiDownloaderMain %1 %2 %3 %4

pause