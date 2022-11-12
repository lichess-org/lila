package lila.ublog

import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.api.bson.*
import scala.util.{ Success, Try }

import lila.common.Iso
import lila.db.dsl.{ *, given }

private object UblogBsonHandlers:

  import lila.memo.PicfitImage.*
  import UblogPost.{ LightPost, Likes, PreviewPost, Rank, Recorded, Views }

  given BSONHandler[UblogBlog.Id] = tryHandler(
    { case BSONString(v) => UblogBlog.Id(v).toTry(s"Invalid blog id $v") },
    id => BSONString(id.full)
  )
  given BSONDocumentHandler[UblogBlog] = Macros.handler

  given postIdHandler: BSONHandler[UblogPost.Id] = stringAnyValHandler(_.value, UblogPost.Id)
  given topicHandler: BSONHandler[UblogTopic]    = stringAnyValHandler[UblogTopic](_.value, UblogTopic)
  given BSONReader[List[UblogTopic]] =
    summon[BSONReader[List[UblogTopic]]].afterRead(_.filter(t => UblogTopic.exists(t.value)))
  given BSONHandler[Lang]                = stringAnyValHandler[Lang](_.code, Lang)
  given BSONDocumentHandler[Recorded]    = Macros.handler
  given BSONDocumentHandler[UblogImage]  = Macros.handler
  given BSONHandler[Likes]               = intAnyValHandler[Likes](_.value, Likes)
  given BSONHandler[Views]               = intAnyValHandler[Views](_.value, Views)
  given BSONHandler[Rank]                = dateIsoHandler[Rank](Iso[DateTime, Rank](Rank, _.value))
  given BSONDocumentHandler[UblogPost]   = Macros.handler
  given BSONDocumentHandler[LightPost]   = Macros.handler
  given BSONDocumentHandler[PreviewPost] = Macros.handler

  val lightPostProjection = $doc("title" -> true)
  val previewPostProjection =
    $doc(
      "blog"    -> true,
      "title"   -> true,
      "intro"   -> true,
      "image"   -> true,
      "created" -> true,
      "lived"   -> true,
      "topics"  -> true
    )
