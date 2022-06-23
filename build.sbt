val libraryName = "webdriver-factory"

val scala2_12 = "2.12.12"
val scala2_13 = "2.13.7"

val compileDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.seleniumhq.selenium"     % "selenium-java" % "4.2.2",
  "org.slf4j"                   % "slf4j-simple"  % "1.7.36"
)

val testDependencies = Seq(
  "com.vladsch.flexmark" % "flexmark-all" % "0.36.8"  % Test,
  "org.mockito"          % "mockito-all"  % "1.10.19" % Test,
  "org.pegdown"          % "pegdown"      % "1.6.0"   % Test,
  "org.scalatest"       %% "scalatest"    % "3.2.9"   % Test
)

lazy val library = Project(libraryName, file("."))
  .settings(
    majorVersion := 0,
    scalaVersion := scala2_12,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= compileDependencies ++ testDependencies,
    isPublicArtefact := true
  )
