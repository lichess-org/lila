package lila.ublog

import scala.util.{ Try, Success }
import play.api.i18n.Lang
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private object UblogBsonHandlers:

  import UblogPost.{ LightPost, PreviewPost, Recorded }
  import UblogAutomod.Result

  given BSONHandler[UblogBlog.Id] = tryHandler(
    { case BSONString(v) => UblogBlog.Id(v).toTry(s"Invalid blog id $v") },
    id => BSONString(id.full)
  )
  given BSONDocumentHandler[Result] = new BSONDocumentHandler[Result]:
    def writeTry(result: Result) = Success:
      val quality = result.classification match
        case "spam"  => 0
        case "weak"  => 1
        case "great" => 3
        case _       => 2
      BSONDocument(
        "quality"    -> quality,
        "flagged"    -> result.flagged,
        "commercial" -> result.commercial,
        "evergreen"  -> result.evergreen,
        "hash"       -> result.hash
      )
    def readDocument(doc: BSONDocument) = Try[Result]:
      val fromQuality = doc.getAsOpt[Int]("quality") match
        case Some(0) => "spam"
        case Some(1) => "weak"
        case Some(3) => "great"
        case _       => "good"
      Result(
        classification = doc.getAsOpt[String]("classification").getOrElse(fromQuality),
        flagged = doc.getAsOpt[String]("flagged"),
        commercial = doc.getAsOpt[String]("commercial"),
        offtopic = doc.getAsOpt[String]("offtopic"),
        evergreen = doc.getAsOpt[Boolean]("evergreen"),
        hash = doc.getAsOpt[String]("hash")
      )
  given BSONDocumentHandler[UblogBlog] = Macros.handler

  given BSONHandler[Lang]                 = langByCodeHandler
  given BSONDocumentHandler[Recorded]     = Macros.handler
  given BSONDocumentHandler[UblogImage]   = Macros.handler
  given BSONDocumentHandler[UblogPost]    = Macros.handler
  given BSONDocumentHandler[LightPost]    = Macros.handler
  given BSONDocumentHandler[PreviewPost]  = Macros.handler
  given BSONDocumentHandler[UblogSimilar] = Macros.handler

  val postProjection        = $doc("likers" -> false)
  val lightPostProjection   = $doc("title" -> true)
  val previewPostProjection =
    $doc(
      "blog"    -> true,
      "title"   -> true,
      "intro"   -> true,
      "image"   -> true,
      "created" -> true,
      "lived"   -> true,
      "topics"  -> true,
      "sticky"  -> true
    )

  val userLiveSort = $doc("sticky" -> -1, "lived.at" -> -1)
