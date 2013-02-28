package lila.app
package tournament

import game.{ GameRepo, DbGame }
import user.User
import core.Settings
import security.Flood
import lobby.{ Socket ⇒ LobbySocket }
import socket.History
import memo.MonoMemo

import scalastic.elasticsearch
import com.mongodb.casbah.MongoCollection
import scalaz.effects._
import akka.actor.Props
import play.api.libs.concurrent._
import play.api.Application

final class TournamentEnv(
    app: Application,
    settings: Settings,
    getUser: String ⇒ IO[Option[User]],
    gameRepo: GameRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    flood: Flood,
    siteSocket: site.Socket,
    lobbySocket: LobbySocket,
    roundMeddler: round.Meddler,
    incToints: String ⇒ Int ⇒ IO[Unit],
    mongodb: String ⇒ MongoCollection) {

  implicit val ctx = app
  import settings._

  lazy val forms = new DataForm(IsDev)

  lazy val repo = new TournamentRepo(
    collection = mongodb(TournamentCollectionTournament))

  lazy val api = new TournamentApi(
    repo = repo,
    roomRepo = roomRepo,
    joiner = joiner,
    socket = socket,
    siteSocket = siteSocket,
    lobbySocket = lobbySocket,
    roundMeddler = roundMeddler,
    incToints = incToints)

  lazy val roomRepo = new RoomRepo(
    collection = mongodb(TournamentCollectionRoom)
  )

  lazy val messenger = new Messenger(
    roomRepo = roomRepo,
    getTournament = repo.byId,
    getUser = getUser,
    netDomain = NetDomain)

  lazy val socket = new Socket(
    getTournament = repo.byId,
    hubMaster = hubMaster,
    messenger = messenger,
    flood = flood)

  lazy val history = () ⇒ new History(timeout = TournamentMessageLifetime)

  lazy val hubMaster = Akka.system.actorOf(Props(new HubMaster(
    makeHistory = history,
    messenger = messenger,
    uidTimeout = TournamentUidTimeout,
    hubTimeout = TournamentHubTimeout
  )), name = ActorTournamentHubMaster)

  lazy val organizer = Akka.system.actorOf(Props(new Organizer(
    repo = repo,
    api = api,
    reminder = reminder,
    hubMaster = hubMaster
  )), name = ActorTournamentOrganizer)

  lazy val reminder = Akka.system.actorOf(Props(new Reminder(List(
    ActorLobbyHub, ActorSiteHub, ActorRoundHubMaster, ActorTournamentHubMaster
  ))), name = ActorTournamentReminder)

  private lazy val joiner = new GameJoiner(
    gameRepo = gameRepo,
    roundMeddler = roundMeddler,
    timelinePush = timelinePush,
    getUser = getUser)
}
