package lila.cli

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

case class CompactDb(mongodb: MongoDB) extends Command {

  def apply: IO[Unit] = for {
    _ ← compact("game")
    _ ← putStrLn("Done")
  } yield ()

  def compact(coll: String) = io {
    println("Compact game")
    mongodb.command(DBObject("compact" -> coll))
  }
}
