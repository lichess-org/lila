package lila.user

import org.specs2.mutable.Specification
import java.util.Base64
import Authenticator.AuthData
import User.{ClearPassword => P}

class AuthTest extends Specification {

  val secret = Array.fill(32)(1.toByte).toBase64
  def getAuth(passHasher: PasswordHasher) = new Authenticator(
    passHasher = passHasher,
    userRepo = null,
    upgradeShaPasswords = false,
    onShaLogin = () => ()
  )

  val auth = getAuth(new PasswordHasher(secret, 2))

  // Extracted from mongo
  val shaUser = AuthData(
    "",
    password = Some("1c4b2f9a0605c1af73d0ac66ab67c89a6bc76efa"),
    salt = Some("7IzdmPSe0iZnGc1ChY32fVsfrZBLdIlN")
  )

  "sha matches" in {
    // Mongo after password change
    val shaUserWithKey = shaUser.copy(sha512 = Some(false))

    "correct1" >> auth.compare(shaUser, P("password"))
    "correct2" >> auth.compare(shaUserWithKey, P("password"))
    "wrong1" >> !auth.compare(shaUser, P(""))
    "wrong2" >> !auth.compare(shaUser, P(""))
    "wrong sha" >> !auth.compare(shaUser.copy(sha512 = Some(true)), P("password"))
  }

  "bcrypt checks" in {
    val bCryptUser = AuthData(
      "",
      bpass = HashedPassword(Base64.getDecoder.decode(
        "+p7ysDb8OU9yMQ/LuFxFNgJ0HBKH7iJy8tkowG65NWjPC3Y6CzYV"
      )).some
    )
    "correct" >> auth.compare(bCryptUser, P("password"))
    "wrong pass" >> !auth.compare(bCryptUser, P(""))

    // sanity check of aes encryption
    "wrong secret" >> !{
      getAuth(new PasswordHasher((new Array[Byte](32)).toBase64, 2)).compare(
        bCryptUser, P("password")
      )
    }

    "very long password" in {
      val a100 = P("a" * 100)
      val user = AuthData("", bpass = auth.passEnc(a100).some)
      "correct" >> auth.compare(user, a100)
      "wrong fails" >> !auth.compare(user, P("a" * 99))
    }

    "handle crazy passwords" in {
      val abcUser = AuthData("", bpass = auth.passEnc(P("abc")).some)

      "test eq" >> auth.compare(abcUser, P("abc"))
      "vs null bytes" >> !auth.compare(abcUser, P("abc\u0000"))
      "vs unicode" >> !auth.compare(abcUser, P("abc\uD83D\uDE01"))
      "vs empty" >> !auth.compare(abcUser, P(""))
    }
  }

  "migrated user" in {
    val shaToBcrypt = shaUser.copy(
      // generated purely from stored data
      bpass = shaUser.password map { p => auth.passEnc(P(p)) }
    )

    val shaToBcryptNoPass = shaToBcrypt.copy(password = None)

    "correct" >> auth.compare(shaToBcrypt, P("password"))
    "wrong pass" >> !auth.compare(shaToBcrypt, P(""))
    "no pass" >> auth.compare(shaToBcryptNoPass, P("password"))
    "wrong sha" >> !auth.compare(shaToBcryptNoPass.copy(sha512 = Some(true)), P("password"))
  }
}
