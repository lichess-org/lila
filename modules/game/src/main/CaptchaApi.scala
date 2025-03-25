package lila.game
import chess.Game as ChessGame
import chess.format.pgn.{ SanStr, Sans, Tags }
import chess.format.{ BoardFen, Fen, pgn }

import scala.util.Success

import lila.core.captcha.{ Captcha, CaptchaApi as ICaptchaApi, Solutions, WithCaptcha }
import lila.core.game.Game

// only works with standard chess (not chess960)
final private class CaptchaApi(gameRepo: GameRepo)(using Executor) extends ICaptchaApi:

  def any: Captcha = Impl.challenges.head

  def get(id: GameId): Fu[Captcha] = Impl.find(id) match
    case None    => Impl.getFromDb(id).dmap(_ | Impl.default).addEffect(Impl.add)
    case Some(c) => fuccess(c)

  def validate(gameId: GameId, move: String): Fu[Boolean] =
    get(gameId).map(_.solutions.toList contains move)

  def validateSync(data: WithCaptcha): Boolean =
    validate(data.gameId, data.move).await(2.seconds, "CaptchaApi.validateSync")

  def newCaptcha() = Impl.refresh

  private object Impl:

    val default = Captcha(
      gameId = GameId("00000000"),
      fen = BoardFen("1k3b1r/r5pp/pNQppq2/2p5/4P3/P3B3/1P3PPP/n4RK1"),
      color = chess.White,
      solutions = NonEmptyList.one("c6 c8"),
      moves = Map("c6" -> "c8")
    )

    def refresh = createFromDb.andThen:
      case Success(Some(captcha)) => add(captcha)

    var challenges       = NonEmptyList.one(default)
    private val capacity = 256

    def add(c: Captcha): Unit =
      if find(c.gameId).isEmpty then challenges = NonEmptyList(c, challenges.toList.take(capacity))

    def find(id: GameId): Option[Captcha] =
      challenges.find(_.gameId == id)

    def createFromDb: Fu[Option[Captcha]] =
      findCheckmateInDb(10).orElse(findCheckmateInDb(1)).flatMapz(fromGame)

    def findCheckmateInDb(distribution: Int): Fu[Option[Game]] =
      gameRepo.findRandomStandardCheckmate(distribution)

    def getFromDb(id: GameId): Fu[Option[Captcha]] =
      gameRepo.game(id).flatMapz(fromGame)

    def fromGame(game: Game): Fu[Option[Captcha]] =
      gameRepo
        .getOptionPgn(game.id)
        .map:
          _.flatMap { makeCaptcha(game, _) }

    def makeCaptcha(game: Game, moves: Vector[SanStr]): Option[Captcha] =
      for
        rewinded  <- rewind(moves)
        solutions <- solve(rewinded)
        moves = rewinded.situation.destinations.map: (from, dests) =>
          from.key -> dests.map(_.key).mkString
      yield Captcha(game.id, fenOf(rewinded), rewinded.player, solutions, moves = moves)

    def solve(game: ChessGame): Option[Solutions] =
      game.situation.moves.view
        .flatMap: (_, moves) =>
          moves.filter: move =>
            (move.after.situationOf(!game.player)).checkMate
        .to(List)
        .map: move =>
          s"${move.orig.key} ${move.dest.key}"
        .toNel

    def rewind(moves: Vector[SanStr]): Option[ChessGame] =
      pgn.Reader
        .movesWithSans(
          moves,
          sans => Sans(safeInit(sans.value)),
          tags = Tags.empty
        )
        .flatMap(_.valid)
        .map(_.state)
        .toOption

    def safeInit[A]: List[A] => List[A] =
      case _ :: Nil => Nil
      case x :: xs  => x :: safeInit(xs)
      case _        => Nil

    def fenOf(game: ChessGame) = Fen.writeBoard(game.board)
