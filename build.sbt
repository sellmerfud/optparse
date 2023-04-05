
import xerial.sbt.Sonatype._

lazy val optparse = (project in file("."))
  .settings(
    name         := "optparse",
    version      := "2.3",
    organization := "org.sellmerfud",
    scalaVersion := "2.13.9",
    description  := "A simple but powerful Scala command line parser.",
    
    scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature" ),
    
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    
    ThisBuild / versionScheme := Some("semver-spec"),
    credentials += Credentials(Path.userHome / ".sbt" / "pgp.credentials"),
    publishMavenStyle := true,
    Test / publishArtifact  := false,
    publishTo := sonatypePublishToBundle.value,
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/MIT")),
    sonatypeProjectHosting := Some(GitHubHosting("sellmerfud", "optparse", "sellmerfud@gmail.com"))
)


