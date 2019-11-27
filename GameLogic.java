import java.util.*;

public class GameLogic {
    static final int TALON_COUNT = 3;
    static final char EMPTY = '*', EXIT = 'q', TALON = 'n', STOCK = 's', FOUNDATION = 'f', TABLEAU = 't';

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

        // Debug
        updateTalon();

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

        // Remove from talon
        Card moveCard = talon[--remainTalonSize];
        talon[remainTalonSize] = null;
        talonPointer.remove();

        // Set talon pointer properly for the next remove
        if(talonPointer.hasPrevious()) {
            talonPointer.previous();
            talonPointer.next();
        }

        // TODO Move to specified location
        if(foundation) {
            // Check correct suit
            if(slot != moveCard.suit) {
                System.out.println("Wrong suit to foundation.");
            }
        } else {

        }
    }

    private void moveFoundation(int fromSlot, int toSlot) {

    }

    private void moveTableau(int fromSlot, int toSlot, int startNum, boolean foundation) {
        // -1 for last number only
        // number to start till end of pile
        // Test is startNum exists in the given tableau and whether is it possible to move
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
                    toSlot = getSlot(parts[1].substring(1));

                    if(toSlot < 0 || toSlot >= GameDeck.FOUNDATION_COUNT) {
                        System.out.println("Wrong slot number [0-3].");
                    } else {
                        moveTalon(toSlot, true);
                    }
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
                if(toSlot == -1) continue;
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
                } else if(Character.toLowerCase(parts[0].charAt(0)) == FOUNDATION) {
                    if(toSlot < 0 || toSlot >= GameDeck.FOUNDATION_COUNT) {
                        System.out.println("Wrong slot number [0-3].");
                        continue;
                    }

                    moveTableau(fromSlot, toSlot, -1, true);

                } else {
                    System.out.println("Illegal destination.");
                }
            } else if(Character.toLowerCase(parts[0].charAt(0)) == STOCK) {
                // Stock to talon
                updateTalon();
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
        for(ArrayList<Card> a: deck.tableau) System.out.println(a);

        // Debug print stock
        System.out.println(deck.stock);
    }

    public static void main(String[] args) {
        new GameLogic();
    }
}