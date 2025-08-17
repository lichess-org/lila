package lila.relay

import play.api.libs.json.*
import scalalib.model.Seconds
import chess.{ Outcome, ByColor }
import chess.format.pgn.{ PgnStr, SanStr, Move, Tag, Tags }

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
    def formattedDate = date.map(_.replace("-", "."))

  case class ClockJson(white: Option[Seconds], black: Option[Seconds], time: Long):
    def referenceTime: Instant = millisToInstant(time)
    def byColor = ByColor(white, black)

  /** This is DGT so it's all sorts of wrong. When the clock time is set in both the move `Rxd1 3010` and in
    * `clock.white`, then the latter should be used. Most of the time.
    */
  case class GameJson(
      moves: List[String],
      result: Option[String],
      clock: Option[ClockJson] = none,
      chess960: Option[Int] = none
  ):
    def outcome = result.flatMap(Outcome.fromResult)
    def mergeRoundTags(roundTags: Tags): Tags =
      val chess960PositionId = chess960.filter(_ != 518) // LCC sends 518 for standard chess
      val fenTag = chess960PositionId
        .flatMap(chess.variant.Chess960.positionToFen)
        .map(pos => Tag(_.FEN, pos.value))
      val variantTag = (chess960PositionId.isDefined && roundTags.variant.isEmpty).option:
        Tag(_.Variant, chess.variant.Chess960.name)
      val outcomeTag = outcome.map(o => Tag(_.Result, Outcome.showResult(o.some)))
      roundTags ++ Tags(List(fenTag, variantTag, outcomeTag).flatten)
    def clockTags: Tags =
      clock.fold(Tags.empty): c =>
        Tags(
          List(
            c.white.map(v => Tag(_.WhiteClock, v.toString)),
            c.black.map(v => Tag(_.BlackClock, v.toString))
            // Tag(_.ReferenceTime, c.referenceTime.toString).some // unused
          ).flatten
        )
    def toPgn(roundTags: Tags): PgnStr =
      val mergedTags = clockTags ++ mergeRoundTags(roundTags)
      val parsedMoves = moves.map(parseMove)
      val fixedMoves = clock.foldLeft(parsedMoves)(replaceLastMoveTimesWithClock)
      val strMoves = fixedMoves.map(_.render).mkString(" ")
      PgnStr(s"$mergedTags\n\n$strMoves")

  /* - dxe4 63
   * - fxe4 +31
   * - Nc2 34+2
   */
  private def parseMove(str: String): Move =
    val parts = str.split(' ')
    val (clk: Option[Int], emt: Option[Int]) = parts
      .lift(1)
      .fold((none, none)):
        _.split('+') match
          case Array(clk) => (clk.toIntOption, none)
          case Array("", emt) => (none, emt.toIntOption)
          case Array(clk, emt) => (clk.toIntOption, emt.toIntOption)
          case _ => (none, none)
    Move(san = SanStr(~parts.headOption), timeLeft = Seconds.from(clk), moveTime = Seconds.from(emt))

  private def replaceLastMoveTimesWithClock(moves: List[Move], clock: ClockJson): List[Move] =
    val lastMoveIndex = moves.size - 1
    def change(move: Move, sec: Option[Seconds]) = sec.fold(move)(s => move.copy(timeLeft = s.some))
    moves.mapWithIndex: (move, i) =>
      if i >= lastMoveIndex - 1
      then change(move, clock.byColor(Color.fromWhite(i % 2 == 0)))
      else move

  given Reads[PairingPlayer] = Json.reads
  given Reads[RoundJsonPairing] = Json.reads
  given Reads[RoundJson] = Json.reads
  given Reads[Seconds] = Reads.of[Int].map(Seconds.apply)
  given Reads[ClockJson] = Json.reads
  given Reads[GameJson] = Json.reads
