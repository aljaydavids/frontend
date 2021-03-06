import com.gu.deploy.PlayArtifact._
import com.gu.deploy.PlayAssetHash._
import com.typesafe.sbtscalariform.ScalariformPlugin._
import io.Source
import org.sbtidea.SbtIdeaPlugin._
import sbt._
import sbt.Keys._
import sbt.PlayProject._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin.MergeStrategy
import com.gu.RequireJS._
import com.gu.RequireJS
import com.gu.SbtJshintPlugin
import com.gu.SbtJshintPlugin._

object Frontend extends Build with Prototypes {

  val version = "1-SNAPSHOT"

  val common = library("common")

  val commonWithTests = common % "test->test;compile->compile"

  val article = application("article").dependsOn(commonWithTests)
  val gallery = application("gallery").dependsOn(commonWithTests)
  val tag = application("tag").dependsOn(commonWithTests)
  val section = application("section").dependsOn(commonWithTests)
  val front = application("front").dependsOn(commonWithTests)
  val coreNavigation = application("core-navigation").dependsOn(commonWithTests)
  val video = application("video").dependsOn(commonWithTests)

  val dev = application("dev-build")
    .dependsOn(front)
    .dependsOn(article)
    .dependsOn(video)
    .dependsOn(tag)
    .dependsOn(section)
    .dependsOn(gallery)
    .dependsOn(coreNavigation)

  val main = root().aggregate(
    common, article, gallery, tag, section, front, video, coreNavigation, dev
  )
}

trait Prototypes {

  val features = TaskKey[Seq[File]]("features", "Builds a file with BDD features in it")

  def featuresTask = (sources in Test, target, streams) map { (testFiles, targetDir, s) =>

    import s.log

    val Feature = """.*feature\("(.*)"\).*""".r
    val Scenario = """.*scenario\("(.*)".*""".r  // TODO tags
    val Given = """.*given\("(.*)"\).*""".r
    val When = """.*when\("(.*)"\).*""".r
    val Then = """.*then\("(.*)"\).*""".r
    val And = """.*and\("(.*)"\).*""".r
    val Info = """.*info\("(.*)"\).*""".r

    testFiles.filter(_.getName.endsWith("FeatureTest.scala")).map{ testFile: File =>
      log.info("creating feature file for: " + testFile)
      val name = testFile.getName.replace("FeatureTest.scala", ".feature")
      val featureFile = targetDir / name
      if (featureFile.exists) featureFile.delete()
      featureFile.createNewFile()
      (testFile, featureFile)
    }.map{ case(source, output) =>
      Source.fromFile(source).getLines().foreach{
        case Feature(message) => IO.append(output, "Feature: " + message + "\n")
        case Scenario(message) => IO.append(output, "\n\tScenario: " + message + "\n")
        case Given(message) => IO.append(output, "\t\tGiven " + message + "\n")
        case When(message) => IO.append(output, "\t\tWhen " + message + "\n")
        case Then(message) => IO.append(output, "\t\tThen " + message + "\n")
        case And(message) => IO.append(output, "\t\tAnd " + message + "\n")
        case Info(message) => IO.append(output, "\t" + message + "\n")
        case line => Unit
      }
      output
    }
  }

  val version: String

  def root() = Project("root", base = file("."))
    .settings(ideaSettings: _*)
    .settings(
      parallelExecution in Global := false
    )

  def base(name: String) = PlayProject(name, version, path = file(name), mainLang = SCALA)
    .settings(RequireJS.settings:_*)
    .settings(requireJsConfiguration: _*)
    .settings(jshintSettings:_*)
    .settings(scalariformSettings: _*)
    .settings(playAssetHashDistSettings: _*)
    .settings(
      scalaVersion := "2.9.1",

      maxErrors := 20,
      javacOptions := Seq("-g", "-source", "1.6", "-target", "1.6", "-encoding", "utf8"),
      scalacOptions := Seq("-unchecked", "-optimise", "-deprecation", "-Xcheckinit", "-encoding", "utf8"),

      ivyXML :=
        <dependencies>
          <exclude org="commons-logging"><!-- Conflicts with jcl-over-slf4j in Play. --></exclude>
          <exclude org="org.springframework"><!-- Because I don't like it. --></exclude>
        </dependencies>,

      organization := "com.gu",

      resolvers := Seq(
        "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases",
        Resolver.url("Typesafe Ivy Releases", url("http://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns),
        "JBoss Releases" at "http://repository.jboss.org/nexus/content/repositories/releases",
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Akka" at "http://repo.akka.io/releases"
      ),

      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "1.8" % "test"
      ),

      // Use ScalaTest https://groups.google.com/d/topic/play-framework/rZBfNoGtC0M/discussion
      testOptions in Test := Nil,

      // Copy unit test resources https://groups.google.com/d/topic/play-framework/XD3X6R-s5Mc/discussion
      unmanagedClasspath in Test <+= (baseDirectory) map { bd => Attributed.blank(bd / "test") },

      jshintFiles <+= baseDirectory { base =>
        (base / "app" / "assets" / "javascripts" ** "*.js") --- (base / "app" / "assets" / "javascripts" / "vendor" ** "*.js") 
      },

      jshintOptions <+= (baseDirectory) { base =>
        (base.getParentFile / "resources" / "jshint_conf.json")
      },

      (test in Test) <<= (test in Test) dependsOn (jshint),

      templatesImport ++= Seq(
        "common._",
        "model._",
        "views._",
        "views.support._",
        "conf._"
      )
    )

  val requireJsConfiguration = Seq(
    //effectively disables built in Play javascript compiler
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base => (base / "assets" ** "*.none")),

    requireJsAppDir <<= (baseDirectory){ base => base / "app" / "assets" / "javascripts" },
    requireJsBaseUrl := ".",
    requireJsDir <<= (resourceManaged) { resources => resources / "main" /"public" / "javascripts"},
    requireJsModules := Seq("bootstraps/article"),
    requireJsPaths := Map(
                            "bonzo" -> "vendor/bonzo-1.2.1",
                            "reqwest" -> "vendor/reqwest-0.4.5",
                            "qwery" -> "vendor/qwery-3.3.11",
                            "bean" -> "vendor/bean-0.4.11-1",
                            "domReady" -> "vendor/domReady-2.0.1",
                            "EventEmitter" -> "vendor/EventEmitter-3.1.5"
                          ),
    requireJsOptimize := true,

    resourceGenerators in Compile <+=  requireJsCompiler
  )

  def library(name: String) = base(name).settings(
    staticFilesPackage := "frontend-static",
    libraryDependencies ++= Seq(
      "com.gu" %% "management-play" % "5.13",
      "com.gu" %% "management-logback" % "5.13",
      "com.gu" %% "configuration" % "3.6",
      "com.gu.openplatform" %% "content-api-client" % "1.17",

      "com.typesafe.akka" % "akka-agent" % "2.0.2",
      "org.scala-tools.time" % "time_2.9.1" % "0.5",
      "com.googlecode.htmlcompressor" % "htmlcompressor" % "1.4",
      "com.yahoo.platform.yui" % "yuicompressor" % "2.4.6",

      "org.codehaus.jackson" % "jackson-core-asl" % "1.9.6",
      "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.6",
      "org.jsoup" % "jsoup" % "1.6.3",
      "org.jboss.dna" % "dna-common" % "0.6"
    )
  )

  def application(name: String) = base(name).settings(
    features <<= featuresTask,
    staticFilesPackage := "frontend-static",
    executableName := "frontend-%s" format  name,
    jarName in assembly <<= (executableName) { "%s.jar" format _ },
    //these merge strategies are for the htmlcompressor
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
      {
        case s: String if s.startsWith("org/mozilla/javascript/") => MergeStrategy.first
        case s: String if s.startsWith("jargs/gnu/") => MergeStrategy.first
        case "README" => MergeStrategy.first
        case "CHANGELOG" => MergeStrategy.first
        case x => old(x)
      }
    }
  )
}
