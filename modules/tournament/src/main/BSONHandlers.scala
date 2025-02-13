package lila.tournament

import chess.Mode
import chess.format.Fen
import chess.variant.Variant
import chess.IntRating
import reactivemongo.api.bson.*

import lila.core.id.TourPlayerId
import lila.core.tournament.Status
import lila.core.tournament.leaderboard.Ratio
import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.gathering.Thematic
import lila.rating.PerfType

object BSONHandlers:

  private[tournament] given BSONHandler[Status] = valueMapHandler(Status.byId)(_.id)

  private[tournament] given BSONHandler[Schedule.Freq] = tryHandler(
    { case BSONString(v) => Schedule.Freq.byName.get(v).toTry(s"No such freq: $v") },
    x => BSONString(x.name)
  )

  given BSONWriter[Scheduled] = Macros.handler

  private given BSONHandler[chess.Clock.Config] = clockConfigHandler

  given BSONHandler[lila.ui.Icon]              = isoHandler[lila.ui.Icon, String]
  private given BSONDocumentHandler[Spotlight] = Macros.handler

  given BSONDocumentHandler[TeamBattle] = Macros.handler

  private given BSONHandler[Ratio] = BSONIntegerHandler.as(
    i => Ratio(i.toDouble / 100_000),
    r => (r.value * 100_000).toInt
  )

  import TournamentCondition.bsonHandler

  given tourHandler: BSON[Tournament] with
    def reads(r: BSON.Reader) =
      val variant = Variant.idOrDefault(r.getO[Variant.Id]("variant"))
      val position: Option[Fen.Standard] =
        r.getO[Fen.Full]("fen")
          .map(_.opening: Fen.Standard)
          .filter(_ != Fen.Standard.initial)
          .orElse(r.getO[chess.opening.Eco]("eco").flatMap(Thematic.byEco).map(_.fen)) // for BC
      val startsAt   = r.date("startsAt")
      val conditions = r.getD[TournamentCondition.All]("conditions")
      Tournament(
        id = r.get[TourId]("_id"),
        name = r.str("name"),
        status = r.get[Status]("status"),
        clock = r.get[chess.Clock.Config]("clock"),
        minutes = r.int("minutes"),
        variant = variant,
        position = position,
        mode = r.intO("mode").flatMap(Mode.apply).getOrElse(Mode.Rated),
        password = r.strO("password"),
        conditions = conditions,
        teamBattle = r.getO[TeamBattle]("teamBattle"),
        noBerserk = r.boolD("noBerserk"),
        noStreak = r.boolD("noStreak"),
        schedule = for
          doc  <- r.getO[Bdoc]("schedule")
          freq <- doc.getAsOpt[Schedule.Freq]("freq")
          at = doc.getAsOpt[LocalDateTime]("at") | startsAt.dateTime
        yield Scheduled(freq, at),
        nbPlayers = r.int("nbPlayers"),
        createdAt = r.date("createdAt"),
        createdBy = r.getO[UserId]("createdBy") | UserId.lichess,
        startsAt = startsAt,
        winnerId = r.getO[UserId]("winner"),
        featuredId = r.getO[GameId]("featured"),
        spotlight = r.getO[Spotlight]("spotlight"),
        description = r.strO("description"),
        hasChat = r.boolO("chat").getOrElse(true)
      )
    def writes(w: BSON.Writer, o: Tournament) =
      $doc(
        "_id"         -> o.id,
        "name"        -> o.name,
        "status"      -> o.status,
        "clock"       -> o.clock,
        "minutes"     -> o.minutes,
        "variant"     -> o.variant.some.filterNot(_.standard).map(_.id),
        "fen"         -> o.position,
        "mode"        -> o.mode.some.filterNot(_.rated).map(_.id),
        "password"    -> o.password,
        "conditions"  -> o.conditions.nonEmpty.option(o.conditions),
        "teamBattle"  -> o.teamBattle,
        "noBerserk"   -> w.boolO(o.noBerserk),
        "noStreak"    -> w.boolO(o.noStreak),
        "schedule"    -> o.schedule,
        "nbPlayers"   -> o.nbPlayers,
        "createdAt"   -> w.date(o.createdAt),
        "createdBy"   -> o.nonLichessCreatedBy,
        "startsAt"    -> w.date(o.startsAt),
        "winner"      -> o.winnerId,
        "featured"    -> o.featuredId,
        "spotlight"   -> o.spotlight,
        "description" -> o.description,
        "chat"        -> (!o.hasChat).option(false)
      )

  given BSON[Player] with
    def reads(r: BSON.Reader) =
      Player(
        _id = r.get[TourPlayerId]("_id"),
        tourId = r.get("tid"),
        userId = r.get("uid"),
        rating = r.get("r"),
        provisional = r.yesnoD("pr"),
        withdraw = r.boolD("w"),
        score = r.intD("s"),
        fire = r.boolD("f"),
        performance = r.getO[IntRating]("e"),
        team = r.getO[TeamId]("t"),
        bot = r.boolD("bot")
      )
    def writes(w: BSON.Writer, o: Player) =
      $doc(
        "_id" -> o._id,
        "tid" -> o.tourId,
        "uid" -> o.userId,
        "r"   -> o.rating,
        "pr"  -> w.yesnoO(o.provisional),
        "w"   -> w.boolO(o.withdraw),
        "s"   -> w.intO(o.score),
        "m"   -> o.magicScore,
        "f"   -> w.boolO(o.fire),
        "e"   -> o.performance,
        "t"   -> o.team,
        "bot" -> w.boolO(o.bot)
      )

  given pairingHandler: BSON[Pairing] with
    def reads(r: BSON.Reader) =
      val users = r.strsD("u")
      val user1 = UserId(users.headOption.err("tournament pairing first user"))
      val user2 = UserId(users.lift(1).err("tournament pairing second user"))
      Pairing(
        id = r.get[GameId]("_id"),
        tourId = r.get[TourId]("tid"),
        status = chess.Status(r.int("s")).err("tournament pairing status"),
        user1 = user1,
        user2 = user2,
        winner = r.boolO("w").map {
          if _ then user1
          else user2
        },
        turns = r.intO("t"),
        berserk1 = r.intO("b1").fold(r.boolD("b1"))(1 ==), // it used to be int = 0/1
        berserk2 = r.intO("b2").fold(r.boolD("b2"))(1 ==)
      )
    def writes(w: BSON.Writer, o: Pairing) =
      $doc(
        "_id" -> o.id,
        "tid" -> o.tourId,
        "s"   -> o.status.id,
        "u"   -> BSONArray(o.user1, o.user2),
        "w"   -> o.winner.map(o.user1 ==),
        "t"   -> o.turns,
        "b1"  -> w.boolO(o.berserk1),
        "b2"  -> w.boolO(o.berserk2)
      )

  given BSON[LeaderboardApi.Entry] with
    def reads(r: BSON.Reader) =
      LeaderboardApi.Entry(
        id = r.get("_id"),
        userId = r.get("u"),
        tourId = r.get("t"),
        nbGames = r.int("g"),
        score = r.int("s"),
        rank = r.get("r"),
        rankRatio = r.get("w"),
        freq = r.intO("f").flatMap(Schedule.Freq.byId.get),
        perf = PerfType.byId.get(r.get("v")).err("Invalid leaderboard perf"),
        date = r.date("d")
      )

    def writes(w: BSON.Writer, o: LeaderboardApi.Entry) =
      $doc(
        "_id" -> o.id,
        "u"   -> o.userId,
        "t"   -> o.tourId,
        "g"   -> o.nbGames,
        "s"   -> o.score,
        "r"   -> o.rank,
        "w"   -> o.rankRatio,
        "f"   -> o.freq.map(_.id),
        "v"   -> o.perf.id,
        "d"   -> w.date(o.date)
      )

  given leaderboardAggResult: BSONDocumentHandler[LeaderboardApi.ChartData.AggregationResult] = Macros.handler
