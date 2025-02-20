package lila.local

import java.nio.file.{ Files as NioFiles, Paths }
import play.api.libs.json.*
import play.api.libs.Files
import play.api.mvc.*
import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString

// this stuff is for bot devs

final private class LocalApi(config: LocalConfig, repo: LocalRepo, getFile: (String => java.io.File))(using
    Executor,
    akka.stream.Materializer
):

  type LocalAssets = Map[String, List[String]]

  @volatile private var cachedAssets: Option[(LocalAssets, JsonStr)] = None

  def storeAsset(
      tpe: AssetType,
      name: String,
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ): Fu[Either[String, (LocalAssets, JsonStr)]] =
    FileIO
      .fromPath(file.ref.path)
      .runWith(FileIO.toPath(getFile(s"${config.assetPath}/${tpe}/$name").toPath))
      .map(res => Right(updateAssets))
      .recover:
        case e: Exception => Left(s"Exception: ${e.getMessage}")

  def getBoth: (LocalAssets, JsonStr) = cachedAssets.getOrElse(updateAssets)
  def getAssets: LocalAssets          = getBoth._1
  def getJson: JsonStr                = getBoth._2

  def devGetAssets: Fu[JsObject] =
    repo.getAssets.map: m =>
      Json.toJsObject:
        getAssets.map: (categ, keys) =>
          categ -> (for
            key  <- keys
            name <- m.get(key)
          yield Json.obj("key" -> key, "name" -> name))

  private def listFiles(tpe: String, ext: String): List[String] =
    val path = getFile(s"${config.assetPath}/${tpe}")
    if !path.exists() then
      NioFiles.createDirectories(path.toPath)
      Nil
    else
      path
        .listFiles(_.getName.endsWith(s".${ext}"))
        .toList
        .map(_.getName)

  def updateAssets: (LocalAssets, JsonStr) =
    val data = Map(
      "image" -> listFiles("image", "webp"),
      "net"   -> listFiles("net", "pb"),
      "sound" -> listFiles("sound", "mp3"),
      "book"  -> listFiles("book", "png").map(_.dropRight(4))
    )
    val newAssets = (data, JsonStr(Json.stringify(Json.toJson(data))))
    cachedAssets = newAssets.some
    newAssets
