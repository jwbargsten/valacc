val scalacVersion = "3.8.1"
ThisBuild / organization := "org.bargsten"
ThisBuild / organizationName := "Joachim Bargsten"
ThisBuild / organizationHomepage := Some(url("https://bargsten.org/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/jwbargsten/valacc"),
    "scm:git@github.com:jwbargsten/valacc.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "jwbargsten",
    name = "Joachim Bargsten",
    email = "jw@bargsten.org",
    url = url("https://bargsten.org")
  )
)
ThisBuild / versionScheme := Some("early-semver")
Global / pgpSigningKey := sys.env.get("PGP_KEY_ID")

ThisBuild / description := "accumulative validation for Scala"
ThisBuild / licenses := List(
  "Apache 2" -> new URI("http://www.apache.org/licenses-2.0.txt").toURL
)
ThisBuild / homepage := Some(url("https://github.com/jwbargsten/valacc"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true

lazy val root = project
  .in(file("."))
  .settings(
    name         := "valacc",
    version      := "0.2.0",
    scalaVersion := scalacVersion,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.scalameta" %% "munit"     % "1.2.2" % Test,
    ),
  )

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
