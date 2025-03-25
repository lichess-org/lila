package lila.security

import com.roundeights.hasher.Implicits.*

import lila.core.email.NormalizedEmailAddress
import lila.core.security.{ ClearPassword, HashedPassword }
import lila.db.dsl.{ *, given }
import lila.user.{ AuthData, BSONFields as F, TotpToken, UserRepo }

case class PasswordAndToken(password: ClearPassword, token: Option[TotpToken])
type CredentialCheck = ClearPassword => Boolean
case class LoginCandidate(user: User, check: CredentialCheck, isBlanked: Boolean, must2fa: Boolean = false):
  import LoginCandidate.*
  import lila.user.TotpSecret.verify
  def apply(p: PasswordAndToken): Result =
    val res =
      if user.totpSecret.isEmpty && must2fa then Result.Must2fa
      else if check(p.password) then
        user.totpSecret.fold[Result](Result.Success(user)): tp =>
          p.token.fold[Result](Result.MissingTotpToken): token =>
            if tp.verify(token) then Result.Success(user) else Result.InvalidTotpToken
      else if isBlanked then Result.BlankedPassword
      else Result.InvalidUsernameOrPassword
    lila.mon.user.auth.count(res.success).increment()
    res
  def option(p: PasswordAndToken): Option[User] = apply(p).toOption
object LoginCandidate:
  enum Result(val toOption: Option[User]):
    def success = toOption.isDefined
    case Success(user: User)       extends Result(user.some)
    case InvalidUsernameOrPassword extends Result(none)
    case Must2fa                   extends Result(none)
    case BlankedPassword           extends Result(none)
    case WeakPassword              extends Result(none)
    case MissingTotpToken          extends Result(none)
    case InvalidTotpToken          extends Result(none)

final class Authenticator(
    passHasher: PasswordHasher,
    userRepo: UserRepo
)(using Executor)
    extends lila.core.security.Authenticator:

  def passEnc(p: ClearPassword): HashedPassword = passHasher.hash(p)

  def compare(auth: AuthData, p: ClearPassword): Boolean =
    val newP = auth.salt.fold(p): s =>
      val salted = s"${p.value}{$s}" // BC
      ClearPassword(if ~auth.sha512 then salted.sha512 else salted.sha1)
    passHasher.check(auth.bpass, newP)

  def authenticateById(id: UserId, passwordAndToken: PasswordAndToken): Fu[Option[User]] =
    loginCandidateById(id).map { _.flatMap { _.option(passwordAndToken) } }

  def authenticateByEmail(
      email: NormalizedEmailAddress,
      passwordAndToken: PasswordAndToken
  ): Fu[Option[User]] =
    loginCandidateByEmail(email).map { _.flatMap { _.option(passwordAndToken) } }

  def loginCandidate(using me: Me): Fu[LoginCandidate] =
    loginCandidateById(me.userId).dmap { _ | LoginCandidate(me, _ => false, false) }

  def loginCandidateById(id: UserId): Fu[Option[LoginCandidate]] =
    loginCandidate($id(id))

  def loginCandidateByEmail(email: NormalizedEmailAddress): Fu[Option[LoginCandidate]] =
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
      for _ <- setPassword(id = auth._id, p)
      yield lila.mon.user.auth.bcFullMigrate.increment()
    res

  private def loginCandidate(select: Bdoc): Fu[Option[LoginCandidate]] = {
    import lila.user.BSONHandlers.given
    userRepo.coll
      .one[AuthData](select, AuthData.projection)
      .zip(userRepo.coll.one[User](select))
      .map:
        case (Some(authData), Some(user)) =>
          LoginCandidate(user, authWithBenefits(authData), isBlanked = authData.bpass.bytes.isEmpty).some
        case _ => none
  }.recover:
    case _: reactivemongo.api.bson.exceptions.HandlerException           => none
    case _: reactivemongo.api.bson.exceptions.BSONValueNotFoundException => none // erased user
