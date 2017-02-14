package lila.teamSearch

import lila.search._
import lila.team.{ Team, TeamRepo }

import play.api.libs.json._

final class TeamSearchApi(client: ESClient) extends SearchReadApi[Team, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      TeamRepo byOrderedIds res.ids
    }

  def count(query: Query) = client.count(query) map (_.count)

  def store(team: Team) = client.store(Id(team.id), toDoc(team))

  private def toDoc(team: Team) = Json.obj(
    Fields.name -> team.name,
    Fields.description -> team.description.take(10000),
    Fields.location -> team.location,
    Fields.nbMembers -> team.nbMembers
  )

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      import play.api.libs.iteratee._
      import reactivemongo.api.ReadPreference
      import reactivemongo.play.iteratees.cursorProducer
      import lila.db.dsl._

      logger.info(s"Index to ${c.index.name}")

      val batchSize = 200
      val maxEntries = Int.MaxValue

      TeamRepo.cursor(
        selector = $doc("enabled" -> true),
        readPreference = ReadPreference.secondaryPreferred
      )
        .enumerator(maxEntries) &>
        Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
        Iteratee.foldM[Seq[Team], Int](0) {
          case (nb, teams) =>
            c.storeBulk(teams.toList map (t => Id(t.id) -> toDoc(t))) inject {
              logger.info(s"Indexed $nb teams")
              nb + teams.size
            }
        }
    } >> client.refresh
    case _ => funit
  }
}
