package lila.user

import org.specs2.mutable.Specification
import java.util.Base64
import Authenticator.AuthData
import User.{ ClearPassword => P }
import lila.common.config.Secret
import scala.concurrent.ExecutionContext.Implicits.global

class AuthTest extends Specification {

  val secret = Secret(Array.fill(32)(1.toByte).toBase64)
  final def getAuth(passHasher: PasswordHasher) =
    new Authenticator(
      passHasher = passHasher,
      userRepo = null
    )

  val auth = getAuth(new PasswordHasher(secret, 2))

  "bcrypt checks" in {
    val bCryptUser = AuthData(
      "",
      bpass = HashedPassword(
        Base64.getDecoder.decode(
          "+p7ysDb8OU9yMQ/LuFxFNgJ0HBKH7iJy8tkowG65NWjPC3Y6CzYV"
        )
      )
    )
    "correct" >> auth.compare(bCryptUser, P("password"))
    "wrong pass" >> !auth.compare(bCryptUser, P(""))

    // sanity check of aes encryption
    "wrong secret" >> ! {
      getAuth(new PasswordHasher(Secret((new Array[Byte](32)).toBase64), 2)).compare(
        bCryptUser,
        P("password")
      )
    }

    "very long password" in {
      val a100 = P("a" * 100)
      val user = AuthData("", bpass = auth.passEnc(a100))
      "correct" >> auth.compare(user, a100)
      "wrong fails" >> !auth.compare(user, P("a" * 99))
    }

    "handle crazy passwords" in {
      val abcUser = AuthData("", bpass = auth.passEnc(P("abc")))

      "test eq" >> auth.compare(abcUser, P("abc"))
      "vs null bytes" >> !auth.compare(abcUser, P("abc\u0000"))
      "vs unicode" >> !auth.compare(abcUser, P("abc\uD83D\uDE01"))
      "vs empty" >> !auth.compare(abcUser, P(""))
    }
  }

  "migrated user" in {
    val shaToBcrypt = AuthData(
      "",
      salt = Some("7IzdmPSe0iZnGc1ChY32fVsfrZBLdIlN"),
      // Sha hash extracted from mongo
      bpass = auth.passEnc(P("1c4b2f9a0605c1af73d0ac66ab67c89a6bc76efa"))
    )

    "correct" >> auth.compare(shaToBcrypt, P("password"))
    "wrong pass" >> !auth.compare(shaToBcrypt, P(""))
    "wrong sha" >> !auth.compare(shaToBcrypt.copy(sha512 = Some(true)), P("password"))
  }
}
