package lila.simulation

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import ornicar.scalalib.Random

import lila.user.User
import lila.tv.Featured

private[simulation] final class Simulator(
    config: Config,
    featured: lila.tv.Featured,
    lobbyEnv: lila.lobby.Env,
    roundEnv: lila.round.Env) extends SimulActor {

  import Simulator._

  val name = "simulator"
  val players = collection.mutable.Set[ActorRef]()
  val watchers = collection.mutable.Set[ActorRef]()

  val playerConfig = PlayerConfig(
    useClock = (config.players < 10) ? true.some | none[Boolean],
    randomClock = (config.players < 10) ? false | true,
    thinkDelay = (config.players < 10) ? 200 | 5000)

  def receive = {

    case Start => {
      println("----------------- start simulation --------------------")
      self ! Spawn.Player
      delay(2 second) {
        self ! Spawn.Watcher
      }
    }

    case Spawn.Player => (players.size < config.players) ! {
      val n = s"P-${nameIterator.next}"
      val player = context.actorOf(Props(mkPlayer(n)), name = n)
      players += player
      player ! Start
      context.system.scheduler.scheduleOnce(0.07 second, self, Spawn.Player)
    }

    case Spawn.Watcher => (watchers.size < config.watchers) ! {
      val n = s"W-${nameIterator.next}"
      val watcher = context.actorOf(Props(mkWatcher(n)), name = n)
      watchers += watcher
      watcher ! Start
      context.system.scheduler.scheduleOnce(0.09 second, self, Spawn.Watcher)
    }
  }

  def mkPlayer(n: String) = new PlayerBot(n, playerConfig, lobbyEnv, roundEnv)
  def mkWatcher(n: String) = new WatcherBot(n, featured, roundEnv)
}

private[simulation] object Simulator {

  val names = scala.util.Random shuffle List("Abbott", "Abrams", "Adams", "Adcock", "Adkins", "Adler", "Albright", "Aldridge", "Alexander", "Alford", "Allen", "Allison", "Allred", "Alston", "Anderson", "Andrews", "Anthony", "Archer", "Armstrong", "Arnold", "Arthur", "Ashley", "Atkins", "Atkinson", "Austin", "Avery", "Aycock", "Ayers", "Bailey", "Baird", "Baker", "Baldwin", "Ball", "Ballard", "Banks", "Barbee", "Barber", "Barbour", "Barefoot", "Barker", "Barnes", "Barnett", "Barr", "Barrett", "Barry", "Bartlett", "Barton", "Bass", "Batchelor", "Bates", "Bauer", "Baxter", "Beach", "Bean", "Beard", "Beasley", "Beatty", "Beck", "Becker", "Bell", "Bender", "Bennett", "Benson", "Benton", "Berg", "Berger", "Berman", "Bernstein", "Berry", "Best", "Bishop", "Black", "Blackburn", "Blackwell", "Blair", "Blake", "Blalock", "Blanchard", "Bland", "Blanton", "Block", "Bloom", "Blum", "Bolton", "Bond", "Boone", "Booth", "Boswell", "Bowden", "Bowen", "Bowers", "Bowles", "Bowling", "Bowman", "Boyd", "Boyer", "Boyette", "Boykin", "Boyle", "Bradford", "Bradley", "Bradshaw", "Brady", "Branch", "Brandon", "Brandt", "Brantley", "Braswell", "Braun", "Bray", "Brennan", "Brewer", "Bridges", "Briggs", "Britt", "Brock", "Brooks", "Brown", "Browning", "Bruce", "Bryan", "Bryant", "Buchanan", "Buck", "Buckley", "Bullard", "Bullock", "Bunn", "Burch", "Burgess", "Burke", "Burnett", "Burnette", "Burns", "Burton", "Bush", "Butler", "Byers", "Bynum", "Byrd", "Byrne", "Cain", "Caldwell", "Callahan", "Cameron", "Camp", "Campbell", "Cannon", "Capps", "Carey", "Carlson", "Carlton", "Carpenter", "Carr", "Carroll", "Carson", "Carter", "Carver", "Case", "Casey", "Cash", "Cassidy", "Cates", "Chambers", "Chan", "Chandler", "Chang", "Chapman", "Chappell", "Chase", "Cheek", "Chen", "Cheng", "Cherry", "Cho", "Choi", "Christensen", "Christian", "Chu", "Chung", "Church", "Clapp", "Clark", "Clarke", "Clayton", "Clements", "Cline", "Coates", "Cobb", "Coble", "Cochran", "Cohen", "Cole", "Coleman", "Coley", "Collier", "Collins", "Combs", "Conner", "Connolly", "Connor", "Conrad", "Conway", "Cook", "Cooke", "Cooper", "Copeland", "Corbett", "Covington", "Cowan", "Cox", "Crabtree", "Craft", "Craig", "Crane", "Craven", "Crawford", "Creech", "Crews", "Cross", "Crowder", "Crowell", "Cummings", "Cunningham", "Currie", "Currin", "Curry", "Curtis", "Dale", "Dalton", "Daly", "Daniel", "Daniels", "Davenport", "Davidson", "Davies", "Davis", "Dawson", "Day", "Deal", "Dean", "Decker", "Dennis", "Denton", "Desai", "Diaz", "Dickens", "Dickerson", "Dickinson", "Dickson", "Dillon", "Dixon", "Dodson", "Dolan", "Donnelly", "Donovan", "Dorsey", "Dougherty", "Douglas", "Doyle", "Drake", "Dudley", "Duffy", "Duke", "Duncan", "Dunlap", "Dunn", "Durham", "Dyer", "Eason", "Eaton", "Edwards", "Ellington", "Elliott", "Ellis", "Elmore", "English", "Ennis", "Epstein", "Erickson", "Evans", "Everett", "Faircloth", "Farmer", "Farrell", "Faulkner", "Feldman", "Ferguson", "Fernandez", "Ferrell", "Field", "Fields", "Finch", "Fink", "Finley", "Fischer", "Fisher", "Fitzgerald", "Fitzpatrick", "Fleming", "Fletcher", "Flowers", "Floyd", "Flynn", "Foley", "Forbes", "Ford", "Forrest", "Foster", "Fowler", "Fox", "Francis", "Frank", "Franklin", "Frazier", "Frederick", "Freedman", "Freeman", "French", "Friedman", "Frost", "Frye", "Fuller", "Gallagher", "Galloway", "Garcia", "Gardner", "Garner", "Garrett", "Garrison", "Gates", "Gay", "Gentry", "George", "Gibbons", "Gibbs", "Gibson", "Gilbert", "Giles", "Gill", "Gillespie", "Gilliam", "Glass", "Glenn", "Glover", "Godfrey", "Godwin", "Gold", "Goldberg", "Golden", "Goldman", "Goldstein", "Gonzalez", "Goodman", "Goodwin", "Gordon", "Gorman", "Gould", "Grady", "Graham", "Grant", "Graves", "Gray", "Green", "Greenberg", "Greene", "Greer", "Gregory", "Griffin", "Griffith", "Grimes", "Gross", "Grossman", "Gunter", "Gupta", "Guthrie", "Haas", "Hahn", "Hale", "Hall", "Hamilton", "Hammond", "Hampton", "Hamrick", "Han", "Hancock", "Hanna", "Hansen", "Hanson", "Hardin", "Harding", "Hardison", "Hardy", "Harmon", "Harper", "Harrell", "Harrington", "Harris", "Harrison", "Hart", "Hartman", "Harvey", "Hatcher", "Hauser", "Hawkins", "Hawley", "Hayes", "Haynes", "Heath", "Hedrick", "Heller", "Helms", "Henderson", "Hendricks", "Hendrix", "Henry", "Hensley", "Henson", "Herbert", "Herman", "Hernandez", "Herndon", "Herring", "Hess", "Hester", "Hewitt", "Hicks", "Higgins", "High", "Hill", "Hines", "Hinson", "Hinton", "Hirsch", "Ho", "Hobbs", "Hodge", "Hodges", "Hoffman", "Hogan", "Holden", "Holder", "Holland", "Holloway", "Holmes", "Holt", "Honeycutt", "Hong", "Hood", "Hoover", "Hopkins", "Horn", "Horne", "Horner", "Horowitz", "Horton", "House", "Houston", "Howard", "Howe", "Howell", "Hoyle", "Hsu", "Hu", "Huang", "Hubbard", "Hudson", "Huff", "Huffman", "Hughes", "Hull", "Humphrey", "Hunt", "Hunter", "Hurley", "Hurst", "Hutchinson", "Hwang", "Ingram", "Ivey", "Jackson", "Jacobs", "Jacobson", "Jain", "James", "Jenkins", "Jennings", "Jensen", "Jernigan", "Jiang", "Johnson", "Johnston", "Jones", "Jordan", "Joseph", "Joyce", "Joyner", "Justice", "Kahn", "Kane", "Kang", "Kaplan", "Katz", "Kaufman", "Kay", "Kearney", "Keith", "Keller", "Kelley", "Kelly", "Kemp", "Kendall", "Kennedy", "Kenney", "Kent", "Kern", "Kerr", "Kessler", "Khan", "Kidd", "Kim", "King", "Kinney", "Kirby", "Kirk", "Kirkland", "Klein", "Knight", "Knowles", "Knox", "Koch", "Kramer", "Kuhn", "Kumar", "Lam", "Lamb", "Lambert", "Lamm", "Lancaster", "Lane", "Lang", "Langley", "Langston", "Lanier", "Larson", "Lassiter", "Law", "Lawrence", "Lawson", "Leach", "Lee", "Lehman", "Leonard", "Lester", "Levin", "Levine", "Levy", "Lewis", "Li", "Lim", "Lin", "Lindsay", "Lindsey", "Link", "Little", "Liu", "Livingston", "Lloyd", "Locklear", "Logan", "Long", "Lopez", "Love", "Lowe", "Lowry", "Lu", "Lucas", "Lutz", "Lynch", "Lynn", "Lyon", "Lyons", "MacDonald", "Mack", "Malone", "Mangum", "Mann", "Manning", "Marcus", "Marks", "Marsh", "Marshall", "Martin", "Martinez", "Mason", "Massey", "Mathews", "Matthews", "Maxwell", "May", "Mayer", "Maynard", "Mayo", "McAllister", "McBride", "McCall", "McCarthy", "McClure", "McConnell", "McCormick", "McCoy", "McCullough", "McDaniel", "McDonald", "McDowell", "McFarland", "McGee", "McGuire", "McIntosh", "McIntyre", "McKay", "McKee", "McKenna", "McKenzie", "McKinney", "McKnight", "McLamb", "McLaughlin", "McLean", "McLeod", "McMahon", "McMillan", "McNamara", "McNeill", "McPherson", "Meadows", "Medlin", "Melton", "Melvin", "Mercer", "Merrill", "Merritt", "Meyer", "Meyers", "Michael", "Middleton", "Miles", "Miller", "Mills", "Mitchell", "Monroe", "Montgomery", "Moody", "Moon", "Moore", "Moran", "Morgan", "Morris", "Morrison", "Morrow", "Morse", "Morton", "Moser", "Moss", "Mueller", "Mullen", "Mullins", "Murphy", "Murray", "Myers", "Nance", "Nash", "Neal", "Nelson", "Newell", "Newman", "Newton", "Nguyen", "Nichols", "Nicholson", "Nixon", "Noble", "Nolan", "Norman", "Norris", "Norton", "O'Brien", "O'Connell", "O'Connor", "O'Donnell", "O'Neal", "O'Neill", "Oakley", "Odom", "Oh", "Oliver", "Olsen", "Olson", "Orr", "Osborne", "Owen", "Owens", "Pace", "Padgett", "Page", "Palmer", "Pappas", "Park", "Parker", "Parks", "Parrish", "Parrott", "Parsons", "Pate", "Patel", "Patrick", "Patterson", "Patton", "Paul", "Payne", "Peacock", "Pearce", "Pearson", "Peck", "Peele", "Pennington", "Perez", "Perkins", "Perry", "Peters", "Petersen", "Peterson", "Petty", "Phelps", "Phillips", "Pickett", "Pierce", "Pittman", "Pitts", "Poe", "Pollard", "Pollock", "Poole", "Pope", "Porter", "Potter", "Powell", "Powers", "Pratt", "Preston", "Price", "Pridgen", "Prince", "Pritchard", "Proctor", "Pruitt", "Puckett", "Pugh", "Quinn", "Ramsey", "Randall", "Rankin", "Rao", "Ray", "Raynor", "Reddy", "Reed", "Reese", "Reeves", "Reid", "Reilly", "Reynolds", "Rhodes", "Rice", "Rich", "Richards", "Richardson", "Richmond", "Riddle", "Riggs", "Riley", "Ritchie", "Rivera", "Roach", "Robbins", "Roberson", "Roberts", "Robertson", "Robinson", "Rodgers", "Rodriguez", "Rogers", "Rollins", "Rose", "Rosen", "Rosenberg", "Rosenthal", "Ross", "Roth", "Rouse", "Rowe", "Rowland", "Roy", "Rubin", "Russell", "Ryan", "Sanchez", "Sanders", "Sanford", "Saunders", "Savage", "Sawyer", "Scarborough", "Schaefer", "Schmidt", "Schneider", "Schroeder", "Schultz", "Schwartz", "Schwarz", "Scott", "Sellers", "Shaffer", "Shah", "Shannon", "Shapiro", "Sharma", "Sharp", "Sharpe", "Shaw", "Shea", "Shelton", "Shepherd", "Sherman", "Sherrill", "Shields", "Shore", "Short", "Siegel", "Sigmon", "Silver", "Silverman", "Simmons", "Simon", "Simpson", "Sims", "Sinclair", "Singer", "Singh", "Singleton", "Skinner", "Sloan", "Small", "Smith", "Snow", "Snyder", "Solomon", "Song", "Sparks", "Spears", "Spence", "Spencer", "Spivey", "Stafford", "Stallings", "Stanley", "Stanton", "Stark", "Starr", "Steele", "Stein", "Stephens", "Stephenson", "Stern", "Stevens", "Stevenson", "Stewart", "Stokes", "Stone", "Stout", "Strauss", "Strickland", "Stroud", "Stuart", "Sullivan", "Summers", "Sumner", "Sun", "Sutherland", "Sutton", "Swain", "Swanson", "Sweeney", "Sykes", "Talley", "Tan", "Tanner", "Tate", "Taylor", "Teague", "Terrell", "Terry", "Thomas", "Thompson", "Thomson", "Thornton", "Tilley", "Todd", "Townsend", "Tucker", "Turner", "Tuttle", "Tyler", "Tyson", "Underwood", "Upchurch", "Vaughan", "Vaughn", "Vick", "Vincent", "Vogel", "Wade", "Wagner", "Walker", "Wall", "Wallace", "Waller", "Walsh", "Walter", "Walters", "Walton", "Wang", "Ward", "Warner", "Warren", "Washington", "Waters", "Watkins", "Watson", "Watts", "Weaver", "Webb", "Weber", "Webster", "Weeks", "Weiner", "Weinstein", "Weiss", "Welch", "Wells", "Welsh", "Werner", "West", "Wheeler", "Whitaker", "White", "Whitehead", "Whitfield", "Whitley", "Wiggins", "Wilcox", "Wilder", "Wiley", "Wilkerson", "Wilkins", "Wilkinson", "Willard", "Williams", "Williamson", "Williford", "Willis", "Wilson", "Winstead", "Winters", "Wise", "Wolf", "Wolfe", "Womble", "Wong", "Wood", "Woodard", "Woodruff", "Woods", "Woodward", "Wooten", "Wrenn", "Wright", "Wu", "Wyatt", "Xu", "Yang", "Yates", "York", "Young", "Yu", "Zhang", "Zhao", "Zhou", "Zhu", "Zimmerman")
  val nameIterator = Stream.continually(names.toStream).flatten.toIterator

  case object Start
  case object Spawn {
    case object Player
    case object Watcher
  }
}
