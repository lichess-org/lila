package lila.web

import akka.stream.scaladsl.{ Source, Sink }
import akka.util.ByteString
import play.api.mvc.MultipartFormData
import play.api.libs.streams.Accumulator
import play.api.mvc.{ BodyParser, PlayBodyParsers }
import play.core.parsers.Multipart
import java.security.MessageDigest

import lila.memo.{ PicfitApi, HashedSource }

object HashedMultiPart:
  private val maxLength: Long = PicfitApi.uploadMaxMb * 1024 * 1024

  def apply(
      parse: PlayBodyParsers,
      maxLength: Long = maxLength
  )(using Executor): BodyParser[MultipartFormData[HashedSource]] =
    parse.multipartFormData(
      filePartHandler = hashedFilePartHandler(),
      maxLength = maxLength,
      allowEmptyFiles = false
    )

  private def hashedFilePartHandler()(using Executor): Multipart.FilePartHandler[HashedSource] =
    info =>
      val digest = MessageDigest.getInstance("SHA-256")
      val builder = Vector.newBuilder[ByteString]
      val sink: Sink[ByteString, Future[Long]] =
        Sink.fold[Long, ByteString](0L): (bytesSoFar, chunk) =>
          digest.update(chunk.asByteBuffer)
          builder += chunk
          bytesSoFar + chunk.length

      Accumulator(sink).map: fileSize =>
        MultipartFormData.FilePart(
          key = info.partName,
          filename = info.fileName,
          contentType = info.contentType,
          ref = HashedSource(Source(builder.result()), digest.digest()),
          fileSize = fileSize,
          dispositionType = info.dispositionType
        )
