# runrightfast-vertx-demo

## The demo features and key points

### 2015-08-01
1. co.runrightfast.vertx.demo.VertxApp
    - main class
    - shows how easy it is to start a RunRightFast Vertx app
    - the code to launch the app is centralized in co.runrightfast.vertx.core.application.RunRightFastVertxApplicationLauncher
    - provide a CLI - current options supported are:

      -c,--config    Show the application configuration as JSON
      -h,--help      Print usage
      -v,--version   Show the application version

    - registers an application MBean under the co.runrightfast JMX domain
      - used to view the deployed application version
      - exposes an operation to shutdown the app
2. Framework provides support to make it easy to develop, configure, and deploy verticles     
3. Vertx is embedded
    - see co.runrightfast.vertx.core.VertxService and its implementation
3. Application configuration is managed via TypeSafe config
4. Dagger 2 is used for DI
    - component interfaces package: co.runrightfast.vertx.core.components
      - the RunRightFastVertxApplication component interface is the main application component
5. Hazelcast is used for Vertx clustering
6. DropWizard is used for metrics and healthchecks
   - metrics are enabled by default and exposed via JMX under domain co.runrightfast.vertx.metrics
7. JDK built in logging is used

### 2015-08-02
1. First class citizen support added for HealthChecks
   - HealthChecks can be discovered and run via JMX
   - Each verticle has its owne HealthCheck registry
   - Each verticle is responsible for registering its own health checks
     - this is supported by the RunRightFastVerticle base class
     - sub-classes must implement: abstract Set<RunRightFastHealthCheck> getHealthChecks() 

### 2015-08-04
1. Integrated Docker plugin   
    - appDocker Gradle task
     - generates a Dockerfile - located within build/docker dir
     - creates a Docker image named : runrightfast/runrightfast-vertx-demo
     - requires Docker to be installed locally
    - to create a container and be able to run remote debug (port 4000) and connect to it via JMX (port 7410), run the following docker command
           
                docker create --name=runrightfast-vertx-demo-1 -p 7410:7410 -p 4000:4000 -p 5701:5701 -p 9123:9123 <image_id_5701_9123>

    - to run another instance of the container on the same machine
        
                docker create --name=runrightfast-vertx-demo-2 -p 7411:7410 -p 4001:4000 -p 5702:5702 -p 9124:9124 <image_id_5702_9124>
        

### 2015-08-08
1. Added new DemoMXBean
    - used to get deployed verticles, using a ProtobufMessageProducer
    - metrics are available via JMX unser JMX domain DemoMXBean.metrics

### 2015-08-22
1. Enabled Vertx clustering
    - this was enabled via the application.conf - runrightfast.vertx.VertxOptions.clusterManager
    - steps to run multiple docker instances that are clustered together
        1. application.conf changes - follow the instructions in the **CHANGE ME** comments
        2. build the docker image using gradle : ../gradlew build appDocker
    - How to run multiple docker instances on the same machine
        1. application.conf - set port to 5701
        2. build the docker image using gradle
        3. `docker create --name=runrightfast-vertx-demo-1 -p 7410:7410 -p 4000:4000 -p 5701:5701 p 9123:9123 <image_id_5701_9123>`
        4. application.conf - set port to 5702
        5. build the docker image using gradle
        6. `docker create --name=runrightfast-vertx-demo-2 -p 7411:7410 -p 4001:4000 -p 9124:9124 <image_id_5702_9124>`
