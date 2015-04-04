package lila.simul

import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }

final class JsonView(
    getLightUser: String => Option[LightUser]) {

  def apply(simul: Simul): Fu[JsObject] =
    GameRepo.games(simul.gameIds) map { games =>
      val lightHost = getLightUser(simul.hostId)
      Json.obj(
        "id" -> simul.id,
        "host" -> lightHost.map { host =>
          Json.obj(
            "id" -> host.id,
            "name" -> host.name,
            "title" -> host.title,
            "rating" -> simul.hostRating)
        },
        "name" -> simul.name,
        "fullName" -> simul.fullName,
        "variants" -> simul.variants.map(variantJson(chess.Speed(simul.clock.chessClock.some))),
        "applicants" -> simul.applicants.sortBy(-_.player.rating).map(applicantJson),
        "pairings" -> simul.pairings.sortBy(-_.player.rating).map(pairingJson(games)),
        "isCreated" -> simul.isCreated,
        "isRunning" -> simul.isRunning,
        "isFinished" -> simul.isFinished)
    }

  private def variantJson(speed: chess.Speed)(v: chess.variant.Variant) = Json.obj(
    "key" -> v.key,
    "icon" -> lila.game.PerfPicker.perfType(speed, v, none).map(_.iconChar.toString),
    "name" -> v.name)

  private def playerJson(player: SimulPlayer) = {
    val light = getLightUser(player.user)
    Json.obj(
      "id" -> player.user,
      "variant" -> player.variant.key,
      "username" -> light.map(_.name),
      "title" -> light.map(_.title),
      "rating" -> player.rating
    ).noNull
  }

  private def applicantJson(app: SimulApplicant) = Json.obj(
    "player" -> playerJson(app.player),
    "accepted" -> app.accepted)

  private def gameJson(g: Game) = Json.obj(
    "id" -> g.id,
    "status" -> g.status.id,
    "fen" -> (chess.format.Forsyth exportBoard g.toChess.board),
    "lastMove" -> ~g.castleLastMoveTime.lastMoveString)

  private def pairingJson(games: List[Game])(p: SimulPairing) = Json.obj(
    "player" -> playerJson(p.player),
    "wins" -> p.wins,
    "game" -> games.find(_.id == p.gameId).map(gameJson)
  )
}
