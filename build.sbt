lazy val root = project
  .in(file("."))
  .settings(
    name         := "v4s",
    version      := "0.1.0",
    scalaVersion := "3.3.4",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.12.0",
      "org.scalameta" %% "munit"     % "1.0.3" % Test,
    ),
  )
