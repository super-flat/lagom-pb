import com.lightbend.lagom.core.LagomVersion

parallelExecution in test := true

lazy val root = project
  .in(file("."))
  .aggregate(`lagompb-core`, `lagompb-readside`, `lagompb-plugin`)
  .enablePlugins(CommonSettings)
  .enablePlugins(NoPublish)
  .settings(name := "lagompb")

lazy val `lagompb-core` = project
  .in(file("core/lagompb-core"))
  .enablePlugins(LagomScala)
  .settings(lagomForkedTestSettings: _*)
  .enablePlugins(BuildSettings)
  .enablePlugins(Publish)
  .settings(
    name := "lagompb-core",
    unmanagedResources / excludeFilter := HiddenFileFilter || "*tests*",
    coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";"),
    PB.protoSources in Compile ++= Seq(file("submodules/protobuf"), file("core/lagompb-core/src/test/protobuf")),
    PB.includePaths in Compile ++= Seq(file("submodules/protobuf")),
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = false, javaConversions = false, grpc = false) -> (sourceManaged in Compile).value
    )
  )

lazy val `lagompb-readside` = project
  .in(file("core/lagompb-readside"))
  .enablePlugins(LagomScala)
  .enablePlugins(BuildSettings)
  .enablePlugins(Publish)
  .settings(name := "lagompb-readside", coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";"))
  .dependsOn(`lagompb-core`)

lazy val `lagompb-plugin` = project
  .in(file("core/lagompb-plugin"))
  .enablePlugins(SbtPlugin)
  .enablePlugins(Publish)
  .settings(
    name := "lagompb-plugin",
    coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";"),
    crossScalaVersions := Dependencies.Versions.CrossScalaForPlugin,
    scalaVersion := Dependencies.Versions.Scala212,
    resolvers += Resolver.bintrayRepo("playframework", "maven"),
    resolvers ++= Seq(Resolver.jcenterRepo, Resolver.sonatypeRepo("public"), Resolver.sonatypeRepo("snapshots")),
    addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % LagomVersion.current),
    addSbtPlugin("com.lightbend.akka.grpc" %% "sbt-akka-grpc" % Dependencies.Versions.AkkaGrpcVersion),
    addSbtPlugin("com.thesamet" % "sbt-protoc" % Dependencies.Versions.SbtProtocVersion),
    addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % Dependencies.Versions.JavaAgentVersion),
    libraryDependencies ++= Dependencies.SbtPlugin
  )

cancelable in Global := true
