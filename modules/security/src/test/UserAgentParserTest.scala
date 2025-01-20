package lila.security

import lila.core.net.UserAgent

class UserAgentTrustTest extends munit.FunSuite:

  def susp(ua: String) = UserAgentParser.trust.isSuspicious(UserAgent(ua))

  test("normal"):
    assert:
      !susp:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    assert:
      !susp:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    assert:
      !susp:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0"
    assert:
      !susp:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
    assert:
      !susp:
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    assert:
      !susp:
        "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0"
    assert:
      !susp:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Safari/605.1.15"
    assert:
      !susp:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"
    assert:
      !susp:
        "LM/0.13.13 Android/14 SM-S916N"

  test("susp"):
    assert:
      susp("")
    assert:
      susp("too short")
    assert:
      susp:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
    assert:
      susp("Mozilla/5.0 (X11; U; FreeBSD i386; zh-tw; rv:31.0) Gecko/20100101 Opera/13.0")
    assert:
      susp:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36 OPR/48.0.2685.52"
    assert:
      susp:
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36 Vivaldi/1.2.490.43"
    assert:
      susp("Mozilla/5.0 (Android 6.0.1; Mobile; rv:43.0) Gecko/43.0 Firefox/43.0")
    assert:
      susp:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36"
    assert:
      susp:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6812.83 Safari/537.36 Edg/130.0.2876.112"
    assert:
      susp:
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6787.59 Safari/537.36 Edg/130.0.2762.100"
    assert:
      susp:
        "Mozilla/5.0 Slackware/13.37 (X11; U; Linux x86_64; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/11.0.696.50"
