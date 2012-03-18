package lila.system

import lila.chess._
import model._
import scalaz.effects._
import scalaz.{ Success, Failure }

class ServerTest extends SystemTest {

  val env = SystemEnv()
  val repo = env.gameRepo
  val server = env.server

  def insert(dbGame: DbGame = newDbGameWithRandomIds()): IO[DbGame] = for {
    _ ← repo insert dbGame
  } yield dbGame

  def move(game: DbGame, m: String = "d2 d4"): IO[Valid[Unit]] =
    server.playMove(game fullIdOf White, m)

  def updated(
    game: DbGame = newDbGameWithRandomIds,
    m: String = "d2 d4"): IO[Valid[DbGame]] = for {
    inserted ← insert(game)
    res ← move(inserted, m)
    updatedGame ← res.fold(
      e ⇒ io(Failure(e)),
      _ ⇒ repo game game.id map (x ⇒ success(x))
    )
  } yield updatedGame

  def play(game: IO[DbGame], ms: List[String]): IO[Valid[DbGame]] =
    ms.foldLeft(game map { g ⇒ Success(g) }: IO[Valid[DbGame]]) {
      case (ioGame, move) ⇒ for {
        vGame ← ioGame
        newGame ← vGame.fold(
          e ⇒ io(Failure(e)),
          game ⇒ updated(game, move)
        )
      } yield newGame
    }

  "play a single move" in {
    "wrong player" in {
      insert() flatMap { move(_, "d7 d5") } must beIO.like {
        case g ⇒ g must beFailure
      }
    }
    "report success" in {
      insert() flatMap { move(_) } must beIO.like {
        case g ⇒ g must beSuccess
      }
    }
    "be persisted" in {
      "update turns" in {
        updated() must beIO.like {
          case vg ⇒ vg must beSuccess.like {
            case g ⇒ g.turns must_== 1
          }
        }
      }
      "update board" in {
        updated() must beIO.like {
          case vg ⇒ vg must beSuccess.like {
            case g ⇒ addNewLines(g.toChess.board.visual) must_== """
rnbqkbnr
pppppppp


   P

PPP PPPP
RNBQKBNR
"""
          }
        }
      }
    }
  }
  "play the Peruvian Immortal" in {

    val moves = List("e2 e4", "d7 d5", "e4 d5", "d8 d5", "b1 c3", "d5 a5", "d2 d4", "c7 c6", "g1 f3", "c8 g4", "c1 f4", "e7 e6", "h2 h3", "g4 f3", "d1 f3", "f8 b4", "f1 e2", "b8 d7", "a2 a3", "e8 c8", "a3 b4", "a5 a1", "e1 d2", "a1 h1", "f3 c6", "b7 c6", "e2 a6")

    "report success" in {
      play(insert(), moves) must beIO.like {
        case vg ⇒ vg must beSuccess
      }
    }
    "be persisted" in {
      def found: IO[Valid[DbGame]] = for {
        game ← play(insert(), moves)
        found ← game.fold(
          e ⇒ io(Failure(e)),
          a ⇒ repo game a.id map Success.apply
        )
      } yield found

      "update turns" in {
        found must beIO.like {
          case vg ⇒ vg must beSuccess.like {
            case g ⇒ g.turns must_== 27
          }
        }
      }
      "update board" in {
        found must beIO.like {
          case vg ⇒ vg must beSuccess.like {
            case g ⇒ addNewLines(g.toChess.board.visual) must_== """
  kr  nr
p  n ppp
B p p

 P P B
  N    P
 PPK PP
       q
"""
          }
        }
      }
      "event stacks" in {

        "high version number" in {
          found must beIO.like {
            case vg ⇒ vg must beSuccess.like {
              case g ⇒ g.player(White).eventStack.lastVersion must be_>(20)
            }
          }
        }
        "rotated" in {
          found must beIO.like {
            case vg ⇒ vg must beSuccess.like {
              case g ⇒ g.player(White).eventStack.events.size must_== EventStack.maxEvents
            }
          }
        }
      }
    }
  }
  "play to threefold repetition" in {
    val moves = List("b1 c3", "b8 c6", "c3 b1", "c6 b8", "b1 c3", "b8 c6", "c3 b1", "c6 b8", "b1 c3", "b8 c6")

    "report success" in {
      play(insert(), moves) must beIO.like {
        case vg ⇒ vg must beSuccess.like {
          case g ⇒ g.player(White).eventStack.events map (_._2) must contain(ThreefoldEvent())
        }
      }
    }
    "be persisted" in {
      def found: IO[Valid[DbGame]] = for {
        game ← play(insert(), moves)
        found ← game.fold(
          e ⇒ io(Failure(e)),
          a ⇒ repo game a.id map Success.apply
        )
      } yield found
      "propose threefold" in {
        found must beIO.like {
          case vg ⇒ vg must beSuccess.like {
            case g ⇒ g.player(White).eventStack.events map (_._2) must contain(ThreefoldEvent())
          }
        }
      }
    }
  }
  "play on playing game" in {
    val dbGame = insert(randomizeIds(newDbGameWithBoard("""
PP kr
K
""")))
    play(dbGame, List("a1 b1")) must beIO.like {
      case vg ⇒ vg must beSuccess
    }
  }
  "play on finished game" in {
    "by checkmate" in {
      val dbGame = insert(randomizeIds(newDbGameWithBoard("""
PP
K  r
    """)))
      play(dbGame, List("a1 b1")) must beIO.like {
        case vg ⇒ vg must beFailure
      }
    }
    "by autodraw" in {
      val dbGame = insert(randomizeIds(newDbGameWithBoard("""
k
K     B""")))
      play(dbGame, List("a1 b1")) must beIO.like {
        case vg ⇒ vg must beFailure
      }
    }
  }
}
