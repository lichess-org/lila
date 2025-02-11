package lila.prismic

// Error thrown when communicating with prismic.io API
sealed trait ApiError extends RuntimeException {
  def message: String
  override def getMessage = message
}

// Error thrown when the auth token is omitted, but required
case class AuthorizationNeeded(message: String, oAuthUrl: String) extends ApiError

// Error thrown when the auth token is provided, but invalid
case class InvalidToken(message: String, oAuthUrl: String) extends ApiError

// Error that should never happen
case class UnexpectedError(message: String) extends ApiError
