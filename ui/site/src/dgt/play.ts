import { parseFen } from 'chessops/fen';

export default function (token: string) {
  // here's the new setting keys
  [
    'dgt-livechess-url',
    'dgt-speech-keywords',
    'dgt-speech-synthesis',
    'dgt-speech-announce-all-moves',
    'dgt-speech-announce-move-format',
    'dgt-verbose'
  ].forEach(k => {
    console.log(k, localStorage.getItem(k));
  });

  // put your UI in there
  const root = document.getElementById('dgt-play-zone') as HTMLDivElement;
  const consoleOutput = document.getElementById('dgt-play-zone-log') as HTMLPreElement;

  console.log(parseFen('rnbqkbnr/pppp1ppp/8/8/3pP3/2P5/PP3PPP/RNBQKBNR b KQkq - 1 3'));

  // and your code in here.

  /**
   * CONFIGURATION VALUES
   */
  const liveChessURL = localStorage.getItem('dgt-livechess-url');
  const announceAllMoves = (localStorage.getItem('dgt-speech-announce-all-moves') == "true") ? true : false;
  const verbose = (localStorage.getItem('dgt-verbose') == "true") ? true : false;
  const speechSynthesisOn = (localStorage.getItem('dgt-speech-synthesis') == "true") ? true : false;
  const announceMoveFormat = localStorage.getItem('dgt-speech-announce-move-format');
  const voice = localStorage.getItem('dgt-speech-voice');
  var keywords = {
    "K": "King",
    "Q": "Queen",
    "R": "Rook",
    "B": "Bishop",
    "N": "Knight",
    "P": "Pawn",
    "x": "Takes",
    "+": "Check",
    "#": "Checkmate",
    "(=)": "Game ends in draw",
    "O-O-O": "Castles queenside",
    "O-O": "Castles kingside",
    "white": "White",
    "black": "Black",
    "wins by": "wins by",
    "timeout": "timeout",
    "resignation": "resignation",
    "illegal": "illegal",
    "move": "move"
  }
  try {
    var tempString = localStorage.getItem('dgt-speech-keywords');
    keywords = tempString && JSON.parse(tempString);
  } catch (error) {
    console.error("Invalid JSON Object for Speech Keywords. Using English default.");
  }

  //Lichess Integration with Board API

  /**
   * GLOBAL VATIABLES
   */
  var time = new Date(); //A Global time object
  var currentGameId = ''; //Track which is the current Game, in case there are several open games
  var currentGameColor = ''; //Track which color is being currently played by the player
  var me; //Track my information
  var gameInfoMap = new Map(); //A collection of key values to store game inmutable information of all open games
  var gameStateMap = new Map(); //A collection of key values to store the changing state of all open games
  var gameConnectionMap = new Map(); //A collection of key values to store the network status of a game
  var gameChessBoardMap = new Map(); //A collection of Chess Boads representing the current board of the games
  var eventSteamStatus = { connected: false, lastEvent: time.getTime() }; //An object to store network status of the main eventStream
  //const dgtBoard = new BoardManager(); //Store the board manager object

  /***
   * Bind console output to HTML pre Element
   */
  rewireLoggingToElement(consoleOutput, root, true);
  function rewireLoggingToElement(eleLocator: HTMLPreElement, eleOverflowLocator: HTMLDivElement, autoScroll: boolean) {
    //Clear the console
    eleLocator.innerHTML = ""
    //Bind to all types of console messages
    fixLoggingFunc('log');
    fixLoggingFunc('debug');
    fixLoggingFunc('warn');
    fixLoggingFunc('error');
    fixLoggingFunc('info');
    fixLoggingFunc('table');

    function fixLoggingFunc(name: string) {
      console['old' + name] = console[name];
      //Rewire function
      console[name] = function () {
        var output = "";
        for (let i = 0; i < arguments.length; i++) {
          let arg = arguments[i];
          output += "<span class=\"log-" + (typeof arg) + " log-" + name + "\">";
          if (
            typeof arg === "object" &&
            typeof JSON === "object" &&
            typeof JSON.stringify === "function"
          ) {
            output += JSON.stringify(arg);
          } else {
            output += arg;
          }
          output += "</span>&nbsp;";
        }
        if (output != "*" && output != ":")
          output += "<br>";
        if (autoScroll) {
          const isScrolledToBottom = eleOverflowLocator.scrollHeight - eleOverflowLocator.clientHeight <= eleOverflowLocator.scrollTop + 1;
          eleLocator.innerHTML += output;
          if (isScrolledToBottom) {
            eleOverflowLocator.scrollTop = eleOverflowLocator.scrollHeight - eleOverflowLocator.clientHeight;
          }
        } else {
          eleLocator.innerHTML += output;
        }
        //Call original function
        try {
          console['old' + name].apply(undefined, arguments);
        } catch {
          console['olderror'].apply(undefined, ['Error when loggin']);
        }
      };
    }
  }

  /**
   * Wait some time without blocking other code
   *
   * @param {number} ms - The number of milliseconds to sleep
   */
  function sleep(ms: number = 0) {
    return new Promise(r => setTimeout(r, ms));
  }


  /**
   * GET /api/account
   * 
   * Get my profile
   * 
   * Shows Public informations about the logged in user.
   * 
   * Example
   * {"id":"andrescavallin","username":"andrescavallin","online":true,"perfs":{"blitz":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"bullet":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"correspondence":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"classical":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true},"rapid":{"games":0,"rating":1500,"rd":350,"prog":0,"prov":true}},"createdAt":1599930231644,"seenAt":1599932744930,"playTime":{"total":0,"tv":0},"language":"en-US","url":"http://localhost:9663/@/andrescavallin","nbFollowing":0,"nbFollowers":0,"count":{"all":0,"rated":0,"ai":0,"draw":0,"drawH":0,"loss":0,"lossH":0,"win":0,"winH":0,"bookmark":0,"playing":0,"import":0,"me":0},"followable":true,"following":false,"blocking":false,"followsYou":false}
   * */
  function getProfile() {
    //Log intention
    if (verbose) console.log('getProfile - About to call /api/account');
    fetch('/api/account', {
      headers: { 'Authorization': 'Bearer ' + token }
    }).then(
      function (response) {
        return response.json();
      }).then(
        function (data) {
          //Log raw data received
          if (verbose) console.log('/api/account Response:' + JSON.stringify(data));
          //Diplay Title + UserName . Title may be undefined
          console.log("\n");
          console.log("┌─────────────────────────────────────────────────────┐");
          console.log("│ " + (typeof (data.title) == "undefined" ? '' : data.title) + ' ' + data.username);
          //Display performance ratings
          console.table(data.perfs);
          //Store my profile
          me = data;
        })
      .catch(
        err => { console.error('getProfile - Error. ' + err.message) }
      );
  }

  /** 
    GET /api/stream/event
    Stream incoming events

    Stream the events reaching a lichess user in real time as ndjson.

    Each line is a JSON object containing a type field. Possible values are:

    challenge Incoming challenge
    gameStart Start of a game
    When the stream opens, all current challenges and games are sent.

    Examples:
    {"type":"gameStart","game":{"id":"kjKzl2MO"}}
    {"type":"challenge","challenge":{"id":"WTr3JNcm","status":"created","challenger":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1362,"provisional":true,"online":true,"lag":3},"destUser":{"id":"godking666","name":"Godking666","title":null,"rating":1910,"online":true,"lag":3},"variant":{"key":"standard","name":"Standard","short":"Std"},"rated":false,"speed":"rapid","timeControl":{"type":"clock","limit":900,"increment":10,"show":"15+10"},"color":"white","perf":{"icon":"#","name":"Rapid"}}}
 */
  async function connectToEventStream() {
    //Log intention
    if (verbose) console.log('connectToEventStream - About to call /api/stream/event');
    const response = await fetch('/api/stream/event', {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      if (verbose) console.log('connectToEventStream - Chunk received', value);
      //Update connection status
      eventSteamStatus = { connected: true, lastEvent: time.getTime() };
      //Response may contain several JSON objects on the same chunk separated by \n . This may create an empty element at the end.
      var JSONArray = value.split('\n');
      for (let i = 0; i < JSONArray.length; i++) {
        //Skip empty elements that may have happened witht the .split('\n')
        if (JSONArray[i].length > 2) {
          try {
            var data = JSON.parse(JSONArray[i]);
            //JSON data found, let's check if this is a game that started. field type is mandatory except on http 4xx
            if (data.type == "gameStart") {
              if (verbose) console.log('connectToEventStream - gameStart event arrived. GameId: ' + data.game.id);
              try {
                //Connect to that game's stream
                connectToGameStream(data.game.id);
              }
              catch (error) {
                //This will trigger if connectToGameStream fails
                console.error('connectToEventStream - Failed to connect to game stream. ' + Error(error).message);
              }
            }
            else if (data.type == "challenge") {
              //Challenge received
              //TODO
            }
            else if (response.status >= 400) {
              console.warn('connectToEventStream - ' + data.error);
            }
          }
          catch (error) {
            console.error('connectToEventStream - Unable to parse JSON or Unexpected error. ' + Error(error).message);
          }
        }
        else {
          //Signal that some empty message arrived. This is normal to keep the connection alive.
          if (verbose) console.log("*"); //process.stdout.write("*"); Replace to support browser
        }
      }
    }

    console.warn('connectToEventStream - Event Stream ended by server');
    //Update connection status
    eventSteamStatus = { connected: false, lastEvent: time.getTime() };
  }

  /**
  Stream Board game state
   
  GET /api/board/game/stream/{gameId}
   
  Stream the state of a game being played with the Board API, as ndjson.
  Use this endpoint to get updates about the game in real-time, with a single request.
  Each line is a JSON object containing a type field. Possible values are:
   
  gameFull Full game data. All values are immutable, except for the state field.
  gameState Current state of the game. Immutable values not included. Sent when a move is played, a draw is offered, or when the game ends.
  chatLine Chat message sent by a user in the room "player" or "spectator".
  The first line is always of type gameFull.
   
  Examples:
   
  New Game
  {"id":"972RKuuq","variant":{"key":"standard","name":"Standard","short":"Std"},"clock":{"initial":900000,"increment":10000},"speed":"rapid","perf":{"name":"Rapid"},"rated":false,"createdAt":1586647003562,"white":{"id":"godking666","name":"Godking666","title":null,"rating":1761},"black":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1362,"provisional":true},"initialFen":"startpos","type":"gameFull","state":{"type":"gameState","moves":"e2e4","wtime":900000,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}}
  First Move
  {"type":"gameState","moves":"e2e4","wtime":900000,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}
  Middle Game
  {"type":"gameState","moves":"e2e4 c7c6 g1f3 d7d5 e4e5 c8f5 d2d4 e7e6 h2h3 f5e4 b1d2 f8b4 c2c3 b4a5 d2e4 d5e4 f3d2 d8h4 g2g3 h4e7 d2e4 e7d7 e4d6 e8f8 d1f3 g8h6 c1h6 h8g8 h6g5 a5c7 e1c1 c7d6 e5d6 d7d6 g5f4 d6d5 f3d5 c6d5 f4d6 f8e8 d6b8 a8b8 f1b5 e8f8 h1e1 f8e7 d1d3 a7a6 b5a4 g8c8 a4b3 b7b5 b3d5 e7f8","wtime":903960,"btime":847860,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}
  After reconnect
  {"id":"ZQDjy4sa","variant":{"key":"standard","name":"Standard","short":"Std"},"clock":{"initial":900000,"increment":10000},"speed":"rapid","perf":{"name":"Rapid"},"rated":true,"createdAt":1586643869056,"white":{"id":"gg60","name":"gg60","title":null,"rating":1509},"black":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1433,"provisional":true},"initialFen":"startpos","type":"gameFull","state":{"type":"gameState","moves":"e2e4 c7c6 g1f3 d7d5 e4e5 c8f5 d2d4 e7e6 h2h3 f5e4 b1d2 f8b4 c2c3 b4a5 d2e4 d5e4 f3d2 d8h4 g2g3 h4e7 d2e4 e7d7 e4d6 e8f8 d1f3 g8h6 c1h6 h8g8 h6g5 a5c7 e1c1 c7d6 e5d6 d7d6 g5f4 d6d5 f3d5 c6d5 f4d6 f8e8 d6b8 a8b8 f1b5 e8f8 h1e1 f8e7 d1d3 a7a6 b5a4 g8c8 a4b3 b7b5 b3d5 e7f8 d5b3 a6a5 a2a3 a5a4 b3a2 f7f6 e1e6 f8f7 e6b6","wtime":912940,"btime":821720,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"resign","winner":"white"}}
  Draw Offered
  {"type":"gameState","moves":"e2e4 c7c6","wtime":880580,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":true,"status":"started"}
  After draw accepted
  {"type":"gameState","moves":"e2e4 c7c6","wtime":865460,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"draw"}
  Out of Time
  {"type":"gameState","moves":"e2e3 e7e5","wtime":0,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"outoftime","winner":"black"}
  Mate
  {"type":"gameState","moves":"e2e4 e7e5 f1c4 d7d6 d1f3 b8c6 f3f7","wtime":900480,"btime":907720,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"mate"}
  Promotion
  {"type":"gameState","moves":"e2e4 b8c6 g1f3 c6d4 f1c4 e7e5 d2d3 d7d5 f3d4 f7f6 c4d5 f6f5 f2f3 g7g6 e1g1 c7c6 d5b3 d8d5 e4d5 a8b8 d4e6 f8b4 e6c7 e8e7 d5d6 e7f6 d6d7 b4f8 d7d8q","wtime":2147483647,"btime":2147483647,"winc":0,"binc":0,"wdraw":false,"bdraw":false,"status":"started"}
  @param {string} gameId - The alphanumeric identifier of the game to be tracked
   */

  async function connectToGameStream(gameId) {
    //Log intention
    if (verbose) console.log('connectToGameStream - About to call /api/board/game/stream/' + gameId);
    const response = await fetch('/api/board/game/stream/' + gameId, {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      //Log raw data received
      if (verbose) console.log('connectToGameStream - board game stream recevied:', value);
      //Update connection status
      gameConnectionMap.set(gameId, { connected: true, lastEvent: time.getTime() });
      //Response may contain several JSON objects on the same chunk separated by \n . This may create an empty element at the end.
      var JSONArray = value.split('\n');
      for (let i = 0; i < JSONArray.length; i++) {
        //Skip empty elements that may have happened witht the .split('\n')
        if (JSONArray[i].length > 2) {
          try {
            var data = JSON.parse(JSONArray[i]);
            //The first line is always of type gameFull.
            if (data.type == "gameFull") {
              if (!verbose) console.clear();
              //Log game Summary 
              //logGameSummary(data);
              //Store game inmutable information on the gameInfoMap dictionary collection 
              gameInfoMap.set(gameId, data);
              //Store game state on the gameStateMap dictionary collection
              gameStateMap.set(gameId, data.state);
              //Update the ChessBoard to the ChessBoard Map
              /* PENDING MIGRATION initializeChessBoard(gameId, data); */
              //Log the state. Note that we are doing this after storing the state and initializing the chess.js board
              /* pending migration logGameState(gameId);*/
              //Call chooseCurrentGame to determine if this stream will be the new current game
              /* PENDING MIGRATION chooseCurrentGame();*/
            }
            else if (data.type == "gameState") {
              if (!verbose) console.clear();
              //Update the ChessBoard Map
              /* PENDING MIGRATION updateChessBoard(gameId, gameStateMap.get(gameId), data);*/
              //Update game state with most recent state
              gameStateMap.set(gameId, data);
              //Log the state. Note that we are doing this after storing the state and updating the chess.js board
              /* PENDING MIGRATION logGameState(gameId);*/
            }
            else if (data.type == "chatLine") {
              //Received chat line
              //TODO
            }
            else if (response.status >= 400) {
              console.log('connectToGameStream - ' + data.error);
            }
          }
          catch (error) {
            console.error('connectToGameStream - No valid game data or Unexpected error. ' + Error(error).message);
          }
        }
        else {
          //Signal that some empty message arrived
          if (verbose) console.log(":"); //process.stdout.write(":"); Changed to support browser
        }
      }
    }
    //End Stream output.end();
    console.warn('connectToGameStream - Game ' + gameId + ' Stream ended.');
    //Update connection state
    gameConnectionMap.set(gameId, { connected: false, lastEvent: time.getTime() });
  }


  /**
  * mainLoop() is a function that tries to keep the streams connected at all times, up to a maximum of 20 retries
  */
  async function mainLoop() {
    //Program ends after 20 re-connection attempts
    for (let attempts = 0; attempts < 20; attempts++) {
      //Connect to main event stream
      connectToEventStream();
      //On the first time, if there are no games, it may take several seconds to receive data so lets wait a bit. Also give some time to connect to started games
      await sleep(5000);
      //Now enter a loop to monitor the connection
      do {
        //sleep 5 seconds and just listen to events
        await sleep(5000);
        //Check if any started games are disconnected
        for (let [gameId, networkState] of gameConnectionMap) {
          if (!networkState.connected && gameStateMap.get(gameId).status == "started") {
            //Game is not conencted and has not finished, reconnect
            if (verbose) console.log(`Started game is disconnected. Attempting reconnection for gameId: ${gameId}`);
            connectToGameStream(gameId);
          }
        }
      }
      while (eventSteamStatus.connected)
      //This means event stream is not connected
      console.warn("No connection to event stream. Attempting re-connection. Attempt: " + attempts);
    }
    console.error("No connection to event stream after maximum number of attempts 20. Reload page to start again.");

  }


  /**
   * Show the profile and then
   * Start the Main Loop
   */
  //start();
  //connectToBoardEvents();  //Connect to events from DGT Board
  getProfile();
  mainLoop();
  ////keyboardInputHandler(); //This is to allow moves to be entered by keyboard
  ////boardInputHandler(); //start monitoring moves one at a time instead of by events
  console.log('Not Hanged');
}
