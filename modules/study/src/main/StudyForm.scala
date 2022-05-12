package lila.study

import chess.format.FEN
import chess.variant.Variant
import play.api.data._
import play.api.data.Forms._

import lila.common.Form.{ cleanNonEmptyText, formatter, variantFormat }

object StudyForm {

  implicit private val modeFormat =
    formatter.stringOptionFormatter[ChapterMaker.Mode](_.key, ChapterMaker.Mode.apply)

  implicit private val orientationFormat =
    formatter.stringFormatter[ChapterMaker.Orientation](_.key, ChapterMaker.Orientation.apply)

  object importGame {

    lazy val form = Form(
      mapping(
        "gameId"      -> optional(nonEmptyText),
        "orientation" -> optional(of[ChapterMaker.Orientation]),
        "fen"         -> optional(lila.common.Form.fen.playable(strict = false)),
        "pgn"         -> optional(nonEmptyText),
        "variant"     -> optional(of[Variant]),
        "as"          -> optional(nonEmptyText)
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        gameId: Option[String] = None,
        orientation: Option[ChapterMaker.Orientation] = None,
        fen: Option[FEN] = None,
        pgnStr: Option[String] = None,
        variant: Option[Variant] = None,
        asStr: Option[String] = None
    ) {
      def as: As =
        asStr match {
          case None | Some("study") => AsNewStudy
          case Some(studyId)        => AsChapterOf(Study.Id(studyId))
        }

      def toChapterData =
        ChapterMaker.Data(
          name = Chapter.Name(""),
          game = gameId,
          variant = variant,
          fen = fen,
          pgn = pgnStr,
          orientation = orientation | ChapterMaker.Orientation.Auto,
          mode = ChapterMaker.Mode.Normal,
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
        "name"          -> cleanNonEmptyText,
        "orientation"   -> optional(of[ChapterMaker.Orientation]),
        "variant"       -> optional(of[Variant]),
        "mode"          -> of[ChapterMaker.Mode],
        "initial"       -> boolean,
        "sticky"        -> boolean,
        "pgn"           -> nonEmptyText,
        "isDefaultName" -> boolean
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        name: String,
        orientation: Option[ChapterMaker.Orientation] = None,
        variant: Option[Variant] = None,
        mode: ChapterMaker.Mode,
        initial: Boolean,
        sticky: Boolean,
        pgn: String,
        isDefaultName: Boolean
    ) {

      def toChapterDatas = {
        val pgns = MultiPgn.split(pgn, max = 32).value
        pgns.zipWithIndex map { case (onePgn, index) =>
          ChapterMaker.Data(
            // only the first chapter can be named
            name = Chapter.Name((index == 0) ?? name),
            variant = variant,
            pgn = onePgn.some,
            orientation = orientation | ChapterMaker.Orientation.Auto,
            mode = mode,
            initial = initial && index == 0,
            isDefaultName = index > 0 || isDefaultName
          )
        }
      }
    }
  }

  def topicsForm = Form(single("topics" -> text))

  def topicsForm(topics: StudyTopics) =
    Form(single("topics" -> text)) fill topics.value.map(_.value).mkString(",")
}
