package lila.chat

class GarbageTest extends munit.FunSuite:

  test("detect rank-only messages"):
    assert(GarbageDetector("last"))
    assert(GarbageDetector("FIRST"))
    assert(GarbageDetector(" second "))
    assert(GarbageDetector(" third "))

  test("detect number"):
    assert(GarbageDetector("1"))
    assert(GarbageDetector("1st"))
    assert(GarbageDetector("1ts"))
    assert(GarbageDetector("2"))
    assert(GarbageDetector("2nd"))
    assert(GarbageDetector("2rd"))
    assert(GarbageDetector("3rd"))
    assert(GarbageDetector("4th"))
    assert(GarbageDetector("454th"))

  test("detect claim messages"):
    assert(GarbageDetector("I claim 3rd"))
    assert(GarbageDetector("I claim 1st"))
    assert(GarbageDetector("I claim 324245"))
    assert(GarbageDetector("claimed first"))

  test("detect repeated 6-7 and six-seven memes"):
    assert(GarbageDetector("6767"))
    assert(GarbageDetector("6 7 6 7"))
    assert(GarbageDetector("67 67 67"))
    assert(GarbageDetector("6a7a6B7/67"))
    assert(GarbageDetector("sixsevensixseven"))
    assert(GarbageDetector("six seven six seven"))
    assert(GarbageDetector("sixseven sixseven"))
    assert(GarbageDetector("My name is sixseven sixseven"))
    assert(GarbageDetector("I am sixAsevenasixBseven"))

  test("detect repeated characters"):
    assert(GarbageDetector("aaaaa"))
    assert(GarbageDetector("11111"))
    assert(GarbageDetector("Hahahahaha"))
    assert(GarbageDetector("....."))

  test("ignore normal messages"):
    assert(!GarbageDetector("hello everyone"))
    assert(!GarbageDetector("Sindarov is first"))
    assert(!GarbageDetector("Caruna is 2nd"))
    assert(!GarbageDetector("firstly"))
    assert(!GarbageDetector("I claim victory"))
    assert(!GarbageDetector("6 7"))
    assert(!GarbageDetector("six seven"))
    assert(!GarbageDetector("My name is sixseven"))
    assert(!GarbageDetector("six/seven"))
    assert(!GarbageDetector("6/7"))
    assert(!GarbageDetector("Sindarov 6/7"))
    assert(!GarbageDetector("yess"))
    assert(!GarbageDetector("no"))
    assert(!GarbageDetector("siu"))
    assert(!GarbageDetector("..."))
    assert(!GarbageDetector("haha"))
    assert(!GarbageDetector("aaaaaa 111111"))
