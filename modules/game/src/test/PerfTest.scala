package lila.game

import org.specs2.mutable._
import org.specs2.specification._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

class PerfTest extends Specification {

  val nb = 200
  val iterations = 10

  def runOne = Game.tube read bson map (_.toChess)
  def run { for (i ← 1 to nb) runOne }

  "game model" should {
    "work" in {
      Game.tube read bson must beSome.like { case g ⇒ g.turns must_== 12 }
    }
    "be fast" in {
      if (nb * iterations > 1) {
        print("warming up")
        run
        println(" again")
        run
      }
      println("measuring")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb games in $duration ms")
        duration
      }
      val nbGames = iterations * nb
      val gameMicros = (1000*durations.sum) / nbGames
      println(s"Average = $gameMicros micros per game")
      println(s"          ${1000000 / gameMicros} games per second")
      true must_== true
    }
  }

  private lazy val bson = JsObjectWriter write Json.parse("""
{
  "_id": "s9kgwwwq",
  "c": {
    "b": 68.21699523925781,
    "c": true,
    "i": 0,
    "l": 420,
    "t": 1371840910,
    "w": 13.557001113891602
  },
  "ca": {"$date": 1371848110002},
  "cs": "KQkq",
  "lm": "a7a5",
  "lmt": 1371840910,
  "me": {
    "so": 1
  },
  "p": [
    {
      "bs": 1,
      "elo": 1151,
      "id": "4xqm",
      "mts": "21247",
      "ps": "cbjpnpwparobppBpdqbnApmnipupekhr",
      "uid": "red1"
    },
    {
      "elo": 968,
      "id": "ufh0",
      "mts": "32abaA",
      "ps": "SpYpQn9b7q6b8k3pJpGp2p?rTnPp1p4r",
      "uid": "japr"
    }
  ],
  "ra": true,
  "s": 20,
  "t": 12,
  "tk": "2j5a",
  "ua": {"$date": 1371848112002},
  "uids": [
    "red1",
    "japr"
  ]
}""").as[JsObject]
}
