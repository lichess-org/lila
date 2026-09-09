package lila.user

import chess.PlayerTitle
import com.roundeights.hasher.Implicits.*
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom
import scalalib.model.{ Days, LangTag }

import lila.core.LightUser
import lila.core.email.NormalizedEmailAddress
import lila.core.security.HashedPassword
import lila.core.user.{ Plan, PlayTime, Profile, TotpSecret, UserMark, RoleDbKey, KidMode, RealName }
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }

final class UserRepo(c: Coll)(using Executor) extends lila.core.user.UserRepo(c):

  import lila.user.BSONFields as F
  export lila.user.BSONHandlers.given

  private def recoverDeleted[A](user: Fu[Option[A]]): Fu[Option[A]] =
    user.recover:
      case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => none

  def withColl[A](f: Coll => A): A = f(coll)

  def topNbGame(nb: Int): Fu[List[User]] =
    coll.find(enabledNoBotSelect ++ notLame).sort(sort.desc("count.game")).cursor[User]().list(nb)

  def byId[U: UserIdOf](u: U): Fu[Option[User]] =
    u.id.noGhost.so:
      recoverDeleted:
        coll.byId[User](u.id)

  def byIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]] =
    val ids = us.map(_.id).filter(_.noGhost)
    ids.nonEmpty.so(coll.byIds[User, UserId](ids))

  def enabledById[U: UserIdOf](u: U): Fu[Option[User]] =
    u.id.noGhost.so:
      recoverDeleted:
        coll.one[User](enabledSelect ++ bid(u.id))

  def notForeverClosedById[U: UserIdOf](u: U): Fu[Option[User]] =
    u.id.noGhost.so:
      recoverDeleted:
        coll.one[User](notForeverClosedSelect ++ bid(u.id))

  def enabledByIds[U: UserIdOf](us: Iterable[U]): Fu[List[User]] =
    val ids = us.map(_.id).filter(_.noGhost)
    coll.list[User](enabledSelect ++ inIds(ids), _.sec)

  def byIdOrGhost(id: UserId): Fu[Option[Either[LightUser.Ghost, User]]] =
    if id.isGhost
    then fuccess(Left(LightUser.ghost).some)
    else
      coll.byId[User](id).map2(Right.apply).recover { case _: exceptions.BSONValueNotFoundException =>
        Left(LightUser.ghost).some
      }

  def me[U: UserIdOf](u: U): Fu[Option[Me]] = enabledById(u.id).dmap(Me.from(_))

  def meWithConfirmedEmail(id: UserId): Fu[Option[Either[Unit, Me]]] =
    recoverDeleted:
      for opt <- coll.one[Bdoc](enabledSelect ++ bid(id))
      yield
        for
          doc <- opt
          user <- doc.asOpt[User]
        yield
          if doc.contains(F.mustConfirmEmail) then Left(())
          else Right(Me(user))

  def byEmail(email: NormalizedEmailAddress): Fu[Option[User]] = coll.one[User](bdoc(F.email -> email))
  def byPrevEmail(
      email: NormalizedEmailAddress,
      readPref: ReadPref = _.sec
  ): Fu[List[User]] =
    coll.list[User](bdoc(F.prevEmail -> email), readPref)

  def idByAnyEmail(emails: List[NormalizedEmailAddress]): Fu[Option[UserId]] =
    coll.primitiveOne[UserId](F.email.in(emails), "_id")

  def pair(x: Option[UserId], y: Option[UserId]): Fu[(Option[User], Option[User])] =
    coll.byIds[User, UserId](List(x, y).flatten).map { users =>
      x.so(xx => users.find(_.id == xx)) ->
        y.so(yy => users.find(_.id == yy))
    }

  def pair(x: UserId, y: UserId): Fu[Option[(User, User)]] =
    coll.byIds[User, UserId](List(x, y)).map { users =>
      for
        xx <- users.find(_.id == x)
        yy <- users.find(_.id == y)
      yield xx -> yy
    }

  def lichessAnd(id: UserId): Future[Option[(User, User)]] = pair(UserId.lichess, id)

  def byOrderedIds(ids: Seq[UserId], readPref: ReadPref): Fu[List[User]] =
    coll.byOrderedIds[User, UserId](ids, readPref = readPref)(_.id)

  def isEnabled(id: UserId): Fu[Boolean] =
    id.noGhost.so(coll.exists(enabledSelect ++ bid(id)))

  def disabledById(id: UserId): Fu[Option[User]] =
    id.noGhost.so(coll.one[User](disabledSelect ++ bid(id)))

  def usernamesByIds(ids: List[UserId]): Fu[List[UserName]] =
    coll.distinctEasy[UserName, List](F.username, inIds(ids), _.sec)

  def createdAtById(id: UserId): Fu[Option[Instant]] =
    coll.primitiveOne[Instant](bid(id), F.createdAt)

  def firstGetsWhite(u1: UserId, u2: UserId): Fu[Boolean] =
    coll
      .find(inIds(List(u1, u2)), bid(true).some)
      .sort(bdoc(F.colorIt -> 1))
      .one[Bdoc]
      .map:
        _.fold(ThreadLocalRandom.nextBoolean()): doc =>
          doc.string("_id") contains u1
      .addEffect: v =>
        val u1Color = Color.fromWhite(v)
        incColor(u1, u1Color)
        incColor(u2, !u1Color)

  def firstGetsWhite(u1O: Option[UserId], u2O: Option[UserId]): Fu[Boolean] =
    (u1O, u2O).mapN(firstGetsWhite) | fuccess(ThreadLocalRandom.nextBoolean())

  def incColor(userId: UserId, color: Color): Unit =
    coll
      .update(ordered = false, WriteConcern.Unacknowledged)
      .one(
        // limit to -3 <= colorIt <= 5 but set when undefined
        bid(userId) ++ bdoc(F.colorIt -> not(color.fold(gte(5), lte(-3)))),
        inc(F.colorIt -> color.fold(1, -1))
      )

  def mustPlayAsColor(userId: UserId): Fu[Option[Color]] =
    coll
      .primitiveOne[Int](bid(userId), F.colorIt)
      .map:
        _.flatMap: i =>
          if i > 2 then Color.black.some
          else if i < -2 then Color.white.some
          else none

  def setProfile(id: UserId, profile: Profile): Funit =
    coll.updateField(bid(id), F.profile, profile).void

  def setRealName(id: UserId, name: RealName): Funit =
    coll.updateField(bid(id), s"${F.profile}.realName", name).void

  def realName(id: UserId): Fu[Option[RealName]] =
    coll
      .find(bid(id) ++ enabledSelect, bdoc(s"${F.profile}.realName" -> true).some)
      .one[Bdoc]
      .dmap:
        _.flatMap(_.child(F.profile).flatMap(_.getAsOpt[RealName]("realName")))

  def setUsernameCased(id: UserId, name: UserName): Funit =
    if id.is(name) then
      coll.update
        .one(
          bid(id) ++ F.changedCase.exists(false),
          set(F.username -> name.value, F.changedCase -> true)
        )
        .flatMap: result =>
          if result.n == 0 then fufail(s"You have already changed your username")
          else funit
    else fufail(s"Proposed username $name does not match old username $id")

  def setTitle(id: UserId, title: PlayerTitle): Funit =
    coll.updateField(bid(id), F.title, title).void

  def removeTitle(id: UserId): Funit =
    coll.unsetField(bid(id), F.title).void

  val enabledSelect = bdoc(F.enabled -> true)
  val disabledSelect = bdoc(F.enabled -> false)
  val notForeverClosedSelect = F.foreverClosed.neq(true)
  def markSelect(mark: UserMark)(v: Boolean): Bdoc =
    if v then bdoc(F.marks -> mark.key)
    else F.marks.neq(mark.key)
  def engineSelect = markSelect(UserMark.engine)
  def trollSelect = markSelect(UserMark.troll)
  val lame = bdoc(F.marks.in(List(UserMark.engine, UserMark.boost)))
  val lameOrTroll = bdoc(F.marks.in(List(UserMark.engine, UserMark.boost, UserMark.troll)))
  val notLame = bdoc(F.marks.nin(List(UserMark.engine, UserMark.boost)))
  val enabledNoBotSelect = enabledSelect ++ bdoc(F.title.neq(PlayerTitle.BOT))
  val patronSelect = bdoc(s"${F.plan}.active" -> true)

  val sortCreatedAtDesc = sort.desc(F.createdAt)

  def incNbGames(
      id: UserId,
      rated: chess.Rated,
      result: Int,
      totalTime: Option[Int],
      tvTime: Option[Int],
      botVsHuman: Boolean
  ) =
    val incs: List[BSONElement] = List(
      "count.game".some,
      rated.yes.option("count.rated"),
      (result match
        case -1 => "count.loss".some
        case 1 => "count.win".some
        case 0 => "count.draw".some
        case _ => none
      )
    ).flatten.map(k => BSONElement(k, BSONInteger(1))) ::: List(
      totalTime.map(v => BSONElement(s"${F.playTime}.total", BSONInteger(v + 2))),
      tvTime.map(v => BSONElement(s"${F.playTime}.tv", BSONInteger(v + 2))),
      totalTime.ifTrue(botVsHuman).map(v => BSONElement(s"${F.playTime}.human", BSONInteger(v + 2)))
    ).flatten

    coll.update.one(bid(id), inc(bdoc(incs*)))

  def incToints(id: UserId, nb: Int): Funit = coll.update.one(bid(id), inc("toints" -> nb)).void

  def create(
      name: UserName,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mustConfirmEmail: Boolean,
      lang: Option[LangTag] = None,
      kid: KidMode = KidMode.No
  ): Fu[Option[User]] =
    existsPri(name).not.flatMapz:
      val doc = newUser(name, passwordHash, email, blind, mustConfirmEmail, lang, kid) ++
        ("len" -> BSONInteger(name.value.length))
      coll.insert.one(doc) >> byId(name.id)

  def existsPri[U: UserIdOf](u: U): Fu[Boolean] = coll.exists(bid(u.id))
  def existsSec[U: UserIdOf](u: U): Fu[Boolean] = coll.secondary.exists(bid(u.id))

  def filterExists(ids: Set[UserId]): Fu[List[UserId]] =
    coll.primitive[UserId](inIds(ids), F.id)

  def userIdsLikeWithRole(text: UserSearch, role: RoleDbKey, max: Int = 10): Fu[List[UserId]] =
    userIdsLikeFilter(text, bdoc(F.roles -> role), max)

  def userIdsLikeClosed(text: UserSearch, max: Int = 10): Fu[List[UserId]] =
    userIdsLikeFilter(text, bdoc(F.enabled -> false), max)

  private[user] def userIdsLikeFilter(text: UserSearch, filter: Bdoc, max: Int): Fu[List[UserId]] =
    coll
      .find(
        bdoc(F.id.regexStart(text.value)) ++ enabledSelect ++ filter,
        bdoc(F.id -> true).some
      )
      .sort(bdoc("len" -> 1))
      .cursor[Bdoc](ReadPref.sec)
      .list(max)
      .map:
        _.flatMap { _.getAsOpt[UserId](F.id) }

  def idLikeCanBeVeryExpensive(regex: String, closed: Boolean): Fu[List[User]] =
    coll.find(F.id.regex(regex) ++ bdoc(F.enabled -> !closed)).cursor[User](ReadPref.sec).list(200)

  private def setMark(mark: UserMark)(id: UserId, v: Boolean): Funit =
    coll.update.one(bid(id), addOrPull(F.marks, mark, v)).void

  def setEngine = setMark(UserMark.engine)
  def setBoost = setMark(UserMark.boost)
  def setTroll = setMark(UserMark.troll)
  def setIsolate = setMark(UserMark.isolate)
  def setReportban = setMark(UserMark.reportban)
  def setRankban = setMark(UserMark.rankban)
  def setArenaBan = setMark(UserMark.arenaban)
  def setPrizeban = setMark(UserMark.prizeban)
  def setAlt = setMark(UserMark.alt)

  private[user] def setKid(user: User, v: KidMode) = coll.updateField(bid(user.id), F.kid, v).void

  def isKid[U: UserIdOf](u: U): Fu[KidMode] = KidMode.from:
    coll.exists(bid(u.id) ++ bdoc(F.kid -> true))

  def updateTroll(user: User) = setTroll(user.id, user.marks.troll)

  def filterLame(ids: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some(inIds(ids) ++ lame))

  def filterKid[U: UserIdOf](ids: Seq[U]): Fu[Set[UserId]] =
    coll.distinct[UserId, Set]("_id", Some(inIds(ids.map(_.id)) ++ bdoc(F.kid -> true)))

  def isTroll(id: UserId): Fu[Boolean] = coll.exists(bid(id) ++ trollSelect(true))

  def isBot(id: UserId): Fu[Boolean] = coll.exists(bid(id) ++ botSelect(true))

  def isAlt(id: UserId): Fu[Boolean] = coll.exists(bid(id) ++ markSelect(UserMark.alt)(true))

  def isCreatedSince(id: UserId, since: Instant): Fu[Boolean] =
    coll.exists(bid(id) ++ bdoc(F.createdAt.lt(since)))

  def setRoles(id: UserId, roles: List[RoleDbKey]): Funit =
    coll.updateOrUnsetField(bid(id), F.roles, Option.when(roles.nonEmpty)(roles)).void

  def getRoles[U: UserIdOf](u: U): Fu[List[RoleDbKey]] =
    coll.primitiveOne[List[RoleDbKey]](bid(u.id), BSONFields.roles).dmap(_.orZero)

  def addPermission(id: UserId, perm: lila.core.perm.Permission): Funit =
    coll.update.one(bid(id), push(F.roles -> perm.dbKey)).void

  def accountAge(id: UserId): Fu[Days] =
    coll
      .primitiveOne[Instant](bid(id), F.createdAt)
      .map:
        _.fold(Days(0)): date =>
          Days(scalalib.time.daysBetween(date.withTimeAtStartOfDay, nowInstant.withTimeAtStartOfDay))

  def disableTwoFactor(id: UserId) = coll.update.one(bid(id), unset(F.totpSecret))

  def setupTwoFactor(id: UserId, totp: TotpSecret): Funit =
    coll.update
      .one(
        bid(id) ++ (F.totpSecret.exists(false)), // never overwrite existing secret
        set(F.totpSecret -> totp.secret)
      )
      .void

  def reopen(id: UserId) =
    coll.update.one(
      bid(id),
      set(F.enabled -> true) ++ unset(F.delete) ++ pull(F.marks, UserMark.alt)
    ) >>
      coll.update
        .one(
          bid(id) ++ bdoc(F.email.exists(false)),
          bdoc("$rename" -> bdoc(F.prevEmail -> F.email))
        )
        .void
        .recover(lila.db.recoverDuplicateKey(_ => ()))

  def disable(user: User, keepEmail: Boolean, forever: Boolean): Funit =
    val sets = bdoc(F.enabled -> false).++(forever.so(bdoc(F.foreverClosed -> true)))
    val unsets = List(F.roles.some, keepEmail.option(F.mustConfirmEmail)).flatten
    coll.update
      .one(
        bid(user.id),
        bdoc("$set" -> sets) ++
          unset(unsets) ++
          keepEmail.not.so(bdoc("$rename" -> bdoc(F.email -> F.prevEmail)))
      )
      .void

  object delete:

    def nowWithTosViolation(user: User) =
      import F.*
      val fields = List(
        profile,
        roles,
        toints,
        "time",
        kid,
        lang,
        title,
        plan,
        totpSecret,
        changedCase,
        blind,
        salt,
        bpass,
        "mustConfirmEmail",
        colorIt,
        F.foreverClosed,
        F.delete
      )
      coll.update.one(
        bid(user.id),
        unset(fields) ++ set("deletedAt" -> nowInstant)
      )

    def nowFully(user: User) = for
      lockEmail <- emailOrPrevious(user.id)
      _ <- coll.update.one(
        bid(user.id),
        bdoc(
          "prevEmail" -> lockEmail,
          "createdAt" -> user.createdAt,
          "deletedAt" -> nowInstant
        )
      )
    yield ()

    def findNextScheduled: Fu[Option[User]] =
      val requestedAt = nowInstant.minusDays(7)
      coll
        .find:
          bdoc( // hits the delete.requested_1 index
            s"${F.delete}.requested".lt(requestedAt),
            s"${F.delete}.done" -> false
          )
        .sort(bdoc(s"${F.delete}.requested" -> 1))
        .one[User]

    def schedule(userId: UserId, delete: Option[UserDelete]): Funit =
      coll.updateOrUnsetField(bid(userId), F.delete, delete).void

  def getPasswordHash(id: UserId): Fu[Option[String]] =
    coll.byId[AuthData](id, AuthData.projection).map2(_.bpass.bytes.sha512.hex)

  def blankPassword(id: UserId): Funit =
    coll.updateField(bid(id), F.bpass, HashedPassword(Array.empty)).void

  def setEmail(id: UserId, email: EmailAddress): Funit =
    val normalized = email.normalize
    coll.update
      .one(
        bid(id),
        if email.value == normalized.value then
          set(F.email -> normalized) ++ unset(F.prevEmail, F.verbatimEmail)
        else set(F.email -> normalized, F.verbatimEmail -> email) ++ unset(F.prevEmail)
      )
      .map: _ =>
        lila.common.Bus.pub(lila.core.user.ChangeEmail(id, email))

  private[user] def anyEmail(doc: Bdoc): Option[EmailAddress] =
    doc.getAsOpt[EmailAddress](F.verbatimEmail).orElse(doc.getAsOpt[EmailAddress](F.email))

  private def anyEmailOrPrevious(doc: Bdoc): Option[EmailAddress] =
    anyEmail(doc).orElse(doc.getAsOpt[EmailAddress](F.prevEmail))

  def email(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find(bid(id), bdoc(F.email -> true, F.verbatimEmail -> true).some)
      .one[Bdoc]
      .mapz(anyEmail)

  def emailOrPrevious(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find(bid(id), bdoc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .mapz(anyEmailOrPrevious)

  def notClosedForeverWithEmail(email: NormalizedEmailAddress): Fu[Option[(User, EmailAddress)]] =
    coll
      .find(bdoc(F.email -> email, notForeverClosedSelect))
      .one[Bdoc]
      .map: maybeDoc =>
        for
          doc <- maybeDoc
          storedEmail <- anyEmail(doc)
          user <- doc.asOpt[User]
        yield (user, storedEmail)

  def prevEmail(id: UserId): Fu[Option[EmailAddress]] =
    coll.primitiveOne[EmailAddress](bid(id), F.prevEmail)

  def currentOrPrevEmail(id: UserId): Fu[Option[EmailAddress]] =
    coll
      .find(bid(id), bdoc(F.email -> true, F.verbatimEmail -> true, F.prevEmail -> true).some)
      .one[Bdoc]
      .mapz: doc =>
        anyEmail(doc).orElse(doc.getAsOpt[EmailAddress](F.prevEmail))

  def emailMap(ids: List[UserId]): Fu[Map[UserId, EmailAddress]] =
    coll
      .find(
        inIds(ids),
        bdoc(F.verbatimEmail -> true, F.email -> true, F.prevEmail -> true).some
      )
      .cursor[Bdoc](ReadPref.sec)
      .listAll()
      .map: docs =>
        for
          doc <- docs
          email <- anyEmailOrPrevious(doc)
          id <- doc.getAsOpt[UserId](F.id)
        yield id -> email
      .dmap(_.toMap)

  def isManaged(id: UserId): Fu[Boolean] = email(id).dmap(_.exists(_.isNoReply))

  def botSelect(v: Boolean) =
    if v then bdoc(F.title -> PlayerTitle.BOT)
    else bdoc(F.title -> neq(PlayerTitle.BOT))

  def botWithBioSelect = botSelect(true) ++ bdoc(s"${F.profile}.bio" -> exists(true))

  private[user] def botIds =
    coll.distinctEasy[UserId, Set]("_id", botSelect(true) ++ enabledSelect, _.sec)

  def getTitle(id: UserId): Fu[Option[PlayerTitle]] = coll.primitiveOne[PlayerTitle](bid(id), F.title)

  def hasTitle(id: UserId): Fu[Boolean] = getTitle(id).dmap(_.exists(PlayerTitle.BOT != _))

  def setPlan(user: User, plan: Option[Plan]): Funit =
    coll.updateOrUnsetField(bid(user.id), BSONFields.plan, plan).void

  def setSeenAt(id: UserId): Unit =
    coll.updateFieldUnchecked(bid(id), F.seenAt, nowInstant)

  def setLang(user: User, lang: play.api.i18n.Lang) =
    coll.updateField(bid(user.id), "lang", lang.code).void

  def langOf(id: UserId): Fu[Option[LangTag]] = coll.primitiveOne[LangTag](bid(id), "lang")

  def filterByEnabledPatrons(userIds: List[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, inIds(userIds) ++ enabledSelect ++ patronSelect, _.sec)

  def filterEnabled(userIds: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, inIds(userIds) ++ enabledSelect, _.sec)

  def filterDisabled(userIds: Iterable[UserId]): Fu[Set[UserId]] =
    userIds.nonEmpty.so:
      coll.distinctEasy[UserId, Set](F.id, inIds(userIds) ++ disabledSelect, _.sec)

  def containsDisabled(userIds: Iterable[UserId]): Fu[Boolean] =
    userIds.nonEmpty.so:
      coll.secondary.exists(inIds(userIds) ++ disabledSelect)

  def userIdsWithRoles(roles: List[RoleDbKey]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("_id", bdoc("roles".in(roles)))

  def countEngines(userIds: List[UserId]): Fu[Int] =
    coll.secondary.countSel(inIds(userIds) ++ engineSelect(true))

  def filterEngines(userIds: Seq[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](F.id, inIds(userIds) ++ engineSelect(true), _.sec)

  def countLameOrTroll(userIds: List[UserId]): Fu[Int] =
    coll.secondary.countSel(inIds(userIds) ++ lameOrTroll)

  def containsEngine(userIds: List[UserId]): Fu[Boolean] =
    coll.exists(inIds(userIds) ++ engineSelect(true))

  def mustConfirmEmail(id: UserId): Fu[Boolean] =
    coll.exists(bid(id) ++ bdoc(F.mustConfirmEmail.exists(true)))

  def setEmailConfirmed(id: UserId): Fu[Option[EmailAddress]] = for
    res <- coll.update.one(bid(id) ++ bdoc(F.mustConfirmEmail.exists(true)), unset(F.mustConfirmEmail))
    email <- (res.nModified == 1).so(email(id))
  yield email

  def setFlair(user: User, flair: Option[Flair]): Funit =
    coll.updateOrUnsetField(bid(user.id), F.flair, flair).void

  def unsetFlairs(all: Set[(UserId, Flair)]): Funit = all.nonEmpty.so:
    all.toList.sequentiallyVoid: (userId, flair) =>
      coll.unsetField(bid(userId) ++ bdoc(BSONFields.flair -> flair), BSONFields.flair)

  def unsetBio(id: UserId): Funit =
    coll.unsetField(bid(id), s"${F.profile}.bio").void

  def byIdAs[A: BSONDocumentReader](id: String, proj: Bdoc): Fu[Option[A]] =
    coll.one[A](bid(id), proj)

  def closedFlags(user: User): Fu[Option[ClosedFlags]] =
    user.enabled.no.so:
      coll
        .exists(bid(user.id) ++ bdoc(F.foreverClosed -> true))
        .zip(coll.exists(bid(user.id) ++ bdoc(s"${F.delete}.done" -> true)))
        .map(ClosedFlags(_, _).some)

  def filterClosedOrInactiveIds(since: Instant)(ids: Iterable[UserId]): Fu[List[UserId]] =
    coll.distinctEasy[UserId, List](F.id, inIds(ids) ++ or(disabledSelect, F.seenAt.lt(since)), _.sec)

  private val defaultCount = lila.core.user.Count(0, 0, 0, 0, 0)

  private def newUser(
      name: UserName,
      passwordHash: HashedPassword,
      email: EmailAddress,
      blind: Boolean,
      mustConfirmEmail: Boolean,
      lang: Option[LangTag],
      kid: KidMode
  ) =
    val normalizedEmail = email.normalize
    val now = nowInstant
    bdoc(
      F.id -> name.id,
      F.username -> name.value,
      F.email -> normalizedEmail,
      F.mustConfirmEmail -> mustConfirmEmail.option(now),
      F.bpass -> passwordHash,
      F.count -> defaultCount,
      F.enabled -> true,
      F.createdAt -> now,
      F.seenAt -> now,
      F.playTime -> PlayTime(0, 0, none),
      F.lang -> lang
    ) ++ {
      (email.value != normalizedEmail.value).so(bdoc(F.verbatimEmail -> email))
    } ++ {
      blind.so(bdoc(F.blind -> true))
    } ++ {
      kid.yes.so(bdoc(F.kid -> kid))
    }
