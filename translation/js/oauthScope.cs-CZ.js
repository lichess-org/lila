"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.oauthScope)window.i18n.oauthScope={};let i=window.i18n.oauthScope;i['alreadyHavePlayedGames']="Již jsi hrál hry!";i['apiAccessTokens']="Přístupové API tokeny";i['apiDocumentation']="Dokumentace API";i['apiDocumentationLinks']=s("Zde je %1$s a %2$s.");i['attentionOfDevelopers']="Upozornění pouze pro vývojáře:";i['authorizationCodeFlow']="tok kódu autorizace";i['boardPlay']="Hrát hry přes deskové API";i['botPlay']="Hrát hry s botem přes API";i['canMakeOauthRequests']=s("Můžete vytvořit OAuth žádosti bez procházení %s.");i['carefullySelect']="Pečlivě vyberte, co je povoleno dělat Vaším jménem.";i['challengeBulk']="Vytvořte mnoho her najednou pro ostatní hráče";i['challengeRead']="Číst příchozí výzvy";i['challengeWrite']="Odeslat, potvrdit a odmítnout výzvy";i['copyTokenNow']="Ujistěte se, že máte svůj nový osobní přístupový token uložený. Není možné ho znovu zobrazit!";i['created']=s("Vytvořeno %s");i['doNotShareIt']="Token umožní přístup k vašemu účtu. NESDÍLEJTE jej s nikým!";i['emailRead']="Číst e-mailovou adresu";i['engineRead']="Zobrazit a používat externí engine";i['engineWrite']="Vytvořit a aktualizovat externí engine";i['followRead']="Číst sledované hráče";i['followWrite']="Sledovat a přestat sledovat jiné hráče";i['forExample']=s("%s");i['generatePersonalToken']="generovat osobní přístupový token";i['givingPrefilledUrls']="Poskytnutí těchto předvyplněných URL adres uživatelům jim pomůže získat správné rozsahy tokenů.";i['guardTokensCarefully']="Pečlivě tyto žetony střežte! Jsou jako hesla. Výhodou používání tokenů oproti zadávání hesla do skriptu je, že tokeny lze odvolat a lze jich hodně vygenerovat.";i['insteadGenerateToken']=s("Místo, %s můžete přímo použít API žádosti.");i['lastUsed']=s("Naposledy použito %s");i['msgWrite']="Poslat soukromé zprávy jiným hráčům";i['newAccessToken']="Nový osobní token pro přístup k API";i['newToken']="Nový přístupový token";i['personalAccessTokens']="Osobní přístupový token API";i['personalTokenAppExample']="příklad aplikace osobního tokenu";i['possibleToPrefill']="Tento formulář lze předvyplnit úpravou parametrů dotazu URL.";i['preferenceRead']="Číst předvolby";i['preferenceWrite']="Zapisovat předvolby";i['puzzleRead']="Číst puzzle aktivity";i['racerWrite']="Vytvářejte a připojte se k hádankám";i['rememberTokenUse']="Pamatuješ si tedy, k čemu je tento token";i['scopesCanBeFound']="Kódy rozsahů naleznete v kódu HTML formuláře.";i['studyRead']="Číst soukromé studie a vysílání";i['studyWrite']="Vytvořit, upravit a smazat studie a vysílání";i['teamLead']="Spravovat týmy, které vedete: posílat soukromé zprávy, vykopávat členy";i['teamRead']="Číst soukromé informace o týmu";i['teamWrite']="Připojit se a opustit týmy";i['ticksTheScopes']=s("zaškrtne rozsahy %1$s a %2$s a nastaví popis tokenu.");i['tokenDescription']="Popis tokenu";i['tokenGrantsPermission']="Token dává jiným lidem oprávnění používat Váš účet.";i['tournamentWrite']="Vytvořit, upravit a připojit k turnajům";i['webLogin']="Vytváření ověřených webových sezení (poskytuje plný přístup!)";i['webMod']="Používat nástroje moderátora (v mezích Vašeho oprávnění)";i['whatTheTokenCanDo']="Co token může udělat vaším jménem:"})()