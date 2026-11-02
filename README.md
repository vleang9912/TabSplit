<h3 align="center">TabSplit</h3>


> The original project started in 2024 with Isauro Reyes and Harold Ponce Saldivar. This version uses Java Swing as a graphics library and will include new updates to the UI, UX, and code for better usability and efficiency.
> The original app focused on the app logic and backend. This 2026 update will focus on UX and the frontend experience.
> **Note:** This README is out of date. The app was moved from the Macalester kilt-graphics library to Java Swing, so the screenshots, setup steps and field names below no longer match. To do: retake the screenshots, fix the broken image links, and update the instructions.

<!-- What is TabSplit? -->
## What is TabSplit? Why use TabSplit?
TabSplit is a program that simulates a cash-flow minimizer between a group of people, given a number of expenses among them; both of which are determined by the user. While it may be easy to calculate the lowest amount of payments among 3 people (assuming they each have debts to eachother), what about with 5 people? Or 7 people? Or even 10? A cash-flow minimzer, such as TabSplit, makes keeping calculating this amount super quick and efficiently.

<!-- How to use TabSplit -->
## How do I use TabSplit?
You need Java 17 or newer. From the project folder, run:

```bash
./gradlew run
```

Run the tests with `./gradlew test`.

Once the program begins running, you should be met with this homescreen

<img width="600" alt="TabSplit_Homescreen" src="https://github.com/user-attachments/assets/468d9b6a-b90e-4049-9077-75628c794b79" />

The buttons and their functions go as followed:
* "Friends" a separate tab in which the user can add and view their friends.
* "Expenses" a separate tab in which the user can add transactions between friends
* "Quit App" closes the application

## Friends
When clicking on the "Friends" button you should be met with this screen. The "back" button will take you back to the homescreen.

<img width="600" alt="TabSplit_Friends1" src="https://github.com/user-attachments/assets/ade8b8d5-7d7a-41df-869e-8a594292456c" />

The leftmost textfield is the name of the friend you wish to add. The "Add to group" button adds them to a user graph, which will be referenced across the entire program. The text in the middle will display the users currently in the group.

<img width="600" alt="TabSplit_Friends2" src="https://github.com/user-attachments/assets/4cd74a75-b2eb-48bb-829a-4231730d0ad4" />

## Expenses
When clicking on the "Expenses" button you should be met with this screen. The "back" button will take you back to the homescreen.

<img width="601" alt="TabSplit_Expenses1" src="https://github.com/user-attachments/assets/7ff0fe18-12e1-4c6a-a959-d6d990ba8fa0" />

The "Person paying" textfield refers to the name of the friend who is the own who paid initially on behalf of another person, meaning that they will be owed money. The "Person owing" textfield refers to the name of the friend who was paid on behald by another person, meaning that they will be owing the other person money. The "Value" textfield refers to the amount that is being owed (must be numbers inputted in this textfield, letters will not work). The "Add Transaction" button will calculate the lowest amount of transaction performed using the cash flow minimizer algorith; the results will be displayed below.

<img width="600" alt="TabSplit_Expenses2" src="https://github.com/user-attachments/assets/689bc6df-73a9-4a45-bcb3-7248fe7feaad" />

<!-- Bugs -->
## Bugs
* The "Description" textfield on the "Expenses" tab may not properly display

