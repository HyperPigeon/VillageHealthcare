# Village Healthcare 🩹

A mod that allows clerics and blacksmiths to heal injured villagers and iron golems. 

Original version by HyperPigeon [here](https://github.com/HyperPigeon/VillageHealthcare)  

## Additions
- Mod Menu integration with Cloth Config API! All options listed can be tuned to preference with live updates in-game.
- Villagers now scan within a customizable radius around them for targets rather than relying on Line of Sight. This can be tuned up or down based on preference.
- Villagers are now a little smarter! Not everyone will rush to heal the same target. If one villager is on their way to heal a target, the others will either let them handle it or make their way to another heal target. One healer per target.
- Multiple customizable heal thresholds!
  - By default these are 70% for a low urgency heal, and 40% for a higher urgency heal. Higher urgency means the villagers will run faster to get to their target
  - Villagers may also selectively abandon their current heal target if a more urgent one is in range and another villager either is not able, or does not exist to help. Whoever's the most hurt gets the most help!
- Clerics can now heal themselves! This also includes a priority system. The cleric will prioritize other damaged villagers first, but if it itself drops under the urgent threshold, it will heal itself to the normal threshold and resume healing other eligible targets.
- Customizable healing amount. Allows you to tune how much per healing tick villagers heal their targets. Default is 6 for Golems and 4 for Clerics.

## Healing injured villager
https://github.com/HyperPigeon/VillageHealthcare/assets/58276133/08c3cd37-4968-47fb-a1ec-596da4e8547b

## Repairing Iron Golem
https://github.com/HyperPigeon/VillageHealthcare/assets/58276133/b5149e13-0edb-47ae-ae4f-7f430e3d4d8d


