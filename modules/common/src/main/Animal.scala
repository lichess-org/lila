package lila.common

object Animal {

  val all: Map[String, String] = Map(
    ("Anaconda", "Anaconda"),
    ("Albatross", "Albatross"),
    ("Alligator", "Alligator"),
    ("Bear", "Bear"),
    ("Beetle", "Beetle"),
    ("Bison", "Bison"),
    ("Bobcat", "Bobcat"),
    ("Capybara", "Capybara"),
    ("Cat", "Cat"),
    ("Cheetah", "Cheetah"),
    ("Cobra", "Cobra"),
    ("Cougar", "Cougar"),
    ("Coyote", "Coyote"),
    ("Crocodile", "Crocodile"),
    ("Dingo", "Dingo"),
    ("Dog", "Dog"),
    ("Dolphin", "Dolphin"),
    ("Eagle", "Eagle"),
    ("Elephant", "Elephant"),
    ("Elk", "Elk"),
    ("Giraffe", "Giraffe"),
    ("Gorilla", "Gorilla"),
    ("Horse", "Horse"),
    ("Jaguar", "Jaguar"),
    ("Kangaroo", "Kangaroo"),
    ("Koala", "Koala"),
    ("Komodo dragon", "Komodo_dragon"),
    ("Leopard", "Leopard"),
    ("Lion", "Lion"),
    ("Llama", "Llama"),
    ("Lynx", "Lynx"),
    ("Manatee", "Manatee"),
    ("Monkey", "Monkey"),
    ("Moose", "Moose"),
    ("Orangutan", "Orangutan"),
    ("Orca", "Killer_whale"),
    ("Ostrich", "Ostrich"),
    ("Panda", "Giant_panda"),
    ("Penguin", "Penguin"),
    ("Platypus", "Platypus"),
    ("Raccoon", "Raccoon"),
    ("Rhino", "Rhinoceros"),
    ("Reindeer", "Reindeer"),
    ("Salamander", "Salamander"),
    ("Seal", "Pinniped"),
    ("Shark", "Shark"),
    ("Sloth", "Sloth"),
    ("Snake", "Snake"),
    ("Squid", "Squid"),
    ("Tasmanian devil", "Tasmanian_devil"),
    ("Tiger", "Tiger"),
    ("Turtle", "Turtle"),
    ("Whale", "Whale"),
    ("Wolf", "Wolf"),
    ("Wolverine", "Wolverine")
  )

  private val size = all.size
  private val names: Vector[String] = all.view.map {
    case (k, _) => k
  }.toVector

  def randomName: String = names(scala.util.Random nextInt size)

  def wikiUrl(name: String) =
    all get name map { s =>
      s"https://wikipedia.org/wiki/$s"
    }
}
