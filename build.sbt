name := "scala-vs-java-bench"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.2"

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value

fork in run := true

javaOptions in run += "-Xmx1G"
