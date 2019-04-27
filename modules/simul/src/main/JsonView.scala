package lidraughts.simul

import play.api.libs.json._
import lidraughts.common.LightUser
import lidraughts.evalCache.EvalCacheEntry.Eval
import lidraughts.game.{ Game, GameRepo }
import draughts.format.{ FEN, Forsyth }

final class JsonView(getLightUser: LightUser.Getter, isOnline: String => Boolean) {

  private def fetchGames(simul: Simul) =
    if (simul.isFinished) GameRepo gamesFromSecondary simul.gameIds
    else GameRepo gamesFromPrimary simul.gameIds

  private def fetchEvals(games: List[Game]) =
    games map { game =>
      Env.current.evalCache.getSinglePvEval(
        game.variant,
        FEN(Forsyth >> game.situation)
      ) map { (game.id, _) }
    } sequenceFu

  def apply(simul: Simul, ceval: Boolean): Fu[JsObject] = for {
    games <- fetchGames(simul)
    evals <- ceval ?? fetchEvals(games)
    lightHost <- getLightUser(simul.hostId)
    lightArbiter <- simul.arbiterId ?? getLightUser
    applicants <- simul.applicants.sortBy(p => -simul.isUnique.fold(p.player.officialRating.getOrElse(p.player.rating), p.player.rating)).map(applicantJson(simul.isUnique)).sequenceFu
    pairings <- simul.pairings.sortBy(p => -simul.isUnique.fold(p.player.officialRating.getOrElse(p.player.rating), p.player.rating)).map(pairingJson(games, simul.hostId, simul.isUnique)).sequenceFu
    allowed <- (~simul.allowed).map(allowedJson).sequenceFu
  } yield Json.obj(
    "id" -> simul.id,
    "host" -> lightHost.map { host =>
      Json.obj(
        "id" -> host.id,
        "username" -> host.name,
        "online" -> isOnline(host.id),
        "patron" -> host.isPatron,
        "title" -> host.title,
        "rating" -> simul.hostRating,
        "gameId" -> simul.hostGameId
      ).add("officialRating" -> simul.isUnique.fold(simul.hostOfficialRating, none))
    },
    "name" -> simul.name,
    "fullName" -> simul.fullName,
    "variants" -> simul.variants.map(variantJson(draughts.Speed(simul.clock.config.some))),
    "applicants" -> applicants,
    "pairings" -> pairings,
    "isCreated" -> simul.isCreated,
    "isRunning" -> simul.isRunning,
    "isFinished" -> simul.isFinished,
    "quote" -> lidraughts.quote.Quote.one(simul.id)
  ).add("arbiter" -> lightArbiter.map { arbiter =>
      Json.obj(
        "id" -> arbiter.id,
        "username" -> arbiter.name,
        "online" -> isOnline(arbiter.id),
        "patron" -> arbiter.isPatron,
        "title" -> arbiter.title
      )
    })
    .add("unique" -> simul.spotlight.map { s => true })
    .add("description" -> simul.spotlight.map { s => lidraughts.common.String.html.markdownLinks(s.description).toString })
    .add("allowed" -> (if (allowed.nonEmpty) allowed.some else none))
    .add("targetPct" -> simul.targetPct)
    .add("evals" -> ceval.fold(evals.flatMap(eval => eval._2 ?? { ev => (eval._1, ev).some }).map(evalJson).some, none))

  def arbiterJson(simul: Simul): Fu[JsArray] = for {
    games <- fetchGames(simul)
    evals <- simul.hasCeval ?? fetchEvals(games)
  } yield JsArray(simul.pairings.map(pairing => {
    val game = games.find(_.id == pairing.gameId)
    val clock = game.flatMap(_.clock)
    Json.obj(
      "id" -> pairing.player.user
    ).add("blurs" -> game.map(_.playerBlurPercent(!pairing.hostColor)))
      .add("clock" -> clock.map(_.remainingTime(!pairing.hostColor).roundSeconds))
      .add("hostClock" -> clock.map(_.remainingTime(pairing.hostColor).roundSeconds))
      .add("turnColor" -> game.map(_.turnColor.name))
      .add("ceval" -> evals.find(_._1 == pairing.gameId).flatMap(eval => eval._2 ?? { ev => (eval._1, ev).some }).map(evalJson))
  }))

  private def variantJson(speed: draughts.Speed)(v: draughts.variant.Variant) = Json.obj(
    "key" -> v.key,
    "icon" -> lidraughts.game.PerfPicker.perfType(speed, v, none).map(_.iconChar.toString),
    "name" -> v.name
  )

  private def playerJson(player: SimulPlayer, unique: Boolean): Fu[JsObject] =
    getLightUser(player.user) map { light =>
      Json.obj(
        "id" -> player.user,
        "online" -> isOnline(player.user),
        "variant" -> player.variant.key,
        "rating" -> player.rating
      ).add("username" -> light.map(_.name))
        .add("title" -> light.map(_.title))
        .add("provisional" -> player.provisional.filter(identity))
        .add("patron" -> light.??(_.isPatron))
        .add("officialRating" -> unique.fold(player.officialRating, none))
    }

  private def allowedJson(userId: String): Fu[JsObject] =
    getLightUser(userId) map { light =>
      Json.obj(
        "id" -> userId,
        "online" -> isOnline(userId)
      ).add("username" -> light.map(_.name))
        .add("title" -> light.map(_.title))
        .add("patron" -> light.??(_.isPatron))
    }

  private def applicantJson(unique: Boolean)(app: SimulApplicant): Fu[JsObject] =
    playerJson(app.player, unique) map { player =>
      Json.obj(
        "player" -> player,
        "accepted" -> app.accepted
      )
    }

  private def gameJson(hostId: String)(g: Game) = Json.obj(
    "id" -> g.id,
    "status" -> g.status.id,
    "fen" -> (draughts.format.Forsyth exportBoard g.board),
    "lastMove" -> ~g.lastMoveKeys,
    "orient" -> g.playerByUserId(hostId).map(_.color)
  )

  private def evalJson(eval: (Game.ID, Eval)) = {
    val pv = eval._2.pvs.head
    Json.obj(
      "id" -> eval._1,
      "moves" -> pv.moves.value.toList.mkString(" "),
      "depth" -> eval._2.depth
    ).add("cp", pv.score.cp.map(_.value))
      .add("win", pv.score.win.map(_.value))
  }

  private def pairingJson(games: List[Game], hostId: String, unique: Boolean)(p: SimulPairing): Fu[JsObject] =
    playerJson(p.player, unique) map { player =>
      Json.obj(
        "player" -> player,
        "hostColor" -> p.hostColor,
        "winnerColor" -> p.winnerColor,
        "wins" -> p.wins, // can't be normalized because BC
        "game" -> games.find(_.id == p.gameId).map(gameJson(hostId))
      )
    }

  private implicit val colorWriter: Writes[draughts.Color] = Writes { c =>
    JsString(c.name)
  }
}
