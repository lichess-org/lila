package lila.common

import scalatags.Text.all.*

class StringTest extends munit.FunSuite:

  given config.NetDomain = config.NetDomain("lichess.org")

  val i18nValidStrings = List(
    """éâòöÌÒÒçÇ""",
    """صارف اپنا نام تبدیل کریں۔ یہ صرف ایک دفعہ ہو سکتا ہے اور صرف انگریزی حروف چھوٹے یا بڑے کرنے کی اجازت ہے۔.""",
    """ユーザー名を変更します。これは一回限りで、行なえるのは大文字・小文字の変更だけです。""",
    """ਤੁਹਾਡੇ ਵਿਰੋਧੀ ਨੇ ਖੇਡ ਨੂੰ ਛੱਡ ਦਿੱਤਾ. ਤੁਸੀਂ ਜਿੱਤ ਦਾ ਦਾਅਵਾ ਕਰ ਸਕਦੇ ਹੋ, ਖੇਡ ਨੂੰ ਡਰਾਅ ਕਹਿ ਸਕਦੇ ਹੋ, ਜਾਂ ਇੰਤਜ਼ਾਰ ਕਰ ਸਕਦੇ ਹੋ.""",
    """మీ ప్రత్యర్థి బహుశా ఆట విడిచి వెళ్లిపోయారేమో. మీరు కాసేపు ఆగి చూడవచ్చు, లేదా గెలుపోటములు సమానంగా పంచుకోవచ్చు, లేదా విజయం ప్రకటించుకోవచ్చు.""",
    """ผู้เล่นที่เป็นคอมพิวเตอร์หรือใช้คอมพิวเตอร์ช่วย จะไม่ได้รับอนุญาตให้เล่น  โปรดอย่าใช้การช่วยเหลือจากตัวช่วยเล่นหมากรุก, ฐานข้อมูล หรือบุคคลอื่น ในขณะเล่น""",
    """သင့်ရဲ့ပြိုင်ဘက် ဂိမ်းမှထွက်ခွာသွားပါပြီ. လက်ရှိပွဲကို အနိုင်ယူမည်လား သရေကျပေးမည်လား သို့မဟုတ် စောင့်ဆိုင််းဦးမလား.""",
    """יריבך עזב את המשחק. באפשרותך לכפות פרישה, להכריז על תיקו או להמתין לו."""
  )

  val rms = String.removeMultibyteSymbols _
  test("remove multibyte garbage") {
    assertEquals(rms("""🕸Trampas en Aperturas🕸: INTRO👋"""), "Trampas en Aperturas: INTRO")
    assertEquals(
      rms("""🚌🚎🚐🚑🚒🚓🚕🚗🚙🚚🚛🚜🚲🛴🛵🛺🦼🦽 with new and better !pizzes on lichess.org"""),
      " with new and better !pizzes on lichess.org"
    )
    assertEquals(rms("🥹"), "")
    assertEquals(rms("🥹🥹🥹 xxx 🥹"), " xxx ")
  }
  test("preserve languages") {
    i18nValidStrings.foreach: txt =>
      assertEquals(rms(txt), txt)
  }
  test("preserve half point") {
    assertEquals(rms("½"), "½")
  }

  test("remove garbage chars") {
    assertEquals(String.removeGarbageChars("""ℱ۩۞۩꧁꧂"""), "")
    assertEquals(String.removeGarbageChars("""ᴀᴛᴏᴍɪᴄ"""), "")
    assertEquals(String.removeGarbageChars("""af éâòöÌÒÒçÇℱ۩۞۩꧁꧂"  صار"""), """af éâòöÌÒÒçÇ"  صار""")
    i18nValidStrings.foreach: txt =>
      assertEquals(String.removeGarbageChars(txt), txt)
  }

  test("normalize keep º and ª") {
    assertEquals(String.normalize("keep normal text"), "keep normal text")
    assertEquals(String.normalize("keep º and ª"), "keep º and ª")
  }
  test("normalize preserve half point") {
    assertEquals(String.normalize("½"), "½")
  }

  test("slugify be safe >> html") {
    assert(!String.slugify("hello \" world").contains("\""))
    assert(!String.slugify("<<<").contains("<"))
  }

  test("richText handle nl") {
    val url = "http://imgur.com/gallery/pMtTE"
    assertEquals(
      String.html.richText(s"link to $url here\n"),
      raw:
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here<br>"""
    )

    assertEquals(String.html.richText(s"link\n", false), raw("link\n"))
  }

  test("richText escape chars") {
    assertEquals(String.html.richText(s"&"), raw("&amp;"))
  }

  test("richText keep trailing dash on url") {
    // We use trailing dashes (-) >> our own URL slugs. Always consider them
    // to be part of the URL.
    assertEquals(
      String.html.richText("a https://example.com/foo--. b"),
      raw:
        """a <a rel="nofollow noopener noreferrer" href="https://example.com/foo--" target="_blank">example.com/foo--</a>. b"""
    )
  }

  def extractPosts(s: String) = String.forumPostPathRegex.findAllMatchIn(s).toList.map(_.group(1))

  test("richText forum post path regex find forum post path") {
    assertEquals(
      extractPosts("[mod](https://lichess.org/@/mod) :gear: Unfeature topic  general-chess-discussion/abc"),
      List("general-chess-discussion/abc")
    )
    assertEquals(extractPosts("lichess-feedback/test-2"), List("lichess-feedback/test-2"))
    assertEquals(extractPosts("off-topic-discussion/how-come"), List("off-topic-discussion/how-come"))
    assertEquals(
      extractPosts(
        "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes xx team-4-player-chess/chess-getting-boring off-topic-discussion/how-come"
      ),
      List(
        "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes",
        "team-4-player-chess/chess-getting-boring",
        "off-topic-discussion/how-come"
      )
    )
  }

  test("richText Not find forum post path") {
    assertEquals(extractPosts("yes/no/maybe"), List())
    assertEquals(extractPosts("go/to/some/very/long/path"), List())
    assertEquals(extractPosts("Answer me yes/no?"), List())
  }

  test("noShouting") {
    assertEquals(String.noShouting("HELLO SIR"), "hello sir")
    assertEquals(String.noShouting("1. Nf3 O-O-O#"), "1. Nf3 O-O-O#")
  }
