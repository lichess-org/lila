package lila.local

import java.nio.file.{ Files as NioFiles, Paths }
import play.api.libs.json.*
import play.api.libs.Files
import play.api.mvc.*
import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString

// this stuff is for bot devs

final private class LocalApi(repo: LocalRepo, getFile: (String => java.io.File))(using
    Executor,
    akka.stream.Materializer
):

  @volatile private var cachedAssets: Option[JsObject] = None

  def storeAsset(
      tpe: "image" | "net" | "book" | "sound",
      name: String,
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ): Fu[Either[String, JsObject]] =
    FileIO
      .fromPath(file.ref.path)
      .runWith(FileIO.toPath(getFile(s"public/lifat/bots/${tpe}s/$name").toPath))
      .map: result =>
        if result.wasSuccessful then Right(updateAssets)
        else Left(s"Error uploading asset $tpe $name")
      .recover:
        case e: Exception => Left(s"Exception: ${e.getMessage}")

  def assets: JsObject = cachedAssets.getOrElse(updateAssets)

  def updateAssets: JsObject =
    val newAssets = Json.obj(
      "image" -> getFile("public/lifat/bots/images").listFiles().toList.map(_.getName),
      "net"   -> getFile("public/lifat/bots/nets").listFiles().toList.map(_.getName),
      "book" -> getFile("public/lifat/bots/books")
        .listFiles()
        .toList
        .map(_.getName)
        .filter(_.endsWith(".png"))
        .map(_.dropRight(4)),
      "sound" -> getFile("public/lifat/bots/sounds").listFiles().toList.map(_.getName)
    )
    cachedAssets = newAssets.some
    newAssets
