"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="Барлық стримерлер";i['approved']="Сіздің стриміңіз расталды.";i['becomeStreamer']="Личес стримері болу";i['changePicture']="Суретті өзгерту/жою";i['currentlyStreaming']=s("Қазіргі стрим: %s");i['downloadKit']="Стример құралдарын жүктеп алу";i['doYouHaveStream']="Сізде YouTube не Twitch арнасы бар ма?";i['editPage']="Стример парақшасын өңдеу";i['headline']="Басты тақырып";i['hereWeGo']="Кеттік!";i['keepItShort']=p({"one":"Қысқа болсын: ең көбі %s таңба","other":"Қысқа болсын: ең көбі %s таңба"});i['lastStream']=s("Кейінгі стрим %s");i['lichessStreamer']="Личес стримері";i['lichessStreamers']="Личес стримерлері";i['live']="Тікелей эфир!";i['longDescription']="Ұзын сипаттама";i['maxSize']=s("Өлшемнің шегі: %s");i['offline']="Эфирден тыс";i['optionalOrEmpty']="Міндетті емес. Жоқ болса, бос қалдырыңыз";i['pendingReview']="Сіздің стриміңіз модераторлардың тексеруінде.";i['perk1']="Личес куәлігіңізде жалынды стример таңбашасы шығады.";i['perk2']="Стримерлер тізімінің төбесіне шығасыз.";i['perk3']="Личес серіктеріңізді ескертесіз.";i['perk4']="Стримді өз ойындарыңызда, жарыс пен зерттеуде көрсетесіз.";i['perks']="Стримді кілтсөзбен жүргізудің артықшылығы";i['pleaseFillIn']="Стример мәліметтерін толтырып, суретіңізді жүктеп салыңыз.";i['requestReview']="модератор тексеруін сұраңыз";i['rule1']="Личестің стримін жүргізгенде, \\\"lichess.org\\\" кілтсөзін стрим тақырыбына қосып, \\\"Chess\\\" санатын таңдаңыз.";i['rule2']="Личестен басқа нәрсенің стримін жүргізсеңіз, кілтсөзді алып тастаңыз.";i['rule3']="Личес стриміңізді автоматты түрде анықтап, келесі жеңілдіктерге жол ашады:";i['rule4']=s("Стрим кезінде бәрі бірдей әділ ойнау үшін біздің %s оқыңыз.");i['rules']="Стрим жүргізудің ережелері";i['streamerLanguageSettings']="Личес стример парақшаңыз белгілі бір тілді көрерменге арналған. Бұл тіл стрим жасайтын платформаның тіліне негізделген. Шахмат стримді бастар бұрын лайықты тілді көрсетілім жасайтын қызметте я қолданбада орнатып алыңыз.";i['streamerName']="Личестегі стример атыңыз";i['streamingFairplayFAQ']="Әділ стрим ойын туралы Жұрттан-сұрақ";i['tellUsAboutTheStream']="Стрим туралы бір сөйлеммен айтып беріңіз";i['twitchUsername']="Twitch тіркеулі атыңыз немесе URL";i['uploadPicture']="Суретті жүктеп салу";i['visibility']="Стримерлер парақшасында көріну";i['whenApproved']="Модераторлар растаса";i['whenReady']=s("Личес стримерлер қатарына қосылу үшін %s");i['xIsStreaming']=s("%s стрим жүргізіп отыр");i['xStreamerPicture']=s("%s стримердің суреті");i['yourPage']="Сіздің стример парақшаңыз";i['youTubeChannelId']="Сіздің YouTube арнаңыздың ID"})()