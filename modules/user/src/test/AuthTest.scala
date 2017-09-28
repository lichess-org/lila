package lila.user

import org.specs2.mutable.Specification
import java.util.Base64
import Authenticator.AuthData

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

    "correct1" >> auth.compare(shaUser, "password")
    "correct2" >> auth.compare(shaUserWithKey, "password")
    "wrong1" >> !auth.compare(shaUser, "")
    "wrong2" >> !auth.compare(shaUser, "")
    "wrong sha" >> !auth.compare(shaUser.copy(sha512 = Some(true)), "password")
  }

  "bcrypt checks" in {
    val bCryptUser = AuthData(
      "",
      bpass = Some(Base64.getDecoder.decode(
        "+p7ysDb8OU9yMQ/LuFxFNgJ0HBKH7iJy8tkowG65NWjPC3Y6CzYV"
      ))
    )
    "correct" >> auth.compare(bCryptUser, "password")
    "wrong pass" >> !auth.compare(bCryptUser, "")

    // sanity check of aes encryption
    "wrong secret" >> !{
      getAuth(new PasswordHasher((new Array[Byte](32)).toBase64, 2)).compare(
        bCryptUser, "password"
      )
    }

    "very long password" in {
      val longPass = "a" * 100
      val user = AuthData("", bpass = Some(auth.passEnc(longPass).bytes))
      "correct" >> auth.compare(user, longPass)
      "wrong fails" >> !auth.compare(user, "a" * 99)
    }

    "handle crazy passwords" in {
      val abcUser = AuthData("", bpass = Some(auth.passEnc("abc").bytes))

      "test eq" >> auth.compare(abcUser, "abc")
      "vs null bytes" >> !auth.compare(abcUser, "abc\u0000")
      "vs unicode" >> !auth.compare(abcUser, "abc\uD83D\uDE01")
      "vs empty" >> !auth.compare(abcUser, "")
    }
  }

  "migrated user" in {
    val shaToBcrypt = shaUser.copy(
      // generated purely from stored data
      bpass = shaUser.password map { auth.passEnc(_).bytes }
    )

    val shaToBcryptNoPass = shaToBcrypt.copy(password = None)

    "correct" >> auth.compare(shaToBcrypt, "password")
    "wrong pass" >> !auth.compare(shaToBcrypt, "")
    "no pass" >> auth.compare(shaToBcryptNoPass, "password")
    "wrong sha" >> !auth.compare(shaToBcryptNoPass.copy(sha512 = Some(true)), "password")
  }
}