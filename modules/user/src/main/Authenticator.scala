package lila.user

import com.roundeights.hasher.Implicits._
import reactivemongo.bson._

import lila.common.EmailAddress
import lila.db.dsl._
import lila.user.User.{ ClearPassword, BSONFields => F }

final class Authenticator(
    passHasher: PasswordHasher,
    userRepo: UserRepo.type,
    upgradeShaPasswords: Boolean,
    onShaLogin: () => Unit
) {
  import Authenticator._

  def passEnc(p: ClearPassword): HashedPassword = passHasher.hash(p)

  def compare(auth: AuthData, p: ClearPassword): Boolean = {
    val newP = auth.salt.fold(p) { s =>
      val salted = s"${p.value}{$s}" // BC
      ClearPassword((~auth.sha512).fold(salted.sha512, salted.sha1))
    }
    auth.bpass match {
      // Deprecated fallback. Log & fail after DB migration.
      case None => auth.password ?? { sHash => onShaLogin(); sHash == newP.value }
      case Some(bpass) => passHasher.check(bpass, newP)
    }
  }

  def authenticateById(id: User.ID, password: ClearPassword): Fu[Option[User]] =
    loginCandidateById(id) map { _ flatMap { _(password) } }

  def authenticateByEmail(email: EmailAddress, password: ClearPassword): Fu[Option[User]] =
    loginCandidateByEmail(email) map { _ flatMap { _(password) } }

  // This creates a bcrypt hash using the existing sha as input,
  // allowing us to migrate all users in bulk.
  def upgradePassword(a: AuthData): Funit = (a.bpass, a.password) match {
    case (None, Some(shaHash)) => setPassword(a._id, ClearPassword(shaHash)).void >>-
      lila.mon.user.auth.shaBcUpgrade()
    case _ => funit
  }

  def loginCandidate(u: User): Fu[User.LoginCandidate] =
    loginCandidateById(u.id) map { _ | User.LoginCandidate(u, _ => false) }

  def loginCandidateById(id: User.ID): Fu[Option[User.LoginCandidate]] =
    loginCandidate($id(id))

  def loginCandidateByEmail(email: EmailAddress): Fu[Option[User.LoginCandidate]] =
    loginCandidate($doc(F.email -> email))

  def setPassword(id: User.ID, p: ClearPassword): Funit =
    userRepo.coll.update(
      $id(id),
      $set(F.bpass -> passEnc(p).bytes) ++ $unset(F.salt, F.password, F.sha512)
    ).void

  private def authWithBenefits(auth: AuthData)(p: ClearPassword): Boolean = {
    val res = compare(auth, p)
    if (res && auth.salt.isDefined && upgradeShaPasswords)
      setPassword(id = auth._id, p) >>- lila.mon.user.auth.bcFullMigrate()
    res
  }

  private def loginCandidate(select: Bdoc): Fu[Option[User.LoginCandidate]] =
    userRepo.coll.uno[AuthData](select, authProjection)(AuthDataBSONHandler) zip userRepo.coll.uno[User](select) map {
      case (Some(authData), Some(user)) if user.enabled =>
        User.LoginCandidate(user, authWithBenefits(authData)).some
      case _ => none
    }
}

object Authenticator {

  case class AuthData(
      _id: User.ID,
      bpass: Option[HashedPassword] = None,
      password: Option[String] = None,
      salt: Option[String] = None,
      sha512: Option[Boolean] = None
  ) {

    def hashToken: String = bpass.fold(~password) { _.bytes.sha512.hex }
  }

  val authProjection = $doc(
    F.bpass -> true,
    F.password -> true,
    F.salt -> true,
    F.sha512 -> true
  )

  implicit val HashedPasswordBsonHandler = new BSONHandler[BSONBinary, HashedPassword] {
    def read(b: BSONBinary) = HashedPassword(b.byteArray)
    def write(hash: HashedPassword) = BSONBinary(hash.bytes, Subtype.GenericBinarySubtype)
  }

  implicit val AuthDataBSONHandler = Macros.handler[AuthData]
}
