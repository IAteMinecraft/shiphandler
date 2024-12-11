VS: Ship Handler is a performance mod for Valkyrien Skies.  
  
Have you ever had people creating millions of ships, making the server a crashaholic?  
Well fear no more, ship handler is here to save the day!  
  
Players will have to register their ships to their name if they want them to stay on the server.  
Admins can run the "handler" command to delete all unregistered ships from the server.  
  
## How To Use:  
  
Players can register, and unregister their ships with the commands:  
  `ship-handler ship register <ship>`  
and  
  `ship-handler ship unregister <ship>`  
  
You can list the ships that you currently have registered with:  
  `ship-handler list list`  
You can view the ships that you have created with:  
  `ship-handler list created-ships`  
  
You can view your current AutoRegister status with:  
  `ship-handler ship autoRegister`  
You can turn it on or off by adding true or false to the end of the command like so:  
  `ship-handler ship autoRegister <true|false>`  
  
  
## <Server Owner Area>  

To handle the ships, run the command:  
  `ship-handler handler run`  
  
To see all the currently registered ships, and to which player it is registered, use:  
  `ship-handler list all-registered-ships`  
To see all the currently created ships, and which player created it, use:  
  `ship-handler list all-created-ships`  
  
To remove a registered ship from another player (regardless of if you know the player's name or not) use:  
  `ship-handler ship remove <ship>`  
To register a ship to your name (regardless of previous ownership) use:  
  `hip-handler ship add <ship>`  
  
To change the various config settings go to the Server config file at:  
```
<world>
  └serverconfig
    └shiphandler.toml
```
  
The settings that you can change:  
- Auto Register Ships: Default AutoRegister Value for new players (Set to true by default)  

- Infinite OP Ships: If players that have been set as operator get to have infinite ships (Set to true by default)  
  
- Max Ships: The maximum allowed ships that a player can register to their name, set to -1 for infinite ships. This is overridden by "Infinite OP Ships" (Default set to -1)  
  
---
  
Feel free to give suggestions for improvements!  
  
You are free to use this in modpacks, just credit me please :D

---
## TODO:  
- Fix AutoRegister not working in modded dimensions
