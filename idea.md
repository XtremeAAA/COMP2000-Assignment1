# Ideas;

- Zombie apocolypse
    - Zombie vs human
    - Zombies will win
    - Pathfinding algorithm
    - Features
        - See the zombies until their in x radius
        - Safe zones
        - Day and night time different effects are applied to classes
    - Background;
        - Ruined background
        - Blocked pathway
    - Tools
        - Cure


        - Day vs night
            - Day;
                - Zombies are slowed
                - Humans are still able to move around
            - 
                
                
- Draw on JPanel inside of JFrame
- Simulation must be watchable
- Pause play and fast foward button
- Day and night environment + logo shown
- Time so you know the time
- Count on zombies, humans and cures that are on the screen
- Bottom of the screen will have a current effect status showing
- All characters will be locked to the middle of their screen
- Use an array matrix to define what is shelter, land, sea.
- No one is able to go on the sea
- Shelter is just for humans and they are imune from the zombies
- Make the map backend a 2d array and then the generated aspects are created based off that
- Make the number of columns easily adjustable
    - Backend algoirthm to make sure the 2d array matches the number of column
- Simulation should run by default and speeds should be adjustable by a button that goes form 0.5, 1, 2, and 4 times speed

--- TO DO
Achint:
[] Look at implementing A* to the algorithm
[] Add a nested Array for the back in the background

Elise:
[X] Balance behaviour of zombies and humans
[] UML Diagram for the whole team
    - Make it in Mermaid

Armak:
[] Implement the new Icons

An:
[] Look into adding a FPS variable in `COMP2000-Assignment1/Main.java`
+ added the fps vaiable starting from line 78 
+ fixed renterTimer
+ // Fast timer: smooth walking animation, day/night cycle, clock widget and //add an event for Slider are moved above the buttonBox
[] Day added
+ updated in `GamePanel.java` in `public void tick(){}`
+ add line for Day in `Main.java`
[] added the number of doctors
Suraiya: 
[] Pausing and end screen.