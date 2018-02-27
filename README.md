# FiniteAutoConcat
I wrote a program that generates the NFA that accepts the concatenation of two NFAs/DFAs. The project can be compiled and run as follows:
```
javac -d bin src/*.java
java -cp bin FAConcater [-t <FolderWithInputFiles>] [-r <OutputFolderForNFAFiles>] [-rt < OutputFolderForJFFFiles> ] [-d true for debug logging]
```
The program will save the output to the results folder and resultsJFLAP folder if no path is specified in the command line arguments.
