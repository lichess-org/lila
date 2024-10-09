package lila.game

import chess.*
import chess.CoreArbitraries.given
import org.scalacheck.{ Arbitrary, Gen }
import play.api.libs.json.JsObject
import chess.variant.Crazyhouse

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

  given Arbitrary[Status] = Arbitrary(Gen.oneOf(Status.all))

  given Arbitrary[Event.State] = Arbitrary:
    for
      turns           <- Arbitrary.arbitrary[Ply]
      status          <- Gen.option(Arbitrary.arbitrary[Status])
      winner          <- Gen.option(Arbitrary.arbitrary[Color])
      whiteOffersDraw <- Arbitrary.arbitrary[Boolean]
      blackOffersDraw <- Arbitrary.arbitrary[Boolean]
    yield Event.State(turns, status, winner, whiteOffersDraw, blackOffersDraw)

  given Arbitrary[PromotableRole] = Arbitrary(Gen.oneOf(Rook, Knight, Bishop, Queen))

  given Arbitrary[Event.Promotion] = Arbitrary:
    for
      role <- Arbitrary.arbitrary[PromotableRole]
      pos  <- Arbitrary.arbitrary[Square]
    yield Event.Promotion(role, pos)

  given Arbitrary[Event.ClockEvent] = Arbitrary:
    for
      whiteTime <- Arbitrary.arbitrary[Centis]
      blackTime <- Arbitrary.arbitrary[Centis]
      nextLag   <- Arbitrary.arbitrary[Option[Centis]]
    yield Event.Clock(whiteTime, blackTime, nextLag)

  given Arbitrary[Crazyhouse.Pocket] = Arbitrary:
    for
      pawn   <- Gen.oneOf(0 to 8)
      knight <- Gen.oneOf(0 to 2)
      bishop <- Gen.oneOf(0 to 2)
      rook   <- Gen.oneOf(0 to 2)
      queen  <- Gen.oneOf(0 to 2)
    yield Crazyhouse.Pocket(pawn, knight, bishop, rook, queen)

  given Arbitrary[Crazyhouse.Data] = Arbitrary:
    for
      pockets  <- Arbitrary.arbitrary[ByColor[Crazyhouse.Pocket]]
      promoted <- Arbitrary.arbitrary[chess.bitboard.Bitboard]
    yield Crazyhouse.Data(pockets, promoted)

  given Arbitrary[Event.Move] = Arbitrary:
    for
      orig          <- Arbitrary.arbitrary[Square]
      dest          <- Arbitrary.arbitrary[Square]
      san           <- Arbitrary.arbitrary[format.pgn.SanStr]
      fen           <- Arbitrary.arbitrary[format.BoardFen]
      check         <- Arbitrary.arbitrary[Check]
      threefold     <- Arbitrary.arbitrary[Boolean]
      promotion     <- Gen.option(Arbitrary.arbitrary[Event.Promotion])
      enpassant     <- Gen.option(Arbitrary.arbitrary[Event.Enpassant])
      castle        <- Gen.option(Arbitrary.arbitrary[Event.Castling])
      state         <- Arbitrary.arbitrary[Event.State]
      clock         <- Gen.option(Arbitrary.arbitrary[Event.ClockEvent])
      possibleMoves <- Arbitrary.arbitrary[Map[Square, chess.bitboard.Bitboard]]
      possibleDrops <- Gen.option(Gen.listOfN(8, Gen.oneOf(Square.all)))
      crazyData     <- Gen.option(Arbitrary.arbitrary[Crazyhouse.Data])
    yield Event.Move(
      orig,
      dest,
      san,
      fen,
      check,
      threefold,
      promotion,
      enpassant,
      castle,
      state,
      clock,
      possibleMoves,
      possibleDrops,
      crazyData
    )

  extension (rank: Rank) def squares: List[Square] = Square.all.filter(_.rank == rank)
