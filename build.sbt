import sbt.Keys.libraryDependencies

lazy val root = project
  .in(file("."))
  .aggregate(`lagompb-core`, `lagompb-readside`, `lagompb-plugin`, docs)
  .enablePlugins(CommonSettings)
  .enablePlugins(NoPublish)
  .settings(name := "lagompb")

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ParadoxMaterialThemePlugin)
  .enablePlugins(ParadoxSitePlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(NoPublish)
  .settings(
    // Make sure code generation is run before paradox:
    (Compile / paradox) := (Compile / paradox).dependsOn(Compile / compile).value,
    Compile / paradoxMaterialTheme ~= {
      _.withCopyright("Copyright © SuperFlat.io")
        .withColor("light-blue", "blue")
        .withFavicon("")
    },
    paradoxProperties in Compile ++= Map("snip.github_link" -> "false", "version" -> version.value),
    git.remoteRepo := "git@github.com:super-flat/lagom-pb.git"
  )

lazy val `lagompb-core` = project
  .in(file("core/lagompb-core"))
  .enablePlugins(LagomScala)
  .settings(lagomForkedTestSettings: _*)
  .enablePlugins(LagomSettings)
  .enablePlugins(LagomAkka)
  .enablePlugins(Publish)
  .settings(
    name := "lagompb-core",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "protobuf",
    unmanagedResources / excludeFilter := HiddenFileFilter || "*tests*",
    coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";")
  )
  .settings(
    PB.protoSources in Compile := Seq(file("core/lagompb-core/src/main/protobuf")),
    PB.includePaths in Compile ++= Seq(file("core/lagompb-core/src/main/protobuf")),
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = false, javaConversions = false, grpc = false) -> (sourceManaged in Compile).value
    )
  )

lazy val `lagompb-readside` = project
  .in(file("core/lagompb-readside"))
  .enablePlugins(LagomScala)
  .enablePlugins(LagomSettings)
  .enablePlugins(LagomAkka)
  .enablePlugins(Publish)
  .settings(name := "lagompb-readside", coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";"))
  .dependsOn(`lagompb-core`)

lazy val `lagompb-plugin` = project
  .in(file("core/lagompb-plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "lagompb-plugin",
    crossScalaVersions := Dependencies.Versions.CrossScalaForPlugin,
    scalaVersion := Dependencies.Versions.Scala212,
    addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % Dependencies.Versions.LagomVersion),
    addSbtPlugin("com.lightbend.akka.grpc" %% "sbt-akka-grpc" % Dependencies.Versions.AkkaGrpcVersion),
    addSbtPlugin("com.thesamet" % "sbt-protoc" % Dependencies.Versions.SbtProtocVersion),
    resolvers += Resolver.bintrayRepo("playframework", "maven"),
    libraryDependencies ++= Seq(
      "com.lightbend.play" %% "play-grpc-generators" % Dependencies.Versions.PlayGrpcVersion,
      "com.thesamet.scalapb" %% "compilerplugin" % "0.10.7",
      "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.1.2",
      lagomScaladslApi,
      lagomScaladslServer,
      filters,
      lagomScaladslCluster,
      lagomScaladslPersistenceJdbc,
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      Dependencies.Compile.PlayJsonDerivedCodecs,
      Dependencies.Compile.LagomScaladslAkkaDiscovery,
      Dependencies.Compile.postgresDriver,
      Dependencies.Compile.Macwire,
      Dependencies.Compile.AkkaServiceLocator,
      Dependencies.Compile.akkaManagement,
      Dependencies.Compile.AkkaManagementClusterBootstrap,
      Dependencies.Compile.AkkaManagementClusterHttp,
      Dependencies.Compile.AkkaKubernetesDiscoveryApi,
      Dependencies.Compile.JwtPlayJson,
      Dependencies.Compile.ScalapbJson4s,
      Dependencies.Compile.Janino,
      Dependencies.Compile.Reflections,
      Dependencies.Compile.KamonBundle,
      Dependencies.Compile.KamonPrometheus,
      Dependencies.Compile.KamonJaeger,
      Dependencies.Compile.LogstashLogback,
      Dependencies.Compile.ScalaClassFinder,
      Dependencies.Compile.ApacheCommonValidator,
      Dependencies.Compile.GoogleRe2j,
      Dependencies.Compile.GoogleProtobufUtil,
      Dependencies.Compile.ScalapbCommonProtos,
      Dependencies.Compile.AkkaProjectionCore,
      Dependencies.Compile.AkkaProjectionKafka,
      Dependencies.Compile.AkkaProjectionSlick,
      Dependencies.Compile.AkkaProjectionEventSourced,
      Dependencies.Compile.CatsCore,
      Dependencies.Runtime.AkkaGrpcRuntime,
      Dependencies.Runtime.ScalapbRuntime,
      Dependencies.Runtime.ScalapbValidationRuntime,
      Dependencies.Runtime.PlayGrpcRuntime,
      Dependencies.Runtime.ScalapbCommonProtosRuntime,
      Dependencies.Test.ScalaTest,
      Dependencies.Test.ScalaMock,
      Dependencies.Test.AkkaMultiNodeTeskit,
      Dependencies.Test.AkkaTestkit,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.AkkaActorTestkitTyped,
      Dependencies.Test.EmbeddedPostgres,
      Dependencies.Test.EmbeddedKafka
    )
  )

cancelable in Global := true
