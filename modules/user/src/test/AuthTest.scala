package lila.user

import org.specs2.mutable.Specification
import java.util.Base64

class AuthTest extends Specification {

  val secret = Array.fill(32)(1.toByte).toBase64
  val authWrapper = new Authenticator(new PasswordHasher(secret, 2), None)
  import authWrapper.{ passEnc, AuthData }

  // Extracted from mongo
  val shaUser = AuthData(
    _id = "foo",
    password = Some("1c4b2f9a0605c1af73d0ac66ab67c89a6bc76efa"),
    salt = Some("7IzdmPSe0iZnGc1ChY32fVsfrZBLdIlN")
  )

  "sha matches" in {
    // Mongo after password change
    val shaUserWithKey = shaUser.copy(sha512 = Some(false))

    "correct1" >> shaUser.compare("password")
    "correct2" >> shaUserWithKey.compare("password")
    "wrong1" >> !shaUser.compare("")
    "wrong2" >> !shaUser.compare("")
    "wrong sha" >> !shaUser.copy(sha512 = Some(true)).compare("password")
  }

  "bcrypt checks" in {
    val bCryptUser = AuthData(
      _id = "foo",
      bpass = Some(Base64.getDecoder.decode(
        "nMY6Pi45178YiOMcWncklizO3Z2enZfiFF5RDBkFZFYEOP7rZ2F0"
      ))
    )
    "correct" >> bCryptUser.compare("password")
    "wrong pass" >> !bCryptUser.compare("")

    // bpass is salted with id to prevent copying a bpass field
    "wrong user" >> !bCryptUser.copy(_id = "bar").compare("password")

    // sanity check of aes encryption
    "wrong secret" >> !{
      val badHasher = new PasswordHasher((new Array[Byte](32)).toBase64, 2)
      new Authenticator(badHasher, None).AuthData(
        _id = "foo",
        bpass = bCryptUser.bpass
      ).compare("password")
    }
  }

  "migrated user" in {
    val shaToBcrypt = shaUser.copy(
      // generated purely from stored data
      bpass = Some(passEnc("foo", shaUser.password.get))
    )

    val shaToBcryptNoPass = shaToBcrypt.copy(
      password = None,
      sha512 = Some(false)
    )

    "correct" >> shaToBcrypt.compare("password")
    "wrong pass" >> !shaToBcrypt.compare("")
    "wrong user" >> !shaToBcrypt.copy(_id = "bar").compare("password")
    "no pass" >> shaToBcryptNoPass.compare("password")
    "sha flag lost" >> !shaToBcryptNoPass.copy(sha512 = None).compare("password")
    "wrong sha" >> !shaToBcryptNoPass.copy(sha512 = Some(true)).compare("password")
  }
}