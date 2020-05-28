package lila.team

import org.joda.time.{ DateTime, Period }
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final class TeamRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def byOrderedIds(ids: Seq[Team.ID]) = coll.byOrderedIds[Team, Team.ID](ids)(_.id)

  def byLeader(id: Team.ID, leaderId: User.ID): Fu[Option[Team]] =
    coll.one[Team]($id(id) ++ $doc("leaders" -> leaderId))

  def enabled(id: Team.ID) = coll.one[Team]($id(id) ++ enabledSelect)

  def byIdsSortPopular(ids: Seq[Team.ID]): Fu[List[Team]] =
    coll.ext
      .find($inIds(ids))
      .sort(sortPopular)
      .list[Team](100, ReadPreference.secondaryPreferred)

  def enabledTeamsByLeader(userId: User.ID): Fu[List[Team]] =
    coll.ext
      .find($doc("leaders" -> userId) ++ enabledSelect)
      .sort(sortPopular)
      .list[Team](100, ReadPreference.secondaryPreferred)

  def enabledTeamIdsByLeader(userId: User.ID): Fu[List[Team.ID]] =
    coll.ext
      .primitive[Team.ID](
        $doc("leaders" -> userId) ++ enabledSelect,
        sortPopular,
        "_id"
      )

  def leadersOf(teamId: Team.ID): Fu[Set[User.ID]] =
    coll.primitiveOne[Set[User.ID]]($id(teamId), "leaders").dmap(~_)

  def setLeaders(teamId: String, leaders: Set[User.ID]) =
    coll.updateField($id(teamId), "leaders", leaders)

  def leads(teamId: String, userId: User.ID) =
    coll.exists($id(teamId) ++ $doc("leaders" -> userId))

  def name(id: String): Fu[Option[String]] =
    coll.primitiveOne[String]($id(id), "name")

  private[team] def countCreatedSince(userId: String, duration: Period): Fu[Int] =
    coll.countSel(
      $doc(
        "createdAt" $gt DateTime.now.minus(duration),
        "createdBy" -> userId
      )
    )

  def incMembers(teamId: String, by: Int): Funit =
    coll.update.one($id(teamId), $inc("nbMembers" -> by)).void

  def enable(team: Team) = coll.updateField($id(team.id), "enabled", true)

  def disable(team: Team) = coll.updateField($id(team.id), "enabled", false)

  def addRequest(teamId: String, request: Request): Funit =
    coll.update
      .one(
        $id(teamId) ++ $doc("requests.user" $ne request.user),
        $push("requests" -> request.user)
      )
      .void

  def cursor =
    coll.ext
      .find(enabledSelect)
      .cursor[Team](ReadPreference.secondaryPreferred)

  def countRequestsOfLeader(userId: User.ID, requestColl: Coll): Fu[Int] =
    coll
      .aggregateOne(readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match($doc("leaders" -> userId)) -> List(
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> requestColl.name,
                "localField"   -> "_id",
                "foreignField" -> "team",
                "as"           -> "requests"
              )
            )
          ),
          Group(BSONNull)(
            "nb" -> Sum($doc("$size" -> "$requests"))
          )
        )
      }
      .map(~_.flatMap(_.int("nb")))

  private[team] val enabledSelect = $doc("enabled" -> true)

  private[team] val sortPopular = $sort desc "nbMembers"
}
