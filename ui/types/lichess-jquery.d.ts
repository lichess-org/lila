interface JQueryStatic {
  modal: LichessModal;
}

interface LichessModal {
  (html: string | JQuery): JQuery;
  close(): void;
}

interface JQuery {
  powerTip(options?: PowerTip.Options): JQuery
}

declare namespace PowerTip {
    type Placement = 'n' | 'e' | 's' | 'w' | 'nw' | 'ne' | 'sw' | 'se' |
                     'nw-alt' | 'ne-alt' | 'sw-alt' | 'se-alt';

    interface Options {
        followMouse?: boolean;
        mouseOnToPopup?: boolean;
        placement?: Placement;
        smartPlacement?: boolean;
        popupId?: string;
        poupClass?: string;
        offset?: number;
        fadeInTime?: number;
        fadeOutTime?: number;
        closeDelay?: number;
        intentPollInterval?: number;
        intentSensitivity?: number;
        manual?: boolean;
        openEvents?: string[];
        closeEvents?: string[];
    }
}
