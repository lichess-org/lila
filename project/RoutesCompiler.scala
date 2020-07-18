import play.routes.compiler._

import java.io.File
import play.routes.compiler.RoutesCompiler.RoutesCompilerTask

object LilaRoutesGenerator extends RoutesGenerator {

  val ForwardsRoutesFile = "Routes.scala"
  val ReverseRoutesFile  = "ReverseRoutes.scala"
  val RoutesPrefixFile   = "RoutesPrefix.scala"
  val JavaWrapperFile    = "routes.java"

  val id = "lila"

  import InjectedRoutesGenerator.Dependency

  def generate(
      task: RoutesCompilerTask,
      namespace: Option[String],
      rules: List[Rule]
  ): Seq[(String, String)] = {
    val folder = namespace.map(_.replace('.', '/') + "/").getOrElse("") + "/"

    val sourceInfo =
      RoutesSourceInfo(task.file.getCanonicalPath.replace(File.separator, "/"), new java.util.Date().toString)
    val routes = rules.collect { case r: Route => r }

    val routesPrefixFiles = Seq(folder + RoutesPrefixFile -> generateRoutesPrefix(sourceInfo, namespace))

    val forwardsRoutesFiles = if (task.forwardsRouter) {
      Seq(folder + ForwardsRoutesFile -> generateRouter(sourceInfo, namespace, task.additionalImports, rules))
    } else {
      Nil
    }

    val reverseRoutesFiles = if (task.reverseRouter) {
      generateReverseRouters(
        sourceInfo,
        namespace,
        task.additionalImports,
        routes,
        task.namespaceReverseRouter
      ) ++
        generateJavaWrappers(sourceInfo, namespace, rules, task.namespaceReverseRouter)
    } else {
      Nil
    }

    routesPrefixFiles ++ forwardsRoutesFiles ++ reverseRoutesFiles
  }

  private def generateRouter(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      additionalImports: Seq[String],
      rules: List[Rule]
  ) = {
    @annotation.tailrec
    def prepare(
        rules: List[Rule],
        includes: Seq[Include],
        routes: Seq[Route]
    ): (Seq[Include], Seq[Route]) =
      rules match {
        case (inc: Include) :: rs =>
          prepare(rs, inc +: includes, routes)

        case (rte: Route) :: rs =>
          prepare(rs, includes, rte +: routes)

        case _ => includes.reverse -> routes.reverse
      }

    val (includes, routes) = prepare(rules, Seq.empty, Seq.empty)

    // Generate dependency descriptors for all includes
    val includesDeps: Map[String, Dependency[Include]] =
      includes
        .groupBy(_.router)
        .zipWithIndex
        .flatMap {
          case ((router, includes), index) =>
            includes.headOption.map { inc =>
              router -> Dependency(router.replace('.', '_') + "_" + index, router, inc)
            }
        }
        .toMap

    // Generate dependency descriptors for all routes
    val routesDeps: Map[(Option[String], String, Boolean), Dependency[Route]] =
      routes
        .groupBy { r =>
          (r.call.packageName, r.call.controller, r.call.instantiate)
        }
        .zipWithIndex
        .flatMap {
          case ((key @ (packageName, controller, instantiate), routes), index) =>
            routes.headOption.map { route =>
              val clazz = packageName.map(_ + ".").getOrElse("") + controller
              // If it's using the @ syntax, we depend on the provider (ie, look it up each time)
              val dep   = if (instantiate) s"javax.inject.Provider[$clazz]" else clazz
              val ident = controller + "_" + index

              key -> Dependency(ident, dep, route)
            }
        }
        .toMap

    // Get the distinct dependency descriptors in the same order as defined in the routes file
    val orderedDeps = rules.map {
      case include: Include =>
        includesDeps(include.router)
      case route: Route =>
        routesDeps((route.call.packageName, route.call.controller, route.call.instantiate))
    }.distinct

    // Map all the rules to dependency descriptors
    val rulesWithDeps = rules.map {
      case include: Include =>
        includesDeps(include.router).copy(rule = include)
      case route: Route =>
        routesDeps((route.call.packageName, route.call.controller, route.call.instantiate)).copy(rule = route)
    }

    inject.twirl
      .forwardsRouter(
        sourceInfo,
        namespace,
        additionalImports,
        orderedDeps,
        rulesWithDeps,
        includesDeps.values.toSeq
      )
      .body
      .replace("""import _root_.controllers.Assets.Asset""", "")
  }

  private def generateRoutesPrefix(sourceInfo: RoutesSourceInfo, namespace: Option[String]) =
    static.twirl
      .routesPrefix(
        sourceInfo,
        namespace,
        _ => true
      )
      .body

  private def generateReverseRouters(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      additionalImports: Seq[String],
      routes: List[Route],
      namespaceReverseRouter: Boolean
  ) = {
    routes.groupBy(_.call.packageName).map {
      case (pn, routes) =>
        val packageName = namespace
          .filter(_ => namespaceReverseRouter)
          .map(_ + pn.map("." + _).getOrElse(""))
          .orElse(pn.orElse(namespace))
        (packageName.map(_.replace(".", "/") + "/").getOrElse("") + ReverseRoutesFile) ->
          static.twirl
            .reverseRouter(
              sourceInfo,
              namespace,
              additionalImports,
              packageName,
              routes,
              namespaceReverseRouter,
              _ => true
            )
            .body
            .replace("""import _root_.controllers.Assets.Asset""", "")
    }
  }

  private def generateJavaWrappers(
      sourceInfo: RoutesSourceInfo,
      namespace: Option[String],
      rules: List[Rule],
      namespaceReverseRouter: Boolean
  ): Iterable[(String, String)] =
    rules.collect { case r: Route => r }.groupBy(_.call.packageName).map {
      case (pn, routes) =>
        val packageName = namespace
          .filter(_ => namespaceReverseRouter)
          .map(_ + pn.map("." + _).getOrElse(""))
          .orElse(pn.orElse(namespace))
        val controllers = routes.groupBy(_.call.controller).keys.toSeq
        (packageName.map(_.replace(".", "/") + "/").getOrElse("") + JavaWrapperFile) -> {
          val pack = packageName getOrElse "controllers"
          val ns   = namespace getOrElse "routes"
          s"""package $pack;

import $ns.RoutesPrefix;

public class routes {
""" + controllers.map { controller =>
            s"""public static final ${pack}.Reverse${controller} ${controller} = new ${pack}.Reverse${controller}(RoutesPrefix.byNamePrefix());
"""
          }.mkString + """

}"""
        }
    }
}
