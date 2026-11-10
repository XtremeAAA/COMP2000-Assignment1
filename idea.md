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

Suraiya:
[] Pausing and end screen

----
Assignment 2: 

*AN*
Eliminate redundant memory allocation: Instead of repeatedly calling `new FleeStrategy()`, `new WanderStrategy()`, etc., during every tick iteration—which consumes excess memory and slows down the program—we now pre-initialize these strategies as `static final` constants.

Integrate `ScaledSpeedDecorator` for Humans: Much like Zombies, Humans are now affected by adverse weather conditions (e.g., `STORM` or `FOG` will reduce the probability of a Human moving). However, a Doctor who carries the cure (`hasCure`) remains highly determined, maintaining their maximum movement speed to continue their mission of saving others.

Nov/9: Implement Safe Zones and SeekShelterStrategy for emergent survival behaviour
- Added safeZones coordinates and visual rendering (drawSafeZones) in GamePanel.java.
- Created SeekShelterStrategy using Stream min() and A* PathFinder to route humans to the nearest shelter.
- Updated Human.move() to prioritize shelter-seeking during night or storm conditions.
- Updated GamePanel.handleInfections() stream pipeline to grant infection immunity to humans inside safe zones.
Nov/10: Add dynamic Danger Zones and custom DangerZoneStats collector
- Added DangerZoneStats.java using a custom Collector.of(...) to count entities inside the danger zone in a single pass.
- Added a shifting red Danger Zone in GamePanel.java that moves every 15 steps and renders on the grid.
- Used parallel streams and method references to infect unprotected humans inside the danger zone.