package lidraughts.study

import play.api.data._
import play.api.data.Forms._

object DataForm {

  object importGame {

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

  object importPdn {

    lazy val form = Form(mapping(
      "name" -> text,
      "orientation" -> optional(nonEmptyText),
      "variant" -> optional(nonEmptyText),
      "mode" -> nonEmptyText.verifying(ChapterMaker.Mode(_).isDefined),
      "initial" -> boolean,
      "sticky" -> boolean,
      "pdn" -> nonEmptyText
    )(Data.apply)(Data.unapply))

    case class Data(
        name: String,
        orientationStr: Option[String] = None,
        variantStr: Option[String] = None,
        mode: String,
        initial: Boolean,
        sticky: Boolean,
        pdn: String
    ) {

      def orientation = orientationStr.flatMap(draughts.Color.apply) | draughts.White

      def toChapterDatas = MultiPdn.split(pdn, max = 20).value.zipWithIndex map {
        case (onePdn, index) =>
          ChapterMaker.Data(
            // only the first chapter can be named
            name = Chapter.Name((index == 0) ?? name),
            variant = variantStr,
            pdn = onePdn.some,
            orientation = orientation.name,
            mode = mode,
            initial = initial && index == 0
          )
      }
    }
  }
}
