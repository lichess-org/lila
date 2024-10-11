"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['announceAllMoves']="모든 수 알리기";i['announceMoveFormat']="수 알림 표기법";i['asALastResort']=s("마지막 수단으로 보드를 Liches와 동일하게 설정한 다음 %s");i['boardWillAutoConnect']="이 보드는 이미 진행 중인 게임이나 시작하는 새로운 게임에 자동으로 연결됩니다. 어떤 게임을 할지 선택할 수 있는 기능이 곧 나올 예정입니다.";i['checkYouHaveMadeOpponentsMove']="DGT 보드에서 먼저 상대방이 말을 움직였는지 확인하고, 다시 시도해주세요.";i['clickToGenerateOne']="눌러서 생성하기";i['configurationSection']="구성 섹션";i['configure']="설정";i['configureVoiceNarration']="각 플레이어의 수에 음성 내레이션을 구성하여 보드에 더 집중 할 수 있게 만듭니다";i['debug']="디버그";i['dgtBoard']="DGT 보드";i['dgtBoardConnectivity']="DGT 보드 연결";i['dgtBoardLimitations']="DGT 보드 제약사항";i['dgtBoardRequirements']="DGT 보드 요구사항";i['dgtConfigure']="DGT - 설정";i['dgtPlayMenuEntryAdded']=s("%s 입장이 당신의 플레이 메뉴 상단에 추가되었습니다.");i['downloadHere']=s("소프트웨어 다운로드가 가능합니다: %s.");i['enableSpeechSynthesis']="합성 음성 활성화";i['ifLiveChessRunningElsewhere']=s("만약 %1$s가 다른 장치나 포트에서 작동하고 있다면, %2$s에서 IP 주소를 설정하고 이곳으로 이식해야 합니다.");i['ifLiveChessRunningOnThisComputer']=s("만약 %1$s가 이 컴퓨터에서 작동하고 있다면, %2$s하여 연결을 확인할 수 있습니다.");i['ifMoveNotDetected']="움직임이 감지되지 않을 경우";i['keepPlayPageOpen']="플레이 페이지는 브라우저에서 열려 있어야 합니다. 볼 필요가 없고 최소화하거나 리치 게임 페이지와 나란히 설정할 수 있지만 닫지만 않으면 보드가 작동을 중단하지 않습니다.";i['keywordFormatDescription']="키워드는 JSON 형식입니다. 이동 및 결과를 언어로 변환하는 데 사용됩니다. 기본값은 영어이지만, 자유롭게 변경하셔도 좋습니다.";i['keywords']="키워드";i['lichessAndDgt']="Lichess & DGT";i['lichessConnectivity']="Lichess 연결";i['moveFormatDescription']="SAN은 \\\"Nf6\\\"과 같은, Lichess에서 쓰이는 표준 표기법입니다. UCI는 \\\"g8f6\\\"과 같은, 체스 엔진에서 흔이 쓰이는 표기법입니다.";i['noSuitableOauthToken']="알맞은 OAuth 토큰이 생성되지 않았습니다";i['openingThisLink']="이 링크를 클릭";i['playWithDgtBoard']="DGT 보드로 게임하기";i['reloadThisPage']="이 페이지를 다시 로드하세요";i['selectAnnouncePreference']="YES를 선택하면 자신의 수와 상대의 수를 모두 알 수 있고, NO를 선택하면 상대의 수만 알 수 있습니다.";i['speechSynthesisVoice']="합성 음성 출력";i['textToSpeech']="텍스트 음성 변환";i['thisPageAllowsConnectingDgtBoard']="이 페이지에서는 DGT 보드를 Lichess에 연결하여 사용 할 수 있습니다.";i['timeControlsForCasualGames']="캐주얼 게임에서의 시간 제어: 클래시컬, 긴 대국, 래피드 전용.";i['timeControlsForRatedGames']="캐주얼 게임에서의 시간 제어: 클래시컬, 긴 대국, 15+10과 20+0를 포함한 래피드.";i['toConnectTheDgtBoard']=s("DGT 보드에 연결하려면 %s를 설치해야 합니다.");i['toSeeConsoleMessage']="콘솔 메시지를 보려면 Command + Option + C(Mac) 또는 Control + Shift + C(Windows, Linux, Chrome OS) 를 누르세요.";i['useWebSocketUrl']=s("%2$s가 다른 장치나 포트에서 작동하는 것이 아니라면 \\\"%1$s\\\"를 사용하십시오.");i['validDgtOauthToken']="DGT 플레이에 적합한 OAuth 토큰이 있습니다.";i['verboseLogging']="상세 로깅";i['webSocketUrl']=s("%s WebSocket URL");i['whenReadySetupBoard']=s("준비가 되셨으면 보드를 설치한 다음 %s를 클릭하세요.")})()