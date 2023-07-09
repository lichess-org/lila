package lila.user

import com.roundeights.hasher.Implicits.*
import reactivemongo.api.bson.*

import lila.common.NormalizedEmailAddress
import lila.db.dsl.{ *, given }
import lila.user.User.{ BSONFields as F, ClearPassword, PasswordAndToken }

final class Authenticator(
    passHasher: PasswordHasher,
    userRepo: UserRepo
)(using Executor):
  import Authenticator.{ *, given }

  def passEnc(p: ClearPassword): HashedPassword = passHasher.hash(p)

  def compare(auth: AuthData, p: ClearPassword): Boolean =
    val newP = auth.salt.fold(p) { s =>
      val salted = s"${p.value}{$s}" // BC
      ClearPassword(if ~auth.sha512 then salted.sha512 else salted.sha1)
    }
    passHasher.check(auth.bpass, newP)

  def authenticateById(id: UserId, passwordAndToken: PasswordAndToken): Fu[Option[User]] =
    loginCandidateById(id) map { _ flatMap { _ option passwordAndToken } }

  def authenticateByEmail(
      email: NormalizedEmailAddress,
      passwordAndToken: PasswordAndToken
  ): Fu[Option[User]] =
    loginCandidateByEmail(email) map { _ flatMap { _ option passwordAndToken } }

  def loginCandidate(using me: Me): Fu[User.LoginCandidate] =
    loginCandidateById(me.userId) dmap { _ | User.LoginCandidate(me, _ => false, false) }

  def loginCandidateById(id: UserId): Fu[Option[User.LoginCandidate]] =
    loginCandidate($id(id))

  def loginCandidateByEmail(email: NormalizedEmailAddress): Fu[Option[User.LoginCandidate]] =
    loginCandidate($doc(F.email -> email))

  def setPassword(id: UserId, p: ClearPassword): Funit =
    userRepo.coll.update
      .one(
        $id(id),
        $set(F.bpass -> passEnc(p).bytes) ++ $unset(F.salt, F.sha512)
      )
      .void

  private def authWithBenefits(auth: AuthData)(p: ClearPassword): Boolean =
    val res = compare(auth, p)
    if res && auth.salt.isDefined then
      setPassword(id = auth._id, p) andDo lila.mon.user.auth.bcFullMigrate.increment()
    res

  private def loginCandidate(select: Bdoc): Fu[Option[User.LoginCandidate]] = {
    userRepo.coll.one[AuthData](select, authProjection) zip
      userRepo.coll.one[User](select) map {
        case (Some(authData), Some(user)) =>
          User.LoginCandidate(user, authWithBenefits(authData), authData.isBlanked).some
        case _ => none
      }
  } recover {
    case _: reactivemongo.api.bson.exceptions.HandlerException           => none
    case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => none // erased user
  }

object Authenticator:

  case class AuthData(
      _id: UserId,
      bpass: HashedPassword,
      salt: Option[String] = None,
      sha512: Option[Boolean] = None
  ):
    export bpass.isBlanked

    def hashToken: String = bpass.bytes.sha512.hex

  val authProjection = $doc(
    F.bpass  -> true,
    F.salt   -> true,
    F.sha512 -> true
  )

  private[user] given BSONHandler[HashedPassword] = quickHandler[HashedPassword](
    { case v: BSONBinary => HashedPassword(v.byteArray) },
    v => BSONBinary(v.bytes, Subtype.GenericBinarySubtype)
  )

  given BSONDocumentHandler[AuthData] = Macros.handler[AuthData]
