package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.config.parser.RecipeConfigParser;
import me.mrbast.structory.manager.RecipeManager;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.util.Optional;

public class RecipeConfig extends Config {

    private static final RecipeConfig instance;

    static {
        try {
            instance = new RecipeConfig();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static RecipeConfig getInstance() {
        return instance;
    }

    public RecipeConfig() throws IOException, InvalidConfigurationException {
        super();
    }

    @Override
    public void load() {
        initDirectory("recipes", true, config -> config.getNodes().forEach(keySection -> {
            Optional<RecipeConfigParser.RecipeParserConfig> parsed = keySection.read(RecipeConfigParser.RecipeParserConfig.class);
            parsed.ifPresent(recipe -> {
                RecipeManager.getInstance().registerRecipe(recipe.getRecipe());
                recipe.getGroups().forEach(group -> {
                    RecipeManager.getInstance().registerRecipeGroup(group);
                    RecipeManager.getInstance().setRecipeGroup(recipe.getRecipe(), group);
                });
            });
        }));
    }
}
