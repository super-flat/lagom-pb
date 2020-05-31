import lagompb.{CommonSettings, CoverageWhitelist, LagomAkka, LagomSettings, NoPublish}

// custom task that prints the artifact name
lazy val printArtifactName: TaskKey[Unit] = taskKey[Unit]("Get the artifact name")
printArtifactName := {
  // scalastyle:off println
  println((artifactPath in (Compile, packageBin)).value.getName)
  // scalastyle:on println
}

lazy val root = project
  .in(file("."))
  .aggregate(
    `lagompb-core`,
    docs,
    `lagompb-readside`
  )
  .enablePlugins(CommonSettings)
  .enablePlugins(NoPublish)
  .settings(
    name := "lagompb"
  )

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ParadoxMaterialThemePlugin)
  .enablePlugins(NoPublish)
  .settings(
    // Make sure code generation is run before paradox:
    (Compile / paradox) := (Compile / paradox).dependsOn(Compile / compile).value,
    Compile / paradoxMaterialTheme ~= {
      _.withFont("Ubuntu", "Ubuntu Mono")
        .withCopyright("Copyright © SuperFlat.io")
    },
    paradoxProperties in Compile ++= Map(
      "snip.github_link" -> "true"
    )
  )

lazy val `lagompb-core` = project
  .in(file("core/lagompb-core"))
  .enablePlugins(LagomScala)
  .settings(lagomForkedTestSettings: _*)
  .enablePlugins(AkkaGrpcPlugin)
  .enablePlugins(PlayAkkaHttp2Support)
  .enablePlugins(LagomSettings)
  .enablePlugins(LagomAkka)
  .enablePlugins(Publish)
  .settings(
    name := "lagompb-core",
    Compile / unmanagedResources += (Compile / sourceDirectory).value / "main" / "protobuf",
    coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";")
  )
  .settings(
    PB.protoSources in Compile := Seq(file("core/lagompb-core/src/main/protobuf")),
    PB.includePaths in Compile ++= Seq(file("core/lagompb-core/src/main/protobuf")),
    PB.targets in Compile := Seq(
      scalapb.gen(
        flatPackage = false,
        javaConversions = false,
        grpc = false
      ) -> (sourceManaged in Compile).value
    ),
  )

lazy val `lagompb-readside` = project
  .in(file("core/lagompb-readside"))
  .enablePlugins(LagomScala)
  .enablePlugins(LagomSettings)
  .enablePlugins(LagomAkka)
  .enablePlugins(Publish)
  .settings(
    name := "lagompb-readside",
    coverageExcludedPackages := CoverageWhitelist.whitelist.mkString(";")
  )
  .dependsOn(`lagompb-core`)

cancelable in Global := true
