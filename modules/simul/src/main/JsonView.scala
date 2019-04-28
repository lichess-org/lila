package lidraughts.simul

import play.api.libs.json._

import draughts.format.{ FEN, Forsyth }
import lidraughts.common.LightUser
import lidraughts.evalCache.JsonHandlers.gameEvalJson
import lidraughts.evaluation.{ Display, PlayerAssessment }
import lidraughts.game.{ Game, GameRepo, Rewind }
import lidraughts.pref.Pref
import lidraughts.user.User

import Simul.ShowFmjdRating

final class JsonView(getLightUser: LightUser.Getter, isOnline: String => Boolean) {

  private def fetchGames(simul: Simul) =
    if (simul.isFinished) GameRepo gamesFromSecondary simul.gameIds
    else GameRepo gamesFromPrimary simul.gameIds

  private def fetchEvals(games: List[Game]) =
    games map { game =>
      Rewind.rewindCapture(game) flatMap { situation =>
        Env.current.evalCache.getSinglePvEval(
          game.variant,
          FEN(Forsyth >> situation)
        )
      } map { (game.id, _) }
    } sequenceFu

  private def fetchAssessments(games: List[Game]) =
    games map { game =>
      if (game.turns > 5)
        Env.current.api.getAssessments(game.id) map { (game.id, _) }
      else
        fuccess(game.id -> none)
    } sequenceFu

  def apply(simul: Simul, ceval: Boolean, pref: Option[Pref]): Fu[JsObject] = for {
    games <- fetchGames(simul)
    evals <- ceval ?? fetchEvals(games)
    lightHost <- getLightUser(simul.hostId)
    lightArbiter <- simul.arbiterId ?? getLightUser
    applicants <- simul.applicants.sortBy(p => -(if (simul.hasFmjd) p.player.officialRating.getOrElse(p.player.rating) else p.player.rating)).map(applicantJson(simul.spotlight.flatMap(_.fmjdRating))).sequenceFu
    pairingOptions <- simul.pairings
      .sortBy(p => -(if (simul.hasFmjd) p.player.officialRating.getOrElse(p.player.rating) else p.player.rating))
      .map(pairingJson(games, simul.hostId, simul.spotlight.flatMap(_.fmjdRating)))
      .sequenceFu
    pairings = pairingOptions.flatten
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
      ).add("officialRating" -> (if (simul.hasFmjd) simul.hostOfficialRating else none))
    },
    "name" -> simul.name,
    "fullName" -> simul.fullName,
    "variants" -> simul.variants.map(variantJson(draughts.Speed(simul.clock.config.some))),
    "applicants" -> applicants,
    "pairings" -> pairings,
    "isCreated" -> simul.isCreated,
    "isRunning" -> simul.isRunning,
    "isFinished" -> simul.isFinished,
    "quote" -> lidraughts.quote.Quote.one(simul.id),
    "text" -> simul.text
  ).add("arbiter" -> lightArbiter.map { arbiter =>
      Json.obj(
        "id" -> arbiter.id,
        "username" -> arbiter.name,
        "online" -> isOnline(arbiter.id),
        "patron" -> arbiter.isPatron,
        "title" -> arbiter.title,
        "hidden" -> simul.spotlight.flatMap(_.arbiterHidden)
      )
    })
    .add("unique" -> simul.spotlight.map { s => true })
    .add("description" -> simul.spotlight.map { s => lidraughts.common.String.html.markdownLinks(s.description).toString })
    .add("allowed" -> allowed.nonEmpty ?? allowed.some)
    .add("targetPct" -> simul.targetPct)
    .add("evals" -> ceval ?? evals.flatMap(eval => eval._2 ?? { ev => gameEvalJson(eval._1, ev).some }).some)
    .add("pref" -> pref.map(p => Json.obj(
      "draughtsResult" -> (p.gameResult == Pref.GameResult.DRAUGHTS)
    )))

  def arbiterJson(simul: Simul): Fu[JsArray] = for {
    games <- fetchGames(simul)
    evals <- simul.hasCeval ?? fetchEvals(games)
    assessments <- simul.hasCeval ?? fetchAssessments(games)
  } yield JsArray(simul.pairings.map(pairing => {
    def assessment = assessments.find(_._1 == pairing.gameId)
    def eval = evals.find(_._1 == pairing.gameId)
    val game = games.find(_.id == pairing.gameId)
    val clock = game.flatMap(_.clock)
    val playerColor = !pairing.hostColor
    Json.obj(
      "id" -> pairing.player.user
    ).add("officialRating" -> pairing.player.officialRating)
      .add("turnColor" -> game.map(_.turnColor.name))
      .add("lastMove" -> game.flatMap(_.lastMovePdn))
      .add("clock" -> clock.map(_.remainingTime(playerColor).roundSeconds))
      .add("hostClock" -> clock.map(_.remainingTime(!playerColor).roundSeconds))
      .add("ceval" -> eval.flatMap(_._2 ?? { gameEvalJson(pairing.gameId, _).some }))
      .add("assessment" -> game.??(_.playedTurns > 5) ?? assessment.flatMap(_._2 ?? { _.color(playerColor).map(assessmentJson) }))
      .add("drawReason" -> game.flatMap(drawReason))
  }))

  private def drawReason(game: Game) = game.status match {
    case draughts.Status.Draw =>
      if (game.situation.threefoldRepetition) "repetition".some
      else if (game.situation.autoDraw) "autodraw".some
      else "agreement".some
    case _ => none
  }

  def evalWithGame(simul: Simul, gameId: Game.ID, eval: JsObject) =
    GameRepo.game(gameId) map { game =>
      eval.add("game" -> game ?? { g => gameJson(simul.hostId, g).some })
    }

  private def assessmentJson(assessment: PlayerAssessment) = {
    val scanSig = Display.scanSig(assessment)
    val mtSig = Display.moveTimeSig(assessment)
    val blurSig = Display.blurSig(assessment)
    Json.obj(
      "scanAvg" -> assessment.sfAvg,
      "scanSd" -> assessment.sfSd,
      "scanSig" -> scanSig,
      "scanSort" -> (scanSig + 0.9999 - Math.min(9998, assessment.sfAvg) / 10000.0),
      "mtAvg" -> assessment.mtAvg,
      "mtSd" -> assessment.mtSd,
      "mtSig" -> mtSig,
      "mtSort" -> (mtSig + 0.9999 - Math.min(9998, assessment.mtAvg) / 10000.0),
      "blurPct" -> assessment.blurs,
      "blurSig" -> blurSig,
      "blurSort" -> (blurSig + assessment.blurs / 1000.0),
      "totalSig" -> assessment.assessment.id,
      "totalTxt" -> assessment.assessment.description
    )
  }

  private def variantJson(speed: draughts.Speed)(v: draughts.variant.Variant) = Json.obj(
    "key" -> v.key,
    "icon" -> lidraughts.game.PerfPicker.perfType(speed, v, none).map(_.iconChar.toString),
    "name" -> v.name
  )

  private def playerJson(player: SimulPlayer, fmjd: Option[ShowFmjdRating]): Fu[JsObject] = {
    val rating = fmjd match {
      case Some(ShowFmjdRating.Always) => none
      case _ => player.rating.some
    }
    val officialRating = fmjd ?? {
      case ShowFmjdRating.Never => none
      case _ => player.officialRating
    }
    getLightUser(player.user) map { light =>
      Json.obj(
        "id" -> player.user,
        "online" -> isOnline(player.user),
        "variant" -> player.variant.key
      ).add("username" -> light.map(_.name))
        .add("title" -> light.map(_.title))
        .add("provisional" -> player.provisional.filter(identity))
        .add("patron" -> light.??(_.isPatron))
        .add("rating" -> rating)
        .add("officialRating" -> officialRating)
    }
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

  private def applicantJson(fmjd: Option[ShowFmjdRating])(app: SimulApplicant): Fu[JsObject] =
    playerJson(app.player, fmjd) map { player =>
      Json.obj(
        "player" -> player,
        "accepted" -> app.accepted
      )
    }

  private def gameJson(hostId: User.ID, g: Game) = Json.obj(
    "id" -> g.id,
    "status" -> g.status.id,
    "fen" -> (draughts.format.Forsyth exportBoard g.board),
    "lastMove" -> ~g.lastMoveKeys,
    "orient" -> g.playerByUserId(hostId).map(_.color)
  )

  private def pairingJson(games: List[Game], hostId: String, fmjd: Option[ShowFmjdRating])(p: SimulPairing): Fu[Option[JsObject]] =
    games.find(_.id == p.gameId) ?? { game =>
      playerJson(p.player, fmjd) map { player =>
        Json.obj(
          "player" -> player,
          "hostColor" -> p.hostColor,
          "winnerColor" -> p.winnerColor,
          "wins" -> p.wins, // can't be normalized because BC
          "game" -> gameJson(hostId, game)
        ).some
      }
    }

  private implicit val colorWriter: Writes[draughts.Color] = Writes { c =>
    JsString(c.name)
  }
}
