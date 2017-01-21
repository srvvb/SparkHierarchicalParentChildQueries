name := "SparkHierarchicalParentChildQueries"

version := "1.0"

scalaVersion := "2.10.5"

// adding graphframes and spark-sql , if needed for future
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.6.3",
  "org.apache.spark" %% "spark-sql" % "1.6.3",
  "org.apache.spark" %% "spark-graphx" % "1.6.3",
  "graphframes" % "graphframes" % "0.3.0-spark1.6-s_2.10",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

// for graphframes
resolvers += "SparkPackages" at "https://dl.bintray.com/spark-packages/maven/"

