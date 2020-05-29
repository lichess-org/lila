package lidraughts.simul

import play.api.libs.json._

import draughts.format.{ FEN, Forsyth }
import lidraughts.common.LightUser
import lidraughts.evalCache.JsonHandlers.gameEvalJson
import lidraughts.evaluation.{ Display, PlayerAssessment }
import lidraughts.game.JsonView.boardSizeWriter
import lidraughts.game.{ Game, GameRepo, Rewind }
import lidraughts.pref.Pref
import lidraughts.user.User
import Simul.ShowFmjdRating

final class JsonView(
    getLightUser: LightUser.Getter,
    isOnline: String => Boolean,
    proxyGame: Game.ID => Fu[Option[Game]]
) {

  private def fetchGames(simul: Simul) =
    if (simul.isFinished) GameRepo gamesFromSecondary simul.gameIds
    else simul.gameIds.map(proxyGame).sequenceFu.map(_.flatten)

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

  def apply(simul: Simul, ceval: Boolean, pref: Option[Pref], team: Option[SimulTeam]): Fu[JsObject] = for {
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
  } yield baseSimul(simul, lightHost, lightArbiter) ++ Json.obj(
    "applicants" -> applicants,
    "pairings" -> pairings
  ).add("team", team)
    .add("quote" -> simul.isCreated.option(lidraughts.quote.Quote.one(simul.id)))
    .add("allowed" -> allowed.nonEmpty ?? allowed.some)
    .add("evals" -> ceval ?? evals.flatMap(eval => eval._2 ?? { ev => gameEvalJson(eval._1, ev).some }).some)
    .add("pref" -> pref.map { p =>
      Json.obj("draughtsResult" -> p.draughtsResult)
    })

  def api(simul: Simul): Fu[JsObject] = for {
    lightHost <- getLightUser(simul.hostId)
    lightArbiter <- simul.arbiterId ?? getLightUser
  } yield baseSimul(simul, lightHost, lightArbiter) ++ Json.obj(
    "nbApplicants" -> simul.applicants.size,
    "nbPairings" -> simul.pairings.size
  )

  def api(simuls: List[Simul]): Fu[JsArray] =
    simuls.map(api).sequenceFu map JsArray.apply

  def apiAll(
    pending: List[Simul],
    created: List[Simul],
    started: List[Simul],
    finished: List[Simul]
  ): Fu[JsObject] = for {
    pendingJson <- api(pending)
    createdJson <- api(created)
    startedJson <- api(started)
    finishedJson <- api(finished)
  } yield Json.obj(
    "pending" -> pendingJson,
    "created" -> createdJson,
    "started" -> startedJson,
    "finished" -> finishedJson
  )

  private def baseSimul(simul: Simul, lightHost: Option[LightUser], lightArbiter: Option[LightUser]) = Json.obj(
    "id" -> simul.id,
    "host" -> lightHost.map { host =>
      Json.obj(
        "id" -> host.id,
        "name" -> host.name,
        "online" -> isOnline(host.id),
        "patron" -> host.isPatron,
        "rating" -> simul.hostRating
      ).add("gameId" -> simul.hostGameId)
        .add("title" -> host.title)
        .add("officialRating" -> simul.hasFmjd.option(simul.spotlight.flatMap(_.hostFmjdRating)))
    },
    "name" -> simul.name,
    "fullName" -> simul.fullName,
    "variants" -> simul.variants.map(variantJson(draughts.Speed(simul.clock.config.some))),
    "isCreated" -> simul.isCreated,
    "isRunning" -> simul.isRunning,
    "isFinished" -> simul.isFinished,
    "text" -> ~simul.text
  ).add("arbiter" -> lightArbiter.map { arbiter =>
      Json.obj(
        "id" -> arbiter.id,
        "name" -> arbiter.name,
        "online" -> isOnline(arbiter.id),
        "patron" -> arbiter.isPatron,
        "title" -> arbiter.title,
        "hidden" -> simul.spotlight.flatMap(_.arbiterHidden)
      )
    })
    .add("unique" -> simul.spotlight.map { s => true })
    .add("description" -> simul.spotlight.map(s => lidraughts.base.RawHtml.markdownLinks(s.description)))
    .add("targetPct" -> simul.targetPct)

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
      ).add("name" -> light.map(_.name))
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
      ).add("name" -> light.map(_.name))
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
    "orient" -> g.playerByUserId(hostId).map(_.color),
    "board" -> g.variant.boardSize
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

  private implicit val simulTeamWriter = Json.writes[SimulTeam]
}
