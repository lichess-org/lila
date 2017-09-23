package lila.user

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.api.commands.GetLastError
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
  private val coll = Env.current.userColl
  import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, Group, SumField }

  val normalize = User normalize _

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledSelect).sort($sort desc "count.game").cursor[User]().gather[List](nb)

  def byId(id: ID): Fu[Option[User]] = coll.byId[User](id)

  def byIds(ids: Iterable[ID]): Fu[List[User]] = coll.byIds[User](ids)

  def byIdsSecondary(ids: Iterable[ID]): Fu[List[User]] = coll.byIds[User](ids, ReadPreference.secondaryPreferred)

  def byEmail(email: EmailAddress): Fu[Option[User]] = coll.uno[User]($doc(F.email -> email))
  def byPrevEmail(email: EmailAddress): Fu[List[User]] = coll.find($doc(F.prevEmail -> email)).list[User]()

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
    coll.byOrderedIds[User, User.ID](ids, readPreference)(_.id)

  def idsMap(ids: Seq[ID], readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[Map[User.ID, User]] =
    coll.idsMap[User, User.ID](ids, readPreference)(_.id)

  def usersFromSecondary(userIds: Seq[ID]): Fu[List[User]] =
    byOrderedIds(userIds, ReadPreference.secondaryPreferred)

  def enabledByIds(ids: Iterable[ID]): Fu[List[User]] =
    coll.list[User](enabledSelect ++ $inIds(ids), ReadPreference.secondaryPreferred)

  def enabledById(id: ID): Fu[Option[User]] =
    coll.uno[User](enabledSelect ++ $id(id))

  def named(username: String): Fu[Option[User]] = coll.byId[User](normalize(username))

  def nameds(usernames: List[String]): Fu[List[User]] = coll.byIds[User](usernames.map(normalize))

  // expensive, send to secondary
  def byIdsSortRating(ids: Iterable[ID], nb: Int): Fu[List[User]] =
    coll.find($inIds(ids) ++ goodLadSelectBson)
      .sort($sort desc "perfs.standard.gl.r")
      .cursor[User](ReadPreference.secondaryPreferred)
      .gather[List](nb)

  // expensive, send to secondary
  def idsByIdsSortRating(ids: Iterable[ID], nb: Int): Fu[List[User.ID]] =
    coll.find(
      $inIds(ids) ++ goodLadSelectBson,
      $id(true)
    )
      .sort($doc(s"perfs.standard.gl.r" -> -1))
      .list[Bdoc](nb, ReadPreference.secondaryPreferred).map {
        _.flatMap { _.getAs[User.ID]("_id") }
      }

  private[user] def allSortToints(nb: Int) =
    coll.find($empty).sort($sort desc F.toints).cursor[User]().gather[List](nb)

  def usernameById(id: ID) =
    coll.primitiveOne[User.ID]($id(id), F.username)

  def usernamesByIds(ids: List[ID]) =
    coll.distinct[String, List](F.username, $inIds(ids).some)

  def orderByGameCount(u1: User.ID, u2: User.ID): Fu[Option[(User.ID, User.ID)]] = {
    coll.find(
      $inIds(List(u1, u2)),
      $doc(s"${F.count}.game" -> true)
    ).cursor[Bdoc]().gather[List]() map { docs =>
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
    $id(true)
  ).sort($doc(F.colorIt -> 1)).uno[Bdoc].map {
      _.fold(scala.util.Random.nextBoolean) { doc =>
        doc.getAs[User.ID]("_id") contains u1
      }
    }.addEffect { v =>
      incColor(u1, v.fold(1, -1))
      incColor(u2, v.fold(-1, 1))
    }

  def firstGetsWhite(u1O: Option[User.ID], u2O: Option[User.ID]): Fu[Boolean] =
    (u1O |@| u2O).tupled.fold(fuccess(scala.util.Random.nextBoolean)) {
      case (u1, u2) => firstGetsWhite(u1, u2)
    }

  def incColor(userId: User.ID, value: Int): Unit =
    coll.update($id(userId), $inc(F.colorIt -> value), writeConcern = GetLastError.Unacknowledged)

  val lichessId = "lichess"
  def lichess = byId(lichessId)

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
    diff.nonEmpty ?? coll.update(
      $id(user.id),
      $doc("$set" -> $doc(diff))
    ).void
  }

  def setPerf(userId: String, pt: PerfType, perf: Perf) =
    coll.update($id(userId), $set(
      s"${F.perfs}.${pt.key}" -> Perf.perfBSONHandler.write(perf)
    )).void

  def setProfile(id: ID, profile: Profile): Funit =
    coll.update(
      $id(id),
      $set(F.profile -> Profile.profileBSONHandler.write(profile))
    ).void

  def setTitle(id: ID, title: Option[String]): Funit = title match {
    case Some(t) => coll.updateField($id(id), F.title, t).void
    case None => coll.update($id(id), $unset(F.title)).void
  }

  def setPlayTime(id: ID, playTime: User.PlayTime): Funit =
    coll.update($id(id), $set(F.playTime -> User.playTimeHandler.write(playTime))).void

  def getPlayTime(id: ID): Fu[Option[User.PlayTime]] =
    coll.primitiveOne[User.PlayTime]($id(id), F.playTime)

  val enabledSelect = $doc(F.enabled -> true)
  def engineSelect(v: Boolean) = $doc(F.engine -> v.fold[BSONValue]($boolean(true), $ne(true)))
  def trollSelect(v: Boolean) = $doc(F.troll -> v.fold[BSONValue]($boolean(true), $ne(true)))
  def boosterSelect(v: Boolean) = $doc(F.booster -> v.fold[BSONValue]($boolean(true), $ne(true)))
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

    coll.update($id(id), $inc(incs))
  }

  def incToints(id: ID, nb: Int) = coll.update($id(id), $inc("toints" -> nb))
  def removeAllToints = coll.update($empty, $unset("toints"), multi = true)

  def authenticateById(id: ID, password: String): Fu[Option[User]] =
    loginCandidateById(id) map { _ flatMap { _(password) } }

  def authenticateByEmail(email: EmailAddress, password: String): Fu[Option[User]] =
    loginCandidateByEmail(email) map { _ flatMap { _(password) } }

  @inline def passHasher = Env.current.passwordHasher

  private def salted(p: String, salt: String) = s"$p{$salt}"
  private def passEnc(pass: String, id: String) = passHasher.hash(salted(pass, id))

  private case class AuthData(
      _id: String,
      bpass: Option[Array[Byte]],
      password: Option[String],
      salt: Option[String],
      sha512: Option[Boolean]
  ) {
    def compare(p: String) = {
      val newP = (password, sha512) match {
        case (None, None) => p
        case _ => {
          val pSalt = salt.fold(p) { salted(p, _) }
          (~sha512).fold(pSalt.sha512, pSalt.sha1).hex
        }
      }

      val res = bpass match {
        // Deprecated fallback. Log & fail after DB migration.
        case None => password ?? { _ == newP }
        case Some(bHash) => passHasher.check(bHash, salted(newP, _id))
      }

      if (res && password.isDefined && Env.current.upgradeShaPasswords)
        passwd(id = _id, pass = p)

      res
    }
  }

  // This creates a bcrypt password using the an existing sha hash as
  // the "plain text", allowing us to migrate all users in bulk.
  def upgradePassword(a: AuthData) = (a.bpass, a.password) match {
    case (None, Some(p)) => coll.update($id(a._id), $set(
      F.sha512 -> ~a.sha512,
      F.bpass -> passEnc(p, a._id)
    ) ++ $unset(F.password)).void.some

    case _ => None
  }

  private implicit val AuthDataBSONHandler = Macros.handler[AuthData]

  def loginCandidateById(id: ID): Fu[Option[User.LoginCandidate]] =
    loginCandidate($id(id))

  def loginCandidateByEmail(email: EmailAddress): Fu[Option[User.LoginCandidate]] =
    loginCandidate($doc(F.email -> email))

  def loginCandidate(u: User): Fu[User.LoginCandidate] =
    loginCandidateById(u.id) map { _ | User.LoginCandidate(u, _ => false) }

  private def loginCandidate(select: Bdoc): Fu[Option[User.LoginCandidate]] =
    coll.uno[AuthData](select) zip coll.uno[User](select) map {
      case (Some(login), Some(user)) if user.enabled => User.LoginCandidate(user, login.compare).some
      case _ => none
    }

  def getPasswordHash(id: ID): Fu[Option[String]] = coll.byId[AuthData](id) map {
    _.map { auth => auth.bpass.fold(~auth.password) { _.sha512.hex } }
  }

  def create(
    username: String,
    password: String,
    email: EmailAddress,
    blind: Boolean,
    mobileApiVersion: Option[ApiVersion],
    mustConfirmEmail: Boolean
  ): Fu[Option[User]] =
    !nameExists(username) flatMap {
      _ ?? {
        val doc = newUser(username, password, email, blind, mobileApiVersion, mustConfirmEmail) ++
          ("len" -> BSONInteger(username.size))
        coll.insert(doc) >> named(normalize(username))
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
  def existingUsernameIds(usernames: Set[String]): Fu[List[String]] =
    coll.primitive[String]($inIds(usernames.map(normalize)), "_id")

  def usernamesLike(text: String, max: Int = 10): Fu[List[String]] = {
    val id = normalize(text)
    if (!User.idPattern.matcher(id).matches) fuccess(Nil)
    else coll.find(
      $doc("_id".$regex("^" + id + ".*$", "")) ++ enabledSelect,
      $doc(F.username -> true)
    )
      .sort($doc("len" -> 1))
      .cursor[Bdoc](ReadPreference.secondaryPreferred).gather[List](max)
      .map {
        _ flatMap { _.getAs[String](F.username) }
      }
  }

  def toggleEngine(id: ID): Funit =
    coll.fetchUpdate[User]($id(id)) { u =>
      $set("engine" -> !u.engine)
    }

  def setEngine(id: ID, v: Boolean): Funit = coll.updateField($id(id), "engine", v).void

  def setBooster(id: ID, v: Boolean): Funit = coll.updateField($id(id), "booster", v).void

  def setReportban(id: ID, v: Boolean): Funit = coll.updateField($id(id), "reportban", v).void

  def setIpBan(id: ID, v: Boolean) = coll.updateField($id(id), "ipBan", v).void

  def toggleKid(user: User) = coll.updateField($id(user.id), "kid", !user.kid)

  def isKid(id: ID) = coll.exists($id(id) ++ $doc("kid" -> true))

  def updateTroll(user: User) = coll.updateField($id(user.id), "troll", user.troll)

  def isEngine(id: ID): Fu[Boolean] = coll.exists($id(id) ++ engineSelect(true))

  def isTroll(id: ID): Fu[Boolean] = coll.exists($id(id) ++ trollSelect(true))

  def setRoles(id: ID, roles: List[String]) = coll.updateField($id(id), "roles", roles)

  def enable(id: ID) = coll.updateField($id(id), F.enabled, true)

  def disable(user: User) = coll.update(
    $id(user.id),
    $set(F.enabled -> false) ++ {
      if (user.lameOrTroll) $empty
      else $doc("$rename" -> $doc(F.email -> F.prevEmail))
    }
  )

  def passwd(id: ID, pass: String): Funit =
    coll.update($id(id), $set(F.bpass -> passEnc(pass, id)) ++ $unset(F.salt, F.password, F.sha512)).void

  def email(id: ID, email: EmailAddress): Funit =
    coll.update($id(id), $set(F.email -> email) ++ $unset(F.prevEmail)).void

  def email(id: ID): Fu[Option[EmailAddress]] = coll.primitiveOne[EmailAddress]($id(id), F.email)

  def emails(id: ID): Fu[User.Emails] =
    coll.find($id(id), $doc(F.email -> true, F.prevEmail -> true)).uno[Bdoc].map { doc =>
      User.Emails(
        current = doc.flatMap(_.getAs[EmailAddress](F.email)),
        previous = doc.flatMap(_.getAs[EmailAddress](F.prevEmail))
      )
    }

  def hasEmail(id: ID): Fu[Boolean] = email(id).map(_.isDefined)

  def getTitle(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.title)

  def setPlan(user: User, plan: Plan): Funit = {
    implicit val pbw: BSONValueWriter[Plan] = Plan.planBSONHandler
    coll.updateField($id(user.id), "plan", plan).void
  }

  private def docPerf(doc: Bdoc, perfType: PerfType): Option[Perf] =
    doc.getAs[Bdoc](F.perfs).flatMap(_.getAs[Perf](perfType.key))

  def perfOf(id: ID, perfType: PerfType): Fu[Option[Perf]] = coll.find(
    $id(id),
    $doc(s"${F.perfs}.${perfType.key}" -> true)
  ).uno[Bdoc].map {
      _.flatMap { docPerf(_, perfType) }
    }

  def perfOf(ids: Iterable[ID], perfType: PerfType): Fu[Map[ID, Perf]] = coll.find(
    $inIds(ids),
    $doc(s"${F.perfs}.${perfType.key}" -> true)
  ).cursor[Bdoc]()
    .collect[List](Int.MaxValue, err = Cursor.FailOnError[List[Bdoc]]()).map { docs =>
      docs.map { doc =>
        ~doc.getAs[ID]("_id") -> docPerf(doc, perfType).getOrElse(Perf.default)
      }(scala.collection.breakOut)
    }

  def setSeenAt(id: ID) {
    coll.updateFieldUnchecked($id(id), "seenAt", DateTime.now)
  }

  def recentlySeenNotKidIdsCursor(since: DateTime)(implicit cp: CursorProducer[Bdoc]) =
    coll.find($doc(
      F.enabled -> true,
      "seenAt" $gt since,
      "count.game" $gt 9,
      "kid" $ne true
    ), $id(true)).cursor[Bdoc](readPreference = ReadPreference.secondary)

  def setLang(id: ID, lang: String) = coll.updateField($id(id), "lang", lang).void

  def langOf(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), "lang")

  def idsSumToints(ids: Iterable[String]): Fu[Int] =
    ids.nonEmpty ?? coll.aggregateWithReadPreference(
      Match($inIds(ids)),
      List(Group(BSONNull)(F.toints -> SumField(F.toints))),
      ReadPreference.secondaryPreferred
    ).map(
        _.firstBatch.headOption flatMap { _.getAs[Int](F.toints) }
      ).map(~_)

  def filterByEngine(userIds: Iterable[User.ID]): Fu[Set[User.ID]] =
    coll.distinctWithReadPreference[String, Set](
      F.id,
      Some($inIds(userIds) ++ engineSelect(true)),
      ReadPreference.secondaryPreferred
    )

  def filterByEnabledPatrons(userIds: List[User.ID]): Fu[Set[User.ID]] =
    coll.distinct[String, Set](F.id, Some($inIds(userIds) ++ enabledSelect ++ patronSelect))

  def userIdsWithRoles(roles: List[String]): Fu[Set[User.ID]] =
    coll.distinct[String, Set]("_id", $doc("roles" $in roles).some)

  def countEngines(userIds: List[User.ID]): Fu[Int] =
    coll.countSel($inIds(userIds) ++ engineSelect(true), ReadPreference.secondaryPreferred)

  def containsEngine(userIds: List[User.ID]): Fu[Boolean] =
    coll.exists($inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: String): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.mustConfirmEmail $exists true))

  def setEmailConfirmed(id: String): Funit = coll.update($id(id), $unset(F.mustConfirmEmail)).void

  private def newUser(
    username: String,
    password: String,
    email: EmailAddress,
    blind: Boolean,
    mobileApiVersion: Option[ApiVersion],
    mustConfirmEmail: Boolean
  ) = {

    implicit def countHandler = Count.countBSONHandler
    implicit def perfsHandler = Perfs.perfsBSONHandler
    import lila.db.BSON.BSONJodaDateTimeHandler

    val id = normalize(username)

    $doc(
      F.id -> id,
      F.username -> username,
      F.email -> email,
      F.mustConfirmEmail -> mustConfirmEmail.option(DateTime.now),
      F.bpass -> passEnc(password, id),
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
