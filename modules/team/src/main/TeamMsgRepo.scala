package lila.team

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class TeamMsgRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  // never load the seenBy field in memory! it could be huge
  // private val project = $doc("seenBy" -> false)

  def send(msg: TeamMsg[TeamId]): Funit =
    val bson = toBdoc(msg).get ++ $doc("seenBy" -> $arr())
    coll.insert.one(bson).void

  def countUnread(teams: Team.IdsStr)(using me: Me): Fu[Int] =
    coll.secondary.countSel:
      $doc(
        "team".$in(teams.toArray),
        "date".$gt(nowInstant.minusMonths(1)),
        "seenBy".$ne(me.userId)
      )

  def markSeen(team: TeamId)(using me: Me): Funit =
    coll.update
      .one(
        $doc("team" -> team, "seenBy".$ne(me.userId)),
        $doc("$addToSet" -> $doc("seenBy" -> me.userId)),
        multi = true
      )
      .void

  def teamRecent(team: TeamId)(using me: Me): Fu[List[TeamMsgSeen[TeamId]]] =
    allRecent(Team.IdsStr(List(team)))

  def allRecent(teams: Team.IdsStr)(using me: Me): Fu[List[TeamMsgSeen[TeamId]]] =
    coll
      .aggregateList(100, _.sec): framework =>
        import framework.*
        Match($doc("team".$in(teams.toArray))) ->
          List(
            Sort(Descending("date")),
            Limit(100),
            AddFields($doc("seenBy" -> $doc("$in" -> $arr(me.userId, "$seenBy"))))
          )
      .map: docs =>
        for
          doc <- docs
          msg <- doc.asOpt[TeamMsg[TeamId]]
          seen <- doc.getAsOpt[Boolean]("seenBy")
        yield TeamMsgSeen(msg, seen)

  def byTeams(teams: Team.IdsStr)(using me: Me): Fu[List[TeamMsgs[TeamId]]] =
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
              "unread" -> Sum($doc("$cond" -> $arr($doc("$in" -> $arr(me.userId, "$seenBy")), 0, 1))),
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
        yield TeamMsgs(teamId, unread, last)
