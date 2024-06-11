import QRCode from 'qrcode';

const qrs = document.querySelectorAll('canvas.qrcode') as NodeListOf<HTMLCanvasElement>;

for (const qr of qrs) {
  if (!qr.dataset.qrUrl) continue;

  QRCode.toCanvas(qr, qr.dataset.qrUrl, {
    margin: 2,
    width: qr.dataset.width ? parseInt(qr.dataset.width) : 320,
  });
}
