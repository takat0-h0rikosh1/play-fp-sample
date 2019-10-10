import com.typesafe.config.ConfigFactory

name := """play-fp-sample"""
organization := "takato.h0rikosh1@gmail.com"

val scalikejdbcVer = "3.3.2"
val monix = "io.monix" %% "monix" % "3.0.0-RC4"

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
  ),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-Xlint:-unused,_",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-language:higherKinds",
    "-language:implicitConversions"
  ),
  fork in Test := true,
  javaOptions in Test ++= Seq("-Xmx2G", "-Djava.awt.headless=true"),
  scalacOptions in Test ++= Seq("-Yrangepos"),
  parallelExecution in Test := false,
  // パッケージング時scalaDoc作成しないため
  sources in(Compile, doc) := Seq.empty,
  publishArtifact in(Compile, packageDoc) := false,
  updateOptions := updateOptions.value.withCachedResolution(true),
  resolvers += Resolver.sonatypeRepo("releases")
)

lazy val coreBase = file("play-fp-sample")
lazy val portBase = coreBase / "port"
lazy val webServiceBase = portBase / "primary" / "webservice"
lazy val portDBBase = portBase / "secondary" / "database"

lazy val persistenceDBConfLocal = ConfigFactory
  .defaultOverrides()
  .withFallback(ConfigFactory.parseFile(portDBBase / "src" / "main" / "resources" / "db" / "db.local.conf"))
  .resolve()

lazy val persistenceDBConfTest = ConfigFactory
  .defaultOverrides()
  .withFallback(ConfigFactory.parseFile(portDBBase / "src" / "test" / "resources" / "application.conf"))
  .resolve()

lazy val core = Project(
  id = "core",
  base = coreBase
).settings(commonSettings)
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    libraryDependencies := Seq(
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVer,
      "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.7.0-scalikejdbc-3.3"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
  .aggregate(domain, application, port)
  .dependsOn(port % "compile->compile;test->test")

lazy val domain = Project(
  id = "domain",
  base = coreBase / "domain"
).settings(commonSettings)

lazy val application = Project(
  id = "application",
  base = coreBase / "application"
).settings(commonSettings,
  libraryDependencies += monix)
  .dependsOn(domain % "compile->compile;test->test")

lazy val port = Project(
  id = "port",
  base = portBase
)
  .aggregate(portWebService, portDB)
  .dependsOn(
    portWebService % "compile->compile;test->test",
    portDB % "compile->compile;test->test",
  )

lazy val portWebService = Project(
  id = "port-web-service",
  base = webServiceBase
).settings(commonSettings, libraryDependencies ++= Seq(monix, guice))
  .settings(routesImport ++= Seq(
    "controllers.CustomRouteBinder._",
//    "controllers.NameBinder._",
    "bindable.Implicits._"
  ))
  .dependsOn(application % "compile->compile;test->test")
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)

lazy val portDB = Project(
  id = "port-database",
  base = portDBBase
).dependsOn(
  application % "compile->compile;test->test"
)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.mariadb.jdbc" % "mariadb-java-client" % "1.5.7",
      "org.skinny-framework" %% "skinny-orm" % "3.0.2",
      "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVer % Test,
      "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % "3.2.+",
      monix,
      guice
    )
  )
//  .settings(
//    flywayUrl := persistenceDBConfLocal.getString("db.default.url"),
//    flywayUser := persistenceDBConfLocal.getString("db.default.user"),
//    flywayPassword := persistenceDBConfLocal.getString("db.default.password"),
//    flywayLocations := Seq(
//      "filesystem:" + portDatabaseBase + "/src/main/resources/db/migration",
//      "filesystem:" + portDatabaseBase + "/src/main/resources/db/data"
//    ),
//    flywayUrl in Test := persistenceDBConfTest.getString("db.default.url"),
//    flywayUser in Test := persistenceDBConfTest.getString("db.default.user"),
//    flywayPassword in Test := persistenceDBConfTest.getString("db.default.password"),
//    flywayLocations in Test := Seq(
//      "filesystem:" + portDatabaseBase + "/src/main/resources/db/migration"
//    ),
//    test.in(Test) := {
//      test
//        .in(Test)
//        .dependsOn(flywayMigrate.in(Test))
//        .dependsOn(flywayClean.in(Test))
//        .value
//    }
//  )

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "takato.h0rikosh1@gmail.com.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "takato.h0rikosh1@gmail.com.binders._"
