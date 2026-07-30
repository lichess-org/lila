package lila.team

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import scalalib.paginator.AdapterLike

private final class TeamMsgRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  private val history = 90.days
  private def historyAgo = nowInstant.minus(history)
  private def dateSelect = "date".$gt(historyAgo)
  private def teamSelect(team: TeamId) = $doc("team" -> team)

  // never load the seenBy field in memory! it could be huge
  // private val project = $doc("seenBy" -> false)

  def send(msg: TeamMsg[TeamId], unsubed: List[UserId]): Funit =
    val bson = toBdoc(msg).get ++ $doc("seenBy" -> unsubed)
    coll.insert.one(bson).void

  def countUnread(teams: Team.IdsStr)(using me: Me): Fu[Int] =
    coll.secondary.countSel:
      $doc(
        "team".$in(teams.toArray),
        dateSelect,
        "seenBy".$ne(me.userId)
      )

  def markSeen(team: TeamId)(using me: Me): Funit =
    coll.update
      .one(
        teamSelect(team) ++ $doc("seenBy".$ne(me.userId)),
        $doc("$addToSet" -> $doc("seenBy" -> me.userId)),
        multi = true
      )
      .void

  def teamLatest(team: TeamId): Fu[Option[TeamMsg[TeamId]]] =
    coll.secondary
      .find(teamSelect(team) ++ dateSelect)
      .sort($sort.desc("date"))
      .one[TeamMsg[TeamId]]

  def teamRecent(team: TeamId)(using Me): AdapterLike[TeamMsgSeen[TeamId]] =
    allRecent(Team.IdsStr(List(team)))

  def allRecent(teams: Team.IdsStr)(using me: Me): AdapterLike[TeamMsgSeen[TeamId]] = new:
    private val teamSelector = teams.toArray match
      case Array(single) => teamSelect(single)
      case many => $doc("team".$in(many))
    def nbResults: Fu[Int] = coll.secondary.countSel(teamSelector ++ dateSelect)
    def slice(offset: Int, length: Int): Fu[List[TeamMsgSeen[TeamId]]] =
      coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match(teamSelector) ->
            List(
              Sort(Descending("date")),
              Skip(offset),
              Limit(length),
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
