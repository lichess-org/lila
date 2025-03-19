package lila.study

import chess.format.Fen
import chess.format.pgn.PgnStr
import chess.variant.Variant
import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter

import lila.common.Form.{ cleanNonEmptyText, defaulting, formatter, into, given }

object StudyForm:

  private given Formatter[ChapterMaker.Mode] =
    formatter.stringOptionFormatter(_.key, ChapterMaker.Mode.apply)

  private given Formatter[ChapterMaker.Orientation] =
    formatter.stringFormatter(_.key, ChapterMaker.Orientation.apply)

  object importGame:

    lazy val form = Form(
      mapping(
        "gameId"      -> optional(of[GameId]),
        "orientation" -> optional(of[ChapterMaker.Orientation]),
        "fen"         -> optional(lila.common.Form.fen.playable(strict = false)),
        "pgn"         -> optional(nonEmptyText.into[PgnStr]),
        "variant"     -> optional(of[Variant]),
        "as"          -> optional(nonEmptyText)
      )(Data.apply)(unapply)
    )

    case class Data(
        gameId: Option[GameId] = None,
        orientation: Option[ChapterMaker.Orientation] = None,
        fen: Option[Fen.Full] = None,
        pgnStr: Option[PgnStr] = None,
        variant: Option[Variant] = None,
        asStr: Option[String] = None
    ):
      def as: As = asStr match
        case None | Some("study") => As.NewStudy
        case Some(studyId)        => As.ChapterOf(StudyId(studyId))

      def toChapterData = ChapterMaker.Data(
        name = StudyChapterName(""),
        game = gameId.map(_.value),
        variant = variant,
        fen = fen,
        pgn = pgnStr,
        orientation = orientation | ChapterMaker.Orientation.Auto,
        mode = ChapterMaker.Mode.Normal,
        initial = false
      )

    enum As:
      case NewStudy
      case ChapterOf(studyId: StudyId)

  object importPgn:

    lazy val form = Form(
      mapping(
        "name"          -> optional(cleanNonEmptyText(minLength = 1, maxLength = 100)),
        "orientation"   -> optional(of[ChapterMaker.Orientation]),
        "variant"       -> optional(of[Variant]),
        "mode"          -> defaulting(of[ChapterMaker.Mode], ChapterMaker.Mode.Normal),
        "initial"       -> boolean,
        "sticky"        -> boolean,
        "pgn"           -> nonEmptyText.into[PgnStr],
        "isDefaultName" -> boolean
      )(Data.apply)(unapply)
    )

    case class Data(
        name: Option[String],
        orientation: Option[ChapterMaker.Orientation] = None,
        variant: Option[Variant] = None,
        mode: ChapterMaker.Mode,
        initial: Boolean,
        sticky: Boolean,
        pgn: PgnStr,
        isDefaultName: Boolean
    ):

      def toChapterDatas: List[ChapterMaker.Data] =
        val pgns = MultiPgn.split(pgn, max = Study.maxChapters).value
        pgns.mapWithIndex: (onePgn, index) =>
          ChapterMaker.Data(
            // only the first chapter can be named
            name = StudyChapterName((index == 0).so(~name)),
            variant = variant,
            pgn = onePgn.some,
            orientation = orientation | ChapterMaker.Orientation.Auto,
            mode = mode,
            initial = initial && index == 0,
            isDefaultName = index > 0 || name.isEmpty || isDefaultName
          )

  def topicsForm = Form(single("topics" -> text))

  def topicsForm(topics: StudyTopics) =
    Form(single("topics" -> text)).fill(topics.value.mkString(","))
