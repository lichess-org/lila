package lila.common

import scalatags.Text.all._

import org.specs2.mutable.Specification
import org.specs2.execute.Result

class StringTest extends Specification {

  implicit def netDomain = lila.common.config.NetDomain("lichess.org")

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

  "detect garbage chars" should {
    val dgc = String.distinctGarbageChars _
    "detect 1-byte" in {
      dgc("""ℱ۩۞۩꧁꧂"""") must_== Set('ℱ', '۞', '۩', '꧁', '꧂')
    }
    "preserve languages" in {
      Result.foreach(i18nValidStrings) { txt =>
        dgc(txt) must_== Set.empty
      }
    }
    "detect phonetic extensions" in {
      dgc("ᴀᴛᴏᴍɪᴄ") must_== Set('ᴀ', 'ᴛ', 'ᴏ', 'ᴍ', 'ɪ', 'ᴄ')
    }
  }

  "remove garbage chars" should {
    val rms = String.removeMultibyteSymbols _
    "remove multibyte garbage" in {
      rms("""🕸Trampas en Aperturas🕸: INTRO👋""") must_== "Trampas en Aperturas: INTRO"
      rms(
        """🚌🚎🚐🚑🚒🚓🚕🚗🚙🚚🚛🚜🚲🛴🛵🛺🦼🦽 with new and better !pizzes on lichess.org"""
      ) must_== " with new and better !pizzes on lichess.org"
    }
    "preserve languages" in {
      Result.foreach(i18nValidStrings) { txt =>
        rms(txt) must_== txt
      }
    }
  }

  "normalize" should {
    "keep º and ª" in {
      String.normalize("keep normal text") must_== "keep normal text"
      String.normalize("keep º and ª") must_== "keep º and ª"
    }
  }

  "slugify" should {
    "be safe in html" in {
      String.slugify("hello \" world") must not contain "\""
      String.slugify("<<<") must not contain "<"
    }
  }

  "richText" should {
    "handle nl" in {
      val url = "http://imgur.com/gallery/pMtTE"
      String.html.richText(s"link to $url here\n") must_== raw {
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here<br>"""
      }

      String.html.richText(s"link\n", false) must_== raw("link\n")
    }

    "escape chars" in {
      String.html.richText(s"&") must_== raw("&amp;")
    }

    "keep trailing dash on url" in {
      // We use trailing dashes (-) in our own URL slugs. Always consider them
      // to be part of the URL.
      String.html.richText("a https://example.com/foo--. b") must_== raw {
        """a <a rel="nofollow noopener noreferrer" href="https://example.com/foo--" target="_blank">example.com/foo--</a>. b"""
      }
    }

    "prize regex" should {
      "not find btc in url" in {
        String.looksLikePrize(s"HqVrbTcy") must beFalse
        String.looksLikePrize(s"10btc") must beTrue
        String.looksLikePrize(s"ten btc") must beTrue
      }
    }

    def extractPosts(s: String) = String.forumPostPathRegex.findAllMatchIn(s).toList.map(_.group(1))

    "forum post path regex" should {
      "find forum post path" in {
        extractPosts(
          "[mod](https://lichess.org/@/mod) :gear: Unfeature topic  general-chess-discussion/abc"
        ) must_== List("general-chess-discussion/abc")
        extractPosts("lichess-feedback/test-2") must_== List("lichess-feedback/test-2")
        extractPosts("off-topic-discussion/how-come") must_== List("off-topic-discussion/how-come")
        extractPosts(
          "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes xx team-4-player-chess/chess-getting-boring off-topic-discussion/how-come"
        ) must_== List(
          "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes",
          "team-4-player-chess/chess-getting-boring",
          "off-topic-discussion/how-come"
        )
      }

      "Not find forum post path" in {
        extractPosts("yes/no/maybe") must_== List()
        extractPosts("go/to/some/very/long/path") must_== List()
        extractPosts("Answer me yes/no?") must_== List()
      }
    }
  }

}
