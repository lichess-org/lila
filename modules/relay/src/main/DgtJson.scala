package lila.relay

import play.api.libs.json.*
import scalalib.model.Seconds
import chess.Outcome
import chess.format.pgn.{ PgnStr, SanStr, Tag, Tags }

private object DgtJson:

  case class PairingPlayer(
      fname: Option[String],
      mname: Option[String],
      lname: Option[String],
      title: Option[String],
      fideid: Option[Int]
  ):
    def fullName = some {
      List(fname, mname, lname).flatten.mkString(" ")
    }.filter(_.nonEmpty)

  case class RoundJsonPairing(
      white: Option[PairingPlayer],
      black: Option[PairingPlayer],
      result: Option[String]
  ):
    import chess.format.pgn.*
    def tags(round: Int, game: Int, date: Option[String]) = Tags:
      List(
        white.flatMap(_.fullName).map { Tag(_.White, _) },
        white.flatMap(_.title).map { Tag(_.WhiteTitle, _) },
        white.flatMap(_.fideid).map { Tag(_.WhiteFideId, _) },
        black.flatMap(_.fullName).map { Tag(_.Black, _) },
        black.flatMap(_.title).map { Tag(_.BlackTitle, _) },
        black.flatMap(_.fideid).map { Tag(_.BlackFideId, _) },
        result.map(Tag(_.Result, _)),
        Tag(_.Round, s"$round.$game").some,
        date.map(Tag(_.Date, _))
      ).flatten

  case class RoundJson(
      date: Option[String],
      pairings: List[RoundJsonPairing]
  ):
    def finishedGameIndexes: List[Int] = pairings.zipWithIndex.collect:
      case (pairing, i) if pairing.result.forall(_ != "*") => i

  case class ClockJson(white: Seconds, black: Seconds, time: Seconds)

  case class GameJson(
      moves: List[String],
      result: Option[String],
      clock: Option[ClockJson] = none,
      chess960: Option[Int] = none
  ):
    def outcome = result.flatMap(Outcome.fromResult)
    def mergeRoundTags(roundTags: Tags): Tags =
      val fenTag = chess960
        .filter(_ != 518) // LCC sends 518 for standard chess
        .flatMap(chess.variant.Chess960.positionToFen)
        .map(pos => Tag(_.FEN, pos.value))
      val outcomeTag = outcome.map(o => Tag(_.Result, Outcome.showResult(o.some)))
      roundTags ++ Tags(List(fenTag, outcomeTag).flatten)
    def toPgn(roundTags: Tags): PgnStr =
      val mergedTags = mergeRoundTags(roundTags)
      val strMoves = moves
        .map(_.split(' '))
        .map: move =>
          chess.format.pgn
            .Move(
              san = SanStr(~move.headOption),
              secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)).flatMap(_.toIntOption)
            )
            .render
        .mkString(" ")
      PgnStr(s"$mergedTags\n\n$strMoves")

  given Reads[PairingPlayer]    = Json.reads
  given Reads[RoundJsonPairing] = Json.reads
  given Reads[RoundJson]        = Json.reads
  given Reads[Seconds]          = Reads.of[Int].map(Seconds.apply)
  given Reads[ClockJson]        = Json.reads
  given Reads[GameJson]         = Json.reads
