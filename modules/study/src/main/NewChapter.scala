package lila.study

import chess.format.pgn.{ Glyph, Tags }
import chess.format.UciPath
import chess.opening.{ Opening, OpeningDb }
import chess.variant.Variant
import chess.{ Ply, Centis, Color, Outcome }
import ornicar.scalalib.ThreadLocalRandom

import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Gamebook, Shapes }
import lila.tree.NewRoot

case class NewChapter(
    _id: StudyChapterId,
    studyId: StudyId,
    name: StudyChapterName,
    setup: Chapter.Setup,
    root: NewRoot,
    tags: Tags,
    order: Int,
    ownerId: UserId,
    conceal: Option[Ply] = None,
    practice: Option[Boolean] = None,
    gamebook: Option[Boolean] = None,
    description: Option[String] = None,
    relay: Option[Chapter.Relay] = None,
    serverEval: Option[Chapter.ServerEval] = None,
    createdAt: Instant
) extends Chapter.Like
