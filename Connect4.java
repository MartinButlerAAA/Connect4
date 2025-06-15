/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Connect 4 an example of a Neural Net
// 
// Martin Butler 14/03/2024
//
// See report for further details.
// See Readme.txt for details about code and build.
//
// This is a console program that uses a form of neural network to calculate the computer's move. A database approach had been tried, but failed.
// There are two main options: 
//     1 to play the computer. 
//     Any other input will cause the computer to play itself to trial different weightings for the neural net.

// There is a command line option to support use by other software.
//
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import java.awt.*;			// General Java
import java.io.*;			// Input and output for keyboard input
import java.util.Random;	// A pseudo random number stream to support a random element to computer move.
import java.lang.Math.*;	// Math library to support raising a number to a power. x^1, x^2, x^3 so that 3 in a line has a much higher weighting.

// The game is all contained in one class.
public class Connect4 {
	private static Random myRandom = new Random();						// Set up a pseudo random number chain.

	private static char[][]   gameTable = new char[7][6]; 				// Standard Connect 4 game board with 7 rows and 6 columns.
	private static int[][][]  winningColumns = new int[7][3][2];		// Array of possible vertical winning groups of 4, with counts of 'R' red and 'Y' pieces.
	private static int[][][]  winningRows = new int[4][6][2];			// Array of possible horizontal winning groups of 4, with counts of 'R' red and 'Y' pieces.
	private static int[][][]  winningDiagonalsUp = new int[4][3][2];	// Array of possible diagonal up/right winning groups of 4, with counts of 'R' red and 'Y' pieces.
	private static int[][][]  winningDiagonalsDown = new int[4][3][2];	// Array of possible diagonal down/right winning groups of 4, with counts of 'R' red and 'Y' pieces.
	private static double[][] combinedScoresR = new double[7][6];		// Array used to combine vertical, horizontal and diagonal scores for each board position for red.
	private static double[][] combinedScoresY = new double[7][6];		// Array used to combine vertical, horizontal and diagonal scores for each board position for yellow.

	// The default values below have been selected after optimisation.
	private static double 	PIECESDEFAULT = 6.0; 						// (x from documentation) Now not optimised	
	private static double	HORIZONTALDEFAULT = 1.0;					// (h from documentation) 
	private static double	VERTICALDEFAULT = 1.0;						// (v documentation)
	private static double	DIAGONALDEFAULT = 1.0;						// (d from documentation)
	private static double	OPPNTMOVEDEFAULT = 0.5; 					// (o from documentation)
	private static double	NEXTMOVEDEFAULT = 0.5;						// (n from documentation)
	
	// Weightings used in neural network calculations. These are optimised by lthe computer playing against itself.
	private static double 	piecesWeight = PIECESDEFAULT;				// Number which is raised to the count of pieces 1, 2 or 3, so for 4.0 this would give 64 for 3 pieces.
	private static double 	horizontalWeight = HORIZONTALDEFAULT; 		// Horizontal scores are multiplied by this weighting.
	private static double 	verticalWeight = VERTICALDEFAULT;			// Weighting for verticals.
	private static double 	diagonalWeight = DIAGONALDEFAULT;			// Weighting for diagonals.
	private static double	oppntMoveWeight = OPPNTMOVEDEFAULT;			// 
	private static double 	nextMoveWeight = NEXTMOVEDEFAULT;			// Weighting to multiply the score for the following move, before it is subtracted from the score for this move.

	// Trial weightings used for optimising neural network.
	private static double 	piecesNew = PIECESDEFAULT;
	private static double 	horizontalNew = HORIZONTALDEFAULT; 		
	private static double 	verticalNew = VERTICALDEFAULT;			
	private static double 	diagonalNew = DIAGONALDEFAULT;			
	private static double	oppntMoveNew = OPPNTMOVEDEFAULT;
	private static double 	nextMoveNew = NEXTMOVEDEFAULT;			
	
	// The main program always starts at main. This just runs Connect 4 if there is no command line argument.
	public static void main(String[] args) {
		// If there is no command line argument then run the game
		if (args.length == 0) {			
			new Connect4("");
		}
		// Otherwise pass the command line to the game to process.
		else {
			new Connect4(args[0]);
		}
	}

	// The connect 4 start program. This can either run the game for a player or run optimisation.
	// It also has the option to process a game board passed in via the command line argument.
	Connect4(String cmdLine) {
		int option = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String inputSt;
		int move = 0;

		// Run the game
		if (cmdLine == "") {
			playConnect4();
		}
		// If the command line is 'O' run the optimisation.
		else if (cmdLine.charAt(0) == 'O') {
			optimiseWeightings();
		}
	}

	// Loop to keep playing connect 4 alternating human and computer moves, checking for game end and keeping score.
	// On alternate goes the human or the computer get to start.
	// Note that human always places R red and computer Y yellow.
	private static void playConnect4() {
		char winner = ' ';
		int Ywin = 0;	// Counts of game wins and draws
		int Rwin = 0;
		int Draw = 0;
		
		// This could be a higher number, but it is unlikely that a human would have 10000 games in a row.
		for (int z = 0;z < 10000; z++) {
			// Human goes first
			if (z%2 == 0) {
				clearGameTable();
				for(;;) {
					displayBoard();
					humanMove();
					winner = gameEnded();
					if (winner != ' ') {	// This construct just exits the loop if the game has ended
						break;
					}				
					displayBoard();
					calculateMove('Y', 2); // computer plays yellow with current weightings
					winner = gameEnded();
					if (winner != ' ') {
						break;
					}				
				}
			}
			// Computer goes first
			else {
				clearGameTable();
				for(;;) {
					displayBoard();
					calculateMove('Y', 2);
					winner = gameEnded();
					if (winner != ' ') {
						break;
					}				
					displayBoard();
					humanMove();
					winner = gameEnded();
					if (winner != ' ') {
						break;
					}				
				}
			}
			displayBoard();	// Display the board again to show the winning move.
			//Update and display the winning counts.
			if (winner == 'Y') { Ywin++; }
			if (winner == 'R') { Rwin++; }
			if (winner == 'D') { Draw++; }
			System.out.println("Winner " + winner + "   R-wins " + Rwin + " Y-wins " + Ywin + " Draws " + Draw);				
		}
	}
	
	// Optimisation is done by playing the current weightings against the default weightings and then the new weightings. If the new weightings win more games, the new weightings are adopted.
	private static void optimiseWeightings() {
		char winner = ' ';
		// Winning counts used to determine if the weightings are an improvement.
		int Ywin = 0;
		int Rwin = 0;
		int Draw = 0;
		
		for (int a = 0; a < 10000; a++)
		{
			piecesNew = PIECESDEFAULT; //myRandom.nextDouble()     * 7.0 + 1.01; // 1.0 to 8.0
			horizontalNew = myRandom.nextDouble() * 1.5 + 0.51; // 0.5 to 2.0
			verticalNew = myRandom.nextDouble()   * 1.5 + 0.51; // 0.5 to 2.0
			diagonalNew = DIAGONALDEFAULT; // myRandom.nextDouble()   * 1.5 + 0.51; // 0.5 to 2.0
			nextMoveNew = OPPNTMOVEDEFAULT; // myRandom.nextDouble()   * 1.5 + 0.51; // 0.5 to 2.0			
			oppntMoveNew = NEXTMOVEDEFAULT; // myRandom.nextDouble()   * 1.5 + 0.51; // 0.5 to 2.0			

			// The counts are cleared before each run of games.
			Ywin = 0;
			Rwin = 0;
			Draw = 0;
			// Play games with players alternately playing first.
			// Red plays the default weightings, Yellow plays the new ones.
			// Not many games are played as the games are likely to be similar.
			for (int z = 0;z < 14; z++) {
				if (z%2 == 1) {
					gameTable[z%7][0] = 'Y';
					clearGameTable();
					for(;;) {
						calculateMove('R', 1); // Default weightings
						winner = gameEnded();
						if (winner != ' ') {
							break;
						}				
						calculateMove('Y', 3); // Trial weightings
						winner = gameEnded();
						if (winner != ' ') {
							break;
						}				
					}
				}
				else {
					clearGameTable();
					gameTable[z%7][0] = 'R';
					for(;;) {
						calculateMove('Y', 3);
						winner = gameEnded();
						if (winner != ' ') {
							break;
						}				
						calculateMove('R', 1);
						winner = gameEnded();
						if (winner != ' ') {
							break;
						}				
					}
				}
				if (winner == 'Y') { Ywin++; }
				if (winner == 'R') { Rwin++; }
				if (winner == 'D') { Draw++; }
			}
			
			// If the new weightings won more games than the default weightings, try playing the current weightings.
			// Only bother if there is a noticeable difference. The random effect allows for some change anyway.
			if (Ywin > (Rwin + 2)) {
				// The counts are again cleared.
				Ywin = 0;
				Rwin = 0;
				Draw = 0;
				// Play games with players alternately playing first.
				// Red plays current weightings, yellow plays the new ones.
				// Red first
				for (int z = 0;z < 14; z++) {
					if (z%2 == 1) {
						clearGameTable();
						gameTable[z%7][0] = 'Y';
						for(;;) {
							calculateMove('R', 2); // current weightings
							winner = gameEnded();
							if (winner != ' ') {
								break;
							}				
							calculateMove('Y', 3); // Trial weightings
							winner = gameEnded();
							if (winner != ' ') {
								break;
							}				
						}
					}
					// Yellow first
					else {
						clearGameTable();
						gameTable[z%7][0] = 'R';
						for(;;) {
							calculateMove('Y', 3);
							winner = gameEnded();
							if (winner != ' ') {
								break;
							}				
							calculateMove('R', 2);
							winner = gameEnded();
							if (winner != ' ') {
								break;
							}				
						}
					}
					if (winner == 'Y') { Ywin++; }
					if (winner == 'R') { Rwin++; }
					if (winner == 'D') { Draw++; }
				}
		
				// If the new settings won more use them.
				// Only bother if there is a noticeable difference. The random effect allows for some change anyway.
				if (Ywin > (Rwin + 2)) {
					piecesWeight = piecesNew;
					horizontalWeight = horizontalNew; 		
					verticalWeight = verticalNew;			
					diagonalWeight = diagonalNew;			
					nextMoveWeight = nextMoveNew;
					oppntMoveWeight = oppntMoveNew;
					System.out.println(a + " R " + Rwin + " Y " + Ywin + " Pieces " + (float)((int)(piecesWeight*10)/10.0) + " Horizontal " + (float)((int)(horizontalWeight*10)/10.0) + " Vertical " + (float)((int)(verticalWeight*10)/10.0) + " Diagonal " + (float)((int)(diagonalWeight*10)/10.0) + " OppntMove " + (float)((int)(oppntMoveWeight*10)/10.0) + " NextMove " + (float)((int)(nextMoveWeight*10)/10.0));
				}
			}
		}
	}
	
	// This is a small function to clear all places in the game table ready for a new game.
	private static void clearGameTable() {
		for (int x = 0; x < 7; x++) {
			for (int y = 0; y < 6; y++) {
				gameTable[x][y] = ' ';
			}
		}
		return;
	}

	// This function prompts the human player for their move. If the value they enter does not correspond to a column with space to play, they are prompted again.
	private static void humanMove() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String inputSt;
		int move = 0;
		for (;;) {
			System.out.println("Select the column (1 to 7)");
			// try catch must be used. As a string is entered, there is no chance of an exception.
			try {
				inputSt = br.readLine();
				move = Integer.parseInt(inputSt);
			}
				catch (Exception e) {
			}
			// Only if the move is inside the game table and the column has space is the move used and the function exited.
			if ((move >= 1) && (move <= 7)) {
				for (int  y = 0; y < 6; y++) {
					if (gameTable[move - 1][y] == ' ') {
						gameTable[move - 1][y] = 'R';
						return;
					}
				}
			}
		}
	}	

	// This function looks through all of the possible winning lines of 4 to see if there is a winner. If there is a winner R is returned for red or Y for yellow.
	// If the top row of the board is full the game is over and it was a draw, otherwise a space is returned.
	private static char gameEnded() {
		// Check for vertical winning lines.
		for (int x = 0; x < 7; x++) {
			for (int y = 0; y < 3; y++) {
				if ((gameTable[x][y] == 'R') && (gameTable[x][y+1] == 'R') && (gameTable[x][y+2] == 'R') && (gameTable[x][y+3] == 'R')) {
					return 'R';
				}
				if ((gameTable[x][y] == 'Y') && (gameTable[x][y+1] == 'Y') && (gameTable[x][y+2] == 'Y') && (gameTable[x][y+3] == 'Y')) {
					return 'Y';
				}
			}
		}
		// Check for horizontal winning lines.
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 6; y++) {
				if ((gameTable[x][y] == 'R') && (gameTable[x+1][y] == 'R') && (gameTable[x+2][y] == 'R') && (gameTable[x+3][y] == 'R')) {
					return 'R';
				}
				if ((gameTable[x][y] == 'Y') && (gameTable[x+1][y] == 'Y') && (gameTable[x+2][y] == 'Y') && (gameTable[x+3][y] == 'Y')) {
					return 'Y';
				}
			}
		}
		// Check for diagonal up winning lines.
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 3; y++) {
				if ((gameTable[x][y] == 'R') && (gameTable[x+1][y+1] == 'R') && (gameTable[x+2][y+2] == 'R') && (gameTable[x+3][y+3] == 'R')) {
					return 'R';
				}
				if ((gameTable[x][y] == 'Y') && (gameTable[x+1][y+1] == 'Y') && (gameTable[x+2][y+2] == 'Y') && (gameTable[x+3][y+3] == 'Y')) {
					return 'Y';
				}
			}
		}
		// Check for diagonal down winning lines.
		for (int x = 0; x < 4; x++) {
			for (int y = 3; y < 6; y++) {
				if ((gameTable[x][y] == 'R') && (gameTable[x+1][y-1] == 'R') && (gameTable[x+2][y-2] == 'R') && (gameTable[x+3][y-3] == 'R')) {
					return 'R';
				}
				if ((gameTable[x][y] == 'Y') && (gameTable[x+1][y-1] == 'Y') && (gameTable[x+2][y-2] == 'Y') && (gameTable[x+3][y-3] == 'Y')) {
					return 'Y';
				}
			}
		}
		// Check if the top row is full, showing the board is full and therefore the end of the game.
		if ((gameTable[0][5] != ' ')&&(gameTable[1][5] != ' ')&&(gameTable[2][5] != ' ')&&(gameTable[3][5] != ' ')&&(gameTable[4][5] != ' ')&&(gameTable[5][5] != ' ')&&(gameTable[6][5] != ' ')) {
			return 'D';
		}

		// If not over return a blank.
		return ' ';
	}
		
	// function to display the board.
	private static void displayBoard() {
		System.out.println("");
		System.out.println("-----------------------------");
		System.out.println("| " + gameTable[0][5] + " | " + gameTable[1][5] + " | " + gameTable[2][5] + " | " + gameTable[3][5] + " | " + gameTable[4][5] + " | " + gameTable[5][5] + " | " + gameTable[6][5] + " | ");
		System.out.println("| " + gameTable[0][4] + " | " + gameTable[1][4] + " | " + gameTable[2][4] + " | " + gameTable[3][4] + " | " + gameTable[4][4] + " | " + gameTable[5][4] + " | " + gameTable[6][4] + " | ");
		System.out.println("| " + gameTable[0][3] + " | " + gameTable[1][3] + " | " + gameTable[2][3] + " | " + gameTable[3][3] + " | " + gameTable[4][3] + " | " + gameTable[5][3] + " | " + gameTable[6][3] + " | ");
		System.out.println("| " + gameTable[0][2] + " | " + gameTable[1][2] + " | " + gameTable[2][2] + " | " + gameTable[3][2] + " | " + gameTable[4][2] + " | " + gameTable[5][2] + " | " + gameTable[6][2] + " | ");
		System.out.println("| " + gameTable[0][1] + " | " + gameTable[1][1] + " | " + gameTable[2][1] + " | " + gameTable[3][1] + " | " + gameTable[4][1] + " | " + gameTable[5][1] + " | " + gameTable[6][1] + " | ");
		System.out.println("| " + gameTable[0][0] + " | " + gameTable[1][0] + " | " + gameTable[2][0] + " | " + gameTable[3][0] + " | " + gameTable[4][0] + " | " + gameTable[5][0] + " | " + gameTable[6][0] + " | ");
		System.out.println("-----------------------------");
		System.out.println("");
		return;
	}

	// function to work out the computer move. This calls different functions to calculate the neural network. 
	// Then puts the computer move into the next available space in the chosen column.
	private static int calculateMove(char Player, int weights) {	// The player selects R for red or Y for Yellow. The weights is 1 default, 2 current or 3 new
		int move = 0;
		doWinningColumns();					// 21 neurons
		doWinningRows();					// 24 neurons
		doWinningDiagonalsUp();				// 12 neurons
		doWinningDiagonalsDown();			// 12 neurons
		doCombinedScores(weights);			// The selection of which weightings to use must be passed to the function.
		move = selectMove(Player, weights); // The move selected depends on the player viewpoint as well as weightings.
		// put the player move into the game table.
		for (int  y = 0; y < 6; y++) {
			if (gameTable[move][y] == ' ') {
				gameTable[move][y] = Player;
				// The move is returned to support the command line option.
				break;
			}
		}
		return(move);
	}

	// function to look at the combined scores for each column and select the highest as the computer move.
	// 8 neurons
	private static int selectMove(char Player, int weights) {	// The player selects R for red or Y for Yellow. The weights is 1 default, 2 current or 3 new
		double 	nextMoveLocal 	= NEXTMOVEDEFAULT;
		double  oppntMoveLocal	= OPPNTMOVEDEFAULT;
		double possibles[] = new double[7]; 		// array of scores for each column to select move.
		double highest = -1.0; 						// variable to select the highest to identify the column.
		int move = 3; 								// The move is set to default to the middle of the table.

		// Select the weighting to be used.
		if (weights == 2) { 
			nextMoveLocal 	= nextMoveWeight; 
			oppntMoveLocal	= oppntMoveWeight;
		}
		if (weights == 3) { 
			nextMoveLocal 	= nextMoveNew; 
			oppntMoveLocal	= oppntMoveNew;
		}			

		// Go through all columns to calculate the score for each one to select the move.
		for (int x = 0; x < 7; x++) {
			// The possibles array isn't strictly needed, but helps with debugging and development.
			possibles[x] = 0.0;	
			// Go through the column to find the next free place.
			for (int y = 0; y < 6; y++) {
				if (gameTable[x][y] == ' ') { 
					// Calculate the score for the column by adding the red and yellow scores.
					// It is a good idea to block a position, if it is a good move for the opponent.
					if (Player == 'Y') {
						possibles[x] = combinedScoresY[x][y] + (combinedScoresR[x][y] * oppntMoveLocal);
					}
					else {
						possibles[x] = combinedScoresR[x][y] + (combinedScoresY[x][y] * oppntMoveLocal);
					}
					// check if column at top before looking at next move.
					if (y < 5) {
					// If there is a space above, subtract the opponents score from the score calculated.
					// This is to reduce the score if the move lets the opponent get an advantage or win.
					// The next move score is adjusted by the weighting.
						if (Player == 'Y') {
							possibles[x] = possibles[x]  - (combinedScoresR[x][y+1] * nextMoveLocal);
						}
						else {
							possibles[x] = possibles[x]  - (combinedScoresY[x][y+1] * nextMoveLocal);
						}
					}
					// check each possible value to see if it is the highest, then capture it and the corresponding move. 
					if (possibles[x] > highest) {
						highest = possibles[x];
						move = x;
					}
					y = 6; // exit the y loop, once the column has been processed.
				}
			}
		}
		// Just in case something has gone wrong, the move is checked to see that there is space for it to fit. 
		// If there isn't, the table is searched for any valid move.
		// Coding errors in the neural network meant this happened in the past.
		if (gameTable[move][5] != ' ') { 
			for (int x = 0; x < 7; x++) {
				if (gameTable[x][5] == ' ') {
					move = x;
					break;
				}
			}
		}
		return (move);
	}

	// This function populates the array of piece counts for each players columns.
	// 21 neurons
	private static void doWinningColumns() {		
		for (int x = 0; x < 7; x++) {
			for (int y = 0; y < 3; y++) {
				winningColumns[x][y][0] = 0; 
				if (gameTable[x][y] == 'R')   { winningColumns[x][y][0] = winningColumns[x][y][0] + 1; }
				if (gameTable[x][y+1] == 'R') { winningColumns[x][y][0] = winningColumns[x][y][0] + 1; }
				if (gameTable[x][y+2] == 'R') { winningColumns[x][y][0] = winningColumns[x][y][0] + 1; }
				if (gameTable[x][y+3] == 'R') { winningColumns[x][y][0] = winningColumns[x][y][0] + 1; }

				winningColumns[x][y][1] = 0; 
				if (gameTable[x][y] == 'Y')   { winningColumns[x][y][1] = winningColumns[x][y][1] + 1; }
				if (gameTable[x][y+1] == 'Y') { winningColumns[x][y][1] = winningColumns[x][y][1] + 1; }
				if (gameTable[x][y+2] == 'Y') { winningColumns[x][y][1] = winningColumns[x][y][1] + 1; }
				if (gameTable[x][y+3] == 'Y') { winningColumns[x][y][1] = winningColumns[x][y][1] + 1; }
				
				// If both players have pieces, the line is blocked and no longer part of the game, so set the scores to zero.
				if ((winningColumns[x][y][0] != 0)&&(winningColumns[x][y][1] != 0)) {
					winningColumns[x][y][0] = 0;
					winningColumns[x][y][1] = 0;
				}
			}
		}		
		return;
	}

	// This function populates the array of piece counts for each players rows.
	// 24 neurons
	private static void doWinningRows() {
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 6; y++) {
				winningRows[x][y][0] = 0; 
				if (gameTable[x][y] == 'R')   { winningRows[x][y][0] = winningRows[x][y][0] + 1; }
				if (gameTable[x+1][y] == 'R') { winningRows[x][y][0] = winningRows[x][y][0] + 1; }
				if (gameTable[x+2][y] == 'R') { winningRows[x][y][0] = winningRows[x][y][0] + 1; }
				if (gameTable[x+3][y] == 'R') { winningRows[x][y][0] = winningRows[x][y][0] + 1; }

				winningRows[x][y][1] = 0; 
				if (gameTable[x][y] == 'Y')   { winningRows[x][y][1] = winningRows[x][y][1] + 1; }
				if (gameTable[x+1][y] == 'Y') { winningRows[x][y][1] = winningRows[x][y][1] + 1; }
				if (gameTable[x+2][y] == 'Y') { winningRows[x][y][1] = winningRows[x][y][1] + 1; }
				if (gameTable[x+3][y] == 'Y') { winningRows[x][y][1] = winningRows[x][y][1] + 1; }

				// If both players have pieces, the line is blocked and no longer part of the game, so set the scores to zero.
				if ((winningRows[x][y][0] != 0)&&(winningRows[x][y][1] != 0)) {
					winningRows[x][y][0] = 0;
					winningRows[x][y][1] = 0;
				}
			}
		}
		return;
	}
	
	// This function populates the array of piece counts for each players diagonals going up/right.
	// 12 neurons
	private static void doWinningDiagonalsUp() {
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 3; y++) {
				winningDiagonalsUp[x][y][0] = 0; 
				if (gameTable[x][y] == 'R')     { winningDiagonalsUp[x][y][0] = winningDiagonalsUp[x][y][0] + 1; }
				if (gameTable[x+1][y+1] == 'R') { winningDiagonalsUp[x][y][0] = winningDiagonalsUp[x][y][0] + 1; }
				if (gameTable[x+2][y+2] == 'R') { winningDiagonalsUp[x][y][0] = winningDiagonalsUp[x][y][0] + 1; }
				if (gameTable[x+3][y+3] == 'R') { winningDiagonalsUp[x][y][0] = winningDiagonalsUp[x][y][0] + 1; }
	
				winningDiagonalsUp[x][y][1] = 0; 
				if (gameTable[x][y] == 'Y')     { winningDiagonalsUp[x][y][1] = winningDiagonalsUp[x][y][1] + 1; }
				if (gameTable[x+1][y+1] == 'Y') { winningDiagonalsUp[x][y][1] = winningDiagonalsUp[x][y][1] + 1; }
				if (gameTable[x+2][y+2] == 'Y') { winningDiagonalsUp[x][y][1] = winningDiagonalsUp[x][y][1] + 1; }
				if (gameTable[x+3][y+3] == 'Y') { winningDiagonalsUp[x][y][1] = winningDiagonalsUp[x][y][1] + 1; }

				// If both players have pieces, the line is blocked and no longer part of the game, so set the scores to zero.
				if ((winningDiagonalsUp[x][y][0] != 0)&&(winningDiagonalsUp[x][y][1] != 0)) {
					winningDiagonalsUp[x][y][0] = 0;
					winningDiagonalsUp[x][y][1] = 0;
				}
			}
		}
		return;
	}
	
	// This function populates the array of piece counts for each players diagonals going down/right.
	// 12 neurons
	private static void doWinningDiagonalsDown() {
		for (int x = 0; x < 4; x++) {
			for (int y = 3; y < 6; y++) {
				winningDiagonalsDown[x][y-3][0] = 0; 
				if (gameTable[x][y] == 'R')     { winningDiagonalsDown[x][y-3][0] = winningDiagonalsDown[x][y-3][0] + 1; }
				if (gameTable[x+1][y-1] == 'R') { winningDiagonalsDown[x][y-3][0] = winningDiagonalsDown[x][y-3][0] + 1; }
				if (gameTable[x+2][y-2] == 'R') { winningDiagonalsDown[x][y-3][0] = winningDiagonalsDown[x][y-3][0] + 1; }
				if (gameTable[x+3][y-3] == 'R') { winningDiagonalsDown[x][y-3][0] = winningDiagonalsDown[x][y-3][0] + 1; }
	
				winningDiagonalsDown[x][y-3][1] = 0; 
				if (gameTable[x][y] == 'Y')     { winningDiagonalsDown[x][y-3][1] = winningDiagonalsDown[x][y-3][1] + 1; }
				if (gameTable[x+1][y-1] == 'Y') { winningDiagonalsDown[x][y-3][1] = winningDiagonalsDown[x][y-3][1] + 1; }
				if (gameTable[x+2][y-2] == 'Y') { winningDiagonalsDown[x][y-3][1] = winningDiagonalsDown[x][y-3][1] + 1; }
				if (gameTable[x+3][y-3] == 'Y') { winningDiagonalsDown[x][y-3][1] = winningDiagonalsDown[x][y-3][1] + 1; }

				// If both players have pieces, the line is blocked and no longer part of the game, so set the scores to zero.
				if ((winningDiagonalsDown[x][y-3][0] != 0)&&(winningDiagonalsDown[x][y-3][1] != 0)) {
					winningDiagonalsDown[x][y-3][0] = 0;
					winningDiagonalsDown[x][y-3][1] = 0;
				}
			}
		}
		return;
	}

	// This function populates the combined score array to determine moves.
	// 42 neurons
	private static void doCombinedScores(int weights) {	// The weights is 1 default, 2 current or 3 new
		// To simplify neuron processing for people to understand the arrays are re-populated to 7x6 to match the game board so that they can be added.
		double[][] combinedColumnsR = new double[7][6];
		double[][] combinedColumnsY = new double[7][6];
		double[][] combinedRowsR = new double[7][6];
		double[][] combinedRowsY = new double[7][6];
		double[][] combinedDiagonalsUpR = new double[7][6];
		double[][] combinedDiagonalsUpY = new double[7][6];
		double[][] combinedDiagonalsDownR = new double[7][6];
		double[][] combinedDiagonalsDownY = new double[7][6];

		// Local copies of weightings so that code can be used with each different type of weighting.
		double 	piecesLocal 	= PIECESDEFAULT;	// Local copy taken so it can be optimised.
		double 	horizontalLocal = HORIZONTALDEFAULT;
		double 	verticalLocal 	= VERTICALDEFAULT;
		double 	diagonalLocal 	= DIAGONALDEFAULT;

		// Select the weighting to be used.
		if (weights == 2) {
			piecesLocal 	= piecesWeight;
			horizontalLocal = horizontalWeight;
			verticalLocal	= verticalWeight;
			diagonalLocal	= diagonalWeight;
		}
		if (weights == 3) {
			piecesLocal 	= piecesNew;
			horizontalLocal = horizontalNew;
			verticalLocal	= verticalNew;
			diagonalLocal	= diagonalNew;
		}
			
		// Do arrays of 7 by 6 of the scores for vertical.
		// This can't all be simplified with a loop, positions in the middle of the table are in multiple lines of 4.
		for (int x = 0; x < 7; x++) {
			combinedColumnsR[x][0] = Math.pow(piecesLocal,(double)winningColumns[x][0][0]);
			combinedColumnsR[x][1] = Math.pow(piecesLocal,(double)winningColumns[x][0][0]) + Math.pow(piecesLocal,(double)winningColumns[x][1][0]);
			combinedColumnsR[x][2] = Math.pow(piecesLocal,(double)winningColumns[x][0][0]) + Math.pow(piecesLocal,(double)winningColumns[x][1][0]) + Math.pow(piecesLocal,(double)winningColumns[x][2][0]);
			combinedColumnsR[x][3] = Math.pow(piecesLocal,(double)winningColumns[x][0][0]) + Math.pow(piecesLocal,(double)winningColumns[x][1][0]) + Math.pow(piecesLocal,(double)winningColumns[x][2][0]);
			combinedColumnsR[x][4] = Math.pow(piecesLocal,(double)winningColumns[x][1][0]) + Math.pow(piecesLocal,(double)winningColumns[x][2][0]);
			combinedColumnsR[x][5] = Math.pow(piecesLocal,(double)winningColumns[x][2][0]);

			combinedColumnsY[x][0] = Math.pow(piecesLocal,(double)winningColumns[x][0][1]);
			combinedColumnsY[x][1] = Math.pow(piecesLocal,(double)winningColumns[x][0][1]) + Math.pow(piecesLocal,(double)winningColumns[x][1][1]);
			combinedColumnsY[x][2] = Math.pow(piecesLocal,(double)winningColumns[x][0][1]) + Math.pow(piecesLocal,(double)winningColumns[x][1][1]) + Math.pow(piecesLocal,(double)winningColumns[x][2][1]);
			combinedColumnsY[x][3] = Math.pow(piecesLocal,(double)winningColumns[x][0][1]) + Math.pow(piecesLocal,(double)winningColumns[x][1][1]) + Math.pow(piecesLocal,(double)winningColumns[x][2][1]);
			combinedColumnsY[x][4] = Math.pow(piecesLocal,(double)winningColumns[x][1][1]) + Math.pow(piecesLocal,(double)winningColumns[x][2][1]);
			combinedColumnsY[x][5] = Math.pow(piecesLocal,(double)winningColumns[x][2][1]);
		}
			
		// Do arrays of 7 by 6 for the scores for horizontal.
		// This can't all be simplified with a loop, positions in the middle of the table are in multiple lines of 4.
		for (int y = 0; y < 6; y++) {
			combinedRowsR[0][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][0]);
			combinedRowsR[1][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][0]) + Math.pow(piecesLocal,(double)winningRows[1][y][0]);
			combinedRowsR[2][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][0]) + Math.pow(piecesLocal,(double)winningRows[1][y][0]) + Math.pow(piecesLocal,(double)winningRows[2][y][0]);
			combinedRowsR[3][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][0]) + Math.pow(piecesLocal,(double)winningRows[1][y][0]) + Math.pow(piecesLocal,(double)winningRows[2][y][0]) + Math.pow(piecesLocal,(double)winningRows[3][y][0]);
			combinedRowsR[4][y] =  Math.pow(piecesLocal,(double)winningRows[1][y][0]) + Math.pow(piecesLocal,(double)winningRows[2][y][0]) + Math.pow(piecesLocal,(double)winningRows[3][y][0]);
			combinedRowsR[5][y] =  Math.pow(piecesLocal,(double)winningRows[2][y][0]) + Math.pow(piecesLocal,(double)winningRows[3][y][0]);
			combinedRowsR[6][y] =  Math.pow(piecesLocal,(double)winningRows[3][y][0]);

			combinedRowsY[0][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][1]);
			combinedRowsY[1][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][1]) + Math.pow(piecesLocal,(double)winningRows[1][y][1]);
			combinedRowsY[2][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][1]) + Math.pow(piecesLocal,(double)winningRows[1][y][1]) + Math.pow(piecesLocal,(double)winningRows[2][y][1]);
			combinedRowsY[3][y] =  Math.pow(piecesLocal,(double)winningRows[0][y][1]) + Math.pow(piecesLocal,(double)winningRows[1][y][1]) + Math.pow(piecesLocal,(double)winningRows[2][y][1]) + Math.pow(piecesLocal,(double)winningRows[3][y][1]);
			combinedRowsY[4][y] =  Math.pow(piecesLocal,(double)winningRows[1][y][1]) + Math.pow(piecesLocal,(double)winningRows[2][y][1]) + Math.pow(piecesLocal,(double)winningRows[3][y][1]);
			combinedRowsY[5][y] =  Math.pow(piecesLocal,(double)winningRows[2][y][1]) + Math.pow(piecesLocal,(double)winningRows[3][y][1]);
			combinedRowsY[6][y] =  Math.pow(piecesLocal,(double)winningRows[3][y][1]);
		}
			
		// Do an arrays of 7 by 6 for diagonals.
		// Diagonals need to be processed with a specific line of code for each position. Some positions are not part of diagonals at all, some are in one and some are more than one diagonal of 4.
		combinedDiagonalsUpR[0][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][0]);
		combinedDiagonalsUpR[1][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][0]);
		combinedDiagonalsUpR[2][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][0]);
		combinedDiagonalsUpR[3][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][0]);
		combinedDiagonalsUpR[4][0] = 0;
		combinedDiagonalsUpR[5][0] = 0;
		combinedDiagonalsUpR[6][0] = 0;
		combinedDiagonalsUpR[0][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][0]);
		combinedDiagonalsUpR[1][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][0]);
		combinedDiagonalsUpR[2][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][0]);
		combinedDiagonalsUpR[3][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][0]);
		combinedDiagonalsUpR[4][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][0]);
		combinedDiagonalsUpR[5][1] = 0;
		combinedDiagonalsUpR[6][1] = 0;
		combinedDiagonalsUpR[0][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][0]);
		combinedDiagonalsUpR[1][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][0]);
		combinedDiagonalsUpR[2][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][0]);
		combinedDiagonalsUpR[3][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][0]);
		combinedDiagonalsUpR[4][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][0]);
		combinedDiagonalsUpR[5][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][0]);
		combinedDiagonalsUpR[6][2] = 0;
		combinedDiagonalsUpR[0][3] = 0;
		combinedDiagonalsUpR[1][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][0]);
		combinedDiagonalsUpR[2][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][0]);
		combinedDiagonalsUpR[3][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][0]);
		combinedDiagonalsUpR[4][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][0]);
		combinedDiagonalsUpR[5][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][0]);
		combinedDiagonalsUpR[6][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][0]);
		combinedDiagonalsUpR[0][4] = 0;
		combinedDiagonalsUpR[1][4] = 0;
		combinedDiagonalsUpR[2][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][0]);
		combinedDiagonalsUpR[3][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][0]);
		combinedDiagonalsUpR[4][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][0]);
		combinedDiagonalsUpR[5][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][0]);
		combinedDiagonalsUpR[6][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][0]);
		combinedDiagonalsUpR[0][5] = 0;
		combinedDiagonalsUpR[1][5] = 0;
		combinedDiagonalsUpR[2][5] = 0;
		combinedDiagonalsUpR[3][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][0]);
		combinedDiagonalsUpR[4][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][0]);
		combinedDiagonalsUpR[5][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][0]);
		combinedDiagonalsUpR[6][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][0]);

		combinedDiagonalsUpY[0][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][1]);
		combinedDiagonalsUpY[1][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][1]);
		combinedDiagonalsUpY[2][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][1]);
		combinedDiagonalsUpY[3][0] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][1]);
		combinedDiagonalsUpY[4][0] = 0;
		combinedDiagonalsUpY[5][0] = 0;
		combinedDiagonalsUpY[6][0] = 0;
		combinedDiagonalsUpY[0][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][1]);
		combinedDiagonalsUpY[1][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][1]);
		combinedDiagonalsUpY[2][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][1]);
		combinedDiagonalsUpY[3][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][1]);
		combinedDiagonalsUpY[4][1] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][1]);
		combinedDiagonalsUpY[5][1] = 0;
		combinedDiagonalsUpY[6][1] = 0;
		combinedDiagonalsUpY[0][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][1]);
		combinedDiagonalsUpY[1][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][1]);
		combinedDiagonalsUpY[2][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][1]);
		combinedDiagonalsUpY[3][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][1]);
		combinedDiagonalsUpY[4][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][1]);
		combinedDiagonalsUpY[5][2] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][1]);
		combinedDiagonalsUpY[6][2] = 0;
		combinedDiagonalsUpY[0][3] = 0;
		combinedDiagonalsUpY[1][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][1]);
		combinedDiagonalsUpY[2][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][1]);
		combinedDiagonalsUpY[3][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][1]);
		combinedDiagonalsUpY[4][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][1]);
		combinedDiagonalsUpY[5][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][0][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][1]);
		combinedDiagonalsUpY[6][3] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][0][1]);
		combinedDiagonalsUpY[0][4] = 0;
		combinedDiagonalsUpY[1][4] = 0;
		combinedDiagonalsUpY[2][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][1]);
		combinedDiagonalsUpY[3][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][1]);
		combinedDiagonalsUpY[4][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][1]);
		combinedDiagonalsUpY[5][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][1]);
		combinedDiagonalsUpY[6][4] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][1][1]);
		combinedDiagonalsUpY[0][5] = 0;
		combinedDiagonalsUpY[1][5] = 0;
		combinedDiagonalsUpY[2][5] = 0;
		combinedDiagonalsUpY[3][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[0][2][1]);
		combinedDiagonalsUpY[4][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[1][2][1]);
		combinedDiagonalsUpY[5][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[2][2][1]);
		combinedDiagonalsUpY[6][5] = Math.pow(piecesLocal,(double)winningDiagonalsUp[3][2][1]);
			
		combinedDiagonalsDownR[0][0] = 0;
		combinedDiagonalsDownR[1][0] = 0;
		combinedDiagonalsDownR[2][0] = 0;
		combinedDiagonalsDownR[3][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][0]);
		combinedDiagonalsDownR[4][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][0]);
		combinedDiagonalsDownR[5][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][0]);
		combinedDiagonalsDownR[6][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][0]);
		combinedDiagonalsDownR[0][1] = 0;
		combinedDiagonalsDownR[1][1] = 0;
		combinedDiagonalsDownR[2][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][0]);
		combinedDiagonalsDownR[3][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][0]);
		combinedDiagonalsDownR[4][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][0]);
		combinedDiagonalsDownR[5][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][0]);
		combinedDiagonalsDownR[6][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][0]);
		combinedDiagonalsDownR[0][2] = 0;
		combinedDiagonalsDownR[1][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][0]);
		combinedDiagonalsDownR[2][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][0]);
		combinedDiagonalsDownR[3][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][0]);
		combinedDiagonalsDownR[4][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][0]);
		combinedDiagonalsDownR[5][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][0]);
		combinedDiagonalsDownR[6][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][0]);
		combinedDiagonalsDownR[0][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][0]);
		combinedDiagonalsDownR[1][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][0]);
		combinedDiagonalsDownR[2][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][0]);
		combinedDiagonalsDownR[3][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][0]);
		combinedDiagonalsDownR[4][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][0]);
		combinedDiagonalsDownR[5][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][0]);
		combinedDiagonalsDownR[6][3] = 0;
		combinedDiagonalsDownR[0][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][0]);
		combinedDiagonalsDownR[1][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][0]);
		combinedDiagonalsDownR[2][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][0]);
		combinedDiagonalsDownR[3][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][0]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][0]);
		combinedDiagonalsDownR[4][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][0]);
		combinedDiagonalsDownR[5][4] = 0;
		combinedDiagonalsDownR[6][4] = 0;
		combinedDiagonalsDownR[0][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][0]);
		combinedDiagonalsDownR[1][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][0]);
		combinedDiagonalsDownR[2][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][2][0]);
		combinedDiagonalsDownR[3][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][0]);
		combinedDiagonalsDownR[4][5] = 0;
		combinedDiagonalsDownR[5][5] = 0;
		combinedDiagonalsDownR[6][5] = 0;

		combinedDiagonalsDownY[0][0] = 0;
		combinedDiagonalsDownY[1][0] = 0;
		combinedDiagonalsDownY[2][0] = 0;
		combinedDiagonalsDownY[3][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][1]);
		combinedDiagonalsDownY[4][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][1]);
		combinedDiagonalsDownY[5][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][1]);
		combinedDiagonalsDownY[6][0] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][1]);
		combinedDiagonalsDownY[0][1] = 0;
		combinedDiagonalsDownY[1][1] = 0;
		combinedDiagonalsDownY[2][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][1]);
		combinedDiagonalsDownY[3][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][1]);
		combinedDiagonalsDownY[4][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][1]);
		combinedDiagonalsDownY[5][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][1]);
		combinedDiagonalsDownY[6][1] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][1]);
		combinedDiagonalsDownY[0][2] = 0;
		combinedDiagonalsDownY[1][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][1]);
		combinedDiagonalsDownY[2][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][1]);
		combinedDiagonalsDownY[3][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][1]);
		combinedDiagonalsDownY[4][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][1]);
		combinedDiagonalsDownY[5][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][1]);
		combinedDiagonalsDownY[6][2] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][1]);
		combinedDiagonalsDownY[0][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][0][1]);
		combinedDiagonalsDownY[1][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][0][1]);
		combinedDiagonalsDownY[2][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][0][1]);
		combinedDiagonalsDownY[3][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][0][1]);
		combinedDiagonalsDownY[4][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][1]);
		combinedDiagonalsDownY[5][3] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][1]);
		combinedDiagonalsDownY[6][3] = 0;
		combinedDiagonalsDownY[0][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][1][1]);
		combinedDiagonalsDownY[1][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[1][1][1]);
		combinedDiagonalsDownY[2][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[2][1][1]);
		combinedDiagonalsDownY[3][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][1]) + Math.pow(piecesLocal,(double)winningDiagonalsDown[3][1][1]);
		combinedDiagonalsDownY[4][4] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][1]);
		combinedDiagonalsDownY[5][4] = 0;
		combinedDiagonalsDownY[6][4] = 0;
		combinedDiagonalsDownY[0][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[0][2][1]);
		combinedDiagonalsDownY[1][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[1][2][1]);
		combinedDiagonalsDownY[2][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[2][2][1]);
		combinedDiagonalsDownY[3][5] = Math.pow(piecesLocal,(double)winningDiagonalsDown[3][2][1]);
		combinedDiagonalsDownY[4][5] = 0;
		combinedDiagonalsDownY[5][5] = 0;
		combinedDiagonalsDownY[6][5] = 0;

		// Go through the board and combine the different scores for each position. However if the position has already been played, set it to 0. 
		for (int x = 0; x < 7; x++) {
			for (int y = 0; y < 6; y++) {
				if (gameTable[x][y] == ' ') {
					// For each position the corresponding vertical, horizontal and diagonals scores are added.
					// Each of the scores is multiplied by a weighting, before the addition.
					combinedScoresR[x][y] = (combinedColumnsR[x][y] * verticalLocal) + (combinedRowsR[x][y] * horizontalLocal) + (combinedDiagonalsUpR[x][y] * diagonalLocal) + (combinedDiagonalsDownR[x][y] * diagonalLocal);
					combinedScoresY[x][y] = (combinedColumnsY[x][y] * verticalLocal) + (combinedRowsY[x][y] * horizontalLocal) + (combinedDiagonalsUpY[x][y] * diagonalLocal) + (combinedDiagonalsDownY[x][y] * diagonalLocal);
				}
				else {
					// Clearing the array elements that have already been played isn't strictly necessary, but it makes the code easier to debug.
					combinedScoresR[x][y] = 0;
					combinedScoresY[x][y] = 0;
				}
			}
		}		
		return;
	}
}
