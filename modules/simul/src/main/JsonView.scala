package lila.simul

import play.api.libs.json._

import lila.common.LightUser
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(
    gameRepo: GameRepo,
    getLightUser: LightUser.Getter,
    proxyRepo: lila.round.GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val colorWriter: Writes[chess.Color] = Writes { c =>
    JsString(c.name)
  }

  implicit private val simulTeamWriter = Json.writes[SimulTeam]

  private def fetchGames(simul: Simul) =
    if (simul.isFinished) gameRepo gamesFromSecondary simul.gameIds
    else simul.gameIds.map(proxyRepo.game).sequenceFu.dmap(_.flatten)

  def apply(simul: Simul, team: Option[SimulTeam]): Fu[JsObject] =
    for {
      games      <- fetchGames(simul)
      lightHost  <- getLightUser(simul.hostId)
      applicants <- simul.applicants.sortBy(-_.player.rating).map(applicantJson).sequenceFu
      pairingOptions <-
        simul.pairings
          .sortBy(-_.player.rating)
          .map(pairingJson(games, simul.hostId))
          .sequenceFu
      pairings = pairingOptions.flatten
    } yield baseSimul(simul, lightHost) ++ Json
      .obj(
        "applicants" -> applicants,
        "pairings"   -> pairings
      )
      .add("team", team)
      .add("quote" -> simul.isCreated.option(lila.quote.Quote.one(simul.id)))

  def api(simul: Simul): Fu[JsObject] =
    getLightUser(simul.hostId) map { lightHost =>
      baseSimul(simul, lightHost) ++ Json.obj(
        "nbApplicants" -> simul.applicants.size,
        "nbPairings"   -> simul.pairings.size
      )
    }

  def api(simuls: List[Simul]): Fu[JsArray] =
    simuls.map(api).sequenceFu map JsArray.apply

  def apiAll(
      pending: List[Simul],
      created: List[Simul],
      started: List[Simul],
      finished: List[Simul]
  ): Fu[JsObject] =
    for {
      pendingJson  <- api(pending)
      createdJson  <- api(created)
      startedJson  <- api(started)
      finishedJson <- api(finished)
    } yield Json.obj(
      "pending"  -> pendingJson,
      "created"  -> createdJson,
      "started"  -> startedJson,
      "finished" -> finishedJson
    )

  private def baseSimul(simul: Simul, lightHost: Option[LightUser]) =
    Json.obj(
      "id" -> simul.id,
      "host" -> lightHost.map { host =>
        Json
          .obj(
            "id"     -> host.id,
            "name"   -> host.name,
            "rating" -> simul.hostRating
          )
          .add("gameId" -> simul.hostGameId.ifTrue(simul.isRunning))
          .add("title" -> host.title)
          .add("patron" -> host.isPatron)
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
      "icon" -> lila.game.PerfPicker.perfType(speed, v, none).map(_.iconChar.toString),
      "name" -> v.name
    )

  private def playerJson(player: SimulPlayer): Fu[JsObject] =
    getLightUser(player.user) map { light =>
      Json
        .obj(
          "id"     -> player.user,
          "rating" -> player.rating
        )
        .add("name" -> light.map(_.name))
        .add("title" -> light.map(_.title))
        .add("provisional" -> ~player.provisional)
        .add("patron" -> light.??(_.isPatron))
    }

  private def applicantJson(app: SimulApplicant): Fu[JsObject] =
    playerJson(app.player) map { player =>
      Json.obj(
        "player"   -> player,
        "variant"  -> app.player.variant.key,
        "accepted" -> app.accepted
      )
    }

  private def gameJson(hostId: User.ID, g: Game) =
    Json
      .obj(
        "id"       -> g.id,
        "status"   -> g.status.id,
        "fen"      -> (chess.format.Forsyth boardAndColor g.situation),
        "lastMove" -> ~g.lastMoveKeys,
        "orient"   -> g.playerByUserId(hostId).map(_.color)
      )
      .add(
        "clock" -> g.clock.ifTrue(g.isBeingPlayed).map { c =>
          Json.obj(
            "white" -> c.remainingTime(chess.White).roundSeconds,
            "black" -> c.remainingTime(chess.Black).roundSeconds
          )
        }
      )
      .add("winner" -> g.winnerColor.map(_.name))

  private def pairingJson(games: List[Game], hostId: String)(p: SimulPairing): Fu[Option[JsObject]] =
    games.find(_.id == p.gameId) ?? { game =>
      playerJson(p.player) map { player =>
        Json
          .obj(
            "player"    -> player,
            "variant"   -> p.player.variant.key,
            "hostColor" -> p.hostColor,
            "game"      -> gameJson(hostId, game)
          )
          .some
      }
    }
}
