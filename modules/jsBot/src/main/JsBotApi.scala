package lila.jsBot

import java.nio.file.Files as NioFiles
import play.api.libs.json.*
import play.api.libs.Files
import play.api.mvc.*
import akka.stream.scaladsl.FileIO
import lila.common.config.GetRelativeFile

// this stuff is for bot devs

final private class JsBotApi(config: JsBotConfig, repo: JsBotRepo, getFile: GetRelativeFile)(using
    Executor,
    akka.stream.Materializer
):

  type AlmostFileName = String
  type JsBotAssets = Map[AssetType, List[AlmostFileName]]

  @volatile private var cachedAssets: Option[(JsBotAssets, JsonStr)] = None

  def storeAsset(
      tpe: AssetType,
      fileName: String,
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ): Fu[Either[String, (JsBotAssets, JsonStr)]] =
    FileIO
      .fromPath(file.ref.path)
      .runWith(FileIO.toPath(getFile.exec(s"${config.assetPath}/${tpe}/$fileName").toPath))
      .map(_ => Right(updateAssets))
      .recover:
        case e: Exception => Left(s"Exception: ${e.getMessage}")

  def getBoth: (JsBotAssets, JsonStr) = cachedAssets.getOrElse(updateAssets)
  def getAssets: JsBotAssets = getBoth._1
  def getJson: JsonStr = getBoth._2

  def devGetAssets: Fu[JsObject] =
    repo.getAssets.map: m =>
      Json.toJsObject:
        getAssets.map: (categ, keys) =>
          categ -> (for
            key <- keys
            name <- m.get(key)
          yield Json.obj("key" -> key, "name" -> name))

  private def listFiles(tpe: AssetType, ext: String): List[AlmostFileName] =
    val path = getFile.exec(s"${config.assetPath}/${tpe}")
    if !path.exists() then
      NioFiles.createDirectories(path.toPath)
      Nil
    else
      path
        .listFiles(_.getName.endsWith(s".${ext}"))
        .toList
        .map(_.getName)

  def updateAssets: (JsBotAssets, JsonStr) =
    val data: JsBotAssets = Map(
      "image" -> listFiles("image", "webp"),
      "net" -> listFiles("net", "pb"),
      "sound" -> listFiles("sound", "mp3"),
      "book" -> listFiles("book", "png").map(_.dropRight(4))
    )
    val newAssets = (data, JsonStr(Json.stringify(Json.toJson(data))))
    cachedAssets = newAssets.some
    newAssets
