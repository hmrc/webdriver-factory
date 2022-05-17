val libraryName = "webdriver-factory"

val scala2_12 = "2.12.12"
val scala2_13 = "2.13.7"

val compileDependencies = Seq(
  "org.seleniumhq.selenium"     % "selenium-java" % "3.141.59",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.slf4j"                   % "slf4j-simple"  % "1.7.30"
)

val testDependencies = Seq(
  "org.pegdown"          % "pegdown"      % "1.6.0"   % "test",
  "org.scalatest"       %% "scalatest"    % "3.2.3"   % "test",
  "org.mockito"          % "mockito-all"  % "1.10.19" % "test",
  "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test
)

lazy val library = Project(libraryName, file("."))
  .settings(
    majorVersion := 0,
    scalaVersion := scala2_12,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= compileDependencies ++ testDependencies,
    isPublicArtefact := true
  )
