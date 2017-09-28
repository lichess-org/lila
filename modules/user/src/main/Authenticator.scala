package lila.user

import com.roundeights.hasher.Implicits._
import reactivemongo.bson.Macros

import lila.common.EmailAddress
import lila.db.dsl._
import lila.user.User.{ BSONFields => F }

final class Authenticator(
    passHasher: PasswordHasher,
    userRepo: UserRepo.type,
    upgradeShaPasswords: Boolean,
    onShaLogin: () => Unit
) {
  import Authenticator._

  def passEnc(pass: String): Array[Byte] = passHasher.hash(pass)

  def compare(auth: AuthData, p: String): Boolean = {
    val newP = auth.salt.fold(p) { s =>
      val salted = s"$p{$s}" // BC
      (~auth.sha512).fold(salted.sha512, salted.sha1).hex
    }
    auth.bpass match {
      // Deprecated fallback. Log & fail after DB migration.
      case None => auth.password ?? { p => onShaLogin(); p == newP }
      case Some(bHash) => passHasher.check(bHash, newP)
    }
  }

  def authenticateById(id: User.ID, password: String): Fu[Option[User]] =
    loginCandidateById(id) map { _ flatMap { _(password) } }

  def authenticateByEmail(email: EmailAddress, password: String): Fu[Option[User]] =
    loginCandidateByEmail(email) map { _ flatMap { _(password) } }

  // This creates a bcrypt hash using the existing sha as input,
  // allowing us to migrate all users in bulk.
  def upgradePassword(a: AuthData) = (a.bpass, a.password) match {
    case (None, Some(pass)) => Some(userRepo.coll.update(
      $id(a._id),
      $set(F.bpass -> passEnc(pass)) ++ $unset(F.password)
    ).void >>- lila.mon.user.auth.shaBcUpgrade())

    case _ => None
  }

  def loginCandidate(u: User): Fu[User.LoginCandidate] =
    loginCandidateById(u.id) map { _ | User.LoginCandidate(u, _ => false) }

  def loginCandidateById(id: User.ID): Fu[Option[User.LoginCandidate]] =
    loginCandidate($id(id))

  def loginCandidateByEmail(email: EmailAddress): Fu[Option[User.LoginCandidate]] =
    loginCandidate($doc(F.email -> email))

  def setPassword(id: User.ID, pass: String): Funit =
    userRepo.coll.update(
      $id(id),
      $set(F.bpass -> passEnc(pass)) ++ $unset(F.salt, F.password, F.sha512)
    ).void

  private def authWithBenefits(auth: AuthData)(p: String): Boolean = {
    val res = compare(auth, p)
    if (res && auth.salt.isDefined && upgradeShaPasswords)
      setPassword(id = auth._id, pass = p) >>- lila.mon.user.auth.bcFullMigrate()
    res
  }

  private def loginCandidate(select: Bdoc): Fu[Option[User.LoginCandidate]] =
    userRepo.coll.uno[AuthData](select)(AuthDataBSONHandler) zip userRepo.coll.uno[User](select) map {
      case (Some(authData), Some(user)) if user.enabled =>
        User.LoginCandidate(user, authWithBenefits(authData)).some
      case _ => none
    }
}

object Authenticator {

  case class AuthData(
      _id: User.ID,
      bpass: Option[Array[Byte]] = None,
      password: Option[String] = None,
      salt: Option[String] = None,
      sha512: Option[Boolean] = None
  ) {

    def hashToken: String = bpass.fold(~password) { _.sha512.hex }
  }
  implicit val AuthDataBSONHandler = Macros.handler[AuthData]
}
