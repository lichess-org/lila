package lila.team

import scala.concurrent.duration._
import play.api.libs.iteratee._

import lila.common.paginator._
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class MemberPager(coll: Coll)(implicit system: akka.actor.ActorSystem) {

  def apply(team: Team, page: Int, maxPerPage: lila.common.MaxPerPage): Fu[Paginator[User]] =
    Paginator(
      new MemberAdapter(team),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class MemberAdapter(team: Team) extends AdapterLike[User] {

    def nbResults: Fu[Int] = fuccess(team.nbMembers)

    def slice(offset: Int, length: Int): Fu[Seq[User]] =
      coll.find($doc("team" -> team.id), $doc("user" -> true))
        .sort($sort desc "date")
        .skip(offset)
        .cursor[Bdoc]()
        .gather[List](length) map {
          _ flatMap { _.getAs[String]("user") }
        } flatMap UserRepo.usersFromSecondary
  }

  def stream(team: Team, max: Option[Int]): Enumerator[User] = {
    import reactivemongo.play.iteratees.cursorProducer
    import lila.db.dsl._
    coll.find($doc("team" -> team.id), $doc("user" -> true))
      .sort($sort desc "date")
      .batchSize(20)
      .cursor[Bdoc]()
      .bulkEnumerator(maxDocs = max | Int.MaxValue) &>
      lila.common.Iteratee.delay(1 second) &>
      Enumeratee.mapM { docs =>
        UserRepo usersFromSecondary docs.toSeq.flatMap(_.getAs[String]("user"))
      } &>
      Enumeratee.mapConcat(_.toSeq)
  }
}
