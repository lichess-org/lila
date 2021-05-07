package lila.study

import chess.format.FEN
import play.api.data._
import play.api.data.Forms._

import lila.common.Form.cleanNonEmptyText

object StudyForm {

  object importGame {

    lazy val form = Form(
      mapping(
        "gameId"      -> optional(nonEmptyText),
        "orientation" -> optional(nonEmptyText),
        "fen"         -> optional(lila.common.Form.fen.playable(strict = false)),
        "pgn"         -> optional(nonEmptyText),
        "variant"     -> optional(nonEmptyText),
        "as"          -> optional(nonEmptyText)
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        gameId: Option[String] = None,
        orientationStr: Option[String] = None,
        fen: Option[FEN] = None,
        pgnStr: Option[String] = None,
        variantStr: Option[String] = None,
        asStr: Option[String] = None
    ) {

      def orientation = orientationStr.flatMap(chess.Color.fromName) | chess.White

      def as: As =
        asStr match {
          case None | Some("study") => AsNewStudy
          case Some(studyId)        => AsChapterOf(Study.Id(studyId))
        }

      def toChapterData =
        ChapterMaker.Data(
          name = Chapter.Name(""),
          game = gameId,
          variant = variantStr,
          fen = fen,
          pgn = pgnStr,
          orientation = orientation.name,
          mode = ChapterMaker.Mode.Normal.key,
          initial = false
        )
    }

    sealed trait As
    case object AsNewStudy                    extends As
    case class AsChapterOf(studyId: Study.Id) extends As
  }

  object importPgn {

    lazy val form = Form(
      mapping(
        "name"        -> cleanNonEmptyText,
        "orientation" -> optional(nonEmptyText),
        "variant"     -> optional(nonEmptyText),
        "mode"        -> nonEmptyText.verifying(ChapterMaker.Mode(_).isDefined),
        "initial"     -> boolean,
        "sticky"      -> boolean,
        "pgn"         -> nonEmptyText
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        name: String,
        orientationStr: Option[String] = None,
        variantStr: Option[String] = None,
        mode: String,
        initial: Boolean,
        sticky: Boolean,
        pgn: String
    ) {

      def toChapterDatas = {
        val pgns = MultiPgn.split(pgn, max = 32).value
        pgns.zipWithIndex map { case (onePgn, index) =>
          ChapterMaker.Data(
            // only the first chapter can be named
            name = Chapter.Name((index == 0) ?? name),
            variant = variantStr,
            pgn = onePgn.some,
            orientation =
              if (pgns.sizeIs > 1) "auto"
              else (orientationStr.flatMap(chess.Color.fromName) | chess.White).name,
            mode = mode,
            initial = initial && index == 0
          )
        }
      }
    }
  }

  def topicsForm = Form(single("topics" -> text))

  def topicsForm(topics: StudyTopics) =
    Form(single("topics" -> text)) fill topics.value.map(_.value).mkString(", ")
}
