package lila.user

import org.joda.time.DateTime

import reactivemongo.api.{
  CursorProducer,
  ReadConcern,
  ReadPreference,
  WriteConcern
}
import reactivemongo.bson._

import lila.common.ApiVersion
import lila.common.EmailAddress
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.rating.{ Perf, PerfType }

object UserRepo {

  import User.userBSONHandler

  import User.ID
  import User.{ BSONFields => F }

  // dirty
  private[user] val coll = Env.current.userColl
  import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, Group, SumField }

  def withColl[A](f: Coll => A): A = f(coll)

  val normalize = User normalize _

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledSelect).sort($sort desc "count.game")
      .cursor[User]().list(nb)

  def byId(id: ID): Fu[Option[User]] = coll.byId[User](id)

  def byIds(ids: Iterable[ID]): Fu[List[User]] = coll.byIds[User](ids)

  def byIdsSecondary(ids: Iterable[ID]): Fu[List[User]] = coll.byIds[User](ids, ReadPreference.secondaryPreferred)

  def byEmail(email: EmailAddress): Fu[Option[User]] =
    coll.find($doc(F.email -> email)).one[User]

  def byPrevEmail(email: EmailAddress): Fu[List[User]] =
    coll.find($doc(F.prevEmail -> email)).cursor[User]().list

  def idByEmail(email: EmailAddress): Fu[Option[String]] =
    coll.primitiveOne[String]($doc(F.email -> email), "_id")

  def enabledByEmail(email: EmailAddress): Fu[Option[User]] = byEmail(email) map (_ filter (_.enabled))

  def pair(x: Option[ID], y: Option[ID]): Fu[(Option[User], Option[User])] =
    coll.byIds[User](List(x, y).flatten) map { users =>
      x.??(xx => users.find(_.id == xx)) ->
        y.??(yy => users.find(_.id == yy))
    }

  def pair(x: ID, y: ID): Fu[Option[(User, User)]] =
    coll.byIds[User](List(x, y)) map { users =>
      for {
        xx <- users.find(_.id == x)
        yy <- users.find(_.id == y)
      } yield xx -> yy
    }

  def byOrderedIds(ids: Seq[ID], readPreference: ReadPreference): Fu[List[User]] =
    coll.byOrderedIds[User, User.ID](ids, readPreference = readPreference)(_.id)

  def idsMap(ids: Seq[ID], readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[Map[User.ID, User]] =
    coll.idsMap[User, User.ID](ids, readPreference)(_.id)

  def usersFromSecondary(userIds: Seq[ID]): Fu[List[User]] =
    byOrderedIds(userIds, ReadPreference.secondaryPreferred)

  def enabledByIds(ids: Iterable[ID]): Fu[List[User]] =
    coll.find(enabledSelect ++ $inIds(ids)).
      cursor[User](ReadPreference.secondaryPreferred).list

  def enabledById(id: ID): Fu[Option[User]] =
    coll.find(enabledSelect ++ $id(id)).one[User]

  def named(username: String): Fu[Option[User]] =
    coll.byId[User](normalize(username))

  def nameds(usernames: List[String]): Fu[List[User]] =
    coll.byIds[User](usernames.map(normalize))

  // expensive, send to secondary
  def byIdsSortRatingNoBot(ids: Iterable[ID], nb: Int): Fu[List[User]] =
    coll.find($inIds(ids) ++ goodLadSelectBson ++ botSelect(false))
      .sort($sort desc "perfs.standard.gl.r")
      .cursor[User](ReadPreference.secondaryPreferred).list(nb)

  // expensive, send to secondary
  def idsByIdsSortRating(ids: Iterable[ID], nb: Int): Fu[List[User.ID]] =
    coll.find(
      $inIds(ids) ++ goodLadSelectBson,
      projection = Some($id(true))
    ).sort($doc(s"perfs.standard.gl.r" -> -1))
      .cursor[Bdoc](ReadPreference.secondaryPreferred).list(nb).map {
        _.flatMap { _.getAs[User.ID]("_id") }
      }

  private[user] def allSortToints(nb: Int): Fu[List[User]] =
    coll.find($empty).sort($sort desc F.toints).cursor[User]().list(nb)

  def usernameById(id: ID) =
    coll.primitiveOne[User.ID]($id(id), F.username)

  def usernamesByIds(ids: List[ID]) = coll.distinct[String, List](
    F.username, $inIds(ids).some, ReadConcern.Local, None
  )

  def orderByGameCount(u1: User.ID, u2: User.ID): Fu[Option[(User.ID, User.ID)]] = {
    coll.find(
      $inIds(List(u1, u2)),
      projection = Some($doc(s"${F.count}.game" -> true))
    ).cursor[Bdoc]().list map { docs =>
        docs.sortBy {
          _.getAs[Bdoc](F.count).flatMap(_.getAs[BSONNumberLike]("game")).??(_.toInt)
        }.map(_.getAs[User.ID]("_id")).flatten match {
          case List(u1, u2) => (u1, u2).some
          case _ => none
        }
      }
  }

  def firstGetsWhite(u1: User.ID, u2: User.ID): Fu[Boolean] = coll.find(
    $inIds(List(u1, u2)),
    projection = Some($id(true))
  ).sort($doc(F.colorIt -> 1)).one[Bdoc].map {
      _.fold(scala.util.Random.nextBoolean) { doc =>
        doc.getAs[User.ID]("_id") contains u1
      }
    }.addEffect { v =>
      incColor(u1, if (v) 1 else -1)
      incColor(u2, if (v) -1 else 1)
    }

  def firstGetsWhite(u1O: Option[User.ID], u2O: Option[User.ID]): Fu[Boolean] =
    (u1O |@| u2O).tupled.fold(fuccess(scala.util.Random.nextBoolean)) {
      case (u1, u2) => firstGetsWhite(u1, u2)
    }

  def incColor(userId: User.ID, value: Int): Unit = {
    coll.update(false, WriteConcern.Unacknowledged).one(
      q = $id(userId) ++ (value < 0).??($doc("colorIt" $gt -3)),
      u = $inc(F.colorIt -> value)
    )

    ()
  }

  def lichess: Fu[Option[User]] = byId(User.lichessId)

  val irwinId = "irwin"
  def irwin = byId(irwinId)

  def setPerfs(user: User, perfs: Perfs, prev: Perfs) = {
    val diff = PerfType.all flatMap { pt =>
      perfs(pt).nb != prev(pt).nb option {
        BSONElement(
          s"${F.perfs}.${pt.key}", Perf.perfBSONHandler.write(perfs(pt))
        )
      }
    }

    diff.nonEmpty ?? coll.update.one(
      $id(user.id), $doc("$set" -> $doc(diff))
    ).void
  }

  def setPerf(userId: String, pt: PerfType, perf: Perf): Funit =
    coll.update.one(q = $id(userId), u = $set(
      s"${F.perfs}.${pt.key}" -> Perf.perfBSONHandler.write(perf)
    )).void

  def setProfile(id: ID, profile: Profile): Funit = coll.update.one(
    $id(id), $set(F.profile -> Profile.profileBSONHandler.write(profile))
  ).void

  def addTitle(id: ID, title: String): Funit =
    coll.updateField($id(id), F.title, title).void

  def removeTitle(id: ID): Funit =
    coll.unsetField($id(id), F.title).void

  def setPlayTime(id: ID, playTime: User.PlayTime): Funit = coll.update.one(
    $id(id), $set(F.playTime -> User.playTimeHandler.write(playTime))
  ).void

  def getPlayTime(id: ID): Fu[Option[User.PlayTime]] =
    coll.primitiveOne[User.PlayTime]($id(id), F.playTime)

  val enabledSelect = $doc(F.enabled -> true)
  def engineSelect(v: Boolean) = $doc(F.engine -> (if (v) $boolean(true) else $ne(true)))
  def trollSelect(v: Boolean) = $doc(F.troll -> (if (v) $boolean(true) else $ne(true)))
  def boosterSelect(v: Boolean) = $doc(F.booster -> (if (v) $boolean(true) else $ne(true)))
  def stablePerfSelect(perf: String) = $doc(
    s"perfs.$perf.nb" -> $gte(30),
    s"perfs.$perf.gl.d" -> $lt(lila.rating.Glicko.provisionalDeviation)
  )
  val goodLadSelect = enabledSelect ++ engineSelect(false) ++ boosterSelect(false)
  val goodLadSelectBson = $doc(
    F.enabled -> true,
    F.engine $ne true,
    F.booster $ne true
  )
  val patronSelect = $doc(s"${F.plan}.active" -> true)

  def sortPerfDesc(perf: String) = $sort desc s"perfs.$perf.gl.r"
  val sortCreatedAtDesc = $sort desc F.createdAt

  def incNbGames(id: ID, rated: Boolean, ai: Boolean, result: Int, totalTime: Option[Int], tvTime: Option[Int]) = {
    val incs: List[BSONElement] = List(
      "count.game".some,
      rated option "count.rated",
      ai option "count.ai",
      (result match {
        case -1 => "count.loss".some
        case 1 => "count.win".some
        case 0 => "count.draw".some
        case _ => none
      }),
      (result match {
        case -1 => "count.lossH".some
        case 1 => "count.winH".some
        case 0 => "count.drawH".some
        case _ => none
      }) ifFalse ai
    ).flatten.map(k => BSONElement(k, BSONInteger(1))) ::: List(
        totalTime map (v => BSONElement(s"${F.playTime}.total", BSONInteger(v + 2))),
        tvTime map (v => BSONElement(s"${F.playTime}.tv", BSONInteger(v + 2)))
      ).flatten

    coll.update.one($id(id), $inc(incs))
  }

  def incToints(id: ID, nb: Int) =
    coll.update.one($id(id), $inc("toints" -> nb))

  def removeAllToints = coll.update.one($empty, $unset("toints"), multi = true)

  def create(
    username: String,
    passwordHash: HashedPassword,
    email: EmailAddress,
    blind: Boolean,
    mobileApiVersion: Option[ApiVersion],
    mustConfirmEmail: Boolean
  ): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        val doc = newUser(username, passwordHash, email, blind, mobileApiVersion, mustConfirmEmail) ++
          ("len" -> BSONInteger(username.size))
        coll.insert.one(doc) >> named(normalize(username))
      }
    }

  def nameExists(username: String): Fu[Boolean] = idExists(normalize(username))
  def idExists(id: String): Fu[Boolean] = coll exists $id(id)

  /**
   * Filters out invalid usernames and returns the IDs for those usernames
   *
   * @param usernames Usernames to filter out the non-existent usernames from, and return the IDs for
   * @return A list of IDs for the usernames that were given that were valid
   */
  def existingUsernameIds(usernames: Set[String]): Fu[List[User.ID]] =
    coll.primitive[String]($inIds(usernames.map(normalize)), F.id)

  def userIdsLike(text: String, max: Int = 10): Fu[List[User.ID]] =
    User.couldBeUsername(text) ?? {
      coll.find(
        $doc(F.id $startsWith normalize(text)) ++ enabledSelect,
        projection = Some($doc(F.id -> true))
      )
        .sort($doc("len" -> 1))
        .cursor[Bdoc](ReadPreference.secondaryPreferred).list(max).map {
          _ flatMap { _.getAs[String](F.id) }
        }
    }

  def toggleEngine(id: ID): Funit = coll.fetchUpdate[User]($id(id)) { u =>
    $set("engine" -> !u.engine)
  }

  def setEngine(id: ID, v: Boolean): Funit = coll.updateField($id(id), "engine", v).void

  def setBooster(id: ID, v: Boolean): Funit = coll.updateField($id(id), "booster", v).void

  def setReportban(id: ID, v: Boolean): Funit = coll.updateField($id(id), "reportban", v).void

  def setRankban(id: ID, v: Boolean): Funit = coll.updateField($id(id), "rankban", v).void

  def setIpBan(id: ID, v: Boolean) = coll.updateField($id(id), "ipBan", v).void

  def setKid(user: User, v: Boolean) = coll.updateField($id(user.id), "kid", v)

  def isKid(id: ID) = coll.exists($id(id) ++ $doc("kid" -> true))

  def updateTroll(user: User) = coll.updateField($id(user.id), "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = coll.exists($id(id) ++ engineSelect(true))

  def isTroll(id: ID): Fu[Boolean] = coll.exists($id(id) ++ trollSelect(true))

  def setRoles(id: ID, roles: List[String]) = coll.updateField($id(id), "roles", roles)

  def disableTwoFactor(id: ID) = coll.update.one($id(id), $unset(F.totpSecret))

  def setupTwoFactor(id: ID, totp: TotpSecret): Funit =
    coll.update.one( // never overwrite existing secret
      q = $id(id) ++ (F.totpSecret $exists false),
      u = $set(F.totpSecret -> totp.secret)
    ).void

  def enable(id: ID) = coll.updateField($id(id), F.enabled, true)

  def disable(user: User, keepEmail: Boolean) = coll.update.one(
    q = $id(user.id), u = $set(F.enabled -> false) ++ $unset(F.roles) ++ {
      if (keepEmail) $empty
      else $doc("$rename" -> $doc(F.email -> F.prevEmail))
    }
  )

  import Authenticator._
  def getPasswordHash(id: User.ID): Fu[Option[String]] =
    coll.byId[AuthData](id, authProjection) map {
      _.map { _.hashToken }
    }

  def email(id: ID, email: EmailAddress): Funit = coll.update.one($id(id), $set(
    F.email -> email
  ) ++ $unset(F.prevEmail)).void

  def email(id: ID): Fu[Option[EmailAddress]] = coll.primitiveOne[EmailAddress]($id(id), F.email)

  def emails(id: ID): Fu[User.Emails] =
    coll.find($id(id), Some($doc(F.email -> true, F.prevEmail -> true))).
      one[Bdoc].map { doc =>
        User.Emails(
          current = doc.flatMap(_.getAs[EmailAddress](F.email)),
          previous = doc.flatMap(_.getAs[EmailAddress](F.prevEmail))
        )
      }

  def hasEmail(id: ID): Fu[Boolean] = email(id).map(_.isDefined)

  def setBot(user: User): Funit =
    if (user.count.game > 0) fufail("You already have games played. Make a new account.")
    else coll.updateField($id(user.id), F.title, User.botTitle).void

  private def botSelect(v: Boolean) =
    if (v) $doc(F.title -> User.botTitle)
    else $doc(F.title -> $ne(User.botTitle))

  def getTitle(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.title)

  def setPlan(user: User, plan: Plan): Funit = {
    implicit val pbw: BSONValueWriter[Plan] = Plan.planBSONHandler
    coll.updateField($id(user.id), "plan", plan).void
  }

  private def docPerf(doc: Bdoc, perfType: PerfType): Option[Perf] =
    doc.getAs[Bdoc](F.perfs).flatMap(_.getAs[Perf](perfType.key))

  def perfOf(id: ID, perfType: PerfType): Fu[Option[Perf]] = coll.find(
    $id(id),
    projection = Some($doc(s"${F.perfs}.${perfType.key}" -> true))
  ).one[Bdoc].map {
      _.flatMap { docPerf(_, perfType) }
    }

  def perfOf(ids: Iterable[ID], perfType: PerfType): Fu[Map[ID, Perf]] = coll.find(
    $inIds(ids),
    projection = Some($doc(s"${F.perfs}.${perfType.key}" -> true))
  ).cursor[Bdoc]().list.map { docs =>
      docs.map { doc =>
        ~doc.getAs[ID]("_id") -> docPerf(doc, perfType).getOrElse(Perf.default)
      }(scala.collection.breakOut)
    }

  def setSeenAt(id: ID): Unit = {
    coll.update(false, WriteConcern.Unacknowledged)
      .one($id(id), $set("seenAt" -> DateTime.now))

    ()
  }

  def recentlySeenNotKidIdsCursor(since: DateTime)(implicit cursorProducer: CursorProducer[Bdoc]) =
    coll.find($doc(
      F.enabled -> true,
      "seenAt" $gt since,
      "count.game" $gt 9,
      "kid" $ne true
    ), projection = Some($id(true)))
      .cursor[Bdoc](ReadPreference.secondary)

  def setLang(id: ID, lang: String) = coll.updateField($id(id), "lang", lang).void

  def langOf(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), "lang")

  def idsSumToints(ids: Iterable[String]): Fu[Int] =
    ids.nonEmpty ?? coll.aggregateOne(
      Match($inIds(ids)),
      List(Group(BSONNull)(F.toints -> SumField(F.toints))),
      ReadPreference.secondaryPreferred
    ).map {
        _ flatMap { _.getAs[Int](F.toints) }
      }.map(~_)

  def filterByEngine(userIds: Iterable[User.ID]): Fu[Set[User.ID]] =
    coll.distinctWithReadPreference[String, Set](
      F.id,
      Some($inIds(userIds) ++ engineSelect(true)),
      ReadPreference.secondaryPreferred
    )

  def filterByEnabledPatrons(userIds: List[User.ID]): Fu[Set[User.ID]] =
    coll.distinct[String, Set](F.id, Some($inIds(userIds) ++ enabledSelect ++ patronSelect), ReadConcern.Local, None)

  def userIdsWithRoles(roles: List[String]): Fu[Set[User.ID]] =
    coll.distinct[String, Set]("_id", $doc("roles" $in roles).some, ReadConcern.Local, None)

  def countEngines(userIds: List[User.ID]): Fu[Int] =
    coll.countSel(
      $inIds(userIds) ++ engineSelect(true),
      ReadPreference.secondaryPreferred
    )

  def containsEngine(userIds: List[User.ID]): Fu[Boolean] =
    coll.exists($inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: User.ID): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.mustConfirmEmail $exists true))

  def setEmailConfirmed(id: User.ID): Funit =
    coll.update.one($id(id), $unset(F.mustConfirmEmail)).void

  def erase(user: User): Funit = coll.update.one(
    $id(user.id), $unset(F.profile) ++ $set("erasedAt" -> DateTime.now)
  ).void

  def isErased(user: User): Fu[User.Erased] =
    coll.exists($id(user.id) ++ $doc("erasedAt" $exists true)) map User.Erased.apply

  private def newUser(
    username: String,
    passwordHash: HashedPassword,
    email: EmailAddress,
    blind: Boolean,
    mobileApiVersion: Option[ApiVersion],
    mustConfirmEmail: Boolean
  ) = {

    implicit def countHandler = Count.countBSONHandler
    implicit def perfsHandler = Perfs.perfsBSONHandler
    import lila.db.BSON.BSONJodaDateTimeHandler

    $doc(
      F.id -> normalize(username),
      F.username -> username,
      F.email -> email,
      F.mustConfirmEmail -> mustConfirmEmail.option(DateTime.now),
      F.bpass -> passwordHash,
      F.perfs -> $empty,
      F.count -> Count.default,
      F.enabled -> true,
      F.createdAt -> DateTime.now,
      F.createdWithApiVersion -> mobileApiVersion.map(_.value),
      F.seenAt -> DateTime.now,
      F.playTime -> User.PlayTime(0, 0)
    ) ++ {
        if (blind) $doc("blind" -> true) else $empty
      }
  }
}
