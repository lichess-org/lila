package lila.app

import play.api.{ Environment, Mode }
import play.api.libs.json.{ JsObject, Json, JsValue, JsString }
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

case class SplitAsset(name: String, imports: List[String])
case class AssetMaps(js: Map[String, SplitAsset], css: Map[String, String])

final class AssetManifest(environment: Environment):

  private val filename = s"manifest.${if environment.mode == Mode.Prod then "prod" else "dev"}.json"
  private val pathname = environment.getFile(s"conf/$filename").toPath
  private var lastModified: Instant = Instant.MIN
  private var maps: AssetMaps       = AssetMaps(Map.empty, Map.empty)
  private val keyRe                 = """^(?!common\.)(\S+)\.([A-Z0-9]{8})\.(?:js|css)""".r

  def js(key: String): Option[SplitAsset]    = maps.js.get(key)
  def css(key: String): Option[String]       = maps.css.get(key)
  def deps(keys: List[String]): List[String] = keys.flatMap { key => js(key).so(_.imports) }.distinct

  def update(): Unit =
    val current = Files.getLastModifiedTime(pathname).toInstant
    if current.isAfter(lastModified) then
      lastModified = current
      maps = readMaps(Files.newInputStream(pathname))

  update()

  private def keyOf(fullName: String): String =
    fullName match
      case keyRe(k, _) => k
      case _           => fullName

  private def closure(
      name: String,
      jsMap: Map[String, SplitAsset],
      visited: Set[String] = Set.empty
  ): List[String] =
    val k = keyOf(name)
    jsMap.get(k) match
      case Some(asset) if !visited.contains(k) =>
        asset.imports.flatMap: importName =>
          importName :: closure(importName, jsMap, visited + name)
      case _ => Nil

  private def readMaps(istream: java.io.InputStream) =
    val manifest = Json.parse(istream)
    val js = (manifest \ "js")
      .as[JsObject]
      .value
      .map { (k, value) =>
        val name    = (value \ "hash").asOpt[String].fold(s"$k.js")(h => s"$k.$h.js")
        val imports = (value \ "imports").asOpt[List[String]].getOrElse(Nil)
        (k, SplitAsset(name, imports))
      }
      .toMap
    AssetMaps(
      js.map { (k, asset) =>
        k -> (if asset.imports.nonEmpty then asset.copy(imports = closure(asset.name, js).distinct)
              else asset)
      },
      (manifest \ "css")
        .as[JsObject]
        .value
        .map { case (k, asset) =>
          val hash = (asset \ "hash").as[String]
          (k, s"$k.$hash.css")
        }
        .toMap
    )
