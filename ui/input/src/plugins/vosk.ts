import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { VoskOpts } from '../interfaces';
import { VoiceCtrlImpl } from '../voiceCtrl';

//const wasmSource = 'vendor/vosk/vosk.js';
const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

export default (window as any).LichessVoice = async (opts: VoskOpts) => {

  const ctrl = opts.ctrl as VoiceCtrlImpl;
  let voiceModel: Model;
  let recognizer: KaldiRecognizer;

  try {
    //ctrl.voskStatus = 'fetching wasm';
    //await lichess.loadScript(wasmSource);
    ctrl.voskStatus = 'fetching model';
    const voiceModel = await createModel(lichess.assetUrl(modelSource));

    const recognizer = new voiceModel.KaldiRecognizer(sampleRate, JSON.stringify([...opts.speechLookup.keys()]));
    recognizer.on('result', (message: any) => {
      if ('result' in message && 'text' in message.result) {
        ctrl.listen(message.result.text as string);
      }
    });
    await (opts.impl == 'vanilla' ? vanillaProcessor : workletProcessor)();
    console.log(`Voice input using ${opts.impl} engine with ${modelSource}`);
    ctrl.voskReady = true;
  } catch (e) {
    console.log('Voice module init failed', e);
    ctrl.voskStatus = `${JSON.stringify(e).slice(0, 40)}...`;
  }

  //========================== works ok on all but deprecated ==============================

  async function vanillaProcessor() {
    // createScriptProcessor was deprecated in 2014
    const recognizerNode = ctrl.audioContext.createScriptProcessor(4096, 1, 1);
    recognizerNode.onaudioprocess = (e: any) => recognizer.acceptWaveform(e.inputBuffer);
    ctrl.audioContext.createMediaStreamSource(ctrl.mediaStream).connect(recognizerNode);
    recognizerNode.connect(ctrl.audioContext.destination); // (shouldn't need this but it do)
  }

  //========================= preferred impl but safari bugged =============================

  async function workletProcessor() {
    const debugChannel = new MessageChannel(); // safari can't log in a worker
    debugChannel.port1.onmessage = (e: MessageEvent) => console.log(e.data);

    const channel = new MessageChannel();
    voiceModel.registerPort(channel.port1);

    await ctrl.audioContext.audioWorklet.addModule(lichess.assetUrl('compiled/voskWorklet.js'));

    const voskNode = new AudioWorkletNode(ctrl.audioContext, 'vosk-worklet', {
      channelCount: 1,
      numberOfInputs: 1,
      numberOfOutputs: 1,
    });
    voskNode.port.postMessage({ action: 'register', recognizerId: recognizer.id }, [channel.port2, debugChannel.port2]);
    voskNode.connect(ctrl.audioContext.destination);

    const sourceNode = ctrl.audioContext.createMediaStreamSource(ctrl.mediaStream);
    sourceNode.connect(voskNode);
  }
};

