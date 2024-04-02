package lila.core
package fishnet

import chess.format.{ Uci, Fen }

case class NewKey(userId: UserId, key: String)

case class GameRequest(gameId: GameId)
case class StudyChapterRequest(
    studyId: StudyId,
    chapterId: StudyChapterId,
    initialFen: Option[Fen.Full],
    variant: chess.variant.Variant,
    moves: List[Uci],
    userId: UserId,
    unlimited: Boolean
)
