package lila.game

import shogi.Color.{ Gote, Sente }
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.Variant
import shogi.{ Centis, Clock, Color, Game => ShogiGame, Handicap, Mode, Speed, Status }
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
    pausedSeconds: Option[Int] = None,
    sealedUsi: Option[Usi] = None,
    mode: Mode = Mode.default,
    bookmarks: Int = 0,
    createdAt: DateTime = DateTime.now,
    movedAt: DateTime = DateTime.now,
    metadata: Metadata
) {
  lazy val clockHistory = shogi.clock flatMap loadClockHistory

  def situation = shogi.situation
  def board     = shogi.board
  def hands     = shogi.hands
  def history   = shogi.history
  def variant   = shogi.variant
  def plies     = shogi.plies
  def clock     = shogi.clock
  def usis      = shogi.usis

  def initialSfen = history.initialSfen

  val players = List(sentePlayer, gotePlayer)

  def player(color: Color): Player = color.fold(sentePlayer, gotePlayer)

  def player(playerId: Player.ID): Option[Player] =
    players find (_.id == playerId)

  def player(user: User): Option[Player] =
    players find (_ isUser user)

  def player(c: Color.type => Color): Player = player(c(Color))

  def isPlayerFullId(player: Player, fullId: String): Boolean =
    (fullId.sizeIs == Game.fullIdSize) && player.id == (fullId drop Game.gameIdSize)

  def player: Player = player(turnColor)

  def playerByUserId(userId: String): Option[Player]   = players.find(_.userId contains userId)
  def opponentByUserId(userId: String): Option[Player] = playerByUserId(userId) map opponent

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val firstColor = Color.fromSente(sentePlayer before gotePlayer)
  def firstPlayer     = player(firstColor)
  def secondPlayer    = player(!firstColor)

  def turnColor = shogi.color

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean  = c == turnColor
  def turnOf(u: User): Boolean   = player(u) ?? turnOf

  def playedPlies = plies - shogi.startedAtPly

  def flagged = (status == Status.Outoftime).option(turnColor)

  def fullIdOf(player: Player): Option[String] =
    (players contains player) option s"$id${player.id}"

  def fullIdOf(color: Color): String = s"$id${player(color).id}"

  def tournamentId  = metadata.tournamentId
  def simulId       = metadata.simulId
  def postGameStudy = metadata.postGameStudy

  def isTournament = tournamentId.isDefined
  def isSimul      = simulId.isDefined
  def isMandatory  = isTournament || isSimul
  def isClassical  = perfType contains Classical
  def nonMandatory = !isMandatory

  def hasChat = !isTournament && !isSimul && nonAi

  // Only for defined handicaps in shogi/Handicap.scala
  lazy val isHandicap: Boolean = initialSfen.fold(false)(sfen => Handicap.isHandicap(sfen, variant))

  // we can't rely on the clock,
  // because if moretime was given,
  // elapsed time is no longer representing the game duration
  def durationSeconds: Option[Int] =
    (movedAt.getSeconds - createdAt.getSeconds - ~pausedSeconds) match {
      case seconds if seconds > 60 * 60 * 12 => none // no way it lasted more than 12 hours, come on.
      case seconds                           => (seconds.toInt atLeast 0).some
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
      byo = clk.byoyomiOf(color)
      history <- clockHistory
      clocks = history(color)
    } yield Centis(0) :: {
      val pairs = clocks.iterator zip clocks.iterator.drop(1)

      // We need to determine if this color's last clock had inc applied.
      // if finished and history.size == playedPlies then game was ended
      // by a players move, such as with mate or autodraw. In this case,
      // the last move of the game, and the only one without inc, is the
      // last entry of the clock history for !turnColor.
      //
      // On the other hand, if history.size is more than playedPlies,
      // then the game ended during a players turn by async event, and
      // the last recorded time is in the history for turnColor.
      val noLastInc = finished && (history.size <= playedPlies) == (color != turnColor)

      // Also if we timed out over a period or periods, we need to
      // multiply byoyomi by number of periods entered that turn and add
      // previous remaining time, which could either be byoyomi or
      // remaining time
      val byoyomiStart = history.firstEnteredPeriod(color)
      val byoyomiTimeout =
        byoyomiStart.isDefined && (status == Status.Outoftime) && (color == turnColor)

      pairs.zipWithIndex.map { case ((first, second), index) =>
        {
          val turn         = index + 2 + shogi.startedAtPly / 2
          val afterByoyomi = byoyomiStart ?? (_ <= turn)

          // after byoyomi we store movetimes directly, not remaining time
          val mt   = if (afterByoyomi) second else first - second
          val cInc = (!afterByoyomi && (pairs.hasNext || !noLastInc)) ?? inc

          if (!pairs.hasNext && byoyomiTimeout) {
            val prevTurnByoyomi = byoyomiStart ?? (_ < turn)
            (if (prevTurnByoyomi) byo else first) + byo * history.countSpentPeriods(color, turn)
          } else mt + cInc
        } nonNeg
      } toList
    }
  } orElse binaryMoveTimes.map { binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, playedPlies)
    val mts  = if (color == startColor) base else base.drop(1)
    everyOther(mts.toList)
  }

  def moveTimes: Option[Vector[Centis]] =
    for {
      a <- moveTimes(startColor)
      b <- moveTimes(!startColor)
    } yield Sequence.interleave(a, b)

  def bothClockStates: Option[Vector[Centis]] =
    clockHistory.map(_.bothClockStates(startColor, clock ?? (_.byoyomi)))

  def usis(color: Color): Usis = {
    val pivot = if (color == startColor) 0 else 1
    usis.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }
  }

  def resume =
    if (!paused) this
    else {
      val shogiAfterSealedUsi = sealedUsi.flatMap(u => shogi(u).toOption)
      val newShogi            = shogiAfterSealedUsi.getOrElse(shogi)
      val pSeconds            = (nowSeconds - movedAt.getSeconds).toInt atLeast 0
      val resumed = copy(
        // clock was already updated, make sure proper color is set
        shogi = newShogi.copy(clock = clock.map(_.copy(color = newShogi.situation.color).start)),
        status = Status.Started,
        sealedUsi = none,
        pausedSeconds = pausedSeconds.map(_ + pSeconds).orElse(pSeconds.some),
        movedAt = DateTime.now
      )

      if (shogiAfterSealedUsi.isDefined) resumed
      else
        resumed.copy(
          binaryMoveTimes = binaryMoveTimes.map { binary =>
            val moveTimes = BinaryFormat.moveTime.read(binary, playedPlies)
            BinaryFormat.moveTime.write(moveTimes)
          },
          loadClockHistory = _ =>
            clockHistory.map { ch =>
              (ch.update(newShogi.color, _.dropRight(1)))
            }
        )
    }

  def pauseAndSealUsi(
      usi: Usi,
      newShogi: ShogiGame, // new shogi position
      blur: Boolean
  ) =
    applyGame(newShogi, blur, true).map { g =>
      g.copy(
        sentePlayer = g.sentePlayer.removePauseOffer,
        gotePlayer = g.gotePlayer.removePauseOffer,
        shogi = shogi.copy(clock = newShogi.clock.map(_.stop)), // use old shogi position but new clock
        sealedUsi = usi.some,
        status = Status.Paused
      )
    }

  // after making a move
  def applyGame(
      newShogi: ShogiGame, // new shogi position
      blur: Boolean,
      reload: Boolean = false
  ): Progress = {
    def copyPlayer(player: Player) =
      if (blur && turnColor == player.color)
        player.addBlurs(playerMoves(player.color))
      else player

    // This must be computed eagerly
    // because it depends on the current time
    val newClockHistory = for {
      clk <- newShogi.clock
      ch  <- clockHistory
    } yield ch
      .record(turnColor, clk, shogi.fullTurnNumber)

    val updated = copy(
      sentePlayer = copyPlayer(sentePlayer),
      gotePlayer = copyPlayer(gotePlayer),
      shogi = newShogi,
      binaryMoveTimes = (!isNotationImport && !shogi.clock.isDefined).option {
        BinaryFormat.moveTime.write {
          binaryMoveTimes.?? { t =>
            BinaryFormat.moveTime.read(t, playedPlies)
          } :+ Centis(nowCentis - movedAt.getCentis).nonNeg
        }
      },
      loadClockHistory = _ => newClockHistory,
      status = newShogi.situation.status | status,
      movedAt = DateTime.now
    )

    val event = newShogi.usis.lastOption.ifFalse(reload).fold[Event](Event.Reload) { usi =>
      Event.UsiEvent(
        usi = usi,
        situation = newShogi.situation,
        state = Event.State(
          color = newShogi.situation.color,
          plies = newShogi.plies,
          status = (status != updated.status) option updated.status,
          winner = newShogi.situation.winner
        ),
        clock = updated.shogi.clock map Event.Clock.apply orElse {
          updated.playableCorrespondenceClock map Event.CorrespondenceClock.apply
        }
      )
    }

    Progress(this, updated, List(event))
  }

  def lastUsiStr: Option[String] =
    history.lastUsi map (_.usi)

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
        mode = Mode(mode.rated && userIds.distinct.sizeIs == 2)
      )

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn map { days =>
      val increment   = days * 24 * 60 * 60
      val secondsLeft = if (paused) increment else (movedAt.getSeconds + increment - nowSeconds).toInt max 0
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

  def prePaused = players.forall(_.isOfferingPause)

  def paused = status == Status.Paused

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

  def abort = copy(status = Status.Aborted)

  def playable = status < Status.Aborted && !imported && !paused

  def playableEvenPaused = status < Status.Aborted && !imported

  def playableBy(p: Player): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def playableByAi: Boolean = playable && player.isAi

  def mobilePushable = isCorrespondence && playable && nonAi

  def alarmable = hasCorrespondenceClock && playable && nonAi

  def aiPlayer: Option[Player]       = players find (_.isAi)
  def aiEngine: Option[EngineConfig] = aiPlayer flatMap (_.engineConfig)
  def aiLevel: Option[Int]           = aiEngine map (_.level)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi          = !hasAi

  // Set only after July 2024
  def hasBot: Boolean = players.exists(_.isBot)

  def hasHuman: Boolean = players.exists(_.isHuman)

  def aiPov: Option[Pov] = aiPlayer.map(_.color) map pov

  def mapPlayers(f: Player => Player) =
    copy(
      sentePlayer = f(sentePlayer),
      gotePlayer = f(gotePlayer)
    )

  def playerCanOfferDraw(color: Color) =
    Game.drawableVariants.contains(variant) &&
      started && playable &&
      plies >= 2 &&
      !player(color).isOfferingDraw &&
      !opponent(color).isAi &&
      !playerHasOfferedDraw(color)

  def playerHasOfferedDraw(color: Color) =
    player(color).lastDrawOffer ?? (_ >= plies - 20)

  def playerCanOfferPause(color: Color) =
    Game.pausableVariants.contains(variant) &&
      started && playable && nonAi &&
      plies >= 20 && players.forall(_.hasUser) &&
      !player(color).isOfferingPause &&
      clock.exists(c =>
        c.config.limitSeconds >= 60 * 15 || c.config.byoyomiSeconds >= 30
      ) // only real time - for now

  def playerCouldRematch =
    finishedOrAborted &&
      nonMandatory &&
      !boosted && {
        nonAi || initialSfen.isEmpty || !(clock.exists(_.config.limitSeconds < 60))
      }

  def playerCanProposeTakeback(color: Color) =
    started && playable && !isTournament && !isSimul &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def boosted = rated && finished && bothPlayersHaveMoved && playedPlies < 10

  def moretimeable(color: Color) =
    playable && nonMandatory && {
      clock.??(_ moretimeable color) || correspondenceClock.??(_ moretimeable color)
    }

  def abortable = status == Status.Started && playedPlies < 2 && nonMandatory

  def berserkable = clock.??(_.config.berserkable) && status == Status.Started && playedPlies < 2

  def goBerserk(color: Color) =
    clock.ifTrue(berserkable && !player(color).berserk).map { c =>
      val newClock = c goBerserk color
      Progress(
        this,
        copy(
          shogi = shogi.copy(clock = Some(newClock)),
          loadClockHistory = _ =>
            clockHistory.map(history => {
              if (history(color).isEmpty && history.periodEntries(color).isEmpty) history
              else history.reset(color).record(color, newClock, shogi.fullTurnNumber)
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

  def finish(status: Status, winner: Option[Color]) =
    copy(
      status = status,
      sentePlayer = sentePlayer.finish(winner contains Sente),
      gotePlayer = gotePlayer.finish(winner contains Gote),
      shogi = shogi.copy(clock = clock map { _.stop }),
      loadClockHistory = clk =>
        clockHistory map { history =>
          // If not already finished, we're ending due to an event
          // in the middle of a turn, such as resignation or draw
          // acceptance. In these cases, record a final clock time
          // for the active color. This ensures the end time in
          // clockHistory always matches the final clock time on
          // the board.
          if (!finished)
            history
              .record(turnColor, clk, shogi.fullTurnNumber)
          else history
        }
    )

  def rated  = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedPlies >= 2 || isTournament

  def replayable = isNotationImport || finished || (aborted && bothPlayersHaveMoved)

  def analysable =
    replayable && playedPlies > 4 &&
      Game.analysableVariants(variant)

  def fromPosition =
    initialSfen.isDefined || source.??(Source.Position ==)

  def imported = source contains Source.Import

  def fromLobby  = source contains Source.Lobby
  def fromFriend = source contains Source.Friend

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserColor: Option[Color] = loser map (_.color)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winner map (_.color == c)

  def lostBy(c: Color): Option[Boolean] = winner map (_.color != c)

  def drawn = finished && winner.isEmpty

  def outoftime(withGrace: Boolean): Boolean =
    if (isCorrespondence)
      outoftimeCorrespondence
    else outoftimeClock(withGrace)

  private def outoftimeClock(withGrace: Boolean): Boolean =
    clock ?? { c =>
      started && playable && {
        c.outOfTime(turnColor, withGrace) || {
          !c.isRunning && c.players.exists(_.elapsed.centis > 0)
        }
      }
    }

  private def outoftimeCorrespondence: Boolean =
    playableCorrespondenceClock ?? { _ outoftime turnColor }

  def isCorrespondence = speed == Speed.Correspondence

  def isSwitchable = nonAi && (isCorrespondence || isSimul)

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def withClock(c: Clock) = Progress(this, copy(shogi = shogi.copy(clock = Some(c))))

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
      }
      else
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

  def playerWhoDidNotMove: Option[Player] = {
    playedPlies match {
      case 0 => player(startColor).some
      case 1 => player(!startColor).some
      case _ => none
    }
  } filterNot { player => winnerColor contains player.color }

  def onePlayerHasMoved    = playedPlies > 0
  def bothPlayersHaveMoved = playedPlies > 1

  def startColor = Color.fromPly(shogi.startedAtPly)

  def playerMoves(color: Color): Int =
    if (color == startColor) (playedPlies + 1) / 2
    else playedPlies / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int =
    if (playedPlies > 5) (player(color).blurs.nb * 100) / playerMoves(color)
    else 0

  def isBeingPlayed = !isNotationImport && !finishedOrAborted && !paused

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

  def averageUsersRating(default: Int) =
    userRatings match {
      case a :: b :: Nil => Some((a + b) / 2)
      case a :: Nil      => Some((a + default) / 2)
      case _             => None
    }

  def withTournamentId(id: String) = copy(metadata = metadata.copy(tournamentId = id.some))

  def withSimulId(id: String) = copy(metadata = metadata.copy(simulId = id.some))

  def withId(newId: String) = copy(id = newId)

  def source = metadata.source

  def notationImport   = metadata.notationImport
  def isNotationImport = notationImport.isDefined
  def isKifImport      = notationImport.fold(false)(_.isKif)
  def isCsaImport      = notationImport.fold(false)(_.isCsa)

  def synthetic = id == Game.syntheticId

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap f

  def pov(c: Color)                                 = Pov(this, c)
  def playerIdPov(playerId: Player.ID): Option[Pov] = player(playerId) map { Pov(this, _) }
  def sentePov                                      = pov(Sente)
  def gotePov                                       = pov(Gote)
  def playerPov(p: Player)                          = pov(p.color)
  def loserPov                                      = loser map playerPov

  def setAnalysed = copy(metadata = metadata.copy(analysed = true))

  def setPostGameStudy(studyId: String) = copy(metadata = metadata.copy(postGameStudy = studyId.some))

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

  val syntheticId = "synthetic"

  val maxPlayingRealtime = 100 // plus 200 correspondence games

  def maxPlies(variant: Variant) =
    if (variant.chushogi) 1000
    else 700 // unlimited can cause StackOverflowError

  val analysableVariants: Set[Variant] = Set(
    shogi.variant.Standard,
    shogi.variant.Minishogi,
    shogi.variant.Kyotoshogi
  )

  val unanalysableVariants: Set[Variant] = Variant.all.toSet -- analysableVariants

  val drawableVariants: Set[Variant] = Set(
    shogi.variant.Chushogi
  )

  val pausableVariants: Set[Variant] = Set(
    shogi.variant.Chushogi
  )

  val blindModeVariants: Set[Variant] = Set(
    shogi.variant.Standard,
    shogi.variant.Minishogi,
    shogi.variant.Annanshogi,
    shogi.variant.Kyotoshogi,
    shogi.variant.Checkshogi
  )

  val gifVariants: Set[Variant] =
    Set(shogi.variant.Standard, shogi.variant.Checkshogi)

  def allowRated(initialSfen: Option[Sfen], clock: Option[Clock.Config], variant: Variant) =
    initialSfen.filterNot(_.initialOf(variant)).isEmpty && clock.fold(true) { c =>
      c.periodsTotal <= 1 && (!c.hasByoyomi || !c.hasIncrement) && (!variant.chushogi || c.estimateTotalSeconds >= 250)
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

  private[game] val someEmptyClockHistory = Some(ClockHistory())

  def make(
      shogi: ShogiGame,
      initialSfen: Option[Sfen],
      sentePlayer: Player,
      gotePlayer: Player,
      mode: Mode,
      source: Source,
      notationImport: Option[NotationImport],
      daysPerTurn: Option[Int] = None
  ): NewGame = {
    val createdAt = DateTime.now
    val shogiWithInitialSfen =
      initialSfen.filterNot(_.initialOf(shogi.variant)).fold(shogi) { sfen =>
        shogi.withHistory(shogi.situation.history.withInitialSfen(sfen))
      }
    NewGame(
      Game(
        id = IdGenerator.uncheckedGame,
        sentePlayer = sentePlayer,
        gotePlayer = gotePlayer,
        shogi = shogiWithInitialSfen,
        status = Status.Created,
        daysPerTurn = daysPerTurn,
        mode = mode,
        metadata = Metadata(
          source = source.some,
          notationImport = notationImport,
          tournamentId = none,
          simulId = none,
          postGameStudy = none,
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
      notationImport = none,
      tournamentId = none,
      simulId = none,
      postGameStudy = none,
      analysed = false
    )

  object BSONFields {

    val id                 = "_id"
    val sentePlayer        = "p0"
    val gotePlayer         = "p1"
    val playerIds          = "is"
    val playerUids         = "us"
    val playingUids        = "pl"
    val usis               = "um"
    val status             = "s"
    val plies              = "t"
    val clock              = "c"
    val positionHashes     = "ph"
    val daysPerTurn        = "cd"
    val moveTimes          = "mt"
    val senteClockHistory  = "cw"
    val goteClockHistory   = "cb"
    val periodsSente       = "pw"
    val periodsGote        = "pb"
    val rated              = "ra"
    val analysed           = "an"
    val postGameStudy      = "pgs"
    val variant            = "v"
    val lastLionCapture    = "llc"
    val consecutiveAttacks = "cna"
    val hands              = "hs"
    val bookmarks          = "bm"
    val createdAt          = "ca"
    val movedAt            = "ua" // ua = updatedAt (bc)
    val source             = "so"
    val notationImport     = "pgni"
    val tournamentId       = "tid"
    val simulId            = "sid"
    val tvAt               = "tv"
    val winnerColor        = "w"
    val winnerId           = "wid"
    val initialSfen        = "if"
    val checkAt            = "ck"
    val sealedUsi          = "su"
    val pausedSeconds      = "ps"
    val perfType           = "pt" // only set on student games for aggregation
  }
}

// At what turns we entered a new period
case class PeriodEntries(
    sente: Vector[Int],
    gote: Vector[Int]
) {

  def apply(c: Color) =
    c.fold(sente, gote)

  def update(c: Color, f: Vector[Int] => Vector[Int]) =
    c.fold(copy(sente = f(sente)), copy(gote = f(gote)))

}

object PeriodEntries {
  val default    = PeriodEntries(Vector(), Vector())
  val maxPeriods = 5
}

case class ClockHistory(
    sente: Vector[Centis] = Vector.empty,
    gote: Vector[Centis] = Vector.empty,
    periodEntries: PeriodEntries = PeriodEntries.default
) {

  def apply(color: Color): Vector[Centis] = color.fold(sente, gote)

  def update(color: Color, f: Vector[Centis] => Vector[Centis]): ClockHistory =
    color.fold(copy(sente = f(sente)), copy(gote = f(gote)))

  def updatePeriods(color: Color, f: Vector[Int] => Vector[Int]): ClockHistory =
    copy(periodEntries = periodEntries.update(color, f))

  def record(color: Color, clock: Clock, turn: Int): ClockHistory = {
    val curClock        = clock currentClockFor color
    val initiatePeriods = clock.config.startsAtZero && periodEntries(color).isEmpty
    val isUsingByoyomi  = curClock.periods > 0 && !initiatePeriods

    val timeToStore = if (isUsingByoyomi) clock.lastTimeOf(color) else curClock.time

    update(color, _ :+ timeToStore)
      .updatePeriods(
        color,
        _.padTo(initiatePeriods ?? 1, 0).padTo(curClock.periods atMost PeriodEntries.maxPeriods, turn)
      )
  }

  def reset(color: Color) = update(color, _ => Vector.empty).updatePeriods(color, _ => Vector.empty)

  def last(color: Color) = apply(color).lastOption

  def size = sente.size + gote.size

  def firstEnteredPeriod(color: Color): Option[Int] =
    periodEntries(color).headOption

  def countSpentPeriods(color: Color, turn: Int) =
    periodEntries(color).count(_ == turn)

  def refundSpentPeriods(color: Color, turn: Int) =
    updatePeriods(color, _.filterNot(_ == turn))

  private def padWithByo(color: Color, byo: Centis): Vector[Centis] = {
    val times = apply(color)
    times.take(firstEnteredPeriod(color).fold(times.size)(_ - 1)).padTo(times.size, byo)
  }

  // first state is of the color that moved first.
  def bothClockStates(firstMoveBy: Color, byo: Centis): Vector[Centis] = {
    val senteTimes = padWithByo(Sente, byo)
    val goteTimes  = padWithByo(Gote, byo)
    Sequence.interleave(
      firstMoveBy.fold(senteTimes, goteTimes),
      firstMoveBy.fold(goteTimes, senteTimes)
    )
  }
}
