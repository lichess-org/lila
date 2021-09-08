package lila.coach

import scala.concurrent.duration._
import akka.stream.scaladsl._
import play.api.libs.Files
import play.api.mvc.MultipartFormData
import reactivemongo.akkastream.cursorProducer

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.Pov
import lila.memo.PicfitApi
import akka.util.ByteString

final private class CoachPictureMigration(
    coll: Coll,
    picfitApi: PicfitApi,
    imageRepo: lila.db.ImageRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import BsonHandlers._

  def apply() =
    coll
      .find($doc("picturePath" $exists true, "picture" $exists false))
      .sort($sort desc "user.seenAt")
      .cursor[Bdoc]()
      .documentSource()
      .throttle(20, 1 second)
      .mapAsync(1)(migrate)
      .via(lila.common.LilaStream.logRate[Unit]("coach.picfit.migration")(logger))
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect { count =>
        println(s"$count coaches migrated")
      }
      .void

  private def migrate(s: Bdoc): Funit = {
    val coachId = s string "_id" err "missing id"
    val picPath = s string "picturePath" err "missing picturePath"
    val picId   = picPath.split('/').headOption err "missing picId"
    imageRepo.fetch(picId) flatMap {
      case None =>
        println(s"Missing picture for coach $coachId pic $picPath")
        funit
      case Some(pic) =>
        val part: MultipartFormData.FilePart[Source[ByteString, _]] = MultipartFormData.FilePart(
          key = "data",
          filename = pic.name,
          contentType = "image/jpeg".some,
          ref = Source(ByteString.fromArray(pic.data) :: Nil),
          fileSize = pic.size
        )
        picfitApi.uploadSource(s"coach:$coachId", part, coachId, monitor = false) flatMap { picture =>
          coll.updateField($id(coachId), "picture", picture.id.value).void
        }
    } recover { case e: Exception =>
      if (!e.getMessage.contains("Invalid file type: "))
        logger.error(s"Can't migrate coach $coachId", e)
    }
  }
}
