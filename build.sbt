
lazy val commonSettings = Seq(
  organization := "org.sellmerfud",
  version      := "2.2-SNAPSHOT",
  scalaVersion := "2.11.1"
)

lazy val optparse = (project in file("."))
  .settings(
    commonSettings,
    name        := "optparse",
    description := "A simple but powerful Scala command line parser.",
    scalacOptions       ++= Seq( "-deprecation", "-unchecked", "-feature" ),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    
    publishMavenStyle := true,
    publishArtifact in Test := false,

    publishTo := {
      val nexus = "https://oss.sonatype.org"
      if (isSnapshot.value) 
        Some("snapshots" at s"$nexus/content/repositories/snapshots") 
      else
        Some("releases"  at s"$nexus/service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/sellmerfud/optparse</url>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>http://www.opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>https://github.com/sellmerfud/optparse</url>
        <connection>scm:git:git://github.com/sellmerfud/optparse.git</connection>
      </scm>
      <developers>
        <developer>
          <id>sellmerfud</id>
          <name>Curt Sellmer</name>
          <email>sellmerfud@gmail.com</email>
        </developer>
      </developers>
    )
)



