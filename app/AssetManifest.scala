package lila.app

import play.api.{ Environment, Mode }
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.json.{ JsObject, Json, JsValue, JsString }
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import lila.core.config.NetConfig

case class SplitAsset(name: String, imports: List[String])
case class AssetMaps(js: Map[String, SplitAsset], css: Map[String, String])

final class AssetManifest(environment: Environment, net: NetConfig)(using ws: StandaloneWSClient)(using
    Executor
):

  private var lastModified: Instant = Instant.MIN
  private var maps: AssetMaps       = AssetMaps(Map.empty, Map.empty)
  private val defaultFilename       = s"manifest.${if net.minifiedAssets then "prod" else "dev"}.json"
  private val keyRe                 = """^(?!common\.)(\S+)\.([A-Z0-9]{8})\.(?:js|css)""".r

  def js(key: String): Option[SplitAsset]    = maps.js.get(key)
  def css(key: String): Option[String]       = maps.css.get(key)
  def deps(keys: List[String]): List[String] = keys.flatMap { key => js(key).so(_.imports) }.distinct
  def lastUpdate: Instant                    = lastModified

  def update(filename: String = defaultFilename): Unit =
    if environment.mode == Mode.Prod || net.externalManifest then
      fetchManifestJson(filename).foreach:
        case Some(manifestJson) =>
          maps = readMaps(manifestJson)
          lastModified = Instant.now
        case _ => ()
    else
      val pathname = environment.getFile(s"public/compiled/$filename").toPath
      try
        val current = Files.getLastModifiedTime(pathname).toInstant
        if current.isAfter(lastModified) then
          maps = readMaps(Json.parse(Files.newInputStream(pathname)))
          lastModified = current
      catch
        case _: Throwable =>
          lila.log("assetManifest").warn(s"Error reading $pathname")

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

  private def readMaps(manifest: JsValue) =
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

  private def fetchManifestJson(filename: String) =
    val resource = s"${net.assetBaseUrlInternal}/assets/compiled/$filename"
    ws.url(resource)
      .get()
      .map:
        case res if res.status == 200 => res.body[JsValue].some
        case res =>
          lila.log("assetManifest").warn(s"${res.status} fetching $resource")
          none
      .recoverWith:
        case e: Exception =>
          lila.log("assetManifest").warn(s"fetching $resource", e)
          fuccess(none)
