package lila.user

import cats.syntax.all.*
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.*
import reactivemongo.api.bson.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.{ ApiVersion, EmailAddress, LightUser, NormalizedEmailAddress }
import lila.db.dsl.{ *, given }
import lila.rating.Glicko
import lila.rating.{ Perf, PerfType }

final class UserRepo(val coll: Coll)(using Executor):

  import User.{ BSONFields as F, given }
  import UserMark.given

  def withColl[A](f: Coll => A): A = f(coll)

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledNoBotSelect ++ notLame).sort($sort desc "count.game").cursor[User]().list(nb)

  def byId[U](u: U)(using idOf: UserIdOf[U]): Fu[Option[User]] =
    User.noGhost(idOf(u)) so coll.byId[User](idOf(u)).recover {
      case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => none // probably GDPRed user
    }

  def byIds[U](us: Iterable[U])(using idOf: UserIdOf[U]): Fu[List[User]] = {
    val ids = us.map(idOf.apply).filter(User.noGhost)
    ids.nonEmpty so coll.byIds[User, UserId](ids)
  }

  def byIdsSecondary(ids: Iterable[UserId]): Fu[List[User]] =
    coll.byIds[User, UserId](ids, ReadPreference.secondaryPreferred)

  def enabledById[U](u: U)(using idOf: UserIdOf[U]): Fu[Option[User]] =
    User.noGhost(idOf(u)) so coll.one[User](enabledSelect ++ $id(u))

  def enabledByIds[U](us: Iterable[U])(using idOf: UserIdOf[U]): Fu[List[User]] = {
    val ids = us.map(idOf.apply).filter(User.noGhost)
    coll.list[User](enabledSelect ++ $inIds(ids), temporarilyPrimary)
  }

  def byIdOrGhost(id: UserId): Fu[Option[Either[LightUser.Ghost, User]]] =
    if (User isGhost id) fuccess(Left(LightUser.ghost).some)
    else
      coll.byId[User](id).map2(Right.apply) recover { case _: exceptions.BSONValueNotFoundException =>
        Left(LightUser.ghost).some
      }

  def byEmail(email: NormalizedEmailAddress): Fu[Option[User]] = coll.one[User]($doc(F.email -> email))
  def byPrevEmail(
      email: NormalizedEmailAddress,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): Fu[List[User]] =
    coll.list[User]($doc(F.prevEmail -> email), readPreference)

  def idByEmail(email: NormalizedEmailAddress): Fu[Option[UserId]] =
    coll.primitiveOne[UserId]($doc(F.email -> email), "_id")

  def countRecentByPrevEmail(email: NormalizedEmailAddress, since: Instant): Fu[Int] =
    coll.countSel($doc(F.prevEmail -> email, F.createdAt $gt since))

  def pair(x: Option[UserId], y: Option[UserId]): Fu[(Option[User], Option[User])] =
    coll.byIds[User, UserId](List(x, y).flatten) map { users =>
      x.so(xx => users.find(_.id == xx)) ->
        y.so(yy => users.find(_.id == yy))
    }

  def pair(x: UserId, y: UserId): Fu[Option[(User, User)]] =
    coll.byIds[User, UserId](List(x, y)) map { users =>
      for {
        xx <- users.find(_.id == x)
        yy <- users.find(_.id == y)
      } yield xx -> yy
    }

  def lichessAnd(id: UserId) = pair(User.lichessId, id) map2 { case (lichess, user) =>
    Holder(lichess) -> user
  }

  def byOrderedIds(ids: Seq[UserId], readPreference: ReadPreference): Fu[List[User]] =
    coll.byOrderedIds[User, UserId](ids, readPreference = readPreference)(_.id)

  def usersFromSecondary(userIds: Seq[UserId]): Fu[List[User]] =
    byOrderedIds(userIds, ReadPreference.secondaryPreferred)

  def optionsByIds(userIds: Seq[UserId]): Fu[List[Option[User]]] =
    coll.optionsByOrderedIds[User, UserId](userIds, readPreference = ReadPreference.secondaryPreferred)(_.id)

  def isEnabled(id: UserId): Fu[Boolean] =
    User.noGhost(id) so coll.exists(enabledSelect ++ $id(id))

  def disabledById(id: UserId): Fu[Option[User]] =
    User.noGhost(id) so coll.one[User](disabledSelect ++ $id(id))

  // expensive, send to secondary
  def byIdsSortRatingNoBot(ids: Iterable[UserId], nb: Int): Fu[List[User]] =
    coll
      .find(
        $doc(
          F.enabled -> true,
          F.marks $nin List(UserMark.Engine.key, UserMark.Boost.key),
          "perfs.standard.gl.d" $lt Glicko.provisionalDeviation
        ) ++ $inIds(ids) ++ botSelect(false)
      )
      .sort($sort desc "perfs.standard.gl.r")
      .cursor[User](ReadPreference.secondaryPreferred)
      .list(nb)

  def botsByIdsCursor(ids: Iterable[UserId]): AkkaStreamCursor[User] =
    coll.find($inIds(ids) ++ botSelect(true)).cursor[User](temporarilyPrimary)

  def botsByIds(ids: Iterable[UserId]): Fu[List[User]] =
    coll.find($inIds(ids) ++ botSelect(true)).cursor[User](temporarilyPrimary).listAll()

  def usernameById(id: UserId): Fu[Option[UserName]] =
    coll.primitiveOne[UserName]($id(id), F.username)

  def usernamesByIds(ids: List[UserId]) =
    coll.distinctEasy[UserName, List](F.username, $inIds(ids), ReadPreference.secondaryPreferred)

  def createdAtById(id: UserId) =
    coll.primitiveOne[Instant]($id(id), F.createdAt)

  def orderByGameCount(u1: UserId, u2: UserId): Fu[Option[(UserId, UserId)]] =
    coll
      .find(
        $inIds(List(u1, u2)),
        $doc(s"${F.count}.game" -> true).some
      )
      .cursor[Bdoc]()
      .listAll() map { docs =>
      docs
        .sortBy {
          _.child(F.count).flatMap(_.int("game"))
        }
        .flatMap(_.getAsOpt[UserId]("_id")) match
        case List(u1, u2) => (u1, u2).some
        case _            => none
    }

  def firstGetsWhite(u1: UserId, u2: UserId): Fu[Boolean] =
    coll
      .find(
        $inIds(List(u1, u2)),
        $id(true).some
      )
      .sort($doc(F.colorIt -> 1))
      .one[Bdoc]
      .map {
        _.fold(ThreadLocalRandom.nextBoolean()) { doc =>
          doc.string("_id") contains u1
        }
      }
      .addEffect { v =>
        incColor(u1, if (v) 1 else -1)
        incColor(u2, if (v) -1 else 1)
      }

  def firstGetsWhite(u1O: Option[UserId], u2O: Option[UserId]): Fu[Boolean] =
    (u1O, u2O).mapN(firstGetsWhite) | fuccess(ThreadLocalRandom.nextBoolean())

  def incColor(userId: UserId, value: Int): Unit =
    coll
      .update(ordered = false, WriteConcern.Unacknowledged)
      .one(
        // limit to -3 <= colorIt <= 5 but set when undefined
        $id(userId) ++ $doc(F.colorIt -> $not(if (value < 0) $lte(-3) else $gte(5))),
        $inc(F.colorIt -> value)
      )
      .unit

  def lichess = byId(User.lichessId)
  def irwin   = byId(User.irwinId)
  def kaladin = byId(User.kaladinId)

  def setPerfs(user: User, perfs: Perfs, prev: Perfs)(using wr: BSONHandler[Perf]) =
    val diff = for {
      pt <- PerfType.all
      if perfs(pt).nb != prev(pt).nb
      bson <- wr.writeOpt(perfs(pt))
    } yield BSONElement(s"${F.perfs}.${pt.key}", bson)
    diff.nonEmpty so coll.update
      .one(
        $id(user.id),
        $doc("$set" -> $doc(diff*))
      )
      .void

  def setManagedUserInitialPerfs(id: UserId) =
    coll.updateField($id(id), F.perfs, Perfs.defaultManaged).void

  def setPerf(userId: UserId, pt: PerfType, perf: Perf) =
    coll.updateField($id(userId), s"${F.perfs}.${pt.key}", perf).void

  def addStormRun  = addStormLikeRun("storm")
  def addRacerRun  = addStormLikeRun("racer")
  def addStreakRun = addStormLikeRun("streak")

  private def addStormLikeRun(field: String)(userId: UserId, score: Int): Funit =
    coll.update
      .one(
        $id(userId),
        $inc(s"perfs.$field.runs" -> 1) ++
          $doc("$max" -> $doc(s"perfs.$field.score" -> score))
      )
      .void

  def setProfile(id: UserId, profile: Profile): Funit =
    coll.updateField($id(id), F.profile, profile).void

  def setUsernameCased(id: UserId, name: UserName): Funit =
    if (id is name)
      coll.update.one(
        $id(id) ++ (F.changedCase $exists false),
        $set(F.username -> name, F.changedCase -> true)
      ) flatMap { result =>
        if (result.n == 0) fufail(s"You have already changed your username")
        else funit
      }
    else fufail(s"Proposed username $name does not match old username $id")

  def addTitle(id: UserId, title: UserTitle): Funit =
    coll.updateField($id(id), F.title, title).void

  def removeTitle(id: UserId): Funit =
    coll.unsetField($id(id), F.title).void

  def getPlayTime(id: UserId): Fu[Option[User.PlayTime]] =
    coll.primitiveOne[User.PlayTime]($id(id), F.playTime)

  val enabledSelect  = $doc(F.enabled -> true)
  val disabledSelect = $doc(F.enabled -> false)
  def markSelect(mark: UserMark)(v: Boolean): Bdoc =
    if (v) $doc(F.marks -> mark.key)
    else F.marks $ne mark.key
  def engineSelect       = markSelect(UserMark.Engine)
  def trollSelect        = markSelect(UserMark.Troll)
  val lame               = $doc(F.marks $in List(UserMark.Engine.key, UserMark.Boost.key))
  val lameOrTroll        = $doc(F.marks $in List(UserMark.Engine.key, UserMark.Boost.key, UserMark.Troll.key))
  val notLame            = $doc(F.marks $nin List(UserMark.Engine.key, UserMark.Boost.key))
  val enabledNoBotSelect = enabledSelect ++ $doc(F.title $ne Title.BOT)
  def stablePerfSelect(perf: String) =
    $doc(s"perfs.$perf.gl.d" -> $lt(lila.rating.Glicko.provisionalDeviation))
  val patronSelect = $doc(s"${F.plan}.active" -> true)

  def sortPerfDesc(perf: String) = $sort desc s"perfs.$perf.gl.r"
  val sortCreatedAtDesc          = $sort desc F.createdAt

  def glicko(userId: UserId, perfType: PerfType): Fu[Glicko] =
    coll
      .find($id(userId), $doc(s"${F.perfs}.${perfType.key}.gl" -> true).some)
      .one[Bdoc]
      .dmap {
        _.flatMap(_ child F.perfs)
          .flatMap(_ child perfType.key.value)
          .flatMap(_.getAsOpt[Glicko]("gl")) | Glicko.default
      }

  def incNbGames(
      id: UserId,
      rated: Boolean,
      ai: Boolean,
      result: Int,
      totalTime: Option[Int],
      tvTime: Option[Int]
  ) =
    val incs: List[BSONElement] = List(
      "count.game".some,
      rated option "count.rated",
      ai option "count.ai",
      (result match {
        case -1 => "count.loss".some
        case 1  => "count.win".some
        case 0  => "count.draw".some
        case _  => none
      }),
      (result match {
        case -1 => "count.lossH".some
        case 1  => "count.winH".some
        case 0  => "count.drawH".some
        case _  => none
      }) ifFalse ai
    ).flatten.map(k => BSONElement(k, BSONInteger(1))) ::: List(
      totalTime map (v => BSONElement(s"${F.playTime}.total", BSONInteger(v + 2))),
      tvTime map (v => BSONElement(s"${F.playTime}.tv", BSONInteger(v + 2)))
    ).flatten

    coll.update.one($id(id), $inc($doc(incs*)))

  def incToints(id: UserId, nb: Int) = coll.update.one($id(id), $inc("toints" -> nb))
  def removeAllToints                = coll.update.one($empty, $unset("toints"), multi = true)

  def create(
      name: UserName,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mobileApiVersion: Option[ApiVersion],
      mustConfirmEmail: Boolean,
      lang: Option[String] = None
  ): Fu[Option[User]] =
    !exists(name) flatMapz {
      val doc = newUser(name, passwordHash, email, blind, mobileApiVersion, mustConfirmEmail, lang) ++
        ("len" -> BSONInteger(name.value.length))
      coll.insert.one(doc) >> byId(name.id)
    }

  def exists[U](id: U)(using uid: UserIdOf[U]): Fu[Boolean] = coll exists $id(uid(id))

  def filterExists(ids: Set[UserId]): Fu[List[UserId]] =
    coll.primitive[UserId]($inIds(ids), F.id)

  def userIdsLikeWithRole(text: UserStr, role: String, max: Int = 10): Fu[List[UserId]] =
    userIdsLikeFilter(text, $doc(F.roles -> role), max)

  private[user] def userIdsLikeFilter(text: UserStr, filter: Bdoc, max: Int): Fu[List[UserId]] =
    User.validateId(text) so { id =>
      coll
        .find(
          $doc(F.id $startsWith id.value) ++ enabledSelect ++ filter,
          $doc(F.id -> true).some
        )
        .sort($doc("len" -> 1))
        .cursor[Bdoc](ReadPreference.secondaryPreferred)
        .list(max)
        .map {
          _ flatMap { _.getAsOpt[UserId](F.id) }
        }
    }

  private def setMark(mark: UserMark)(id: UserId, v: Boolean): Funit =
    coll.update.one($id(id), $addOrPull(F.marks, mark, v)).void

  def setEngine    = setMark(UserMark.Engine)
  def setBoost     = setMark(UserMark.Boost)
  def setTroll     = setMark(UserMark.Troll)
  def setReportban = setMark(UserMark.Reportban)
  def setRankban   = setMark(UserMark.Rankban)
  def setPrizeban  = setMark(UserMark.PrizeBan)
  def setAlt       = setMark(UserMark.Alt)

  def setKid(user: User, v: Boolean) = coll.updateField($id(user.id), F.kid, v).void

  def isKid[U: UserIdOf](id: U) = coll.exists($id(id) ++ $doc(F.kid -> true))

  def updateTroll(user: User) = setTroll(user.id, user.marks.troll)

  def filterLame(ids: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some($inIds(ids) ++ lame))

  def filterNotKid(ids: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some($inIds(ids) ++ $doc(F.kid $ne true)))

  def isTroll(id: UserId): Fu[Boolean] = coll.exists($id(id) ++ trollSelect(true))

  def isCreatedSince(id: UserId, since: Instant): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.createdAt $lt since))

  def setRoles(id: UserId, roles: List[String]): Funit =
    coll.updateField($id(id), F.roles, roles).void

  def withoutTwoFactor(id: UserId) = coll.one[User]($id(id) ++ $doc(F.totpSecret $exists false))

  def disableTwoFactor(id: UserId) = coll.update.one($id(id), $unset(F.totpSecret))

  def setupTwoFactor(id: UserId, totp: TotpSecret): Funit =
    coll.update
      .one(
        $id(id) ++ (F.totpSecret $exists false), // never overwrite existing secret
        $set(F.totpSecret -> totp.secret)
      )
      .void

  def reopen(id: UserId) =
    coll.updateField($id(id), F.enabled, true) >>
      coll.update
        .one(
          $id(id) ++ $doc(F.email $exists false),
          $doc("$rename" -> $doc(F.prevEmail -> F.email)) ++
            $doc("$unset" -> $doc(F.eraseAt -> true))
        )
        .void
        .recover(lila.db.recoverDuplicateKey(_ => ()))

  def disable(user: User, keepEmail: Boolean): Funit =
    coll.update
      .one(
        $id(user.id),
        $set(F.enabled -> false) ++ $unset(F.roles) ++ {
          if (keepEmail) $unset(F.mustConfirmEmail)
          else $doc("$rename" -> $doc(F.email -> F.prevEmail))
        }
      )
      .void

  def isMonitoredMod[U: UserIdOf](u: U) = coll.exists($id(u) ++ $doc(F.roles -> "ROLE_MONITORED_MOD"))

  import Authenticator.*
  def getPasswordHash(id: UserId): Fu[Option[String]] =
    coll.byId[AuthData](id, authProjection) map {
      _.map { _.hashToken }
    }

  def setEmail(id: UserId, email: EmailAddress): Funit = {
    val normalized = email.normalize
    coll.update
      .one(
        $id(id),
        if email.value == normalized.value then
          $set(F.email -> normalized) ++ $unset(F.prevEmail, F.verbatimEmail)
        else $set(F.email -> normalized, F.verbatimEmail -> email) ++ $unset(F.prevEmail)
      )
      .void
  } >>- lila.common.Bus.publish(lila.hub.actorApi.user.ChangeEmail(id, email), "email")

  private def anyEmail(doc: Bdoc): Option[EmailAddress] =
    doc.getAsOpt[EmailAddress](F.verbatimEmail) orElse doc.getAsOpt[EmailAddress](F.email)

  private def anyEmailOrPrevious(doc: Bdoc): Option[EmailAddress] =
    anyEmail(doc) orElse doc.getAsOpt[EmailAddress](F.prevEmail)

  def email(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true).some)
      .one[Bdoc]
      .mapz(anyEmail)

  def emailOrPrevious(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .mapz(anyEmailOrPrevious)

  def enabledWithEmail(email: NormalizedEmailAddress): Fu[Option[(User, EmailAddress)]] =
    coll
      .find($doc(F.email -> email, F.enabled -> true))
      .one[Bdoc]
      .map { maybeDoc =>
        for {
          doc         <- maybeDoc
          storedEmail <- anyEmail(doc)
          user        <- summon[BSONHandler[User]] readOpt doc
        } yield (user, storedEmail)
      }

  def prevEmail(id: UserId): Fu[Option[EmailAddress]] =
    coll.primitiveOne[EmailAddress]($id(id), F.prevEmail)

  def currentOrPrevEmail(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find($id(id), $doc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .mapz { doc =>
        anyEmail(doc) orElse doc.getAsOpt[EmailAddress](F.prevEmail)
      }

  def withEmails[U: UserIdOf](u: U)(using r: BSONHandler[User]): Fu[Option[User.WithEmails]] =
    coll.find($id(u.id)).one[Bdoc].mapz { doc =>
      r readOpt doc map {
        User
          .WithEmails(
            _,
            User.Emails(
              current = anyEmail(doc),
              previous = doc.getAsOpt[NormalizedEmailAddress](F.prevEmail)
            )
          )
      }
    }

  def withEmails[U](
      users: List[U]
  )(using idOf: UserIdOf[U], r: BSONHandler[User]): Fu[List[User.WithEmails]] =
    coll
      .list[Bdoc]($inIds(users.map(idOf.apply)), temporarilyPrimary)
      .map { docs =>
        for {
          doc  <- docs
          user <- r readOpt doc
        } yield User.WithEmails(
          user,
          User.Emails(
            current = anyEmail(doc),
            previous = doc.getAsOpt[NormalizedEmailAddress](F.prevEmail)
          )
        )
      }

  def emailMap(ids: List[UserId]): Fu[Map[UserId, EmailAddress]] =
    coll
      .find(
        $inIds(ids),
        $doc(F.verbatimEmail -> true, F.email -> true, F.prevEmail -> true).some
      )
      .cursor[Bdoc](temporarilyPrimary)
      .listAll()
      .map { docs =>
        for
          doc   <- docs
          email <- anyEmailOrPrevious(doc)
          id    <- doc.getAsOpt[UserId](F.id)
        yield id -> email
      }
      .dmap(_.toMap)

  def hasEmail(id: UserId): Fu[Boolean] = email(id).dmap(_.isDefined)

  def isManaged(id: UserId): Fu[Boolean] = email(id).dmap(_.exists(_.isNoReply))

  def setBot(user: User): Funit =
    if (user.count.game > 0)
      fufail(lila.base.LilaInvalid("You already have games played. Make a new account."))
    else
      coll.update
        .one(
          $id(user.id),
          $set(F.title -> Title.BOT, F.perfs -> Perfs.defaultBot)
        )
        .void

  private def botSelect(v: Boolean) =
    if (v) $doc(F.title -> Title.BOT)
    else $doc(F.title   -> $ne(Title.BOT))

  private[user] def botIds =
    coll.distinctEasy[UserId, Set](
      "_id",
      botSelect(true) ++ enabledSelect,
      ReadPreference.secondaryPreferred
    )

  def getTitle(id: UserId): Fu[Option[UserTitle]] = coll.primitiveOne[UserTitle]($id(id), F.title)

  def hasTitle(id: UserId): Fu[Boolean] = getTitle(id).dmap(_.exists(Title.BOT !=))

  def setPlan(user: User, plan: Plan): Funit =
    import Plan.given
    coll.updateField($id(user.id), User.BSONFields.plan, plan).void
  def unsetPlan(user: User): Funit = coll.unsetField($id(user.id), User.BSONFields.plan).void

  private def docPerf(doc: Bdoc, perfType: PerfType): Option[Perf] =
    doc.child(F.perfs).flatMap(_.getAsOpt[Perf](perfType.key.value))

  def perfOf(id: UserId, perfType: PerfType): Fu[Option[Perf]] =
    coll
      .find(
        $id(id),
        $doc(s"${F.perfs}.${perfType.key}" -> true).some
      )
      .one[Bdoc]
      .dmap {
        _.flatMap { docPerf(_, perfType) }
      }

  def perfOf(ids: Iterable[UserId], perfType: PerfType): Fu[Map[UserId, Perf]] =
    coll
      .find(
        $inIds(ids),
        $doc(s"${F.perfs}.${perfType.key}" -> true).some
      )
      .cursor[Bdoc]()
      .listAll()
      .map { docs =>
        for
          doc <- docs
          id  <- doc.getAsOpt[UserId](F.id)
          perf = docPerf(doc, perfType) | Perf.default
        yield id -> perf
      }
      .dmap(_.toMap)

  def setSeenAt(id: UserId): Unit =
    coll.updateFieldUnchecked($id(id), F.seenAt, nowInstant)

  def setLang(user: User, lang: play.api.i18n.Lang) =
    coll.updateField($id(user.id), "lang", lang.code).void

  def langOf(id: UserId): Fu[Option[String]] = coll.primitiveOne[String]($id(id), "lang")

  def filterByEnabledPatrons(userIds: List[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](
      F.id,
      $inIds(userIds) ++ enabledSelect ++ patronSelect,
      ReadPreference.secondaryPreferred
    )

  def filterEnabled(userIds: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, $inIds(userIds) ++ enabledSelect, ReadPreference.secondaryPreferred)

  def filterDisabled(userIds: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, $inIds(userIds) ++ disabledSelect, ReadPreference.secondaryPreferred)

  def userIdsWithRoles(roles: List[String]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("_id", $doc("roles" $in roles))

  def countEngines(userIds: List[UserId]): Fu[Int] =
    coll.secondaryPreferred.countSel($inIds(userIds) ++ engineSelect(true))

  def countLameOrTroll(userIds: List[UserId]): Fu[Int] =
    coll.secondaryPreferred.countSel($inIds(userIds) ++ lameOrTroll)

  def containsEngine(userIds: List[UserId]): Fu[Boolean] =
    coll.exists($inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: UserId): Fu[Boolean] =
    coll.exists($id(id) ++ $doc(F.mustConfirmEmail $exists true))

  def setEmailConfirmed(id: UserId): Fu[Option[EmailAddress]] =
    coll.update.one($id(id) ++ $doc(F.mustConfirmEmail $exists true), $unset(F.mustConfirmEmail)) flatMap {
      res =>
        (res.nModified == 1) so email(id)
    }

  private val speakerProjection = $doc(
    F.username -> true,
    F.title    -> true,
    F.plan     -> true,
    F.enabled  -> true,
    F.marks    -> true
  )

  def speaker(id: UserId): Fu[Option[User.Speaker]] =
    coll.one[User.Speaker]($id(id), speakerProjection)

  def contacts(orig: UserId, dest: UserId): Fu[Option[User.Contacts]] =
    coll.byOrderedIds[User.Contact, UserId](
      List(orig, dest),
      $doc(F.kid -> true, F.marks -> true, F.roles -> true, F.createdAt -> true).some
    )(_._id) map {
      case List(o, d) => User.Contacts(o, d).some
      case _          => none
    }

  def isErased(user: User): Fu[User.Erased] = User.Erased from {
    user.enabled.no so {
      coll.exists($id(user.id) ++ $doc(F.erasedAt $exists true))
    }
  }

  def filterClosedOrInactiveIds(since: Instant)(ids: Iterable[UserId]): Fu[List[UserId]] =
    coll.distinctEasy[UserId, List](
      F.id,
      $inIds(ids) ++ $or(disabledSelect, F.seenAt $lt since),
      ReadPreference.secondaryPreferred
    )

  def setEraseAt(user: User) =
    coll.updateField($id(user.id), F.eraseAt, nowInstant plusDays 1).void

  private def newUser(
      name: UserName,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mobileApiVersion: Option[ApiVersion],
      mustConfirmEmail: Boolean,
      lang: Option[String]
  ) =
    import Count.given
    import Authenticator.given

    val normalizedEmail = email.normalize
    val now             = nowInstant
    $doc(
      F.id                    -> name.id,
      F.username              -> name,
      F.email                 -> normalizedEmail,
      F.mustConfirmEmail      -> mustConfirmEmail.option(now),
      F.bpass                 -> passwordHash,
      F.perfs                 -> $empty,
      F.count                 -> Count.default,
      F.enabled               -> true,
      F.createdAt             -> now,
      F.createdWithApiVersion -> mobileApiVersion,
      F.seenAt                -> now,
      F.playTime              -> User.PlayTime(0, 0),
      F.lang                  -> lang
    ) ++ {
      (email.value != normalizedEmail.value) so $doc(F.verbatimEmail -> email)
    } ++ {
      blind so $doc(F.blind -> true)
    }
