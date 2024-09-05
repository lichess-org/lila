package lila.web

import play.api.Environment
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import java.nio.file.Files

import lila.core.config.NetConfig

case class SplitAsset(name: String, imports: List[String])
case class AssetMaps(
    js: Map[String, SplitAsset],
    css: Map[String, String],
    hashed: Map[String, String],
    modified: Instant
)

final class AssetManifest(environment: Environment, net: NetConfig)(using ws: StandaloneWSClient)(using
    Executor
) extends lila.ui.AssetManifest:
  private var maps: AssetMaps = AssetMaps(Map.empty, Map.empty, Map.empty, java.time.Instant.MIN)

  private val filename = s"manifest.${if net.minifiedAssets then "prod" else "dev"}.json"
  private val logger   = lila.log("assetManifest")

  def js(key: String): Option[SplitAsset]    = maps.js.get(key)
  def css(key: String): Option[String]       = maps.css.get(key)
  def hashed(path: String): Option[String]   = maps.hashed.get(path)
  def deps(keys: List[String]): List[String] = keys.flatMap { key => js(key).so(_.imports) }.distinct
  def lastUpdate: Instant                    = maps.modified

  def jsName(key: String): String = js(key).fold(key)(_.name)

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

  private val keyRe = """^(?!common\.)(\S+)\.([A-Z0-9]{8})\.(?:js|css)""".r
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

  // throws an Exception if JsValue is not as expected
  private def readMaps(manifest: JsValue): AssetMaps =

    val splits: Map[String, SplitAsset] = (manifest \ "js")
      .as[JsObject]
      .value
      .map { (k, value) =>
        val name    = (value \ "hash").asOpt[String].fold(s"$k.js")(h => s"$k.$h.js")
        val imports = (value \ "imports").asOpt[List[String]].getOrElse(Nil)
        (k, SplitAsset(name, imports))
      }
      .toMap

    val js = splits.view.mapValues: asset =>
      if asset.imports.nonEmpty
      then asset.copy(imports = closure(asset.name, splits).distinct)
      else asset

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
        val extPos = name.indexOf('.')
        val hashedName =
          if extPos < 0 then s"${name}.$hash"
          else s"${name.slice(0, extPos)}.$hash${name.substring(extPos)}"
        (k, s"hashed/$hashedName")
      }
      .toMap

    AssetMaps(js.toMap, css, hashed, nowInstant)

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
