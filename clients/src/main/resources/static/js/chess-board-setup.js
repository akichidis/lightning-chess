var CHESSBOARD = {};
var game, board;
var CHESS_MOVE_ENABLED = false;

var setupChessBoard = function(onPieceMove, orientation) {
    game = new Chess();

    var cfg = {
      draggable: true,
      orientation: orientation == null ? 'white' : orientation,
      position: 'start',
      onDragStart: onDragStart,
      onDrop: function(source, target) {
        onDrop(source, target, onPieceMove);
      },
      onMouseoutSquare: onMouseoutSquare,
      onMouseoverSquare: onMouseoverSquare,
      onSnapEnd: onSnapEnd
    };

    board = ChessBoard('chessBoard', cfg);
}

var removeGreySquares = function() {
  $('#chessBoard .square-55d63').css('background', '');
};

var greySquare = function(square) {
  var squareEl = $('#chessBoard .square-' + square);

  var background = '#a9a9a9';
  if (squareEl.hasClass('black-3c85d') === true) {
    background = '#696969';
  }

  squareEl.css('background', background);
};

var onDragStart = function(source, piece) {
  if (!CHESS_MOVE_ENABLED) {
    return false;
  }

  // do not pick up pieces if the game is over
  // or if it's not that side's turn
  if (game.game_over() === true ||
      (game.turn() === 'w' && piece.search(/^b/) !== -1) ||
      (game.turn() === 'b' && piece.search(/^w/) !== -1)) {
    return false;
  }
};

var onDrop = function(source, target, onPieceMove) {
  if (!CHESS_MOVE_ENABLED) {
    return false;
  }

  removeGreySquares();

  // see if the move is legal
  var move = game.move({
    from: source,
    to: target,
    promotion: 'q' // NOTE: always promote to a queen for example simplicity
  });

  // illegal move
  if (move === null) return 'snapback';

  onPieceMove(move, board.fen());
};

var onMouseoverSquare = function(square, piece) {
  if (!CHESS_MOVE_ENABLED) {
    return false;
  }

  // get list of possible moves for this square
  var moves = game.moves({
    square: square,
    verbose: true
  });

  // exit if there are no moves available for this square
  if (moves.length === 0) return;

  // highlight the square they moused over
  greySquare(square);

  // highlight the possible squares for this piece
  for (var i = 0; i < moves.length; i++) {
    greySquare(moves[i].to);
  }
};

var onMouseoutSquare = function(square, piece) {
  if (!CHESS_MOVE_ENABLED) {
    return false;
  }

  removeGreySquares();
};

var onSnapEnd = function() {
  if (!CHESS_MOVE_ENABLED) {
    return false;
  }

  board.position(game.fen());
};