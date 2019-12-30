import java.util.*;
import java.io.*;

public class GameLogic {
    static final int TALON_COUNT = 3;
    static final char EXIT = 'q', TALON = 'n', STOCK = 's', FOUNDATION = 'f', TABLEAU = 't', AUTO = 'a', HINT = 'h', SOLVE = '~', INSPECT = 'i';
    static final String BACK = "*-*", MOVE_FILE = "moves";
    static final String HINT_STR = "Move card %s from %s to %s";

    private GameDeck deck;
    private ListIterator<Card> talonPointer;
    private int remainStockSize, remainTalonSize;
    private Card[] talon;
    private String deckString;
    private boolean solveMode = false;
    // Solver variable
    private boolean pulled = false;
    private String input = "?";

    public GameLogic(String inputFile, String outputFile) {
        String input = null;

        // Read potential input file
        if(inputFile != null && !inputFile.equals("")) {
            try {
                File inFile = new File(inputFile);

                if(inFile.exists() && inFile.isFile() && inFile.canRead()) {
                    BufferedReader reader = new BufferedReader(new FileReader(inFile));
                    StringBuffer buff = new StringBuffer();
                    String nextLine;
                    while((nextLine = reader.readLine()) != null) buff.append(nextLine);
                    reader.close();

                    input = buff.toString();
                    System.out.println("Successful read from file " + inFile.getName());
                }
            } catch(IOException ex) {
                System.out.println("Can't read file.");
            }
        }
        deck = new GameDeck(input);

        // Initialize board
        talon = new Card[TALON_COUNT];
        talonPointer = deck.stock.listIterator();
        remainStockSize = deck.stock.size();
        remainTalonSize = 0;

        // Flip the last card of each pile
        for(int i = 0; i < GameDeck.TABLEAU_COUNT; i++) {
            ArrayList<Card> currPile = deck.tableau.get(i);
            currPile.get(currPile.size()-1).isShown = true;
        }

        // Flip all cards in stock
        for(Card c: deck.stock) c.isShown = true;

        // Store deck string before modify
        if(input == null) deckString = deck.getDeckString();

        // User input
        try {
            initInput();
        } catch(Exception ex) {ex.printStackTrace();}

        // If card is randomly generated, print last played deck
        if(input == null) {
            if(outputFile != null) {
                try {
                    File outFile = new File(outputFile);
                    if(!outFile.exists()) outFile.createNewFile();
                    PrintWriter pw = new PrintWriter(outFile);
                    pw.print(deckString);
                    pw.close();
                } catch(IOException ex) {
                    System.out.println("Can't open or write to file.");
                }
            } else {
                System.out.println("The deck you just played: ");
                System.out.println(deckString);
            }
        }
    }

    private void updateTalon() {
        // Have reach end, reset talon and stock count
        if(remainStockSize == 0) {
            for(int i = 0; i < TALON_COUNT; i++) talon[i] = null;
            remainStockSize = deck.stock.size();
            remainTalonSize = 0;
            talonPointer = deck.stock.listIterator();
            return;
        }

        // Reset talon to null
        for(int i = 0; i < TALON_COUNT; i++) talon[i] = null;

        // Show the next three cards
        int index = 0;
        remainTalonSize = 0;
        while(remainStockSize > 0 && index < TALON_COUNT) {
            talon[index++] = talonPointer.next();
            remainStockSize--;
            remainTalonSize++;
        }
    }

    /**
     * Contain last card from talon
     */
    private Card[] getLastCards() {
        Card[] possibleCards = new Card[GameDeck.TABLEAU_COUNT+1];
        // Get cards from tableau
        for(int i = 0; i < GameDeck.TABLEAU_COUNT; i++) {
            ArrayList<Card> pile = deck.tableau.get(i);
            if(!pile.isEmpty()) {
                possibleCards[i] = pile.get(pile.size()-1);
            }
        }
        // Get cards from talon
        if(remainTalonSize != 0 && talonPointer.hasPrevious()) {
            possibleCards[possibleCards.length-1] = talonPointer.previous();
            talonPointer.next();
        }

        return possibleCards;
    }

    private void autoMove() {
        // Get all possible cards
        Card[] possibleCards = getLastCards();

        int movedIdx;
        do {
            movedIdx = -1;
            for(int i = 0; i < possibleCards.length; i++) {
                Card c = possibleCards[i];
                if(c == null) continue;
                if(deck.foundation.get(c.suit).isEmpty()) {
                    if(c.face == GameDeck.ACE) {
                        if(i != possibleCards.length-1) moveTableau(i, c.suit, -1, true);
                        else moveTalon(c.suit, true);
                        movedIdx = i;
                        break;
                    }
                } else {
                    if(c.face == deck.foundation.get(c.suit).peek().face+1) {
                        if(i != possibleCards.length-1) moveTableau(i, c.suit, -1, true);
                        else moveTalon(c.suit, true);
                        movedIdx = i;
                        break;
                    }
                }
            }

            // Update moved card
            if(movedIdx != -1) {
                if(movedIdx != possibleCards.length-1) {
                    ArrayList<Card> pile = deck.tableau.get(movedIdx);
                    if(!pile.isEmpty()) possibleCards[movedIdx] = pile.get(pile.size()-1);
                } else {
                    if(remainTalonSize != 0 && talonPointer.hasPrevious()) {
                        possibleCards[possibleCards.length-1] = talonPointer.previous();
                        talonPointer.next();
                    }
                }
            }
        } while(movedIdx != -1);
    }

    private String giveHint() {
        Card[] lastCards = getLastCards();
        ArrayList<Card> pile;
        int lastFace;
        boolean goodMove, lastSuit;

        // N-square find all possible card moves (exclude unmeaningful swaps unless it can improve situation)
        for(int i = GameDeck.TABLEAU_COUNT-1; i >= 0; i--) {
            pile = deck.tableau.get(i);

            // Find possible straights within a pile
            lastFace = -1; lastSuit = true;
            for(int j = pile.size()-1; j >= 0; j--) {
                Card curr = pile.get(j);

                // Test valid straight and whether it's flipped or not
                if((lastFace != -1 && (lastSuit & curr.isBlack || lastFace+1 != curr.face)) || !curr.isShown) break;

                goodMove = false;
                // Empty a pile, always good, but not in the case of K (move between empty piles)
                if(j-1 < 0) {
                    if(curr.face != GameDeck.KING) goodMove = true;
                } else {
                    Card last = pile.get(j-1);
                    // Can flip another card
                    if(!last.isShown) goodMove = true;
                    // Card before can be moved to foundation
                    else {
                        if(deck.foundation.get(last.suit).isEmpty()) {
                            if(last.face == GameDeck.ACE) goodMove = true;
                        } else {
                            if(deck.foundation.get(last.suit).peek().face+1 == last.face) goodMove = true;
                        }
                    }
                }

                // Last card move to foundation, always good
                if(lastFace == -1) {
                    if(deck.foundation.get(curr.suit).isEmpty()) {
                        if(curr.face == GameDeck.ACE) {
                            System.out.println("Move " + GameDeck.FACE[curr.face] + " to foundation");
                            return "t" + i + " f";
                        }
                    } else {
                        if(deck.foundation.get(curr.suit).peek().face+1 == curr.face) {
                            System.out.println("Move " + GameDeck.FACE[curr.face] + " to foundation");
                            return "t" + i + " f";
                        }
                    }
                }

                if(goodMove) {
                    // If it's a good move, test validity
                    for(int k = 0; k < GameDeck.TABLEAU_COUNT; k++) {
                        // Same pile
                        if(k == i) continue;

                        // If current card is K, only empty piles are possible
                        if(curr.face == GameDeck.KING) {
                            if(lastCards[k] == null) {
                                System.out.println("Move " + GameDeck.FACE[curr.face] + " from pile " + i + " to pile " + k);
                                return "t" + i + " t" + k + " " + GameDeck.FACE[curr.face];
                            }
                        } else if(lastCards[k] != null && lastCards[k].isBlack ^ curr.isBlack && curr.face+1 == lastCards[k].face) {
                            System.out.println("Move " + GameDeck.FACE[curr.face] + " from pile " + i + " to pile " + k);
                            return "t" + i + " t" + k + " " + GameDeck.FACE[curr.face];
                        }
                    }
                }

                // Assign last face and last suit
                lastFace = curr.face;
                lastSuit = curr.isBlack;
            }
        }

        if(remainTalonSize != 0) {
            // Last card from talon
            // Always good to move talon to anywhere (reveal the next card)
            // To foundation
            Card talonLast = lastCards[lastCards.length-1];
            if(deck.foundation.get(talonLast.suit).isEmpty()) {
                if(talonLast.face == GameDeck.ACE) {
                    System.out.println("Move " + GameDeck.FACE[talonLast.face] + "(talon) to foundation");
                    return "n f";
                }
            } else {
                if(deck.foundation.get(talonLast.suit).peek().face+1 == talonLast.face) {
                    System.out.println("Move " + GameDeck.FACE[talonLast.face] + "(talon) to foundation");
                    return "n f";
                }
            }

            // To tableau
            for(int k = 0; k < GameDeck.TABLEAU_COUNT; k++) {
                // A king
                if(talonLast.face == GameDeck.KING) {
                    if(lastCards[k] == null) {
                        System.out.println("Move " + GameDeck.FACE[talonLast.face] + "(talon) to pile " + k);
                        return "n t" + k;
                    }
                } else if(lastCards[k] != null && lastCards[k].isBlack ^ talonLast.isBlack && talonLast.face+1 == lastCards[k].face) {
                    System.out.println("Move " + GameDeck.FACE[talonLast.face] + "(talon) to pile " + k);
                    return "n t" + k;
                }
            }
        }

        System.out.println("No hints found");
        return null;
    }

    private void moveTalon(int slot, boolean foundation) {
        // Talon is empty
        if(remainTalonSize == 0) return;

        // System.out.println("Move talon to " + slot + " of " + (foundation? "foundation" : "tableau"));

        // Check valid move
        boolean success = false;
        Card moveCard = talon[remainTalonSize-1];
        if(foundation) {
            Stack<Card> currSuit = deck.foundation.get(moveCard.suit);
            if(currSuit.isEmpty()) {
                if(moveCard.face == GameDeck.ACE) success = true;
            } else if(currSuit.peek().face+1 == moveCard.face) {
                success = true;
            }
            if(success) {
                currSuit.push(moveCard);
                checkWin();
            }
        } else {
            // Move to tableau
            ArrayList<Card> currPile = deck.tableau.get(slot);
            if(currPile.isEmpty()) {
                if(moveCard.face == GameDeck.KING) success = true;
            } else {
                Card last = currPile.get(currPile.size()-1);
                if((last.isBlack ^ moveCard.isBlack) && (last.face == moveCard.face+1))
                    success = true;
            }
            if(success) currPile.add(moveCard);
        }

        // Successful move
        if(success) {
            talon[--remainTalonSize] = null;
            talonPointer.remove();

            // Set talon pointer properly for the next remove
            if(talonPointer.hasPrevious()) {
                talonPointer.previous();
                talonPointer.next();
            }
        }
    }

    private void moveFoundation(int fromSlot, int toSlot) {
        // System.out.println("Move foundation " + fromSlot + " to " + toSlot + " of tableau");

        Stack<Card> currSuit = deck.foundation.get(fromSlot);
        ArrayList<Card> currPile = deck.tableau.get(toSlot);
        boolean success = false;

        if(!currSuit.isEmpty()) {
            Card moveCard = currSuit.peek();
            if(currPile.isEmpty()) {
                if(moveCard.face == GameDeck.KING) success = true;
            } else {
                Card lastCard = currPile.get(currPile.size()-1);
                if((lastCard.isBlack ^ moveCard.isBlack) && (lastCard.face == moveCard.face+1)) success = true;
            }

            if(success) {
                currSuit.pop();
                currPile.add(moveCard);
            }
        }
    }

    private void moveTableau(int fromSlot, int toSlot, int startNum, boolean foundation) {
        // System.out.println("Move tableau " + fromSlot + " to " + toSlot + " of " + (foundation? "foundation" : "tableau") + " starting from " + startNum);

        ArrayList<Card> currPile = deck.tableau.get(fromSlot);
        // Empty pile
        if(currPile.isEmpty()) return;

        boolean success = false;
        if(foundation) {
            // Move to foundation
            Card moveCard = currPile.get(currPile.size()-1);
            Stack<Card> currSuit = deck.foundation.get(moveCard.suit);
            if(currSuit.isEmpty()) {
                if(moveCard.face == GameDeck.ACE) success = true;
            } else if(currSuit.peek().face+1 == moveCard.face) {
                success = true;
            }

            if(success) {
                currSuit.push(moveCard);
                currPile.remove(currPile.size()-1);
                checkWin();
            }
        } else {
            // Move to other tableau
            int index = -1;
            Card moveCard = null;

            if(startNum == -1) {
                // Default to the last card
                index = currPile.size()-1;
                moveCard = currPile.get(index);
            } else {
                for(int i = 0; i < currPile.size(); i++) {
                    moveCard = currPile.get(i);
                    if(moveCard.isShown && startNum-1 == moveCard.face) {
                        index = i;
                        break;
                    }
                }
            }

            // Valid card input
            if(index != -1) {
                ArrayList<Card> toPile = deck.tableau.get(toSlot);
                if(toPile.isEmpty()) {
                    if(moveCard.face == GameDeck.KING) success = true;
                } else {
                    Card lastCard = toPile.get(toPile.size()-1);
                    if((lastCard.isBlack ^ moveCard.isBlack) && (lastCard.face == moveCard.face+1)) success = true;
                }

                if(success) {
                    // Move all cards starting from index to destination pile
                    List<Card> transferCards = currPile.subList(index, currPile.size());
                    toPile.addAll(transferCards);
                    currPile.removeAll(transferCards);
                }
            }
        }

        // Potentially flip a new card
        if(success) {
            if(!currPile.isEmpty())
                currPile.get(currPile.size()-1).isShown = true;
        }
    }

    private boolean checkWin() {
        for(Stack<Card> suit: deck.foundation) {
            if(suit.isEmpty() || suit.peek().face != GameDeck.KING) return false;
        }

        printBoard();
        System.out.println("You win!!!");
        // Terminate loop
        input = "q";

        if(solveMode) System.out.println("O_O!");
        return true;
    }

    private void initInput() throws Exception {
        Scanner sc = new Scanner(System.in);

        String[] parts;
        int fromSlot, toSlot, startNum;
        do {
            // Print board
            System.out.println();
            printBoard();

            // Input taken over by solver
            if(solveMode) {
                String nextMove = giveHint();

                // No hints available
                if(nextMove == null) {
                    // Finished examining stock
                    if(remainStockSize == 0) {
                        if(pulled) {
                            // Have drawn some cards from talon, continue
                            pulled = false;
                            input = "s";
                        } else {
                            // Else solver gets stuck
                            System.out.println("x_x");
                            solveMode = false;
                            return;
                        }
                    } else {
                        // Keep examing stock
                        input = "s";
                    }
                } else {
                    // Execute next hint
                    input = nextMove;
                    if(nextMove.charAt(0) == TALON) pulled = true;
                    System.err.println(nextMove);
                }

                // Easier to see output
                try {
                    Thread.sleep(1000);
                } catch(Exception ex) {break;}
            } else {
                // Input taken over by user
                input = sc.nextLine().trim();
            }

            parts = input.split(" ");

            // Mistyped commands
            if(input.equals("") || parts.length == 0) {
                input = "?";
                continue;
            }

            for(String s: parts) {
                if(s.length() == 0) {
                    input = "?";
                    continue;
                }
            }

            // Check input format
            if(Character.toLowerCase(parts[0].charAt(0)) == TALON) {
                // Test length
                if(parts.length < 2) {
                    System.out.println("Missing destination pile.");
                    continue;
                }

                if(Character.toLowerCase(parts[1].charAt(0)) == FOUNDATION) {
                    // Talon to foundation
                    moveTalon(-1, true);
                } else if(Character.toLowerCase(parts[1].charAt(0)) == TABLEAU) {
                    // Talon to tableau
                    toSlot = getSlot(parts[1].substring(1));

                    if(toSlot < 0 || toSlot >= GameDeck.TABLEAU_COUNT) {
                        System.out.println("Wrong slot number [0-6].");
                    } else {
                        moveTalon(toSlot, false);
                    }
                } else {
                    System.out.println("Illegal destination.");
                }
            } else if (Character.toLowerCase(parts[0].charAt(0)) == FOUNDATION) {
                // Test length
                if(parts.length < 2) {
                    System.out.println("Missing destination.");
                    continue;
                }

                // Find from slot
                fromSlot = getSlot(parts[0].substring(1));
                if(fromSlot < 0 || fromSlot >= GameDeck.FOUNDATION_COUNT) {
                    System.out.println("Wrong slot number [0-3].");
                    continue;
                }

                // Find destination tableau
                if(Character.toLowerCase(parts[1].charAt(0)) == TABLEAU) {
                    toSlot = getSlot(parts[1].substring(1));
                    if(toSlot < 0 || toSlot >= GameDeck.TABLEAU_COUNT) {
                        System.out.println("Wrong slot number [0-6].");
                    } else {
                        moveFoundation(fromSlot, toSlot);
                    }
                } else {
                    System.out.println("Illegal destination.");
                }
            } else if(Character.toLowerCase(parts[0].charAt(0)) == TABLEAU) {
                // Test length
                if(parts.length < 2) {
                    System.out.println("Missing destination.");
                    continue;
                }

                // Find from slot
                fromSlot = getSlot(parts[0].substring(1));
                if(fromSlot < 0 || fromSlot >= GameDeck.TABLEAU_COUNT) {
                    System.out.println("Wrong slot number [0-6].");
                    continue;
                }

                // Find destination
                toSlot = getSlot(parts[1].substring(1));
                if(Character.toLowerCase(parts[1].charAt(0)) == TABLEAU) {
                    if(toSlot < 0 || toSlot >= GameDeck.TABLEAU_COUNT) {
                        System.out.println("Wrong slot number [0-6].");
                        continue;
                    }

                    // Test is there a third arguement
                    if(parts.length >= 3) {
                        startNum = getSlot(parts[2]);
                        if(startNum != -1) {
                            moveTableau(fromSlot, toSlot, startNum, false);
                        } else if(Character.toLowerCase(parts[2].charAt(0)) == 'j') {
                            moveTableau(fromSlot, toSlot, GameDeck.JACK+1, false);
                        } else if(Character.toLowerCase(parts[2].charAt(0)) == 'q') {
                            moveTableau(fromSlot, toSlot, GameDeck.QUEEN+1, false);
                        } else if(Character.toLowerCase(parts[2].charAt(0)) == 'k') {
                            moveTableau(fromSlot, toSlot, GameDeck.KING+1, false);
                        } else if(Character.toLowerCase(parts[2].charAt(0)) == 'a') {
                            moveTableau(fromSlot, toSlot, GameDeck.ACE+1, false);
                        } else if(Character.toLowerCase(parts[2].charAt(0)) == 'x') {
                            moveTableau(fromSlot, toSlot, GameDeck.TEN+1, false);
                        }
                    } else {
                        moveTableau(fromSlot, toSlot, -1, false);
                    }
                } else if(Character.toLowerCase(parts[1].charAt(0)) == FOUNDATION) {
                    moveTableau(fromSlot, toSlot, -1, true);
                } else {
                    System.out.println("Illegal destination.");
                }
            } else if(Character.toLowerCase(parts[0].charAt(0)) == STOCK) {
                // Stock to talon
                updateTalon();
            } else if(Character.toLowerCase(parts[0].charAt(0)) == AUTO) {
                // Move cards from tableau to foundation automatically
                autoMove();
            } else if(Character.toLowerCase(parts[0].charAt(0)) == HINT) {
                // Give hint
                giveHint();
            }  else if(Character.toLowerCase(parts[0].charAt(0)) == INSPECT) {
                // Inspect the deck, show all cards
                for(ArrayList<Card> v: deck.tableau) {
                    for(Card c: v) System.out.print(c);
                    System.out.println();
                }
                for(Card c: deck.stock) System.out.print(c);
                System.out.println();
            } else if(Character.toLowerCase(parts[0].charAt(0)) == SOLVE) {
                // Use hint to solve
                solveMode = true;
                pulled = false;
                System.out.println("Solve mode on");
            } else if(Character.toLowerCase(input.charAt(0)) != 'q') {
                System.out.println("Unrecognized command.");
            }
        } while(input.charAt(0) != EXIT);

        sc.close();
    }

    private int getSlot(String s) {
        int slot;
        try {
            slot = Integer.parseInt(s);
        } catch(Exception ex) {
            slot = -1;
        }

        return slot;
    }

    private void printBoard() {
        // Print Foundation
        for(int i = 0; i < GameDeck.FOUNDATION_COUNT; i++) {
            if(deck.foundation.get(i).isEmpty()) System.out.print(GameDeck.SUIT_SYMBOL[i] + " ");
            else System.out.print(deck.foundation.get(i).peek() + " ");
        }
        System.out.printf("%5s", " ");

        // Print Talon
        for(Card c: talon) {
            if(c != null) System.out.print(c + " ");
        }
        System.out.printf("%5s", " ");

        // Print Stock number
        System.out.println(remainStockSize);

        // Print Tableau
        for(int i = 0; i < deck.tableau.size(); i++) {
            System.out.print(i + ": ");

            for(Card c: deck.tableau.get(i))
                System.out.print(c.isShown? c: "[" + BACK + "]");
            System.out.println();
        }

        // Debug print stock
        // System.out.println(deck.stock);
    }

    public static void main(String[] args) {
        String file = null;
        if(args.length >= 1) {
            if(args.length >= 2) file = args[1];
            new GameLogic(args[0], file);
        } else new GameLogic(null, file);
    }
}