package lila.cli

import lila.system.model.Room
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

case class ImportRooms(mongodb: MongoDB) extends Command {

  val oldRooms = mongodb("game_room")
  val newRooms = mongodb("room")
  val bulkSize = 200
  //val max = Some(20000)
  val max: Option[Int] = None

  def apply: IO[Unit] = for {
    _ ← putStrLn("- Import rooms")
    _ ← importRooms
    _ ← putStrLn("Done")
  } yield ()

  private def importRooms: IO[Unit] = io {
    val total = oldRooms.count
    println("%d rooms to import" format total)
    println("drop db.room")
    newRooms.drop()
    println("start import")
    var it = 0
    val pool = max.fold(
      m ⇒ oldRooms.find().limit(m).grouped(bulkSize),
      oldRooms.grouped(bulkSize))
    for (olds ← pool) {
      newRooms insert (olds.toList map convert).flatten
      it = it + bulkSize
      if (it % 5000 == 0) {
        val percentage = it / total.toFloat * 100
        println("%d/%d %2.1f%%".format(it, total, percentage))
      }
    }
  }

  def convert(room: DBObject): Option[DBObject] = try {
    val messages = room.as[BasicDBList]("messages").toList
    if (messages.size < 4) None
    else {
      val msgs: List[String] = (messages map {
        case msg: BasicDBList ⇒ msg.toList match {
          case author :: message :: Nil ⇒ Some(
            Room.encode(author.toString, message.toString))
          case _ ⇒ None
        }
        case _ ⇒ None
      }).flatten
      val msgs2 =
        if (msgs.size < 40) msgs
        else msgs.reverse.take(40).reverse
      room.put("messages", msgs2)
      Some(room)
    }
  }
  catch {
    case e ⇒ {
      println("%s - %s".format(room.as[String]("_id"), e.getMessage))
      None
    }
  }
}
