# The Universal Shogi Interface

## 1. Introduction

This document contains Tord Romstadt's original description of the Universal Shogi Interface, a protocol for communication between a shogi engine and GUI, supplemented with some extensions introduced by the authors of the Shogidogoro GUI.

- For beginning shogi programmers, a standard protocol with supporting GUIs considerably reduces the amount of work required to write a shogi program. Instead of having to write a complete graphical application, they can write their shogi program as a simple console mode program, and plug this little program into an existing GUI.
- If the majority of shogi programs support a standard protocol, it becomes easy to run automatic tournament and matches between shogi programs. This is very useful for programmers, because a big number of games is often necessary in order to decide whether some change to a shogi program improves the strength.
- Portability between different operating systems becomes much easier when the engine is separated from the GUI. Porting a graphical application from Windows to Linux or Mac OS X is usually a big programming task, but simple text-mode programs can often be ported almost without any work. If all platforms have at least one GUI conforming to the protocol, a shogi programmer can make her program run on all platforms without doing much more than recompiling for each platform.
- For users of shogi software, a standardized protocol would make it possible to run several shogi engines in a single graphical interface. Instead of having to switch from one GUI to another whenever they want to use a different shogi program, they can use their favorite shogi GUI all the time, and load individual engines into the GUI.

The USI protocol, as well as the textual description on the protocol below, is based on the UCI protocol used in computer chess. Almost all the current top computer chess programs support the UCI protocol, and compatible GUIs exist for Windows, Linux and Mac OS X.

I am grateful to Stefan Meyer Kahlen, the author of the UCI protocol, for allowing me to use his work as the basis for my shogi protocol. Most of the text in section 5 below is copied verbatim from the official UCI protocol description.

## 2. Some notes about language

All communication between the engine and the GUI is done by 7-bit ASCII text, and the English language is used in the names of the various commands. English letters are used to denote the various piece types ("P" for pawn, "L" for lance, etc.). This may seem like a strange choice (at least to non-technical users), considering that the vast majority of users of shogi software are Japanese.

The justification for the use of 7-bit ASCII is to make the work easier for the engine programmer. Most shogi engines are written in low-level programming languages like C, where reading and writing Unicode is difficult, especially while trying to maintain portability. Translation of the move output from the engine into Japanese notation is assumed to be the responsibility of the GUI.

A USI engine may also have several engine parameters which can be configured by the user (e.g. style of play, piece values for the various pieces, different kinds of search parameters, and so on). Each parameter has an internal name in 7-bit ASCII which is used in all communication between the engine and the GUI. An USI engine should, in addition to the executable file, be delivered with a Unicode text file containing Japanese translations (and optionally translations into other languages) of all option names. The GUI should use the translations in this file when displaying the options to the user.

The format of the translations file is not yet decided, and will be described in a future version of this document.

## 3. Forsyth-Edwards notation for shogi

In computer chess, there is a standardized compact ASCII notation for chess positions, known as the Forsyth-Edwards notation (usually abbreviated as FEN). It was originally invented by David Forsyth more than 100 years ago, and was extended for use with chess software by Steven Edwards some time in the 1990s. For a precise definition of the FEN standard, consult section 16.1 of the [PGN specification](https://web.archive.org/web/20150326001450/http://www.yacdb.com/pgn/pgn_1616.htm#SEC16.1).

As far as I know, no equivalent standard exists for shogi (please correct me if I am wrong). This section describes a variant of FEN notation suitable for shogi.

A Shogi FEN string (SFEN from now on) consists of a single line of ASCII text containing four data fields, separated by whitespace. These are:

- **Board state.** The placement of the pieces of the board is written rank by rank, with each rank separated by a slash ('/'). The ranks are ordered from the white side, beginning with rank 'a' and ending with rank 'i' (using the standard English shogi board coordinates). For each rank, the squares are specified from file 9 to file 1. The uppercase letters "PLNSGBRK" are used for the non-promoted black pieces, while the lowercase letters "plnsgbrk" are used for the non-promoted white pieces. A promoted piece is denoted by a '+' character immediately before the piece letter, e.g. +B for a black promoted bishop. One or more contiguous empty squares are represented by a digit counting the number of squares.
- **The side to move.** This field contains of a single letter, "b" (for "black") or "w" (for "white"), depending on the current side to move.
- **Pieces in hand.** Again we use uppercase letters for black pieces, and lowercase letters for white's captured pieces. A digit before a letter means that there are several pieces of the given type in hand. The pieces are always listed in the order rook, bishop, gold, silver, knight, lance, pawn; and with all black pieces before all white pieces. As an example, in a position where black has one rook, one gold and four pawns in hand, while white has two bishops, two silvers and three pawns, the pieces in hand data field in the SFEN would look like "RG4P2b2s3p". If neither player has any pieces in hand, a single minus character ("-") is used for the pieces in hand field.
- **Move count.** An integer representing the number of the current move in the game. We are using the shogi convention for move counting, which means that we count what international players would call "plies" or "half moves". For instance, after the moves 1. P7g-7f 2. P8c-8d, the move counter is 3. The "move count" data field is optional; a program should be able to read and understand an SFEN string even if this field is missing.

As an example, the initial position in shogi is encoded like this:

`lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1`

A more complicated example is the following position, taken from the 3rd game of the 19th Ryu-O match between Sato and Watanabe:

```
  9    8    7    6    5    4    3    2    1
+----+----+----+----+----+----+----+----+----+
|    |    |    |    |    |    |    |    | Lw | a    1B 1G 1N 3P
+----+----+----+----+----+----+----+----+----+
|    | Lw |+Rb |    |    | Pb |    |    |    | b
+----+----+----+----+----+----+----+----+----+
| Pw |    |    | Pw | Bb | Gb |    | Pw | Pw | c
+----+----+----+----+----+----+----+----+----+
| Kw | Pw | Sw |    | Pw |    |    |    |    | d
+----+----+----+----+----+----+----+----+----+
| Nb | Nw |    | Pb |    |    | Gb |    |    | e
+----+----+----+----+----+----+----+----+----+
| Pb |    | Pb |    | Pb |    |    | Pb | Pb | f
+----+----+----+----+----+----+----+----+----+
|    | Pb | Sb |    |    |    |    |    |    | g
+----+----+----+----+----+----+----+----+----+
|    | Kb | Sb | Gb |    |    |    |+Rw |    | h
+----+----+----+----+----+----+----+----+----+
| Lb | Nb |    |    |+Pw |    |    |    | Lb | i    1S 
+----+----+----+----+----+----+----+----+----+
```
The SFEN for the position above is:

`8l/1l+R2P3/p2pBG1pp/kps1p4/Nn1P2G2/P1P1P2PP/1PS6/1KSG3+r1/LN2+p3L w Sbgn3p 124`

The SFEN notation described above differs from the FEN notation for chess positions in a few ways. The castling and en passant fields have been removed, for the obvious reason that castling and en passant captures do not exist in shogi. The halfmove clock has been removed because shogi has no 50-move draw rule. Finally, because captured pieces can be dropped back on the board in shogi, the "pieces in hand" data field has been added.

## 4. Differences between the UCI and USI protocols

This section is intended for programmers who are already familiar with the UCI protocol. Those who do not know anything about UCI are adviced to skip to section 5.

The USI protocol is identical to the USI protocol apart from the following changes:

- An UCI engine consists of only the executable file, but an USI engine should contain a separate "translation file", a Unicode text file containing translations of the engine and author names and all the parameter names into one or more languages. The format for the translation file is not yet decided.
- Spaces are not allowed in option names. This fixes a small defect in the UCI protocol: In a UCI chess engine, there are certain words (most notably "type") which cannot be used in option names because it would cause ambiguity. The only advantage of allowing spaces in option names is that it makes the names look cosmetically nicer in the GUI, and this advantage disappears in USI because the GUI will not display the option name, but the translation found for this option name in the translation file. For instance, if the engine has an option named "NULL_MOVE_R", the translation file can contain an English translation like "Null move reduction factor", and it is this translation that will be displayed in the GUI (assuming that the user has set English as her preferred language).
- A new option type, "filename", has been added. This is identical to the "string" option type, except for the way it is presented in the GUI. For "filename" options, the GUI will display a file browser, while for "string" options, it will let the user type a string. The new option type is intended to be used for selecting opening book files and similar tasks.
- Instead of FEN strings, SFEN strings (as described in section 3) are used. Where UCI uses `position fen <fen>`, USI uses `position sfen <sfen>`.
- Moves are written in coordinate notation, using the standard English shogi coordinates with the upper right corner (white's left corner) as square 1a and the lower left corner (black's left corner) as square 9i. Normal moves are written exactly as in UCI, except for the different coordinates (e.g. 7g7f). Promotions are written with an extra '+' character at the end of the move (e.g. 4e3c+). Drops are written as a piece letter in upper case, followed by a star ('\*'), and the destination square (e.g. P\*3d).
- Mate scores are always sent using the shogi convention for move counting: We count plies, not full moves. Where you would send `info score mate 3` in a UCI engine, you should send `info score mate 5` in a USI engine.
- All options with fixed semantics start with the prefix `USI_`. For instance, the option which is named `Hash` in UCI is named `USI_Hash` in USI.

## 5. Description of the universal shogi interface

### 5.1. General rules

- The specification is independent of the operating system. For Windows, the engine is a normal exe file, either a console or "real" windows application.
- All communication is done via standard input and output with text commands.
- The engine should boot and wait for input from the GUI, the engine should wait for the `isready` or `setoption` command to set up its internal parameters as the boot process should be as quick as possible.
- The engine must always be able to process input from stdin, even while thinking.
- All command strings the engine receives will end with '\n', also all commands the GUI receives should end with '\n'. Note: '\n' can be 0x0d or 0x0a0d or any combination depending on your OS. If you use the engine and GUI in the same OS this should be no problem if you communicate in text mode, but be aware of this when for example running a Linux engine in a Windows GUI.
- Arbitrary white space between tokens is allowed.
- The engine will always be in forced mode which means it should never start calculating or pondering without receiving a `go` command first.
- Before the engine is asked to search on a position, there will always be a position command to tell the engine about the current position.
- By default all the opening book handling is done by the GUI, but there is an option for the engine to use its own book (`USI_OwnBook` option, see below).
- If the engine or the GUI receives an unknown command or token it should just ignore it and try to parse the rest of the string in this line. Examples: `joho debug on\n` should switch the debug mode on given that joho is not defined, `debug joho on\n` will be undefined however.
- if the engine receives a command which is not supposed to come, for example `stop` when the engine is not calculating, it should also just ignore it.

### 5.2. Move format

- Normal (i.e. non-promoting, non-dropping moves) are written in English coordinate notation, using only the source and destination squares, without any piece letters or other characters (for instance `7g7f`).
- Promotion moves are written just like normal moves, except that an extra '+' character at the end of the move (for instance `4e3c+`).
- Drops are written with the English piece letter in upper case followed by a star (\*) and the destination square (for instance `P*3d`).

### 5.3. GUI to engine

These are all the command the engine gets from the interface.

- `usi` <br/>
Tell engine to use the USI (universal shogi interface). This will be sent once as a first command after program boot to tell the engine to switch to USI mode. After receiving the `usi` command the engine must identify itself with the `id` command and send the `option` commands to tell the GUI which engine settings the engine supports. After that, the engine should send `usiok` to acknowledge the USI mode. If no `usiok` is sent within a certain time period, the engine task will be killed by the GUI.

- `debug [ on | off ]` <br/>
Switch the debug mode of the engine on and off. In debug mode the engine should send additional infos to the GUI, e.g. with the `info string` command, to help debugging, e.g. the commands that the engine has received etc. This mode should be switched off by default and this command can be sent any time, also when the engine is thinking.

- `isready` <br/>
This is used to synchronize the engine with the GUI. When the GUI has sent a command or multiple commands that can take some time to complete, this command can be used to wait for the engine to be ready again or to ping the engine to find out if it is still alive. This command is also required once before the engine is asked to do any search to wait for the engine to finish initializing. This command must always be answered with `readyok` and can be sent also when the engine is calculating in which case the engine should also immediately answer with `readyok` without stopping the search.

- `setoption name <id> [value <x>]` <br/>
This is sent to the engine when the user wants to change the internal parameters of the engine. For the button type no value is needed. One string will be sent for each parameter and this will only be sent when the engine is waiting. The name and value of the option in `<id>` should not be case sensitive and can not include spaces.

- `register` <br/>
This is the command to try to register an engine or to tell the engine that registration will be done later. This command should always be sent if the engine has sent `registration error` at program startup. <br/>
The following tokens are allowed:

    - `later` <br/>
The user doesn't want to register the engine now.
    - `name <x>` <br/>
The engine should be registered with the name `<x>`
    - `code <y>` <br/>
The engine should be registered with the code `<y>` <br/>
Example:

```
	"register later"
	"register name Stefan MK code 4359874324"
```
    
- `usinewgame` <br/>
This is sent to the engine when the next search (started with `position` and `go`) will be from a different game. This can be a new game the engine should play or a new game it should analyse but also the next position from a testsuite with positions only. As the engine's reaction to `usinewgame` can take some time the GUI should always send `isready` after `usinewgame` to wait for the engine to finish its operation.

- `position [sfen <sfenstring> | startpos ] moves <move1> ... <movei>` <br/>
Set up the position described in sfenstring on the internal board and play the moves on the internal board. If the game was played from the start position, the string `startpos` will be sent. <br/> <br/>
Note: If this position is from a different game than the last position sent to the engine, the GUI should have sent a `usinewgame` inbetween.

- `go` <br/>
Start calculating on the current position set up with the `position` command. There are a number of commands that can follow this command, all will be sent in the same string. <br/> <br/>
If one command is not sent its value should be interpreted as if it would not influence the search.

    - `searchmoves <move1> ... <movei>` <br/>
Restrict search to this moves only <br/> <br/>
Example: After `position startpos` and `go infinite searchmoves 7g7f 2g2f`, the engine should only search the two moves P-7f and P-2f in the initial position.

    - `ponder` <br/>
Start searching in pondering mode. Do not exit the search in ponder mode, even if it's mate! This means that the last move sent in in the position string is the ponder move. The engine can do what it wants to do, but after a `ponderhit` command it should execute the suggested move to ponder on. This means that the ponder move sent by the GUI can be interpreted as a recommendation about which move to ponder. However, if the engine decides to ponder on a different move, it should not display any mainlines as they are likely to be misinterpreted by the GUI because the GUI expects the engine to ponder on the suggested move.

    - `btime <x>` <br/>
Black has x milliseconds left on the clock
    - `wtime <x>` <br/>
White has x milliseconds left on the clock
    - `binc <x>` <br/>
Black increment per move i milliseconds if x > 0.
    - `winc <x>` <br/>
White increment per move i milliseconds if x > 0.
    - `byoyomi <x>` <br/>
(Shogidogoro) Amount (in milliseconds) the clocks are allowed to go negative before the player is flagged (which by default would be 0). Negative times left on the clock will be reset to 0 for the next turn.
    - `movestogo <x>` <br/>
There are x moves to the next time control. This will only be sent if x > 0. If you don't get this and get the `wtime` and `btime`, it's sudden death.
    - `depth <x>` <br/>
Search `x` plies only.
    - `nodes <x>` <br/>
Search `x` nodes only.
    - `mate <x>` <br/>
Original proposal was: Search for a mate in `x` (half)moves. In Shogidogoro this has been changed to `x` being a time specification rather than a depth, either a number indicating milliseconds, or the word `infinite`. And such a search would have its own dedicated set of replies, different from other `go commands` (see the `checkmate` command). <br/>
The command also seems largely redundant, as even engines that do have a special tsume search mode could activate this whenever they get to search on a position where one side lacks a King, and there already are plenty options on the `go` command to control thinking time. The solution could then be reported through the usual `info pv` commands, and failure through a `bestmove resign` command.
    - `movetime <x>` <br/>
Search exactly `x` milliseconds.
    - `infinite` <br/>
Search until the `stop` command is received. Do not exit the search without being told so in this mode!
- `stop` <br/>
Stop calculating as soon as possible. Don't forget the `bestmove` and possibly the `ponder` token when finishing the search.

- `ponderhit` <br/>
The user has played the expected move. This will be sent if the engine was told to ponder on the same move the user has played. The engine should continue searching but switch from pondering to normal search.

- `gameover [win | lose | draw]` <br/>
(Shogidogoro) Informs the engine that the game has ended with the specified result, from the engine's own point or view.

- `quit` <br/>
Quit the program as soon as possible.

### 5.4. Engine to GUI

- `id` <br/>
  `name <x>` <br/>
This must be sent after receiving the usi command to identify the engine, e.g. `id name Shredder X.Y\n` <br/>
  `author <x>` <br/>
This must be sent after receiving the usi command to identify the engine, e.g. `id author Stefan MK\n`

- `usiok` <br/>
Must be sent after the id and optional options to tell the GUI that the engine has sent all infos and is ready in usi mode.

- `readyok` <br/>
This must be sent when the engine has received an `isready` command and has processed all input and is ready to accept new commands now. It is usually sent after a command that can take some time to be able to wait for the engine, but it can be used anytime, even when the engine is searching, and must always be answered with `readyok`.

- `bestmove <move1> [ponder <move2>]` <br/>
`bestmove [resign | win]` <br/>
The engine has stopped searching and found the move `<move>` best in this position. The engine can send the move it likes to ponder on. The engine must not start pondering automatically. this command must always be sent if the engine stops searching, also in pondering mode if there is a `stop` command, so for every `go` command a `bestmove` command is needed! ((Shogidogoro) except after the combination "go mate", where this must be a `checkmate` command instead.) Directly before that the engine should send a final `info` command with the final search information, the the GUI has the complete statistics about the last search. <br/> <br/>
(Shogidogoro) Engines can resign a game by sending a `bestmove` command with the word `resign` instead of a valid move. To claim a win the word `win` can be used similarly.

- `checkmate [<move1> ... <movei> | nomate | timeout | notimplemented]` <br/>
(Shogidogoro) Sent when a "go mate" search terminates, instead of a `bestmove` command, to convey the main line of the solution rather than just a single move. If it could be proven no solution exists, the word `nomate` will be sent instead of a main line, while `timeout` here would indicate the search was inconclusive. Finally, engines that do not implement the "go mate" function should reply to it with `checkmate notimplemented`.

- `copyprotection` <br/>
This is needed for copyprotected engines. After the usiok command the engine can tell the GUI, that it will check the copy protection now. This is done by `copyprotection checking`. If the check is ok the engine should send `copyprotection ok`, otherwise `copyprotection error`. <br/> <br/>
If there is an error the engine should not function properly but should not quit alone. If the engine reports `copyprotection error` the GUI should not use this engine and display an error message instead! <br/> <br/>
The code in the engine can look like this:

```
	TellGUI("copyprotection checking\n");
	// ... check the copy protection here ...
	if(ok)
	  TellGUI("copyprotection ok\n");
	else
          TellGUI("copyprotection error\n");
```
    
- `registration` <br/>
This is needed for engines that need a username and/or a code to function with all features. Analogously to the `copyprotection` command the engine can send `registration checking` after the usiok command followed by either `registration ok` or `registration error`. Also after every attempt to register the engine it should answer with `registration checking` and then either `registration ok` or `registration error`. <br/> <br/>
In contrast to the `copyprotection` command, the GUI can use the engine after the engine has reported an error, but should inform the user that the engine is not properly registered and might not use all its features. <br/> <br/>
In addition the GUI should offer to open a dialog to enable registration of the engine. To try to register an engine the GUI can send the `register` command. The GUI has to always answer with the `register` command if the engine sends `registration error` at engine startup (this can also be done with `register later`) and tell the user somehow that the engine is not registered. This way the engine knows that the GUI can deal with the registration procedure and the user will be informed that the engine is not properly registered.

- `info` <br/>
The engine wants to send information to the GUI. This should be done whenever one of the info has changed. <br/> <br/>
The engine can send only selected infos or multiple infos with one info command, e.g. `info currmove 2g2f currmovenumber 1 or info depth 12 nodes 123456 nps 100000`. <br/> <br/>
Also all infos belonging to the pv should be sent together, e.g.

```
	info depth 2 score cp 214 time 1242 nodes 2124 nps 34928 pv 2g2f 8c8d 2f2e
```

I suggest to start sending currmove, currmovenumber, currline and refutation only after one second in order to avoid too much traffic. <br/> <br/>
Additional info:

- `depth <x>` <br/>
Search depth in plies.
- `seldepth <x>` <br/>
Selective search depth in plies. If the engine sends seldepth there must also be a `depth` present in the same string.
- `time <x>` <br/>
The time searched in ms. This should be sent together with the pv.
- `nodes <x>` <br/>
x nodes searched. The engine should send this info regularly.
- `pv <move1> ... <movei>` <br/>
The best line found.
- `multipv <num>` <br/>
This for the multi pv mode. For the best move/pv add `multipv 1` in the string when you send the pv. In k-best mode, always send all k variants in k strings together.
- `score`
    - `cp <x>` <br/>
The score from the engine's point of view, in centipawns.
    - `mate <y>` <br/>
Mate in y plies. If the engine is getting mated, use negative values for y.
    - `lowerbound` <br/>
The score is just a lower bound.
    - `upperbound` <br/>
The score is just an upper bound.

- `currmove <move>` <br/>
Currently searching this move.
- `currmovenumber <x>` <br/>
Currently searching move number x, for the first move x should be 1, not 0.
- `hashfull <x>` <br/>
The hash is x permill full. The engine should send this info regularly.
- `nps <x>` <br/>
x nodes per second searched. the engine should send this info regularly.
- `cpuload <x>` <br/>
The cpu usage of the engine is x permill.
- `string <str>` <br/>
Any string str which will be displayed be the engine. if there is a string command the rest of the line will be interpreted as `<str>`.
- `refutation <move1> <move2> ... <movei>` <br/>
Move `<move1>` is refuted by the line `<move2>` ... `<movei>`, where i can be any number >= 1. Example: after move 8h2b+ is searched, the engine can send `info refutation 8h2b+ 1c2b` if 1c2b is the best answer after 8h2b+ or if 1c2b refutes the move 8h2b+. If there is no refutation for 8h2b+ found, the engine should just send `info refutation 8h2b+`. The engine should only send this if the option `USI_ShowRefutations` is set to true.
- `currline <cpunr> <move1> ... <movei>` <br/>
This is the current line the engine is calculating. `<cpunr>` is the number of the cpu if the engine is running on more than one cpu. `<cpunr>` = 1,2,3.... If the engine is just using one cpu, `<cpunr>` can be omitted. If `<cpunr>` is greater than 1, always send all k lines in k strings together. The engine should only send this if the option `USI_ShowCurrLine` is set to true. <br/> <br/> <br/>

- `option` <br/>
This command tells the GUI which parameters can be changed in the engine. This should be sent once at engine startup after the `usi` and the `id` commands if any parameter can be changed in the engine. The GUI should parse this and build a dialog for the user to change the settings. Note that not every option should appear in this dialog, as some options like `USI_Ponder`, `USI_AnalyseMode`, etc. are better handled elsewhere or are set automatically. <br/> <br/>
If the user wants to change some settings, the GUI will send a setoption command to the engine. <br/> <br/>
Note that the GUI need not send the setoption command when starting the engine for every option if it doesn't want to change the default value. For all allowed combinations see the examples below, as some combinations of this tokens don't make sense. <br/> <br/>
One string will be sent for each parameter.

    - `name <id>` <br/>
The option has the name `<id>`. Whitespace is not allowed in an option name. Note that the name should normally not be displayed directly in the GUI: The GUI should look up the option name in the translation file, and present the translation into the users preferred language in the engine's option dialog. <br/> <br/>
Certain options have a fixed value for `<id>`, which means that the semantics of this option is fixed. Usually those options should not be displayed in the normal engine options window of the GUI but get a special treatment. `USI_Pondering` for example should be set automatically when pondering is enabled or disabled in the GUI options. The same for `USI_AnalyseMode` which should also be set automatically by the GUI. All those certain options have the prefix `USI_`. If the GUI gets an unknown option with the prefix `USI_`, it should just ignore it and not display it in the engine's options dialog. <br/> <br/>
The options with fixed semantics are: <br/> <br/>
`<id>` = `USI_Hash`, type spin <br/>
The value in MB for memory for hash tables can be changed, this should be answered with the first `setoptions` command at program boot if the engine has sent the appropriate option name Hash command, which should be supported by all engines! So the engine should use a very small hash first as default. <br/> <br/>
`<id>` = `USI_Ponder`, type check <br/>
This means that the engine is able to ponder (i.e. think during the opponent's time). The GUI will send this whenever pondering is possible or not. Note: The engine should not start pondering on its own if this is enabled, this option is only needed because the engine might change its time management algorithm when pondering is allowed. <br/> <br/>
`<id>` = `USI_OwnBook`, type check <br/>
This means that the engine has its own opening book which is accessed by the engine itself. If this is set, the engine takes care of the opening book and the GUI will never execute a move out of its book for the engine. If this is set to false by the GUI, the engine should not access its own book. <br/> <br/>
`<id>` = `USI_MultiPV`, type spin <br/>
The engine supports multi best line or k-best mode. The default value is 1. <br/> <br/>
`<id>` = `USI_ShowCurrLine`, type check <br/>
The engine can show the current line it is calculating. See info currline above. This option should be false by default. <br/> <br/>
`<id>` = `USI_ShowRefutations`, type check <br/>
The engine can show a move and its refutation in a line. See info refutations above. This option should be false by default. <br/> <br/>
`<id>` = `USI_LimitStrength`, type check <br/>
The engine is able to limit its strength to a specific dan/kyu number. This should always be implemented together with `USI_Strength`. This option should be false by default. <br/> <br/>
`<id>` = `USI_Strength`, type spin <br/>
The engine can limit its strength within the given interval. Negative numbers are kyu levels, while positive numbers are amateur dan levels. If `USI_LimitStrength` is set to false, this value should be ignored. If `USI_LimitStrength` is set to true, the engine should play with this specific strength. This option should always be implemented together with `USI_LimitStrength`. <br/> <br/>
`<id>` = `USI_AnalyseMode`, type check <br/>
The engine wants to behave differently when analysing or playing a game. For example when playing it can use some kind of learning, or an asymetric evaluation function. The GUI should set this option to false if the engine is playing a game, and to true if the engine is analysing.

    - `type <t>` <br/>
The option has type `t`. There are 5 different types of options the engine can send: <br/> <br/>
`check` <br/>
A checkbox that can either be true or false. <br/> <br/>
`spin` <br/>
A spin wheel or slider that can be an integer in a certain range. <br/> <br/>
`combo` <br/>
A combo box that can have different predefined strings as a value. <br/> <br/>
`button` <br/>
A button that can be pressed to send a command to the engine <br/> <br/>
`string` <br/>
A text field that has a string as a value, an empty string has the value `<empty>`. <br/> <br/>
`filename` <br/>
Similar to string, but is presented as a file browser instead of a text field in the GUI.

    - `default <x>` <br/>
The default value of this parameter is `x`.

    - `min <x>` <br/>
The minimum value of this parameter is `x`.

    - `max <x>` <br/>
The maximum value of this parameter is `x`.

Here are some examples illustrating the different types of options:

```
	"option name Nullmove type check default true\n"
	"option name Selectivity type spin default 2 min 0 max 4\n"
	"option name Style type combo default Normal var Solid var Normal var Risky\n"
	"option name LearningFile type filename default /shogi/my-shogi-engine/learn.bin"
	"option name ResetLearning type button\n"
```
    
### 5.5. Examples

This is how the communication when the engine boots can look like:

```
GUI     engine
// tell the engine to switch to USI mode
usi

// engine identify  

        id name Glaurung Shogi 0.1
        id author Tord Romstad

// engine sends the options it can change
// the engine can change the hash size from 8 to 1024 MB

        option name USI_Hash type spin default 16 min 8 max 1024

// the engine can switch off Nullmove and set the playing style

        option name Nullmove type check default true
        option name Style type combo default Normal var Solid var Normal var Risky
// the engine has sent all parameters and is ready

        usiok

// Note: here the GUI can already send a "quit" command if it just
// wants to find out details about the engine, so the engine should
// not initialize its internal parameters before here.

// now the GUI sets some values in the engine

// set hash to 128 MB

setoption name USI_Hash value 128

// set play style to "Solid"

setoption name Style value Solid


// waiting for the engine to finish initializing

// this command and the answer is required here!

isready

// engine has finished setting up the internal values

        readyok

// now we are ready to go

// if the GUI is supporting it, tell the engine that is is
// searching on a game that it hasn't searched on before

usinewgame

// if the engine supports the "USI_AnalyseMode" option and the next
// search is supposed to be an analysis, the GUI should set
// "USI_AnalyseMode" to true if it is currently set to false with this 
// engine.

setoption name USI_AnalyseMode value true

// tell the engine to search infinite from the start position after
// 1. P-7f 2. P-3d:

position startpos moves 7g7f 3c3d
go infinite

// the engine starts sending infos about the search to the GUI

// (only some examples are given)

        info depth 1
        info score cp -2 depth 1 nodes 13 time 5 pv 8h2b+ 3a2b
        info depth 1
        info score cp 13 depth 1 nodes 24 time 11 pv 2g2f
	info depth 2
        info nps 15937
        info score cp 5 depth 2 nodes 255 time 90 pv 2g2f 4c4d
        info depth 3 seldepth 7
        info nps 26437
        info score cp 20  depth 3 nodes 1123 time 315 pv 2g2f 4c4d 2f2e
        ...

// here the user has seen enough and asks to stop the searching

stop

// the engine has finished searching and is sending the bestmove command
// which is needed for every "go" command sent to tell the GUI
// that the engine is ready again

        bestmove 2g2f ponder 4c4d
```