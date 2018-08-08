package lidraughts.study

import play.api.data._
import play.api.data.Forms._

object DataForm {

  lazy val form = Form(mapping(
    "gameId" -> optional(nonEmptyText),
    "orientation" -> optional(nonEmptyText),
    "fen" -> optional(nonEmptyText),
    "pdn" -> optional(nonEmptyText),
    "variant" -> optional(nonEmptyText),
    "as" -> optional(nonEmptyText)
  )(Data.apply)(Data.unapply))

  case class Data(
      gameId: Option[String] = None,
      orientationStr: Option[String] = None,
      fenStr: Option[String] = None,
      pdnStr: Option[String] = None,
      variantStr: Option[String] = None,
      asStr: Option[String] = None
  ) {

    def orientation = orientationStr.flatMap(draughts.Color.apply) | draughts.White

    def as: As = asStr match {
      case None | Some("study") => AsNewStudy
      case Some(studyId) => AsChapterOf(Study.Id(studyId))
    }

    def toChapterData = ChapterMaker.Data(
      name = Chapter.Name(""),
      game = gameId,
      variant = variantStr,
      fen = fenStr,
      pdn = pdnStr,
      orientation = orientation.name,
      mode = ChapterMaker.Mode.Normal.key,
      initial = false
    )
  }

  sealed trait As
  case object AsNewStudy extends As
  case class AsChapterOf(studyId: Study.Id) extends As
}
