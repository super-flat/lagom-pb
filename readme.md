# lagompb = lagom + protocol buffer

Scala shared code for [lagom](https://www.lagomframework.com/documentation/1.6.x/scala/Home.html) development using protocol buffer. 

This library helps write lagom microservices easily by making use of protocol buffer messages to define the es/cqrs core
components like _**api requests/responses**_, _**grpc services**_, _**events**_, _**commands**_ and _**state**_. 

### Features

- Implementation of an HTTP/Json based microservice using REST interfaces by defining _**api requests and responses as protobuf messages**_. 
- Implementation of a gRPC based microservice using protocol buffer messages. More info: [Grpc](https://grpc.io/).
- Kafka producer in-built battery([message broker api](https://www.lagomframework.com/documentation/1.6.x/scala/MessageBrokerApi.html)) to publish domain events to kafka as serialized protocol buffer messages.
- Pure testable functions for events and commands handlers.
- Easy definition of aggregate root and command serializer.
- Easy definition of api service descriptors either with message broker api or without.
- Easy implementation of api service either with message broker api or without.
- State meta adds some `revision number` that can help easily implement optimistic lock.  
- At every event handled a snapshot of the aggregate state with the state meta are made available for the readSide.
- All events and snapshots are by default persisted to Postgres SQL.

### Usage Overview

This library encapsulates all the necessary requirements to implement a micro-service
using the [lagom framework](https://www.lagomframework.com/documentation/1.6.x/scala/Home.html).

The following steps will let you have in short time a scaffolded lagom-based service.

- Add the content of the `plugins.sbt` of this repository into your `project/plugins.sbt`

#### API requests and responses definition/GRPC services

- Define your **_api requests/responses_** as protocol buffer messages for HTTP-Json services (a.k.a REST).
- Define your service definitions in protocol buffers message for Grpc.

#### Domain model Implemention or ES/CQRS implementation

- Define your _**commands**_ as protocol buffer messages.
- Define your _**events**_ as protocol buffer messages.

Example:
```protobuf
syntax = "proto3";

package account;

message AccountOpened {
    string company_uuid = 1;
    string account_id = 2;
    double balance = 3;
    string account_owner = 4;
}
```
- Define the _**aggregate state**_ as protocol buffer message.
Example:
```protobuf
syntax = "proto3";

package account;

message BankAccount {
    string account_id = 1;
    double account_balance = 2;
    string account_owner = 3;
    bool is_closed = 4;
}
```

**Note:** One must configure the protocol buffer package name in the `application.conf` under the
section `lagompb`. Please refer to the `reference.conf`.

This allows lagompb to handle under the hood the serialization of api requests and responses as well as the commands and replies sent respectively to and from the aggregate root. Failure to do so will let your service not to run smoothly.

- Implement the commands handler by extending the `lagompb.LagompbCommandHandler[TState]` where `TState`
represents the aggregate state type.
- Implement the events handler by extending the `lagompb.LagompbEventHandler[TState]` where `TState`
represents the aggregate state type.
- Implement the aggregate or persistence entity by extending the `lagompb.LagompbAggregate[TState]` where
`TState` represents the aggregate state type.
- Define the rest api service definition by extending the `lagompb.LagompbServiceWithKafka` or `lagompb.LagompbService` in a separate sbt submodule project. This only applies for  HTTP-Json services (a.k.a REST). See [Grpc](#grpc) service guidelines.
- Implement the rest api by extending the `lagompb.LagompbServiceImplWithKafka` or `lagompb.LagompbServiceImpl`. 
This only applies for  HTTP-Json services (a.k.a REST) See [Grpc](#grpc) service guidelines.
- Implement the lagom application by extending the `lagompb.LagompbApplication`.
- Implement the lagom application loader by extending the `LagomApplicationLoader`.

**Note:** It is important to set the `number of shards` and `snapshot retention criteria` in the configuration file. 
The default value for `number of shards` is `9` due some a bug in the [akka persistence driver](https://github.com/dnvriend/akka-persistence-jdbc/issues/168) by adding to `application.conf`

```hocon
akka {
  cluster {
    sharding {
      # Number of shards to be used.
      number-of-shards = 9
    }
  }
}

# custom lagompb settings
lagompb {
  snaphsot-criteria {
    # number of events to batch persist
    frequency = 100
    # number of snapshots to retain
    retention = 2
  }
}
```

#### gRPC
To implement a grpc service you must:
- Add the following settings to your `build.sbt` of your sbt module settings

```scala
  settings(
    inConfig(Compile)(
      Seq(
        PB.protoSources := Seq(file(<path-to-your-proto-definitions>)),
        PB.includePaths ++= Seq(file(<path-to-your-include>)),
      )
    ),
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala),
    akkaGrpcExtraGenerators in Compile += PlayScalaServerCodeGenerator,
    akkaGrpcExtraGenerators in Compile += PlayScalaClientCodeGenerator,
    akkaGrpcCodeGeneratorSettings += "server_power_apis",
    akkaGrpcCodeGeneratorSettings := akkaGrpcCodeGeneratorSettings.value.filterNot(
      _ == "flat_package"
    )
  )
```
- From the root folder of your project run `sbt clean compile` to generate the grpc scala traits.
- Implement the generated traits by injecting the following `akka.cluster.sharding.typed.scaladsl.ClusterSharding` and `com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry` and mixed in with `io.superflat.lagompb.LagompbGrpcServiceImpl` to be able to interact with the domain model implementation.
- Hook the implementation to your lagom application as documented [Lagom doc](https://www.lagomframework.com/documentation/1.6.x/scala/AdditionalRouters.html#Additional-Routers).
