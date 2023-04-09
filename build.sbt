
import xerial.sbt.Sonatype._

lazy val scala_3_2  = "3.2.2"
lazy val scala_2_13 = "2.13.10"
lazy val scala_2_12 = "2.12.17"
lazy val supportedScalaVersions = List(scala_3_2, scala_2_13, scala_2_12)

// Set the default version for building
ThisBuild / scalaVersion := scala_2_13

// lazy val root = (project in file("."))
//   .aggregate(optparse)
//   .settings(
//     name               := "optparse root",
//     publish / skip     := true,
//     crossScalaVersions := Nil,
//   )
  
lazy val optparse = (project in file("."))
  .settings(
    name               := "optparse",
    description        := "A simple but powerful Scala command line parser.",
    version            := "2.4-SNAPSHOT",
    organization       := "org.sellmerfud",
    crossScalaVersions := supportedScalaVersions,
    
    scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature" ),
    
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.15" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    
    ThisBuild / versionScheme := Some("semver-spec"),
    credentials += Credentials(Path.userHome / ".sbt" / "pgp.credentials"),
    publishMavenStyle := true,
    Test / publishArtifact  := false,
    publishTo := sonatypePublishToBundle.value,
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/MIT")),
    sonatypeProjectHosting := Some(GitHubHosting("sellmerfud", "optparse", "sellmerfud@gmail.com"))
)


