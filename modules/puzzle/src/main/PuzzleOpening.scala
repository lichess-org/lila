package lila.puzzle

import akka.stream.scaladsl._
import chess.opening.FullOpeningDB
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.GameRepo

case class PuzzleOpening(key: PuzzleOpening.Key, name: String)

final class PuzzleOpeningApi(colls: PuzzleColls, gameRepo: GameRepo)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._

  def addAllMissing: Funit =
    colls.puzzle {
      _.find($doc(Puzzle.BSONFields.opening $exists false))
        .cursor[Puzzle]()
        .documentSource()
        .mapAsyncUnordered(8)(addMissing)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done adding $count puzzle openings")
        .result
        .void
    }

  private def addMissing(puzzle: Puzzle): Funit = gameRepo game puzzle.gameId flatMap {
    _ ?? { game =>
      FullOpeningDB.search(game.pgnMoves) match {
        case None =>
          fuccess {
            logger warn s"No opening for https://lichess.org/training/${puzzle.id}"
          }
        case Some(o) =>
          val family = o.opening.name.split(":").headOption | "Unknown"
          colls.puzzle {
            _.updateField(
              $id(puzzle.id),
              Puzzle.BSONFields.opening,
              PuzzleOpening.nameToKey(family)
            ).void
          }
      }
    }
  }
}

object PuzzleOpening {

  case class Key(value: String) extends AnyVal with StringValue

  import java.text.Normalizer

  type Name = String

  implicit val keyIso = lila.common.Iso.string[Key](Key.apply, _.value)

  def nameToKey(name: Name) = Key {
    Normalizer
      .normalize(name, Normalizer.Form.NFD)  // split an accented letter in the base letter and the accent
      .replaceAllIn("[\u0300-\u036f]".r, "") // remove all previously split accents
      .replaceAllIn("""\s+""".r, "_")
      .replaceAllIn("""[^\w\-]+""".r, "")
  }

  def apply(key: Key): Option[PuzzleOpening] = openings get key map { PuzzleOpening(key, _) }

  def find(key: String): Option[PuzzleOpening] = apply(Key(key))

  lazy val openings: Map[Key, Name] = List(
    "Caro-Kann Defense",
    "Pirc Defense",
    "Scandinavian Defense",
    "Bird Opening",
    "Ponziani Opening",
    "King's Gambit Accepted",
    "Van Geet Opening",
    "Italian Game",
    "Indian Defense",
    "Sicilian Defense",
    "Queen's Gambit Declined",
    "Pterodactyl Defense",
    "Nimzo-Indian Defense",
    "Hungarian Opening",
    "Alekhine Defense",
    "English Opening",
    "King's Pawn",
    "Vienna Game",
    "Three Knights Opening",
    "Dutch Defense",
    "Zukertort Defense",
    "Ruy Lopez",
    "Queen's Pawn Game",
    "King's Pawn Opening",
    "Center Game",
    "Latvian Gambit",
    "Queen's Gambit Accepted",
    "King's Pawn Game",
    "Englund Gambit Complex",
    "King's Indian Defense",
    "English Defense",
    "French Defense",
    "Blackmar-Diemer Gambit Declined",
    "Bishop's Opening",
    "Russian Game",
    "Nimzowitsch Defense",
    "Norwegian Defense",
    "Philidor Defense",
    "Old Indian Defense",
    "Benoni Defense",
    "Trompowsky Attack",
    "Four Knights Game",
    "Polish Opening",
    "Montevideo Defense",
    "Modern Defense",
    "Semi-Slav Defense",
    "Slav Defense",
    "Scotch Game",
    "Zukertort Opening",
    "King's Indian Attack",
    "King's Gambit Declined",
    "Rubinstein Opening",
    "Borg Defense",
    "Guatemala Defense",
    "Queen's Indian Defense",
    "Réti Opening",
    "Grünfeld Defense",
    "Nimzo-Larsen Attack",
    "Elephant Gambit",
    "Catalan Opening",
    "Sodium Attack",
    "Benko Gambit Accepted",
    "Bogo-Indian Defense",
    "Lasker Simul Special",
    "Grob Opening",
    "Tarrasch Defense",
    "Neo-Grünfeld Defense",
    "Amsterdam Attack",
    "Van't Kruijs Opening",
    "St. George Defense",
    "Yusupov-Rubinstein System",
    "Rat Defense",
    "Kangaroo Defense",
    "Blackmar-Diemer Gambit",
    "Latvian Gambit Accepted",
    "Owen Defense",
    "Kádas Opening",
    "Ware Defense",
    "Rapport-Jobava System",
    "Mieses Opening",
    "London System",
    "Horwitz Defense",
    "Carr Defense",
    "Ware Opening",
    "Formation",
    "Paleface Attack",
    "Queen's Pawn, Mengarini Attack",
    "Mexican Defense",
    "Blumenfeld Countergambit",
    "Vulture Defense",
    "Gedult's Opening",
    "Hippopotamus Defense",
    "English Rat",
    "Mikenas Defense",
    "Danish Gambit",
    "Barnes Opening",
    "Veresov Opening",
    "Amazon Attack",
    "Amar Opening",
    "Valencia Opening",
    "Danish Gambit Accepted",
    "Robatsch Defense",
    "Lion Defense",
    "Colle System",
    "Englund Gambit Declined",
    "Global Opening",
    "Benko Gambit",
    "Fried Fox Defense",
    "Benko Gambit Declined",
    "Semi-Slav Defense Accepted",
    "East Indian Defense",
    "King's Gambit",
    "Borg Opening",
    "Amar Gambit",
    "Czech Defense",
    "Gunderam Defense",
    "Barnes Defense",
    "Clemenz Opening",
    "Portuguese Opening",
    "Englund Gambit",
    "Slav Indian",
    "Dresden Opening",
    "Crab Opening",
    "Danish Gambit Declined",
    "Polish Defense",
    "Blackburne Shilling Gambit",
    "King's Knight Opening",
    "English Orangutan",
    "Döry Defense",
    "Queen's Gambit",
    "Torre Attack",
    "Australian Defense",
    "Anderssen's Opening",
    "Marienbad System",
    "Richter-Veresov Attack",
    "Venezolana Opening",
    "Englund Gambit Complex Declined",
    "Canard Opening",
    "Creepy Crawly Formation",
    "Irish Gambit",
    "Blumenfeld Countergambit Accepted",
    "Zaire Defense",
    "Bronstein Gambit",
    "Lemming Defense",
    "Wade Defense",
    "Goldsmith Defense",
    "Duras Gambit",
    "Queen's Indian Accelerated",
    "Blackmar-Diemer, Lemberger Countergambit",
    "Giuoco Piano",
    "Blackmar Gambit",
    "Saragossa Opening",
    "Bongcloud Attack",
    "Center Game Accepted"
  ).view.map(name => nameToKey(name) -> name).toMap
}
