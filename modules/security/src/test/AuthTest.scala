package lila.security

import java.util.Base64

import lila.core.config.Secret
import lila.core.security.{ ClearPassword as P, HashedPassword }
import lila.user.AuthData

class AuthTest extends munit.FunSuite:

  given Conversion[String, UserId]           = UserId(_)
  given Executor                             = scala.concurrent.ExecutionContextOpportunistic
  given lila.core.config.RateLimit           = lila.core.config.RateLimit.No
  extension (self: Array[Byte]) def toBase64 = Base64.getEncoder.encodeToString(self)

  val secret = Secret(Array.fill(32)(1.toByte).toBase64)
  final def getAuth(passHasher: PasswordHasher) =
    new Authenticator(
      passHasher = passHasher,
      userRepo = null
    )

  val auth = getAuth(new PasswordHasher(secret, 2))

  val bCryptUser = AuthData(
    "",
    bpass = HashedPassword(
      Base64.getDecoder.decode(
        "+p7ysDb8OU9yMQ/LuFxFNgJ0HBKH7iJy8tkowG65NWjPC3Y6CzYV"
      )
    )
  )
  test("bcrypt check correct"):
    assert(auth.compare(bCryptUser, P("password")))
  test("bcrypt check wrong pass"):
    assert(!auth.compare(bCryptUser, P("")))

  // sanity check of aes encryption
  test("bcrypt check wrong secret"):
    assert(
      !getAuth(new PasswordHasher(Secret((new Array[Byte](32)).toBase64), 2)).compare(
        bCryptUser,
        P("password")
      )
    )

  test("bcrypt check very long password correct"):
    val a100 = P("a" * 100)
    val user = AuthData("", bpass = auth.passEnc(a100))
    assert(auth.compare(user, a100))
  test("bcrypt check very long password wrong"):
    val a100 = P("a" * 100)
    val user = AuthData("", bpass = auth.passEnc(a100))
    assert(!auth.compare(user, P("a" * 99)))

  val abcUser = AuthData("", bpass = auth.passEnc(P("abc")))

  test("bcrypt check handle crazy passwords test eq"):
    assert(auth.compare(abcUser, P("abc")))
  test("bcrypt check handle crazy passwords vs null bytes"):
    assert(!auth.compare(abcUser, P("abc\u0000")))
  test("bcrypt check handle crazy passwords vs unicode"):
    assert(!auth.compare(abcUser, P("abc\uD83D\uDE01")))
  test("bcrypt check handle crazy passwords vs empty"):
    assert(!auth.compare(abcUser, P("")))

  val shaToBcrypt = AuthData(
    "",
    salt = Some("7IzdmPSe0iZnGc1ChY32fVsfrZBLdIlN"),
    // Sha hash extracted from mongo
    bpass = auth.passEnc(P("1c4b2f9a0605c1af73d0ac66ab67c89a6bc76efa"))
  )

  test("migrated correct"):
    assert(auth.compare(shaToBcrypt, P("password")))
  test("migrated wrong pass"):
    assert(!auth.compare(shaToBcrypt, P("")))
  test("migrated wrong sha"):
    assert(!auth.compare(shaToBcrypt.copy(sha512 = Some(true)), P("password")))
