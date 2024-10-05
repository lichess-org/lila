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
      tpe: "image" | "book" | "sound",
      name: String,
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ): Fu[Either[String, JsObject]] =
    FileIO
      .fromPath(file.ref.path)
      .runWith(FileIO.toPath(getFile(s"public/lifat/bots/${tpe}/$name").toPath))
      .map: result =>
        if result.wasSuccessful then Right(updateAssets)
        else Left(s"Error uploading asset $tpe $name")
      .recover:
        case e: Exception => Left(s"Exception: ${e.getMessage}")

  def assetKeys: JsObject = cachedAssets.getOrElse(updateAssets)

  private def listFiles(tpe: String): List[String] =
    val path = getFile(s"public/lifat/bots/${tpe}")
    if !path.exists() then
      NioFiles.createDirectories(path.toPath)
      Nil
    else
      path
        .listFiles()
        .toList
        .map(_.getName)

  def updateAssets: JsObject =
    val newAssets = Json.obj(
      "image" -> listFiles("image"),
      "net"   -> listFiles("net"),
      "sound" -> listFiles("sound"),
      "book" -> listFiles("book")
        .filter(_.endsWith(".bin"))
        .map(_.dropRight(4))
    )
    cachedAssets = newAssets.some
    newAssets
