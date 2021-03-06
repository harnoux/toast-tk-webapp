name := "toast-tk-webapp"

version := "0.1.4"

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "MavenSnapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Typesafe" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  ws,
  "com.google.code.gson" % "gson" % "2.5",
  "org.reactivemongo" % "reactivemongo_2.11" % "0.11.7",
  "org.webjars" % "jquery" % "1.7.2",
  "org.webjars" % "angularjs" % "1.3.0",
  "org.webjars" % "requirejs" % "2.1.1",
  "org.webjars" % "webjars-play" % "2.1.0-1",
  "org.webjars" % "bootstrap" % "3.2.0-1",
  "io.toast-tk" % "toast-tk-runtime" % "0.1.4",
  "io.toast-tk" % "toast-tk-dao-api" % "0.1.4",
  "io.toast-tk" % "toast-tk-selenium-plugin" % "0.1.4-SNAPSHOT",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.0",
  "io.toast-tk" % "toast-tk-interpret" % "0.1.4",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.0",
  "com.pauldijou" %% "jwt-play" % "0.5.1",
  "org.scalatestplus" % "play_2.11" % "1.4.0-M3"% "test",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % "test",
  "com.typesafe.play" %% "play-mailer" % "4.0.0",
  "io.megl" % "play-json-extra_2.11" % "2.4.3",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % "test",
  "org.seleniumhq.selenium" % "htmlunit-driver" % "2.23.1" % "test",
  "org.seleniumhq.selenium" % "selenium-support" % "3.0.1" % "test",
  "com.sendgrid"%"sendgrid-java"%"3.0.9"
)

//TODO: move to injected resources @Inject()
//routesGenerator := StaticRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(SbtWeb).enablePlugins(PlayScala)

unmanagedResourceDirectories in Assets += baseDirectory.value / "assets"

lazy val npmBuildTask = TaskKey[Unit]("npm") 
npmBuildTask := {
  "npm install" #&& "node ./node_modules/bower/bin/bower install" #&& "node ./node_modules/gulp/bin/gulp" #&& "rm -rf ./app/assets/libs" #&& "mv -f ./libs ./app/assets"!
}

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

compile <<= (compile in Compile) dependsOn npmBuildTask