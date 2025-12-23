import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { dividePhases } from '../src/local/dividePhases';
import { pgnToNodes } from 'lib/tree/util';

// scalachess/.../DividerTest.scala
describe('dividePhases', () => {
  test('game1', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. e3 g6 2. d4 Bg7 3. Nf3 Nf6 4. Bd3 O-O 5. O-O b6 6. c4 Bb7 7. Nbd2 d5 8. b3 Nbd7 9. Bb2 Re8 10. Qc2 dxc4 11. bxc4 c5 12. d5 e5 13. e4 h5 14. a4 Nf8 15. h3 Qd6 16. Nxe5 Rxe5 17. Nf3 N6d7 18. Nxe5 Bxe5 19. Bxe5 Nxe5 20. Be2 Bc8 21. f4 Ned7 22. e5 Qe7 23. Bf3 Rb8 24. Rae1 f5 25. d6 Qh4 26. e6 Nxe6 27. Rxe6 Nf6 28. Ree1 Qxf4 29. Bd5+ Nxd5 30. Rxf4 Nxf4 31. Qd2 g5 32. d7 Bb7 33. d8=Q+ Rxd8 34. Qxd8+ Kh7 35. Qc7+ Kh6 36. Qxb7 g4 37. Qc6+ Ng6 38. Re6 gxh3 39. Rxg6+ Kh7 40. Rh6+ Kg7 41. Qf6+ Kg8 42. Rh8#',
        ),
      ),
      { middle: 18, end: 58 },
    );
  });

  test('game2', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. e4 c5 2. Nf3 d6 3. Bc4 Nf6 4. d3 g6 5. c3 Bg7 6. Bg5 O-O 7. h3 Nc6 8. Nbd2 a6 9. Bb3 b5 10. Bc2 Bb7 11. O-O Nd7 12. Nh2 f6 13. Be3 e5 14. Ndf3 Ne7 15. Qd2 f5 16. Qe2 h6 17. Bd2 g5 18. g4 f4 19. Bb3+ d5 20. exd5 Bxd5 21. c4 Bf7 22. cxb5 axb5 23. Bxf7+ Rxf7 24. Bc3 Ng6 25. a3 b4 26. axb4 Rxa1 27. Rxa1 cxb4 28. Bxb4 Qb6 29. Bc3 Re7 30. Ra8+ Kh7 31. Kg2 Nc5 32. Qc2 Qb3 33. Qxb3 Nxb3 34. Rb8 Nc5 35. Rb5 Nxd3 36. Nf1 e4 37. Ng1 Nh4+ 38. Kh2 Bxc3 39. bxc3 Nxf2 40. Rd5 e3 41. Ne2 Nf3+ 42. Kg2 Ne1+ 43. Kh2 f3 44. Neg3 e2 45. Nd2 Ng2 46. Nxf3 e1=Q 47. Nxe1 Nxe1 48. c4 Nc2 49. c5 Ne4 50. c6 Nxg3 51. Kxg3 Re3+ 52. Kf2 Rc3 53. Rd7+ Kg6 54. c7 Nb4 55. Rd6+ Kf7 56. Rxh6 Rxc7 57. Rh7+ Ke6 58. Rxc7 Nd3+ 59. Kf3 Nf4 60. Kg3 Ne2+ 61. Kf2 Nf4 62. Kg3 Ne2+ 63. Kh2 Nf4 64. Rc5 Kf6 65. Rf5+ Kg6 66. Kg3 Ne2+ 67. Kf3 Nd4+ 68. Ke4 Nxf5 69. gxf5+ Kf6 70. h4 gxh4 71. Kf4 h3 72. Kg3 Kxf5 73. Kxh3 1/2-1/2',
        ),
      ),
      { middle: 17, end: 76 },
    );
  });

  test('game3', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nc6 5. Nc3 e5 6. Nb3 Nf6 7. f3 Be7 8. Be3 O-O 9. Qd2 b6 10. O-O-O Bb7 11. g4 Rc8 12. h4 a5 13. h5 Nb4 14. g5 Nd7 15. g6 Nc5 16. h6 Nxb3+ 17. axb3 fxg6 18. hxg7 Rxf3 19. Bxb6 Qxb6 20. Qh6 Qe3+ 21. Qxe3 Rxe3 22. Bd3 Nxd3+ 23. cxd3 Bg5 24. Kb1 Kxg7 25. Rh2 Bf4 26. Rh4 h5 27. Rhh1 Rf8 28. Rhg1 g5 29. Rg2 g4 30. Nb5 Rf6 31. Nc7 Rg3 32. Rxg3 Bxg3 33. Ne8+ Kf7 34. Nxf6 Kxf6 35. Rf1+ Kg5 36. Rf7 Ba6 37. Ra7 Bxd3+ 38. Kc1 Bf4+ 39. Kd1 Bxe4 40. Rxa5 g3 41. Ke2 g2 42. Ra1 h4 43. Kf2 h3 44. b4 Kg4 45. b5 h2 46. b6 h1=Q 47. Rg1 Be3+ 48. Kxe3 Qxg1+ 49. Kxe4 Qxb6 50. Kd5 g1=Q 51. Ke6 Qgf2 52. Kd5 Qf3+ 53. Ke6 Qbb3+ 54. Ke7 Qff7+ 55. Kxd6 Qbd5# 0-1',
        ),
      ),
      { middle: 25, end: 62 },
    );
  });

  test('game4', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. f3 g6 2. e4 Bg7 3. d4 d5 4. Qe2 Bxd4 5. c3 Bg7 6. Nd2 Nc6 7. exd5 Qxd5 8. Ne4 Nf6 9. Nxf6+ Bxf6 10. Be3 O-O 11. Rd1 Qxa2 12. Qd2 Rd8 13. Qc2 Rxd1+ 14. Kxd1 Bf5 15. Qc1 Rd8+ 16. Ke1 Ne5 17. Kf2 Bd3 18. Ne2 Ba6 19. Nd4 Bxf1 20. Kxf1 Nd3 21. Qc2 Qa1+ 22. Ke2 Qxh1 23. Qxd3 c5 24. Qb5 Qxg2+ 25. Kd3 Qf1+ 26. Kc2 Qxb5 27. Nxb5 a6 28. Na3 Rc8 29. Nc4 b5 30. Nd2 h5 31. Ne4 Be5 32. f4 Bd6 33. b3 f5 34. Ng5 e5 35. fxe5 Bxe5 36. Bg1 Bf4 37. Ne6 Bd6 38. h3 c4 39. b4 Re8 40. Nd4 Be5 41. Nf3 Bg7 42. Nd2 Re2 43. Kd1 Rg2 44. Bc5 Bxc3 45. Nf3 Rg3 46. Ke2 Rxh3 47. Ng5 Rh2+ 48. Kd1 Bf6 49. Ne6 Rh1+ 50. Ke2 Kf7 51. Nc7 Rh2+ 52. Kf3 g5 53. Nxa6 g4+ 54. Kg3 Be5# 0-1',
        ),
      ),
      { middle: 25, end: 44 },
    );
  });

  test('game5', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. c4 e5 2. Nc3 f5 3. d3 Nf6 4. g3 Bb4 5. Bd2 O-O 6. Bg2 c6 7. Nf3 d5 8. cxd5 Bxc3 9. Bxc3 e4 10. dxe4 Nxe4 11. dxc6 Nxc6 12. Qb3+ Kh8 13. Rd1 Qc7 14. O-O Nxc3 15. Qxc3 Be6 16. a3 Rac8 17. Nd4 Nxd4 18. Qxd4 b6 19. e4 fxe4 20. Bxe4 Rcd8 21. Qe3 Qe5 22. b4 Bc4 23. Rfe1 Qh5 24. Rxd8 Rxd8 25. Bf3 Qf5 26. Rd1 Rf8 27. Bg2 h6 28. Qd4 Qf6 29. Qxc4 Qxf2+ 30. Kh1 Qb2 31. Qd3 h5 32. h4 Qf6 33. Rf1 Qe7 34. Rxf8+ Qxf8 35. Qf3 Qe7 36. Qxh5+ Kg8 37. Bd5+ 1-0',
        ),
      ),
      { middle: 22, end: 47 },
    );
  });

  test('game6', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. d4 Nf6 2. c4 g6 3. Nc3 d5 4. cxd5 Nxd5 5. Bd2 Bg7 6. e4 Nxc3 7. Bxc3 O-O 8. Qd2 Nc6 9. Nf3 Bg4 10. d5 Bxf3 11. Bxg7 Kxg7 12. gxf3 Ne5 13. O-O-O c6 14. Qc3 f6 15. Bh3 cxd5 16. exd5 Nf7 17. f4 Qd6 18. Qd4 Rad8 19. Be6 Qb6 20. Qd2 Rd6 21. Rhe1 Nd8 22. f5 Nxe6 23. Rxe6 Qc7+ 24. Kb1 Rc8 25. Rde1 Rxe6 26. Rxe6 Rd8 27. Qe3 Rd7 28. d6 exd6 29. Qd4 Rf7 30. fxg6 hxg6 31. Rxd6 a6 32. a3 Qa5 33. f4 Qh5 34. Qd2 Qc5 35. Rd5 Qc4 36. Rd7 Qc6 37. Rd6 Qe4+ 38. Ka2 Re7 39. Qc1 a5 40. Qf1 a4 41. Rd1 Qc2 42. Rd4 Re2 43. Rb4 b5 44. Qh1 Re7 45. Qd5 Re1 46. Qd7+ Kh6 47. Qh3+ Kg7 48. Qd7+ 1/2-1/2',
        ),
      ),
      { middle: 20, end: 44 },
    );
  });

  test('game7', () => {
    assert.deepEqual(
      dividePhases(
        pgnToNodes(
          '1. e4 e5 2. f4 d6 3. Nf3 exf4 4. Bc4 h6 5. O-O Bg4 6. d4 Nc6 7. Bxf4 Nf6 8. Nc3 Be7 9. e5 dxe5 10. dxe5 Nh5 11. Be3 O-O 12. h3 Bxf3 13. Qxf3 Nxe5 14. Bxf7+ Rxf7 15. Qxh5 Qf8',
        ),
      ),
      { middle: 24, end: undefined },
    );
  });
});
