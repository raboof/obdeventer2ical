resolvers += Resolver.sonatypeRepo("public")

scalacOptions := Seq("-feature", "-deprecation")

scalaVersion := "2.12.4"

fork in run := true

libraryDependencies += "net.bzzt" %% "scala-icalendar" % "0.0.1-SNAPSHOT"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.2.0"
libraryDependencies += "org.dispatchhttp" %% "dispatch-core" % "0.14.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
