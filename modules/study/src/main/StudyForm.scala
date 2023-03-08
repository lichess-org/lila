package lila.study

import play.api.data._
import play.api.data.Forms._

import lila.common.Form.cleanNonEmptyText
import shogi.format.forsyth.Sfen

object StudyForm {

  object importFree {

    lazy val form = Form(
      mapping(
        "notation" -> nonEmptyText
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        notation: String
    )

  }

  object postGameStudy {

    lazy val form = Form(
      mapping(
        "gameId"      -> nonEmptyText,
        "orientation" -> optional(nonEmptyText),
        "invited"     -> optional(nonEmptyText)
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        gameId: String,
        orientationStr: Option[String],
        invitedUsername: Option[String]
    ) {
      def orientation = orientationStr.flatMap(shogi.Color.fromName) | shogi.Sente
    }

  }

  object importGame {

    lazy val form = Form(
      mapping(
        "gameId"      -> optional(nonEmptyText),
        "orientation" -> optional(nonEmptyText),
        "sfen"        -> optional(lila.common.Form.sfen.clean),
        "notation"    -> optional(nonEmptyText),
        "variant"     -> optional(nonEmptyText),
        "as"          -> optional(nonEmptyText)
      )(Data.apply)(Data.unapply)
        .verifying("Invalid SFEN", _.validSfen)
    )

    case class Data(
        gameId: Option[String] = None,
        orientationStr: Option[String] = None,
        sfen: Option[Sfen] = None,
        notationStr: Option[String] = None,
        variantStr: Option[String] = None,
        asStr: Option[String] = None
    ) {

      def orientation = orientationStr.flatMap(shogi.Color.fromName) | shogi.Sente

      def variant = (variantStr flatMap shogi.variant.Variant.apply) | shogi.variant.Standard

      def validSfen =
        sfen.fold(true) { sf =>
          sf.toSituation(variant).exists(_.playable(strict = false, withImpasse = false))
        }

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
          sfen = sfen,
          notation = notationStr,
          orientation = orientation.name,
          mode = ChapterMaker.Mode.Normal.key,
          initial = false
        )
    }

    sealed trait As
    case object AsNewStudy                    extends As
    case class AsChapterOf(studyId: Study.Id) extends As
  }

  object importNotation {

    lazy val form = Form(
      mapping(
        "name"        -> cleanNonEmptyText,
        "orientation" -> optional(nonEmptyText),
        "variant"     -> optional(nonEmptyText),
        "mode"        -> nonEmptyText.verifying(ChapterMaker.Mode(_).isDefined),
        "initial"     -> boolean,
        "sticky"      -> boolean,
        "notation"    -> nonEmptyText
      )(Data.apply)(Data.unapply)
    )

    case class Data(
        name: String,
        orientationStr: Option[String] = None,
        variantStr: Option[String] = None,
        mode: String,
        initial: Boolean,
        sticky: Boolean,
        notation: String
    ) {

      def orientation = orientationStr.flatMap(shogi.Color.fromName) | shogi.Sente

      def toChapterDatas =
        MultiNotation.split(notation, max = 20).value.zipWithIndex map { case (oneNotation, index) =>
          ChapterMaker.Data(
            // only the first chapter can be named
            name = Chapter.Name((index == 0) ?? name),
            variant = variantStr,
            notation = oneNotation.some,
            orientation = orientation.name,
            mode = mode,
            initial = initial && index == 0
          )
        }
    }
  }

  def topicsForm = Form(single("topics" -> text))

  def topicsForm(topics: StudyTopics) =
    Form(single("topics" -> text)) fill topics.value.map(_.value).mkString(", ")
}
