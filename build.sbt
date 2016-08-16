name := """play-java"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava,PlayEbean)


scalaVersion := "2.11.7"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"


libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.alibaba" % "fastjson" % "1.2.14",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
   "org.apache.httpcomponents" % "httpmime" % "4.5.2",
"org.apache.httpcomponents" % "httpcore" % "4.4.5",
  "commons-collections" % "commons-collections" % "3.2.2",
  "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B3-SNAPSHOT"
)


fork in run := false


fork in run := true