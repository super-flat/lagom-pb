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
  .enablePlugins(BuildSettings)
  .enablePlugins(Publish)
  .settings(
    name := "lagompb-core",
    unmanagedResources / excludeFilter := HiddenFileFilter || "*tests*",
    coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";"),
    PB.protoSources in Compile ++= Seq(file("submodules/protobuf")),
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
    crossScalaVersions := Dependencies.Versions.CrossScalaForPlugin,
    scalaVersion := Dependencies.Versions.Scala212,
    addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % Dependencies.Versions.LagomVersion),
    addSbtPlugin("com.lightbend.akka.grpc" %% "sbt-akka-grpc" % Dependencies.Versions.AkkaGrpcVersion),
    addSbtPlugin("com.thesamet" % "sbt-protoc" % Dependencies.Versions.SbtProtocVersion),
    resolvers += Resolver.bintrayRepo("playframework", "maven"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Dependencies.SbtPlugin
  )

cancelable in Global := true
