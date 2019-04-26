# SpringRemote

A project completed at Chinese Spring Festival.

An open source, tabbed, remote linux SSH connections manager. 

It allows you to save your command, and run it next time by clicking with the mouse.


## Run form the main method.
To let idea resolve dependencies go to module settings -> modules -> dependencies add lib folder.

Open com.haleywang.putty.SpringRemote, and run the main method.


## Run from the sources.
```
cd SpringRemote
mvn clean install
mvn exec:java
```

## Build
                         
mvn clean package

cd target

java -jar spring-remote-1.0.jar