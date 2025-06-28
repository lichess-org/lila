package lila.game

import chess.format.Uci
import chess.variant.Variant
import chess.{ ByColor, Castles, Centis, Clock, Color, Game as ChessGame, MoveOrDrop, Ply, Speed, Status }
import scalalib.model.Days

import lila.core.game.{ ClockHistory, Game, Player, Pov }
import lila.db.ByteArray
import lila.game.Blurs.addAtMoveIndex
import lila.rating.PerfType

object GameExt:

  def computeMoveTimes(g: Game, color: Color): Option[List[Centis]] = {
    for
      clk <- g.clock
      inc = clk.incrementOf(color)
      history <- g.clockHistory
      clocks = history(color)
    yield Centis(0) :: {
      val pairs = clocks.iterator.zip(clocks.iterator.drop(1))

      // We need to determine if this color's last clock had inc applied.
      // if finished and history.size == playedTurns then game was ended
      // by a players move, such as with mate or autodraw. In this case,
      // the last move of the game, and the only one without inc, is the
      // last entry of the clock history for !turnColor.
      //
      // On the other hand, if history.size is more than playedTurns,
      // then the game ended during a players turn by async event, and
      // the last recorded time is in the history for turnColor.
      val noLastInc = g.finished && (g.playedPlies >= history.size) == (color != g.turnColor)

      pairs
        .map: (first, second) =>
          {
            val d = first - second
            if pairs.hasNext || !noLastInc then d + inc else d
          }.nonNeg
        .toList
    }
  }.orElse(g.binaryMoveTimes.map: binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, g.playedPlies)
    val mts  = if color == g.startColor then base else base.drop(1)
    everyOther(mts.toList))

  def analysable(g: Game) =
    g.replayable && g.playedPlies > 4 &&
      Game.analysableVariants(g.variant) &&
      !Game.isOldHorde(g)

  extension (g: Game)

    def playerIdPov(playerId: GamePlayerId): Option[Pov] = g.playerById(playerId).map(p => Pov(g, p.color))

    def withClock(c: Clock) = Progress(g, g.copy(chess = g.chess.copy(clock = Some(c))))

    def startClock: Option[Progress] =
      g.clock.map: c =>
        g.start.withClock(c.start)

    def playerHasOfferedDrawRecently(color: Color) =
      g.drawOffers.lastBy(color).exists(_ >= g.ply - 20)

    def playerCanOfferDraw(color: Color) =
      g.started && g.playable &&
        g.ply >= 2 &&
        !g.player(color).isOfferingDraw &&
        !g.opponent(color).isAi &&
        !g.playerHasOfferedDrawRecently(color) &&
        !g.swissPreventsDraw &&
        !g.rulePreventsDraw

    def goBerserk(color: Color): Option[Progress] =
      g.clock
        .ifTrue(g.berserkable && !g.player(color).berserk)
        .map: c =>
          val newClock = c.goBerserk(color)
          Progress(
            g,
            g.copy(
              chess = g.chess.copy(clock = Some(newClock)),
              loadClockHistory = _ =>
                g.clockHistory.map: history =>
                  if history(color).isEmpty then history
                  else history.reset(color).record(color, newClock)
            ).updatePlayer(color, _.copy(berserk = true))
          ) ++
            List(
              Event.ClockInc(color, -c.config.berserkPenalty, newClock),
              Event.Clock(newClock), // BC
              Event.Berserk(color)
            )

    def setBlindfold(color: Color, blindfold: Boolean): Progress =
      Progress(g, g.updatePlayer(color, _.copy(blindfold = blindfold)), Nil)

    def moveTimes: Option[Vector[Centis]] = for
      a <- GameExt.computeMoveTimes(g, g.startColor)
      b <- GameExt.computeMoveTimes(g, !g.startColor)
    yield lila.core.game.interleave(a, b)

    // apply a move
    def applyMove(
        game: ChessGame, // new chess.Position
        moveOrDrop: MoveOrDrop,
        blur: Boolean = false
    ): Progress =

      def copyPlayer(player: Player) =
        if blur && moveOrDrop.color == player.color then
          player.copy(blurs = player.blurs.addAtMoveIndex(g.playerMoves(player.color)))
        else player

      // This must be computed eagerly
      // because it depends on the current time
      val newClockHistory = for
        clk <- game.clock
        ch  <- g.clockHistory
      yield ch.record(g.turnColor, clk)

      val updated = g.copy(
        players = g.players.map(copyPlayer),
        chess = game,
        binaryMoveTimes = (!g.sourceIs(_.Import) && g.chess.clock.isEmpty).option {
          BinaryFormat.moveTime.write {
            g.binaryMoveTimes.so { t =>
              BinaryFormat.moveTime.read(t, g.playedPlies)
            } :+ Centis.ofLong(nowCentis - g.movedAt.toCentis).nonNeg
          }
        },
        loadClockHistory = _ => newClockHistory,
        status = game.position.status | g.status,
        movedAt = nowInstant
      )

      val state = Event.State(
        turns = game.ply,
        status = (g.status != updated.status).option(updated.status),
        winner = game.position.winner,
        whiteOffersDraw = g.whitePlayer.isOfferingDraw,
        blackOffersDraw = g.blackPlayer.isOfferingDraw
      )

      val clockEvent = updated.chess.clock
        .map(Event.Clock.apply)
        .orElse:
          updated.playableCorrespondenceClock.map(Event.CorrespondenceClock.apply)

      val events = moveOrDrop.fold(
        Event.Move(_, game.position, state, clockEvent, updated.position.crazyData),
        Event.Drop(_, game.position, state, clockEvent, updated.position.crazyData)
      ) :: {
        (updated.position.variant.threeCheck && game.position.check.yes).so(List:
          Event.CheckCount(
            white = updated.history.checkCount.white,
            black = updated.history.checkCount.black
          ))
      }

      Progress(g, updated, events)
    end applyMove

    def finish(status: Status, winner: Option[Color]): Game =
      g.copy(
        status = status,
        players = winner.fold(g.players): c =>
          g.players.update(c, _.copy(isWinner = true.some)),
        chess = g.chess.copy(clock = g.clock.map(_.stop)),
        loadClockHistory = clk =>
          g.clockHistory.map: history =>
            // If not already finished, we're ending due to an event
            // in the middle of a turn, such as resignation or draw
            // acceptance. In these cases, record a final clock time
            // for the active color. This ensures the end time in
            // clockHistory always matches the final clock time on
            // the board.
            if !g.finished then history.record(g.turnColor, clk)
            else history
      )

    def abandoned = (g.status <= Status.Started) && (g.movedAt.isBefore(Game.abandonedDate))

    def playerBlurPercent(color: Color): Int =
      if g.playedPlies > 5
      then (g.player(color).blurs.nb * 100) / g.playerMoves(color)
      else 0

    def drawReason =
      if g.variant.isInsufficientMaterial(g.position) then DrawReason.InsufficientMaterial.some
      else if g.variant.fiftyMoves(g.history) then DrawReason.FiftyMoves.some
      else if g.history.threefoldRepetition then DrawReason.ThreefoldRepetition.some
      else if g.drawOffers.normalizedPlies.exists(g.ply <= _) then DrawReason.MutualAgreement.some
      else None

    def perfType: PerfType = PerfType(g.perfKey)

  end extension

  private def everyOther[A](l: List[A]): List[A] =
    l match
      case a :: _ :: tail => a :: everyOther(tail)
      case _              => l

end GameExt

object Game:

  val syntheticId = GameId("synthetic")

  val maxPlies = Ply(600) // unlimited would be a DoS target

  val analysableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Crazyhouse,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck,
    chess.variant.Antichess,
    chess.variant.FromPosition,
    chess.variant.Horde,
    chess.variant.Atomic,
    chess.variant.RacingKings
  )

  val unanalysableVariants: Set[Variant] = Variant.list.all.toSet -- analysableVariants

  val hordeWhitePawnsSince = instantOf(2015, 4, 11, 10, 0)

  def isOldHorde(game: Game) =
    game.variant == chess.variant.Horde &&
      game.createdAt.isBefore(Game.hordeWhitePawnsSince)

  val abandonedDays = Days(21)
  def abandonedDate = nowInstant.minusDays(abandonedDays.value)

  def isBoardCompatible(game: Game): Boolean =
    game.clockConfig.forall: c =>
      lila.core.game.isBoardCompatible(c) || {
        (game.hasAi || game.sourceIs(_.Friend) || game.sourceIs(_.Api)) &&
        chess.Speed(c) >= Speed.Blitz
      }

  // if source is Arena, we will also need to check if the arena accepts bots!
  def isBotCompatible(game: Game): Option[Boolean] =
    if !game.clockConfig.forall(lila.core.game.isBotCompatible) then false.some
    else if game.hasAi || game.sourceIs(_.Friend) || game.sourceIs(_.Api) then true.some
    else if game.sourceIs(_.Arena) then none
    else false.some

  def mightBeBoardOrBotCompatible(game: Game) = isBoardCompatible(game) || isBotCompatible(game).|(true)

  object BSONFields:
    export lila.core.game.BSONFields.*
    val whitePlayer       = "p0"
    val blackPlayer       = "p1"
    val playerIds         = "is"
    val binaryPieces      = "ps"
    val oldPgn            = "pg"
    val huffmanPgn        = "hp"
    val status            = "s"
    val startedAtTurn     = "st"
    val clock             = "c"
    val positionHashes    = "ph"
    val checkCount        = "cc"
    val castleLastMove    = "cl"
    val unmovedRooks      = "ur"
    val daysPerTurn       = "cd"
    val moveTimes         = "mt"
    val whiteClockHistory = "cw"
    val blackClockHistory = "cb"
    val rated             = "ra"
    val variant           = "v"
    val crazyData         = "chd"
    val bookmarks         = "bm"
    val source            = "so"
    val tournamentId      = "tid"
    val swissId           = "iid"
    val simulId           = "sid"
    val tvAt              = "tv"
    val winnerColor       = "w"
    val initialFen        = "if"
    val checkAt           = "ck"
    val drawOffers        = "do"
    val rules             = "rules"

case class CastleLastMove(castles: Castles, lastMove: Option[Uci])

object CastleLastMove:

  def init = CastleLastMove(Castles.init, None)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  import lila.db.ByteArray.byteArrayHandler

  private[game] given castleLastMoveHandler: BSONHandler[CastleLastMove] = tryHandler[CastleLastMove](
    { case bin: BSONBinary =>
      byteArrayHandler.readTry(bin).map(BinaryFormat.castleLastMove.read)
    },
    clmt => byteArrayHandler.writeTry(BinaryFormat.castleLastMove.write(clmt)).get
  )

enum DrawReason:
  case MutualAgreement, FiftyMoves, ThreefoldRepetition, InsufficientMaterial

private val someEmptyClockHistory = Some(ClockHistory())
