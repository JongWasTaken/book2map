/*
Taken from https://github.com/Patbox/Image2Map/blob/1.20.2/src/main/java/space/essem/image2map/mixin/ItemFrameEntityMixin.java
This mixin will only be applied if image2map is not loaded, as book2map behaves the same way and needs it.
 */

package pw.smto.book2map.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.smto.book2map.Book2Map;

import java.util.Arrays;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {
    @Shadow
    private boolean fixed;

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void image2map$fillMaps(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!this.fixed && clickItemFrame(player, hand, (ItemFrameEntity) (Object) this)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "dropHeldStack", at = @At("HEAD"), cancellable = true)
    private void image2map$destroyMaps(@Nullable Entity entity, boolean alwaysDrop,
                                       CallbackInfo ci) {
        var frame = (ItemFrameEntity) (Object) this;

        if (!this.fixed && destroyItemFrame(entity, frame)) {
            if (alwaysDrop) {
                frame.dropStack(new ItemStack(Items.ITEM_FRAME));
            }
            ci.cancel();
        }
    }


    private static boolean clickItemFrame(PlayerEntity player, Hand hand, ItemFrameEntity itemFrameEntity) {
        var stack = player.getStackInHand(hand);
        var stackNBT = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!stackNBT.isEmpty() && stack.isOf(Items.BUNDLE) && stackNBT.getBoolean("image2map:quick_place")) {
            var world = itemFrameEntity.getWorld();
            var start = itemFrameEntity.getBlockPos();
            var width = stackNBT.getInt("image2map:width");
            var height = stackNBT.getInt("image2map:height");

            var frames = new ItemFrameEntity[width * height];

            var facing = itemFrameEntity.getHorizontalFacing();
            Direction right;
            Direction down;

            int rot;

            if (facing.getAxis() != Direction.Axis.Y) {
                right = facing.rotateYCounterclockwise();
                down = Direction.DOWN;
                rot = 0;
            } else {
                right = player.getHorizontalFacing().rotateYClockwise();
                if (facing.getDirection() == Direction.AxisDirection.POSITIVE) {
                    down = right.rotateYClockwise();
                    rot = player.getHorizontalFacing().getOpposite().getHorizontal();
                } else {
                    down = right.rotateYCounterclockwise();
                    rot = (right.getAxis() == Direction.Axis.Z ? player.getHorizontalFacing() : player.getHorizontalFacing().getOpposite()).getHorizontal();
                }
            }

            var mut = start.mutableCopy();

            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    mut.set(start);
                    mut.move(right, x);
                    mut.move(down, y);
                    var entities = world.getEntitiesByClass(ItemFrameEntity.class, Box.from(Vec3d.of(mut)), (entity1) -> entity1.getHorizontalFacing() == facing && entity1.getBlockPos().equals(mut));
                    if (!entities.isEmpty()) {
                        frames[x + y * width] = entities.get(0);
                    }
                }
            }



            for (var mapStack : stack.get(DataComponentTypes.BUNDLE_CONTENTS).stream().toList()) {
                var map = mapStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
                //Book2Map.Logger.warn(map.toString());
                if (!map.isEmpty()) {
                    var x = map.getInt("image2map:x");
                    var y = map.getInt("image2map:y");

                    map.putString("image2map:right", right.asString());
                    map.putString("image2map:down", down.asString());
                    map.putString("image2map:facing", facing.asString());

                    mapStack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(map));

                    var frame = frames[x + y * width];

                    if (frame != null && frame.getHeldItemStack().isEmpty()) {
                        frame.setHeldItemStack(mapStack);
                        frame.setRotation(rot);
                        frame.setInvisible(true);
                    }
                }
            }

            stack.decrement(1);

            return true;
        }

        return false;
    }

    private static boolean destroyItemFrame(Entity player, ItemFrameEntity itemFrameEntity) {
        //if (true) return false;
        var stack = itemFrameEntity.getHeldItemStack();
        var tag = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();

        //Book2Map.Logger.error(tag.toString());

        String[] requiredTags = new String[] { "image2map:x", "image2map:y", "image2map:width", "image2map:height",
                "image2map:right", "image2map:down", "image2map:facing" };

        if (stack.getItem() == Items.FILLED_MAP && tag != null && Arrays.stream(requiredTags).allMatch(tag::contains)) {
            var xo = tag.getInt("image2map:x");
            var yo = tag.getInt("image2map:y");
            var width = tag.getInt("image2map:width");
            var height = tag.getInt("image2map:height");

            Direction right = Direction.byName(tag.getString("image2map:right"));
            Direction down = Direction.byName(tag.getString("image2map:down"));
            Direction facing = Direction.byName(tag.getString("image2map:facing"));

            var world = itemFrameEntity.getWorld();
            var start = itemFrameEntity.getBlockPos();

            var mut = start.mutableCopy();

            mut.move(right, -xo);
            mut.move(down, -yo);

            start = mut.toImmutable();

            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    mut.set(start);
                    mut.move(right, x);
                    mut.move(down, y);
                    var entities = world.getEntitiesByClass(ItemFrameEntity.class, Box.from(Vec3d.of(mut)),
                            (entity1) -> entity1.getHorizontalFacing() == facing && entity1.getBlockPos().equals(mut));
                    if (!entities.isEmpty()) {
                        var frame = entities.get(0);

                        // Only apply to frames that contain an image2map map
                        var frameStack = frame.getHeldItemStack();
                        if (frameStack.getItem() == Items.FILLED_MAP && tag != null && Arrays.stream(requiredTags).allMatch(tag::contains)) {
                            frame.setHeldItemStack(ItemStack.EMPTY, true);
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