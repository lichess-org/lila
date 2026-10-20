package lila.security

import com.roundeights.hasher.Implicits.*

import lila.core.security.{ ClearPassword, HashedPassword }
import lila.db.dsl.{ *, given }
import lila.user.{ AuthData, BSONFields as F, UserRepo }

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

  def setPassword(id: UserId, p: ClearPassword): Funit =
    userRepo.coll.update
      .one(
        $id(id),
        $set(F.bpass -> passEnc(p).bytes) ++ $unset(F.salt, F.sha512)
      )
      .void
