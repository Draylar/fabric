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

package net.fabricmc.fabric.mixin.registry;

import net.fabricmc.fabric.impl.registry.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.RemapException;
import net.fabricmc.fabric.impl.registry.RemappableRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.level.LevelProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Mixin(WorldSaveHandler.class)
public class MixinWorldSaveHandler {
	@Unique
	private static final int FABRIC_ID_REGISTRY_BACKUPS = 3;
	@Unique
	private static Logger FABRIC_LOGGER = LogManager.getLogger();
	@Shadow
	public File worldDir;

	@Unique
	private CompoundTag fabric_lastSavedIdMap = null;

	private boolean fabric_readIdMapFile(File file) throws IOException, RemapException {
		if (file.exists()) {
			FileInputStream fileInputStream = new FileInputStream(file);
			CompoundTag tag = NbtIo.readCompressed(fileInputStream);
			fileInputStream.close();
			if (tag != null) {
				RegistrySyncManager.apply(tag, RemappableRegistry.RemapMode.AUTHORITATIVE);
				return true;
			}
		}

		return false;
	}

	private File getWorldIdMapFile(int i) {
		return new File(new File(worldDir, "data"), "fabricRegistry" + ".dat" + (i == 0 ? "" : ("." + i)));
	}

	// TODO: stop double save on client?
	@Inject(method = "readProperties", at = @At("HEAD"))
	public void readWorldProperties(CallbackInfoReturnable<LevelProperties> callbackInfo) {
		// Load
		for (int i = 0; i < FABRIC_ID_REGISTRY_BACKUPS; i++) {
			FABRIC_LOGGER.info("Loading Fabric registry [file " + (i + 1) + "/" + (FABRIC_ID_REGISTRY_BACKUPS + 1) + "]");
			try {
				if (fabric_readIdMapFile(getWorldIdMapFile(i))) {
					break;
				}
			} catch (IOException e) {
				if (i >= FABRIC_ID_REGISTRY_BACKUPS - 1) {
					throw new RuntimeException(e);
				} else {
					FABRIC_LOGGER.warn("Reading registry file failed!", e);
				}
			} catch (RemapException e) {
				throw new RuntimeException("Remapping world failed!", e);
			}
		}

		CompoundTag newIdMap = RegistrySyncManager.toTag(false);
		if (!newIdMap.equals(fabric_lastSavedIdMap)) {
			for (int i = FABRIC_ID_REGISTRY_BACKUPS - 1; i >= 0; i--) {
				File file = getWorldIdMapFile(i);
				if (file.exists()) {
					if (i == FABRIC_ID_REGISTRY_BACKUPS - 1) {
						file.delete();
					} else {
						File target = getWorldIdMapFile(i + 1);
						file.renameTo(target);
					}
				}
			}

			try {
				FileOutputStream fileOutputStream = new FileOutputStream(getWorldIdMapFile(0));
				NbtIo.writeCompressed(newIdMap, fileOutputStream);
				fileOutputStream.close();
			} catch (IOException e) {
				FABRIC_LOGGER.warn("Failed to save registry file!", e);
			}

			fabric_lastSavedIdMap = newIdMap;
		}
	}
}
