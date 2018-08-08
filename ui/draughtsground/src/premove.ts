import * as cg from './types'

const movesDown: number[][] = [
    [-1, -1, -1],
    [6, 7, 11],
    [7, 8, 12],
    [8, 9, 13],
    [9, 10, 14],
    [10, -1, 15],
    [-1, 11, 16],
    [11, 12, 17],
    [12, 13, 18],
    [13, 14, 19],
    [14, 15, 20],
    [16, 17, 21],
    [17, 18, 22],
    [18, 19, 23],
    [19, 20, 24],
    [20, -1, 25],
    [-1, 21, 26],
    [21, 22, 27],
    [22, 23, 28],
    [23, 24, 29],
    [24, 25, 30],
    [26, 27, 31],
    [27, 28, 32],
    [28, 29, 33],
    [29, 30, 34],
    [30, -1, 35],
    [-1, 31, 36],
    [31, 32, 37],
    [32, 33, 38],
    [33, 34, 39],
    [34, 35, 40],
    [36, 37, 41],
    [37, 38, 42],
    [38, 39, 43],
    [39, 40, 44],
    [40, -1, 45],
    [-1, 41, 46],
    [41, 42, 47],
    [42, 43, 48],
    [43, 44, 49],
    [44, 45, 50],
    [46, 47, -1],
    [47, 48, -1],
    [48, 49, -1],
    [49, 50, -1],
    [50, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1]
];

const movesUp: number[][] = [
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, -1, -1],
    [-1, 1, -1],
    [1, 2, -1],
    [2, 3, -1],
    [3, 4, -1],
    [4, 5, -1],
    [6, 7, 1],
    [7, 8, 2],
    [8, 9, 3],
    [9, 10, 4],
    [10, -1, 5],
    [-1, 11, 6],
    [11, 12, 7],
    [12, 13, 8],
    [13, 14, 9],
    [14, 15, 10],
    [16, 17, 11],
    [17, 18, 12],
    [18, 19, 13],
    [19, 20, 14],
    [20, -1, 15],
    [-1, 21, 16],
    [21, 22, 17],
    [22, 23, 18],
    [23, 24, 19],
    [24, 25, 20],
    [26, 27, 21],
    [27, 28, 22],
    [28, 29, 23],
    [29, 30, 24],
    [30, -1, 25],
    [-1, 31, 26],
    [31, 32, 27],
    [32, 33, 28],
    [33, 34, 29],
    [34, 35, 30],
    [36, 37, 31],
    [37, 38, 32],
    [38, 39, 33],
    [39, 40, 34],
    [40, -1, 35],
    [-1, 41, 36],
    [41, 42, 37],
    [42, 43, 38],
    [43, 44, 39],
    [44, 45, 40]
];

const movesHorizontal: number[][] = [
    [-1, -1],
    [-1, 2],
    [1, 3],
    [2, 4],
    [3, 5],
    [4, -1],
    [-1, 7],
    [6, 8],
    [7, 9],
    [8, 10],
    [9, -1],
    [-1, 12],
    [11, 13],
    [12, 14],
    [13, 15],
    [14, -1],
    [-1, 17],
    [16, 18],
    [17, 19],
    [18, 20],
    [19, -1],
    [-1, 22],
    [21, 23],
    [22, 24],
    [23, 25],
    [24, -1],
    [-1, 27],
    [26, 28],
    [27, 29],
    [28, 30],
    [29, -1],
    [-1, 32],
    [31, 33],
    [32, 34],
    [33, 35],
    [34, -1],
    [-1, 37],
    [36, 38],
    [37, 39],
    [38, 40],
    [39, -1],
    [-1, 42],
    [41, 43],
    [42, 44],
    [43, 45],
    [44, -1],
    [-1, 47],
    [46, 48],
    [47, 49],
    [48, 50],
    [49, -1]
]

function number2key(n: number): cg.Key {
    if (n < 10)
        return ("0" + n.toString()) as cg.Key;
    else
        return n.toString() as cg.Key;
}

export default function premove(pieces: cg.Pieces, key: cg.Key, variant?: string): cg.Key[] {

    const piece = pieces[key],
        field: number = Number(key);

    if (piece === undefined || isNaN(field)) return new Array<cg.Key>();

    const frisian = (variant && variant === "frisian");

    const dests: cg.Key[] = new Array<cg.Key>();
    switch (piece.role) {

        case 'man':

            //
            //It is always impossible to premove a capture if the first field in that direction contains a piece of our own color:
            //enemy pieces can never land there because you only take pieces from the board after capture sequence is completed
            //

            for (let i = 0; i < (frisian ? 3 : 2); i++) {
                let f = movesUp[field][i];
                if (f != -1) {

                    if (piece.color === 'white' && i < 2)
                        dests.push(number2key(f));

                    const pc = pieces[number2key(f)];
                    if (pc === undefined || pc.color !== piece.color) {
                        f = movesUp[f][i];
                        if (f !== -1)
                            dests.push(number2key(f));
                    }

                }
            }

            for (let i = 0; i < (frisian ? 3 : 2); i++) {
                let f = movesDown[field][i];
                if (f != -1) {

                    if (piece.color === 'black' && i < 2)
                        dests.push(number2key(f));

                    const pc = pieces[number2key(f)];
                    if (pc === undefined || pc.color !== piece.color) {
                        f = movesDown[f][i];
                        if (f !== -1)
                            dests.push(number2key(f));
                    }

                }
            }

            if (frisian) {
                for (let i = 0; i < 2; i++) {
                    let f = movesHorizontal[field][i];
                    if (f != -1) {

                        const pc = pieces[number2key(f)];
                        if (pc === undefined || pc.color !== piece.color) {
                            f = movesHorizontal[f][i];
                            if (f !== -1)
                                dests.push(number2key(f));
                        }

                    }
                }
            }

            break;

        case 'king':

            //
            //As far as I can tell there is no configuration of pieces that makes any square theoretically impossible to be premovable 
            //

            for (let i = 0; i < (frisian ? 3 : 2); i++) {
                let f = movesUp[field][i], k = 0;
                while (f != -1) {
                    if (i < 2 || k > 0)
                        dests.push(number2key(f));
                    f = movesUp[f][i];
                    k++;
                }
            }

            for (let i = 0; i < (frisian ? 3 : 2); i++) {
                let f = movesDown[field][i], k=0;
                while (f != -1) {
                    if (i < 2 || k > 0)
                        dests.push(number2key(f));
                    f = movesDown[f][i];
                    k++;
                }
            }

            if (frisian) {
                for (let i = 0; i < 2; i++) {
                    let f = movesHorizontal[field][i], k = 0;
                    while (f != -1) {
                        if (k > 0)
                            dests.push(number2key(f));
                        f = movesHorizontal[f][i];
                        k++;
                    }
                }
            }

            break;

    }

    return dests;

};
