package lila.puzzle

import chess.IntRating
import chess.format.{ Fen, Uci }
import chess.rating.glicko.{ Glicko, GlickoCalculator }
import reactivemongo.api.bson.*
import scala.util.Success
import scalalib.ThreadLocalRandom
import scalalib.actor.AsyncActorSequencers

import lila.db.AsyncColl
import lila.db.dsl.{ *, given }
import lila.db.BSON
import lila.rating.GlickoExt.{ cap, sanityCheck }

final class PuzzleGuessColls(
    val guess: AsyncColl,
    val round: AsyncColl,
    val player: AsyncColl
)

private object PuzzleGuessBson:

  import lila.rating.Glicko.glickoHandler

  given guessHandler: BSON[PuzzleGuess] with
    import PuzzleGuess.BSONFields.*
    def reads(r: BSON.Reader) = PuzzleGuess(
      id = r.get[PuzzleGuess.Id](id),
      gameId = r.get[GameId](gameId),
      fen = r.get[Fen.Full](fen),
      puzzleId = r.getO[PuzzleId](puzzleId),
      glicko = r.get[Glicko](glicko),
      plays = r.intD(plays)
    )
    def writes(w: BSON.Writer, g: PuzzleGuess) = $doc(
      id -> g.id,
      gameId -> g.gameId,
      fen -> g.fen,
      puzzleId -> g.puzzleId,
      glicko -> g.glicko,
      plays -> g.plays
    )

  given roundIdHandler: BSONHandler[PuzzleGuess.Round.Id] = tryHandler[PuzzleGuess.Round.Id](
    { case BSONString(v) =>
      v.split(PuzzleGuess.Round.idSep) match
        case Array(userId, guessId) =>
          Success(PuzzleGuess.Round.Id(UserId(userId), PuzzleGuess.Id(guessId)))
        case _ => handlerBadValue(s"Invalid puzzle guess round id $v")
    },
    id => BSONString(id.toString)
  )

  given roundHandler: BSON[PuzzleGuess.Round] with
    import PuzzleGuess.Round.BSONFields.*
    def reads(r: BSON.Reader) = PuzzleGuess.Round(
      id = r.get[PuzzleGuess.Round.Id](id),
      guessWin = r.get[PuzzleWin](guessWin),
      solveWin = r.getO[PuzzleWin](solveWin),
      win = r.get[PuzzleWin](win),
      date = r.date(date)
    )
    def writes(w: BSON.Writer, r: PuzzleGuess.Round) = $doc(
      id -> r.id,
      guessWin -> r.guessWin,
      solveWin -> r.solveWin,
      win -> r.win,
      date -> r.date
    )

  given playerHandler: BSON[PuzzleGuess.Player] with
    import PuzzleGuess.Player.BSONFields.*
    def reads(r: BSON.Reader) = PuzzleGuess.Player(
      id = r.get[UserId](id),
      glicko = r.getD[Glicko](glicko, lila.rating.Glicko.default),
      runs = r.intD(runs),
      wins = r.intD(wins)
    )
    def writes(w: BSON.Writer, p: PuzzleGuess.Player) = $doc(
      id -> p.id,
      glicko -> p.glicko,
      runs -> p.runs,
      wins -> p.wins
    )

object PuzzleGuessApi:
  case class Result(
      guess: PuzzleGuess,
      correct: Boolean,
      finished: Boolean, // false when a correctly identified puzzle awaits solving
      win: Option[PuzzleWin], // overall result, defined when finished
      solution: Option[List[Uci.Move]], // to play from the shown fen, when guess was "puzzle" and correct
      rating: Option[(IntRating, IntRating)] // player rating before/after, when finished and rated
  )

final class PuzzleGuessApi(
    guessColls: PuzzleGuessColls,
    colls: PuzzleColls,
    gameRepo: lila.core.game.GameRepo
)(using Executor, Scheduler):

  import PuzzleGuessBson.given
  import BsonHandlers.puzzleReader
  import PuzzleGuessApi.Result
  import lila.rating.Glicko.glickoHandler

  private val sequencer = AsyncActorSequencers[PuzzleGuess.Id](
    maxSize = Max(64),
    expiration = 5.minutes,
    timeout = 5.seconds,
    name = "puzzle.guess.finish",
    lila.mon.asyncActorMonitor.full
  )

  private val calculator = GlickoCalculator()

  // correctly-guessed puzzles awaiting a solving attempt
  private val pendingSolve = scalalib.cache.ExpireSetMemo[String](30.minutes)
  private def pendingKey(userId: UserId, id: PuzzleGuess.Id) = s"$userId:$id"

  def find(id: PuzzleGuess.Id): Fu[Option[PuzzleGuess]] =
    guessColls.guess(_.byId[PuzzleGuess](id))

  private def findRound(userId: UserId, id: PuzzleGuess.Id): Fu[Option[PuzzleGuess.Round]] =
    guessColls.round(_.byId[PuzzleGuess.Round](PuzzleGuess.Round.Id(userId, id).toString))

  def playerOf(userId: UserId): Fu[PuzzleGuess.Player] =
    guessColls
      .player(_.byId[PuzzleGuess.Player](userId))
      .dmap(_ | PuzzleGuess.Player.default(userId))

  def nextFor(userId: Option[UserId]): Fu[Option[PuzzleGuess]] =
    userId
      .fold(fuccess(PuzzleGuess.Player.default(UserId("anon"))))(playerOf)
      .flatMap: player =>
        nextFor(player, userId, canGenerate = true)

  private def nextFor(
      player: PuzzleGuess.Player,
      userId: Option[UserId],
      canGenerate: Boolean
  ): Fu[Option[PuzzleGuess]] =
    sample(player.glicko.rating.toInt, 15).flatMap: candidates =>
      unplayed(userId, candidates).flatMap:
        case head :: _ => fuccess(head.some)
        case Nil if candidates.nonEmpty => fuccess(ThreadLocalRandom.oneOf(candidates.toVector))
        case Nil if canGenerate => generate(30) >> nextFor(player, userId, canGenerate = false)
        case Nil => fuccess(none)

  private def sample(rating: Int, nb: Int): Fu[List[PuzzleGuess]] =
    guessColls.guess: coll =>
      coll
        .aggregateList(nb): framework =>
          import framework.*
          Match(
            $doc("glicko.r".$gt((rating - 500).toDouble).$lt((rating + 500).toDouble))
          ) -> List(Sample(nb))
        .map(_.flatMap(guessHandler.readOpt))

  private def unplayed(userId: Option[UserId], candidates: List[PuzzleGuess]): Fu[List[PuzzleGuess]] =
    userId.fold(fuccess(candidates)): uid =>
      guessColls
        .round:
          _.distinctEasy[String, Set](
            "_id",
            $inIds(candidates.map(g => PuzzleGuess.Round.Id(uid, g.id).toString))
          )
        .map: played =>
          candidates.filterNot(g => played(PuzzleGuess.Round.Id(uid, g.id).toString))

  def guess(id: PuzzleGuess.Id, isPuzzleGuess: Boolean, userId: Option[UserId]): Fu[Option[Result]] =
    find(id).flatMapz: g =>
      val correct = isPuzzleGuess == g.isPuzzle
      if correct && g.isPuzzle then
        solutionOf(g).flatMap: solution =>
          userId.foreach(uid => pendingSolve.put(pendingKey(uid, id)))
          fuccess:
            Result(g, correct, finished = false, win = none, solution = solution, rating = none).some
      else
        finish(g, userId, guessWin = PuzzleWin(correct), solveWin = none).dmap(some)

  def solve(id: PuzzleGuess.Id, win: PuzzleWin, userId: Option[UserId]): Fu[Option[Result]] =
    find(id).flatMapz: g =>
      val rated = userId.exists(uid => pendingSolve.get(pendingKey(uid, id)))
      finish(g, userId.filter(_ => rated), guessWin = PuzzleWin(true), solveWin = win.some).dmap(some)

  private def solutionOf(g: PuzzleGuess): Fu[Option[List[Uci.Move]]] =
    g.puzzleId.so: pid =>
      colls
        .puzzle(_.byId[Puzzle](pid))
        .dmap(_.map(_.line.tail))

  private def finish(
      g: PuzzleGuess,
      userId: Option[UserId],
      guessWin: PuzzleWin,
      solveWin: Option[PuzzleWin]
  ): Fu[Result] =
    val win = PuzzleWin(guessWin.yes && solveWin.forall(_.yes))
    userId match
      case None =>
        guessColls.guess.map(_.incFieldUnchecked($id(g.id), PuzzleGuess.BSONFields.plays)).inject:
          Result(g, guessWin.yes, finished = true, win = win.some, solution = none, rating = none)
      case Some(uid) =>
        sequencer(g.id):
          findRound(uid, g.id).flatMap:
            case Some(_) => // already played: update the round but never re-rate
              val round = PuzzleGuess.Round(
                id = PuzzleGuess.Round.Id(uid, g.id),
                guessWin = guessWin,
                solveWin = solveWin,
                win = win,
                date = nowInstant
              )
              upsertRound(round).inject:
                Result(g, guessWin.yes, finished = true, win = win.some, solution = none, rating = none)
            case None => rateAndFinish(g, uid, guessWin, solveWin, win)

  private def rateAndFinish(
      g: PuzzleGuess,
      uid: UserId,
      guessWin: PuzzleWin,
      solveWin: Option[PuzzleWin],
      win: PuzzleWin
  ): Fu[Result] =
    playerOf(uid).flatMap: player =>
      val (newPlayerGlicko, guessGlicko) = computeGlicko(player, g, win)
      val newGuessGlicko = guessGlicko
        .copy(
          rating = guessGlicko.rating
            .atMost(g.glicko.rating + lila.rating.Glicko.maxRatingDelta)
            .atLeast(g.glicko.rating - lila.rating.Glicko.maxRatingDelta)
        )
        .cap
        .some
        .filter(g.glicko !=)
        .filter(_.sanityCheck)
      val round = PuzzleGuess.Round(
        id = PuzzleGuess.Round.Id(uid, g.id),
        guessWin = guessWin,
        solveWin = solveWin,
        win = win,
        date = nowInstant
      )
      val newPlayer = player.copy(
        glicko = newPlayerGlicko,
        runs = player.runs + 1,
        wins = player.wins + win.yes.so(1)
      )
      for
        _ <- upsertRound(round)
        _ <- guessColls.player:
          _.update.one($id(uid), playerHandler.write(newPlayer), upsert = true)
        _ <- guessColls.guess.map:
          _.updateUnchecked(
            $id(g.id),
            $inc(PuzzleGuess.BSONFields.plays -> $int(1)) ++ newGuessGlicko.so { glicko =>
              $set(PuzzleGuess.BSONFields.glicko -> glicko)
            }
          )
      yield Result(
        g,
        guessWin.yes,
        finished = true,
        win = win.some,
        solution = none,
        rating = (player.intRating -> newPlayer.intRating).some
      )

  private def computeGlicko(
      player: PuzzleGuess.Player,
      g: PuzzleGuess,
      win: PuzzleWin
  ): (Glicko, Glicko) =
    val players = chess.ByColor(
      chess.rating.glicko.Player(player.glicko.cap, player.runs, none),
      chess.rating.glicko.Player(g.glicko.cap, g.plays, none)
    )
    calculator
      .computeGame(chess.rating.glicko.Game(players, chess.Outcome(Color.fromWhite(win.yes).some)))
      .map(_.map(_.glicko))
      .fold(
        err =>
          logger.error(s"Failed to compute glicko for puzzle guess ${g.id}", err)
          players.map(_.glicko).toPair
        ,
        _.toPair
      )

  private def upsertRound(r: PuzzleGuess.Round): Funit =
    val doc = roundHandler.write(r) ++
      $doc(PuzzleGuess.Round.BSONFields.user -> r.id.userId)
    guessColls.round(_.update.one($id(r.id.toString), doc, upsert = true)).void

  // ---------------------------------------------------------------------------
  // position generation: half real puzzles, half quiet positions
  // sampled from the same games a few moves before the tactic occurred
  // ---------------------------------------------------------------------------

  def generate(nb: Int): Fu[Int] =
    samplePuzzles(nb).flatMap: puzzles =>
      guessColls
        .guess:
          _.distinctEasy[GameId, Set](
            PuzzleGuess.BSONFields.gameId,
            $doc(PuzzleGuess.BSONFields.gameId.$in(puzzles.map(_.gameId)))
          )
        .flatMap: existing =>
          puzzles
            .filterNot(p => existing(p.gameId))
            .sequentially(makeEntry)
            .map(_.flatten)
            .flatMap: entries =>
              entries.nonEmpty.so:
                guessColls
                  .guess(_.insert.many(entries.map(guessHandler.write)))
                  .dmap(_.n)
                  .recover { case _: Exception => 0 }

  private def samplePuzzles(nb: Int): Fu[List[Puzzle]] =
    colls.puzzle: coll =>
      coll
        .aggregateList(nb): framework =>
          import framework.*
          Match(
            $doc(
              "glicko.d".$lte(110d),
              Puzzle.BSONFields.plays.$gte(50),
              Puzzle.BSONFields.issue.$exists(false)
            )
          ) -> List(Sample(nb))
        .map(_.flatMap(puzzleReader.readOpt))

  private def makeEntry(puzzle: Puzzle): Fu[Option[PuzzleGuess]] =
    if puzzle.line.tail.isEmpty then fuccess(none)
    else if ThreadLocalRandom.nextBoolean() then fuccess(makePuzzleEntry(puzzle))
    else makeNormalEntry(puzzle)

  private def makePuzzleEntry(puzzle: Puzzle): Option[PuzzleGuess] =
    puzzle.boardAfterInitialMove.map: _ =>
      PuzzleGuess(
        id = PuzzleGuess.makeId,
        gameId = puzzle.gameId,
        fen = puzzle.fenAfterInitialMove,
        puzzleId = puzzle.id.some,
        glicko = initialGlicko(puzzle.glicko.rating),
        plays = 0
      )

  private def makeNormalEntry(puzzle: Puzzle): Fu[Option[PuzzleGuess]] =
    gameRepo
      .gameFromSecondary(puzzle.gameId)
      .map:
        _.flatMap: game =>
          // the position solvers face is after `initialPly + 1` moves;
          // pick an earlier ply with the same side to move
          val shownPly = puzzle.initialPly.value + 1
          val offset = 6 + 2 * ThreadLocalRandom.nextInt(5) // 6, 8, .., 14 keeps parity
          val targetPly = shownPly - offset
          (targetPly >= 10).so:
            game.variant.initialPosition
              .forward(game.sans.take(targetPly))
              .toOption
              .map: position =>
                PuzzleGuess(
                  id = PuzzleGuess.makeId,
                  gameId = puzzle.gameId,
                  fen = Fen.write(position),
                  puzzleId = none,
                  glicko = initialGlicko(1400d),
                  plays = 0
                )
      .recover { case _: Exception => none }

  private def initialGlicko(rating: Double) =
    Glicko(rating, 300d, lila.rating.Glicko.defaultVolatility).cap
