package com.skys.cobblemoncosmetics.mixin;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.api.mark.Marks;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Mixin to inject custom marks from our config file into Cobblemon's mark registry.
 * This allows us to define multiple marks in a single JSON file.
 */
@Mixin(value = Marks.class, remap = false)
public class CobblemonMarksMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Inject after Cobblemon's mark registry reloads to add our custom marks.
     */
    @Inject(method = "reload(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void skyscobblemon$injectCustomMarks(Map<ResourceLocation, Mark> data, CallbackInfo ci) {
        LOGGER.info("[CobblemonMarksMixin] Injecting custom marks from config...");

        try {
            // Load our marks config from resources
            InputStream stream = getClass().getClassLoader().getResourceAsStream("data/skyscobblemonitems/marks_config.json");
            if (stream == null) {
                LOGGER.warn("[CobblemonMarksMixin] Could not find marks_config.json");
                return;
            }

            Gson gson = new Gson();
            JsonObject config = gson.fromJson(new InputStreamReader(stream), JsonObject.class);
            JsonObject marksObj = config.getAsJsonObject("marks");

            if (marksObj == null) {
                LOGGER.warn("[CobblemonMarksMixin] No 'marks' object found in config");
                return;
            }

            // Access the private marks HashMap via reflection
            Field marksField = Marks.class.getDeclaredField("marks");
            marksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            HashMap<ResourceLocation, Mark> marksMap = (HashMap<ResourceLocation, Mark>) marksField.get(Marks.INSTANCE);

            int count = 0;
            for (Map.Entry<String, JsonElement> entry : marksObj.entrySet()) {
                String markId = entry.getKey();
                JsonObject markData = entry.getValue().getAsJsonObject();

                ResourceLocation identifier = ResourceLocation.fromNamespaceAndPath("cobblemon", markId);

                // Parse mark data
                String name = markData.get("name").getAsString();
                String title = markData.has("title") ? markData.get("title").getAsString() : null;
                String titleColor = markData.has("titleColor") ? markData.get("titleColor").getAsString() : null;
                String description = markData.get("description").getAsString();
                String textureStr = markData.get("texture").getAsString();
                ResourceLocation texture = ResourceLocation.parse(textureStr);
                Integer indexNumber = markData.has("indexNumber") ? markData.get("indexNumber").getAsInt() : null;

                // Create Mark instance using reflection (constructor is package-private)
                Mark mark = createMark(identifier, name, description, title, titleColor, texture, indexNumber);

                if (mark != null) {
                    marksMap.put(identifier, mark);
                    count++;
                }
            }

            LOGGER.info("[CobblemonMarksMixin] Injected {} custom marks", count);
            stream.close();

        } catch (Exception e) {
            LOGGER.error("[CobblemonMarksMixin] Failed to inject custom marks", e);
        }
    }

    /**
     * Create a Mark instance using reflection since the constructor may not be accessible.
     */
    private Mark createMark(ResourceLocation identifier, String name, String description,
                           String title, String titleColor, ResourceLocation texture, Integer indexNumber) {
        try {
            // Use Gson to create the Mark since it has a proper constructor
            Gson gson = new Gson();
            JsonObject markJson = new JsonObject();
            markJson.addProperty("name", name);
            markJson.addProperty("description", description);
            if (title != null) markJson.addProperty("title", title);
            if (titleColor != null) markJson.addProperty("titleColor", titleColor);
            markJson.addProperty("texture", texture.toString());
            if (indexNumber != null) markJson.addProperty("indexNumber", indexNumber);

            Mark mark = gson.fromJson(markJson, Mark.class);

            // Set the identifier field via reflection
            Field identifierField = Mark.class.getDeclaredField("identifier");
            identifierField.setAccessible(true);
            identifierField.set(mark, identifier);

            return mark;
        } catch (Exception e) {
            LOGGER.error("[CobblemonMarksMixin] Failed to create mark: {}", identifier, e);
            return null;
        }
    }
}
