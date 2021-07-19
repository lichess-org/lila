package lila.game

import shogi.Color.{ Gote, Sente }
import shogi.format.{ FEN, Uci }
import shogi.opening.{ FullOpening, FullOpeningDB }
import shogi.variant.{ FromPosition, Standard, Variant }
import shogi.{
  Centis,
  CheckCount,
  Clock,
  Color,
  Mode,
  MoveOrDrop,
  Speed,
  Status,
  Game => ShogiGame,
  StartingPosition
}
import lila.common.Sequence
import lila.db.ByteArray
import lila.rating.PerfType
import lila.rating.PerfType.Classical
import lila.user.User
import org.joda.time.DateTime

case class Game(
    id: Game.ID,
    sentePlayer: Player,
    gotePlayer: Player,
    shogi: ShogiGame,
    loadClockHistory: Clock => Option[ClockHistory] = _ => Game.someEmptyClockHistory,
    status: Status,
    daysPerTurn: Option[Int],
    binaryMoveTimes: Option[ByteArray] = None,
    mode: Mode = Mode.default,
    bookmarks: Int = 0,
    createdAt: DateTime = DateTime.now,
    movedAt: DateTime = DateTime.now,
    metadata: Metadata
) {
  lazy val clockHistory = shogi.clock flatMap loadClockHistory

  def situation = shogi.situation
  def board     = shogi.situation.board
  def history   = shogi.situation.board.history
  def variant   = shogi.situation.board.variant
  def turns     = shogi.turns
  def clock     = shogi.clock
  def pgnMoves  = shogi.pgnMoves

  val players = List(sentePlayer, gotePlayer)

  def player(color: Color): Player = color.fold(sentePlayer, gotePlayer)

  def player(playerId: Player.ID): Option[Player] =
    players find (_.id == playerId)

  def player(user: User): Option[Player] =
    players find (_ isUser user)

  def player(c: Color.type => Color): Player = player(c(Color))

  def isPlayerFullId(player: Player, fullId: String): Boolean =
    (fullId.size == Game.fullIdSize) && player.id == (fullId drop Game.gameIdSize)

  def player: Player = player(turnColor)

  def playerByUserId(userId: String): Option[Player]   = players.find(_.userId contains userId)
  def opponentByUserId(userId: String): Option[Player] = playerByUserId(userId) map opponent

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val firstColor = Color(sentePlayer before gotePlayer)
  def firstPlayer     = player(firstColor)
  def secondPlayer    = player(!firstColor)

  def turnColor = shogi.player

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean  = c == turnColor
  def turnOf(u: User): Boolean   = player(u) ?? turnOf

  def playedTurns = turns - shogi.startedAtTurn

  def flagged = (status == Status.Outoftime).option(turnColor)

  def fullIdOf(player: Player): Option[String] =
    (players contains player) option s"$id${player.id}"

  def fullIdOf(color: Color): String = s"$id${player(color).id}"

  def isHandicap(initialFen: Option[FEN]): Boolean =
    if (variant == Standard) false else StartingPosition isFENHandicap initialFen

  def tournamentId = metadata.tournamentId
  def simulId      = metadata.simulId
  def swissId      = metadata.swissId

  def isTournament = tournamentId.isDefined
  def isSimul      = simulId.isDefined
  def isSwiss      = swissId.isDefined
  def isMandatory  = isTournament || isSimul || isSwiss
  def isClassical  = perfType contains Classical
  def nonMandatory = !isMandatory

  def hasChat = !isTournament && !isSimul && nonAi

  // we can't rely on the clock,
  // because if moretime was given,
  // elapsed time is no longer representing the game duration
  def durationSeconds: Option[Int] =
    (movedAt.getSeconds - createdAt.getSeconds) match {
      case seconds if seconds > 60 * 60 * 12 => none // no way it lasted more than 12 hours, come on.
      case seconds                           => seconds.toInt.some
    }

  private def everyOther[A](l: List[A]): List[A] =
    l match {
      case a :: _ :: tail => a :: everyOther(tail)
      case _              => l
    }

  def moveTimes(color: Color): Option[List[Centis]] = {
    for {
      clk <- clock
      inc = clk.incrementOf(color)
      history <- clockHistory
      clocks = history(color)
    } yield Centis(0) :: {
      val pairs = clocks.iterator zip clocks.iterator.drop(1)

      // We need to determine if this color's last clock had inc applied.
      // if finished and history.size == playedTurns then game was ended
      // by a players move, such as with mate or autodraw. In this case,
      // the last move of the game, and the only one without inc, is the
      // last entry of the clock history for !turnColor.
      //
      // On the other hand, if history.size is more than playedTurns,
      // then the game ended during a players turn by async event, and
      // the last recorded time is in the history for turnColor.
      val noLastInc = finished && (history.size <= playedTurns) == (color != turnColor)

      val byoStart = {
        if (clk.players(color).startsAtZero)
          0
        else (history.turnByoyomiStarted(color) - 1)
      }

      val outOfTimeByo = (status == Status.Outoftime) && (color == turnColor)

      pairs.zipWithIndex.map { case ((first, second), index) =>
        {
          val d = first - second
          if (((index + 1) >= byoStart) && !pairs.hasNext && outOfTimeByo)
            clk.byoyomi // If we time out over a period(s) this doesn't work
          else if ((index + 1) >= byoStart) second
          else if (pairs.hasNext || !noLastInc) d + inc
          else d
        } nonNeg
      } toList
    }
  } orElse binaryMoveTimes.map { binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, playedTurns)
    val mts  = if (color == startColor) base else base.drop(1)
    everyOther(mts.toList)
  }

  def moveTimes: Option[Vector[Centis]] =
    for {
      a <- moveTimes(startColor)
      b <- moveTimes(!startColor)
    } yield Sequence.interleave(a, b)

  def bothClockStates: Option[Vector[Centis]] = {
    (clock, clockHistory) match {
      case (Some(c), Some(ch)) => Some(ch.bothClockStates(startColor, c.byoyomi))
      case (_, Some(ch))       => Some(ch.bothClockStates(startColor, Centis(0)))
      case _                   => None
    }
  }

  def pgnMoves(color: Color): PgnMoves = {
    val pivot = if (color == startColor) 0 else 1
    pgnMoves.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }
  }

  // apply a move
  def update(
      game: ShogiGame, // new shogi position
      moveOrDrop: MoveOrDrop,
      blur: Boolean = false
  ): Progress = {
    def copyPlayer(player: Player) =
      if (blur && moveOrDrop.fold(_.color, _.color) == player.color)
        player.copy(
          blurs = player.blurs.add(playerMoves(player.color))
        )
      else player

    // This must be computed eagerly
    // because it depends on the current time
    val newClockHistory = for {
      clk <- game.clock
      ch  <- clockHistory
    } yield ch.record(turnColor, clk).validateStart(clk)

    val updated = copy(
      sentePlayer = copyPlayer(sentePlayer),
      gotePlayer = copyPlayer(gotePlayer),
      shogi = game,
      binaryMoveTimes = (!isPgnImport && !shogi.clock.isDefined).option {
        BinaryFormat.moveTime.write {
          binaryMoveTimes.?? { t =>
            BinaryFormat.moveTime.read(t, playedTurns)
          } :+ Centis(nowCentis - movedAt.getCentis).nonNeg
        }
      },
      loadClockHistory = _ => newClockHistory,
      status = game.situation.status | status,
      movedAt = DateTime.now
    )

    val state = Event.State(
      color = game.situation.color,
      turns = game.turns,
      status = (status != updated.status) option updated.status,
      winner = game.situation.winner,
      senteOffersDraw = sentePlayer.isOfferingDraw,
      goteOffersDraw = gotePlayer.isOfferingDraw
    )

    val clockEvent = updated.shogi.clock map Event.Clock.apply orElse {
      updated.playableCorrespondenceClock map Event.CorrespondenceClock.apply
    }

    val events = List(moveOrDrop.fold(
      Event.Move(_, game.situation, state, clockEvent, updated.board.crazyData),
      Event.Drop(_, game.situation, state, clockEvent, updated.board.crazyData)
    ))
    
    Progress(this, updated, events)
  }

  def lastMoveKeys: Option[String] = {
    history.lastMove map {
      case Uci.Drop(target, pos) => s"${target.forsyth}*$pos"
      case m: Uci.Move           => m.keys
    }
  }

  def lastMoveUsiKeys: Option[String] = {
    history.lastMove map {
      case Uci.Drop(target, pos) => s"${target.forsyth}*${pos.usiKey}"
      case m: Uci.Move           => m.usiKeys
    }
  }

  def updatePlayer(color: Color, f: Player => Player) =
    color.fold(
      copy(sentePlayer = f(sentePlayer)),
      copy(gotePlayer = f(gotePlayer))
    )

  def updatePlayers(f: Player => Player) =
    copy(
      sentePlayer = f(sentePlayer),
      gotePlayer = f(gotePlayer)
    )

  def start =
    if (started) this
    else
      copy(
        status = Status.Started,
        mode = Mode(mode.rated && userIds.distinct.size == 2)
      )

  def startClock =
    clock map { c =>
      start.withClock(c.start)
    }

  def moveToNextPeriod = {
    clock map { c =>
      {
        clockHistory match {
          case Some(ch) =>
            start.withClockAndHistory(
              c.nextPeriod(turnColor),
              ch.enteredNewPeriod(turnColor, shogi.fullMoveNumber)
            )
          case _ => start.withClock(c.nextPeriod(turnColor))
        }
      }
    }
  }

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn map { days =>
      val increment   = days * 24 * 60 * 60
      val secondsLeft = (movedAt.getSeconds + increment - nowSeconds).toInt max 0
      CorrespondenceClock(
        increment = increment,
        senteTime = turnColor.fold(secondsLeft, increment).toFloat,
        goteTime = turnColor.fold(increment, secondsLeft).toFloat
      )
    }

  def playableCorrespondenceClock: Option[CorrespondenceClock] =
    playable ?? correspondenceClock

  def speed = Speed(shogi.clock.map(_.config))

  def perfKey  = PerfPicker.key(this)
  def perfType = PerfType(perfKey)

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

  def abort = copy(status = Status.Aborted)

  def playable = status < Status.Aborted && !imported

  def playableEvenImported = status < Status.Aborted

  def playableBy(p: Player): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def playableByAi: Boolean = playable && player.isAi

  def mobilePushable = isCorrespondence && playable && nonAi

  def alarmable = hasCorrespondenceClock && playable && nonAi

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi          = !hasAi

  def aiPov: Option[Pov] = players.find(_.isAi).map(_.color) map pov

  def mapPlayers(f: Player => Player) =
    copy(
      sentePlayer = f(sentePlayer),
      gotePlayer = f(gotePlayer)
    )

  def playerCanOfferDraw(color: Color) = false
  //started && playable &&
  //  turns >= 8 &&
  //  shogi.situation.impasse &&
  //  !player(color).isOfferingDraw &&
  //  !opponent(color).isAi &&
  //  !playerHasOfferedDraw(color)

  def playerHasOfferedDraw(color: Color) =
    player(color).lastDrawOffer ?? (_ >= turns - 20)

  def playerCouldRematch =
    finishedOrAborted &&
      nonMandatory &&
      !boosted && ! {
        hasAi && variant == FromPosition && clock.exists(_.config.limitSeconds < 60)
      }

  def playerCanProposeTakeback(color: Color) =
    started && playable && !isTournament && !isSimul &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def moretimeable(color: Color) =
    playable && nonMandatory && {
      clock.??(_ moretimeable color) || correspondenceClock.??(_ moretimeable color)
    }

  def abortable = status == Status.Started && playedTurns < 2 && nonMandatory

  def berserkable = clock.??(_.config.berserkable) && status == Status.Started && playedTurns < 2

  def goBerserk(color: Color) =
    clock.ifTrue(berserkable && !player(color).berserk).map { c =>
      val newClock = c goBerserk color
      Progress(
        this,
        copy(
          shogi = shogi.copy(clock = Some(newClock)),
          loadClockHistory = _ =>
            clockHistory.map(history => {
              if (history(color).isEmpty) history
              else history.reset(color).record(color, newClock)
            })
        ).updatePlayer(color, _.goBerserk)
      ) ++
        List(
          Event.ClockInc(color, -c.config.berserkPenalty),
          Event.Clock(newClock), // BC
          Event.Berserk(color)
        )
    }

  def resignable      = playable && !abortable
  def drawable        = playable && !abortable
  def forceResignable = resignable && nonAi && !fromFriend && hasClock

  def finish(status: Status, winner: Option[Color]) = {
    val newClock = clock map { _.stop }
    Progress(
      this,
      copy(
        status = status,
        sentePlayer = sentePlayer.finish(winner contains Sente),
        gotePlayer = gotePlayer.finish(winner contains Gote),
        shogi = shogi.copy(clock = newClock),
        loadClockHistory = clk =>
          clockHistory map { history =>
            // If not already finished, we're ending due to an event
            // in the middle of a turn, such as resignation or draw
            // acceptance. In these cases, record a final clock time
            // for the active color. This ensures the end time in
            // clockHistory always matches the final clock time on
            // the board.
            if (!finished) history.record(turnColor, clk, true)
            else history
          }
      ),
      // Events here for BC.
      List(Event.End(winner)) ::: newClock.??(c => List(Event.Clock(c)))
    )
  }

  def rated  = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def replayable = isPgnImport || finished || (aborted && bothPlayersHaveMoved)

  def analysable =
    replayable && playedTurns > 4 &&
      Game.analysableVariants(variant)

  def ratingVariant =
    if (isTournament && variant.fromPosition) Standard
    else variant

  def fromPosition = variant.fromPosition || source.??(Source.Position ==)

  def imported = source contains Source.Import

  def fromPool   = source contains Source.Pool
  def fromLobby  = source contains Source.Lobby
  def fromFriend = source contains Source.Friend

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winner map (_.color == c)

  def lostBy(c: Color): Option[Boolean] = winner map (_.color != c)

  def drawn = finished && winner.isEmpty

  def outoftime(withGrace: Boolean): Boolean = {
    if (isCorrespondence)
      outoftimeCorrespondence
    else outoftimeClock(withGrace)
  }

  def nextPeriodClock(withGrace: Boolean): Boolean =
    clock ?? { c =>
      !isCorrespondence && clockValidity && c.outOfTime(turnColor, withGrace) && c.hasPeriodsLeft(turnColor)
    }

  private def clockValidity: Boolean = started && playable && (bothPlayersHaveMoved || isSimul || isSwiss)

  private def outoftimeClock(withGrace: Boolean): Boolean =
    clock ?? { c =>
      clockValidity && {
        (!c.isRunning && c.players.exists(_.elapsed.centis > 0)) || c.outOfTime(turnColor, withGrace)
      }
    }

  private def outoftimeCorrespondence: Boolean =
    playableCorrespondenceClock ?? { _ outoftime turnColor }

  def isCorrespondence = speed == Speed.Correspondence

  def isSwitchable = nonAi && (isCorrespondence || isSimul)

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def isClockRunning = clock ?? (_.isRunning)

  def withClock(c: Clock) = Progress(this, copy(shogi = shogi.copy(clock = Some(c))))

  def withClockAndHistory(c: Clock, ch: ClockHistory) =
    Progress(this, copy(shogi = shogi.copy(clock = Some(c)), loadClockHistory = _ => Some(ch)))

  def correspondenceGiveTime = Progress(this, copy(movedAt = DateTime.now))

  def estimateClockTotalTime = clock.map(_.estimateTotalSeconds)

  def estimateTotalTime =
    estimateClockTotalTime orElse
      correspondenceClock.map(_.estimateTotalTime) getOrElse 1200

  def timeForFirstMove: Centis =
    Centis ofSeconds {
      import Speed._
      if (isTournament) speed match {
        case UltraBullet => 11
        case Bullet      => 16
        case Blitz       => 21
        case Rapid       => 25
        case _           => 30
      } else
        speed match {
          case UltraBullet => 15
          case Bullet      => 20
          case Blitz       => 25
          case Rapid       => 30
          case _           => 35
        }
    }

  def expirable =
    !bothPlayersHaveMoved && source.exists(Source.expirable.contains) && playable && nonAi && clock.exists(
      !_.isRunning
    )

  def timeBeforeExpiration: Option[Centis] =
    expirable option {
      Centis.ofMillis(movedAt.getMillis - nowMillis + timeForFirstMove.millis).nonNeg
    }

  def playerWhoDidNotMove: Option[Player] =
    playedTurns match {
      case 0 => player(startColor).some
      case 1 => player(!startColor).some
      case _ => none
    }

  def onePlayerHasMoved    = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = Color(shogi.startedAtTurn % 2 == 0)

  def playerMoves(color: Color): Int =
    if (color == startColor) (playedTurns + 1) / 2
    else playedTurns / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int =
    if (playedTurns > 5) (player(color).blurs.nb * 100) / playerMoves(color)
    else 0

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def olderThan(seconds: Int) = movedAt isBefore DateTime.now.minusSeconds(seconds)

  def justCreated = createdAt isAfter DateTime.now.minusSeconds(1)

  def unplayed = !bothPlayersHaveMoved && (createdAt isBefore Game.unplayedDate)

  def abandoned =
    (status <= Status.Started) && {
      movedAt isBefore {
        if (hasAi && !hasCorrespondenceClock) Game.aiAbandonedDate
        else Game.abandonedDate
      }
    }

  def forecastable = started && playable && isCorrespondence && !hasAi

  def hasBookmarks = bookmarks > 0

  def showBookmarks = hasBookmarks ?? bookmarks.toString

  def userIds = playerMaps(_.userId)

  def twoUserIds: Option[(User.ID, User.ID)] =
    for {
      s <- sentePlayer.userId
      g <- gotePlayer.userId
      if s != g
    } yield s -> g

  def userRatings = playerMaps(_.rating)

  def averageUsersRating =
    userRatings match {
      case a :: b :: Nil => Some((a + b) / 2)
      case a :: Nil      => Some((a + 1500) / 2)
      case _             => None
    }

  def withTournamentId(id: String) = copy(metadata = metadata.copy(tournamentId = id.some))
  def withSwissId(id: String)      = copy(metadata = metadata.copy(swissId = id.some))

  def withSimulId(id: String) = copy(metadata = metadata.copy(simulId = id.some))

  def withId(newId: String) = copy(id = newId)

  def source = metadata.source

  def pgnImport   = metadata.pgnImport
  def isPgnImport = pgnImport.isDefined

  def resetTurns =
    copy(
      shogi = shogi.copy(turns = 0, startedAtTurn = 0)
    )

  lazy val opening: Option[FullOpening.AtPly] =
    if (fromPosition || !Variant.openingSensibleVariants(variant)) none
    else FullOpeningDB search pgnMoves

  def synthetic = id == Game.syntheticId

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap f

  def pov(c: Color)                                 = Pov(this, c)
  def playerIdPov(playerId: Player.ID): Option[Pov] = player(playerId) map { Pov(this, _) }
  def sentePov                                      = pov(Sente)
  def gotePov                                       = pov(Gote)
  def playerPov(p: Player)                          = pov(p.color)
  def loserPov                                      = loser map playerPov

  def setAnalysed = copy(metadata = metadata.copy(analysed = true))

  def secondsSinceCreation = (nowSeconds - createdAt.getSeconds).toInt

  override def toString = s"""Game($id)"""
}

object Game {

  type ID = String

  case class Id(value: String) extends AnyVal with StringValue {
    def full(playerId: PlayerId) = FullId(s"$value{$playerId.value}")
  }
  case class FullId(value: String) extends AnyVal with StringValue {
    def gameId   = Id(value take gameIdSize)
    def playerId = PlayerId(value drop gameIdSize)
  }
  case class PlayerId(value: String) extends AnyVal with StringValue

  case class WithInitialFen(game: Game, fen: Option[FEN])

  val syntheticId = "synthetic"

  val maxPlayingRealtime = 100 // plus 200 correspondence games

  val maxPlies = 600 // unlimited can cause StackOverflowError

  val analysableVariants: Set[Variant] = Set(
    shogi.variant.Standard,
    shogi.variant.FromPosition
  )

  val unanalysableVariants: Set[Variant] = Variant.all.toSet -- analysableVariants

  val variantsWhereSenteIsBetter: Set[Variant] = Set()

  val blindModeVariants: Set[Variant] = Set(
    shogi.variant.Standard,
    shogi.variant.FromPosition
  )

  def allowRated(variant: Variant, clock: Option[Clock.Config]) =
    variant.standard || {
      clock ?? { _.estimateTotalTime >= Centis(3000) }
    }

  val gameIdSize   = 8
  val playerIdSize = 4
  val fullIdSize   = 12
  val tokenSize    = 4

  val unplayedHours = 24
  def unplayedDate  = DateTime.now minusHours unplayedHours

  val abandonedDays = 21
  def abandonedDate = DateTime.now minusDays abandonedDays

  val aiAbandonedHours = 6
  def aiAbandonedDate  = DateTime.now minusHours aiAbandonedHours

  def takeGameId(fullId: String)   = fullId take gameIdSize
  def takePlayerId(fullId: String) = fullId drop gameIdSize

  val idRegex         = """[\w-]{8}""".r
  def validId(id: ID) = idRegex matches id

  private val boardApiRatedMinClock = shogi.Clock.Config(20 * 60, 0, 0, 0)

  def isBoardCompatible(game: Game): Boolean =
    game.clock.fold(true) { c =>
      isBoardCompatible(c.config, game.mode)
    }

  def isBoardCompatible(clock: Clock.Config, mode: Mode): Boolean =
    if (mode.rated) clock.estimateTotalTime >= boardApiRatedMinClock.estimateTotalTime
    else shogi.Speed(clock) >= Speed.Rapid

  def isBotCompatible(game: Game) =
    game.hasAi || game.source.contains(Source.Friend)

  private[game] val emptyCheckCount = CheckCount(0, 0)

  private[game] val someEmptyClockHistory = Some(ClockHistory())

  def make(
      shogi: ShogiGame,
      sentePlayer: Player,
      gotePlayer: Player,
      mode: Mode,
      source: Source,
      pgnImport: Option[PgnImport],
      daysPerTurn: Option[Int] = None
  ): NewGame = {
    val createdAt = DateTime.now
    NewGame(
      Game(
        id = IdGenerator.uncheckedGame,
        sentePlayer = sentePlayer,
        gotePlayer = gotePlayer,
        shogi = shogi,
        status = Status.Created,
        daysPerTurn = daysPerTurn,
        mode = mode,
        metadata = Metadata(
          source = source.some,
          pgnImport = pgnImport,
          tournamentId = none,
          swissId = none,
          simulId = none,
          analysed = false
        ),
        createdAt = createdAt,
        movedAt = createdAt
      )
    )
  }

  def metadata(source: Source) =
    Metadata(
      source = source.some,
      pgnImport = none,
      tournamentId = none,
      swissId = none,
      simulId = none,
      analysed = false
    )

  object BSONFields {

    val id                = "_id"
    val sentePlayer       = "p0"
    val gotePlayer        = "p1"
    val playerIds         = "is"
    val playerUids        = "us"
    val playingUids       = "pl"
    val binaryPieces      = "ps"
    val oldPgn            = "pg"
    val huffmanPgn        = "hp"
    val status            = "s"
    val turns             = "t"
    val startedAtTurn     = "st"
    val clock             = "c"
    val positionHashes    = "ph"
    val checkCount        = "cc"
    val historyLastMove   = "cl"
    val daysPerTurn       = "cd"
    val moveTimes         = "mt"
    val senteClockHistory = "cw"
    val goteClockHistory  = "cb"
    val periodsSente      = "pw"
    val periodsGote       = "pb"
    val rated             = "ra"
    val analysed          = "an"
    val variant           = "v"
    val crazyData         = "hs"
    //val hands             = "chd"
    val bookmarks         = "bm"
    val createdAt         = "ca"
    val movedAt           = "ua" // ua = updatedAt (bc)
    val source            = "so"
    val pgnImport         = "pgni"
    val tournamentId      = "tid"
    val swissId           = "iid"
    val simulId           = "sid"
    val tvAt              = "tv"
    val winnerColor       = "w"
    val winnerId          = "wid"
    val initialFen        = "if"
    val checkAt           = "ck"
    val perfType          = "pt" // only set on student games for aggregation
  }
}

// Represents at what turn we entered a new period
case class PeriodEntries(sente: Vector[Int], gote: Vector[Int]) {
  def apply(c: Color) =
    c.fold(sente, gote)

  def update(c: Color, t: Int) =
    c.fold(copy(sente = sente :+ t), copy(gote = gote :+ t))

  def byoyomi(c: Color): Boolean =
    c.fold(!sente.isEmpty, !gote.isEmpty)

  def turnIsPresent(c: Color, t: Int): Boolean = c.fold(sente contains t, gote contains t)
  def dropTurn(c: Color, t: Int) =
    c.fold(
      copy(sente = sente filterNot (_ == t)),
      copy(gote = gote filterNot (_ == t))
    )

  def first(c: Color): Option[Int] = c.fold(sente.headOption, gote.headOption)
  def last(c: Color): Option[Int]  = c.fold(sente.lastOption, gote.lastOption)
}

object PeriodEntries {
  val default = PeriodEntries(Vector(), Vector())
  val zeroes  = PeriodEntries(Vector(0), Vector(0))
}

case class ClockHistory(
    sente: Vector[Centis] = Vector.empty,
    gote: Vector[Centis] = Vector.empty,
    periodEntries: PeriodEntries = PeriodEntries.default
) {

  def update(color: Color, f: Vector[Centis] => Vector[Centis]): ClockHistory =
    color.fold(copy(sente = f(sente)), copy(gote = f(gote)))

  def record(color: Color, clock: Clock, finalRecord: Boolean = false): ClockHistory = {
    if (finalRecord && clock.isUsingByoyomi(color)) {
      val remTime = clock.remainingTime(color)
      update(color, _ :+ (clock.byoyomi - remTime))
    } else update(color, _ :+ clock.remainingTimeTurn(color))
  }

  def validateStart(clock: Clock): ClockHistory = {
    if (clock.startsAtZero && periodEntries(Sente).isEmpty && periodEntries(Gote).isEmpty)
      copy(periodEntries = PeriodEntries.zeroes)
    else this
  }

  def reset(color: Color) = update(color, _ => Vector.empty)

  def apply(color: Color): Vector[Centis] = color.fold(sente, gote)

  def last(color: Color) = apply(color).lastOption

  def size = sente.size + gote.size

  def updateWithByoTime(color: Color, byo: Centis): Vector[Centis] = {
    val standardTimes = color.fold(
      sente.take(turnByoyomiStarted(color) - 1),
      gote.take(turnByoyomiStarted(color) - 1)
    )
    standardTimes.padTo(color.fold(sente.size, gote.size), byo)
  }

  def turnByoyomiStarted(color: Color): Int =
    periodEntries.first(color) getOrElse Game.maxPlies

  def enteredNewPeriod(color: Color, turn: Int) =
    copy(periodEntries = periodEntries.update(color, turn))

  def turnIsPresent(color: Color, turn: Int): Boolean =
    periodEntries.turnIsPresent(color, turn)

  def dropTurn(color: Color, turn: Int) = {
    if (turnIsPresent(color, turn)) copy(periodEntries = periodEntries.dropTurn(color, turn))
    else this
  }

  // first state is of the color that moved first.
  def bothClockStates(firstMoveBy: Color, byo: Centis): Vector[Centis] =
    Sequence.interleave(
      firstMoveBy.fold(updateWithByoTime(Sente, byo), updateWithByoTime(Gote, byo)),
      firstMoveBy.fold(updateWithByoTime(Gote, byo), updateWithByoTime(Sente, byo))
    )
}
