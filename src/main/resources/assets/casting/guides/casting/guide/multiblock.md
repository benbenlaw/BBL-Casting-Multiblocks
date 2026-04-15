---
navigation:
    title: Modifier
    icon: 'castingmb:mb_controller'
    parent: index.md
    position: 200
item_ids:
    - 'castingmb:mb_controller'
    - 'castingmb:mb_solidifier'
    - 'castingmb:mb_tank'

---

# Multiblock

## Multiblock Controller 

The Multiblock Controller is the heart of Casting Multiblocks. This block functionally works the same as the normal controller but with a couple of changes. Depending on the size of the multiblock the controller can have up to 100 slots for melting items and tank scales up depending on the size as well. For each internal air block inside the multiblock controller you gain an additional item slot and 1000mb of tank capacity. The tank is also a single shared tank.

The max size of the multiblock is 1024 volume of air. Up to 128 across and 64 blocks high. If the size exceeds 1024 volume the controller will not work. Corners are also optional in the multiblock 

## Multiblock Solidifier
The Multiblock Solidifier is a solidifier that can be used in the multiblock, it works the same as the normal solidifier but uses the shared tank of the multiblock controller and can be placed anywhere in the multiblock structure. You can use as many of these as you want. Can be sped up with cooling fluids inside Multiblock Tanks

## Multiblock Tank
The Multiblock Tank is a tank that can be used in the multiblock. You can place as many of these as you want but the controller will always use the hottest fuel tank and Solidifiers will always use the coldest fuel tanks. Can be placed anywhere in the multiblock structure.

## Multiblock Bricks
Used to fill create the walls and floor of the structure

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="assets/structures/simple.nbt" />
</GameScene>
