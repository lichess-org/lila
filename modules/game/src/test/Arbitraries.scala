package lila.game

import chess.*
import chess.CoreArbitraries.given
import org.scalacheck.{ Arbitrary, Gen }
import play.api.libs.json.JsObject

object Arbitraries:

  // TODO move somewhere
  given [S, T](using SameRuntime[S, T], Arbitrary[S]): Arbitrary[T] = Arbitrary:
    Arbitrary.arbitrary[S].map(summon[SameRuntime[S, T]].apply)

  // TODO move Move.Castle to scalachess
  given Arbitrary[Event.Castling] = Arbitrary:
    for
      color  <- Arbitrary.arbitrary[Color]
      king   <- Gen.oneOf(color.backRank.squares)
      kingTo <- Gen.oneOf(color.fold(List(Square.G1, Square.C1), List(Square.G8, Square.C8)))
      rookTo <- Gen.oneOf(color.fold(List(Square.F1, Square.D1), List(Square.F8, Square.D8)))
      rook   <- Gen.oneOf(color.backRank.squares.filter(_ != king))
      castle = Move.Castle(king, kingTo, rook, rookTo)
    yield Event.Castling(castle, color)

  given Arbitrary[Event.Enpassant] = Arbitrary:
    for
      color <- Arbitrary.arbitrary[Color]
      pos   <- Gen.oneOf(color.passablePawnRank.squares)
    yield Event.Enpassant(pos, color)

  // We use Event.Castling as a cookie for this test because We don't have an Arbitrary instance for JsObject yet
  given Arbitrary[Event.RedirectOwner] = Arbitrary:
    for
      color  <- Arbitrary.arbitrary[Color]
      id     <- Arbitrary.arbitrary[GameFullId]
      cookie <- Arbitrary.arbitrary[Option[Event.Castling]]
    yield Event.RedirectOwner(color, id, cookie.map(_.data.asInstanceOf[JsObject]))

  extension (rank: Rank) def squares: List[Square] = Square.all.filter(_.rank == rank)
