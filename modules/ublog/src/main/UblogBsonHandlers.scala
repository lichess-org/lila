package lila.ublog

import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.api.bson.*
import scala.util.{ Success, Try }

import lila.common.Iso
import lila.db.dsl.{ *, given }

private object UblogBsonHandlers:

  import lila.memo.PicfitImage.*
  import UblogPost.{ LightPost, Likes, PreviewPost, Recorded, Views }

  given BSONHandler[UblogBlog.Id] = tryHandler(
    { case BSONString(v) => UblogBlog.Id(v).toTry(s"Invalid blog id $v") },
    id => BSONString(id.full)
  )
  given BSONDocumentHandler[UblogBlog] = Macros.handler

  given BSONHandler[Lang]                = stringAnyValHandler(_.code, Lang.apply)
  given BSONDocumentHandler[Recorded]    = Macros.handler
  given BSONDocumentHandler[UblogImage]  = Macros.handler
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
