let baseUrl: string;

function make(file) {
  baseUrl = baseUrl || $('body').data('asset-url') + '/assets/sound/';
  const sound = new window.Howl({
    src: [
      baseUrl + file + '.ogg',
      baseUrl + file + '.mp3'
    ]
  });
  return function() {
    if (window.lichess.sound.set() !== 'silent') sound.play();
  };
};

export default function() {
  return {
    success: make('other/energy3'),
    failure: make('other/failure2')
  };
}
