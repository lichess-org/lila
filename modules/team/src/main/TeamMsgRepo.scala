package lila.team

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class TeamMsgRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  // private val project = $doc("seenBy" -> false)

  def send(msg: TeamMsg[TeamId]): Funit =
    coll.insert.one(msg).void

  def countUnread(teams: Team.IdsStr)(using me: Me): Fu[Int] =
    coll.secondary.countSel:
      $doc(
        "team".$in(teams.toArray),
        "date".$gt(nowInstant.minusMonths(1)),
        "seenBy".$ne(me.userId)
      )

  def allRecent(teams: Team.IdsStr)(using me: Me): Fu[List[TeamMsgSeen[TeamId]]] =
    coll
      .aggregateList(100, _.sec): framework =>
        import framework.*
        Match($doc("team".$in(teams.toArray))) ->
          List(
            Sort(Descending("date")),
            Limit(100),
            AddFields($doc("seenBy" -> $doc("$eq" -> $arr("seenBy", me.userId))))
          )
      .map: docs =>
        for
          doc <- docs
          msg <- doc.asOpt[TeamMsg[TeamId]]
          seen <- doc.getAsOpt[Boolean]("seenBy")
        yield TeamMsgSeen(msg, seen)

  def byTeam(teams: Team.IdsStr)(using me: Me): Fu[List[TeamMsgs[TeamId]]] =
    coll
      .aggregateList(100, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            "team".$in(teams.toArray),
            "date".$gt(nowInstant.minusMonths(1))
          )
        ) ->
          List(
            GroupField("team")(
              "unread" -> Sum($doc("$cond" -> $arr($doc("$eq" -> $arr("$seenBy", me.userId)), 0, 1))),
              "last" -> MaxField("date")
            )
          )
      .map: docs =>
        for
          doc <- docs
          teamId <- doc.getAsOpt[TeamId]("_id")
          unread <- doc.int("unread")
          last <- doc.getAsOpt[Instant]("last")
        yield TeamMsgs(teamId, unread, last)
