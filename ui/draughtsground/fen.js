"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.initial = 'W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20';
function read(fen) {
    if (fen === 'start')
        fen = exports.initial;
    var pieces = {};
    var fenParts = fen.split(':');
    for (var i = 0; i < fenParts.length; i++) {
        var clr = "";
        if (fenParts[i].slice(0, 1) === "W")
            clr = "white";
        else if (fenParts[i].slice(0, 1) === "B")
            clr = "black";
        if (clr.length !== 0 && fenParts[i].length > 1) {
            var fenPieces = fenParts[i].slice(1).split(',');
            for (var k = 0; k < fenPieces.length; k++) {
                var fieldNumber = void 0, role = void 0;
                if (fenPieces[k].slice(0, 1) === "K") {
                    role = "king";
                    fieldNumber = fenPieces[k].slice(1);
                }
                else if (fenPieces[k].slice(0, 1) === "G") {
                    role = "ghostman";
                    fieldNumber = fenPieces[k].slice(1);
                }
                else if (fenPieces[k].slice(0, 1) === "P") {
                    role = "ghostking";
                    fieldNumber = fenPieces[k].slice(1);
                }
                else {
                    role = "man";
                    fieldNumber = fenPieces[k];
                }
                if (fieldNumber.length == 1)
                    fieldNumber = "0" + fieldNumber;
                pieces[fieldNumber] = {
                    role: role,
                    color: clr
                };
            }
        }
    }
    return pieces;
}
exports.read = read;
function write(pieces) {
    var fenW = "W";
    var fenB = "B";
    for (var f = 1; f <= 50; f++) {
        var piece = pieces[(f < 10 ? "0" + f.toString() : f.toString())];
        if (piece) {
            if (piece.color === "white") {
                if (fenW.length > 1)
                    fenW += ',';
                if (piece.role === "king")
                    fenW += "K";
                else if (piece.role === "ghostman")
                    fenW += "G";
                else if (piece.role === "ghostking")
                    fenW += "P";
                fenW += f.toString();
            }
            else {
                if (fenB.length > 1)
                    fenB += ',';
                if (piece.role === "king")
                    fenB += "K";
                else if (piece.role === "ghostman")
                    fenB += "G";
                else if (piece.role === "ghostking")
                    fenB += "P";
                fenB += f.toString();
            }
        }
    }
    return fenW + ":" + fenB;
}
exports.write = write;
function countGhosts(fen) {
    if (fen === 'start')
        fen = exports.initial;
    var ghosts = 0;
    var fenParts = fen.split(':');
    for (var i = 0; i < fenParts.length; i++) {
        var clr = "";
        if (fenParts[i].slice(0, 1) === "W")
            clr = "white";
        else if (fenParts[i].slice(0, 1) === "B")
            clr = "black";
        if (clr.length !== 0 && fenParts[i].length > 1) {
            var fenPieces = fenParts[i].slice(1).split(',');
            for (var k = 0; k < fenPieces.length; k++) {
                if (fenPieces[k].slice(0, 1) === "G" || fenPieces[k].slice(0, 1) === "P")
                    ghosts++;
            }
        }
    }
    return ghosts;
}
exports.countGhosts = countGhosts;
