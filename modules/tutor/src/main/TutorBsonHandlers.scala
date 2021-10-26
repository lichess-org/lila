package lila.tutor

import reactivemongo.api.bson._

import lila.common.Iso
import lila.db.dsl._

private object TutorBsonHandlers {

  implicit val nbGamesBSONHandler      = intAnyValHandler[NbGames](_.value, NbGames)
  implicit val nbMovesBSONHandler      = intAnyValHandler[NbMoves](_.value, NbMoves)
  implicit val nbMovesRatioBSONHandler = Macros.handler[NbMovesRatio]
  implicit val previewPostBSONHandler  = Macros.handler[TutorTimeReport]
}
