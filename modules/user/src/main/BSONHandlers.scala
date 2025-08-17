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

object BSONFields:
  export lila.core.user.BSONFields.*
  val id = "_id"
  val count = "count"
  val profile = "profile"
  val toints = "toints"
  val playTime = "time"
  val playTimeTotal = "time.total"
  val seenAt = "seenAt"
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
  given planHandler: BSONDocumentHandler[Plan] = Macros.handler[Plan]
  given profileHandler: BSONDocumentHandler[Profile] = Macros.handler[Profile]
  private[user] given BSONHandler[TotpSecret] = lila.db.dsl.quickHandler[TotpSecret](
    { case v: BSONBinary => new TotpSecret(v.byteArray) },
    v => BSONBinary(v.secret, Subtype.GenericBinarySubtype)
  )
  private[user] given BSONDocumentHandler[Count] = new BSON[Count]:
    def reads(r: BSON.Reader): Count =
      lila.core.user.Count(
        ai = r.nInt("ai"),
        draw = r.nInt("draw"),
        drawH = r.nInt("drawH"),
        game = r.nInt("game"),
        loss = r.nInt("loss"),
        lossH = r.nInt("lossH"),
        rated = r.nInt("rated"),
        win = r.nInt("win"),
        winH = r.nInt("winH")
      )
    def writes(w: BSON.Writer, o: Count) =
      $doc(
        "ai" -> w.int(o.ai),
        "draw" -> w.int(o.draw),
        "drawH" -> w.int(o.drawH),
        "game" -> w.int(o.game),
        "loss" -> w.int(o.loss),
        "lossH" -> w.int(o.lossH),
        "rated" -> w.int(o.rated),
        "win" -> w.int(o.win),
        "winH" -> w.int(o.winH)
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
      new User(
        id = r.get[UserId](id),
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
        flair = r.getO[Flair](flair).filter(FlairApi.exists),
        marks = r.getO[UserMarks](marks) | UserMarks(Nil),
        hasEmail = r.contains(email)
      )

    def writes(w: BSON.Writer, o: User) =
      BSONDocument(
        id -> o.id,
        username -> o.username,
        count -> o.count,
        enabled -> o.enabled,
        roles -> o.roles.some.filter(_.nonEmpty),
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

  private[user] given BSONDocumentHandler[lila.core.LightUser] = Macros.handler
  private[user] given BSONDocumentHandler[lila.core.user.LightPerf] = Macros.handler
  private[user] given BSONDocumentHandler[lila.user.LightCount] = Macros.handler
