interface JQueryStatic {
  modal: LichessModal;
}

interface LichessModal {
  (html: string | JQuery): JQuery;
  close(): void;
}
