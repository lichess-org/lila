package lila
package setup

import chess.{ Game, Board, Situation, Variant, Clock, Speed }
import chess.format.Forsyth
import game.{ GameRepo, DbGame, Pov }

trait Config {

  // Whether or not to use a clock
  val clock: Boolean

  // Clock time in minutes
  val time: Int

  // Clock increment in seconds
  val increment: Int

  // Game variant code
  val variant: Variant

  // Creator player color
  val color: Color

  lazy val creatorColor = color.resolve

  def makeGame = Game(board = Board init variant, clock = makeClock)

  def validClock = clock.fold(time + increment > 0, true)

  def makeClock = clock option Clock(time * 60, increment)
}

trait GameGenerator { self: Config ⇒

  def game: DbGame

  def pov = Pov(game, creatorColor)
}

trait Positional { self: Config ⇒

  import chess.format.Forsyth, Forsyth.SituationPlus

  def fen: Option[String]

  def fenDbGame(builder: Game ⇒ DbGame): DbGame = {
    val state = fen filter (_ ⇒ variant == Variant.FromPosition) flatMap Forsyth.<<<
    val chessGame = state.fold({
      case sit @ SituationPlus(Situation(board, color), _, _) ⇒
        Game(board = board, player = color, turns = sit.turns)
    }, makeGame)
    val dbGame = builder(chessGame)
    state.fold({
      case sit @ SituationPlus(_, history, _) ⇒ dbGame.copy(
        variant = Variant.FromPosition,
        castles = history.castleNotation,
        turns = sit.turns)
    }, dbGame)
  }
}

object Config extends BaseConfig

trait BaseConfig {

  val variants = List(Variant.Standard.id, Variant.Chess960.id)
  val variantDefault = Variant.Standard

  val variantsWithFen = variants :+ Variant.FromPosition.id

  val speeds = Speed.all map (_.id)

  val timeMin = 0
  val timeMax = 30
  val times = (timeMin to timeMax).toList

  val incrementMin = 0
  val incrementMax = 30
  val increments = (incrementMin to incrementMax).toList
}
