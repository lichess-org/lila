package lila.common

case class EmailAddress(value: String) extends AnyVal with StringValue {

  def username = value.takeWhile(_ != '@')

  def conceal =
    value split '@' match {
      case Array(name, domain) => s"${name take 3}*****@$domain"
      case _                   => value
    }

  def normalize =
    NormalizedEmailAddress {
      // changing normalization requires database migration!
      val lower = value.toLowerCase
      lower.split('@') match {
        case Array(name, domain) if EmailAddress.gmailLikeNormalizedDomains(domain) =>
          val normalizedName = name
            .replace(".", "")  // remove all dots
            .takeWhile('+' !=) // skip everything after the first '+'
          if (normalizedName.isEmpty) lower else s"$normalizedName@$domain"
        case _ => lower
      }
    }

  def domain: Option[Domain] =
    value split '@' match {
      case Array(_, domain) => Domain from domain.toLowerCase
      case _                => none
    }

  def similarTo(other: EmailAddress) = normalize == other.normalize

  def isNoReply  = EmailAddress isNoReply value
  def isSendable = !isNoReply

  def looksLikeFakeEmail =
    domain.map(_.lower.value).exists(EmailAddress.gmailDomains.contains) &&
      username.count('.' ==) >= 4

  // safer logs
  override def toString = "EmailAddress(****)"
}

object EmailAddress {

  private val regex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  val gmailDomains = Set("gmail.com", "googlemail.com")

  // adding normalized domains requires database migration!
  private val gmailLikeNormalizedDomains = gmailDomains ++ Set("protonmail.com", "protonmail.ch", "pm.me")

  def isValid(str: String) =
    str.sizeIs < 320 &&
      regex.matches(str) && !str.contains("..") && !str.contains(".@") && !str.startsWith(".")

  def from(str: String): Option[EmailAddress] =
    isValid(str) option EmailAddress(str)

  private def isNoReply(str: String) = str.startsWith("noreply.") && str.endsWith("@lichess.org")

  val clasIdRegex = """^noreply\.class\.(\w{8})\.[\w-]+@lichess\.org""".r
}

case class NormalizedEmailAddress(value: String) extends AnyVal with StringValue
