lazy val root = project
  .in(file("."))
  .settings(
    name         := "valacc",
    version      := "0.0.1",
    scalaVersion := "3.8.1",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.scalameta" %% "munit"     % "1.2.2" % Test,
    ),
  )
