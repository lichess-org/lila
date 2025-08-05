package lila.quote

import play.api.libs.json.*

import scala.util.Random

final class Quote(val text: String, val author: String)

object Quote:

  def one(seed: String) = all(Random(seed.hashCode).nextInt(all.size))

  // courtesy of http://www.chess-poster.com/english/notes_and_facts/chess_quotes.htm
  // and other various sources
  val all = Vector(
    Quote("When you see a good move, look for a better one.", "Emanuel Lasker"),
    Quote("Nothing excites jaded Grandmasters more than a theoretical novelty.", "Dominic Lawson"),
    Quote("The Pin is mightier than the sword.", "Fred Reinfeld"),
    Quote(
      "We cannot resist the fascination of sacrifice, since a passion for sacrifices is part of a chess player's nature.",
      "Rudolf Spielmann"
    ),
    Quote("All I want to do, ever, is just play chess.", "Bobby Fischer"),
    Quote(
      "A win by an unsound combination, however showy, fills me with artistic horror.",
      "Wilhelm Steinitz"
    ),
    Quote(
      "The chessboard is the world, the pieces are the phenomena of the Universe, the rules of the game are what we call the laws of Nature and the player on the other side is hidden from us.",
      "Thomas Huxley"
    ),
    Quote(
      "Adequate compensation for a sacrifice is having a sound combination leading to a winning position; adequate compensation for a blunder is having your opponent snatch defeat from the jaws of victory.",
      "Bruce A. Moon"
    ),
    Quote("Strategy requires thought, tactics require observation.", "Max Euwe"),
    Quote("I don't believe in psychology. I believe in good moves.", "Bobby Fischer"),
    Quote(
      "Life is a kind of chess, with struggle, competition, good and ill events.",
      "Benjamin Franklin"
    ),
    Quote("Even the laziest king flees wildly in the face of a double check!", "Aron Nimzowitsch"),
    Quote(
      "Combinations have always been the most intriguing aspect of chess. The masters look for them, the public applauds them, the critics praise them. It is because combinations are possible that chess is more than a lifeless mathematical exercise. They are the poetry of the game; they are to chess what melody is to music. They represent the triumph of mind over matter.",
      "Reuben Fine"
    ),
    Quote("Chess is a fairy tale of 1001 blunders.", "Savielly Tartakower"),
    Quote(
      "Chess is no whit inferior to the violin, and we have a large number of professional violinists.",
      "Mikhail Botvinnik"
    ),
    Quote("Only the player with the initiative has the right to attack.", "Wilhelm Steinitz"),
    Quote(
      "The winner of the game is the player who makes the next-to-last mistake.",
      "Savielly Tartakower"
    ),
    Quote(
      "Your body has to be in top condition. Your chess deteriorates as your body does. You can't separate body from mind.",
      "Bobby Fischer"
    ),
    Quote(
      "Of chess it has been said that life is not long enough for it, but that is the fault of life, not chess.",
      "William Ewart Napier"
    ),
    Quote(
      "I have added these principles to the law: get the knights into action before both bishops are developed.",
      "Emanuel Lasker"
    ),
    Quote("Life is like a game of chess, changing with each move.", "Chinese proverb"),
    Quote("You cannot play at chess if you are kind-hearted.", "French proverb"),
    Quote(
      "It's just you and your opponent at the board and you’re trying to prove something.",
      "Bobby Fischer"
    ),
    Quote(
      "It is the aim of the modern school, not to treat every position according to one general law, but according to the principle inherent in the position.",
      "Richard Réti"
    ),
    Quote("The pawns are the soul of the game.", "François-André Danican Philidor"),
    Quote(
      "In order to improve your game, you must study the endgame before everything else, for whereas the endings can be studied and mastered by themselves, the middlegame and the opening must be studied in relation to the endgame.",
      "José Raúl Capablanca"
    ),
    Quote("Without error there can be no brilliancy.", "Emanuel Lasker"),
    Quote("Chess is like war on a board.", "Bobby Fischer"),
    Quote("Chess is played with the mind and not with the hands!", "Renaud and Kahn"),
    Quote("Many have become chess masters, no one has become the master of chess.", "Siegbert Tarrasch"),
    Quote(
      "The most important feature of the chess position is the activity of the pieces. This is absolutely fundamental in all phases of the game: Opening, Middlegame and especially Endgame. The primary constraint on a piece's activity is the pawn structure.",
      "Michael Stean"
    ),
    Quote(
      "You have to have the fighting spirit. You have to force moves and take chances.",
      "Bobby Fischer"
    ),
    Quote(
      "Could we look into the head of a chess player, we should see there a whole world of feelings, images, ideas, emotion and passion.",
      "Alfred Binet"
    ),
    Quote("Openings teach you openings. Endgames teach you chess!", "Stephan Gerzadowicz"),
    Quote("My style is somewhere between that of Tal and Petrosian.", "Samuel Reshevsky"),
    Quote(
      "Play the opening like a book, the middlegame like a magician, and the endgame like a machine.",
      "Rudolf Spielmann"
    ),
    Quote(
      "That's what chess is all about. One day you give your opponent a lesson, the next day he gives you one.",
      "Bobby Fischer"
    ),
    Quote("Some part of a mistake is always correct.", "Savielly Tartakower"),
    Quote("Methodical thinking is of more use in chess than inspiration.", "Cecil Purdy"),
    Quote("When in doubt... play chess!", "Walter Tevis"),
    Quote(
      "Who is your opponent tonight, tonight I am playing against the black pieces.",
      "Akiba Rubinstein"
    ),
    Quote("Excellence at chess is one mark of a scheming mind.", "Sir Arthur Conan Doyle"),
    Quote("A bad day of chess is better than any good day at work.", "Anonymous"),
    Quote("Chess is the art of analysis.", "Mikhail Botvinnik"),
    Quote("The mistakes are there, waiting to be made.", "Savielly Tartakower"),
    Quote(
      "After Black's reply to 1.e4 with 1...e5, leaves him always trying to get into the game.",
      "Howard Staunton"
    ),
    Quote("A player surprised is half beaten.", "Proverb"),
    Quote(
      "A passed pawn increases in strength as the number of pieces on the board diminishes.",
      "José Raúl Capablanca"
    ),
    Quote("The essence of chess is thinking about what chess is.", "David Bronstein"),
    Quote("I am the best player in the world and I am here to prove it.", "Bobby Fischer"),
    Quote(
      "Chess is a forcing house where the fruits of character can ripen more fully than in life.",
      "Edward Morgan Foster"
    ),
    Quote(
      "Half the variations which are calculated in a tournament game turn out to be completely superfluous. Unfortunately, no one knows in advance which half.",
      "Jan Timman"
    ),
    Quote("Good positions don't win games, good moves do.", "Gerald Abrahams"),
    Quote(
      "If I win a tournament, I win it by myself. I do the playing. Nobody helps me.",
      "Bobby Fischer"
    ),
    Quote("What would chess be without silly mistakes?", "Kurt Richter"),
    Quote("Before the endgame, the Gods have placed the middle game.", "Siegbert Tarrasch"),
    Quote("Chess was Capablanca's mother tongue.", "Richard Réti"),
    Quote(
      "Alekhine is a poet who creates a work of art out of something that would hardly inspire another man to send home a picture post card.",
      "Max Euwe"
    ),
    Quote(
      "During a chess competition a chess master should be a combination of a beast of prey and a monk.",
      "Alexander Alekhine"
    ),
    Quote("No one ever won a game by resigning.", "Savielly Tartakower"),
    Quote("The defensive power of a pinned piece is only imaginary.", "Aron Nimzowitsch"),
    Quote("When the chess game is over, the pawn and the king go back to the same box.", "Irish saying"),
    Quote(
      "A strong memory, concentration, imagination, and a strong will is required to become a great chess player.",
      "Bobby Fischer"
    ),
    Quote("Every chess master was once a beginner.", "Irving Chernev"),
    Quote(
      "One doesn't have to play well, it’s enough to play better than your opponent.",
      "Siegbert Tarrasch"
    ),
    Quote("Chess is above all, a fight!", "Emanuel Lasker"),
    Quote("Discovered check is the dive bomber of the chessboard.", "Reuben Fine"),
    Quote(
      "I know people who have all the will in the world, but still can't play good chess.",
      "Bobby Fischer"
    ),
    Quote(
      "A chess game is a dialogue, a conversation between a player and his opponent. Each move by the opponent may contain threats or be a blunder, but a player cannot defend against threats or take advantage of blunders if he does not first ask himself: What is my opponent planning after each move?",
      "Bruce A. Moon"
    ),
    Quote("The hardest game to win is a won game.", "Emanuel Lasker"),
    Quote("The most powerful weapon in chess is to have the next move.", "David Bronstein"),
    Quote("He who fears an isolated queen's pawn should give up chess.", "Siegbert Tarrasch"),
    Quote("Different people feel differently about resigning.", "Bobby Fischer"),
    Quote("Chess is not like life... it has rules!", "Mark Pasternak"),
    Quote("It's always better to sacrifice your opponent’s men.", "Savielly Tartakower"),
    Quote("To avoid losing a piece, many a person has lost the game.", "Savielly Tartakower"),
    Quote("All that matters on the chessboard is good moves.", "Bobby Fischer"),
    Quote("Help your pieces so they can help you.", "Paul Morphy"),
    Quote("In a gambit you give up a pawn for the sake of getting a lost game.", "Samuel Standidge Boden"),
    Quote("It is not enough to be a good player... you must also play well.", "Siegbert Tarrasch"),
    Quote("A sacrifice is best refuted by accepting it.", "Wilhelm Steinitz"),
    Quote("Tactics flow from a superior position.", "Bobby Fischer"),
    Quote(
      "Later, I began to succeed in decisive games. Perhaps because I realized a very simple truth: not only was I worried, but also my opponent.",
      "Mikhail Tal"
    ),
    Quote("Chess is life.", "Bobby Fischer"),
    Quote("Chess is a beautiful mistress.", "Bent Larsen"),
    Quote("Some sacrifices are sound; the rest are mine.", "Mikhail Tal"),
    Quote("Best by test: 1. e4.", "Bobby Fischer"),
    Quote("A bad plan is better than none at all.", "Frank Marshall"),
    Quote(
      "Chess books should be used as we use glasses: to assist the sight, although some players make use of them as if they thought they conferred sight.",
      "José Raúl Capablanca"
    ),
    Quote("There are two types of sacrifices: correct ones and mine.", "Mikhail Tal"),
    Quote("Morphy was probably the greatest genius of them all.", "Bobby Fischer"),
    Quote(
      "My opponents make good moves too. Sometimes I don't take these things into consideration.",
      "Bobby Fischer"
    ),
    Quote(
      "The combination player thinks forward; he starts from the given position, and tries the forceful moves in his mind.",
      "Emanuel Lasker"
    ),
    Quote(
      "A chess game is divided into three stages: the first, when you hope you have the advantage, the second when you believe you have an advantage, and the third... when you know you're going to lose!",
      "Savielly Tartakower"
    ),
    Quote("Chess demands total concentration.", "Bobby Fischer"),
    Quote("Chess, like love, like music, has the power to make people happy.", "Siegbert Tarrasch"),
    Quote("All my games are real.", "Bobby Fischer"),
    Quote("Chess is everything: art, science and sport.", "Anatoly Karpov"),
    Quote("Chess is the art which expresses the science of logic.", "Mikhail Botvinnik"),
    Quote("Not all artists are chess players, but all chess players are artists.", "Marcel Duchamp"),
    Quote("Chess is imagination.", "David Bronstein"),
    Quote(
      "Chess is thirty to forty percent psychology. You don't have this when you play a computer. I can’t confuse it.",
      "Judit Polgar"
    ),
    Quote("On the chessboard, lies and hypocrisy do not survive long.", "Emanuel Lasker"),
    Quote("Chess is war over the board. The object is to crush the opponents mind.", "Bobby Fischer"),
    Quote(
      "The passed pawn is a criminal, who should be kept under lock and key. Mild measures, such as police surveillance, are not sufficient.",
      "Aron Nimzowitsch"
    ),
    Quote(
      "Chess holds its master in its own bonds, shackling the mind and brain so that the inner freedom of the very strongest must suffer.",
      "Albert Einstein"
    ),
    Quote(
      "Human affairs are like a chess game: only those who do not take it seriously can be called good players.",
      "Hung Tzu Ch'eng"
    ),
    Quote("The blunders are all there on the board, waiting to be made.", "Savielly Tartakower"),
    Quote(
      "Via the squares on the chessboard, the Indians explain the movement of time and the age, the higher influences which control the world and the ties which link chess with the human soul.",
      "Al-Masudi"
    ),
    Quote("It is no time to be playing chess when the house is on fire.", "Italian proverb"),
    Quote(
      "You sit at the board and suddenly your heart leaps. Your hand trembles to pick up the piece and move it. But what chess teaches you is that you must sit there calmly and think about whether it's really a good idea and whether there are other better ideas.",
      "Stanley Kubrick"
    ),
    Quote(
      "Daring ideas are like chess men moved forward. They may be beaten, but they may start a winning game.",
      "Johann Wolfgang von Goethe"
    ),
    Quote(
      "Of all my Russian books, The Defense contains and diffuses the greatest 'warmth’ which may seem odd seeing how supremely abstract chess is supposed to be.",
      "Vladimir Nabokov"
    ),
    Quote(
      "For surely of all the drugs in the world, chess must be the most permanently pleasurable.",
      "Assiac"
    ),
    Quote(
      "A thorough understanding of the typical mating continuations makes the most complicated sacrificial combinations leading up to them not only not difficult, but almost a matter of course.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "Chess problems demand from the composer the same virtues that characterize all worthwhile art: originality, invention, conciseness, harmony, complexity, and splendid insincerity.",
      "Vladimir Nabokov"
    ),
    Quote(
      "Personally, I rather look forward to a computer program winning the World Chess Championship. Humanity needs a lesson in humility.",
      "Richard Dawkins"
    ),
    Quote(
      "The boy (then a 12 year old boy named Anatoly Karpov) doesn't have a clue about chess, and there’s no future at all for him in this profession.",
      "Mikhail Botvinnik"
    ),
    Quote("As one by one I mowed them down, my superiority soon became apparent.", "José Raúl Capablanca"),
    Quote(
      "Though most people love to look at the games of the great attacking masters, some of the most successful players in history have been the quiet positional players. They slowly grind you down by taking away your space, tying up your pieces, and leaving you with virtually nothing to do!",
      "Yasser Seirawan"
    ),
    Quote(
      "There must have been a time when men were demigods, or they could not have invented chess.",
      "Gustav Schenk"
    ),
    Quote("Chess is really ninety nine percent calculation.", "Andrew Soltis"),
    Quote("Chess is the gymnasium of the mind.", "Blaise Pascal"),
    Quote(
      "The game of chess is not merely an idle amusement; several very valuable qualities of the mind are to be acquired and strengthened by it, so as to become habits ready on all occasions; for life is a kind of chess.",
      "Benjamin Franklin"
    ),
    Quote("Winning isn't everything... but losing is nothing.", "Mednis"),
    Quote(
      "Look at Garry Kasparov. After he loses, invariably he wins the next game. He just kills the next guy. That's something that we have to learn to be able to do.",
      "Maurice Ashley"
    ),
    Quote("There just isn't enough televised chess.", "David Letterman"),
    Quote(
      "Avoid the crowd. Do your own thinking independently. Be the chess player, not the chess piece.",
      "Ralph Charell"
    ),
    Quote(
      "Chess is a terrible game. If you have no center, your opponent has a freer position. If you do have a center, then you really have something to worry about!",
      "Siegbert Tarrasch"
    ),
    Quote(
      "Any material change in a position must come about by mate, a capture, or a pawn promotion.",
      "Cecil Purdy"
    ),
    Quote(
      "We don't really know how the game was invented, though there are suspicions. As soon as we discover the culprits, we’ll let you know.",
      "Bruce Pandolfini"
    ),
    Quote(
      "The battle for the ultimate truth will never be won. And that's why chess is so fascinating.",
      "Hans Kmoch"
    ),
    Quote(
      "I am still a victim of chess. It has all the beauty of art and much more. It cannot be commercialized. Chess is much purer than art in its social position.",
      "Marcel Duchamp"
    ),
    Quote("Blessed be the memory of him who gave the world this immortal game.", "A. G. Gardiner"),
    Quote(
      "In the perfect chess combination as in a first-rate short story, the whole plot and counter-plot should lead up to a striking finale, the interest not being allayed until the very last moment.",
      "Yates and Winter"
    ),
    Quote("Castle early and often.", "Rob Sillars"),
    Quote(
      "I believe that chess possesses a magic that is also a help in advanced age. A rheumatic knee is forgotten during a game of chess and other events can seem quite unimportant in comparison with a catastrophe on the chessboard.",
      "Vlastimil Hort"
    ),
    Quote(
      "Chess is a more highly symbolic game, but the aggressions are therefore even more frankly represented in the play. It probably began as a war game; that is, the representation of a miniature battle between the forces of two kingdoms.",
      "Karl Meninger"
    ),
    Quote(
      "No chess Grandmaster is normal; they only differ in the extent of their madness.",
      "Viktor Korchnoi"
    ),
    Quote("Chess is 99 percent tactics.", "Richard Teichmann"),
    Quote("I'd rather have a pawn than a finger.", "Reuben Fine"),
    Quote("Chess mastery essentially consists of analyzing.", "Mikhail Botvinnik"),
    Quote(
      "If your opponent cannot do anything active, then don't rush the position; instead you should let him sit there, suffer, and beg you for a draw.",
      "Jeremy Silman"
    ),
    Quote(
      "The chess pieces are the block alphabet which shapes thoughts; and these thoughts, although making a visual design on the chessboard, express their beauty abstractly, like a poem.",
      "Marcel Duchamp"
    ),
    Quote(
      "Examine moves that smite! A good eye for smites is far more important than a knowledge of strategical principles.",
      "Cecil Purdy"
    ),
    Quote("Chess is like life.", "Boris Spassky"),
    Quote(
      "Chess teaches you to control the initial excitement you feel when you see something that looks good and it trains you to think objectively when you're in trouble.",
      "Stanley Kubrick"
    ),
    Quote("Let the perfectionist play postal.", "Yasser Seirawan"),
    Quote(
      "If chess is a science, it's a most inexact one. If chess is an art, it is too exacting to be seen as one. If chess is a sport, it’s too esoteric. If chess is a game, it’s too demanding to be just a game. If chess is a mistress, she’s a demanding one. If chess is a passion, it’s a rewarding one. If chess is life, it’s a sad one.",
      "Anonymous"
    ),
    Quote(
      "Chess is a foolish expedient for making idle people believe they are doing something very clever when they are only wasting their time.",
      "George Bernard Shaw"
    ),
    Quote(
      "You must take your opponent into a deep dark forest where 2+2=5, and the path leading out is only wide enough for one.",
      "Mikhail Tal"
    ),
    Quote(
      "I feel as if I were a piece in a game of chess, when my opponent says of it: That piece cannot be moved.",
      "Søren Kierkegaard"
    ),
    Quote(
      "When your house is on fire, you can't be bothered with the neighbors. Or, as we say in chess, if your king is under attack you don't worry about losing a pawn on the queen’s side.",
      "Garry Kasparov"
    ),
    Quote(
      "Man is a frivolous, a specious creature, and like a chess player, cares more for the process of attaining his goal than for the goal itself.",
      "Fyodor Dostoyevsky"
    ),
    Quote(
      "When asked, -How is that you pick better moves than your opponents?, I responded: I'm very glad you asked me that, because, as it happens, there is a very simple answer. I think up my own moves, and I make my opponent think up his.",
      "Alexander Alekhine"
    ),
    Quote("Mistrust is the most necessary characteristic of the chess player.", "Siegbert Tarrasch"),
    Quote(
      "What is the object of playing a gambit opening...? To acquire a reputation of being a dashing player at the cost of losing a game.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "Pawns; they are the soul of this game, they alone form the attack and defense.",
      "François-André Danican Philidor"
    ),
    Quote("In chess, at least, the brave inherit the earth.", "Edmar Mednis"),
    Quote(
      "There are two classes of men; those who are content to yield to circumstances and who play whist; those who aim to control circumstances, and who play chess.",
      "Mortimer Collins"
    ),
    Quote(
      "The tactician must know what to do whenever something needs doing; the strategist must know what to do when nothing needs doing.",
      "Savielly Tartakower"
    ),
    Quote("All chess players should have a hobby.", "Savielly Tartakower"),
    Quote(
      "I played chess with him and would have beaten him sometimes only he always took back his last move, and ran the game out differently.",
      "Mark Twain"
    ),
    Quote("In chess, just as in life, today's bliss may be tomorrow’s poison.", "Assiac"),
    Quote(
      "You may learn much more from a game you lose than from a game you win. You will have to lose hundreds of games before becoming a good player.",
      "José Raúl Capablanca"
    ),
    Quote("The way he plays chess demonstrates a man's whole nature.", "Stanley Ellin"),
    Quote("You can only get good at chess if you love the game.", "Bobby Fischer"),
    Quote("A man that will take back a move at chess will pick a pocket.", "Richard Fenton"),
    Quote(
      "Whoever sees no other aim in the game than that of giving checkmate to one's opponent will never become a good chess player.",
      "Max Euwe"
    ),
    Quote("In blitz, the knight is stronger than the bishop.", "Vlastimil Hort"),
    Quote("Chess is a fighting game which is purely intellectual and includes chance.", "Richard Réti"),
    Quote("Chess is a sea in which a gnat may drink and an elephant may bathe.", "Hindu proverb"),
    Quote("Pawn endings are to chess what putting is to golf.", "Cecil Purdy"),
    Quote("Chess opens and enriches your mind.", "Saudin Robovic"),
    Quote("The isolated pawn casts gloom over the entire chessboard.", "Aron Nimzowitsch"),
    Quote(
      "For me, chess is life and every game is like a life. Every chess player gets to live many lives in one lifetime.",
      "Eduard Gufeld"
    ),
    Quote("Chess is a terrific way for kids to build self image and self esteem.", "Saudin Robovic"),
    Quote("If a ruler does not understand chess, how can he rule over a kingdom?", "King Khusros II"),
    Quote("Chess is a cold bath for the mind.", "Sir John Simon"),
    Quote(
      "Becoming successful at chess allows you to discover your own personality. That's what I want for the kids I teach.",
      "Saudin Robovic"
    ),
    Quote(
      "Chess is so inspiring that I do not believe a good player is capable of having an evil thought during the game.",
      "Wilhelm Steinitz"
    ),
    Quote("You are for me the queen on d8 and I am the pawn on d7!!", "GM Eduard Gufeld"),
    Quote(
      "By playing at chess then, we may learn: First: Foresight. Second: Circumspection. Third: Caution. And lastly, we learn by chess the habit of not being discouraged by present bad appearances in the state of our affairs, the habit of hoping for a favorable chance, and that of persevering in the secrets of resources.",
      "Benjamin Franklin"
    ),
    Quote("I prefer to lose a really good game than to win a bad one.", "David Levy"),
    Quote(
      "Capture of the adverse king is the ultimate but not the first object of the game.",
      "Wilhelm Steinitz"
    ),
    Quote(
      "When I have white, I win because I am White; When I have black, I win because I am Bogolyubov.",
      "Efim Bogolyubov"
    ),
    Quote("Every pawn is a potential queen.", "James Mason"),
    Quote(
      "Chess is in its essence a game, in its form an art, and in its execution a science.",
      "Baron von der Lasa"
    ),
    Quote("No price is too great for the scalp of the enemy king.", "Koblentz"),
    Quote(
      "In life, as in chess, ones own pawns block ones way. A mans very wealth, ease, leisure, children, books, which should help him to win, more often checkmate him.",
      "Charles Buxton"
    ),
    Quote(
      "Chess is a part of culture and if a culture is declining then chess too will decline.",
      "Mikhail Botvinnik"
    ),
    Quote(
      "A good sacrifice is one that is not necessarily sound but leaves your opponent dazed and confused.",
      "Rudolf Spielmann"
    ),
    Quote(
      "Chess, like any creative activity, can exist only through the combined efforts of those who have creative talent, and those who have the ability to organize their creative work.",
      "Mikhail Botvinnik"
    ),
    Quote("One bad move nullifies forty good ones.", "I. A. Horowitz"),
    Quote(
      "Place the contents of the chess box in a hat, shake them up vigorously, pour them on the board from a height of two feet, and you get the style of Steinitz.",
      "H. E. Bird"
    ),
    Quote(
      "I have never in my life played the French Defence, which is the dullest of all openings.",
      "Wilhelm Steinitz"
    ),
    Quote("Pawns are born free, yet they are everywhere in chains.", "Rick Kennedy"),
    Quote(
      "It is not a move, even the best move that you must seek, but a realizable plan.",
      "Eugene Znosko-Borovsky"
    ),
    Quote("Those who say they understand chess, understand nothing.", "Robert Hübner"),
    Quote("Good offense and good defense both begin with good development.", "Bruce A. Moon"),
    Quote(
      "Botvinnik tried to take the mystery out of chess, always relating it to situations in ordinary life. He used to call chess a typical inexact problem similar to those which people are always having to solve in everyday life.",
      "Garry Kasparov"
    ),
    Quote("A good player is always lucky.", "José Raúl Capablanca"),
    Quote(
      "The sign of a great master is his ability to win a won game quickly and painlessly.",
      "Irving Chernev"
    ),
    Quote(
      "One of these modest little moves may be more embarrassing to your opponent than the biggest threat.",
      "Siegbert Tarrasch"
    ),
    Quote("Live, lose, and learn, by observing your opponent how to win.", "Amber Steenbock"),
    Quote("The older I grow, the more I value pawns.", "Paul Keres"),
    Quote("Everything is in a state of flux, and this includes the world of chess.", "Mikhail Botvinnik"),
    Quote(
      "The beauty of a move lies not in its appearance but in the thought behind it.",
      "Aron Nimzowitsch"
    ),
    Quote("My God, Bobby Fischer plays so simply.", "Alexei Suetin"),
    Quote("You need not play well - just help your opponent to play badly.", "Genrikh Chepukaitis"),
    Quote(
      "It is difficult to play against Einstein's theory --on his first loss to Fischer.",
      "Mikhail Tal"
    ),
    Quote("The only thing chess players have in common is chess.", "Lodewijk Prins"),
    Quote("Bobby just drops the pieces and they fall on the right squares.", "Miguel Najdorf"),
    Quote(
      "We must make sure that chess will not be like a dead language, very interesting, but for a very small group.",
      "Sytze Faber"
    ),
    Quote("The passion for playing chess is one of the most unaccountable in the world.", "H. G. Wells"),
    Quote(
      "Chess is so interesting in itself, as not to need the view of gain to induce engaging in it; and thence it is never played for money.",
      "Benjamin Franklin"
    ),
    Quote(
      "The enormous mental resilience, without which no chess player can exist, was so much taken up by chess that he could never free his mind of this game.",
      "Albert Einstein"
    ),
    Quote("Nowadays, when you're not a Grandmaster at 14, you can forget about it.", "Viswanathan Anand"),
    Quote(
      "Do you realize Fischer almost never has any bad pieces? He exchanges them, and the bad pieces remain with his opponents.",
      "Yuri Balashov"
    ),
    Quote("It is always better to sacrifice your opponent's men.", "Savielly Tartakower"),
    Quote("In chess, as it is played by masters, chance is practically eliminated.", "Emanuel Lasker"),
    Quote(
      "You know you're going to lose. Even when I was ahead I knew I was going to lose --on playing against Fischer.",
      "Andrew Soltis"
    ),
    Quote(
      "I won't play with you anymore. You have insulted my friend! --when an opponent cursed himself for a blunder.",
      "Miguel Najdorf"
    ),
    Quote(
      "You know, comrade Pachman, I don't enjoy being a Minister, I would rather play chess like you.",
      "Che Guevara"
    ),
    Quote(
      "It began to feel as though you were playing against chess itself --on playing against Bobby Fischer.",
      "Walter Shipman"
    ),
    Quote(
      "When you play Bobby, it is not a question if you win or lose. It is a question if you survive.",
      "Boris Spassky"
    ),
    Quote("When you absolutely don't know what to do anymore, it is time to panic.", "John van der Wiel"),
    Quote("We like to think.", "Garry Kasparov"),
    Quote("Dazzling combinations are for the many, shifting wood is for the few.", "Georg Kieninger"),
    Quote("In complicated positions, Bobby Fischer hardly had to be afraid of anybody.", "Paul Keres"),
    Quote(
      "It was clear to me that the vulnerable point of the American Grandmaster (Bobby Fischer) was in double-edged, hanging, irrational positions, where he often failed to find a win even in a won position.",
      "Efim Geller"
    ),
    Quote(
      "I love all positions. Give me a difficult positional game, I will play it. But totally won positions, I cannot stand them.",
      "Hein Donner"
    ),
    Quote(
      "In Fischer's hands, a slight theoretical advantage is as good a being a queen ahead.",
      "Isaac Kashdan"
    ),
    Quote(
      "Bobby Fischer's current state of mind is indeed a tragedy. One of the worlds greatest chess players - the pride and sorrow of American chess.",
      "Frank Brady"
    ),
    Quote("Fischer is an American chess tragedy on par with Morphy and Pillsbury.", "Mig Greengard"),
    Quote(
      "Nonsense was the last thing Fischer was interested in, as far as chess was concerned.",
      "Elie Agur"
    ),
    Quote(
      "Fischer is the strongest player in the world. In fact, the strongest player who ever lived.",
      "Larry Evans"
    ),
    Quote("If you aren't afraid of Spassky, then I have removed the element of money.", "Jim Slater"),
    Quote("I guess a certain amount of temperament is expected of chess geniuses.", "Ron Gross"),
    Quote(
      "Fischer sacrificed virtually everything most of us weakies (to use his term) value, respect, and cherish, for the sake of an artful, often beautiful board game, for the ambivalent privilege of being its greatest master.",
      "Paul Kollar"
    ),
    Quote(
      "Fischer chess play was always razor-sharp, rational and brilliant. One of the best ever.",
      "Dave Regis"
    ),
    Quote("Fischer wanted to give the Russians a taste of their own medicine.", "Larry Evans"),
    Quote(
      "With or without the title, Bobby Fischer was unquestionably the greatest player of his time.",
      "Burt Hochberg"
    ),
    Quote(
      "Fischer is completely natural. He plays no roles. He's like a child. Very, very simple.",
      "Zita Rajcsanyi"
    ),
    Quote("Spassky will not be psyched out by Fischer.", "Mike Goodall"),
    Quote(
      "Already at 15 years of age he was a Grandmaster, a record at that time, and his battle to reach the top was the background for all the major chess events of the 1960.",
      "Tim Harding"
    ),
    Quote(
      "Fischer, who may or may not be mad as a hatter, has every right to be horrified.",
      "Jeremy Silman"
    ),
    Quote(
      "When I asked Fischer why he had not played a certain move in our game, he replied: ‘Well, you laughed when I wrote it down!'",
      "Mikhail Tal"
    ),
    Quote("I look one move ahead... the best!", "Siegbert Tarrasch"),
    Quote("Fischer prefers to enter chess history alone.", "Miguel Najdorf"),
    Quote(
      "Bobby is the most misunderstood, misquoted celebrity walking the face of this earth.",
      "Yasser Seirawan"
    ),
    Quote(
      "When you don't know what to play, wait for an idea to come into your opponent’s mind. You may be sure that idea will be wrong.",
      "Siegbert Tarrasch"
    ),
    Quote("There is no remorse like the remorse of chess.", "H. G. Wells"),
    Quote(
      "By this measure (on the gap between Fischer & his contemporaries), I consider him the greatest world champion.",
      "Garry Kasparov"
    ),
    Quote(
      "By the beauty of his games, the clarity of his play, and the brilliance of his ideas, Fischer made himself an artist of the same stature as Brahms, Rembrandt, and Shakespeare.",
      "David Levy"
    ),
    Quote(
      "Many chess players were surprised when after the game, Fischer quietly explained: 'I had already analyzed this possibility’ in a position which I thought was not possible to foresee from the opening.",
      "Mikhail Tal"
    ),
    Quote(
      "Suddenly it was obvious to me in my analysis I had missed what Fischer had found with the greatest of ease at the board.",
      "Mikhail Botvinnik"
    ),
    Quote("The king is a fighting piece. Use it!", "Wilhelm Steinitz"),
    Quote("Bobby Fischer is the greatest chess genius of all time!", "Alexander Kotov"),
    Quote(
      "The laws of chess do not permit a free choice: you have to move whether you like it or not.",
      "Emanuel Lasker"
    ),
    Quote(
      "First-class players lose to second-class players because second-class players sometimes play a first-class game.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "Bobby is the finest chess player this country ever produced. His memory for the moves, his brilliance in dreaming up combinations, and his fierce determination to win are uncanny.",
      "John Collins"
    ),
    Quote(
      "After a bad opening, there is hope for the middle game. After a bad middle game, there is hope for the endgame. But once you are in the endgame, the moment of truth has arrived.",
      "Edmar Mednis"
    ),
    Quote(
      "Weak points or holes in the opponent's position must be occupied by pieces not pawns.",
      "Siegbert Tarrasch"
    ),
    Quote("There is only one thing Fischer does in chess without pleasure: to lose!", "Boris Spassky"),
    Quote("Bobby Fischer is the greatest chess player who has ever lived.", "Ken Smith"),
    Quote(
      "Up to this point White has been following well-known analysis. But now he makes a fatal error: he begins to use his own head.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "Fischer was a master of clarity and a king of artful positioning. His opponents would see where he was going but were powerless to stop him.",
      "Bruce Pandolfini"
    ),
    Quote(
      "No other master has such a terrific will to win. At the board he radiates danger, and even the strongest opponents tend to freeze, like rabbits when they smell a panther. Even his weaknesses are dangerous.",
      "Anonymous German Expert"
    ),
    Quote(
      "White lost because he failed to remember the right continuation and had to think up the moves himself.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "Not only will I predict his triumph over Botvinnik, but I'll go further and say that he’ll probably be the greatest chess player that ever lived.",
      "John Collins"
    ),
    Quote("I consider Fischer to be one of the greatest opening experts ever.", "Keith Hayward"),
    Quote(
      "I like to say that Bobby Fischer was the greatest player ever. But what made Fischer a genius was his ability to blend an American freshness and pragmatism with Russian ideas about strategy.",
      "Bruce Pandolfini"
    ),
    Quote(
      "At this time Fischer is simply a level above all the best chess players in the world.",
      "John Jacobs"
    ),
    Quote(
      "I have always a slight feeling of pity for the man who has no knowledge of chess.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "There's never before been a chess player with such a thorough knowledge of the intricacies of the game and such an absolutely indomitable will to win. I think Bobby is the greatest player that ever lived.",
      "Lisa Lane"
    ),
    Quote("He who takes the queen's knight’s pawn will sleep in the streets.", "Anonymous"),
    Quote(
      "I had a toothache during the first game. In the second game I had a headache. In the third game it was an attack of rheumatism. In the fourth game, I wasn't feeling well. And in the fifth game? Well, must one have to win every game?",
      "Siegbert Tarrasch"
    ),
    Quote("The stomach is an essential part of the chess master.", "Bent Larsen"),
    Quote(
      "I'm not a materialistic person, in that, I don’t suffer the lack or loss of money. The absence of worldly goods I don’t look back on. For chess is a way I can be as materialistic as I want without having to sell my soul",
      "Jamie Walter Adams"
    ),
    Quote(
      "These are not pieces, they are men! For any man to walk into the line of fire will be one less man in your army to fight for you. Value every troop and use him wisely, throw him not to the dogs as he is there to serve his king.",
      "Jamie Walter Adams"
    ),
    Quote("Chess isn't a game of speed, it is a game of speech through actions.", "Matt Selman"),
    Quote("Life like chess is about knowing to do the right move at the right time.", "Kaleb Rivera"),
    Quote("Come on Harry!", "Simon Williams"),
    Quote(
      "Some people think that if their opponent plays a beautiful game, it's okay to lose. I don’t. You have to be merciless.",
      "Magnus Carlsen"
    ),
    Quote(
      "It's one of those types of positions where he has pieces on squares.",
      "John ~ZugAddict~ Chernoff"
    ),
    Quote("On the bright side, I no longer have any more pieces to lose.", "John ~ZugAddict~ Chernoff"),
    Quote(
      "Tactics... tactics are your friends. But they are weird friends who do strange things.",
      "John ~ZugAddict~ Chernoff"
    ),
    Quote(
      "You can't take the pawn because then the other will queen. Like wonder twin powers",
      "John ~ZugAddict~ Chernoff"
    ),
    Quote(
      "Most of the gods throw dice but Fate plays chess, and you don't find out until too late that he's been using two queens all along.",
      "Terry Pratchett"
    ),
    Quote(
      "Atomic is just like regular chess, except you're exploding, everything's exploding, and you're in bullet hell.",
      "Unihedron 0"
    ),
    Quote("lichess is better, but it's free.", "Thibault Duplessis"),
    Quote(
      "When you trade, the key concern is not always the value of the pieces being exchanged, but what's left on the board.",
      "Dan Heisman"
    ),
    Quote(
      "I detest the endgame. A well-played game should be practically decided in the middlegame.",
      "David Janowski"
    ),
    Quote(
      "Many men, many styles; what is chess style but the intangible expression of the will to win.",
      "Aron Nimzowitsch"
    ),
    Quote("Never play for the win, never play for the draw, just play chess!", "Alexander Khalifman"),
    Quote(
      "In chess, knowledge is a very transient thing. It changes so fast that even a single mouse-slip sometimes changes the evaluation.",
      "Viswanathan Anand"
    ),
    Quote(
      "Having good strategies in playing chess is often a good indication of being focused in life.",
      "Martin Dansky"
    ),
    Quote(
      "Chess is an infinitely complex game, which one can play in infinitely numerous and varied ways.",
      "Vladimir Kramnik"
    ),
    Quote(
      "Chess: It's like alcohol. It’s a drug. I have to control it, or it could overwhelm me.",
      "Charles Krauthammer"
    ),
    Quote(
      "Drawing general conclusions about your main weaknesses can provide a great stimulus to further growth.",
      "Alexander Kotov"
    ),
    Quote(
      "The good thing in chess is that very often the best moves are the most beautiful ones. The beauty of logic.",
      "Boris Gelfand"
    ),
    Quote(
      "Any experienced player knows how a change in the character of the play influences your psychological mood.",
      "Garry Kasparov"
    ),
    Quote("Be a harsh critic of your own wins.", "Vasilios Kotronias"),
    Quote(
      "Good players develop a tactical instinct, a sense of what is possible or likely and what is not worth calculating.",
      "Samuel Reshevsky"
    ),
    Quote(
      "Lack of patience is probably the most common reason for losing a game, or drawing games that should have been won.",
      "Bent Larsen"
    ),
    Quote(
      "The scheme of a game is played on positional lines; the decision of it, as a rule, is effected by combinations.",
      "Richard Réti"
    ),
    Quote(
      "The single most important thing in life is to believe in yourself regardless of what everyone else says.",
      "Hikaru Nakamura"
    ),
    Quote(
      "Attackers may sometimes regret bad moves, but it is much worse to forever regret an opportunity you allowed to pass you by.",
      "Garry Kasparov"
    ),
    Quote(
      "My favorite victory is when it is not even clear where my opponent made a mistake.",
      "Peter Leko"
    ),
    Quote("Win with grace, lose with dignity.", "Susan Polgar"),
    Quote(
      "Pawns are such fascinating pieces, too... so small, almost insignificant, and yet--they can depose kings.",
      "Lavie Tidhar"
    ),
    Quote("The move is there, but you must see it.", "Savielly Tartakower"),
    Quote(
      "The kings are an apt metaphor for human beings: utterly constrained by the rules of the game, defenseless against bombardment from all sides, able only to temporarily dodge disaster by moving one step in any direction.",
      "Jennifer duBois"
    ),
    Quote(
      "If chess is an art, Alekhine. If chess is a science, Capablanca. If chess is a struggle, Lasker. --on who he thought was the best player.",
      "Savielly Tartakower"
    ),
    Quote("Chess is a good mistress, but a bad master.", "Gerald Abrahams"),
    Quote("I often play a move I know how to refute.", "Bent Larsen"),
    Quote("First restrain, next blockade, lastly destroy.", "Aron Nimzowitsch"),
    Quote(
      "If you don't know what to do, find your worst piece and look for a better square.",
      "Gerard Schwarz"
    ),
    Quote(
      "Players who balk at playing one-minute chess are failing to see the whole picture. They shouldn't be worrying that they will make more mistakes – they should be rubbing their hands in glee at the thought of all the mistakes their opponents will make.",
      "Hikaru Nakamura"
    ),
    Quote(
      "A Chess game is divided into three stages: the first, when you hope you have the advantage, the second when you believe that you have an advantage, and the third... when you know you're going to lose!",
      "Savielly Tartakower"
    ),
    Quote(
      "A queen's sacrifice, even when fairly obvious, always rejoices the heart of the chess-lover.",
      "Savielly Tartakower"
    ),
    Quote(
      "A chess game, after all, is a fight in which all possible factors must be made use of, and in which a knowledge of the opponent's good and bad qualities is of the greatest importance.",
      "Emanuel Lasker"
    ),
    Quote("A chess player never has a heart attack in a good position.", "Bent Larsen"),
    Quote("A computer beat me in chess, but it was no match when it came to kickboxing.", "Emo Phillips"),
    Quote(
      "A considerable role in the forming of my style was played by an early attraction to study composition.",
      "Vasily Smyslov"
    ),
    Quote("A defeatist spirit must inevitably lead to disaster.", "Eugene Znosko-Borovsky"),
    Quote(
      "A draw can be obtained not only by repeating moves, but also by one weak move.",
      "Savielly Tartakower"
    ),
    Quote(
      "A draw may be the beautiful and logical result of fine attacks and parries; and the public ought to appreciate such games, in contrast, of course, to the fear-and-laziness draws.",
      "Bent Larsen"
    ),
    Quote(
      "A gambit never becomes sheer routine as long as you fear you may lose the king and pawn ending!",
      "Bent Larsen"
    ),
    Quote("A great chess player always has a very good memory.", "Leonid Shamkovich"),
    Quote("A knight ending is really a pawn ending.", "Mikhail Botvinnik"),
    Quote(
      "A male scorpion is stabbed to death after mating. In chess, the powerful queen often does the same to the king without giving him the satisfaction of a lover.",
      "Gregor Piatigorsky"
    ),
    Quote(
      "A pawn, when separated from his fellows, will seldom or never make a fortune.",
      "François-André Danican Philidor"
    ),
    Quote("A plan is made for a few moves only, not for the whole game.", "Reuben Fine"),
    Quote(
      "A player can sometimes afford the luxury of an inaccurate move, or even a definite error, in the opening or middlegame without necessarily obtaining a lost position. In the endgame... an error can be decisive, and we are rarely presented with a second chance.",
      "Paul Keres"
    ),
    Quote(
      "A real sacrifice involves a radical change in the character of a game which cannot be effected without foresight, fantasy, and the willingness to risk.",
      "Leonid Shamkovich"
    ),
    Quote(
      "A sport, a struggle for results and a fight for prizes. I think that the discussion about \"chess is science or chess is art\" is already inappropriate. The purpose of modern chess is to reach a result.",
      "Alexander Morozevich"
    ),
    Quote(
      "A strong player requires only a few minutes of thought to get to the heart of the conflict. You see a solution immediately, and half an hour later merely convince yourself that your intuition has not deceived you.",
      "David Bronstein"
    ),
    Quote(
      "A win gives one a feeling of self-affirmation, and success - a feeling of self-expression, but only a sensible harmonization between these urges can bring really great achievements in chess.",
      "Oleg Romanishin"
    ),
    Quote(
      "Above all else, before playing in competitions a player must have regard to his health, for if he is suffering from ill-health he cannot hope for success. In this connection the best of all tonics is 15 to 20 days in the fresh air, in the country.",
      "Mikhail Botvinnik"
    ),
    Quote(
      "According to such great attacking players as Bronstein and Tal, most combinations are inspired by the player's memories of earlier games.",
      "Pal Benko"
    ),
    Quote(
      "After I won the title, I was confronted with the real world. People do not behave naturally anymore – hypocrisy is everywhere.",
      "Boris Spassky"
    ),
    Quote(
      "After a great deal of discussion in Soviet literature about the correct definition of a combination, it was decided that from the point of view of a methodical approach it was best to settle on this definition - A combination is a forced variation with a sacrifice.",
      "Alexander Kotov"
    ),
    Quote(
      "Agreeing to draws in the middlegame, equal or otherwise, deprives you of the opportunity to practice playing endgames, and the endgame is probably where you need the most practice.",
      "Pal Benko"
    ),
    Quote(
      "All chess masters have on occasion played a magnificent game and then lost it by a stupid mistake, perhaps in time pressure and it may perhaps seem unjust that all their beautiful ideas get no other recognition than a zero on the tournament table.",
      "Bent Larsen"
    ),
    Quote(
      "All chess players know what a combination is. Whether one makes it oneself, or is its victim, or reads of it, it stands out from the rest of the game and stirs one's admiration.",
      "Eugene Znosko-Borovsky"
    ),
    Quote("All conceptions in the game of chess have a geometrical basis.", "Eugene Znosko-Borovsky"),
    Quote(
      "All lines of play which lead to the imprisonment of the bishop are on principle to be condemned. (on the closed Ruy Lopez)",
      "Siegbert Tarrasch"
    ),
    Quote("All that matters on the chessboard is good moves.", "Bobby Fischer"),
    Quote(
      "All that now seems to stand between Nigel and the prospect of the world crown is the unfortunate fact that fate brought him into this world only two years after Kasparov.",
      "(prophetic comment in 1987) - Garry Kasparov"
    ),
    Quote(
      "Along with my retirement from chess analytical work seems to have gone too.",
      "Mikhail Botvinnik"
    ),
    Quote(
      "Although the knight is generally considered to be on a par with the bishop in strength, the latter piece is somewhat stronger in the majority of cases in which they are opposed to each other.",
      "José Raúl Capablanca"
    ),
    Quote("Amberley excelled at chess - a mark, Watson, of a scheming mind.", "Sir Arthur Conan Doyle"),
    Quote(
      "Americans really don't know much about chess. But I think when I beat Spassky, that Americans will take a greater interest in chess. Americans like winners.",
      "Bobby Fischer"
    ),
    Quote(
      "Among top grandmasters the Dutch is a rare defense, which is good reason to play it! It has not been studied very deeply by many opponents, and theory, based on a small number of 'reliable' games, must be rather unreliable.",
      "Bent Larsen"
    ),
    Quote(
      "An amusing fact: as far as I can recall, when playing the Ruy Lopez I have not yet once in my life had to face the Marshall Attack!",
      "Anatoly Karpov"
    ),
    Quote(
      "An innovation need not be especially ingenious, but it must be well worked out.",
      "Paul Keres"
    ),
    Quote("An isolated pawn spreads gloom all over the chessboard.", "Savielly Tartakower"),
    Quote(
      "Analysis is a glittering opportunity for training: it is just here that capacity for work, perseverance and stamina are cultivated, and these qualities are, in truth, as necessary to a chess player as a marathon runner.",
      "Lev Polugaevsky"
    ),
    Quote(
      "Analysis, if it is really carried out with a complete concentration of his powers, forms and completes a chess player.",
      "Lev Polugaevsky"
    ),
    Quote(
      "Anyone who wishes to learn how to play chess well must make himself or herself thoroughly conversant with the play in positions where the players have castled on opposite sides.",
      "Alexander Kotov"
    ),
    Quote(
      "Apart from direct mistakes, there is nothing more ruinous than routine play, the aim of which is mechanical development.",
      "Alexei Suetin"
    ),
    Quote(
      "As Rousseau could not compose without his cat beside him, so I cannot play chess without my king's bishop. In its absence the game to me is lifeless and void. The vitalizing factor is missing, and I can devise no plan of attack.",
      "Siegbert Tarrasch"
    ),
    Quote(
      "As a chess player one has to be able to control one's feelings, one has to be as cold as a machine.",
      "Levon Aronian"
    ),
    Quote(
      "As a rule, pawn endings have a forced character, and they can be worked out conclusively.",
      "Mark Dvoretsky"
    ),
    Quote(
      "As a rule, so-called \"positional\" sacrifices are considered more difficult, and therefore more praise-worthy than those which are based exclusively on an exact calculation of tactical possibilities.",
      "Alexander Alekhine"
    ),
    Quote(
      "As a rule, the more mistakes there are in a game, the more memorable it remains, because you have suffered and worried over each mistake at the board.",
      "Viktor Korchnoi"
    ),
    Quote(
      "As long as my opponent has not yet castled, on each move I seek a pretext for an offensive. Even when I realize that the king is not in danger.",
      "Mikhail Tal"
    ),
    Quote(
      "As often as not, his strategy consists of stifling Black's activity and then winning in an endgame thanks to his superior pawn structure.",
      "Neil McDonald"
    ),
    Quote("Attack! Always Attack!", "Adolf Anderssen"),
    Quote(
      "Avoidance of mistakes is the beginning, as it is the end, of mastery in chess.",
      "Eugene Znosko-Borovsky"
    ),
    Quote(
      "Barcza is the most versatile player in the opening. He sometimes plays g2-g3 on the first, sometimes on the second, sometimes on the third, and sometimes only on the fourth move.",
      "reputedly stated by Harry Golombek"
    ),
    Quote("Before Geller we did not understand the King's Indian Defence.", "Mikhail Botvinnik"),
    Quote(
      "Begone! Ignorant and impudent knight, not even in chess can a King be taken.",
      "King Louis VI (reputedly stated to one of his knights in 1110 after he was nearly captured by enemy forces)"
    ),
    Quote("Black's d5-square is too weak.", "Ulf Andersson (on the Dragon variation)"),
    Quote("Blitz chess kills your ideas.", "Bobby Fischer"),
    Quote(
      "Bobby Fischer started off each game with a great advantage: after the opening he had used less time than his opponent and thus had more time available later on. The major reason why he never had serious time pressure was that his rapid opening play simply left sufficient time for the middlegame.",
      "Edmar Mednis"
    ),
    Quote(
      "Books on the openings abound; nor are works on the end game wanting; but those on the middle game can be counted on the fingers of one hand.",
      "Harry Golombek"
    ),
    Quote(
      "Boris Vasilievich was the only top-class player of his generation who played gambits regularly and without fear... over a period of 30 years he did not lose a single game with the King's Gambit, and among those defeated were numerous strong players of all generations, from Averbakh, Bronstein and Fischer, to Seirawan.",
      "Garry Kasparov"
    ),
    Quote(
      "Botvinnik tried to take the mystery out of Chess, always relating it to situations in ordinary life. He used to call chess a typical inexact problem similar to those which people are always having to solve in everyday life.",
      "Garry Kasparov"
    ),
    Quote(
      "But alas! Like many another consummation devoutly to be wished, the actual performance was a disappointing one. (on the long awaited Lasker-Capablanca match in 1921)",
      "Fred Reinfeld"
    ),
    Quote(
      "But how difficult it can be to gain the desired full point against an opponent of inferior strength, when this is demanded by the tournament position!",
      "Anatoly Karpov"
    ),
    Quote(
      "But whatever you might say and whatever I might say, a machine which can play chess with people is one of the most marvellous wonders of our 20th century!",
      "David Bronstein"
    ),
    Quote(
      "But you see when I play a game of Bobby, there is no style. Bobby played perfectly. And perfection has no style.",
      "Miguel Najdorf"
    ),
    Quote(
      "By all means examine the games of the great chess players, but don't swallow them whole. Their games are valuable not for their separate moves, but for their vision of chess, their way of thinking.",
      "Anatoly Karpov"
    ),
    Quote(
      "By positional play a master tries to prove and exploit true values, whereas by combinations he seeks to refute false values... a combination produces an unexpected re-assessment of values.",
      "Emanuel Lasker"
    ),
    Quote(
      "By some ardent enthusiasts Chess has been elevated into a science or an art. It is neither; but its principal characteristic seems to be what human nature mostly delights in a fight.",
      "Emanuel Lasker"
    ),
    Quote(
      "By strictly observing Botvinnik's rule regarding the thorough analysis of one's own games, with the years I have come to realize that this provides the foundation for the continuous development of chess mastery.",
      "Garry Kasparov"
    ),
    Quote(
      "By the mid-1990s the number of people with some experience of using computers was many orders of magnitude greater than in the 1960s. In the Kasparov defeat they recognized that here was a great triumph for programmers, but not one that may compete with the human intelligence that helps us to lead our lives.",
      "Igor Aleksander"
    ),
    Quote(
      "By the time a player becomes a Grandmaster, almost all of his training time is dedicated to work on this first phase. The opening is the only phase that holds out the potential for true creativity and doing something entirely.",
      "Garry Kasparov"
    ),
    Quote(
      "By what right does White, in an absolutely even position, such as after move one, when both sides have advanced 1. e4, sacrifice a pawn, whose recapture is quite uncertain, and open up his kingside to attack? And then follow up this policy by leaving the check of the black queen open? None whatever!",
      "Emanuel Lasker"
    ),
    Quote(
      "Can you imagine the relief it gives a mother when her child amuses herself quietly for hours on end?",
      "Klara Polgar"
    ),
    Quote(
      "Capablanca did not apply himself to opening theory (in which he never therefore achieved much), but delved deeply into the study of end-games and other simple positions which respond to technique rather than to imagination.",
      "Max Euwe"
    ),
    Quote(
      "Chess can help a child develop logical thinking, decision making, reasoning, and pattern recognition skills, which in turn can help math and verbal skills.",
      "Susan Polgar"
    ),
    Quote(
      "Chess can learn a lot from poker. First, chess media and sponsors should emphasize its glamorous aspects: worldwide traveling, parties and escape from real world responsibilities.",
      "Jennifer Shahade"
    ),
    Quote(
      "Chess can never reach its height by following in the path of science... let us, therefore, make a effort and with the help of our imagination turn the struggle of technique into a battle of ideas.",
      "José Raúl Capablanca"
    ),
    Quote(
      "Chess continues to advance over time, so the players of the future will inevitably surpass me in the quality of their play, assuming the rules and regulations allow them to play serious chess. But it will likely be a long time before anyone spends 20 consecutive years as number one, as I did.",
      "Garry Kasparov"
    ),
    Quote(
      "Chess is a bond of brotherhood amongst all lovers of the noble game, as perfect as free masonry. It is a leveller of rank - title, wealth, nationality, politics, religion - all are forgotten across the board.",
      "Frederick Milnes Edge"
    ),
    Quote(
      "Chess is a contest between two men which lends itself particularly to the conflicts surrounding aggression.",
      "Reuben Fine"
    ),
    Quote(
      "Chess is a contributor to net human unhappiness, since the pleasure of victory is greatly exceeded by the pain of defeat.",
      "Bill Hartston"
    ),
    Quote("Chess is a cure for headaches.", "John Maynard Keynes"),
    Quote(
      "Chess is a game sufficiently rich in meaning that it is easily capable of containing elements of both tragedy and comedy.",
      "Luke McShane"
    ),
    Quote("Chess is a game which reflects most honor on human wit.", "Voltaire"),
    Quote(
      "Chess is a great game. No matter how good one is, there is always somebody better. No matter how bad one is, there is always somebody worse.",
      "I. A. Horowitz"
    ),
    Quote(
      "Chess is a matter of delicate judgement, knowing when to punch and how to duck.",
      "Bobby Fischer"
    ),
    Quote("Chess is a matter of vanity.", "Alexander Alekhine"),
    Quote("Chess is a meritocracy.", "Lawrence Day"),
    Quote(
      "Chess is a miniature version of life. To be successful, you need to be disciplined, assess resources, consider responsible choices and adjust when circumstances change.",
      "Susan Polgar"
    ),
    Quote("Chess is a natural cerebral high.", "Walter Browne"),
    Quote("Chess is a sport. A violent sport.", "Marcel Duchamp"),
    Quote("Chess is a test of wills.", "Paul Keres"),
    Quote(
      "Chess is a unique cognitive nexus, a place where art and science come together in the human mind and are refined and improved by experience.",
      "Garry Kasparov"
    ),
    Quote("Chess is beautiful enough to waste your life for.", "Hans Ree"),
    Quote("Chess is eminently and emphatically the philosopher's game.", "Paul Morphy"),
    Quote(
      "Chess is far too complex to be definitively solved with any technology we can conceive of today. However, our looked-down-upon cousin, checkers, or draughts, suffered this fate quite recently thanks to the work of Jonathan Schaeffer at the University of Alberta and his unbeatable program Chinook.",
      "Garry Kasparov"
    ),
    Quote(
      "Chess is infinite, and one has to make only one ill-considered move, and one's opponent's wildest dreams will become reality.",
      "David Bronstein"
    ),
    Quote(
      "Chess is like a language, the top players are very fluent at it. Talent can be developed scientifically but you have to find first what you are good at.",
      "Viswanathan Anand"
    ),
    Quote(
      "Chess is like body-building. If you train every day, you stay in top shape. It is the same with your brain – chess is a matter of daily training.",
      "Vladimir Kramnik"
    ),
    Quote("Chess is my life.", "Viktor Korchnoi"),
    Quote(
      "Chess is my profession. I am my own boss; I am free. I like literature and music, classical especially. I am in fact quite normal; I have a Bohemian profession without being myself a Bohemian. I am neither a conformist nor a great revolutionary.",
      "Bent Larsen"
    ),
    Quote(
      "Chess is not for the faint-hearted; it absorbs a person entirely. To get to the bottom of this game, he has to give himself up into slavery. Chess is difficult, it demands work, serious reflection and zealous research.",
      "Wilhelm Steinitz"
    ),
    Quote("Chess is not for the timid.", "Irving Chernev"),
    Quote("Chess is not relaxing; it's stressful even if you win.", "Jennifer Shahade"),
    Quote("Chess is one long regret.", "Stephen Leacock"),
    Quote("Chess is only a recreation and not an occupation.", "Vladimir Lenin"),
    Quote(
      "Chess is something more than a game. It is an intellectual diversion which has certain artistic qualities and many scientific elements.",
      "José Raúl Capablanca"
    ),
    Quote("Chess is the touchstone of intellect.", "Johann Wolfgang von Goethe"),
    Quote(
      "Chess is thriving. There are ever less round robin tournaments and ever more World Champions.",
      "Robert Hübner"
    ),
    Quote(
      "Chess masters as well as chess computers deserve less reverence than the public accords them.",
      "Eliot Hearst"
    ),
    Quote(
      "Chess programs are our enemies, they destroy the romance of chess. They take away the beauty of the game. Everything can be calculated.",
      "Levon Aronian"
    ),
    Quote(
      "Chess strategy as such today is still in its diapers, despite Tarrasch's statement 'We live today in a beautiful time of progress in all fields'. Not even the slightest attempt has been made to explore and formulate the laws of chess strategy.",
      "Aron Nimzowitsch"
    ),
    Quote(
      "Chess strength in general and chess strength in a specific match are by no means one and the same thing.",
      "Garry Kasparov"
    ),
    Quote(
      "Chess will always be in the doldrums as a spectator sport while a draw is given equal mathematical value as a decisive result.",
      "Michael Basman"
    ),
    Quote("Chess, like love, is infectious at any age.", "Salo Flohr"),
    Quote(
      "Chess-play is a good and witty exercise of the mind for some kind of men, but if it proceed from overmuch study, in such a case it may do more harm than good; it is a game too troublesome for some men's brains.",
      "Robert Burton"
    ),
    Quote(
      "Combinations with a queen sacrifice are among the most striking and memorable...",
      "Anatoly Karpov"
    ),
    Quote(
      "Concentrate on material gains. Whatever your opponent gives you take, unless you see a good reason not to.",
      "Bobby Fischer"
    ),
    Quote(
      "Condemned by theory, the Allgaier, certainly one of the most romantic of gambits, is generally successful in practice (and yet so rarely played). Why does the defender often seem hypnotized, quite demoralized?",
      "Tony Santasiere"
    ),
    Quote(
      "Confidence is very important – even pretending to be confident. If you make a mistake but do not let your opponent see what you are thinking then he may overlook the mistake.",
      "Viswanathan Anand"
    ),
    Quote(
      "Contrary to many young colleagues I do believe that it makes sense to study the classics.",
      "Magnus Carlsen"
    ),
    Quote(
      "Deschapelles became a first-rate player in three days, at the age of something like thirty. Nobody ever believed the statement, not even Deschapelles himself, although his biographer declares he had told the lie so often that he at last forgot the facts of the case.",
      "Frederick Milnes Edge"
    ),
    Quote(
      "Despite the development of chess theory, there is much that remains secret and unexplored in chess.",
      "Vasily Smyslov"
    ),
    Quote(
      "Do not bring your queen out too early.",
      "from Francisco Bernardina Calogno's poem 'On the Game of Chess' circa 1500"
    ),
    Quote(
      "Do not permit yourself to fall in love with the end-game play to the exclusion of entire games. It is well to have the whole story of how it happened; the complete play, not the denouement only. Do not embrace the rag-time and vaudeville of chess.",
      "Emanuel Lasker"
    ),
    Quote(
      "Do not pick a move from a list of computer lines - use your own brains. This is important, especially for young players. It's better to study a worse line well than to reproduce a better computer line.",
      "Laszlo Hazai"
    ),
    Quote(
      "Don't be afraid of losing, be afraid of playing a game and not learning something.",
      "Dan Heisman"
    ),
    Quote(
      "Don't worry about your rating, work on your playing strength and your rating will follow.",
      "Dan Heisman"
    ),
    Quote(
      "Don't worry kids, you'll find work. After all, my machine will need strong chess player-programmers. You will be the first.",
      "Mikhail Botvinnik (to Karpov & students, 1965)"
    ),
    Quote(
      "Drawn games are sometimes more scintillating than any conclusive contest.",
      "Savielly Tartakower"
    ),
    Quote(
      "During a chess tournament a master must envisage himself as a cross between an ascetic monk and a beast of prey.",
      "Alexander Alekhine"
    ),
    Quote(
      "During the late Victorian period the majority of chess magazines printed increasing numbers of humorous stories, poems and anecdotes about the agonies and idiocies of women chess players, presumably as an antidote to the alarmed reaction of men to the fact that women were encroaching on their 'territory'.",
      "British Chess Magazine"
    ),
    Quote(
      "Emotional instability can be one of the factors giving rise to a failure by chess players in important duels. Under the influence of surging emotions (and not necessarily negative ones) we sometimes lose concentration and stop objectively evaluating the events that are taking place on the board.",
      "Mark Dvoretsky"
    ),
    Quote(
      "Endings of one rook and pawns are about the most common sort of endings arising on the chess board. Yet though they do occur so often, few have mastered them thoroughly. They are often of a very difficult nature, and sometimes while apparently very simple they are in reality extremely intricate.",
      "José Raúl Capablanca"
    ),
    Quote(
      "Enormous self-belief, intuition, the ability to take a risk at a critical moment and go in for a very dangerous play with counter-chances for the opponent - it is precisely these qualities that distinguish great players.",
      "Garry Kasparov"
    ),
    Quote(
      "Errors have nothing to do with luck; they are caused by time pressure, discomfort or unfamiliarity with a position, distractions, feelings of intimidation, nervous tension, over-ambition, excessive caution, and dozens of other psychological factors.",
      "Pal Benko"
    ),
    Quote(
      "Even in the King's Gambit... White is no longer trying to attack at all costs. He has had to adapt his approach and look for moves with a solid positional foundation",
      "Neil McDonald"
    ),
    Quote(
      "Even in the heat of a middlegame battle the master still has to bear in mind the outlines of a possible future ending.",
      "David Bronstein"
    ),
    Quote(
      "Even the best grandmasters in the world have had to work hard to acquire the technique of rook endings.",
      "Paul Keres"
    ),
    Quote(
      "Even the most distinguished players have in their careers experienced severe disappointments due to ignorance of the best lines or suspension of their own common sense.",
      "Tigran Petrosian"
    ),
    Quote(
      "Even when the time control has been reached, there is one situation where you want to act as if it has not: when your position is absolutely lost.",
      "Edmar Mednis"
    ),
    Quote("Every Chess master was once a beginner.", "Irving Chernev"),
    Quote(
      "Every great master will find it useful to have his own theory on the openings, which only he himself knows, a theory which is closely linked with plans for the middle game.",
      "Mikhail Botvinnik"
    ),
    Quote(
      "Every month I look through some ten thousand games, so not as to miss any ideas and trends.",
      "Vladimir Kramnik"
    ),
    Quote("Every move creates a weakness.", "Siegbert Tarrasch"),
    Quote(
      "Excellent! I will still be in time for the ballet!",
      "José Raúl Capablanca (upon defeating Ossip Bernstein in the famous 29 move exhibition game played in Moscow in 1914, before setting off to the Bolshoi Theatre in horse-drawn carriage)"
    ),
    Quote(
      "Excelling at chess has long been considered a symbol of more general intelligence. That is an incorrect assumption in my view, as pleasant as it might be.",
      "Garry Kasparov"
    ),
    Quote(
      "Experience and the constant analysis of the most varied positions builds up a store of knowledge in a player's mind enabling him often at a glance to assess this or that position.",
      "Alexander Kotov"
    ),
    Quote(
      "Failing to open the center at the right moment - a common error by White in the Exchange Lopez - can allow Black an excellent game.",
      "Andrew Soltis"
    ),
    Quote("Far from all of the obvious moves that go without saying are correct.", "David Bronstein"),
    Quote("Few things are as psychologically brutal as chess.", "Garry Kasparov"),
    Quote(
      "First and foremost it is essential to understand the essence, the overall idea of any fashionable variation, and only then include it in one's repertoire. Otherwise the tactical trees will conceal from the player the strategic picture of the wood, in which his orientation will most likely be lost.",
      "Lev Polugaevsky"
    ),
    Quote("Fischer is Fischer, but a knight is a knight!", "Mikhail Tal"),
    Quote(
      "For a game it is too serious, for seriousness too much of a game.",
      "Moses Mendelssohn"
    ),
    Quote("For every door the computers have closed they have opened a new one.", "Viswanathan Anand"),
    Quote(
      "For me right now I think being the world number one is a bigger deal than being the world champion because I think it shows better who plays the best chess. That sounds self-serving but I think it's also right. (2012)",
      "Magnus Carlsen"
    ),
    Quote(
      "For me, chess is a language, and if it's not my native tongue, it is one I learned via the immersion method at a young age.",
      "Garry Kasparov"
    ),
    Quote(
      "For me, chess is at the same time a game, a sport, a science and an art. And perhaps even more than that. There is something hard to explain to those who do not know the game well. One must first learn to play it correctly in order to savor its richness.",
      "Bent Larsen"
    ),
    Quote(
      "For me, chess is not a profession, it is a way of life, a passion. People may feel that I have conquered the peak and will not have to struggle. Financially, perhaps that is true; but as far as chess goes, I'm still learning a lot!",
      "Viswanathan Anand"
    ),
    Quote(
      "For my victory over Capablanca I am indebted primarily to my superiority in the field of psychology. Capablanca played, relying almost exclusively on his rich intuitive talent. But for the chess struggle nowadays one needs a subtle knowledge of human nature, an understanding of the opponent's psychology.",
      "Alexander Alekhine"
    ),
    Quote(
      "For pleasure you can read the games collections of Andersson and Chigorin, but for benefit you should study Tarrasch, Keres and Bronstein.",
      "Mikhail Tal"
    ),
    Quote(
      "Fortunately I've got a weak character, so I never did decide to dedicate myself to only one of my professions. And I’m very glad. After all, if I’d rejected chess or music then my life wouldn’t have been two times, but a hundred times less interesting.",
      "Mark Taimanov"
    ),
    Quote(
      "From time to time, like many other players, I glance through my own games of earlier years, and return to positions and variations which have gone out of practice. I attempt to restore them, to find ideas and plans.",
      "Efim Geller"
    ),
    Quote(
      "Furman astounded me with his chess depth, a depth which he revealed easily and naturally, as if all he were doing was establishing well-known truths.",
      "Anatoly Karpov"
    ),
    Quote(
      "GM Naiditsch reckoned that me playing the King's Indian against Anand was something akin to a samurai running at a machine gun with a sword.",
      "Hikaru Nakamura"
    ),
    Quote(
      "Genius. It's a word. What does it really mean? If I win I'm a genius. If I don't, I'm not.",
      "Bobby Fischer"
    ),
    Quote(
      "Go through detailed variations in your own time, think in a general way about the position in the opponent's time and you will soon find that you get into time trouble less often, that your games have more content to them, and that their general standard rises.",
      "Alexander Kotov"
    ),
    Quote(
      "Had I not played the Sicilian with Black I could have saved myself the trouble of studying for more than 20 years all the more popular lines of this opening, which comprise probably more than 25 percent of all published opening theory!",
      "Bent Larsen"
    ),
    Quote(
      "Has he some psychological antipathy to realism? I am no psychologist, and cannot say. The fact remains that Euwe commits the most inexplicable mistakes in thoroughly favorable positions, and that this weakness has consistently tarnished his record.",
      "Hans Kmoch"
    ),
    Quote(
      "Haste is never more dangerous than when you feel that victory is in your grasp.",
      "Eugene Znosko-Borovsky"
    ),
    Quote("Haste, the great enemy.", "Eugene Znosko-Borovsky"),
    Quote(
      "Having spent alarmingly large chunks of my life studying the white side of the Open Sicilian, I find myself asking, why did I bother?",
      "Daniel King"
    ),
    Quote(
      "He played with enormous energy and great fighting spirit. Offering him a draw was a waste of time. He would decline it politely, but firmly. \"No, thank you,\" he would say and the fight would go on and on and on.",
      "Lubomir Kavalek on Bent Larsen"
    ),
    Quote(
      "He who has a slight disadvantage plays more attentively, inventively and more boldly than his antagonist who either takes it easy or aspires after too much. Thus a slight disadvantage is very frequently seen to convert into a good, solid advantage.",
      "Emanuel Lasker"
    ),
    Quote(
      "Here is a definition which correctly reflects the course of thought and action of a grandmaster: The plan in a game of chess is the sum total of successive strategical operations which are each carried out according to separate ideas arising from the demands of the position.",
      "Alexander Kotov"
    ),
    Quote(
      "How come the little things bother you when you are in a bad position? They don't bother you in good positions.",
      "Yasser Seirawan"
    ),
    Quote(
      "However hopeless the situation appears to be there yet always exists the possibility of putting up a stubborn resistance.",
      "Paul Keres"
    ),
    Quote(
      "I... have two vocations: chess and engineering. If I played chess only, I believe that my success would not have been significantly greater. I can play chess well only when I have fully convalesced from chess and when the 'hunger for chess' once more awakens within me.",
      "Mikhail Botvinnik"
    ),
    Quote("I always urge players to study composed problems and endgames.", "Pal Benko"),
    Quote(
      "I am both sad and pleased that in his last tournament, Rashid Gibiatovich came to my home in Latvia. He did not take first place, but the prize for beauty, as always, he took with him. Players die, tournaments are forgotten, but the works of great artists are left behind them to live on forever. (on Nezhmetdinov)",
      "Mikhail Tal"
    ),
    Quote(
      "I am pleased that in a match for the World Championship I was able to conduct a game in the style of Akiba Rubinstein, where the entire strategic course was maintained from the first to the last move. (on Game 7 of his 2012 match with Anand)",
      "Boris Gelfand"
    ),
    Quote(
      "I am trying to beat the guy sitting across from me and trying to choose the moves that are most unpleasant for him and his style.",
      "Magnus Carlsen"
    ),
    Quote(
      "I believe in magic... there is magic in the creative faculty such as great poets and philosophers conspicuously possess, and equally in the creative chessmaster.",
      "Emanuel Lasker"
    ),
    Quote(
      "I believe most definitely that one must not only grapple with the problems on the board, one must also make every effort to combat the thoughts and will of the opponent.",
      "Mikhail Tal"
    ),
    Quote(
      "I believe that the best style is a universal one, tactical and positional at the same time...",
      "Susan Polgar"
    ),
    Quote(
      "I cannot think that a player genuinely loving the game can get pleasure just from the number of points scored no matter how impressive the total. I will not speak of myself, but for the masters of the older generation, from whose games we learned, the aesthetic side was the most important.",
      "Alexander Kotov"
    ),
    Quote(
      "I can't count the times I have lagged seemingly hopelessly far behind, and nobody except myself thinks I can win. But I have pulled myself in from desperate [situations]. When you are behind there are two strategies – counter-attack or all men to the defences. I’m good at finding the right balance between those.",
      "Magnus Carlsen"
    ),
    Quote(
      "I claim that nothing else is so effective in encouraging the growth of chess strength as such independent analysis, both of the games of the great players and your own.",
      "Mikhail Botvinnik"
    ),
    Quote("Watch out for the tricky knights.", "ChessNetwork"),
    Quote("I think crazyhouse improves your standard chess.", "ChessNetwork"),
    Quote(
      "The biggest tool for chess improvement would be playing against stronger opposition",
      "Peter Svidler"
    ),
    Quote(
      "Chess is a battle between your aversion to thinking and your aversion to losing.",
      "Anonymous"
    ),
    Quote("It was once said that Tal sacrificed 9 pawns for an attack", "Mato Jelic"),
    Quote("Be well enough prepared that preparation won't play a role.", "Magnus Carlsen"),
    Quote("I don't study; I create.", "Viktor Korchnoi"),
    Quote(
      "During the analysis, I discovered something very remarkable: the board is simply too small for two queens of the same color. They only get in each other's way. I realize that this might sound stupid, but I fully mean it. The advantage is much less than one would expect by counting material.",
      "Viktor Korchnoi"
    ),
    Quote("You'll be amazed at the people I've lost to while playing online.", "Magnus Carlsen"),
    Quote(
      "...even extremely intoxicated my chess strength and knowledge is still in my bones.",
      "Magnus Carlsen"
    ),
    Quote(
      "I don't play unorthodox openings. I prefer to give mainstream openings my own spin.",
      "Magnus Carlsen"
    ),
    Quote(
      "Playing long games online just takes too much time. It's fun to play blitz once in a while, where you can rely more on your intuition, your instincts rather than pure calculation and analysis.",
      "Magnus Carlsen"
    ),
    Quote("Fortune favors the lucky!", "Robert Houdart (Houdini author)"),
    Quote("I don't berserk, I am not a caveman", "Magnus Carlsen"),
    Quote(
      "1.e4 is the move you play when you're young, naive, and believe the world owes you something. Open positions, infinite horizons - what's not to love? Well, I've got s for you, buddy: it's a cruel chess board out there.",
      "John Bartholomew"
    ),
    Quote("Chess as a game is too serious; as a serious pursuit too frivolous.", "Moses Mendelssohn"),
    Quote("Chess makes me a better person", "Albert Badosa"),
    Quote(
      "Formerly I played to amuse myself, then to study, but now I play to create.",
      "Vera Menchik"
    ),
    Quote(
      "Losing is no catastrophe, it's a challenge to improve.",
      "Mariya Muzychuk"
    ),
    Quote(
      "The main thing is to set yourself a goal and to do everything in your power to achieve it. Remember that every moment has value, every minute is important. No one knows what life will bring; but the main thing is that, in the end, you will know that you tried, and that you made the effort to become better every day.",
      "Alexandra Kosteniuk"
    ),
    Quote(
      "Of course, it would be absurd to say that I have no talent and did little work on chess, however, the most important thing is that I love chess so much.",
      "Xie Jun"
    ),
    Quote(
      "Perhaps this is the side-effect of progress made: The more you know, the more you realize how weak you are.",
      "Xie Jun"
    ),
    Quote(
      "To be honest, when I am reflective and consider myself, I think that indeed Nona Gaprindashvili understands only chess, and I cannot imagine my life without chess.",
      "Nona Gaprindashvili"
    ),
    Quote(
      "...it is due to chess that I was able to undergo my own self-realization and become truly content.",
      "Nona Gaprindashvili"
    ),
    Quote(
      "Every chess player likes attacking play.",
      "Zhu Chen"
    ),
    Quote(
      "I don't think so much in general terms like best achievement, best game ever or best tournament. I try to play chess, enjoy it and of course if I win, I enjoy it more.",
      "Antoaneta Stefanova"
    ),
    Quote(
      "...if you lose it's not the end of the world.",
      "Hou Yifan"
    ),
    Quote(
      "Success in chess requires many factors such as environment, hard practice, coach, talent... When we are children, we should not set the goal of becoming a super grandmaster. Do not play chess because of pressure, let chess become your joy and success will come naturally.",
      "Lê Quang Liêm"
    ),
    Quote(
      "In chess, there is only one mistake: over-estimation of your opponent. All else is either bad luck or weakness.",
      "Savielly Tartakower"
    ),
    Quote(
      "I do not set limits for myself. I will continue to play chess as long as I enjoy the fun of competing and learning new things.",
      "Lê Quang Liêm"
    ),
    Quote("In chess, as in life, a man is his own most dangerous opponent.", "Vasíliy Smyslóv"),
    Quote(
      "You can become a big master in chess only if you see your mistakes and shortcomings. Exactly the same as in life itself.",
      "Alexander Alekhine"
    ),
    Quote(
      "The beauty of chess is how it absorbs your mind, demands all your attention... because every move is a problem to be solved.",
      "Irina Krush"
    ),
    Quote(
      "Life has its ups and downs and how we react and cope with them makes the difference. That is one lesson chess has taught me.",
      "Humpy Koneru"
    ),
    Quote(
      "When you finish a game, you are very happy and you feel like a king, but afterwards you see so many mistakes.",
      "Pia Cramling"
    ),
    Quote(
      "When I started chess, it was like reading a book, a new world opens up for you, and I quickly realized that I loved this world. I loved to compete, to play, to win.",
      "Pia Cramling"
    ),
    Quote(
      "I think it's very important to be inclusive, especially because that's part of the beauty of our game. It allows so many different people of different ages and from all over the world to come together, and we should leverage that strength.",
      "Jennifer Shahade"
    ),
    Quote(
      "...fall in love with the process, not the results.",
      "Jennifer Shahade"
    ),
    Quote(
      "Chess is not just a game but a culture and a way to connect with people, past and present, who may seem so different from us on the surface.",
      "Jennifer Shahade"
    ),
    Quote(
      "Especially in my youngest years, the beauty of chess had priority for me over winning a game. But I had to learn that in this sport, achievement is much more important than the beauty of chess.",
      "Judit Polgar"
    ),
    Quote(
      "When I became the World Champion I got 900 roubles and it's just ridiculous. I have 5 golden medals from World Championships, 11 Olympic medals; I won many tournaments and didn't earn anything.",
      "Nona Gaprindashvili"
    ),
    Quote(
      "The good thing about chess is that everyone can learn how to play at any age.",
      "Mariya Muzychuk"
    ),
    Quote(
      "It's not about winning or losing, but of course at the end of the day it's about winning or losing.",
      "Garry Kasparov"
    ),
    Quote(
      "Ok we can change you know? It's an equal change, I change one attacking piece for one defending piece but I still have enough pieces to mate him.",
      "Garry Kasparov"
    ),
    Quote("King safety is the number one priority.", "Garry Kasparov"),
    Quote("Magnus is a lethal combination of Fischer and Karpov...", "Garry Kasparov"),
    Quote(
      "Then it was not considered much of a surprise that I could beat 32 computers at the same time. To me, that was the golden age. Machines were weak, and my hair was strong.",
      "Garry Kasparov"
    ),
    Quote("Ask the knight on g1 what he thinks about the move f3.", "Edward Gufeld"),
    Quote("Every chessplayer should have a hobby.", "Savielly Tartakower"),
    Quote(
      "A match demonstrates less than a tournament. But a tournament demonstrates nothing at all.",
      "Savielly Tartakower"
    ),
    Quote("...the threat is stronger than the execution.", "Aron Nimzowitsch"),
    Quote("I can never get mated with a knight on f8.", "Bent Larsen"),
    Quote(
      "As for knight endings with an extra pawn... they are, as a rule, won.",
      "Mark Dvoretsky"
    ),
    // lichess facts
    Quote("All features for free; for everyone; forever.", "lichess.org"),
    Quote("We will never display ads.", "lichess.org"),
    Quote("We do not track you. It's a rare feature, nowadays.", "lichess.org"),
    Quote("Every chess player is a premium user.", "lichess.org"),
    Quote("I never lose. I either win or learn.", "Nelson Mandela")
  )

  given OWrites[Quote] = OWrites { q =>
    Json.obj(
      "text" -> q.text,
      "author" -> q.author
    )
  }
