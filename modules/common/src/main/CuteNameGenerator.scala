package lila.common

import ornicar.scalalib.ThreadLocalRandom

// children friendly names only
// this is used by /class
object CuteNameGenerator:

  type Language = String
  val defaultLang: Language = "en"

  private type Combinations = Vector[List[Vector[String]]]

  def make(maxSize: Int = 20, triesLeft: Int = 100, lang: Language = defaultLang): Option[UserName] =
    val name = makeForSure(lang)
    if name.value.sizeIs <= maxSize then name.some
    else if triesLeft <= 0 then none
    else make(maxSize, triesLeft - 1)

  def makeForSure(lang: Language = defaultLang): UserName = UserName:
    anyOf(getCombinations(lang)).map(anyOf).mkString

  def fromSeed(seed: Int, lang: Language = defaultLang): UserName = UserName:
    seedOf(seed)(getCombinations(lang)).map(seedOf(seed)).mkString

  private def anyOf[A](vec: Vector[A]): A =
    vec(ThreadLocalRandom.nextInt(vec.size))

  private def seedOf[A](seed: Int)(vec: Vector[A]): A =
    vec(Math.abs(seed) % vec.size)

  private def getCombinations(lang: Language): Combinations =
    combinations.getOrElse(lang, defaultCombinations)

  private lazy val defaultCombinations =
    import CuteNameDicts.en.*
    Vector(
      List(colors ++ adjectives, animals ++ pieces ++ jobs),
      List(colors, adjectives, animals ++ pieces ++ jobs),
      List(colors ++ adjectives, adjectives, animals ++ pieces ++ jobs),
      List(colors ++ adjectives, jobs, animals ++ pieces)
    )

  private lazy val combinations: Map[Language, Combinations] = Map(
    defaultLang -> defaultCombinations,
    "fr" -> {
      import CuteNameDicts.fr.*
      Vector(
        List(colors)
      )
    }
  )
