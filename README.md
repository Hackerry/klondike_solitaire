# klondike_solitaire
<h2>Description</h2>
This is a bare bone Klondike Solitaire game implemented in Java.<br>
This game serves as a playing ground for exploring various properties about this game:<br>
<li>Test whether a game is winnable, given arrangement of all cards
<li>The possibility of randomly generating a winnable arrangement
<li>How many ways to solve a given set
<li>Ultimately, a genric solver???<br>

<h2>Terms</h2>
<b>Tableau (t):</b> 7 slots for putting cards on.<br>
<b>Foundation (f):</b> 4 slots for putting decks of cards in increasing face order.<br>
<b>Talon (n)</b>: 3 facing-up cards.<br>
<b>Stock (s)</b>: 24 cards that doesn't fit on the tableau at the start of the game.<br>

<h2>Commands</h2>
<b>Basic Form:</b> <code>[start pile][pile #] [to pile][pile #] (card face)<br></code><br>
<b>Examples:</b><br>
<li>Move from tableau 3 to foundation 2: <code>t3 f2</code> (can omit foundation # if moving to it)
<li>Move from talon to foundation 0: <code>n f0</code> (can omit foundation # if moving to it)
<li>Move from tableau 0 to foundation 6: <code>t0 t6</code>
<li>Move from foundation 1 to tableau 5: <code>f1 t5</code> (can move foundation cards back to tableau; must specify foundation #)<br>
<li>Move 6 of [suit] from tableau 2 to tableau 3: <code>t2 t3 6</code> (need 3rd argument if there are more than 1 card to be moved)<br>
<br>
<b>Special commands:</b><br>
<li><code>s[tock]</code> - draw from stock; flip the next 3 cards, if possible
<li><code>h[int]</code> - give a "valid" hint (only give a hint if can flip a card on tableau, move a card from stock, empty a slot on tableau or move a card to foundation)
<li><code>a[uto complete]</code> - move as many card to the foundation as possible
<li><code>i[nspect]</code> - inspect all cards; print all cards (debug)
<li><code>~</code> - solve the game (only greedy approach as till Dec. 29, 2019)<br>

<h2>Compile & Run</h2>
<b>Compile:</b> <code>javac GameLogic.java</code><br>
<b>Run:</b> <code>java GameLogic [from deck] [to deck]</code><br>
<b>Note:</b><br>
If [from deck] is given, will read deck configuration from file.<br>
If [to deck] is given, will write deck configuration to file.<br>
If [from deck] and [to deck] is both missing, a random deck is generated.<br>
If want to generate random deck and write to file, give a dummy file (not exsisting file) for [from deck].

<h2>Known Bugs</h2>
After moving 1 card from the talon, the order of stock is modified.
