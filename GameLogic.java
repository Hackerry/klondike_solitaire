import java.util.*;

public class GameLogic {
    static final int TALON_COUNT = 3;
    static final char EXIT = 'q', TALON = 'n', STOCK = 's', FOUNDATION = 'f', TABLEAU = 't';
    static final String BACK = "*-*";

    private GameDeck deck;
    private ListIterator<Card> talonPointer;
    private int remainStockSize, remainTalonSize;
    private Card[] talon;

    public GameLogic() {
        deck = new GameDeck();

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

        // User input
        try {
            initInput();
        } catch(Exception ex) {ex.printStackTrace();}
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

    private void moveTalon(int slot, boolean foundation) {
        // Talon is empty
        if(remainTalonSize == 0) return;

        System.out.println("Move talon to " + slot + " of " + (foundation? "foundation" : "tableau"));

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
            if(success) currSuit.push(moveCard);
        } else {
            // Move to tableau
            ArrayList<Card> currPile = deck.tableau.get(slot);
            if(currPile.isEmpty()) {
                if(moveCard.face == GameDeck.KING) success = true;
            } else {
                Card last = currPile.get(currPile.size()-1);
                if((last.isBlack ^ moveCard.isBlack) && (last.face == moveCard.face+1))
                    success = true;
                System.out.println("Last: " + last + last.face + " MoveCard: " + moveCard + moveCard.face);
                System.out.println("Cross: " + (last.isBlack ^ moveCard.isBlack));
            }
            if(success) currPile.add(moveCard);
            System.out.println("Success " + success + " " + currPile);
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
        System.out.println("Move foundation " + fromSlot + " to " + toSlot + " of tableau");

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
        System.out.println("Move tableau " + fromSlot + " to " + toSlot + " of " + (foundation? "foundation" : "tableau") + " starting from " + startNum);

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
                    if(moveCard.isShown && startNum == moveCard.face) {
                        index = i;
                        break;
                    }
                }
            }

            System.out.println("Found card at " + index + ", " + moveCard + moveCard.face);

            // Valid card input
            if(index != -1) {
                ArrayList<Card> toPile = deck.tableau.get(toSlot);
                if(toPile.isEmpty()) {
                    if(moveCard.face == GameDeck.KING) success = true;
                } else {
                    Card lastCard = toPile.get(toPile.size()-1);
                    if((lastCard.isBlack ^ moveCard.isBlack) && (lastCard.face == moveCard.face+1)) success = true;
                }

                System.out.println("Success " + success);

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

    private void initInput() throws Exception {
        Scanner sc = new Scanner(System.in);

        String input = "h";
        String[] parts;
        int fromSlot, toSlot, startNum;
        while(input.charAt(0) != EXIT) {
            // Print board
            printBoard();

            // Ask for user input
            if(input.equals("")) continue;
            input = sc.nextLine();
            parts = input.split(" ");

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
                        if(fromSlot != -1) {
                            moveTableau(fromSlot, toSlot, startNum, false);
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
            } else if(Character.toLowerCase(input.charAt(0)) != 'q') {
                System.out.println("Unrecognized command.");
            }
        }

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
        new GameLogic();
    }
}