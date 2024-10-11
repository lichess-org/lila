"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.streamer)window.i18n.streamer={};let i=window.i18n.streamer;i['allStreamers']="所有主播";i['approved']="你的直播已被批准。";i['becomeStreamer']="成为 Lichess 主播";i['changePicture']="更改/删除你的图片";i['currentlyStreaming']=s("正在直播：%s");i['downloadKit']="下载主播工具";i['doYouHaveStream']="你有 Twitch 或 YouTube 频道吗？";i['editPage']="编辑主播页面";i['headline']="标题";i['hereWeGo']="我们开始吧！";i['keepItShort']=p({"other":"不超过 %s 个字符"});i['lastStream']=s("上次直播：%s");i['lichessStreamer']="Lichess 主播";i['lichessStreamers']="Lichess 主播";i['live']="在线";i['longDescription']="详细描述";i['maxSize']=s("大小上限：%s");i['offline']="离线";i['optionalOrEmpty']="可选。如果没有则留空";i['pendingReview']="你的直播正在审核。";i['perk1']="在你的 Lichess 个人资料上获得一个主播图标。";i['perk2']="快速到达主播列表顶部。";i['perk3']="通知你的 Lichess 粉丝。";i['perk4']="在你的对局、比赛与研究中显示你的直播。";i['perks']="使用关键词直播的好处";i['pleaseFillIn']="请填写你的主播信息并上传一张图片。";i['requestReview']="请求版主复审";i['rule1']="当你在 Lichess 直播时，请在你的直播标题中包含关键词 \\\"lichess.org\\\"。";i['rule2']="当你直播与 Lichess 不相关的内容时，请移除关键词。";i['rule3']="Lichess 将自动检测你的直播并启用以下津贴：";i['rule4']=s("阅读我们的 %s 以确保你在直播中公平地进行游戏。");i['rules']="直播规则";i['streamerLanguageSettings']="Lichess 主播页面以你的直播平台使用的语言推送给观众。请在你直播的平台设置正确的默认语言。";i['streamerName']="你在 Lichess 的主播名字";i['streamingFairplayFAQ']="直播公平游戏常见问题";i['tellUsAboutTheStream']="用一句话中描述你的直播";i['twitchUsername']="你的 Twitch 用户名或 URL";i['uploadPicture']="上传图片";i['visibility']="在主播页面可见";i['whenApproved']="当被版主批准时";i['whenReady']=s("当你准备好成为 Lichess 主播时，%s");i['xIsStreaming']=s("%s 正在直播");i['xStreamerPicture']=s("%s 主播图片");i['yourPage']="你的主播页面";i['youTubeChannelId']="你的 YouTube 频道 ID"})()