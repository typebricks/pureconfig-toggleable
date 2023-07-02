import xerial.sbt.Sonatype._

ThisBuild / scalaVersion := "3.3.0"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

val pureConfigVersion = "0.17.4"

libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)

sonatypeProjectHosting := Some(GitHubHosting("typebricks", "pureconfig-toggleable", "klass.ivanklass@gmail.com"))

name := "pureconfig-toggleable"
organization := "io.github.typebricks"
licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

description := "A tiny type brick for Scala 3 that make it possible to explicitly toggle pureconfig sections"

version := "1.0.0"

scalacOptions ++= Seq(
  "-feature",
  "-Ykind-projector:underscores",
  "-deprecation",
  "-Xfatal-warnings",
  "-Wunused:all",
  "-Wvalue-discard",
  "-new-syntax",
  "-rewrite"
)

Compile / console / scalacOptions -= "-Wunused:all"

addCommandAlias("prePR", ";scalafixAll;scalafmtAll;compile;test")
