package lila.racer

import lila.common.Bus
import lila.core.id.SessionId
import lila.core.LightUser
import lila.memo.CacheApi
import lila.storm.StormSelector

final class RacerApi(
    selector: StormSelector,
    userApi: lila.core.user.UserApi,
    cacheApi: CacheApi,
    lightUser: LightUser.GetterSyncFallback
)(using Executor)(using scheduler: Scheduler):

  import RacerRace.Id

  private val store = cacheApi.notLoadingSync[RacerRace.Id, RacerRace](2048, "racer.race"):
    _.expireAfterAccess(30.minutes).build()

  def get(id: Id): Option[RacerRace] = store.getIfPresent(id)

  def playerId(sessionId: SessionId, user: Option[User]) = user match
    case Some(u) => RacerPlayer.Id.User(u.id)
    case None    => RacerPlayer.Id.Anon(sessionId)

  def createKeepOwnerAndJoin(race: RacerRace, player: RacerPlayer.Id): Fu[RacerRace.Id] =
    create(race.owner, 10).map { id =>
      join(id, player)
      id
    }

  def createAndJoin(player: RacerPlayer.Id): Fu[RacerRace.Id] =
    create(player, 10).map { id =>
      join(id, player)
      id
    }

  def create(player: RacerPlayer.Id, countdownSeconds: Int): Fu[RacerRace.Id] =
    selector.apply.map: puzzles =>
      val race = RacerRace
        .make(
          owner = player,
          puzzles = puzzles.grouped(2).flatMap(_.headOption).toList,
          countdownSeconds = countdownSeconds
        )
      store.put(race.id, race)
      lila.mon.racer.race(lobby = race.isLobby).increment()
      race.id

  private val rematchQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(32),
    timeout = 20.seconds,
    name = "racer.rematch",
    lila.log.asyncActorMonitor.full
  )

  def rematch(race: RacerRace, player: RacerPlayer.Id): Fu[RacerRace.Id] = race.rematch.flatMap(get) match
    case Some(found) if found.finished => rematch(found, player)
    case Some(found)                   =>
      join(found.id, player)
      fuccess(found.id)
    case None =>
      rematchQueue:
        createKeepOwnerAndJoin(race, player).map { rematchId =>
          save(race.copy(rematch = rematchId.some))
          rematchId
        }

  def makePlayer(id: RacerPlayer.Id) =
    RacerPlayer.make(id, RacerPlayer.Id.userIdOf(id).map(lightUser))

  def join(id: RacerRace.Id, playerId: RacerPlayer.Id): Option[RacerRace] =
    val player = makePlayer(playerId)
    get(id).flatMap(_.join(player)).map { r =>
      val race = (r.isLobby.so(doStart(r))) | r
      saveAndPublish(race)
      race
    }

  private[racer] def manualStart(race: RacerRace): Unit = (!race.isLobby).so:
    doStart(race).foreach(saveAndPublish)

  private def doStart(race: RacerRace): Option[RacerRace] =
    race.startCountdown.map: starting =>
      scheduler.scheduleOnce(RacerRace.duration.seconds + race.countdownSeconds.seconds + 50.millis):
        finish(race.id)
      starting

  private def finish(id: RacerRace.Id): Unit =
    get(id).foreach: race =>
      lila.mon.racer.players(lobby = race.isLobby).record(race.players.size)
      race.players.foreach: player =>
        lila.mon.racer.score(lobby = race.isLobby, auth = player.user.isDefined).record(player.score)
        player.user.ifTrue(player.score > 0).foreach { user =>
          Bus.pub(lila.core.misc.puzzle.RacerRun(user.id, player.score))
          userApi.addPuzRun("racer", user.id, player.score)
        }
      publish(race)

  def registerPlayerScore(id: RacerRace.Id, player: RacerPlayer.Id, score: Int): Unit =
    if score > 160 then logger.warn(s"$id $player score: $score")
    else get(id).flatMap(_.registerScore(player, score)).foreach(saveAndPublish)

  private def save(race: RacerRace): Unit =
    store.put(race.id, race)

  private def saveAndPublish(race: RacerRace): Unit =
    save(race)
    publish(race)
  private def publish(race: RacerRace): Unit =
    socket.foreach(_.publishState(race))

  // work around circular dependency
  private var socket: Option[RacerSocket]           = None
  private[racer] def registerSocket(s: RacerSocket) = socket = s.some
