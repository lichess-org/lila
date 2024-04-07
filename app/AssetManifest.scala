package lila.app

import play.api.{ Environment, Mode }
import play.api.libs.json.{ JsObject, Json, JsValue, JsString }

case class SplitAsset(name: String, imports: List[String])
case class AssetMaps(js: Map[String, SplitAsset], css: Map[String, String])

final class AssetManifest(environment: Environment):

  private var maps: AssetMaps = AssetMaps(Map.empty, Map.empty)
  private val keyRe           = """^(?!chunk\.)(\S+)\.([A-Z0-9]{8})\.(?:js|css)""".r

  def reload(istream: Option[java.io.InputStream] = None): AssetManifest =
    istream
      .orElse(
        environment
          .resourceAsStream(s"manifest.${if environment.mode == Mode.Prod then "prod" else "dev"}.json")
      )
      .fold(AssetMaps(Map.empty, Map.empty))(readMaps)
    this

  def js(key: String): Option[SplitAsset] = maps.js.get(key)
  def css(key: String): Option[String]    = maps.css.get(key)
  def deps(keys: List[String]): List[String] =
    keys.flatMap { key => js(key).fold(List.empty[String])(asset => asset.imports) }.distinct

  private def key(fullName: String): String =
    fullName match
      case keyRe(k, _) => k
      case _           => fullName

  private def closure(
      name: String,
      jsMap: Map[String, SplitAsset],
      visited: Set[String] = Set.empty
  ): List[String] =
    val k = key(name)
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
      .map { case (k, value) =>
        val name    = (value \ "hash").asOpt[String].fold(s"$k.js")(h => s"$k.$h.js")
        val imports = (value \ "imports").asOpt[List[String]].getOrElse(Nil)
        (k, SplitAsset(name, imports))
      }
      .toMap
    maps = AssetMaps(
      js.map { case (k, asset) =>
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

object AssetManifest:
  def apply(environment: Environment): AssetManifest =
    new AssetManifest(environment).reload()
