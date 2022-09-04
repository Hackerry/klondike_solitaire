import random, time

SUIT = ["S", "H", "C", "D"]
DECK = [0] + ["%s%s" % (i, j) for i in range(1, 14) for j in SUIT]

def randomBoard():
    newDeck = [i for i in range(1, len(DECK))]
    random.shuffle(newDeck)
    print("New random deck:", newDeck)
    
    prev = 0
    for i in range(7):
        for k in range(i):
            newDeck[prev+k] *= -1
        prev += i+1
        newDeck.insert(prev, 0)
        prev += 1
    newDeck += [0 for _ in range(len(SUIT))]
    return newDeck

"""
Board structure:
7 tableau at the front with 0 in between deliminator,
tavern pile at the end, then foundation in the order of SUIT
negative means card is not seen
"""
def parseBoard(board):
    tab = []
    for _ in range(7):
        idx = 0
        while board[idx] != 0: idx += 1
        tab.append(board[:idx])
        board = board[idx+1:]
    return (tab, board[:-4], board[-4:])
def compactBoard(tableau, stock, foundation):
    newBoard = []
    for tab in tableau:
        newBoard += tab
        newBoard.append(0)
    newBoard += stock
    newBoard += foundation
    return newBoard
def boardHash(board):
    return hash("".join(map(str, board)))
def canStack(parent, child):
    return abs(parent % 4-child % 4) % 2 and (child-1) // 4 + 1 == (parent-1) // 4
def isKing(card):
    return (card-1) // 4 == 12
def canFound(card, foundation):
    return foundation[(card-1) % 4] == (card-1) // 4
def compactStock(stock):
    return [card for card in stock if card != 0]
def stockToTableau(tableau, tIdx, stock, sIdx, foundation, draw3):
    cardNum = 3 if draw3 else 1
    stockCard = stock[sIdx]
    if draw3:
        newStock, stock[sIdx] = stock, 0
        if sIdx % cardNum == cardNum-1: newStock = stock[:sIdx-cardNum+1] + stock[sIdx+1:]
        elif sIdx == len(stock)-1: newStock = stock[:-1]
    else:
        newStock = stock
        stock.pop(sIdx)
    tableau[tIdx].append(stockCard)
    board = compactBoard(tableau, newStock, foundation)
    tableau[tIdx].pop()
    if draw3: stock[sIdx] = stockCard
    else: stock.insert(sIdx, stockCard)
    return board, boardHash(board)
def tableauToFoundation(tableau, idx, stock, foundation):
    card = tableau[idx].pop()
    cIdx = (card-1) % 4
    foundation[cIdx] += 1
    revealed = False
    if tableau[idx] and tableau[idx][-1] < 0:
        tableau[idx][-1] *= -1
        revealed = True
    board = compactBoard(tableau, stock, foundation)
    if revealed: tableau[idx][-1] *= -1
    foundation[cIdx] -= 1
    tableau[idx].append(card)
    return board, boardHash(board)
def stockToFoundation(tableau, stock, sIdx, foundation, draw3):
    cardNum = 3 if draw3 else 1
    stockCard = stock[sIdx]
    if draw3:
        newStock, stock[sIdx] = stock, 0
        if sIdx % cardNum == cardNum-1: newStock = stock[:sIdx-cardNum+1] + stock[sIdx+1:]
        elif sIdx == len(stock)-1: newStock = stock[:-1]
    else:
        newStock = stock
        stock.pop(sIdx)
    cIdx = (stockCard-1) % 4
    foundation[cIdx] += 1
    board = compactBoard(tableau, newStock, foundation)
    foundation[cIdx] -= 1
    if draw3: stock[sIdx] = stockCard
    else: stock.insert(sIdx, stockCard)
    return board, boardHash(board)
def tableauToTableau(tableau, iFrom, iCard, iTo, stock, foundation):
    moved = tableau[iFrom][iCard:]
    tableau[iFrom] = tableau[iFrom][:iCard]
    toTableauL = len(tableau[iTo])
    tableau[iTo] += moved
    revealed = False
    if tableau[iFrom] and tableau[iFrom][-1] < 0:
        tableau[iFrom][-1] *= -1
        revealed = True
    board = compactBoard(tableau, stock, foundation)
    if revealed: tableau[iFrom][-1] *= -1
    tableau[iFrom] += moved
    tableau[iTo] = tableau[iTo][:toTableauL]
    return board, boardHash(board)
def printBoard(board, readable=True):
    tableau, stock, foundation = parseBoard(board)
    for i in range(7):
        print("%s: %s" % (str(i), list(map(lambda x: DECK[x] if x > 0 else "-" + DECK[abs(x)], tableau[i])) if readable else tableau[i]))
    print("Stock:", list(map(lambda x: DECK[x], stock)) if readable else stock)
    print("Foundation:", [SUIT[i] + ": " + str(f) for i, f in enumerate(foundation)])

def solve(board, draw3=False, output=False):
    print("Board String", board)
    print("Board Array", [DECK[abs(c)] for c in board if c != 0])
    # 1. Move from stock to tableau (K to empty and one card to a pile)
    # 1a. Move from stock to foundation
    # 2. Move from tableau to tableau (K to empty and one pile to another)
    # 3. Move from tableau to foundation (increasing number)
    
    # Search
    bHash = boardHash(board)
    seen, frontier = {bHash: (board, None, "Start.")}, [bHash]
    stockL = float('inf')
    startTime = time.perf_counter()
    solution = []
    numStates = 0
    
    if output: print("Stock length: ", end="")
    while frontier:
        # Get next state
        currBoardHash = frontier.pop()
        currBoard = seen[currBoardHash][0]
        numStates += 1
        
        # Split cards & setup
        tableau, stock, foundation = parseBoard(currBoard)
        if stockL > len(stock):
            stockL = len(stock)
            if output: print("SL", stockL, end=" ")
        # print(tableau)
        # print(stock)
        # print(foundation)
        
        # Check win
        if all([f == 13 for f in foundation]):
            # Short circuit & faster end condition:
            # or len(stock) == 0 and all([all([c > 0 for c in tab]) for tab in tableau]):
            
            # Reconstruct solution
            solution.append(currBoardHash)
            currHash = seen[currBoardHash][1]
            while currHash:
                solution.append(currHash)
                currHash = seen[currHash][1]
            if output: print("\n")
            if output: print("Solution found with length %s" % len(solution))
            break
        
        # Step 1 - move from stack to tableau
        cardNum = 3 if draw3 else 1

        if draw3:
            # Choose from compacted stock
            compactedStock = compactStock(stock)
            options = [(i, compactedStock) for i in range(len(compactedStock)) if i % cardNum == 0]
            
            # Choose from the rest after a move
            if 0 in stock: lastZero = (len(stock)-1-stock[::-1].index(0)) // cardNum * cardNum
            else: lastZero = len(stock)
            for s in range(lastZero, len(stock), cardNum):
                for k in range(min(cardNum, len(stock)-s)):
                    if s+k <= lastZero or stock[s+k] == 0: continue
                    options.append((s+k, stock))
                    break
        # Draw single card
        else: options = [(i, stock) for i in range(len(stock))]
        
        for sIdx, currStock in options:
            stockCard = currStock[sIdx]
            for i, tab in enumerate(tableau):
                # Can move to this tableau
                if tab and canStack(tab[-1], stockCard) or not tab and isKing(stockCard):
                    # print("Can move: Stock(%s) to Tableau(%s)" % (DECK[stockCard], i))
                    board, bHash = stockToTableau(tableau, i, currStock, sIdx, foundation, draw3)
                    if bHash not in seen:
                        seen[bHash] = (board, currBoardHash, "M:S(%s)T(%s)" % (DECK[stockCard], i))
                        frontier.append(bHash)
                        
                        # If card can be moved to any tableau, continue to next card
                        break
                        
                        # printBoard(board)
                        # printBoard(board, False)
        
        # Step 2 - move between tableau
        for i in range(len(tableau)):
            for c in range(len(tableau[i])-1, -1, -1):
                if tableau[i][c] < 0: continue
                for j in range(len(tableau)):
                    if i == j: continue
                    
                    if tableau[j] and canStack(tableau[j][-1], tableau[i][c]) or not tableau[j] and isKing(tableau[i][c]):
                        # print("Can move: Tableau(%s-%s) to Tableau(%s)" % (i, DECK[tableau[i][c]], j))
                        board, bHash = tableauToTableau(tableau, i, c, j, stock, foundation)
                        if bHash not in seen:
                            seen[bHash] = (board, currBoardHash, "M:T(%s-%s)T(%s)" % (i, DECK[tableau[i][c]], j))
                            
                            # Move between tableau
                            if tableau[j]:
                                if c-1 >= 0 and (tableau[i][c-1] < 0 or canFound(tableau[i][c-1], foundation)) or c == 0:
                                    # Heuristic:
                                    # only explore moves that reveal a new card,
                                    # empty a tableau or
                                    # move a stacked card to foundation
                                    frontier.append(bHash)
                            # Move to empty tableau
                            elif c > 0:
                                # Reveal a card
                                frontier.append(bHash)
                                # Move K around - don't even consider, unnecessary
                            
                            # printBoard(board)
                            # printBoard(board, False)
        
        # Step 1a - move stock to foundation
        for sIdx, currStock in options:
            stockCard = currStock[sIdx]
            if canFound(stockCard, foundation):
                # print("Can found:", DECK[stockCard])
                board, bHash = stockToFoundation(tableau, currStock, sIdx, foundation, draw3)
                if bHash not in seen:
                    seen[bHash] = (board, currBoardHash, "F:S(%s)" % DECK[stockCard])
                    frontier.append(bHash)
                    # printBoard(board)
                    # printBoard(board, False)
        
        # Step 3 - move tableau to foundation
        for i, tab in enumerate(tableau):
            if tab and canFound(tab[-1], foundation):
                # print("Can found:", DECK[tab[-1]])
                board, bHash = tableauToFoundation(tableau, i, stock, foundation)
                if bHash not in seen:
                    seen[bHash] = (board, currBoardHash, "F:T(%s)" % DECK[tab[-1]])
                    frontier.append(bHash)
                    # printBoard(board)
                    # printBoard(board, False)
                    
        # print(list(map(printBoard, seen.values())))

    # Print performance values
    duration = time.perf_counter()-startTime
    if output: print("Searched %s states in %ss" % (numStates, duration))
    
    if solution:
        if output:
            for i, b in enumerate(reversed(solution)):
                print("================== Step %s ==================" % i)
                print("Move:", seen[b][2])
                printBoard(seen[b][0])
        return (True, len(solution), numStates, duration)
    return (False, 0, numStates, duration)


# # # # # # # # #
# To construct a board to be solved:
# [tableau 1] + [0] + [tableau 2] + [0] + [...] + [tableau 7] + [0] + [stock] + [0, 0, 0, 0]
#                                                                               ^ Foundation
# Note: Unrevealed cards are marked as negative
#
# Cards are marked with id (1-52 inclusive):
# 1: Ace Spade      2: Ace Heart    3: Ace Club     4: Ace Diamond
# 5: 1 of Spade     6: 1 of Heart   7: 1 of Club    8: 1 of Diamond
# 9: 2 of Spade ...
# (Total 52 cards)
#
# Foundations store the value of the topmost card (default 0)
# The order is [Spade, Heart, Club, Diamond]
#
# How to read steps:
# Operations:                       Locations:
# F - move a card to foundation     S - stock
# M - move a card                   F - tableau
#
# Eg:
# M:T(1-3S)T(2) - Move 3 of spade from tableau 1 (0-indexed) to tableau 2
# F:T(2D) - Move 2 of diamond from tableau to foundation
# F:S(5D) - Move 5 of diamond from stock to foundation
#
# With deal 3 mode, 0 in stock represents a card that has been moved away from this position
# If 3 consecutive 0s are discovered, they are removed in the next search step.
# # # # # # # # #
    

# board = [15, 0, -25, 4, 0, -35, -14, 26, 0, -41, -20, -27, 18, 0, -49, -46, -32, -52, 22, 0, -6, -38, -28, -51, -19, 2, 0, -39, -45, -12, -42, -29, -33, 3, 0, 36, 21, 48, 17, 5, 43, 11, 9, 44, 47, 31, 40, 50, 8, 23, 37, 13, 30, 24, 34, 7, 16, 10, 1, 0, 0, 0, 0]
# unsolvable = 6, 0, -29, 25, 0, -13, -50, 48, 0, -16, -22, -41, 19, 0, -1, -2, -40, -26, 49, 0, -27, -4, -35, -30, -28, 32, 0, -11, -8, -38, -52, -47, -15, 36, 0, 18, 14, 33, 20, 24, 43, 51, 9, 44, 23, 34, 5, 46, 21, 39, 10, 7, 42, 17, 45, 12, 37, 31, 3, 0, 0, 0, 0
# sortedBoard = [1, 0, -8, 2, 0, -14, -9, 3, 0, -19, -15, -10, 4, 0, -23, -20, -16, -11, 5, 0, -26, -24, -21, -17, -12, 6, 0, -28, -27, -25, -22, -18, -13, 7, 0, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 0, 0, 0, 0]
# testBoard = [23, 0, -29, 48, 0, -10, -43, 14, 0, -41, -45, -13, 31, 0, -7, -4, -38, -16, 37, 0, -12, -39, -5, -19, -50, 11, 0, -35, -1, -28, -49, -15, -44, 9, 0, 40, 24, 3, 46, 51, 2, 22, 47, 21, 34, 6, 36, 20, 26, 52, 17, 27, 25, 30, 33, 42, 8, 32, 18, 0, 0, 0, 0]
for i in range(1):
    board = randomBoard()
    print("Solving board...")
    printBoard(board)
    print("\n")
    print(solve(board, draw3=True))


####
# Example results:
# Solution  steps   states    time in seconds
# (True,    94,     302,        0.04274129983969033)
# (True,    93,     158,        0.02568230009637773)
# (True,    101,    1641,       0.284684999845922)
# TLE
# (True,    89,     186,        0.023428899934515357)
# (True,    80,     677049,     110.62624579994008)
# (True,    89,     353580,     49.42586179985665)
# (True,    92,     92,         0.01333330012857914)
# TLE
# TLE
# (True,    77,     87,         0.010809200117364526)
# (True,    73,     95,         0.009563999949023128)
# (True,    90,     90,         0.012440500082448125)
###