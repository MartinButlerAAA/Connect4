del *.bak
del *.class
Javac *.java
"C:\Program Files\Java\jdk-23\bin\jar.exe" cfm Connect4.jar mf.txt *.class
java -jar Connect4.jar
pause