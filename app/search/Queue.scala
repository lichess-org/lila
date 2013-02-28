package lila.app
package search

import game.DbGame

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._

private[search] final class Queue(collection: MongoCollection)  {

  def enqueue(game: DbGame): IO[Unit] = 
    game.finished.fold(enqueue(game.id), io())

  def enqueue(id: String): IO[Unit] = io {
    collection += DBObject("_id" -> id)
  }

  def next(size: Int): IO[List[String]] = io {
    collection.find().limit(size).toList.map(_.getAs[String]("_id")).flatten 
  }

  def remove(ids: List[String]): IO[Unit] = io {
    collection.remove("_id" $in ids)
  }
}
