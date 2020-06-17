# Agent Server Parameter Plugin

This plug-in is used to select the build server before building. If you have multiple build servers, you can use this plug-in to make a convenient selection before building. The plug-in will remember that the server you choose each time is convenient for the next build.

The advantage of using this plugin is that you may have multiple servers for building:
![project doc image](docs/images/image-01.png)

[中文说明](README_ZH.md)

## basic configuration

In the project configuration page, select the "This project is parameterized" check box, add "Agent Server Parameter" parameter, create a parameter name, you can also click "Advanced" to set the default build server name, the default value is not a required option, if it is The default is the master server.
![project doc image](docs/images/image-02.png)

The method of reading parameter values at build time:

```groovy
node{
    print params['agent-name']
}
```

## Select the server before building

![project doc image](docs/images/image-03.png)

The default value is updated after each server selection, which is convenient for the next build.

![project doc image](docs/images/image-04.png)

## Declarative Pipeline

```groovy
pipeline {
   agent { label params['agent-name'] } 

   parameters{
      agentParameter name:'agent-name'
   }
   stages {
      stage('Hello') {
         steps {
            print params['agent-name'] 
         }
      }
   }
}
```

It is possible to create parameters in the build script, but because each execution of the build script creates a new "Agent Server Parameter" build parameter, the last selected value cannot be retained.

