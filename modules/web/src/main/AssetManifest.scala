package lila.web

import play.api.Environment
import play.api.libs.json.{ JsObject, JsValue, Json, JsString }
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import java.nio.file.Files

import lila.core.config.NetConfig

case class SplitAsset(path: Option[String], imports: List[String], inlineJs: Option[String]):
  val allModules = path.toList ++ imports

case class AssetMaps(
    js: Map[String, SplitAsset],
    css: Map[String, String],
    hashed: Map[String, String],
    modified: Instant
)

final class AssetManifest(environment: Environment, net: NetConfig, ws: StandaloneWSClient)(using
    Executor
):
  private var maps: AssetMaps = AssetMaps(Map.empty, Map.empty, Map.empty, java.time.Instant.MIN)

  private val filename = s"manifest.${if net.minifiedAssets then "prod" else "dev"}.json"
  private val logger   = lila.log("assetManifest")

  def css(key: String): String             = maps.css.getOrElse(key, key)
  def hashed(path: String): Option[String] = maps.hashed.get(path)
  def jsAndDeps(keys: List[String]): List[String] = keys.flatMap { key =>
    maps.js.get(key).so(_.allModules)
  }.distinct
  def inlineJs(key: String): Option[String] = maps.js.get(key).flatMap(_.inlineJs)
  def lastUpdate: Instant                   = maps.modified

  def update(): Unit =
    if environment.mode.isProd || net.externalManifest then
      fetchManifestJson(filename).foreach:
        _.foreach: manifestJson =>
          maps = readMaps(manifestJson)
    else
      val pathname = environment.getFile(s"public/compiled/$filename").toPath
      try
        val current = Files.getLastModifiedTime(pathname).toInstant
        if current.isAfter(maps.modified)
        then maps = readMaps(Json.parse(Files.newInputStream(pathname)))
      catch
        case e: Throwable =>
          logger.warn(s"Error reading $pathname")

  private val jsKeyRe = """^(?!common\.)(\S+)\.([A-Z0-9]{8})\.js""".r

  private def closure(
      path: String,
      jsMap: Map[String, SplitAsset],
      visited: Set[String] = Set.empty
  ): List[String] =
    val k = path match
      case jsKeyRe(k, _) => k
      case _             => path
    jsMap.get(k) match
      case Some(asset) if !visited.contains(k) =>
        asset.imports.flatMap: importName =>
          importName :: closure(importName, jsMap, visited + path)
      case _ => Nil

  // throws an Exception if JsValue is not as expected
  private def readMaps(manifest: JsValue): AssetMaps =

    val splits: Map[String, SplitAsset] = (manifest \ "js")
      .as[JsObject]
      .value
      .map:
        case (k, JsString(h)) => (k, SplitAsset(s"$k.$h.js".some, Nil, None))
        case (k, value) =>
          val path = (value \ "hash")
            .asOpt[String]
            .map(h => s"$k.$h.js")
            .orElse((value \ "path").asOpt[String])
          val imports  = (value \ "imports").asOpt[List[String]].getOrElse(Nil)
          val inlineJs = (value \ "inline").asOpt[String]
          (k, SplitAsset(path, imports, inlineJs))
      .toMap

    val js: Map[String, SplitAsset] = splits.map: (key, asset) =>
      key -> asset.path.fold(asset)(path => asset.copy(imports = closure(path, splits).distinct))

    val css = (manifest \ "css")
      .as[JsObject]
      .value
      .map: (k, asset) =>
        val hash = (asset \ "hash").as[String]
        (k, s"$k.$hash.css")
      .toMap

    val hashed = (manifest \ "hashed")
      .as[JsObject]
      .value
      .map { (k, asset) =>
        val hash   = (asset \ "hash").as[String]
        val name   = k.substring(k.lastIndexOf('/') + 1)
        val extPos = name.lastIndexOf('.')
        val hashedName =
          if extPos < 0 then s"${name}.$hash"
          else s"${name.slice(0, extPos)}.$hash${name.substring(extPos)}"
        (k, s"hashed/$hashedName")
      }
      .toMap

    AssetMaps(js, css, hashed, nowInstant)

  private def fetchManifestJson(filename: String) =
    val resource = s"${net.assetBaseUrlInternal}/assets/compiled/$filename"
    ws.url(resource)
      .get()
      .map:
        case res if res.status == 200 => res.body[JsValue].some
        case res =>
          logger.error(s"${res.status} fetching $resource")
          none
      .recoverWith:
        case e: Exception =>
          logger.error(s"fetching $resource", e)
          fuccess(none)

  update()
