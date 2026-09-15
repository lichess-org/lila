package lila.security

import scalalib.net.UserAgent

class UserAgentTest extends munit.FunSuite:

  def parse(ua: String) = UserAgentParser.parseSlowly(UserAgent(ua))
  def isDangerousIOS(ua: String) = UserAgentParser.isDangerousIOS(parse(ua))
  def isDangerousSafari(ua: String) = UserAgentParser.isDangerousSafari(parse(ua))
  def isDangerousChrome(ua: String) = UserAgentParser.isDangerousChrome(parse(ua))

  test("dangerous iOS"):
    assertEquals(
      isDangerousIOS:
        "Mozilla/5.0 (iPod touch; CPU iPhone OS 9_3_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13F69"
      ,
      "iOS 9".some
    )
    assertEquals(
      isDangerousIOS:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.5162.1554 Safari/537.36"
      ,
      none
    )
    assertEquals(
      isDangerousIOS:
        "Mozilla/5.0 (iPhone11,4; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/134.0.7132.229 Mobile/18C728 Safari/604.1"
      ,
      none
    )
    assertEquals(
      isDangerousIOS:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
      ,
      none
    )

  test("dangerous safari"):
    assertEquals(
      isDangerousSafari:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1 Safari/605.1.15"
      ,
      none
    )
    assertEquals(
      isDangerousSafari:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 Safari/605.1.15"
      ,
      "Safari 13".some
    )
    assertEquals(
      isDangerousSafari:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Safari/605.1.15"
      ,
      none
    )
    assertEquals(
      isDangerousSafari:
        "Mozilla/5.0 Slackware/13.37 (X11; U; Linux x86_64; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/11.0.696.50"
      ,
      none
    )

  test("dangerous chrome"):
    assertEquals(
      isDangerousChrome:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4086.33 Safari/537.36"
      ,
      "Chrome 99".some
    )
