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

  @volatile private var cachedAssets: Option[JsObject] = None

  def storeAsset(
      tpe: AssetType,
      name: String,
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ): Fu[Either[String, JsObject]] =
    FileIO
      .fromPath(file.ref.path)
      .runWith(FileIO.toPath(getFile(s"public/lifat/bots/${tpe}/$name").toPath))
      .map(res => Right(updateAssets))
      .recover:
        case e: Exception => Left(s"Exception: ${e.getMessage}")

  def assetKeys: JsObject = cachedAssets.getOrElse(updateAssets)

  private def listFiles(tpe: String, ext: String): List[String] =
    val path = getFile(s"public/lifat/bots/${tpe}")
    if !path.exists() then
      NioFiles.createDirectories(path.toPath)
      Nil
    else
      path
        .listFiles()
        .toList
        .map(_.getName)
        .filter(_.endsWith(s".${ext}"))

  def updateAssets: JsObject =
    val newAssets = Json.obj(
      "image" -> listFiles("image", "webp"),
      "net"   -> listFiles("net", "pb"),
      "sound" -> listFiles("sound", "mp3"),
      "book" -> listFiles("book", "png")
        .map(_.dropRight(4))
    )
    cachedAssets = newAssets.some
    newAssets
