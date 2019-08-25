package lidraughts.simul

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.{ Json, JsObject }
import scala.concurrent.duration._

import draughts.variant.Variant
import lidraughts.analyse.Accuracy
import lidraughts.common.Debouncer
import lidraughts.evaluation.{ Analysed, Assessible, PlayerAssessments }
import lidraughts.game.{ Game, GameRepo, PerfPicker, Player }
import lidraughts.game.actorApi.SimulNextGame
import lidraughts.hub.actorApi.lobby.ReloadSimuls
import lidraughts.hub.actorApi.map.Tell
import lidraughts.hub.actorApi.timeline.{ Propagate, SimulCreate, SimulJoin }
import lidraughts.hub.{ Duct, DuctMap }
import lidraughts.round.actorApi.round.{ ArbiterDraw, ArbiterResign }
import lidraughts.socket.actorApi.SendToFlag
import lidraughts.user.{ User, UserRepo }
import makeTimeout.short

final class SimulApi(
    system: ActorSystem,
    sequencers: DuctMap[_],
    onGameStart: Game.ID => Unit,
    socketMap: SocketMap,
    roundMap: lidraughts.hub.DuctMap[_],
    renderer: ActorSelection,
    timeline: ActorSelection,
    repo: SimulRepo,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val bus = system.lidraughtsBus

  def currentHostIds: Fu[Set[String]] = currentHostIdsCache.get

  def byIds = repo.byIds _

  private val currentHostIdsCache = asyncCache.single[Set[String]](
    name = "simul.currentHostIds",
    f = repo.allStarted dmap (_.map(_.hostId)(scala.collection.breakOut)),
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  private val assessmentsCache = asyncCache.multi[Game.ID, Option[PlayerAssessments]](
    name = "simul.assessments",
    f = fetchPlayerAssessments,
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  private def fetchPlayerAssessments(gameId: String): Fu[Option[PlayerAssessments]] =
    GameRepo game gameId flatMap {
      case Some(game) =>
        lidraughts.draughtsnet.Env.current.analyser.fromCache(game, true) map { analysis =>
          val analysed = Analysed(game, analysis, Player.HoldAlert.emptyMap)
          val assessWhite = Assessible(analysed, draughts.Color.White).playerAssessment
          val assessBlack = Assessible(analysed, draughts.Color.Black).playerAssessment
          PlayerAssessments(assessWhite.some, assessBlack.some).some
        }
      case _ => fufail(s"game $gameId not found")
    } recoverWith {
      case e =>
        logger.warn(s"fetchAnalysisAcpl $gameId - ${e.getMessage}")
        fuccess(none)
    }

  def getAssessments(id: Game.ID): Fu[Option[PlayerAssessments]] = assessmentsCache get id

  def create(setup: SimulForm.Setup, me: User): Fu[Simul] = {
    val simul = Simul.make(
      name = setup.name,
      clock = SimulClock(
        config = draughts.Clock.Config(setup.clockTime * 60, setup.clockIncrement),
        hostExtraTime = setup.clockExtra * 60
      ),
      variants = setup.variants.flatMap { draughts.variant.Variant(_) },
      host = me,
      color = setup.color,
      targetPct = parseIntOption(setup.targetPct),
      text = setup.text,
      team = setup.team
    )
    (repo create simul) >>- publish() >>- {
      timeline ! (Propagate(SimulCreate(me.id, simul.id, simul.fullName)) toFollowersOf me.id)
    } inject simul
  }

  def addApplicant(simulId: Simul.ID, user: User, variantKey: String): Unit = {
    WithSimul(repo.findCreated, simulId) { simul =>
      if (simul.nbAccepted >= Game.maxPlayingRealtime) simul
      else if (!simul.canJoin(user.id)) simul
      else {
        timeline ! (Propagate(SimulJoin(user.id, simul.id, simul.fullName)) toFollowersOf user.id)
        Variant(variantKey).filter(simul.variants.contains).fold(simul) { variant =>
          simul addApplicant SimulApplicant.make(
            SimulPlayer.make(
              user,
              variant,
              PerfPicker.mainOrDefault(
                speed = draughts.Speed(simul.clock.config.some),
                variant = variant,
                daysPerTurn = none
              )(user.perfs)
            )
          )
        }
      }
    }
  }

  def removeApplicant(simulId: Simul.ID, user: User): Unit = {
    WithSimul(repo.findCreated, simulId) { _ removeApplicant user.id }
  }

  def accept(simulId: Simul.ID, userId: String, v: Boolean): Unit = {
    UserRepo byId userId foreach {
      _ foreach { user =>
        WithSimul(repo.findCreated, simulId) { _.accept(user.id, v) }
      }
    }
  }

  def allow(simulId: Simul.ID, userId: String, v: Boolean): Unit = {
    UserRepo byId userId foreach {
      _ foreach { user =>
        WithSimul(repo.uniqueById, simulId) { if (v) _.allow(user.id) else _.disallow(user.id) }
      }
    }
  }

  def start(simulId: Simul.ID): Unit = {
    Sequence(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          simul.start ?? { started =>
            UserRepo byId started.hostId flatten s"No such host: ${simul.hostId}" flatMap { host =>
              started.pairings.zipWithIndex.map {
                case (p, i) => makeGame(started, host)(p, i)
              }.sequenceFu map { games =>
                games.headOption foreach {
                  case (game, _) => socketMap.tell(simul.id, actorApi.StartSimul(game, simul.hostId))
                }
                games.foldLeft(started) {
                  case (s, (g, hostColor)) => s.setPairingHostColor(g.id, hostColor)
                }
              }
            } flatMap { s =>
              bus.publish(Simul.OnStart(s), 'startSimul)
              update(s) >>- currentHostIdsCache.refresh
            }
          }
        }
      }
    }
  }

  def onPlayerConnection(game: Game, user: Option[User])(simul: Simul): Unit = {
    user.filter(simul.isHost) ifTrue simul.isRunning foreach { host =>
      repo.setHostGameId(simul, game.id)
      socketMap.tell(simul.id, actorApi.HostIsOn(game.id))
      bus.publish(
        SimulNextGame(simul.hostId, game),
        Symbol(s"simulNextGame:${simul.hostId}")
      )
    }
  }

  def abort(simulId: Simul.ID): Unit = {
    Sequence(simulId) {
      repo.findCreated(simulId) flatMap {
        _ ?? { simul =>
          (repo remove simul) >>- socketMap.tell(simul.id, actorApi.Aborted) >>- publish()
        }
      }
    }
  }

  def setText(simulId: Simul.ID, text: String): Unit = {
    Sequence(simulId) {
      repo.find(simulId) flatMap {
        _ ?? { simul =>
          repo.setText(simul, text) >>- socketReload(simulId)
        }
      }
    }
  }

  def finishGame(game: Game): Unit = game.simulId foreach { simulId =>
    Sequence(simulId) {
      repo.findStarted(simulId) flatMap {
        _ ?? { simul =>
          val simul2 = simul.updatePairing(
            game.id,
            _.finish(game.status, game.winnerUserId, game.turns)
          )
          update(simul2) >>- {
            socketStanding(simul2, game.id.some)
          } >>- {
            if (simul2.isFinished) onComplete(simul2)
          }
        }
      }
    }
  }

  private def onComplete(simul: Simul): Unit = {
    currentHostIdsCache.refresh
    system.lidraughtsBus.publish(
      lidraughts.hub.actorApi.socket.SendTo(
        simul.hostId,
        lidraughts.socket.Socket.makeMessage("simulEnd", Json.obj(
          "id" -> simul.id,
          "name" -> simul.fullName
        ))
      ),
      'socketUsers
    )
  }

  def ejectCheater(userId: String): Unit = {
    repo.allNotFinished foreach {
      _ foreach { oldSimul =>
        Sequence(oldSimul.id) {
          repo.findCreated(oldSimul.id) flatMap {
            _ ?? { simul =>
              if (simul.isUnique) funit
              else (simul ejectCheater userId) ?? { simul2 =>
                update(simul2).void
              }
            }
          }
        }
      }
    }
  }

  def settle(simulId: Simul.ID, userId: String, result: String): Unit = {
    Sequence(simulId) {
      repo.findStarted(simulId) map {
        _ ?? {
          _.pairings.find(_.player.user == userId)
        } map { pairing =>
          result match {
            case "draw" => roundMap.tell(pairing.gameId, ArbiterDraw)
            case "hostwin" => roundMap.tell(pairing.gameId, ArbiterResign(!pairing.hostColor))
            case "hostloss" => roundMap.tell(pairing.gameId, ArbiterResign(pairing.hostColor))
          }
          funit
        }
      }
    }
  }

  def idToName(id: Simul.ID): Fu[Option[String]] =
    repo find id map2 { (simul: Simul) => simul.fullName }

  private def makeGame(simul: Simul, host: User)(pairing: SimulPairing, index: Int): Fu[(Game, draughts.Color)] = for {
    user ← UserRepo byId pairing.player.user flatten s"No user with id ${pairing.player.user}"
    hostColor = simul.hostColor
    whiteUser = hostColor.fold(host, user)
    blackUser = hostColor.fold(user, host)
    clock = simul.clock.draughtsClockOf(hostColor)
    perfPicker = lidraughts.game.PerfPicker.mainOrDefault(draughts.Speed(clock.config), pairing.player.variant, none)
    game1 = Game.make(
      draughts = draughts.DraughtsGame(
        situation = draughts.Situation(pairing.player.variant),
        clock = clock.start.some
      ),
      whitePlayer = lidraughts.game.Player.make(draughts.White, whiteUser.some, perfPicker),
      blackPlayer = lidraughts.game.Player.make(draughts.Black, blackUser.some, perfPicker),
      mode = draughts.Mode.Casual,
      source = lidraughts.game.Source.Simul,
      pdnImport = None,
      drawLimit = simul.spotlight.flatMap(_.drawLimit)
    )
    game2 = game1
      .withId(pairing.gameId)
      .withSimul(simul.id, index)
      .start
    _ ← (GameRepo insertDenormalized game2) >>-
      onGameStart(game2.id) >>-
      socketMap.tell(simul.id, actorApi.StartGame(game2, simul.hostId))
  } yield game2 -> hostColor

  private def update(simul: Simul) =
    repo.update(simul) >>- socketReload(simul.id) >>- publish()

  private def WithSimul(
    finding: Simul.ID => Fu[Option[Simul]],
    simulId: Simul.ID
  )(updating: Simul => Simul): Unit = {
    Sequence(simulId) {
      finding(simulId) flatMap {
        _ ?? { simul => update(updating(simul)) }
      }
    }
  }

  private def Sequence(simulId: Simul.ID)(fu: => Funit): Unit =
    sequencers.tell(simulId, Duct.extra.LazyFu(() => fu))

  private object publish {
    private val siteMessage = SendToFlag("simul", Json.obj("t" -> "reload"))
    private val debouncer = system.actorOf(Props(new Debouncer(5 seconds, {
      (_: Debouncer.Nothing) =>
        system.lidraughtsBus.publish(siteMessage, 'sendToFlag)
        repo.allCreatedFeaturable foreach { simuls =>
          renderer ? actorApi.SimulTable(simuls) map {
            case view: String => system.lidraughtsBus.publish(ReloadSimuls(view), 'lobbySocket)
          }
        }
    })))
    def apply(): Unit = { debouncer ! Debouncer.Nothing }
  }

  private def socketReload(simulId: Simul.ID): Unit =
    socketMap.tell(simulId, actorApi.Reload)

  def processCommentary(simulId: Simul.ID, gameId: Game.ID, json: JsObject, publish: Boolean): Unit = {
    println(s"processCommentary: $publish")
    if (publish)
      socketMap.tell(simulId, actorApi.ReloadEval(gameId, json))
    assessmentsCache.refresh(gameId)
  }

  def socketStanding(simul: Simul, finishedGame: Option[String]): Unit = {
    def reqWins =
      if (simul.targetReached) 10000.some
      else if (simul.targetFailed) (-10000).some
      else simul.requiredWins
    bus.publish(
      lidraughts.hub.actorApi.round.SimulStanding(Json.obj(
        "id" -> simul.id,
        "w" -> simul.wins,
        "d" -> simul.draws,
        "l" -> simul.losses,
        "g" -> simul.ongoing,
        "r" -> simul.relativeScore
      ).add("pct" -> simul.targetPct ?? { _ => simul.winningPercentageStr.some })
        .add("tpct" -> simul.targetPct)
        .add("rw" -> reqWins)
        .add("rd" -> simul.requiredDraws)
        .add("fg" -> finishedGame)),
      Symbol(s"simul-standing-${simul.id}")
    )
  }
}
