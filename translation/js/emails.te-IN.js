"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("మమ్మల్ని సంప్రదించడానికి దయచేసి %s వాడండి.");i['common_note']=s("ఇది చిన్న పరామర్శండీ, మీరు %s ఈమెయిల్ ఎలా వాడుతున్నారో తెలపడానికి.");i['common_orPaste']="(ఏంటి? నొక్కితే పనిచేయటం లేదా? అయితే, బ్రౌసర్ లో పూసి - అంటే అదేనండీ paste చేసి - చూడండి!)";i['emailChange_click']="మీరు ఈ ఈమెయిల్ ఖాతాను చదవగలరని ధ్రువపరించేందుకుగాను, దయయుంచి కింది లంకె పైన నొక్కండి:";i['emailChange_intro']="మీరు మీ ఈమెయిల్ చిరునామాను మార్చమని అభ్యర్థించారు.";i['emailChange_subject']=s("కొత్త ఈమెయిల్ చిరునామా ను ధ్రువపరచండి, %s");i['emailConfirm_click']="మీ Lichess ఖాతాను వాడడం మొదలుపెట్టడానికి ఈ లంకెను నొక్కండి:";i['emailConfirm_ignore']="మీరు Lichessతో నమోదు చేసుకోకుంటే మీరు ఈ సందేశాన్ని సురక్షితంగా విస్మరించవచ్చు.";i['emailConfirm_subject']=s("మీ lichess.org ఖాతా %s, ధ్రువపరచండి");i['logInToLichess']=s("Lichess.org కి లాగిన్ అవ్వండి, %s");i['passwordReset_clickOrIgnore']="మీరు ఈ అభ్యర్థన చేసియున్నట్లయితే, కింది లంకె ను నొక్కండి. లేదంటే, ఈ ఈమెయిల్ ను పట్టించుకోనవసరం లేదు.";i['passwordReset_intro']="మీ ఖాతాకు ప్రస్తుతమున్న రహస్యపదాన్ని తుడిచిపెట్టి, కొత్త రహస్యపదాన్ని సృష్టించమని అభ్యర్థన వచ్చింది.";i['passwordReset_subject']=s("మీ lichess రహస్యపదాన్ని మార్చండి, %s");i['welcome_subject']=s("%s, lichess.org కి స్వాగతము");i['welcome_text']=s("ఇక దీనితో https://lichess.org లో మీరు ఖాతాను తెరవటం విజయవంతంగా పూర్తయ్యింది. \n\nమీ పూర్తి సమాచారం %1$s లో ఉంది. దానిని %2$s లో చూసి మీకు కావలసిన విధంగా మార్పులు చేసుకోండి.\n\nవిజయీభవ, మీ రాచసైన్యం ప్రత్యర్థి రాజును బందీగా పట్టుగొను గాక!")})()