val libraryName = "webdriver-factory"

val compileDependencies = Seq(
  "org.seleniumhq.selenium"     % "selenium-java" % "3.141.59",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.slf4j"                   % "slf4j-simple"  % "1.7.30"
)

val testDependencies = Seq(
  "org.pegdown"    % "pegdown"     % "1.6.0"   % "test",
  "org.scalatest" %% "scalatest"   % "3.0.8"   % "test",
  "org.mockito"    % "mockito-all" % "1.10.19" % "test"
)

lazy val library = Project(libraryName, file("."))
  .settings(
    majorVersion := 0,
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.10"),
    libraryDependencies ++= compileDependencies ++ testDependencies,
    isPublicArtefact := true
  )
