"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['announceAllMoves']="Anunciar tots els moviments";i['announceMoveFormat']="Format de l\\'anunci dels moviments";i['asALastResort']=s("Com a últim recurs, distribueix el tauler de manera idèntica al de Lichess, després %s");i['boardWillAutoConnect']="El tauler es connectarà automàticament a qualsevol partida que ja estigui en curs o a qualsevol nova partida que comenci. Aviat s\\'activarà la possibilitat d\\'escollir quina partida jugar.";i['checkYouHaveMadeOpponentsMove']="Comprova que primer has realitzat el moviment de l\\'oponent al tauler DGT. Reverteix el teu moviment. Juga de nou.";i['clickToGenerateOne']="Cliqueu per generar-ne un";i['configurationSection']="Secció de configuració";i['configure']="Configurar";i['configureVoiceNarration']="Configura la narració de veu dels moviments realitzats perquè puguis mantenir la mirada en el tauler.";i['debug']="Depuració";i['dgtBoard']="Tauler DGT";i['dgtBoardConnectivity']="Connectivitat del tauler DGT";i['dgtBoardLimitations']="Limitacions del tauler DGT";i['dgtBoardRequirements']="Requeriments del tauler DGT";i['dgtConfigure']="Configuració DGT";i['dgtPlayMenuEntryAdded']=s("L\\'entrada %s s\\'ha afegit al menú JUGAR a la part superior.");i['downloadHere']=s("Pots descarregar el programari aquí: %s.");i['enableSpeechSynthesis']="Habilitar síntesi de la parla";i['ifLiveChessRunningElsewhere']=s("Si %1$s s\\'està executant en un ordinador o un port diferent, hauràs d\\'establir l\\'adreça IP i el port a la %2$s.");i['ifLiveChessRunningOnThisComputer']=s("Si %1$s s\\'està executant en aquest ordinador, pots comprovar la teva connexió %2$s.");i['ifMoveNotDetected']="Si no es detecta un moviment";i['keepPlayPageOpen']="La pàgina del tauler de joc ha de romandre oberta al teu navegador. No cal que estigui visible, pots minimitzar-la o col·locar-la al costat de la pàgina de la partida de Lichess, però no la tanquis o el tauler deixarà de funcionar.";i['keywordFormatDescription']="Les paraules clau estan en format JSON. S\\'utilitzen per a traduir moviments i resultats al teu idioma. Per defecte és l\\'anglès, però no dubtis a canviar-lo.";i['keywords']="Paraules clau";i['lichessAndDgt']="Lichess i DGT";i['lichessConnectivity']="Connectivitat amb Lichess";i['moveFormatDescription']="SAN és l\\'estàndard en Lichess, per exemple \\\"Nf6\\\". UCI és comú en motors, per exemple \\\"g8f6\\\".";i['noSuitableOauthToken']="No s\\'ha creat un token OAuth adequat.";i['openingThisLink']="obrint aquest enllaç";i['playWithDgtBoard']="Jugar amb un tauler DGT";i['reloadThisPage']="Recarrega aquesta pàgina";i['selectAnnouncePreference']="Selecciona SÍ per anunciar tant els teus moviments com els moviments de l\\'oponent. Selecciona NO per anunciar només els moviments de l\\'oponent.";i['speechSynthesisVoice']="Veu de la síntesi de la parla";i['textToSpeech']="Text a veu";i['thisPageAllowsConnectingDgtBoard']="Aquesta pàgina et permet connectar el teu tauler DGT amb Lichess i utilitzar-lo per jugar partides.";i['timeControlsForCasualGames']="Controls de temps per a partides casuals: només Clàssica, Correspondència o Ràpida.";i['timeControlsForRatedGames']="Controls de temps per a partides per punts: Clàssica, Correspondència i algunes Ràpides, 15+10 i 20+0 incloses.";i['toConnectTheDgtBoard']=s("Per a connectar el tauler electrònic DGT has d\\'instal·lar %s.");i['toSeeConsoleMessage']="Per a veure missatges de consola pressiona Command + Option + C (Mac) o Control + Shift + C (Windows, Linux, Chrome OS)";i['useWebSocketUrl']=s("Utilitza \\\"%1$s\\\" tret que %2$s s\\'estigui executant en un ordinador o un port diferent.");i['validDgtOauthToken']="Teniu un token OAuth adequat per jugar amb DGT.";i['verboseLogging']="Registre detallat";i['webSocketUrl']=s("URL de WebSocket %s");i['whenReadySetupBoard']=s("Quan estiguis preparat, configura el tauler i clica en %s.")})()