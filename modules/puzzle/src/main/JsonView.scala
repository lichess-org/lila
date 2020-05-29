package lidraughts.puzzle

import draughts.format.{ Uci, UciCharPair }
import play.api.libs.json._
import lidraughts.game.GameRepo
import lidraughts.game.JsonView.boardSizeWriter
import lidraughts.tree

final class JsonView(
    gameJson: GameJson,
    animationDuration: scala.concurrent.duration.Duration
) {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    mobileApi: Option[lidraughts.common.ApiVersion],
    round: Option[Round] = None,
    result: Option[Result] = None,
    voted: Option[Boolean]
  ): Fu[JsObject] = {
    val isMobile = mobileApi.isDefined
    ((puzzle.gameId != "custom") ?? gameJson(
      gameId = puzzle.gameId,
      plies = puzzle.initialPly,
      onlyLast = isMobile
    ) map some) map { gameJson =>
      Json.obj(
        "game" -> gameJson,
        "puzzle" -> puzzleJson(puzzle),
        "history" -> tree.Branch(
          id = UciCharPair(puzzle.initialMove),
          ply = puzzle.initialPly,
          move = Uci.WithSan(puzzle.initialMove, puzzle.initialMove.toSan),
          fen = puzzle.fenAfterInitialMove.getOrElse(puzzle.fen)
        ),
        "mode" -> mode,
        "voted" -> voted,
        "user" -> userInfos.map(JsonView.infos(puzzle.variant))
      ).noNull
    }
  }

  def pref(p: lidraughts.pref.Pref) = Json.obj(
    "blindfold" -> p.blindfold,
    "coords" -> p.coords,
    "animation" -> Json.obj(
      "duration" -> p.animationFactor * animationDuration.toMillis
    ),
    "destination" -> p.destination,
    "resizeHandle" -> p.resizeHandle,
    "moveEvent" -> p.moveEvent,
    "highlight" -> p.highlight,
    "showKingMoves" -> p.kingMoves
  ).add("fullCapture" -> (p.fullCapture == lidraughts.pref.Pref.FullCapture.YES).option(true))
    .add("coordSystem" -> (p.coordSystem != lidraughts.pref.Pref.CoordSystem.FIELDNUMBERS).option(p.coordSystem))

  def batch(puzzles: List[Puzzle], userInfos: UserInfos, variant: draughts.variant.Variant): Fu[JsObject] = for {
    games <- GameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
    jsons <- (puzzles zip games).collect {
      case (puzzle, maybeGame) => maybeGame match {
        case Some(game) =>
          gameJson.noCache(game, puzzle.initialPly, true) map { gameJson =>
            Json.obj(
              "game" -> gameJson,
              "puzzle" -> puzzleJson(puzzle)
            )
          }
        case _ => fuccess(Json.obj(
          "puzzle" -> puzzleJson(puzzle),
          "history" -> tree.Branch(
            id = UciCharPair(puzzle.initialMove),
            ply = puzzle.initialPly,
            move = Uci.WithSan(puzzle.initialMove, puzzle.initialMove.toSan),
            fen = puzzle.fenAfterInitialMove.getOrElse(puzzle.fen)
          )
        ))
      }
    }.sequenceFu
  } yield Json.obj(
    "user" -> JsonView.infos(variant)(userInfos),
    "puzzles" -> jsons
  )

  private def puzzleJson(puzzle: Puzzle): JsObject = Json.obj(
    "id" -> puzzle.id,
    "variant" -> puzzle.variant,
    "rating" -> puzzle.perf.intRating,
    "attempts" -> puzzle.attempts,
    "fen" -> puzzle.fen,
    "color" -> puzzle.color.name,
    "initialPly" -> puzzle.initialPly,
    "gameId" -> puzzle.gameId,
    "lines" -> lidraughts.puzzle.Line.toJson(puzzle.lines),
    "vote" -> puzzle.vote.sum
  ).add("branch" -> makeBranch(puzzle))
    .add("enabled" -> puzzle.enabled)

  private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
    import draughts.format._
    val fullSolution: List[Uci.Move] = (Line solution puzzle.lines).map { uci =>
      Uci.Move(uci) err s"Invalid puzzle solution UCI $uci"
    }
    val solution =
      if (fullSolution.isEmpty) {
        logger.warn(s"Puzzle ${puzzle.id} has an empty solution from ${puzzle.lines}")
        fullSolution
      } //else if (fullSolution.size % 2 == 0) fullSolution.init
      else fullSolution
    val init = draughts.DraughtsGame(puzzle.variant.some, puzzle.fenAfterInitialMove).withTurns(puzzle.initialPly)
    val (_, branchList) = solution.foldLeft[(draughts.DraughtsGame, List[tree.Branch])]((init, Nil)) {
      case ((prev, branches), uci) =>
        val (game, move) = prev(uci.orig, uci.dest, uci.promotion).prefixFailuresWith(s"puzzle ${puzzle.id}").err
        val branch = tree.Branch(
          id = UciCharPair(move.toUci),
          ply = game.turns,
          move = Uci.WithSan(move.toShortUci, game.pdnMoves.last),
          fen = draughts.format.Forsyth >> game
        )
        (game, branch :: branches)
    }
    branchList.foldLeft[Option[tree.Branch]](None) {
      case (None, branch) => branch.some
      case (Some(child), branch) => Some(branch addChild child)
    }
  }

  implicit val variantWriter: OWrites[draughts.variant.Variant] = OWrites { v =>
    Json.obj(
      "key" -> v.key,
      "name" -> v.name,
      "short" -> v.shortName,
      "gameType" -> v.gameType,
      "board" -> v.boardSize
    )
  }
}

object JsonView {

  def infos(variant: draughts.variant.Variant)(i: UserInfos): JsObject = Json.obj(
    "rating" -> i.user.perfs.puzzle(variant).intRating,
    "history" -> i.history.map(_.rating), // for mobile BC
    "recent" -> i.history.map { r =>
      Json.arr(r.puzzleId, r.ratingDiff, r.rating)
    }
  ).noNull

  def round(r: Round): JsObject = Json.obj(
    "ratingDiff" -> r.ratingDiff,
    "win" -> r.result.win
  )
}
