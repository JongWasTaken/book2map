/*
Taken from https://github.com/Patbox/Image2Map/blob/1.20.2/src/main/java/space/essem/image2map/mixin/ItemFrameEntityMixin.java
This mixin will only be applied if image2map is not loaded, as book2map behaves the same way and needs it.
 */

package dev.smto.book2map.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {
    @Shadow
    private boolean fixed;

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void image2map$fillMaps(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (!this.fixed && clickItemFrame(player, hand, (ItemFrame) (Object) this)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At("HEAD"), cancellable = true)
    private void image2map$destroyMaps(ServerLevel world, @Nullable Entity entity, boolean dropSelf,
                                       CallbackInfo ci) {
        var frame = (ItemFrame) (Object) this;

        if (!this.fixed && destroyItemFrame(entity, frame)) {
            if (dropSelf) {
                frame.spawnAtLocation(world, new ItemStack(Items.ITEM_FRAME));
            }
            ci.cancel();
        }
    }


    private static boolean clickItemFrame(Player player, InteractionHand hand, ItemFrame itemFrameEntity) {
        var stack = player.getItemInHand(hand);
        var stackNBT = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!stackNBT.isEmpty() && stack.is(Items.BUNDLE) && stackNBT.getBoolean("image2map:quick_place").orElse(false)) {
            var world = itemFrameEntity.level();
            var start = itemFrameEntity.blockPosition();
            var width = stackNBT.getInt("image2map:width").orElse(1);
            var height = stackNBT.getInt("image2map:height").orElse(1);

            var frames = new ItemFrame[width * height];

            var facing = itemFrameEntity.getDirection();
            Direction right;
            Direction down;

            int rot;

            if (facing.getAxis() != Direction.Axis.Y) {
                right = facing.getCounterClockWise();
                down = Direction.DOWN;
                rot = 0;
            } else {
                right = player.getDirection().getClockWise();
                if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    down = right.getClockWise();
                    rot = player.getDirection().getOpposite().get2DDataValue();
                } else {
                    down = right.getCounterClockWise();
                    rot = (right.getAxis() == Direction.Axis.Z ? player.getDirection() : player.getDirection().getOpposite()).get2DDataValue();
                }
            }

            var mut = start.mutable();

            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    mut.set(start);
                    mut.move(right, x);
                    mut.move(down, y);
                    var entities = world.getEntitiesOfClass(ItemFrame.class, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(mut)), (entity1) -> entity1.getDirection() == facing && entity1.blockPosition().equals(mut));
                    if (!entities.isEmpty()) {
                        frames[x + y * width] = entities.get(0);
                    }
                }
            }



            for (var mapStack : stack.get(DataComponents.BUNDLE_CONTENTS).itemCopyStream().toList()) {
                var map = mapStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                //Book2Map.Logger.warn(map.toString());
                if (!map.isEmpty()) {
                    var x = map.getInt("image2map:x").orElse(1);
                    var y = map.getInt("image2map:y").orElse(1);

                    map.putString("image2map:right", right.getSerializedName());
                    map.putString("image2map:down", down.getSerializedName());
                    map.putString("image2map:facing", facing.getSerializedName());

                    mapStack.set(DataComponents.CUSTOM_DATA,CustomData.of(map));

                    var frame = frames[x + y * width];

                    if (frame != null && frame.getItem().isEmpty()) {
                        frame.setItem(mapStack);
                        frame.setRotation(rot);
                        frame.setInvisible(true);
                    }
                }
            }

            stack.shrink(1);

            return true;
        }

        return false;
    }

    private static boolean destroyItemFrame(Entity player, ItemFrame itemFrameEntity) {
        //if (true) return false;
        var stack = itemFrameEntity.getItem();
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        //Book2Map.Logger.error(tag.toString());

        String[] requiredTags = new String[] { "image2map:x", "image2map:y", "image2map:width", "image2map:height",
                "image2map:right", "image2map:down", "image2map:facing" };

        if (stack.getItem() == Items.FILLED_MAP && tag != null && Arrays.stream(requiredTags).allMatch(tag::contains)) {
            var xo = tag.getInt("image2map:x").orElse(1);
            var yo = tag.getInt("image2map:y").orElse(1);
            var width = tag.getInt("image2map:width").orElse(1);
            var height = tag.getInt("image2map:height").orElse(1);

            Direction right = Direction.byName(tag.getString("image2map:right").orElse("north"));
            Direction down = Direction.byName(tag.getString("image2map:down").orElse("down"));
            Direction facing = Direction.byName(tag.getString("image2map:facing").orElse("north"));

            var world = itemFrameEntity.level();
            var start = itemFrameEntity.blockPosition();

            var mut = start.mutable();

            mut.move(right, -xo);
            mut.move(down, -yo);

            start = mut.immutable();

            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    mut.set(start);
                    mut.move(right, x);
                    mut.move(down, y);
                    var entities = world.getEntitiesOfClass(ItemFrame.class, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(mut)),
                            (entity1) -> entity1.getDirection() == facing && entity1.blockPosition().equals(mut));
                    if (!entities.isEmpty()) {
                        var frame = entities.get(0);

                        // Only apply to frames that contain an image2map map
                        var frameStack = frame.getItem();
                        if (frameStack.getItem() == Items.FILLED_MAP && tag != null && Arrays.stream(requiredTags).allMatch(tag::contains)) {
                            frame.setItem(ItemStack.EMPTY, true);
                            frame.setInvisible(false);
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

}