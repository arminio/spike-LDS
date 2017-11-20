name := "spike-LDS"

version := "0.1"

scalaVersion := "2.12.4"


libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.github.seratch" %% "awscala" % "0.5.+",
  "org.zeroturnaround" % "zt-zip" % "1.10",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.86",
//  "com.amazonaws" % "aws-java-sdk-elasticbeanstalk" % "1.11.86",
  "com.amazonaws" % "aws-java-sdk-sns" % "1.11.86",
  "com.typesafe.play" % "play-json_2.11" % "2.5.12",
  "commons-lang" % "commons-lang" % "2.6",
  "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.0",
  "org.slf4j" % "slf4j-log4j12" % "1.7.25",
  "log4j" % "log4j" % "1.2.17",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "commons-io" % "commons-io" % "2.5",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.mockito" % "mockito-all" % "1.10.19"  % "test",
  "com.github.tomakehurst" % "wiremock" % "1.52" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "net.sourceforge.jregex" % "jregex" % "1.2_01"
)
