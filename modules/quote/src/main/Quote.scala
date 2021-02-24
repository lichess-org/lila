package lila.quote

import scala.util.Random
import play.api.libs.json._

final class Quote(val text: String, val author: String)

object Quote {

  def one = all(Random.nextInt(all.size))

  def one(seed: String) = all(new Random(seed.hashCode).nextInt(all.size))

  // courtesy of http://www.chess-poster.com/english/notes_and_facts/chess_quotes.htm
  // and other various sources
  val all = Vector(
    new Quote("When you see a good move, look for a better one.", "Emanuel Lasker"),
    new Quote("Nothing excites jaded Grandmasters more than a theoretical novelty.", "Dominic Lawson"),
    new Quote("The Pin is mightier than the sword.", "Fred Reinfeld"),
    new Quote(
      "We cannot resist the fascination of sacrifice, since a passion for sacrifices is part of a chess player’s nature.",
      "Rudolf Spielman"
    ),
    new Quote("All I want to do, ever, is just play chess.", "Bobby Fischer"),
    new Quote(
      "A win by an unsound combination, however showy, fills me with artistic horror.",
      "Wilhelm Steinitz"
    ),
    new Quote(
      "The chessboard is the world, the pieces are the phenomena of the Universe, the rules of the game are what we call the laws of Nature and the player on the other side is hidden from us.",
      "Thomas Huxley"
    ),
    new Quote(
      "Adequate compensation for a sacrifice is having a sound combination leading to a winning position; adequate compensation for a blunder is having your opponent snatch defeat from the jaws of victory.",
      "Bruce A. Moon"
    ),
    new Quote("Strategy requires thought, tactics require observation.", "Max Euwe"),
    new Quote("I don’t believe in psychology. I believe in good moves.", "Bobby Fischer"),
    new Quote(
      "Modern chess is too much concerned with things like pawn structure. Forget it, Checkmate ends the game.",
      "Nigel Short"
    ),
    new Quote(
      "Life is a kind of chess, with struggle, competition, good and ill events.",
      "Benjamin Franklin"
    ),
    new Quote("Even the laziest king flees wildly in the face of a double check!", "Aaron Nimzowitsch"),
    new Quote(
      "Combinations have always been the most intriguing aspect of chess.  The masters look for them, the public applauds them, the critics praise them. It is because combinations are possible that chess is more than a lifeless mathematical exercise. They are the poetry of the game; they are to chess what melody is to music. They represent the triumph of mind over matter.",
      "Reuben Fine"
    ),
    new Quote("Chess is a fairy tale of 1001 blunders.", "Savielly Tartakower"),
    new Quote(
      "Chess is no whit inferior to the violin, and we have a large number of professional violinists.",
      "Mikhail Botvinnik"
    ),
    new Quote("Only the player with the initiative has the right to attack.", "Wilhelm Steinitz"),
    new Quote(
      "The winner of the game is the player who makes the next-to-last mistake.",
      "Savielly Tartakover"
    ),
    new Quote(
      "Your body has to be in top condition. Your chess deteriorates as your body does. You can’t separate body from mind.",
      "Bobby Fischer"
    ),
    new Quote(
      "Of chess it has been said that life is not long enough for it, but that is the fault of life, not chess.",
      "William Ewart Napier"
    ),
    new Quote(
      "I have added these principles to the law: get the Knights into action before both Bishops are developed.",
      "Emanuel Lasker"
    ),
    new Quote("Life is like a game of chess, changing with each move.", "Chinese proverb"),
    new Quote("You cannot play at chess if you are kind-hearted.", "French proverb"),
    new Quote(
      "Its just you and your opponent at the board and you’re trying to prove something.",
      "Bobby Fischer"
    ),
    new Quote(
      "It is the aim of the modern school, not to treat every position according to one general law, but according to the principle inherent in the position.",
      "Richard Reti"
    ),
    new Quote("The pawns are the soul of the game.", "Francois Andre Danican Philidor"),
    new Quote(
      "In order to improve your game, you must study the endgame before everything else, for whereas the endings can be studied and mastered by themselves, the middle game and the opening must be studied in relation to the endgame.",
      "Jose Raul Capablanca"
    ),
    new Quote("Without error there can be no brilliancy.", "Emanuel Lasker"),
    new Quote("Chess is like war on a board.", "Bobby Fischer"),
    new Quote("Chess is played with the mind and not with the hands!", "Renaud and Kahn"),
    new Quote("Chess is mental torture.", "Garry Kasparov"),
    new Quote("Many have become chess masters, no one has become the master of chess.", "Siegbert Tarrasch"),
    new Quote(
      "The most important feature of the chess position is the activity of the pieces. This is absolutely fundamental in all phases of the game: Opening, Middlegame and especially Endgame. The primary constraint on a piece’s activity is the pawn structure.",
      "Michael Stean"
    ),
    new Quote(
      "You have to have the fighting spirit. You have to force moves and take chances.",
      "Bobby Fischer"
    ),
    new Quote(
      "Could we look into the head of a chess player, we should see there a whole world of feelings, images, ideas, emotion and passion.",
      "Alfred Binet"
    ),
    new Quote("Openings teach you openings. Endgames teach you chess!", "Stephan Gerzadowicz"),
    new Quote("My style is somewhere between that of Tal and Petrosian.", "Reshevsky"),
    new Quote(
      "Play the opening like a book, the middle game like a magician, and the endgame like a machine.",
      "Spielmann"
    ),
    new Quote(
      "That’s what chess is all about. One day you give your opponent a lesson, the next day he gives you one.",
      "Bobby Fischer"
    ),
    new Quote("Some part of a mistake is always correct.", "Savielly Tartakover"),
    new Quote("Methodical thinking is of more use in chess than inspiration.", "C. J. S. Purdy"),
    new Quote("When in doubt... play chess!", "Tevis"),
    new Quote(
      "Who is your opponent tonight, tonight I am playing against the black pieces.",
      "Akiba Rubinstein"
    ),
    new Quote("I like the moment when I break a man’s ego.", "Bobby Fischer"),
    new Quote("Excellence at chess is one mark of a scheming mind.", "Sir Arthur Conan Doyle"),
    new Quote("A bad day of chess is better than any good day at work.", "Anonymous"),
    new Quote("Chess is the art of analysis.", "Mikhail Botvinnik"),
    new Quote("The mistakes are there, waiting to be made.", "Savielly Tartakower"),
    new Quote(
      "After black’s reply to 1.e4 with 1..e5, leaves him always trying to get into the game.",
      "Howard Staunton"
    ),
    new Quote("A player surprised is half beaten.", "Proverb"),
    new Quote(
      "A passed pawn increases in strength as the number of pieces on the board diminishes.",
      "Capablanca"
    ),
    new Quote("The essence of chess is thinking about what chess is.", "David Bronstein"),
    new Quote("I am the best player in the world and I am here to prove it.", "Bobby Fischer"),
    new Quote(
      "Chess is a forcing house where the fruits of character can ripen more fully than in life.",
      "Edward Morgan Foster"
    ),
    new Quote(
      "Half the variations which are calculated in a tournament game turn out to be completely superfluous. Unfortunately, no one knows in advance which half.",
      "Jan Timman"
    ),
    new Quote("Good positions don’t win games, good moves do.", "Gerald Abrahams"),
    new Quote(
      "If I win a tournament, I win it by myself.  I do the playing. Nobody helps me.",
      "Bobby Fischer"
    ),
    new Quote("What would chess be without silly mistakes?", "Kurt Richter"),
    new Quote("Before the endgame, the Gods have placed the middle game.", "Siegbert Tarrasch"),
    new Quote("Chess was Capablanca’s mother tongue.", "Reti"),
    new Quote(
      "Alekhine is a poet who creates a work of art out of something that would hardly inspire another man to send home a picture post card.",
      "Max Euwe"
    ),
    new Quote(
      "During a chess competition a chess master should be a combination of a beast of prey and a monk.",
      "Alexander Alekhine"
    ),
    new Quote("No one ever won a game by resigning.", "Saviely Tartakower"),
    new Quote("The defensive power of a pinned piece is only imaginary.", "Aaron Nimzovich"),
    new Quote("When the chess game is over, the pawn and the king go back to the same box.", "Irish saying"),
    new Quote(
      "A strong memory, concentration, imagination, and a strong will is required to become a great chess player.",
      "Bobby Fischer"
    ),
    new Quote("Every chess master was once a beginner.", "Chernev"),
    new Quote(
      "One doesn’t have to play well, it’s enough to play better than your opponent.",
      "Siegbert Tarrasch"
    ),
    new Quote("Chess is above all, a fight!", "Emanuel Lasker"),
    new Quote("Discovered check is the dive bomber of the chessboard.", "Reuben Fine"),
    new Quote(
      "I know people who have all the will in the world, but still can’t play good chess.",
      "Bobby Fischer"
    ),
    new Quote(
      "A chess game is a dialogue, a conversation between a player and his opponent. Each move by the opponent may contain threats or be a blunder, but a player cannot defend against threats or take advantage of blunders if he does not first ask himself: What is my opponent planning after each move?",
      "Bruce A. Moon"
    ),
    new Quote("The hardest game to win is a won game.", "Emanuel Lasker"),
    new Quote("The most powerful weapon in chess is to have the next move.", "David Bronstein"),
    new Quote("He who fears an isolated queen’s pawn should give up chess.", "Siegbert Tarrasch"),
    new Quote("Different people feel differently about resigning.", "Bobby Fischer"),
    new Quote("Chess is not like life... it has rules!", "Mark Pasternak"),
    new Quote("It’s always better to sacrifice your opponent’s men.", "Savielly Tartakover"),
    new Quote("To avoid losing a piece, many a person has lost the game.", "Savielly Tartakover"),
    new Quote("All that matters on the chessboard is good moves.", "Bobby Fischer"),
    new Quote("Help your pieces so they can help you.", "Paul Morphy"),
    new Quote("In a gambit you give up a pawn for the sake of getting a lost game.", "Samuel Standige Boden"),
    new Quote("It is not enough to be a good player... you must also play well.", "Siegbert Tarrasch"),
    new Quote("A sacrifice is best refuted by accepting it.", "Wilhelm Steinitz"),
    new Quote("Tactics flow from a superior position.", "Bobby Fischer"),
    new Quote(
      "Later, I began to succeed in decisive games. Perhaps because I realized a very simple truth: not only was I worried, but also my opponent.",
      "Mikhail Tal"
    ),
    new Quote("Chess is life.", "Bobby Fischer"),
    new Quote("Chess is a beautiful mistress.", "Bent Larsen"),
    new Quote("Some sacrifices are sound; the rest are mine.", "Mikhail Tal"),
    new Quote("Best by test: 1. e4.", "Bobby Fischer"),
    new Quote("A bad plan is better than none at all.", "Frank Marshall"),
    new Quote(
      "Chess books should be used as we use glasses: to assist the sight, although some players make use of them as if they thought they conferred sight.",
      "Jose Raul Capablanca"
    ),
    new Quote("There are two types of sacrifices: correct ones and mine.", "Mikhail Tal"),
    new Quote("Morphy was probably the greatest genius of them all.", "Bobby Fischer"),
    new Quote(
      "My opponents make good moves too. Sometimes I don’t take these things into consideration.",
      "Bobby Fischer"
    ),
    new Quote(
      "The combination player thinks forward; he starts from the given position, and tries the forceful moves in his mind.",
      "Emanuel Lasker"
    ),
    new Quote(
      "A chess game is divided into three stages: the first, when you hope you have the advantage, the second when you believe you have an advantage, and the third... when you know you’re going to lose!",
      "Savielly Tartakower"
    ),
    new Quote("Chess demands total concentration.", "Bobby Fischer"),
    new Quote("Chess, like love, like music, has the power to make people happy.", "Siegbert Tarrasch"),
    new Quote("All my games are real.", "Bobby Fischer"),
    new Quote("Chess is everything: art, science and sport.", "Anatoly Karpov"),
    new Quote("Chess is the art which expresses the science of logic.", "Mikhail Botvinnik"),
    new Quote("Not all artists are chess players, but all chess players are artists.", "Marcel Duchamp"),
    new Quote("Chess is imagination.", "David Bronstein"),
    new Quote(
      "Chess is thirty to forty percent psychology. You don’t have this when you play a computer. I can’t confuse it.",
      "Judith Polgar"
    ),
    new Quote("On the chessboard, lies and hypocrisy do not survive long.", "Emanuel Lasker"),
    new Quote("Chess is war over the board. The object is to crush the opponents mind.", "Bobby Fischer"),
    new Quote(
      "The passed pawn is a criminal, who should be kept under lock and key.  Mild measures, such as police surveillance, are not sufficient.",
      "Aaron Nimzovich"
    ),
    new Quote(
      "Chess holds its master in its own bonds, shackling the mind and brain so that the inner freedom of the very strongest must suffer.",
      "Albert Einstein"
    ),
    new Quote(
      "Human affairs are like a chess game: only those who do not take it seriously can be called good players.",
      "Hung Tzu Ch’eng"
    ),
    new Quote("The blunders are all there on the board, waiting to be made.", "Savielly Tartakover"),
    new Quote(
      "Via the squares on the chessboard, the Indians explain the movement of time and the age, the higher influences which control the world and the ties which link chess with the human soul.",
      "Al-Masudi"
    ),
    new Quote("It is no time to be playing chess when the house is on fire.", "Italian proverb"),
    new Quote(
      "You sit at the board and suddenly your heart leaps. Your hand trembles to pick up the piece and move it.  But what chess teaches you is that you must sit there calmly and think about whether its really a good idea and whether there are other better ideas.",
      "Stanley Kubrick"
    ),
    new Quote(
      "Daring ideas are like chess men moved forward. They may be beaten, but they may start a winning game.",
      "Johann Wolfgang von Goethe"
    ),
    new Quote(
      "Of all my Russian books, The Defense contains and diffuses the greatest ’warmth’ which may seem odd seeing how supremely abstract chess is supposed to be.",
      "Vladimir Nabokov"
    ),
    new Quote(
      "For surely of all the drugs in the world, chess must be the most permanently pleasurable.",
      "Assiac"
    ),
    new Quote(
      "A thorough understanding of the typical mating continuations makes the most complicated sacrificial combinations leading up to them not only not difficult, but almost a matter of course.",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "Chess problems demand from the composer the same virtues that characterize all worthwhile art: originality, invention, conciseness, harmony, complexity, and splendid insincerity.",
      "Vladimir Nabokov"
    ),
    new Quote(
      "Personally, I rather look forward to a computer program winning the world chess Championship. Humanity needs a lesson in humility.",
      "Richard Dawkins"
    ),
    new Quote(
      "The boy (then a 12 year old boy named Anatoly Karpov) doesn’t have a clue about chess, and there’s no future at all for him in this profession.",
      "Mikhail Botvinnik"
    ),
    new Quote("As one by one I mowed them down, my superiority soon became apparent.", "Jose Capablanca"),
    new Quote(
      "Though most people love to look at the games of the great attacking masters, some of the most successful players in history have been the quiet positional players. They slowly grind you down by taking away your space, tying up your pieces, and leaving you with virtually nothing to do!",
      "Yasser Seirawan"
    ),
    new Quote("Chess is ruthless; you’ve got to be prepared to kill people.", "Nigel Short"),
    new Quote(
      "There must have been a time when men were demigods, or they could not have invented chess.",
      "Gustav Schenk"
    ),
    new Quote("Chess is really ninety nine percent calculation.", "Soltis"),
    new Quote("Chess is the gymnasium of the mind.", "Blaise Pascal"),
    new Quote(
      "The game of chess is not merely an idle amusement; several very valuable qualities of the mind are to be acquired and strengthened by it, so as to become habits ready on all occasions; for life is a kind of chess.",
      "Benjamin Franklin"
    ),
    new Quote("Winning isn’t everything... but losing is nothing.", "Mednis"),
    new Quote("Only sissies castle.", "Rob Sillars"),
    new Quote(
      "Look at Garry Kasparov. After he loses, invariably he wins the next game. He just kills the next guy. That’s something that we have to learn to be able to do.",
      "Maurice Ashley"
    ),
    new Quote("There just isn’t enough televised chess.", "David Letterman"),
    new Quote(
      "Avoid the crowd. Do your own thinking independently. Be the chess player, not the chess piece.",
      "Ralph Charell"
    ),
    new Quote(
      "Chess is a terrible game. If you have no center, your opponent has a freer position. If you do have a center, then you really have something to worry about!",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "Any material change in a position must come about by mate, a capture, or a pawn promotion.",
      "Purdy"
    ),
    new Quote(
      "We don’t really know how the game was invented, though there are suspicions. As soon as we discover the culprits, we’ll let you know.",
      "Bruce Pandolfini"
    ),
    new Quote(
      "The battle for the ultimate truth will never be won.  And that’s why chess is so fascinating.",
      "Hans Kmoch"
    ),
    new Quote(
      "I am still a victim of chess. It has all the beauty of art and much more. It cannot be commercialized. chess is much purer than art in its social position.",
      "Marcel Duchamp"
    ),
    new Quote("Blessed be the memory of him who gave the world this immortal game.", "A. G. Gardiner"),
    new Quote(
      "In the perfect chess combination as in a first-rate short story, the whole plot and counter-plot should lead up to a striking finale, the interest not being allayed until the very last moment.",
      "Yates and Winter"
    ),
    new Quote("Castle early and often.", "Rob Sillars"),
    new Quote(
      "I believe that chess possesses a magic that is also a help in advanced age. A rheumatic knee is forgotten during a game of chess and other events can seem quite unimportant in comparison with a catastrophe on the chessboard.",
      "Vlastimil Hort"
    ),
    new Quote(
      "Chess is a more highly symbolic game, but the aggressions are therefore even more frankly represented in the play. It probably began as a war game; that is, the representation of a miniature battle between the forces of two kingdoms.",
      "Karl Meninger"
    ),
    new Quote(
      "No chess Grandmaster is normal; they only differ in the extent of their madness.",
      "Viktor Korchnoi"
    ),
    new Quote("Chess is 99 percent tactics.", "Teichmann"),
    new Quote("I’d rather have a pawn than a finger.", "Reuben Fine"),
    new Quote("Chess mastery essentially consists of analyzing.", "Mikhail Botvinnik"),
    new Quote(
      "If your opponent cannot do anything active, then don’t rush the position; instead you should let him sit there, suffer, and beg you for a draw.",
      "Jeremy Silman"
    ),
    new Quote(
      "The chess pieces are the block alphabet which shapes thoughts; and these thoughts, although making a visual design on the chessboard, express their beauty abstractly, like a poem.",
      "Marcel Duchamp"
    ),
    new Quote(
      "Examine moves that smite! A good eye for smites is far more important than a knowledge of strategical principles.",
      "Purdy"
    ),
    new Quote("Chess is like life.", "Boris Spassky"),
    new Quote(
      "If your opponent offers you a draw, try to work out why he thinks he’s worse off.",
      "Nigel Short"
    ),
    new Quote(
      "Chess teaches you to control the initial excitement you feel when you see something that looks good and it trains you to think objectively when you’re in trouble.",
      "Stanley Kubrick"
    ),
    new Quote("Let the perfectionist play postal.", "Yasser Seirawan"),
    new Quote(
      "If chess is a science, it’s a most inexact one. If chess is an art, it is too exacting to be seen as one. If chess is a sport, it’s too esoteric. If chess is a game, it’s too demanding to be just a game. If chess is a mistress, she’s a demanding one. If chess is a passion, it’s a rewarding one. If chess is life, it’s a sad one.",
      "Anonymous"
    ),
    new Quote(
      "Chess is a foolish expedient for making idle people believe they are doing something very clever when they are only wasting their time.",
      "George Bernard Shaw"
    ),
    new Quote(
      "You must take your opponent into a deep dark forest where 2+2=5, and the path leading out is only wide enough for one.",
      "Mikhail Tal"
    ),
    new Quote(
      "I feel as if I were a piece in a game of chess, when my opponent says of it: That piece cannot be moved.",
      "Soren Kierkegaard"
    ),
    new Quote(
      "When your house is on fire, you can't be bothered with the neighbors.  Or, as we say in chess, if your king is under attack you don’t worry about losing a pawn on the queen’s side.",
      "Garry Kasparov"
    ),
    new Quote(
      "Man is a frivolous, a specious creature, and like a chess player, cares more for the process of attaining his goal than for the goal itself.",
      "Dostoyevsky"
    ),
    new Quote(
      "When asked, -How is that you pick better moves than your opponents?, I responded: I’m very glad you asked me that, because, as it happens, there is a very simple answer. I think up my own moves, and I make my opponent think up his.",
      "Alexander Alekhine"
    ),
    new Quote("Mistrust is the most necessary characteristic of the chess player.", "Siegbert Tarrasch"),
    new Quote(
      "What is the object of playing a gambit opening?... To acquire a reputation of being a dashing player at the cost of losing a game.",
      "Siegbert Tarrasch"
    ),
    new Quote("Pawns; they are the soul of this game, they alone form the attack and defense.", "Philidor"),
    new Quote("In chess, at least, the brave inherit the earth.", "Edmar Mednis"),
    new Quote(
      "There are two classes of men; those who are content to yield to circumstances and who play whist; those who aim to control circumstances, and who play chess.",
      "Mortimer Collins"
    ),
    new Quote(
      "The tactician must know what to do whenever something needs doing; the strategist must know what to do when nothing needs doing.",
      "Savielly Tartakover"
    ),
    new Quote("All chess players should have a hobby.", "Savielly Tartakower"),
    new Quote(
      "I played chess with him and would have beaten him sometimes only he always took back his last move, and ran the game out differently.",
      "Mark Twain"
    ),
    new Quote(
      "The tactician knows what to do when there is something to do; whereas the strategian knows what to do when there is nothing to do.",
      "Gerald Abrahams"
    ),
    new Quote("In chess, just as in life, today’s bliss may be tomorrow’s poison.", "Assaic"),
    new Quote(
      "You may learn much more from a game you lose than from a game you win. You will have to lose hundreds of games before becoming a good player.",
      "Jose Raul Capablanca"
    ),
    new Quote("The way he plays chess demonstrates a man’s whole nature.", "Stanley Ellin"),
    new Quote("You can only get good at chess if you love the game.", "Bobby Fischer"),
    new Quote("A man that will take back a move at chess will pick a pocket.", "Richard Fenton"),
    new Quote(
      "Whoever sees no other aim in the game than that of giving checkmate to one’s opponent will never become a good chess player.",
      "Euwe"
    ),
    new Quote("In blitz, the Knight is stronger than the Bishop.", "Vlastimil Hort"),
    new Quote("Chess is a fighting game which is purely intellectual and includes chance.", "Richard Reti"),
    new Quote("Chess is a sea in which a gnat may drink and an elephant may bathe.", "Hindu proverb"),
    new Quote("Pawn endings are to chess what putting is to golf.", "Cecil Purdy"),
    new Quote("Chess opens and enriches your mind.", "Saudin Robovic"),
    new Quote("The isolated pawn casts gloom over the entire chessboard.", "Aaron Nimzovich"),
    new Quote(
      "For me, chess is life and every game is like a new life. Every chess player gets to live many lives in one lifetime.",
      "Eduard Gufeld"
    ),
    new Quote("Chess is a terrific way for kids to build self image and self esteem.", "Saudin Robovic"),
    new Quote("If a ruler does not understand chess, how can he rule over a kingdom?", "King Khusros II"),
    new Quote("Chess is a cold bath for the mind.", "Sir John Simon"),
    new Quote(
      "Becoming successful at chess allows you to discover your own personality. That’s what I want for the kids I teach.",
      "Saudin Robovic"
    ),
    new Quote(
      "Chess is so inspiring that I do not believe a good player is capable of having an evil thought during the game.",
      "Wilhelm Steinitz"
    ),
    new Quote("You are for me the queen on d8 and I am the pawn on d7!! ", "GM Eduard Gufeld"),
    new Quote(
      "By playing at chess then, we may learn: First: Foresight. Second: Circumspection. Third: Caution. And lastly, we learn by chess the habit of not being discouraged by present bad appearances in the state of our affairs, the habit of hoping for a favorable chance, and that of persevering in the secrets of resources.",
      "Benjamin Franklin"
    ),
    new Quote("I prefer to lose a really good game than to win a bad one.", "David Levy"),
    new Quote(
      "Capture of the adverse king is the ultimate but not the first object of the game.",
      "William Steinitz"
    ),
    new Quote(
      "When I have white, I win because I am white; When I have black, I win because I am Bogolyubov.",
      "Bogolyubov"
    ),
    new Quote("Every pawn is a potential queen.", "James Mason"),
    new Quote(
      "Chess is in its essence a game, in its form an art, and in its execution a science.",
      "Baron Tassilo"
    ),
    new Quote("No price is too great for the scalp of the enemy king.", "Koblentz"),
    new Quote(
      "In life, as in chess, ones own pawns block ones way.  A mans very wealth, ease, leisure, children, books, which should help him to win, more often checkmate him.",
      "Charles Buxton"
    ),
    new Quote(
      "Chess is a part of culture and if a culture is declining then chess too will decline.",
      "Mikhail Botvinnik"
    ),
    new Quote(
      "A good sacrifice is one that is not necessarily sound but leaves your opponent dazed and confused.",
      "Rudolph Spielmann"
    ),
    new Quote(
      "Chess, like any creative activity, can exist only through the combined efforts of those who have creative talent, and those who have the ability to organize their creative work.",
      "Mikhail Botvinnik"
    ),
    new Quote("One bad move nullifies forty good ones.", "Horowitz"),
    new Quote(
      "Place the contents of the chess box in a hat, shake them up vigorously, pour them on the board from a height of two feet, and you get the style of Steinitz.",
      "H. E. Bird"
    ),
    new Quote(
      "I have never in my life played the French Defence, which is the dullest of all openings.",
      "Wilhelm Steinitz"
    ),
    new Quote("Pawns are born free, yet they are everywhere in chains.", "Rick Kennedy"),
    new Quote(
      "It is not a move, even the best move that you must seek, but a realizable plan.",
      "Eugene Znosko-Borovsky"
    ),
    new Quote("Those who say they understand chess, understand nothing.", "Robert Hubner"),
    new Quote("Good offense and good defense both begin with good development.", "Bruce A. Moon"),
    new Quote(
      "Botvinnik tried to take the mystery out of chess, always relating it to situations in ordinary life. He used to call chess a typical inexact problem similar to those which people are always having to solve in everyday life.",
      "Garry Kasparov"
    ),
    new Quote("A good player is always lucky.", "Jose Raul Capablanca"),
    new Quote(
      "The sign of a great master is his ability to win a won game quickly and painlessly.",
      "Irving Chernev"
    ),
    new Quote(
      "One of these modest little moves may be more embarrassing to your opponent than the biggest threat.",
      "Siegbert Tarrasch"
    ),
    new Quote("Live, lose, and learn, by observing your opponent how to win.", "Amber Steenbock"),
    new Quote("The older I grow, the more I value pawns.", "Keres"),
    new Quote("Everything is in a state of flux, and this includes the world of chess.", "Mikhail Botvinnik"),
    new Quote(
      "The beauty of a move lies not in its appearance but in the thought behind it.",
      "Aaron Nimzovich"
    ),
    new Quote("My God, Bobby Fischer plays so simply.", "Alexei Suetin"),
    new Quote("You need not play well - just help your opponent to play badly.", "Genrikh Chepukaitis"),
    new Quote(
      "It is difficult to play against Einstein’s theory --on his first loss to Fischer.",
      "Mikhail Tal"
    ),
    new Quote("The only thing chess players have in common is chess.", "Lodewijk Prins"),
    new Quote("Bobby just drops the pieces and they fall on the right squares.", "Miguel Najdorf"),
    new Quote(
      "We must make sure that chess will not be like a dead language, very interesting, but for a very small group.",
      "Sytze Faber"
    ),
    new Quote("The passion for playing chess is one of the most unaccountable in the world.", "H.G. Wells"),
    new Quote(
      "Chess is so interesting in itself, as not to need the view of gain to induce engaging in it; and thence it is never played for money.",
      "Benjamin Franklin"
    ),
    new Quote(
      "The enormous mental resilience, without which no chess player can exist, was so much taken up by chess that he could never free his mind of this game.",
      "Albert Einstein"
    ),
    new Quote("Nowadays, when you’re not a Grandmaster at 14, you can forget about it.", "Anand Viswanathan"),
    new Quote(
      "Do you realize Fischer almost never has any bad pieces? He exchanges them, and the bad pieces remain with his opponents.",
      "Yuri Balashov"
    ),
    new Quote("It is always better to sacrifice your opponent’s men.", "Savielly Tartakower"),
    new Quote("In chess, as it is played by masters, chance is practically eliminated.", "Emanuel Lasker"),
    new Quote(
      "You know you’re going to lose. Even when I was ahead I knew I was going to lose  --on playing against Fischer.",
      "Andrew Soltis"
    ),
    new Quote(
      "I won’t play with you anymore. You have insulted my friend! --when an opponent cursed himself for a blunder.",
      "Miguel Najdorf"
    ),
    new Quote(
      "You know, comrade Pachman, I don’t enjoy being a Minister, I would rather play chess like you.",
      "Che Guevara"
    ),
    new Quote(
      "It began to feel as though you were playing against chess itself --on playing against Robert Fischer.",
      "Walter Shipman"
    ),
    new Quote("Checkers is for tramps.", "Paul Morphy"),
    new Quote(
      "When you play Bobby, it is not a question if you win or lose.  It is a question if you survive.",
      "Boris Spassky"
    ),
    new Quote("When you absolutely don’t know what to do anymore, it is time to panic.", "John van der Wiel"),
    new Quote("We like to think.", "Garry Kasparov"),
    new Quote("Dazzling combinations are for the many, shifting wood is for the few.", "Georg Kieninger"),
    new Quote("In complicated positions, Bobby Fischer hardly had to be afraid of anybody.", "Paul Keres"),
    new Quote(
      "It was clear to me that the vulnerable point of the American Grandmaster (Bobby Fischer) was in double-edged, hanging, irrational positions, where he often failed to find a win even in a won position.",
      "Efim Geller"
    ),
    new Quote(
      "I love all positions. Give me a difficult positional game, I will play it.  But totally won positions, I cannot stand them.",
      "Hein Donner"
    ),
    new Quote(
      "In Fischer’s hands, a slight theoretical advantage is as good a being a queen ahead.",
      "Isaac Kashdan"
    ),
    new Quote(
      "Bobby Fischer’s current state of mind is indeed a tragedy. One of the worlds greatest chess players - the pride and sorrow of American chess.",
      "Frank Brady"
    ),
    new Quote("Fischer is an American chess tragedy on par with Morphy and Pillsbury.", "Mig Greengard"),
    new Quote(
      "Nonsense was the last thing Fischer was interested in, as far as chess was concerned.",
      "Elie Agur"
    ),
    new Quote(
      "Fischer is the strongest player in the world. In fact, the strongest player who ever lived.",
      "Larry Evans"
    ),
    new Quote("If you aren’t afraid of Spassky, then I have removed the element of money.", "Jim Slater"),
    new Quote("I guess a certain amount of temperament is expected of chess geniuses.", "Ron Gross"),
    new Quote(
      "Fischer sacrificed virtually everything most of us weakies (to use his term) value, respect, and cherish, for the sake of an artful, often beautiful board game, for the ambivalent privilege of being its greatest master.",
      "Paul Kollar"
    ),
    new Quote(
      "Fischer chess play was always razor-sharp, rational and brilliant. One of the best ever.",
      "Dave Regis"
    ),
    new Quote("Fischer wanted to give the Russians a taste of their own medicine.", "Larry Evans"),
    new Quote(
      "With or without the title, Bobby Fischer was unquestionably the greatest player of his time.",
      "Burt Hochberg"
    ),
    new Quote(
      "Fischer is completely natural. He plays no roles.  He’s like a child. Very, very simple.",
      "Zita Rajcsanyi"
    ),
    new Quote("Spassky will not be psyched out by Fischer.", "Mike Goodall"),
    new Quote(
      "Already at 15 years of age he was a Grandmaster, a record at that time, and his battle to reach the top was the background for all the major chess events of the 1960.",
      "Tim Harding"
    ),
    new Quote(
      "Fischer, who may or may not be mad as a hatter, has every right to be horrified.",
      "Jeremy Silman"
    ),
    new Quote(
      "When I asked Fischer why he had not played a certain move in our game, he replied: ‘Well, you laughed when I wrote it down!’",
      "Mikhail Tal"
    ),
    new Quote("I look one move ahead... the best!", "Siegbert Tarrasch"),
    new Quote("Fischer prefers to enter chess history alone.", "Miguel Najdorf"),
    new Quote(
      "Bobby is the most misunderstood, misquoted celebrity walking the face of this earth.",
      "Yasser Seirawan"
    ),
    new Quote(
      "When you don’t know what to play, wait for an idea to come into your opponent’s mind. You may be sure that idea will be wrong.",
      "Siegbert Tarrasch"
    ),
    new Quote("There is no remorse like the remorse of chess.", "H. G. Wells"),
    new Quote(
      "By this measure (on the gap between Fischer & his contemporaries), I consider him the greatest world champion.",
      "Garry Kasparov"
    ),
    new Quote(
      "By the beauty of his games, the clarity of his play, and the brilliance of his ideas, Fischer made himself an artist of the same stature as Brahms, Rembrandt, and Shakespeare.",
      "David Levy"
    ),
    new Quote(
      "Chess is a terrible game. If you have no center, your opponent has a freer position. If you do have a center, then you really have something to worry about!",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "Many chess players were surprised when after the game, Fischer quietly explained: ’I had already analyzed this possibility’ in a position which I thought was not possible to foresee from the opening.",
      "Mikhail Tal"
    ),
    new Quote(
      "Suddenly it was obvious to me in my analysis I had missed what Fischer had found with the greatest of ease at the board.",
      "Mikhail Botvinnik"
    ),
    new Quote("The king is a fighting piece. Use it!", "Wilhelm Steinitz"),
    new Quote(
      "A thorough understanding of the typical mating continuations makes the most complicated sacrificial combinations leading up to them not only not difficult, but almost a matter of course.",
      "Siegbert Tarrasch"
    ),
    new Quote("Bobby Fischer is the greatest chess genius of all time!", "Alexander Kotov"),
    new Quote(
      "The laws of chess do not permit a free choice: you have to move whether you like it or not.",
      "Emanuel Lasker"
    ),
    new Quote(
      "First-class players lose to second-class players because second-class players sometimes play a first-class game.",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "Bobby is the finest chess player this country ever produced. His memory for the moves, his brilliance in dreaming up combinations, and his fierce determination to win are uncanny.",
      "John Collins"
    ),
    new Quote(
      "After a bad opening, there is hope for the middle game. After a bad middle game, there is hope for the endgame. But once you are in the endgame, the moment of truth has arrived.",
      "Edmar Mednis"
    ),
    new Quote(
      "Weak points or holes in the opponent’s position must be occupied by pieces not pawns.",
      "Siegbert Tarrasch"
    ),
    new Quote("There is only one thing Fischer does in chess without pleasure: to lose!", "Boris Spassky"),
    new Quote("Bobby Fischer is the greatest chess player who has ever lived.", "Ken Smith"),
    new Quote(
      "Up to this point white has been following well-known analysis. But now he makes a fatal error: he begins to use his own head.",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "Fischer was a master of clarity and a king of artful positioning. His opponents would see where he was going but were powerless to stop him.",
      "Bruce Pandolfini"
    ),
    new Quote(
      "No other master has such a terrific will to win. At the board he radiates danger, and even the strongest opponents tend to freeze, like rabbits when they smell a panther. Even his weaknesses are dangerous.",
      "Anonymous German Expert"
    ),
    new Quote(
      "White lost because he failed to remember the right continuation and had to think up the moves himself.",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "Not only will I predict his triumph over Botvinnik, but I’ll go further and say that he’ll probably be the greatest chess player that ever lived.",
      "John Collins"
    ),
    new Quote("I consider Fischer to be one of the greatest opening experts ever.", "Keith Hayward"),
    new Quote(
      "I like to say that Bobby Fischer was the greatest player ever. But what made Fischer a genius was his ability to blend an American freshness and pragmatism with Russian ideas about strategy.",
      "Bruce Pandolfini"
    ),
    new Quote(
      "At this time Fischer is simply a level above all the best chessplayers in the world.",
      "John Jacobs"
    ),
    new Quote(
      "I have always a slight feeling of pity for the man who has no knowledge of chess.",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "There’s never before been a chess player with such a thorough knowledge of the intricacies of the game and such an absolutely indomitable will to win. I think Bobby is the greatest player that ever lived.",
      "Lisa Lane"
    ),
    new Quote("He who takes the queen’s Knight’s pawn will sleep in the streets.", "Anonymous"),
    new Quote(
      "I had a toothache during the first game. In the second game I had a headache. In the third game it was an attack of rheumatism. In the fourth game, I wasn’t feeling well. And in the fifth game? Well, must one have to win every game?",
      "Siegbert Tarrasch"
    ),
    new Quote("The stomach is an essential part of the chess master.", "Bent Larsen"),
    new Quote(
      "I’m not a materialistic person, in that, I don’t suffer the lack or loss of money.  The absence of worldly goods I don’t look back on. For chess is a way I can be as materialistic as I want without having to sell my soul ",
      "Jamie Walter Adams"
    ),
    new Quote(
      "These are not pieces, they are men! For any man to walk into the line of fire will be one less man in your army to fight for you. Value every troop and use him wisely, throw him not to the dogs as he is there to serve his king.",
      "Jamie Walter Adams"
    ),
    new Quote("Chess isn’t a game of speed, it is a game of speech through actions.", "Matthew Selman"),
    new Quote("Life like chess is about knowing to do the right move at the right time.", "Kaleb Rivera"),
    new Quote("Come on Harry!", "Simon Williams"),
    new Quote(
      "Some people think that if their opponent plays a beautiful game, it’s okay to lose. I don’t. You have to be merciless.",
      "Magnus Carlsen"
    ),
    new Quote(
      "It's one of those types of positions where he has pieces on squares.",
      "John ~ZugAddict~ Chernoff"
    ),
    new Quote("On the bright side, I no longer have any more pieces to lose.", "John ~ZugAddict~ Chernoff"),
    new Quote(
      "Tactics... Tactics are your friends. But they are weird friends who do strange things.",
      "John ~ZugAddict~ Chernoff"
    ),
    new Quote(
      "You can't take the pawn because then the other will queen. Like wonder twin powers",
      "John ~ZugAddict~ Chernoff"
    ),
    new Quote(
      "Most of the gods throw dice but Fate plays chess, and you don't find out until too late that he's been using two queens all along.",
      "Terry Pratchett"
    ),
    new Quote(
      "Atomic is just like regular chess, except you're exploding, everything's exploding, and you're in bullet hell.",
      "Unihedron 0"
    ),
    new Quote("lichess is better, but it's free.", "Thibault Duplessis"),
    new Quote(
      "When you trade, the key concern is not always the value of the pieces being exchanged, but what’s left on the board.",
      "Dan Heisman"
    ),
    new Quote(
      "I detest the endgame. A well-played game should be practically decided in the middlegame.",
      "David Janowski"
    ),
    new Quote(
      "Many men, many styles; what is chess style but the intangible expression of the will to win.",
      "Aaron Nimzowitsch"
    ),
    new Quote("Never play for the win, never play for the draw, just play chess!", "Khalifman"),
    new Quote(
      "In chess, knowledge is a very transient thing. It changes so fast that even a single mouse-slip sometimes changes the evaluation.",
      "Viswanathan Anand"
    ),
    new Quote(
      "Having good strategies in playing chess is often a good indication of being focused in life.",
      "Martin Dansky"
    ),
    new Quote(
      "Chess is an infinitely complex game, which one can play in infinitely numerous and varied ways.",
      "Vladimir Kramnik"
    ),
    new Quote(
      "Chess: It’s like alcohol. It’s a drug. I have to control it, or it could overwhelm me.",
      "Charles Krauthammer"
    ),
    new Quote(
      "Drawing general conclusions about your main weaknesses can provide a great stimulus to further growth.",
      "Alexander Kotov"
    ),
    new Quote(
      "The good thing in chess is that very often the best moves are the most beautiful ones. The beauty of logic.",
      "Boris Gelfand"
    ),
    new Quote(
      "Any experienced player knows how a change in the character of the play influences your psychological mood.",
      "Garry Kasparov"
    ),
    new Quote("Be a harsh critic of your own wins.", "Vasilios Kotronias"),
    new Quote(
      "Good players develop a tactical instinct, a sense of what is possible or likely and what is not worth calculating.",
      "Sam Reshevsky"
    ),
    new Quote(
      "Lack of patience is probably the most common reason for losing a game, or drawing games that should have been won.",
      "Bent Larsen"
    ),
    new Quote(
      "The scheme of a game is played on positional lines; the decision of it, as a rule, is effected by combinations.",
      "Richard Reti"
    ),
    new Quote("On the chessboard lies and hypocrisy do not last long.", "Emanuel Lasker"),
    new Quote(
      "The single most important thing in life is to believe in yourself regardless of what everyone else says.",
      "Hikaru Nakamura"
    ),
    new Quote(
      "Attackers may sometimes regret bad moves, but it is much worse to forever regret an opportunity you allowed to pass you by.",
      "Garry Kasparov"
    ),
    new Quote(
      "My favorite victory is when it is not even clear where my opponent made a mistake.",
      "Peter Leko"
    ),
    new Quote("Win with grace, lose with dignity.", "Susan Polgar"),
    new Quote(
      "Pawns are such fascinating pieces, too...So small, almost insignificant, and yet--they can depose kings.",
      "Lavie Tidhar"
    ),
    new Quote("The move is there, but you must see it.", "Savielly Tartakower"),
    new Quote(
      "The kings are an apt metaphor for human beings: utterly constrained by the rules of the game, defenseless against bombardment from all sides, able only to temporarily dodge disaster by moving one step in any direction.",
      "Jennifer duBois"
    ),
    new Quote(
      "If chess is an art, Alekhine. If chess is a science, Capablanca. If chess is a struggle, Lasker. --on who he thought was the best player.",
      "Savielly Tartakower"
    ),
    new Quote("Chess is a good mistress, but a bad master.", "Gerald Abrahams"),
    new Quote("I often play a move I know how to refute.", "Bent Larsen"),
    new Quote("First restrain, next blockade, lastly destroy.", "Aron Nimzowitsch"),
    new Quote(
      "If you don't know what to do, find your worst piece and look for a better square.",
      "Gerard Schwarz"
    ),
    new Quote(
      "Players who balk at playing one-minute chess are failing to see the whole picture. They shouldn’t be worrying that they will make more mistakes – they should be rubbing their hands in glee at the thought of all the mistakes their opponents will make.",
      "Hikaru Nakamura"
    ),
    new Quote(
      "A Chess game is divided into three stages: the first, when you hope you have the advantage, the second when you believe that you have an advantage, and the third ... when you know you're going to lose !",
      "Savielly Tartakower"
    ),
    new Quote(
      "A Queen's sacrifice, even when fairly obvious, always rejoices the heart of the chess-lover.",
      "Savielly Tartakower"
    ),
    new Quote(
      "A chess game, after all, is a fight in which all possible factors must be made use of, and in which a knowledge of the opponent's good and bad qualities is of the greatest importance.",
      "Emanuel Lasker"
    ),
    new Quote("A chess player never has a heart attack in a good position.", "Bent Larsen"),
    new Quote("A computer beat me in chess, but it was no match when it came to kickboxing.", "Emo Phillips"),
    new Quote(
      "A considerable role in the forming of my style was played by an early attraction to study composition.",
      "Vasily Smyslov"
    ),
    new Quote("A defeatist spirit must inevitably lead to disaster.", "Eugene Znosko-Borovski"),
    new Quote(
      "A draw can be obtained not only by repeating moves, but also by one weak move.",
      "Savielly Tartakower"
    ),
    new Quote(
      "A draw may be the beautiful and logical result of fine attacks and parries; and the public ought to appreciate such games, in contrast, of course, to the fear-and-laziness draws.",
      "Bent Larsen"
    ),
    new Quote(
      "A gambit never becomes sheer routine as long as you fear you may lose the king and pawn ending!",
      "Bent Larsen"
    ),
    new Quote("A great chess player always has a very good memory.", "Leonid Shamkovich"),
    new Quote("A knight ending is really a pawn ending.", "Mikhail Botvinnik"),
    new Quote(
      "A lot of these ideas are built under wrong presumptions which officials have that chess players are lazy bastards whose sole idea is to deceive (the) public and to make short draws and go home. It's not true. It's a lie. (On the Sofia Corsica rule)",
      "Boris Gelfand"
    ),
    new Quote(
      "A male scorpion is stabbed to death after mating. In chess, the powerful queen often does the same to the king without giving him the satisfaction of a lover.",
      "Gregor Piatigorsky"
    ),
    new Quote(
      "A pawn, when separated from his fellows, will seldom or never make a fortune.",
      "Francois-Andre Danican Philidor"
    ),
    new Quote("A plan is made for a few moves only, not for the whole game.", "Rueben Fine"),
    new Quote(
      "A player can sometimes afford the luxury of an inaccurate move, or even a definite error, in the opening or middlegame without necessarily obtaining a lost position. In the endgame ... an error can be decisive, and we are rarely presented with a second chance.",
      "Paul Keres"
    ),
    new Quote(
      "A real sacrifice involves a radical change in the character of a game which cannot be effected without foresight, fantasy, and the willingness to risk.",
      "Leonid Shamkovich"
    ),
    new Quote(
      "A sport, a struggle for results and a fight for prizes. I think that the discussion about “chess is science or chess is art” is already inappropriate. The purpose of modern chess is to reach a result.",
      "Alexander Morozevich"
    ),
    new Quote(
      "A strong player requires only a few minutes of thought to get to the heart of the conflict. You see a solution immediately, and half an hour later merely convince yourself that your intuition has not deceived you.",
      "David Bronstein"
    ),
    new Quote(
      "A win gives one a feeling of self-affirmation, and success - a feeling of self-expression, but only a sensible harmonization between these urges can bring really great achievements in chess.",
      "Oleg Romanishin"
    ),
    new Quote(
      "Above all else, before playing in competitions a player must have regard to his health, for if he is suffering from ill-health he cannot hope for success. In this connection the best of all tonics is 15 to 20 days in the fresh air, in the country.",
      "Mikhail Botvinnik"
    ),
    new Quote(
      "According to such great attacking players as Bronstein and Tal, most combinations are inspired by the player's memories of earlier games.",
      "Pal Benko"
    ),
    new Quote(
      "After I won the title, I was confronted with the real world. People do not behave naturally anymore – hypocrisy is everywhere.",
      "Boris Spassky"
    ),
    new Quote(
      "After a great deal of discussion in Soviet literature about the correct definition of a combination, it was decided that from the point of view of a methodical approach it was best to settle on this definition - A combination is a forced variation with a sacrifice.",
      "Alexander Kotov"
    ),
    new Quote(
      "Agreeing to draws in the middlegame, equal or otherwise, deprives you of the opportunity to practice playing endgames, and the endgame is probably where you need the most practice.",
      "Pal Benko"
    ),
    new Quote(
      "All chess masters have on occasion played a magnificent game and then lost it by a stupid mistake, perhaps in time pressure and it may perhaps seem unjust that all their beautiful ideas get no other recognition than a zero on the tournament table.",
      "Bent Larsen"
    ),
    new Quote(
      "All chess players know what a combination is. Whether one makes it oneself, or is its victim, or reads of it, it stands out from the rest of the game and stirs one's admiration.",
      "Eugene Znosko-Borowski"
    ),
    new Quote("All conceptions in the game of chess have a geometrical basis.", "Eugene Znosko-Borowski"),
    new Quote(
      "All lines of play which lead to the imprisonment of the bishop are on principle to be condemned. (on the closed Ruy Lopez)",
      "Siegbert Tarrasch"
    ),
    new Quote("All that matters on the chessboard is good moves.", "Bobby Fischer"),
    new Quote(
      "All that now seems to stand between Nigel and the prospect of the world crown is the unfortunate fact that fate brought him into this world only two years after Kasparov.",
      "(prophetic comment in 1987) - Garry Kasparov"
    ),
    new Quote(
      "Along with my retirement from chess analytical work seems to have gone too.",
      "Mikhail Botvinnik"
    ),
    new Quote(
      "Although the Knight is generally considered to be on a par with the Bishop in strength, the latter piece is somewhat stronger in the majority of cases in which they are opposed to each other.",
      "Jose Capablanca"
    ),
    new Quote("Amberley excelled at chess - a mark, Watson, of a scheming mind.", "Sir Arthur Conan Doyle"),
    new Quote(
      "Americans really don't know much about chess. But I think when I beat Spassky, that Americans will take a greater interest in chess. Americans like winners.",
      "Bobby Fischer"
    ),
    new Quote(
      "Among top grandmasters the Dutch is a rare defense, which is good reason to play it! It has not been studied very deeply by many opponents, and theory, based on a small number of 'reliable' games, must be rather unreliable.",
      "Bent Larsen"
    ),
    new Quote(
      "An amusing fact: as far as I can recall, when playing the Ruy Lopez I have not yet once in my life had to face the Marshall Attack!",
      "Anatoly Karpov"
    ),
    new Quote(
      "An innovation need not be especially ingenious, but it must be well worked out.",
      "Paul Keres"
    ),
    new Quote("An isolated pawn spreads gloom all over the chessboard.", "Savielly Tartakower"),
    new Quote(
      "Analysis is a glittering opportunity for training: it is just here that capacity for work, perseverance and stamina are cultivated, and these qualities are, in truth, as necessary to a chess player as a marathon runner.",
      "Lev Polugaevsky"
    ),
    new Quote(
      "Analysis, if it is really carried out with a complete concentration of his powers, forms and completes a chess player.",
      "Lev Polugaevsky"
    ),
    new Quote(
      "Anyone who wishes to learn how to play chess well must make himself or herself thoroughly conversant with the play in positions where the players have castled on opposite sides.",
      "Alexander Kotov"
    ),
    new Quote(
      "Apart from direct mistakes, there is nothing more ruinous than routine play, the aim of which is mechanical development.",
      "Alexei Suetin"
    ),
    new Quote(
      "As Rousseau could not compose without his cat beside him, so I cannot play chess without my king's bishop. In its absence the game to me is lifeless and void. The vitalizing factor is missing, and I can devise no plan of attack.",
      "Siegbert Tarrasch"
    ),
    new Quote(
      "As a chess player one has to be able to control one’s feelings, one has to be as cold as a machine.",
      "Levon Aronian"
    ),
    new Quote(
      "As a rule, pawn endings have a forced character, and they can be worked out conclusively.",
      "Mark Dvoretsky"
    ),
    new Quote(
      "As a rule, so-called \"positional\" sacrifices are considered more difficult, and therefore more praise-worthy, than those which are based exclusively on an exact calculation of tactical possibilities.",
      "Alexander Alekhine"
    ),
    new Quote(
      "As a rule, the more mistakes there are in a game, the more memorable it remains, because you have suffered and worried over each mistake at the board.",
      "Victor Kortchnoi"
    ),
    new Quote(
      "As long as my opponent has not yet castled, on each move I seek a pretext for an offensive. Even when I realize that the king is not in danger.",
      "Mikhail Tal"
    ),
    new Quote(
      "As often as not, his strategy consists of stifling Black's activity and then winning in an endgame thanks to his superior pawn structure.",
      "Neil McDonald (1998)"
    ),
    new Quote("Attack! Always Attack!", "Adolf Anderssen"),
    new Quote(
      "Attackers may sometimes regret bad moves, but it is much worse to forever regret an opportunity you allowed to pass you by.",
      "Garry Kasparov"
    ),
    new Quote(
      "Avoidance of mistakes is the beginning, as it is the end, of mastery in chess.",
      "Eugene Znosko-Borovsky"
    ),
    new Quote(
      "Barcza is the most versatile player in the opening. He sometimes plays P-KKt3 on the first, sometimes on the second, sometimes on the third, and sometimes only on the fourth move.",
      "reputedly stated by Harry Golombek"
    ),
    new Quote("Before Geller we did not understand the King's Indian Defence.", "Mikhail Botvinnik"),
    new Quote(
      "Begone! Ignorant and impudent knight, not even in chess can a King be taken.",
      "King Louis VI (reputedly stated to one of his knights in 1110 after he was nearly captured by enemy forces)"
    ),
    new Quote("Black's d5-square is too weak.", "Ulf Andersson  (on the Dragon variation)"),
    new Quote("Blitz chess kills your ideas.", "Bobby Fischer"),
    new Quote(
      "Bobby Fischer started off each game with a great advantage: after the opening he had used less time than his opponent and thus had more time available later on. The major reason why he never had serious time pressure was that his rapid opening play simply left sufficient time for the middlegame.",
      "Edmar Mednis"
    ),
    new Quote(
      "Books on the openings abound; nor are works on the end game wanting; but those on the middle game can be counted on the fingers of one hand.",
      "Harry Golombek"
    ),
    new Quote(
      "Boris Vasilievich was the only top-class player of his generation who played gambits regularly and without fear ... Over a period of 30 years he did not lose a single game with the King's Gambit, and among those defeated were numerous strong players of all generations, from Averbakh, Bronstein and Fischer, to Seirawan.",
      "Garry Kasparov"
    ),
    new Quote(
      "Botvinnik tried to take the mystery out of Chess, always relating it to situations in ordinary life. He used to call chess a typical inexact problem similar to those which people are always having to solve in everyday life.",
      "Garry Kasparov"
    ),
    new Quote(
      "But alas! Like many another consummation devoutly to be wished, the actual performance was a disappointing one. (on the long awaited Lasker-Capablanca match in 1921)",
      "Fred Reinfeld"
    ),
    new Quote(
      "But how difficult it can be to gain the desired full point against an opponent of inferior strength, when this is demanded by the tournament position!",
      "Anatoly Karpov"
    ),
    new Quote(
      "But whatever you might say and whatever I might say, a machine which can play chess with people is one of the most marvellous wonders of our 20th century!",
      "David Bronstein"
    ),
    new Quote(
      "But you see when I play a game of Bobby, there is no style. Bobby played perfectly. And perfection has no style.",
      "Miguel Najdorf"
    ),
    new Quote(
      "By all means examine the games of the great chess players, but don't swallow them whole. Their games are valuable not for their separate moves, but for their vision of chess, their way of thinking.",
      "Anatoly Karpov"
    ),
    new Quote(
      "By positional play a master tries to prove and exploit true values, whereas by combinations he seeks to refute false values ... A combination produces an unexpected re-assessment of values.",
      "Emanuel Lasker"
    ),
    new Quote(
      "By some ardent enthusiasts Chess has been elevated into a science or an art. It is neither; but its principal characteristic seems to be what human nature mostly delights in a fight.",
      "Emanuel Lasker"
    ),
    new Quote(
      "By strictly observing Botvinnik's rule regarding the thorough analysis of one's own games, with the years I have come to realize that this provides the foundation for the continuous development of chess mastery.",
      "Garry Kasparov"
    ),
    new Quote(
      "By the mid-1990s the number of people with some experience of using computers was many orders of magnitude greater than in the 1960s. In the Kasparov defeat they recognized that here was a great triumph for programmers, but not one that may compete with the human intelligence that helps us to lead our lives.",
      "Igor Aleksander"
    ),
    new Quote(
      "By the time a player becomes a Grandmaster, almost all of his training time is dedicated to work on this first phase. The opening is the only phase that holds out the potential for true creativity and doing something entirely new.",
      "Garry Kasparov"
    ),
    new Quote(
      "By what right does White, in an absolutely even position, such as after move one, when both sides have advanced 1. e4, sacrifice a pawn, whose recapture is quite uncertain, and open up his kingside to attack? And then follow up this policy by leaving the check of the black queen open? None whatever !",
      "Emanuel Lasker"
    ),
    new Quote(
      "Can you imagine the relief it gives a mother when her child amuses herself quietly for hours on end?",
      "Klara Polgar"
    ),
    new Quote(
      "Capablanca did not apply himself to opening theory (in which he never therefore achieved much), but delved deeply into the study of end-games and other simple positions which respond to technique rather than to imagination.",
      "Max Euwe"
    ),
    new Quote(
      "Chess can help a child develop logical thinking, decision making, reasoning, and pattern recognition skills, which in turn can help math and verbal skills.",
      "Susan Polgar"
    ),
    new Quote(
      "Chess can learn a lot from poker. First, chess media and sponsors should emphasize its glamorous aspects: worldwide traveling, parties and escape from real world responsibilities.",
      "Jennifer Shahade"
    ),
    new Quote(
      "Chess can never reach its height by following in the path of science ... Let us, therefore, make a new effort and with the help of our imagination turn the struggle of technique into a battle of ideas.",
      "Jose Capablanca"
    ),
    new Quote(
      "Chess continues to advance over time, so the players of the future will inevitably surpass me in the quality of their play, assuming the rules and regulations allow them to play serious chess. But it will likely be a long time before anyone spends 20 consecutive years as number one, as I did.",
      "Garry Kasparov"
    ),
    new Quote(
      "Chess is a bond of brotherhood amongst all lovers of the noble game, as perfect as free masonry. It is a leveller of rank - title, wealth, nationality, politics, religion - all are forgotten across the board.",
      "Frederick Milne Edge"
    ),
    new Quote(
      "Chess is a contest between two men which lends itself particularly to the conflicts surrounding aggression.",
      "Rueben Fine"
    ),
    new Quote(
      "Chess is a contributor to net human unhappiness, since the pleasure of victory is greatly exceeded by the pain of defeat.",
      "Bill Hartston"
    ),
    new Quote("Chess is a cure for headaches.", "John Maynard Keynes"),
    new Quote(
      "Chess is a game sufficiently rich in meaning that it is easily capable of containing elements of both tragedy and comedy.",
      "Luke McShane"
    ),
    new Quote("Chess is a game which reflects most honor on human wit.", "Voltaire"),
    new Quote(
      "Chess is a great game. No matter how good one is, there is always somebody better. No matter how bad one is, there is always somebody worse.",
      "I.A. Horowitz"
    ),
    new Quote(
      "Chess is a matter of delicate judgement, knowing when to punch and how to duck.",
      "Bobby Fischer"
    ),
    new Quote("Chess is a matter of vanity.", "Alexander Alekhine"),
    new Quote("Chess is a meritocracy.", "Lawrence Day"),
    new Quote(
      "Chess is a miniature version of life. To be successful, you need to be disciplined, assess resources, consider responsible choices and adjust when circumstances change.",
      "Susan Polgar"
    ),
    new Quote("Chess is a natural cerebral high.", "Walter Browne"),
    new Quote("Chess is a sea in which a gnat may drink and an elephant may bathe.", "Hindu proverb"),
    new Quote("Chess is a sport. A violent sport.", "Marcel Duchamp"),
    new Quote("Chess is a test of wills.", "Paul Keres"),
    new Quote(
      "Chess is a unique cognitive nexus, a place where art and science come together in the human mind and are refined and improved by experience.",
      "Garry Kasparov"
    ),
    new Quote("Chess is beautiful enough to waste your life for.", "Hans Ree"),
    new Quote("Chess is eminently and emphatically the philosopher's game.", "Paul Morphy"),
    new Quote(
      "Chess is far too complex to be definitively solved with any technology we can conceive of today. However, our looked-down-upon cousin, checkers, or draughts, suffered this fate quite recently thanks to the work of Jonathan Schaeffer at the University of Alberta and his unbeatable program Chinook.",
      "Garry Kasparov"
    ),
    new Quote(
      "Chess is infinite, and one has to make only one ill-considered move, and one`s opponent`s wildest dreams will become reality.",
      "David Bronstein"
    ),
    new Quote(
      "Chess is like a language, the top players are very fluent at it. Talent can be developed scientifically but you have to find first what you are good at.",
      "Viswanathan Anand"
    ),
    new Quote(
      "Chess is like body-building. If you train every day, you stay in top shape. It is the same with your brain – chess is a matter of daily training.",
      "Vladimir Kramnik"
    ),
    new Quote("Chess is my life.", "Victor Kortchnoi"),
    new Quote(
      "Chess is my profession. I am my own boss; I am free. I like literature and music, classical especially. I am in fact quite normal; I have a Bohemian profession without being myself a Bohemian. I am neither a conformist nor a great revolutionary.",
      "Bent Larsen"
    ),
    new Quote(
      "Chess is not for the faint-hearted; it absorbs a person entirely. To get to the bottom of this game, he has to give himself up into slavery. Chess is difficult, it demands work, serious reflection and zealous research.",
      "Wilhelm Steinitz"
    ),
    new Quote("Chess is not for the timid.", "Irving Chernev"),
    new Quote("Chess is not relaxing ; it's stressful even if you win.", "Jennifer Shahade"),
    new Quote("Chess is one long regret.", "Stephen Leacock"),
    new Quote("Chess is only a recreation and not an occupation.", "Vladimir Lenin"),
    new Quote(
      "Chess is something more than a game. It is an intellectual diversion which has certain artistic qualities and many scientific elements.",
      "Jose Capablanca"
    ),
    new Quote("Chess is the touchstone of intellect.", "Johann Wolfgang von Goethe"),
    new Quote(
      "Chess is thirty to forty percent psychology. You don't have this when you play a computer. I can't confuse it.",
      "Judit Polgar"
    ),
    new Quote(
      "Chess is thriving. There are ever less round robin tournaments and ever more World Champions.",
      "Robert Huebner (1990, Schach)"
    ),
    new Quote("Chess is, above all, a fight.", "Emanuel Lasker"),
    new Quote(
      "Chess is, in essence, a game for children. Computers have exacerbated the trends towards youth because they now have an immensely powerful tool at their disposal and can absorb vast amounts of information extremely quickly.",
      "Nigel Short"
    ),
    new Quote(
      "Chess masters as well as chess computers deserve less reverence than the public accords them.",
      "Eliot Hearst"
    ),
    new Quote(
      "Chess programs are our enemies, they destroy the romance of chess. They take away the beauty of the game. Everything can be calculated.",
      "Levon Aronian"
    ),
    new Quote(
      "Chess strategy as such today is still in its diapers, despite Tarrasch's statement 'We live today in a beautiful time of progress in all fields'. Not even the slightest attempt has been made to explore and formulate the laws of chess strategy.",
      "Aaron Nimzowitsch (1925)"
    ),
    new Quote(
      "Chess strength in general and chess strength in a specific match are by no means one and the same thing.",
      "Garry Kasparov"
    ),
    new Quote(
      "Chess will always be in the doldrums as a spectator sport while a draw is given equal mathematical value as a decisive result.",
      "Michael Basman"
    ),
    new Quote("Chess, like love, is infectious at any age.", "Salo Flohr"),
    new Quote(
      "Chess-play is a good and witty exercise of the mind for some kind of men, but if it proceed from overmuch study, in such a case it may do more harm than good; it is a game too troublesome for some men's brains.",
      "Robt. Burton (1621) (clergyman and Librarian at Oxford University)"
    ),
    new Quote(
      "Combinations with a queen sacrifice are among the most striking and memorable ...",
      "Anatoly Karpov"
    ),
    new Quote(
      "Concentrate on material gains. Whatever your opponent gives you take, unless you see a good reason not to.",
      "Bobby Fischer"
    ),
    new Quote(
      "Condemned by theory, the Allgaier, certainly one of the most romantic of gambits, is generally successful in practice (and yet so rarely played). Why does the defender often seem hypnotized, quite demoralized?",
      "Tony Santasiere"
    ),
    new Quote(
      "Confidence is very important – even pretending to be confident. If you make a mistake but do not let your opponent see what you are thinking then he may overlook the mistake.",
      "Viswanathan Anand"
    ),
    new Quote(
      "Contrary to many young colleagues I do believe that it makes sense to study the classics.",
      "Magnus Carlsen"
    ),
    new Quote(
      "Deschapelles became a first-rate player in three days, at the age of something like thirty. Nobody ever believed the statement, not even Deschapelles himself, although his biographer declares he had told the lie so often that he at last forgot the facts of the case.",
      "Frederick Milne Edge"
    ),
    new Quote(
      "Despite the development of chess theory, there is much that remains secret and unexplored in chess.",
      "Vasily Smyslov"
    ),
    new Quote(
      "Do not bring your Queen out too early.",
      "from Francisco Bernardina Calogno's poem 'On the Game of Chess' circa 1500"
    ),
    new Quote(
      "Do not permit yourself to fall in love with the end-game play to the exclusion of entire games. It is well to have the whole story of how it happened; the complete play, not the denouement only. Do not embrace the rag-time and vaudeville of chess.",
      "Emanuel Lasker"
    ),
    new Quote(
      "Do not pick a move from a list of computer lines - use your own brains. This is important, especially for young players. It's better to study a worse line well than to reproduce a better computer line.",
      "Laszlo Hazai"
    ),
    new Quote(
      "Don't be afraid of losing, be afraid of playing a game and not learning something.",
      "Dan Heisman"
    ),
    new Quote(
      "Don't worry about your rating, work on your playing strength and your rating will follow.",
      "Dan Heisman"
    ),
    new Quote(
      "Don't worry kids, you'll find work. After all, my machine will need strong chess player-programmers. You will be the first.",
      "Mikhail Botvinnik (to Karpov & students, 1965)"
    ),
    new Quote(
      "Drawn games are sometimes more scintillating than any conclusive contest.",
      "Savielly Tartakower"
    ),
    new Quote(
      "During a chess tournament a master must envisage himself as a cross between an ascetic monk and a beast of prey.",
      "Alexander Alekhine"
    ),
    new Quote(
      "During the late Victorian period the majority of chess magazines printed increasing numbers of humourous stories, poems and anecdotes about the agonies and idiocies of women chess players, presumably as an antidote to the alarmed reaction of men to the fact that women were encroaching on their 'territory'.",
      "British Chess Magazine"
    ),
    new Quote(
      "Emotional instability can be one of the factors giving rise to a failure by chess players in important duels. Under the influence of surging emotions (and not necessarily negative ones) we sometimes lose concentration and stop objectively evaluating the events that are taking place on the board.",
      "Mark Dvoretsky"
    ),
    new Quote(
      "Endings of one rook and pawns are about the most common sort of endings arising on the chess board. Yet though they do occur so often, few have mastered them thoroughly. They are often of a very difficult nature, and sometimes while apparently very simple they are in reality extremely intricate.",
      "Jose Capablanca"
    ),
    new Quote(
      "Enormous self-belief, intuition, the ability to take a risk at a critical moment and go in for a very dangerous play with counter-chances for the opponent - it is precisely these qualities that distinguish great players.",
      "Garry Kasparov"
    ),
    new Quote(
      "Errors have nothing to do with luck; they are caused by time pressure, discomfort or unfamiliarilty with a position, distractions, feelings of intimidation, nervous tension, overambition, excessive caution, and dozens of other psychological factors.",
      "Pal Benko"
    ),
    new Quote(
      "Even in the King's Gambit ... White is no longer trying to attack at all costs. He has had to adapt his approach and look for moves with a solid positional foundation",
      "Neil McDonald  (1998) "
    ),
    new Quote(
      "Even in the heat of a middlegame battle the master still has to bear in mind the outlines of a possible future ending.",
      "David Bronstein"
    ),
    new Quote(
      "Even the best grandmasters in the world have had to work hard to acquire the technique of rook endings.",
      "Paul Keres"
    ),
    new Quote(
      "Even the most distinguished players have in their careers experienced severe disappointments due to ignorance of the best lines or suspension of their own common sense.",
      "Tigran Petrosian"
    ),
    new Quote(
      "Even when the time control has been reached, there is one situation where you want to act as if it has not: when your position is absolutely lost.",
      "Edmar Mednis"
    ),
    new Quote("Every Chess master was once a beginner.", "Irving Chernev"),
    new Quote(
      "Every great master will find it useful to have his own theory on the openings, which only he himself knows, a theory which is closely linked with plans for the middle game.",
      "Mikhail Botvinnik"
    ),
    new Quote(
      "Every month I look through some ten thousand games, so not as to miss any new ideas and trends.",
      "Vladimir Kramnik"
    ),
    new Quote("Every move creates a weakness.", "Siegbert Tarrasch"),
    new Quote(
      "Excellent ! I will still be in time for the ballet !",
      "Jose Capablanca (upon defeating Ossip Bernstein in the famous 29 move exhibition game played in Moscow in 1914, before setting off to the Bolshoi Theatre in horse-drawn carriage)"
    ),
    new Quote(
      "Excelling at chess has long been considered a symbol of more general intelligence. That is an incorrect assumption in my view, as pleasant as it might be.",
      "Garry Kasparov"
    ),
    new Quote(
      "Experience and the constant analysis of the most varied positions builds up a store of knowledge in a player's mind enabling him often at a glance to assess this or that position.",
      "Alexander Kotov"
    ),
    new Quote(
      "Failing to open the center at the right moment - a common error by White in the Exchange Lopez - can allow Black an excellent game.",
      "Andy Soltis"
    ),
    new Quote("Far from all of the obvious moves that go without saying are correct.", "David Bronstein"),
    new Quote("Few things are as psychologically brutal as chess.", "Garry Kasparov"),
    new Quote(
      "First and foremost it is essential to understand the essence, the overall idea of any fashionable variation, and only then include it in one's repertoire. Otherwise the tactical trees will conceal from the player the strategic picture of the wood, in which his orientation will most likely be lost.",
      "Lev Polugaevsky"
    ),
    new Quote("First restrain, next blockade, lastly destroy.", "Aron Nimzowitsch"),
    new Quote(
      "First-class players lose to second-class players because second-class players sometimes play a first-class game.",
      "Siegbert Tarrasch"
    ),
    new Quote("Fischer is Fischer, but a knight is a knight!", "Mikhail Tal"),
    new Quote(
      "For a game it is too serious, for seriousness too much of a game.",
      "Moses Mendelssohn 1729-86"
    ),
    new Quote("For every door the computers have closed they have opened a new one.", "Viswanathan Anand"),
    new Quote(
      "For me right now I think being the world number one is a bigger deal than being the world champion because I think it shows better who plays the best chess. That sounds self-serving but I think it’s also right. (2012)",
      "Magnus Carlsen"
    ),
    new Quote(
      "For me, chess is a language, and if it's not my native tongue, it is one I learned via the immersion method at a young age.",
      "Garry Kasparov"
    ),
    new Quote(
      "For me, chess is at the same time a game, a sport, a science and an art. And perhaps even more than that,. There is someting hard to explain to those who do not know the game well. One must first learn to play it correctly in order to savor its richness.",
      "Bent Larsen"
    ),
    new Quote(
      "For me, chess is not a profession, it is a way of life, a passion. People may feel that I have conquered the peak and will not have to struggle. Financially, perhaps that is true; but as far as chess goes, I’m still learning a lot!",
      "Viswanathan Anand"
    ),
    new Quote(
      "For my victory over Capablanca I am indebted primarily to my superiority in the field of psychology. Capablanca played, relying almost exclusively on his rich intuitive talent. But for the chess struggle nowadays one needs a subtle knowledge of human nature, an understanding of the opponent's psychology.",
      "Alexander Alekhine"
    ),
    new Quote(
      "For pleasure you can read the games collections of Andersson and Chigorin, but for benefit you should study Tarrasch, Keres and Bronstein.",
      "Mikhail Tal"
    ),
    new Quote(
      "Fortunately I’ve got a weak character, so I never did decide to dedicate myself to only one of my professions. And I’m very glad. After all, if I’d rejected chess or music then my life wouldn’t have been two times, but a hundred times less interesting.",
      "Mark Taimanov"
    ),
    new Quote(
      "From time to time, like many other players, I glance through my own games of earlier years, and return to positions and variations which have gone out of practice. I attempt to restore them, to find new ideas and plans.",
      "Yefim Geller"
    ),
    new Quote(
      "Furman astounded me with his chess depth, a depth which he revealed easily and naturally, as if all he were doing was establishing well-known truths.",
      "Anatoly Karpov"
    ),
    new Quote(
      "GM Naiditsch reckoned that me playing the King's Indian against Anand was something akin to a samurai running at a machine gun with a sword.",
      "Hikaru Nakamura"
    ),
    new Quote(
      "Genius. It's a word. What does it really mean? If I win I'm a genius. If I don't, I'm not.",
      "Bobby Fischer"
    ),
    new Quote(
      "Go through detailed variations in your own time, think in a general way about the position in the opponent's time and you will soon find that you get into time trouble less often, that your games have more content to them, and that their general standard rises.",
      "Alexander Kotov"
    ),
    new Quote(
      "Had I not played the Sicilian with Black I could have saved myself the trouble of studying for more than 20 years all the more popular lines of this opening, which comprise probably more than 25 percent of all published opening theory!",
      "Bent Larsen"
    ),
    new Quote(
      "Has he some psychological antipathy to realism? I am no psychologist, and cannot say. The fact remains that Euwe commits the most inexplicable mistakes in thoroughly favorable positions, and that this weakness has consistently tarnished his record.",
      "Hans Kmoch"
    ),
    new Quote(
      "Haste is never more dangerous than when you feel that victory is in your grasp.",
      "Eugene Znosko-Borovsky"
    ),
    new Quote("Haste, the great enemy.", "Eugene Znosko-Borowski"),
    new Quote(
      "Having spent alarmingly large chunks of my life studying the white side of the Open Sicilian, I find myself asking, why did I bother?",
      "Daniel King"
    ),
    new Quote(
      "He played with enormous energy and great fighting spirit. Offering him a draw was a waste of time. He would decline it politely, but firmly. \"No, thank you,\" he would say and the fight would go on and on and on.",
      "Lubomir Kavalek on Bent Larsen"
    ),
    new Quote(
      "He who has a slight disadvantage plays more attentively, inventively and more boldly than his antagonist who either takes it easy or aspires after too much. Thus a slight disadvantage is very frequently seen to convert into a good, solid advantage.",
      "Emanuel Lasker"
    ),
    new Quote(
      "Here is a definition which correctly reflects the course of thought and action of a grandmaster: The plan in a game of chess is the sum total of successive strategical operations which are each carried out according to separate ideas arising from the demands of the position.",
      "Alexander Kotov"
    ),
    new Quote(
      "How come the little things bother you when you are in a bad position? They don't bother you in good positions.",
      "Yasser Seirawan"
    ),
    new Quote(
      "However hopeless the situation appears to be there yet always exists the possibility of putting up a stubborn resistance.",
      "Paul Keres"
    ),
    new Quote(
      "I ... have two vocations: chess and engineering. If I played chess only, I believe that my success would not have been significantly greater. I can play chess well only when I have fully convalesced from chess and when the 'hunger for chess' once more awakens within me.",
      "Mikhail Botvinnik"
    ),
    new Quote("I always urge players to study composed problems and endgames.", "Pal Benko"),
    new Quote(
      "I am acutely conscious, from vast experience in opens, that guys around, say 2100 or more can definitely play chess and that one often has to work very hard to beat them.",
      "Nigel Short"
    ),
    new Quote(
      "I am both sad and pleased that in his last tournament, Rashid Gibiatovich came to my home in Latvia. He did not take first place, but the prize for beauty, as always, he took with him. Players die, tournaments are forgotten, but the works of great artists are left behind them to live on forever. (on Nezhmetdinov)",
      "Mikhail Tal"
    ),
    new Quote(
      "I am pleased that in a match for the World Championship I was able to conduct a game in the style of Akiba Rubinstein, where the entire strategic course was maintained from the first to the last move. (on Game 7 of his 2012 match with Anand)",
      "Boris Gelfand"
    ),
    new Quote(
      "I am trying to beat the guy sitting across from me and trying to choose the moves that are most unpleasant for him and his style.",
      "Magnus Carlsen"
    ),
    new Quote(
      "I believe in magic ... There is magic in the creative faculty such as great poets and philosophers conspicuously possess, and equally in the creative chessmaster.",
      "Emanuel Lasker"
    ),
    new Quote(
      "I believe most definitely that one must not only grapple with the problems on the board, one must also make every effort to combat the thoughts and will of the opponent.",
      "Mikhail Tal"
    ),
    new Quote(
      "I believe that the best style is a universal one, tactical and positional at the same time ...",
      "Susan Polgar"
    ),
    new Quote(
      "I cannot claim to thoroughly enjoy coaching, because it is very hard work if you are even moderately conscientious. Nevertheless it does provide a degree of satisfaction, not to mention a steady income, which is why I do it occasionally.",
      "Nigel Short"
    ),
    new Quote(
      "I cannot think that a player genuinely loving the game can get pleasure just from the number of points scored no matter how impressive the total. I will not speak of myself, but for the masters of the older generation, from whose games we learned, the aesthetic side was the most important.",
      "Alexander Kotov"
    ),
    new Quote(
      "I can’t count the times I have lagged seemingly hopelessly far behind, and nobody except myself thinks I can win. But I have pulled myself in from desperate [situations]. When you are behind there are two strategies – counter-attack or all men to the defences. I’m good at finding the right balance between those.",
      "Magnus Carlsen"
    ),
    new Quote(
      "I claim that nothing else is so effective in encouraging the growth of chess strength as such independent analysis, both of the games of the great players and your own.",
      "Mikhail Botvinnik"
    ),
    new Quote("Watch out for the tricky knights.", "ChessNetwork"),
    new Quote("I think crazyhouse improves your standard chess.", "ChessNetwork"),
    new Quote(
      "The biggest tool for chess improvement would be playing against stronger opposition",
      "Peter Svidler"
    ),
    new Quote(
      "Chess is a battle between your aversion to thinking and your aversion to losing.",
      "Anonymous"
    ),
    new Quote("It was once said that Tal sacrificed 9 pawns for an attack", "Mato"),
    new Quote("Be well enough prepared that preparation won't play a role.", "Magnus Carlsen"),
    new Quote("I don't study; I create.", "Viktor Korchnoi"),
    new Quote(
      "During the analysis, I discovered something very remarkable: the board is simply too small for two Queens of the same color. They only get in each other's way. I realize that this might sound stupid, but I fully mean it. The advantage is much less than one would expect by counting material.",
      "Viktor Korchnoi"
    ),
    new Quote("You'll be amazed at the people I've lost to while playing online.", "Magnus Carlsen"),
    new Quote(
      "[...], even extremely intoxicated my chess strength and knowledge is still in my bones.",
      "Magnus Carlsen"
    ),
    new Quote(
      "I don't play unorthodox openings. I prefer to give mainstream openings my own spin.",
      "Magnus Carlsen"
    ),
    new Quote(
      "Playing long games online just takes too much time. It's fun to play blitz once in a while, where you can rely more on your intuition, your instincts rather than pure calculation and analysis.",
      "Magnus Carlsen"
    ),
    new Quote("Fortune favors the lucky!", "Robert Houdart (Houdini author)"),
    new Quote("I don't berserk, I am not a caveman", "Magnus Carlsen"),
    new Quote(
      "1.e4 is the move you play when you're young, naive, and believe the world owes you something. Open positions, infinite horizons - what's not to love? Well, I've got news for you, buddy: it's a cruel chess board out there.",
      "John Bartholomew"
    ),
    new Quote("Chess as a game is too serious; as a serious pursuit too frivolous.", "Moses Mendelssohn"),
    new Quote("Chess makes me a better person", "Albert Badosa"),
    // lichess facts
    new Quote("All features for free; for everyone; forever.", "lichess.org"),
    new Quote("We will never display ads.", "lichess.org"),
    new Quote("We do not track you. It's a rare feature, nowadays.", "lichess.org"),
    new Quote("Every chess player is a premium user.", "lichess.org"),
    new Quote("I never lose. I either win or learn.", "Nelson Mandela")
  )

  implicit def quoteWriter: OWrites[Quote] =
    OWrites { q =>
      Json.obj(
        "text"   -> q.text,
        "author" -> q.author
      )
    }
}
