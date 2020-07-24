# gRPC

The gRPC is an interface that helps interact with both the @ref:[Write Side](write-side.md) and the @ref:[Read Side](read-side.md)
using gRPC service calls.

To implement a grpc service you must:

- Add the following settings to your `build.sbt` of your sbt module

```scala
  ......
  .settings(
    inConfig(Compile)(
      Seq(
        PB.protoSources := Seq(file(<path-to-your-proto-definitions>)),
        PB.includePaths ++= Seq(file(<path-to-your-include>)),
      )
    ),
    // Using Scala
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
- Implement the generated traits by injecting the following `akka.cluster.sharding.typed.scaladsl.ClusterSharding` and `com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry` 
and mixed in with `io.superflat.lagompb.BaseGrpcServiceImpl` to be able to interact with the domain model implementation. 
