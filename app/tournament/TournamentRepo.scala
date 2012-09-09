package lila
package tournament

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

class TournamentRepo(collection: MongoCollection)
    extends SalatDAO[Tournament, String](collection) {

  def byId(id: String): IO[Option[Tournament]] = io {
    findOneById(id)
  }

  def created: IO[List[Tournament]] = io {
    find(DBObject("status" -> Status.Created))
      .sort(DBObject("createdAt" -> -1))
      .toList
  }

  def saveIO(tournament: Tournament): IO[Unit] = io {
    save(tournament)
  }

  private def idSelector(id: String): DBObject = DBObject("_id" -> id)
  private def idSelector(tournament: Tournament): DBObject = idSelector(tournament.id)
}
