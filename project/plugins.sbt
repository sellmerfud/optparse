// // For sbt 1.1.x, and 0.13.x
// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")
// addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
// pgpSecretRing := file("/User/curt/.gnupg/secring.gpg")
// pgpPublicRing := file("/User/curt/.gnupg/pubring.gpg")

// For sbt 1.x (sbt-sonatype 2.3 or higher)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.4")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")