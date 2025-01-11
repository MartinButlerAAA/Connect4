import subprocess

#String for the board 6 rows of 7 columns start from the bottom.
board = " YRYRY   YRY     R                        "

#Add the board string into a string to call the Java program to process it
print("Java bit --------------------------------------------------")
callString = "Java -jar Connect4.jar \"" + board + "\""
subprocess.call(callString)
print("End Java bit ----------------------------------------------")
print("")

#Open the file, read the contents and get the last character which is the move.
#Make sure the file is closed again ready for the next move.
with open("Move.txt", "r") as file:
    MoveString = file.read()
    file.close()

#Get the move into a variable so that it can be used in the game.
Move = MoveString[5]
print("Extracted Move " + Move)

board = " YRYRY   YRY    YRR                       "

#Add the board string into a string to call the Java program to process it
print("Java bit --------------------------------------------------")
callString = "Java -jar Connect4.jar \"" + board + "\""
subprocess.call(callString)
print("End Java bit ----------------------------------------------")
print("")

#Open the file, read the contents and get the last character which is the move.
#Make sure the file is closed again ready for the next move.
with open("Move.txt", "r") as file:
    MoveString = file.read()
    file.close()

#Get the move into a variable so that it can be used in the game.
Move = MoveString[5]
print("Extracted Move " + Move)
