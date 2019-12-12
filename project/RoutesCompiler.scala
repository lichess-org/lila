import play.routes.compiler._

import play.routes.compiler.RoutesCompiler.RoutesCompilerTask
import java.io.File

object LilaRoutesGenerator extends RoutesGenerator {

  val ForwardsRoutesFile = "Routes.scala"
  val ReverseRoutesFile = "ReverseRoutes.scala"
  val RoutesPrefixFile = "RoutesPrefix.scala"
  val JavaWrapperFile = "routes.java"

  val id = "lila"

  def generate(task: RoutesCompilerTask, namespace: Option[String], rules: List[Rule]): Seq[(String, String)] = {

    val folder = namespace.map(_.replace('.', '/') + "/").getOrElse("") + "/"

    val sourceInfo = RoutesSourceInfo(task.file.getCanonicalPath.replace(File.separator, "/"), new java.util.Date().toString)
    val routes = rules.collect { case r: Route => r }

    val forwardsRoutesFiles = if (task.forwardsRouter) {
      Seq(folder + ForwardsRoutesFile -> generateRouter(sourceInfo, namespace, task.additionalImports, rules))
    } else {
      Nil
    }

    val reverseRoutesFiles = if (task.reverseRouter) {
      Seq(folder + RoutesPrefixFile -> generateRoutesPrefix(sourceInfo, namespace)) ++
        generateReverseRouters(sourceInfo, namespace, task.additionalImports, routes, task.namespaceReverseRouter) ++
        generateJavaWrappers(sourceInfo, namespace, rules, task.namespaceReverseRouter)
    } else {
      Nil
    }

    forwardsRoutesFiles ++ reverseRoutesFiles
  }

  private def generateRouter(sourceInfo: RoutesSourceInfo, namespace: Option[String], additionalImports: Seq[String], rules: List[Rule]) =
    static.twirl.forwardsRouter(
      sourceInfo,
      namespace,
      additionalImports,
      rules
    ).body

  private def generateRoutesPrefix(sourceInfo: RoutesSourceInfo, namespace: Option[String]) =
    static.twirl.routesPrefix(
      sourceInfo,
      namespace,
      _.call.instantiate
    ).body

  private def generateReverseRouters(sourceInfo: RoutesSourceInfo, namespace: Option[String], additionalImports: Seq[String], routes: List[Route], namespaceReverseRouter: Boolean) = {
    routes.groupBy(_.call.packageName).map {
      case (pn, routes) =>
        val packageName = namespace.filter(_ => namespaceReverseRouter).map(_ + "." + pn).getOrElse(pn)
        (packageName.replace(".", "/") + "/" + ReverseRoutesFile) ->
          static.twirl.reverseRouter(
            sourceInfo,
            namespace,
            additionalImports,
            packageName,
            routes,
            namespaceReverseRouter,
            _.call.instantiate
          ).body
    }
  }

  private def generateJavaWrappers(sourceInfo: RoutesSourceInfo, namespace: Option[String], rules: List[Rule], namespaceReverseRouter: Boolean) = {
    rules.collect { case r: Route => r }.groupBy(_.call.packageName).map {
      case (pn, routes) =>
        val packageName = namespace.filter(_ => namespaceReverseRouter).map(_ + "." + pn).getOrElse(pn)
        val controllers = routes.groupBy(_.call.controller).keys.toSeq

        (packageName.replace(".", "/") + "/" + JavaWrapperFile) ->
          renderJavaWrappers(sourceInfo, namespace, packageName, controllers)
    }
  }

  private def renderJavaWrappers(
    sourceInfo: RoutesSourceInfo,
    pkg: Option[String],
    packageName: String,
    controllers: Seq[String]) = s"""package $packageName;

import ${pkg getOrElse "_routes_"}.RoutesPrefix;

public class routes {
""" + controllers.map { controller =>
  s"""public static final ${packageName}.Reverse${controller} ${controller} = new ${packageName}.Reverse${controller}(RoutesPrefix.byNamePrefix());
"""
}.mkString + """

}"""
}
