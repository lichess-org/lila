package lila.cli

import lila.system.model._
import lila.system.db.GameRepo
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

case class ImportGames(mongodb: MongoDB, gameRepo: GameRepo) extends Command {

  val oldGames = mongodb("game2")
  val newGames = mongodb("game")
  val bulkSize = 200
  //val max = Some(200000)
  val max: Option[Int] = None

  def apply: IO[Unit] = for {
    _ ← putStrLn("- Drop indexes")
    _ ← gameRepo.dropIndexes
    _ ← putStrLn("- Import games")
    _ ← importGames
    _ ← putStrLn("- Ensure indexes")
    _ ← gameRepo.ensureIndexes
    _ ← putStrLn("Done")
  } yield ()

  private def importGames: IO[Unit] = io {
    val total = oldGames.count
    println("%d games to import" format total)
    println("drop db.game")
    newGames.drop()
    println("start import")
    var it = 0
    val pool = max.fold(
      m ⇒ oldGames.find().limit(m).grouped(bulkSize),
      oldGames.grouped(bulkSize))
    for (olds ← pool) {
      newGames insert (olds.toList map convert).flatten
      it = it + bulkSize
      if (it % 5000 == 0) {
        val percentage = it / total.toFloat * 100
        println("%d/%d %2.1f%%".format(it, total, percentage))
      }
    }
    println("unset userIds: []")
    newGames.update(
      DBObject("userIds" -> DBList()),
      $unset("userIds"),
      upsert = false,
      multi = true)
    println("unset winnerUserId: \"\"")
    newGames.update(
      DBObject("winnerUserId" -> ""),
      $unset("winnerUserId"),
      upsert = false,
      multi = true)
  }

  def convert(game: DBObject): Option[DBObject] = try {
    if (game.as[String]("_id").size != 8) None
    else {
      val status = Status(game.as[Int]("status")).get
      game.put("players", List(0, 1) map { it ⇒
        val player = game.expand[BasicDBObject]("players." + it).get
        player.put("ps", playerPs(player.getString("ps")))
        player.put("c", player.as[String]("color"))
        player.removeField("color")
        if (player.containsField("isWinner")) {
          player.put("w", player.as[Boolean]("isWinner"))
          player.removeField("isWinner")
        }
        if (status >= Aborted) {
          player.removeField("isOfferingDraw")
          player.removeField("isOfferingRematch")
          player.removeField("lastDrawOffer")
          player.removeField("evts")
        }
        player
      })
      game.getAs[DBObject]("clock") map { clock ⇒
        val times = clock.as[DBObject]("times")
        clock.removeField("times")
        clock.put("w", times.as[String]("white"))
        clock.put("b", times.as[String]("black"))
        clock.put("i", clock.as[Int]("increment"))
        clock.removeField("increment")
        clock.put("l", clock.as[Int]("limit"))
        clock.removeField("limit")
        clock.put("c", clock.as[String]("color"))
        clock.removeField("color")
        if (status >= Aborted) {
          clock.removeField("timer")
        }
        game.put("clock", clock)
      }
      if (game.containsField("creatorColor")) {
        game.put("cc", game.as[String]("creatorColor"))
        game -= "creatorColor"
      }
      if (game.containsField("variant")) {
        game.put("v", game.as[Int]("variant"))
        game -= "variant"
      }
      else game.put("v", 1)
      Some(game)
    }
  }
  catch {
    case e ⇒ {
      println("%s - %s".format(game.as[String]("_id"), e.getMessage))
      None
    }
  }

  def playerPs(ps: String) = ps.toString.split(' ').map(_ take 2).mkString(" ")
}
