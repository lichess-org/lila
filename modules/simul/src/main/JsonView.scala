package lila.simul

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.LightUser
import lila.gathering.Condition.WithVerdicts

final class JsonView(
    gameRepo: lila.core.game.GameRepo,
    getLightUser: LightUser.GetterFallback,
    gameProxy: lila.core.game.GameProxy,
    isOnline: lila.core.socket.IsOnline
)(using Executor):

  private def fetchGames(simul: Simul): Fu[List[Game]] =
    if simul.isFinished then gameRepo.gamesFromSecondary(simul.gameIds)
    else simul.gameIds.parallel(gameProxy.game).dmap(_.flatten)

  def apply(simul: Simul, verdicts: WithVerdicts): Fu[JsObject] = for
    games      <- fetchGames(simul)
    lightHost  <- getLightUser(simul.hostId)
    applicants <- simul.applicants.sortBy(-_.player.rating.value).sequentially(applicantJson)
    pairingOptions <-
      simul.pairings
        .sortBy(-_.player.rating.value)
        .parallel(pairingJson(games, simul.hostId))
    pairings = pairingOptions.flatten
  yield baseSimul(simul, lightHost) ++ Json
    .obj(
      "canJoin"    -> verdicts.accepted,
      "applicants" -> applicants,
      "pairings"   -> pairings
    )
    .add("quote" -> simul.isCreated.option(lila.quote.Quote.one(simul.id.value)))

  def apiJson(simul: Simul): Fu[JsObject] =
    getLightUser(simul.hostId).map: lightHost =>
      baseSimul(simul, lightHost) ++ Json
        .obj(
          "nbApplicants" -> simul.applicants.size,
          "nbPairings"   -> simul.pairings.size
        )
        .add("estimatedStartAt" -> simul.startedAt)
        .add("startedAt" -> simul.startedAt)
        .add("finishedAt" -> simul.finishedAt)

  def api(simuls: List[Simul]): Fu[JsArray] =
    simuls.sequentially(apiJson).map(JsArray.apply)

  def apiAll(
      pending: List[Simul],
      created: List[Simul],
      started: List[Simul],
      finished: List[Simul]
  ): Fu[JsObject] =
    for
      pendingJson  <- api(pending)
      createdJson  <- api(created)
      startedJson  <- api(started)
      finishedJson <- api(finished)
    yield Json.obj(
      "pending"  -> pendingJson,
      "created"  -> createdJson,
      "started"  -> startedJson,
      "finished" -> finishedJson
    )

  private def baseSimul(simul: Simul, host: LightUser) =
    Json.obj(
      "id" -> simul.id,
      "host" -> {
        Json.toJsObject(host) ++ Json
          .obj("rating" -> simul.hostRating)
          .add("provisional" -> simul.hostProvisional)
          .add("gameId" -> simul.hostGameId.ifTrue(simul.isRunning))
          .add("online" -> isOnline.exec(host.id))
      },
      "name"       -> simul.name,
      "fullName"   -> simul.fullName,
      "variants"   -> simul.variants.map(variantJson(chess.Speed(simul.clock.config.some))),
      "isCreated"  -> simul.isCreated,
      "isRunning"  -> simul.isRunning,
      "isFinished" -> simul.isFinished,
      "text"       -> simul.text
    )

  private def variantJson(speed: chess.Speed)(v: chess.variant.Variant) =
    Json.obj(
      "key"  -> v.key,
      "icon" -> lila.rating.PerfType(v, speed).icon.toString,
      "name" -> v.name
    )

  private def playerJson(player: SimulPlayer): Fu[JsObject] =
    getLightUser(player.user).map: light =>
      Json.toJsObject(light) ++
        Json
          .obj("rating" -> player.rating)
          .add("provisional" -> ~player.provisional)

  private def applicantJson(app: SimulApplicant): Fu[JsObject] =
    playerJson(app.player).map { player =>
      Json.obj(
        "player"   -> player,
        "variant"  -> app.player.variant.key,
        "accepted" -> app.accepted
      )
    }

  private def gameJson(hostId: UserId, g: Game) =
    Json
      .obj(
        "id"       -> g.id,
        "status"   -> g.status.id,
        "fen"      -> chess.format.Fen.writeBoardAndColor(g.situation),
        "lastMove" -> (g.lastMoveKeys.orZero: String),
        "orient"   -> g.player(hostId).map(_.color)
      )
      .add(
        "clock" -> g.clock
          .ifTrue(g.isBeingPlayed)
          .map: c =>
            Json.obj(
              "white" -> c.remainingTime(chess.White).roundSeconds,
              "black" -> c.remainingTime(chess.Black).roundSeconds
            )
      )
      .add("winner" -> g.winnerColor.map(_.name))

  private def pairingJson(games: List[Game], hostId: UserId)(p: SimulPairing): Fu[Option[JsObject]] =
    games
      .find(_.id == p.gameId)
      .so: game =>
        playerJson(p.player).map: player =>
          Json
            .obj(
              "player"    -> player,
              "variant"   -> p.player.variant.key,
              "hostColor" -> p.hostColor,
              "game"      -> gameJson(hostId, game)
            )
            .some
