import QRCode from 'qrcode';

window.lishogi.ready.then(() => {
  const issuer = window.location.host; // lishogi.org
  const user = $(document.body).data('user');
  const secret = $('input[name=secret]').val();

  QRCode.toCanvas(
    document.getElementById('qrcode'),
    'otpauth://totp/' + issuer + ':' + user + '?secret=' + secret + '&issuer=' + issuer,
    {
      width: 320,
    },
  );
});
