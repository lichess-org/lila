package lidraughts.oauth

case class AccessTokenJWT(value: String) extends AnyVal

object JWT {
  case class PublicKey(value: String) extends AnyVal
}

trait OauthException extends lidraughts.base.LidraughtsException