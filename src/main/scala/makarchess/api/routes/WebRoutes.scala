package makarchess.api.routes

import cats.effect.Concurrent
import org.http4s.{HttpRoutes, Response, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType

import java.io.File

final class WebRoutes[F[_]: Concurrent] extends Http4sDsl[F]:
  
  private val indexHtml = """<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>MaKarChess Web</title>
    <style>
        body { font-family: sans-serif; margin: 20px; }
        .board { display: grid; grid-template-columns: repeat(8, 50px); gap: 1px; width: 408px; margin: 20px 0; }
        .square { width: 50px; height: 50px; display: flex; align-items: center; justify-content: center; font-size: 32px; cursor: pointer; }
        .light { background-color: #f0d9b5; }
        .dark { background-color: #b58863; }
        .selected { background-color: #f6f669 !important; }
        .controls { margin: 20px 0; }
        button { margin: 5px; padding: 10px; }
        textarea { width: 400px; height: 150px; font-family: monospace; }
        #moveHistory { width: 400px; height: 150px; overflow-y: auto; border: 1px solid #ccc; padding: 10px; font-family: monospace; }
        .status { margin: 10px 0; padding: 10px; background: #f0f0f0; }
    </style>
</head>
<body>
    <h1>MaKarChess Web</h1>
    <div class="status" id="status">Loading...</div>
    <div class="board" id="board"></div>
    <div class="controls">
        <button onclick="newGame()">New Game</button>
        <button onclick="resetGame()">Reset</button>
        <button onclick="replayBack()">&lt;</button>
        <button onclick="replayForward()">&gt;</button>
    </div>
    <div>
        <h3>PGN Input:</h3>
        <textarea id="pgnInput" placeholder="1. e4 e5 2. Nf3..."></textarea><br>
        <button onclick="loadPGN()">Load PGN</button>
    </div>
    <div>
        <h3>FEN Input:</h3>
        <input type="text" id="fenInput" placeholder="rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1" style="width: 400px;"><br>
        <button onclick="loadFEN()">Load FEN</button>
    </div>
    <div>
        <h3>Move History / PGN:</h3>
        <div id="moveHistory"></div>
    </div>
    
    <script>
        let selectedSquare = null;
        let gameState = null;
        
        const pieces = {
            'white': { 'king': '♔', 'queen': '♕', 'rook': '♖', 'bishop': '♗', 'knight': '♘', 'pawn': '♙' },
            'black': { 'king': '♚', 'queen': '♛', 'rook': '♜', 'bishop': '♝', 'knight': '♞', 'pawn': '♟' }
        };
        
        function createBoard() {
            const board = document.getElementById('board');
            board.innerHTML = '';
            for (let rank = 8; rank >= 1; rank--) {
                for (let file = 0; file < 8; file++) {
                    const fileChar = String.fromCharCode('a'.charCodeAt(0) + file);
                    const square = document.createElement('div');
                    square.className = 'square ' + ((rank + file) % 2 === 0 ? 'light' : 'dark');
                    square.id = fileChar + rank;
                    square.onclick = () => onSquareClick(fileChar + rank);
                    board.appendChild(square);
                }
            }
        }
        
        function updateBoard(state) {
            document.querySelectorAll('.square').forEach(sq => {
                sq.innerHTML = '';
                sq.classList.remove('selected');
            });
            
            if (state && state.board) {
                state.board.forEach(square => {
                    const el = document.getElementById(square.position.file + square.position.rank);
                    if (el) {
                        el.innerHTML = pieces[square.piece.color][square.piece.kind];
                    }
                });
            }
            
            const status = gameState ? 
                `Turn: ${state.sideToMove} | Phase: ${state.phase}${state.isCheck ? ' | CHECK!' : ''}` 
                : 'Loading...';
            document.getElementById('status').textContent = status;
        }
        
        function updateMoveHistory() {
            fetch('/game/status')
                .then(r => r.json())
                .then(status => {
                    const moves = status.moveHistory || [];
                    let html = '';
                    for (let i = 0; i < moves.length; i += 2) {
                        html += `${Math.floor(i/2) + 1}. ${moves[i]} ${moves[i+1] || ''}<br>`;
                    }
                    document.getElementById('moveHistory').innerHTML = html || 'No moves yet';
                });
        }
        
        function onSquareClick(pos) {
            if (!selectedSquare) {
                selectedSquare = pos;
                document.getElementById(pos).classList.add('selected');
            } else {
                const move = selectedSquare + pos;
                makeMove(move);
                selectedSquare = null;
                document.querySelectorAll('.selected').forEach(el => el.classList.remove('selected'));
            }
        }
        
        function fetchState() {
            fetch('/game/board')
                .then(r => r.json())
                .then(state => {
                    gameState = state;
                    updateBoard(state);
                    updateMoveHistory();
                })
                .catch(err => console.error('Error:', err));
        }
        
        function makeMove(uci) {
            fetch('/game/move', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ uci: uci.toLowerCase() })
            })
            .then(r => r.json())
            .then(state => {
                gameState = state;
                updateBoard(state);
                updateMoveHistory();
            })
            .catch(err => alert('Invalid move'));
        }
        
        function newGame() {
            fetch('/game/new', { method: 'POST' })
                .then(() => fetchState());
        }
        
        function resetGame() {
            fetch('/game/reset', { method: 'POST' })
                .then(() => fetchState());
        }
        
        function loadPGN() {
            const pgn = document.getElementById('pgnInput').value;
            fetch('/game/pgn', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pgn: pgn })
            })
            .then(() => fetchState());
        }
        
        function loadFEN() {
            const fen = document.getElementById('fenInput').value;
            fetch('/game/fen', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ fen: fen })
            })
            .then(() => fetchState());
        }
        
        function replayBack() {
            fetch('/game/replay/backward', { method: 'POST' })
                .then(() => fetchState());
        }
        
        function replayForward() {
            fetch('/game/replay/forward', { method: 'POST' })
                .then(() => fetchState());
        }
        
        createBoard();
        fetchState();
    </script>
</body>
</html>"""

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      Ok(indexHtml, `Content-Type`(MediaType.text.html))
    
    case GET -> Root / "health" =>
      Ok("""{"status":"ok"}""")
  }
end WebRoutes
