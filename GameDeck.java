import java.util.*;

public class GameDeck {
    static final int SPADE = 0, CLUB = 1, HEART = 2, DIAMOND = 3;
    static final String[] SUIT_SYMBOL = {"\u2660","\u2663","\u2661","\u2662"};
    static final String[] FACE = {"A","2","3","4","5","6","7","8","9","10","J","Q","K"};
    static final int DECK_COUNT = 52, TABLEAU_COUNT = 7, FOUNDATION_COUNT = 4, TALON_COUNT = 3, ACE = 0, KING = 12;
    static final int SHUFFLE_COUNT = DECK_COUNT*2, BLACK = 2;

    protected ArrayList<ArrayList<Card>> tableau;
    protected ArrayList<Stack<Card>> foundation;
    protected LinkedList<Card> stock;

    public GameDeck() {
        // Initialize tableau
        tableau = new ArrayList<>(DECK_COUNT);
        for(int i = 0; i < TABLEAU_COUNT; i++) tableau.add(new ArrayList<>());

        // Initialize foundation
        foundation = new ArrayList<>(FOUNDATION_COUNT);
        for(int i = 0; i < FOUNDATION_COUNT; i++) foundation.add(new Stack<>());

        // Initailize stock
        stock = new LinkedList<>();

        // Generate all cards
        ArrayList<Card> deck = randomDeck();

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