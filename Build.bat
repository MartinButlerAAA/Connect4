del *.bak
del *.class
"C:\Program Files\Java\jdk1.8.0_202\bin\javac" *.java
"C:\Program Files\Java\jdk1.8.0_202\bin\jar" cfm Connect4.jar mf.txt *.class
java -jar Connect4.jar
pause