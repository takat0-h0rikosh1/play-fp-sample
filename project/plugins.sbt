resolvers ++= Seq(
  "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.11.0")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.2")
