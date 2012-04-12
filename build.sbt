name := "optparse"

description := "A simple but powerful Scala command line parser."

organization := "org.sellmerfud"

version := "1.0"

scalaVersion := "2.9.1"

publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

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
  </developers>)

