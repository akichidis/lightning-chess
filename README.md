<p align="center">
  <img src="./lightning-chess-full-logo.png" alt="Lightning Chess" width="256">
</p>

# Lightning Chess

Lightning Chess is an open source 2-player chess app for the [Corda](https://corda.net) blockchain, initially built as a proof of concept to show that Corda can support offline transactions. The approach is similar to the [lightning network](https://lightning.network) suggested for Bitcoin and could theoretically be applied to any turn-based games. In short, two transactions are required to fully store a chess game on ledger, one to create the game and another to declare the winner (or a draw). In the meantime, players move pieces without notarising transactions, but by exchanging digital signatures on each move. The lightning chess protocol ensures that the smart contract will cryptographically verify the validity of the results.

**1st note:** Lightning Chess is still under development, the full Cordapp is still not functional and it's not recommended for production use.

**2nd note:** Not to be confused with rapid chess, in which each move has a fixed time allowed (usually 10 seconds). This is not a rapid or blitz chess game, but rather a chess protocol and application tailored to blockchains.

## Why Corda and not Ethereum?
Because Corda is more versatile, provides extra privacy and it's actually doable with Corda. Take a look at this [interesting experiment](https://medium.com/@graycoding/lessons-learned-from-making-a-chess-game-for-ethereum-6917c01178b6) where a student project from Technical University of Berlin engaged in making a Chess game for Ethereum; but, they faced challenges and their conclusion was:
> The Ethereum virtual machine is turing-complete, but that doesn’t mean that everything should be computed on it. 

## Features

* Web-based user interface that allows Corda node owners to interactively play an online chess game.
* Full Cordapp to store chess states in Corda's Vault and validate the results. 
* Offline gameplay (in terms of notarisation).

## Implementation iterations

- Basic PoC in which the smart contract verifies digital signatures only. This version assumes no malicious users and time to move is not taken into account. We are currently at this stage.
- The smart contract is equipped with chess state validation logic (i.e., if a move is valid). If a user signs an invalid move, this can be used as a cryptographic evidence of malicious activity and the opponent will be able to provide this proof to win the game. Along the same lines, a checkmate is automatically checked by the contract's verify method. 
- Use Oracles for handling disputes on "time to respond".

## Full expected protocol (WIP)

**Happy path process**

1. players agree on random gameID and colours (similarly to key agreement).
2. initial commitment (fact-time lock tx) → send for notarisation.
3. off chain game:
   1. player signs next move
   2. sends hash (or gameID + sequence) to SequenceKeeper
   3. SequenceKeeper signs and replies back
   4. player forwards to the other player
   5. next player's turn (go to 3.i)
4. game ends → submit a tx that includes the last two signed moves. For instance, a player provides the signed winning move + the signed previous move of the opponent. Alternatively, a signed acceptance/resignation from the other party or a mutually signed agreement on the result would be enough (this is the 1st iteration). The main benefit with this approach is you don't need to send the full move-sequence and chessboard states, simplifying the contract logic by  large degree.
5. The aim is that the smart contract verify logic should be able to identify a winning or draw state, so consuming of the potentially encumbered assets (awards) is possible.

**Signed payload per move**

Each player should sign his/her move before sending it to the opponent. The signed `GameMove` object should have the following properties:
* `gameId`: required to ensure that this signature applies to a particular chess game; note that each `gameId` should be unique.  
* `index`: a counter that represents the actual _move-index_, starting from zero. We need this to avoid replay attacks by reusing an old signature that was generated in a previous move.   
* `fen`: the game position in [FEN](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation) form. Theoretically, we could avoid sending the current state on each move, because everytime users sign their next move, they inherently accepted the previous state. However, including FEN will speed up move validation, as reruning the whole game from scratch is not required.
* `move`: the actual _from-to_ move. We highlight that a move might involve two pieces as in teh case of [castling](https://en.wikipedia.org/wiki/Castling).
* `previousSignature`: opponent's signature on his/her most recent move. This is required to create a chain of actions and resist against malicious users signing different moves, then trying to "convince" the smart contract that the opponent played a wrong move.

**Ideas:** 
1. sequence of moves can work like a blockchain Vs a counter.
2. using the encumbrance feature one can create a tournament with real cash prizes.
3. we could use hash commitments to avoid full signature verification when both parties are honest. Briefly, during game creation, each user can provide a commitment to a hash pre-image, which will be revealed if this user loses the game. Thus, the winner can use it as a cryptographic evidence to win the game. The same process can be applied if a game ends in a draw.
4. extend the above hash commitment scheme using hash-based post-quantum signatures, such as the Lamport, WOTS, BPQS, XMSS and Sphincs schemes. We also realised that chess in particular is a great example for short hash-based signatures per move (more details in an upcoming scientific paper).

**Prerequisites:** a passive Oracle (SequenceKeeper) is required (it can be a BFT cluster for advanced security/trust, but accurancy in the level of seconds is tricky anyway with leader-based schemes). Note that oracles are only required for disputes on "time to respond" and they don't need to have visibility on the actual game state (moves).

**Dispute cases**
 1. Player leaves the game earlier or refuses to play (on purpose or unexpectedly): Request a signature from SequenceKeeper that the other party has not responded on time (WIP: time-out policy to be defined).
 2. Player makes an invalid move: The other player reveals the signed previous state(s) and the signed "malicious" move. Smart contract logic should be able to identify a wrong move and the other party can use this as evidence to win the game. Chess software should only allow valid moves, thus an invalid move can only happen by hacking the game engine.

## Contributing

We welcome contributions to Lightning Chess! You may find the list of contributors [here](./CONTRIBUTORS.md).

## License

[Apache 2.0](./LICENSE.md)

## Acknowledgements

[Corda](https://corda.net), a blockchain and smart contract platform developed by [R3](https://r3.com). As a blockchain platform, Corda allows parties to transact directly, with value. Smart contracts allow Corda to do this using complex agreements and any asset type. This capability has broad applications across industries including finance, supply chain and healthcare.
