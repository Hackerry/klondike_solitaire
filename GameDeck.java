import java.util.*;

public class GameDeck {
    static final int SPADE = 0, CLUB = 1, HEART = 2, DIAMOND = 3;
    static final String[] SUIT_SYMBOL = {"\u2660","\u2663","\u2661","\u2662"};
    static final String[] FACE = {"A","2","3","4","5","6","7","8","9","10","J","Q","K"};
    static final int DECK_COUNT = 52, TABLEAU_COUNT = 7, FOUNDATION_COUNT = 4, TALON_COUNT = 3, ACE = 0;
    static final int TEN = 9, JACK = 10, QUEEN = 11, KING = 12;
    static final int SHUFFLE_COUNT = DECK_COUNT*2, SUIT_COUNT = 13, BLACK = 2;

    protected ArrayList<ArrayList<Card>> tableau;
    protected ArrayList<Stack<Card>> foundation;
    protected LinkedList<Card> stock;

    public GameDeck(String input) {
        // Initialize tableau
        tableau = new ArrayList<>(DECK_COUNT);
        for(int i = 0; i < TABLEAU_COUNT; i++) tableau.add(new ArrayList<>());

        // Initialize foundation
        foundation = new ArrayList<>(FOUNDATION_COUNT);
        for(int i = 0; i < FOUNDATION_COUNT; i++) foundation.add(new Stack<>());

        // Initailize stock
        stock = new LinkedList<>();

        // Generate all cards randomly or from input
        ArrayList<Card> deck = (input == null? randomDeck(): loadDeck(input));

        // Assign cards to tableau
        int index = 0;
        for(int i = 0; i < TABLEAU_COUNT; i++) {
            for(int j = 0; j <= i; j++) {
                tableau.get(i).add(deck.get(index++));
            }
        }

        // The rest to stock
        stock = new LinkedList<>(deck.subList(index, deck.size()));
        
        // Debug
        // printTableau();printStock();
    }

    private ArrayList<Card> randomDeck() {
        ArrayList<Card> deck = new ArrayList<>(DECK_COUNT);
        for(int i = 0; i < FOUNDATION_COUNT; i++) {
            for(int j = 0; j < FACE.length; j++) {
                deck.add(new Card(i, j));
            }
        }

        // Shuffle deck
        Random rand = new Random(System.currentTimeMillis());
        int index1, index2;
        Card temp;
        for(int i = 0; i < SHUFFLE_COUNT; i++) {
            index1 = rand.nextInt(DECK_COUNT);
            index2 = rand.nextInt(DECK_COUNT);
            temp = deck.get(index1);
            deck.set(index1, deck.get(index2));
            deck.set(index2, temp);
        }

        return deck;
    }

    private ArrayList<Card> loadDeck(String input) {
        String[] cards = input.split(" ");
        if(cards.length != DECK_COUNT) {
            System.out.println("Expect " + DECK_COUNT + " cards (" + cards.length + ")");
            return null;
        }

        ArrayList<Card> deck = new ArrayList<>(DECK_COUNT);
        boolean[] test = new boolean[DECK_COUNT];
        int suit, face;
        String currString = "";
        try {
            for(String s: cards) {
                currString = s;
                suit = Integer.parseInt(s.substring(0, 1));
                face = Integer.parseInt(s.substring(1));
                Card newCard = new Card(suit, face);

                // Test repeated card
                if(test[suit*SUIT_COUNT+face]) {
                    System.out.println("Repeated card " + newCard);
                    return null;
                }

                deck.add(newCard);
                test[suit*SUIT_COUNT+face] = true;
            }
        } catch(Exception ex) {
            System.out.println("Wrong format at " + currString);
        }

        return deck;
    }

    protected String getDeckString() {
        StringBuffer buff = new StringBuffer();

        ArrayList<Card> currPile;
        for(int i = 0; i < TABLEAU_COUNT; i++) {
            currPile = tableau.get(i);
            for(Card c: currPile) {
                buff.append(c.suit);
                buff.append(c.face);
                buff.append(" ");
            }
        }

        for(Card c: stock) {
            buff.append(c.suit);
            buff.append(c.face);
            buff.append(" ");
        }

        return buff.toString();
    }

    private void printTableau() {
        for(ArrayList<Card> t: tableau) {
            System.out.println(t);
        }
    }

    private void printStock() {
        System.out.println(stock);
    }
}

class Card {
    protected int suit;
    protected int face;
    protected boolean isBlack;
    protected boolean isShown;

    public Card(int suit, int face) {
        this.suit = suit;
        this.face = face;
        this.isBlack = suit < GameDeck.BLACK;
        this.isShown = false;
    }

    public String toString() {
        return "[" + GameDeck.SUIT_SYMBOL[suit] + " " + GameDeck.FACE[face] + "]";
    }
}