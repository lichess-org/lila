package lila.simul

import play.api.libs.json._

import lila.common.LightUser
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(getLightUser: LightUser.Getter) {

  private def fetchGames(simul: Simul) =
    if (simul.isFinished) GameRepo gamesFromSecondary simul.gameIds
    else GameRepo gamesFromPrimary simul.gameIds

  def apply(simul: Simul, team: Option[SimulTeam]): Fu[JsObject] = for {
    games <- fetchGames(simul)
    lightHost <- getLightUser(simul.hostId)
    applicants <- simul.applicants.sortBy(-_.player.rating).map(applicantJson).sequenceFu
    pairingOptions <- simul.pairings
      .sortBy(-_.player.rating)
      .map(pairingJson(games, simul.hostId))
      .sequenceFu
    pairings = pairingOptions.flatten
  } yield Json.obj(
    "id" -> simul.id,
    "host" -> lightHost.map { host =>
      Json.obj(
        "id" -> host.id,
        "username" -> host.name,
        "patron" -> host.isPatron,
        "title" -> host.title,
        "rating" -> simul.hostRating,
        "gameId" -> simul.hostGameId
      )
    },
    "name" -> simul.name,
    "fullName" -> simul.fullName,
    "variants" -> simul.variants.map(variantJson(chess.Speed(simul.clock.config.some))),
    "applicants" -> applicants,
    "pairings" -> pairings,
    "isCreated" -> simul.isCreated,
    "isRunning" -> simul.isRunning,
    "isFinished" -> simul.isFinished,
    "quote" -> lila.quote.Quote.one(simul.id),
    "text" -> simul.text
  ).add("team", team)

  private def variantJson(speed: chess.Speed)(v: chess.variant.Variant) = Json.obj(
    "key" -> v.key,
    "icon" -> lila.game.PerfPicker.perfType(speed, v, none).map(_.iconChar.toString),
    "name" -> v.name
  )

  private def playerJson(player: SimulPlayer): Fu[JsObject] =
    getLightUser(player.user) map { light =>
      Json.obj(
        "id" -> player.user,
        "variant" -> player.variant.key,
        "rating" -> player.rating
      ).add("username" -> light.map(_.name))
        .add("title" -> light.map(_.title))
        .add("provisional" -> player.provisional.filter(identity))
        .add("patron" -> light.??(_.isPatron))
    }

  private def applicantJson(app: SimulApplicant): Fu[JsObject] =
    playerJson(app.player) map { player =>
      Json.obj(
        "player" -> player,
        "accepted" -> app.accepted
      )
    }

  private def gameJson(hostId: User.ID, g: Game) = Json.obj(
    "id" -> g.id,
    "status" -> g.status.id,
    "fen" -> (chess.format.Forsyth exportBoard g.board),
    "lastMove" -> ~g.lastMoveKeys,
    "orient" -> g.playerByUserId(hostId).map(_.color)
  )

  private def pairingJson(games: List[Game], hostId: String)(p: SimulPairing): Fu[Option[JsObject]] =
    games.find(_.id == p.gameId) ?? { game =>
      playerJson(p.player) map { player =>
        Json.obj(
          "player" -> player,
          "hostColor" -> p.hostColor,
          "winnerColor" -> p.winnerColor,
          "wins" -> p.wins, // can't be normalized because BC
          "game" -> gameJson(hostId, game)
        ).some
      }
    }

  private implicit val colorWriter: Writes[chess.Color] = Writes { c =>
    JsString(c.name)
  }

  private implicit val simulTeamWriter = Json.writes[SimulTeam]
}
