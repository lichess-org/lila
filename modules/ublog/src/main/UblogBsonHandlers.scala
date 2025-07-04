package lila.ublog

import scala.util.Success
import play.api.i18n.Lang
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private object UblogBsonHandlers:

  import UblogPost.{ LightPost, PreviewPost, Recorded, Featured }
  import UblogAutomod.Assessment
  import lila.core.ublog.Quality

  given BSONHandler[UblogBlog.Id] = tryHandler(
    { case BSONString(v) => UblogBlog.Id(v).toTry(s"Invalid blog id $v") },
    id => BSONString(id.full)
  )
  given BSONDocumentHandler[UblogBlog]    = Macros.handler
  given BSONHandler[Lang]                 = langByCodeHandler
  given BSONDocumentHandler[Recorded]     = Macros.handler
  given BSONDocumentHandler[Featured]     = Macros.handler
  given BSONDocumentHandler[UblogImage]   = Macros.handler
  given BSONDocumentHandler[UblogPost]    = Macros.handler
  given BSONDocumentHandler[LightPost]    = Macros.handler
  given BSONDocumentHandler[PreviewPost]  = Macros.handler
  given BSONDocumentHandler[UblogSimilar] = Macros.handler
  given BSONHandler[Quality]              = tryHandler(
    v => v.asOpt[Int].flatMap(Quality.values.lift).toTry(s"bad quality $v"),
    quality => BSONInteger(quality.ordinal)
  )
  given BSONDocumentHandler[UblogAutomod.Assessment] = Macros.handler

  val postProjection        = $doc("likers" -> false)
  val lightPostProjection   = $doc("title" -> true)
  val previewPostProjection =
    $doc(
      "blog"     -> true,
      "title"    -> true,
      "intro"    -> true,
      "image"    -> true,
      "created"  -> true,
      "lived"    -> true,
      "featured" -> true,
      "topics"   -> true,
      "sticky"   -> true
    )

  val userLiveSort = $doc("sticky" -> -1, "lived.at" -> -1)
