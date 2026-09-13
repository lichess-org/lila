package lila.team

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import scalalib.paginator.AdapterLike

private final class TeamUpdateRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given
  import TeamUpdate.*

  private val history = 90.days
  private def historyAgo = nowInstant.minus(history)
  private def dateSelect = "date".gt(historyAgo)
  private def teamSelect(team: TeamId) = bdoc("team" -> team)

  // never load the seenBy field in memory! it could be huge
  // private val project = bdoc("seenBy" -> false)

  def send(msg: DbTeamUpdate, unsubed: List[UserId]): Funit =
    val bson = toBdoc(msg).get ++ bdoc("seenBy" -> unsubed)
    coll.insert.one(bson).void

  def countUnread(teams: Team.IdsStr)(using me: Me): Fu[Int] =
    coll.secondary.countSel:
      bdoc(
        "team".in(teams.toArray),
        dateSelect,
        "seenBy".neq(me.userId)
      )

  def markSeen(team: TeamId)(using me: Me): Funit =
    coll.update
      .one(
        teamSelect(team) ++ bdoc("seenBy".neq(me.userId)),
        bdoc("$addToSet" -> bdoc("seenBy" -> me.userId)),
        multi = true
      )
      .void

  def teamLatest(team: TeamId): Fu[Option[DbTeamUpdate]] =
    coll.secondary
      .find(teamSelect(team) ++ dateSelect)
      .sort(sort.desc("date"))
      .one[DbTeamUpdate]

  def teamRecent(team: TeamId)(using Me): AdapterLike[DbTeamUpdateSeen] =
    allRecent(List(team))

  def allRecent(teams: Seq[TeamId])(using me: Me): AdapterLike[DbTeamUpdateSeen] = new:
    private val teamSelector = teams match
      case Seq(single) => teamSelect(single)
      case many => bdoc("team".in(many))
    def nbResults: Fu[Int] = coll.secondary.countSel(teamSelector ++ dateSelect)
    def slice(offset: Int, length: Int): Fu[List[DbTeamUpdateSeen]] =
      coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match(teamSelector) ->
            List(
              Sort(Descending("date")),
              Skip(offset),
              Limit(length),
              AddFields(bdoc("seenBy" -> bdoc("$in" -> barr(me.userId, "$seenBy"))))
            )
        .map: docs =>
          for
            doc <- docs
            msg <- doc.asOpt[DbTeamUpdate]
            seen <- doc.getAsOpt[Boolean]("seenBy")
          yield TeamUpdateSeen(msg, seen)

  def byTeams(teams: Team.IdsStr)(using me: Me): Fu[List[TeamUpdates[TeamId]]] =
    coll
      .aggregateList(100, _.sec): framework =>
        import framework.*
        Match(
          bdoc(
            "team".in(teams.toArray),
            "date".gt(nowInstant.minusMonths(1))
          )
        ) ->
          List(
            GroupField("team")(
              "unread" -> Sum(bdoc("$cond" -> barr(bdoc("$in" -> barr(me.userId, "$seenBy")), 0, 1))),
              "last" -> MaxField("date")
            ),
            Sort(Descending("last"))
          )
      .map: docs =>
        for
          doc <- docs
          teamId <- doc.getAsOpt[TeamId]("_id")
          unread <- doc.int("unread")
          last <- doc.getAsOpt[Instant]("last")
        yield TeamUpdates(teamId, unread, last)
