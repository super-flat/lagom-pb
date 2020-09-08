# Get Started

Kindly check the latest version from @link:[Versions](https://github.com/super-flat/lagom-pb/releases) { open=new }

1. Add to your `plugins.sbt`:

```scala
val LagompbVersion = "0.8.2"
addSbtPlugin("io.superflat" % "lagompb-plugin" % LagompbVersion)
```

2. Add to your `build.sbt`:

```scala
val LagompbVersion = "0.8.2"
libraryDependencies ++= Seq(
  "io.superflat" %% "lagompb-core" % LagompbVersion,
  "io.superflat" %% "lagompb-core" % LagompbVersion % "protobuf"
)
```