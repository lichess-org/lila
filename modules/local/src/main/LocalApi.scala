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
      name: String, // already url encoded
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

  private def listAssets(tpe: String): List[String] =
    getFile(s"public/lifat/bots/${tpe}s")
      .listFiles()
      .toList
      .flatMap(f => lila.common.String.decodeUriPath(f.getName))

  def updateAssets: JsObject =
    val newAssets = Json.obj(
      "image" -> listAssets("image"),
      "net"   -> listAssets("net"),
      "sound" -> listAssets("sound"),
      "book" -> listAssets("book")
        .filter(_.endsWith(".bin"))
        .map(_.dropRight(4))
    )
    cachedAssets = newAssets.some
    newAssets
