package lila.simul

import chess.variant.Variant
import chess.{ Clock, Status }
import reactivemongo.api.bson.*

import lila.core.game.GameRepo
import lila.db.BSON
import lila.db.dsl.{ *, given }

final private[simul] class SimulRepo(val coll: Coll, gameRepo: GameRepo)(using Executor):

  import gameRepo.given
  private given BSONHandler[SimulStatus] = tryHandler(
    { case BSONInteger(v) => SimulStatus(v).toTry(s"No such simul status: $v") },
    x => BSONInteger(x.id)
  )
  private given BSONHandler[Variant] = variantByIdHandler
  private given BSONDocumentHandler[Clock.Config] = Macros.handler
  private given BSONDocumentHandler[SimulClock] = Macros.handler
  private given BSONDocumentHandler[SimulPlayer] = Macros.handler
  private given BSONDocumentHandler[SimulApplicant] = Macros.handler
  private given BSON[SimulPairing] with
    def reads(r: BSON.Reader) =
      SimulPairing(
        player = r.get[SimulPlayer]("player"),
        gameId = r.get[GameId]("gameId"),
        status = r.get[Status]("status"),
        wins = r.boolO("wins"),
        hostColor = r.strO("hostColor").flatMap(Color.fromName) | chess.White
      )
    def writes(w: BSON.Writer, o: SimulPairing) =
      $doc(
        "player" -> o.player,
        "gameId" -> o.gameId,
        "status" -> o.status,
        "wins" -> o.wins,
        "hostColor" -> o.hostColor.name
      )

  import SimulCondition.bsonHandler

  private given BSONDocumentHandler[Simul] = Macros.handler

  private val createdSelect = $doc("status" -> SimulStatus.Created.id)
  private val startedSelect = $doc("status" -> SimulStatus.Started.id)
  private val finishedSelect = $doc("status" -> SimulStatus.Finished.id)
  private val createdSort = $sort.desc("createdAt")

  def find(id: SimulId): Fu[Option[Simul]] =
    coll.byId[Simul](id)

  def byIds(ids: List[SimulId]): Fu[List[Simul]] =
    coll.byIds[Simul, SimulId](ids)

  def exists(id: SimulId): Fu[Boolean] =
    coll.exists($id(id))

  def findStarted(id: SimulId): Fu[Option[Simul]] =
    find(id).map(_.filter(_.isStarted))

  def findCreated(id: SimulId): Fu[Option[Simul]] =
    find(id).map(_.filter(_.isCreated))

  def findPending(hostId: UserId): Fu[List[Simul]] =
    coll.list[Simul](createdSelect ++ $doc("hostId" -> hostId))

  def byTeamLeaders[U: UserIdOf](teamId: TeamId, hosts: Seq[U]): Fu[List[Simul]] =
    coll
      .find(createdSelect ++ $doc("hostId".$in(hosts.map(_.id)), "team" -> teamId))
      .hint(coll.hint($doc("hostId" -> 1)))
      .cursor[Simul]()
      .listAll()

  def byHostAdapter(hostId: UserId) =
    lila.db.paginator.Adapter[Simul](
      collection = coll,
      selector = finishedSelect ++ $doc("hostId" -> hostId),
      projection = none,
      sort = createdSort,
      _.sec
    )

  def hostId(id: SimulId): Fu[Option[UserId]] =
    coll.primitiveOne[UserId]($id(id), "hostId")

  def countByHost(hostId: UserId) = coll.countSel($doc("hostId" -> hostId))

  private val featurableSelect = $doc("featurable" -> true)

  def allCreatedFeaturable: Fu[List[Simul]] =
    coll
      .find(
        // hits partial index hostSeenAt_-1
        createdSelect ++ featurableSelect ++ $doc(
          "hostSeenAt".$gte(nowInstant.minusSeconds(12)),
          "createdAt".$gte(nowInstant.minusHours(1))
        )
      )
      .sort(createdSort)
      .hint(coll.hint($doc("hostSeenAt" -> -1)))
      .cursor[Simul]()
      .list(50)
      .map:
        _.foldLeft(List.empty[Simul]) {
          case (acc, sim) if acc.exists(_.hostId == sim.hostId) => acc
          case (acc, sim) => sim :: acc
        }.reverse

  def allStarted: Fu[List[Simul]] =
    coll
      .find(startedSelect)
      .sort(createdSort)
      .cursor[Simul]()
      .list(50)

  def allFinishedFeaturable(max: Int): Fu[List[Simul]] =
    coll
      .find(finishedSelect ++ featurableSelect)
      .sort($sort.desc("finishedAt"))
      .cursor[Simul]()
      .list(max)

  def allNotFinished =
    coll.list[Simul]($doc("status".$ne(SimulStatus.Finished.id)))

  def create(simul: Simul): Funit =
    coll.insert.one(simul).void

  def update(simul: Simul) =
    coll.update
      .one(
        $id(simul.id),
        $set(bsonWriteObjTry[Simul](simul).get) ++
          simul.estimatedStartAt.isEmpty.so($unset("estimatedStartAt"))
      )
      .void

  def remove(simul: Simul) =
    coll.delete.one($id(simul.id)).void

  def setHostGameId(simul: Simul, gameId: GameId) =
    coll.update
      .one(
        $id(simul.id),
        $set("hostGameId" -> gameId)
      )
      .void

  def setHostSeenNow(simul: Simul) =
    coll.update
      .one(
        $id(simul.id),
        $set("hostSeenAt" -> nowInstant)
      )
      .void

  def setText(simul: Simul, text: String) =
    coll.update
      .one(
        $id(simul.id),
        $set("text" -> text)
      )
      .void

  def cleanup =
    coll.delete.one(
      createdSelect ++ $doc(
        "createdAt" -> $doc("$lt" -> (nowInstant.minusMinutes(60)))
      )
    )

  private[simul] def anonymizeHost(id: UserId) =
    coll.update.one($doc("hostId" -> id), $set("hostId" -> UserId.ghost), multi = true)

  private[simul] def anonymizePlayers(id: UserId) =
    coll.update.one(
      $doc("pairings.player.user" -> id),
      $set("pairings.$.player.user" -> UserId.ghost),
      multi = true
    )
