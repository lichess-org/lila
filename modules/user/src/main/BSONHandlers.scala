package lila.user

import reactivemongo.api.bson.*
import scalalib.model.LangTag

import lila.core.security.HashedPassword
import lila.core.user.{
  KidMode,
  Count,
  Plan,
  PlayTime,
  Profile,
  TotpSecret,
  UserEnabled,
  UserMarks,
  RoleDbKey
}
import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.core.plan.{ PatronColor, PatronColorChoice }

object BSONFields:
  export lila.core.user.BSONFields.*
  val id = "_id"
  val count = "count"
  val profile = "profile"
  val toints = "toints"
  val playTime = "time"
  val playTimeTotal = "time.total"
  val createdWithApiVersion = "createdWithApiVersion"
  val lang = "lang"
  val email = "email"
  val verbatimEmail = "verbatimEmail"
  val mustConfirmEmail = "mustConfirmEmail"
  val prevEmail = "prevEmail"
  val colorIt = "colorIt"
  val salt = "salt"
  val bpass = "bpass"
  val sha512 = "sha512"
  val totpSecret = "totp"
  val changedCase = "changedCase"
  val delete = "delete"
  val foreverClosed = "foreverClosed"
  val blind = "blind"

  def withFields[A](f: BSONFields.type => A): A = f(BSONFields)

object BSONHandlers:

  given playTimeHandler: BSONDocumentHandler[PlayTime] = Macros.handler[PlayTime]
  given profileHandler: BSONDocumentHandler[Profile] = Macros.handler[Profile]
  private[user] given BSONHandler[TotpSecret] = lila.db.dsl.quickHandler[TotpSecret](
    { case v: BSONBinary => new TotpSecret(v.byteArray) },
    v => BSONBinary(v.secret, Subtype.GenericBinarySubtype)
  )
  val colorHandler = summon[BSONHandler[PatronColor]]
  given colorChoiceHandler: BSONHandler[PatronColorChoice] =
    isoHandler[PatronColorChoice, PatronColor](_.value, PatronColorChoice.apply)(using colorHandler)

  given planHandler: BSONDocumentHandler[Plan] = new BSON[Plan]:
    def reads(r: BSON.Reader) = Plan(
      months = r.int("months"),
      active = r.bool("active"),
      lifetime = r.boolD("lifetime"),
      since = r.dateO("since"),
      color = r.intO("color").flatMap(PatronColor.map.get).map(PatronColorChoice.apply)
    )
    def writes(w: BSON.Writer, o: Plan) = $doc(
      "months" -> w.int(o.months),
      "active" -> o.active,
      "lifetime" -> w.boolO(o.lifetime),
      "since" -> o.since,
      "color" -> o.color
    )

  private[user] given BSONDocumentHandler[Count] = new BSON[Count]:
    def reads(r: BSON.Reader): Count =
      lila.core.user.Count(
        draw = r.nInt("draw"),
        game = r.nInt("game"),
        loss = r.nInt("loss"),
        rated = r.nInt("rated"),
        win = r.nInt("win")
      )
    def writes(w: BSON.Writer, o: Count) =
      $doc(
        "draw" -> w.int(o.draw),
        "game" -> w.int(o.game),
        "loss" -> w.int(o.loss),
        "rated" -> w.int(o.rated),
        "win" -> w.int(o.win)
      )

  private[user] given BSONHandler[HashedPassword] = quickHandler[HashedPassword](
    { case v: BSONBinary => HashedPassword(v.byteArray) },
    v => BSONBinary(v.bytes, Subtype.GenericBinarySubtype)
  )

  given BSONDocumentHandler[AuthData] = Macros.handler[AuthData]
  given BSONDocumentHandler[UserDelete] = Macros.handler[UserDelete]

  given userHandler: BSONDocumentHandler[User] = new BSON[User]:

    import BSONFields.*

    def reads(r: BSON.Reader): User =
      val userId = r.get[UserId](id)
      new User(
        id = userId,
        username = r.get[UserName](username),
        count = r.get[lila.core.user.Count](count),
        enabled = r.get[UserEnabled](enabled),
        roles = ~r.getO[List[RoleDbKey]](roles),
        profile = r.getO[Profile](profile),
        toints = r.nIntD(toints),
        playTime = r.getO[PlayTime](playTime),
        createdAt = r.date(createdAt),
        seenAt = r.dateO(seenAt),
        kid = KidMode(r.boolD(kid)),
        lang = r.getO[LangTag](lang),
        title = r.getO[chess.PlayerTitle](title),
        plan = r.getO[Plan](plan) | lila.user.Plan.empty,
        totpSecret = r.getO[TotpSecret](totpSecret),
        flair = r.getO[Flair](flair) match
          case Some(f) if FlairApi.exists(f) => Some(f)
          case Some(f) => FlairApi.badFlairs.add(userId, f); None
          case None => None,
        marks = r.getO[UserMarks](marks) | UserMarks(Nil),
        hasEmail = r.contains(email)
      )

    def writes(w: BSON.Writer, o: User) =
      BSONDocument(
        id -> o.id,
        username -> o.username,
        count -> o.count,
        enabled -> o.enabled,
        roles -> o.roles.nonEmptyOption,
        profile -> o.profile,
        toints -> w.intO(o.toints),
        playTime -> o.playTime,
        createdAt -> o.createdAt,
        seenAt -> o.seenAt,
        kid -> w.boolO(o.kid.yes),
        lang -> o.lang,
        title -> o.title,
        plan -> o.plan.nonEmpty,
        totpSecret -> o.totpSecret,
        flair -> o.flair,
        marks -> o.marks.value.nonEmpty.option(o.marks)
      )

  given BSONHandler[PatronColor] = lila.db.dsl.tryHandler[PatronColor](
    { case BSONInteger(id) => PatronColor.map.get(id).toTry(s"Invalid patron color id: $id") },
    s => BSONInteger(s.id)
  )

  // This LightUser handler is only used to store light users in other documents
  // not to read light users from the user collection
  // The LightUser handler is in modules/user/src/main/LightUserApi.scala
  private given BSONDocumentHandler[lila.core.LightUser] = Macros.handler
  private[user] given BSONDocumentHandler[lila.core.user.LightPerf] = Macros.handler
  private[user] given BSONDocumentHandler[lila.user.LightCount] = Macros.handler
