/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.item.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.FabricItemUpdateAnimationHandlers;
import net.fabricmc.fabric.api.client.item.v1.UpdateAnimationHandler;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
	@Redirect(
			method = "updateHeldItems",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z")
	)
	private boolean areStacksEqual(ItemStack original, ItemStack updated) {
		if (updated.getItem().equals(original.getItem())) {
			UpdateAnimationHandler updateAnimationHandler = FabricItemUpdateAnimationHandlers.get(updated.getItem());

			if (updateAnimationHandler != null) {
				return !updateAnimationHandler.shouldRunAnimationUpdate(original, updated);
			}
		}

		return ItemStack.areEqual(original, updated);
	}
}
