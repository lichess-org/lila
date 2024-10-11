"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['announceAllMoves']="Oznámit všechny tahy";i['announceMoveFormat']="Formát ohlašování tahů";i['asALastResort']=s("Jako poslední možnost, nastavte šachovnici do stejné pozice jako na Lichess, pak %s");i['boardWillAutoConnect']="Šachovnice se automaticky připojí k jakékoli hře, která již probíhá, nebo k jakékoli nové hře, která začne. Možnost vybrat si, kterou hru chcete hrát, bude brzy k dispozici.";i['checkYouHaveMadeOpponentsMove']="Nejprve zkontrolujte, zda jste na šachovnici DGT provedli tah svého soupeře. Vraťte svůj tah. Hrajte znovu.";i['clickToGenerateOne']="Kliknutím vygenerujete";i['configurationSection']="sekci konfigurace";i['configure']="Konfigurace";i['configureVoiceNarration']="Konfigurace hlasového komentátora tahů, takže si můžete nechat oči na šachovnici.";i['debug']="Ladění";i['dgtBoard']="DGT šachovnice";i['dgtBoardConnectivity']="Připojení DGT šachovnice";i['dgtBoardLimitations']="Omezení DGT šachovnice";i['dgtBoardRequirements']="Požadavky na připojení DGT šachovnice";i['dgtConfigure']="DGT - Konfigurace";i['dgtPlayMenuEntryAdded']=s("Položka %s byla přidána do tvého HREJ menu nahoře.");i['downloadHere']=s("Software si můžete stáhnout zde: %s.");i['enableSpeechSynthesis']="Povolit syntézu řeči";i['ifLiveChessRunningElsewhere']=s("Pokud %1$s běží na jiném stroji nebo jiném portu, musíte nastavit IP adresu a port zde v %2$s.");i['ifLiveChessRunningOnThisComputer']=s("Pokud %1$s běží na tomto počítači, můžete zkontrolovat připojení %2$s.");i['ifMoveNotDetected']="Pokud není tah rozpoznán";i['keepPlayPageOpen']="Stránka přehrávání musí zůstat otevřená ve vašem prohlížeči. Nemusí být viditelná (můžete jej minimalizovat nebo posunout vedle stránky se samotnou hrou), ale stránku nezavírejte nebo deska přestane fungovat.";i['keywordFormatDescription']="Klíčová slova jsou ve formátu JSON. Používají se k překladu tahů a výsledků do vašeho jazyka. Výchozí je angličtina, ale neváhejte si ji změnit.";i['keywords']="Klíčová slova";i['lichessAndDgt']="Lichess a DGT";i['lichessConnectivity']="Připojení k Lichess";i['moveFormatDescription']="SAN je standardem na Lichess \\\"Nf6\\\". UCI je běžný na enginech \\\"g8f6\\\".";i['noSuitableOauthToken']="Nebyl vytvořen žádný vhodný OAuth token.";i['openingThisLink']="otevírám tento odkaz";i['playWithDgtBoard']="Hrát s DGT šachovnicí";i['reloadThisPage']="Obnovit tuto stránku";i['selectAnnouncePreference']="Vyberte ANO pro oznámení jak tahů, tak tahů soupeře. Vyberte NE, aby se oznamovaly pouze tahy soupeře.";i['speechSynthesisVoice']="Hlas pro syntézu řeči";i['textToSpeech']="Převod textu na řeč";i['thisPageAllowsConnectingDgtBoard']="Tato stránka umožňuje připojení DGT desky k Lichess a její použití pro hraní her.";i['timeControlsForCasualGames']="Kontrola času u příležitostných her: Classical, Correspondence a Rapid pouze.";i['timeControlsForRatedGames']="Kontrola času u hodnocených her: Classical, Correspondence a některé Rapidy včetně 15+10 a20+0";i['toConnectTheDgtBoard']=s("Pro připojení k DGT elektronické šachovnice si musíte nainstalovat %s.");i['toSeeConsoleMessage']="Abyste viděli konzoli zmáčkněte Command + Option + C (Mac) or Control + Shift + C (Windows, Linux, Chrome OS)";i['useWebSocketUrl']=s("Použij \\\"%1$s\\\", pokud %2$s není spuštěn na jiném počítači nebo jiném portu.");i['validDgtOauthToken']="Máte vhodný OAuth token pro hraní na DGT šachovnici.";i['verboseLogging']="Podrobné ladění";i['webSocketUrl']=s("%s WebSocket URL");i['whenReadySetupBoard']=s("Když jste připraven, nastavte si vaši šachovnici a poté klikněte %s.")})()