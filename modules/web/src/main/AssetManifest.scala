package lila.web

import java.nio.file.Files
import play.api.libs.json.{ JsObject, JsValue, Json, JsString }

import lila.common.config.GetRelativeFile

final class AssetManifest(getFile: GetRelativeFile):

  private var maps: AssetMaps = AssetMaps(Map.empty, Map.empty, Map.empty, java.time.Instant.MIN)

  def css(key: String): String = maps.css.getOrElse(key, key)
  def hashed(path: String): Option[String] = maps.hashed.get(path)
  def js(key: String): Option[String] = maps.jsGet(key).flatMap(_.path)
  def jsAndDeps(keys: List[String]): List[String] = keys.flatMap { key =>
    maps.jsGet(key).so(_.allModules)
  }.distinct
  def inlineJs(key: String): Option[String] = maps.js.get(key).flatMap(_.inlineJs)
  def lastUpdate: Instant = maps.modified

  def update(): Unit =
    val pathname = getFile.exec("public/compiled/manifest.json").toPath
    try
      val current = Files.getLastModifiedTime(pathname).toInstant
      if current.isAfter(maps.modified) then
        maps = readMaps(Json.parse(Files.newInputStream(pathname)))
        lila.common.Bus.pub(AssetManifestUpdate)
    catch case e: Throwable => lila.log("assetManifest").warn(s"Error reading $pathname", e)

  private val jsKeyRe = """^(?!lib\.)(\S+)\.([A-Z0-9]{8})\.js""".r

  private def closure(
      path: String,
      jsMap: Map[String, SplitAsset],
      visited: Set[String] = Set.empty
  ): List[String] =
    val key = path match
      case jsKeyRe(k, _) => k
      case _ => path
    jsMap.get(key) match
      case Some(info) if !visited.contains(key) =>
        info.imports.flatMap: importName =>
          importName :: closure(importName, jsMap, visited + path)
      case _ => Nil

  // throws an Exception if JsValue is not as expected
  private def readMaps(manifest: JsValue): AssetMaps =

    val splits: Map[String, SplitAsset] = (manifest \ "js")
      .as[JsObject]
      .value
      .map:
        case (key, JsString(hash)) =>
          (key, SplitAsset(s"$key.$hash.js".some, Nil, None))
        case (key, info) =>
          val path = (info \ "hash")
            .asOpt[String]
            .map(hash => s"$key.$hash.js")
            .orElse((info \ "path").asOpt[String])
          val imports = (info \ "imports").asOpt[List[String]].getOrElse(Nil)
          val inlineJs = (info \ "inline").asOpt[String]
          (key, SplitAsset(path, imports, inlineJs))
      .toMap

    val js: Map[String, SplitAsset] = splits.map: (key, asset) =>
      key -> asset.path.fold(asset)(path => asset.copy(imports = closure(path, splits).distinct))

    val css = (manifest \ "css")
      .as[JsObject]
      .value
      .map:
        case (key, JsString(hash)) => (key, s"$key.$hash.css")
        case (key, info) => (key, s"$key.${(info \ "hash").as[String]}.css")
      .toMap

    val hashed = (manifest \ "hashed")
      .as[JsObject]
      .value
      .map: (key, info) =>
        val hash = info.asOpt[String].getOrElse((info \ "hash").as[String])
        val name = key.substring(key.lastIndexOf('/') + 1)
        val extPos = name.lastIndexOf('.')
        val hashedName =
          if extPos < 0 then s"${name}.$hash"
          else s"${name.slice(0, extPos)}.$hash${name.substring(extPos)}"
        (key, s"hashed/$hashedName")
      .toMap

    AssetMaps(js, css, hashed, nowInstant)

  update()

  private case class SplitAsset(path: Option[String], imports: List[String], inlineJs: Option[String]):
    val allModules = path.toList ++ imports

  private case class AssetMaps(
      js: Map[String, SplitAsset],
      css: Map[String, String],
      hashed: Map[String, String],
      modified: Instant
  ):
    def jsGet(key: String): Option[SplitAsset] =
      js.get(key)
        .orElse:
          if !key.startsWith("i18n/") then none
          else
            val dot = key.lastIndexOf('.')
            if dot > 0 then js.get(key.slice(0, dot) + ".en-GB")
            else none

private case object AssetManifestUpdate
